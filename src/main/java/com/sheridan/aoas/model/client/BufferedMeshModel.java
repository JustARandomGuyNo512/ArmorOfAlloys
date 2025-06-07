package com.sheridan.aoas.model.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sheridan.aoas.ArmorOfAlloys;
import com.sheridan.aoas.ClientProxy;
import com.sheridan.aoas.compat.IrisCompat;
import com.sheridan.aoas.mixin.RenderSystemAccessor;
import com.sheridan.aoas.mixin.VertexBufferAccessor;
import com.sheridan.aoas.model.IAnimatedModel;
import com.sheridan.aoas.model.MeshModelData;
import com.sheridan.aoas.model.Vertex;
import com.sheridan.aoas.utils.RenderUtils;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glVertexAttribI2i;

@OnlyIn(Dist.CLIENT)
public class BufferedMeshModel{
    public static final Map<String, Integer> RENDER_TYPE_TF_SHADERS = new HashMap<>();
    public static final Map<String, Integer> RENDER_TYPE_TF_SHADERS_IRIS = new HashMap<>();
    public static final int COMPILE_LIGHT = LightTexture.pack(15,15);
    private static final float[] color = new float[] {1, 1, 1, 1};
    /**
     * 使用SoA结构更加高效
     * 原始数据段
     * */
    protected float[] positions;
    protected float[] normals;
    protected float[] uvs;
    //bone1 index start -> bone1 index end, bone2 index start -> bone2 index end
    protected int[] boneIndices;
    /**
     * 骨骼数据段
     * */
    public Bone rootBone;
    protected List<BoneRenderStatus> boneRenderStatusList;
    protected Map<Integer, Bone> IndexToBone;
    private FloatBuffer normalMatBuffer;
    private FloatBuffer transMatBuffer;

    protected RenderType renderType;
    protected VertexBuffer rawDataVertexBuffer;

    private int boneCount = 0;
    private int vertexCount = 0;
    private int renderingVertexCount = 0;

    public BufferedMeshModel(MeshModelData root) {
        Map<String, Bone> boneMap = new HashMap<>();
        Map<Bone, List<Vertex>> vertexMap = new HashMap<>();
        IndexToBone = new Object2ObjectArrayMap<>();
        root.depthFirstTraversal((modelData) -> {
            boolean isRoot = MeshModelData.ROOT.equals(modelData.getName());
            List<Vertex> vertices = modelData.getVertices();
            Bone bone = new Bone(boneCount, modelData.getName(), vertices.size());
            bone.loadPose(modelData);
            boneMap.put(bone.name, bone);
            IndexToBone.put(bone.index, bone);
            MeshModelData parent = modelData.getParent();
            if (parent != null) {
                Bone parentBone = boneMap.get(parent.getName());
                parentBone.addChild(bone.name, bone);
            }
            vertexMap.put(bone, vertices);
            vertexCount += vertices.size();
            boneCount ++;
            if (isRoot) {
                rootBone = bone;
            }
        });

        boneRenderStatusList = new ArrayList<>(boneCount);

        positions = new float[vertexCount * 3];
        normals = new float[vertexCount * 3];
        uvs = new float[vertexCount * 2];
        boneIndices = new int[boneCount * 2];

        List<Map.Entry<Bone, List<Vertex>>> entries = new ArrayList<>(vertexMap.entrySet());
        entries.sort(Comparator.comparingInt(o -> o.getKey().index));
        int vertexTail = 0;
        for (Map.Entry<Bone, List<Vertex>> entry : entries) {
            List<Vertex> vertices = entry.getValue();
            for (int i = 0; i < vertices.size(); i++) {
                Vertex vertex = vertices.get(i);
                positions[(vertexTail + i) * 3] = vertex.x;
                positions[(vertexTail + i) * 3 + 1] = vertex.y;
                positions[(vertexTail + i) * 3 + 2] = vertex.z;
                normals[(vertexTail + i) * 3] = vertex.normalX;
                normals[(vertexTail + i) * 3 + 1] = vertex.normalY;
                normals[(vertexTail + i) * 3 + 2] = vertex.normalZ;
                uvs[(vertexTail + i) * 2] = vertex.u;
                uvs[(vertexTail + i) * 2 + 1] = vertex.v;
            }
            boneIndices[entry.getKey().index] = vertexTail;
            vertexTail += vertices.size();
            boneIndices[entry.getKey().index + 1] = vertexTail;
            BoneRenderStatus boneRenderStatus = new BoneRenderStatus(
                    entry.getKey().index,
                    boneIndices[entry.getKey().index],
                    boneIndices[entry.getKey().index + 1]);
            boneRenderStatusList.add(boneRenderStatus);
            entry.getKey().boneRenderStatus = boneRenderStatus;
        }
    }

    public void compile(RenderType type) {
        if (rawDataVertexBuffer != null) {
            return;
        }
        renderType = type;
        rawDataVertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder rawBuilderBuffer = new ByteBufferBuilder(1024 * 256);
        BufferBuilder rawBuilder = new BufferBuilder(rawBuilderBuffer, type.mode, type.format);
        PoseStack poseStack = new PoseStack();
        poseStack.setIdentity();
        compileVertexToBuffer(rawBuilder, rootBone, poseStack.last());
        MeshData rawData = rawBuilder.build();
        if (rawData != null) {
            if (type.sortOnUpload()) {
                rawData.sortQuads(rawBuilderBuffer, RenderSystem.getVertexSorting());
            }
            int totalVertexBytes = rawData.vertexBuffer().limit();
            int floatsPerVertex = totalVertexBytes / vertexCount / 4;
            rawDataVertexBuffer.bind();
            rawDataVertexBuffer.upload(rawData);
            VertexBuffer.unbind();
        }
        rawBuilderBuffer.discard();
        rawBuilderBuffer.close();
        normalMatBuffer = BufferUtils.createFloatBuffer(9);
        transMatBuffer = BufferUtils.createFloatBuffer(16);
    }

    protected void compileVertexToBuffer(VertexConsumer rawData, Bone root, PoseStack.Pose pose) {
        int vertexStart = boneIndices[root.index];
        int vertexEnd = boneIndices[root.index + 1];
        for (int i = vertexStart; i < vertexEnd; i++) {
            int posIndex = i * 3;
            rawData.addVertex(pose, positions[posIndex], positions[posIndex + 1], positions[posIndex + 2])
                    .setColor(1f, 1f, 1f, 1f)
                    .setUv(uvs[i * 2], uvs[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(COMPILE_LIGHT)
                    .setNormal(pose, normals[posIndex], normals[posIndex + 1], normals[posIndex + 2]);
        }

        for (Bone child : root.children.values()) {
            compileVertexToBuffer(rawData, child, pose);
        }
    }

    public boolean isCompiled() {
        return rawDataVertexBuffer != null;
    }

    public void release() {
        if (rawDataVertexBuffer != null) {
            rawDataVertexBuffer.close();
            rawDataVertexBuffer = null;
        }
        if (normalMatBuffer != null) {
            normalMatBuffer.duplicate();
        }
    }

    public void render(PoseStack poseStack, int lightmapUV, float partialTick) {
        if (rawDataVertexBuffer != null && renderType != null) {
            ShaderInstance shader = GameRenderer.getRendertypeEntityCutoutShader();
            if (shader == null) {
                return;
            }

            updateBoneRenderStatus(rootBone, poseStack, lightmapUV);
            if (renderingVertexCount == 0) {
                return;
            }

            renderType.setupRenderState();
            VertexBufferAccessor accessor = (VertexBufferAccessor) rawDataVertexBuffer;
            rawDataVertexBuffer.bind();
            shader.setDefaultUniforms(accessor.getMode(), RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getWindow());
            boolean renderingShadowPass = IrisCompat.isRenderingShadowPass();
            if (renderingShadowPass) {
                shader.PROJECTION_MATRIX.set(IrisCompat.getShadowProjectionMat());
            }
            shader.apply();
            for (BoneRenderStatus status : boneRenderStatusList) {
                if (status.visible) {
                    if (ClientProxy.isIrisShaderInUse) {
                        setUpShaderLightCurrent(lightmapUV);
                        int override = GL20.glGetUniformLocation(shader.getId(), "doTransformOverride");
                        if (override != -1) {
                            GL20.glUniform1i(override, 1);
                            int norLoc = GL20.glGetUniformLocation(shader.getId(), "myTransformMat3");
                            if (norLoc != -1) {
                                Matrix3f normal = status.pose.normal();
                                normalMatBuffer.put(normal.m00);
                                normalMatBuffer.put(normal.m01);
                                normalMatBuffer.put(normal.m02);
                                normalMatBuffer.put(normal.m10);
                                normalMatBuffer.put(normal.m11);
                                normalMatBuffer.put(normal.m12);
                                normalMatBuffer.put(normal.m20);
                                normalMatBuffer.put(normal.m21);
                                normalMatBuffer.put(normal.m22);
                                normalMatBuffer.flip();
                                GL20.glUniformMatrix3fv(norLoc, false, normalMatBuffer);
                                normalMatBuffer.clear();
                            }
                            int transLoc = GL20.glGetUniformLocation(shader.getId(), "myTransformMat4");
                            if (transLoc != -1) {
                                Matrix4f pose = status.pose.pose();
                                transMatBuffer.put(pose.m00());
                                transMatBuffer.put(pose.m01());
                                transMatBuffer.put(pose.m02());
                                transMatBuffer.put(pose.m03());
                                transMatBuffer.put(pose.m10());
                                transMatBuffer.put(pose.m11());
                                transMatBuffer.put(pose.m12());
                                transMatBuffer.put(pose.m13());
                                transMatBuffer.put(pose.m20());
                                transMatBuffer.put(pose.m21());
                                transMatBuffer.put(pose.m22());
                                transMatBuffer.put(pose.m23());
                                transMatBuffer.put(pose.m30());
                                transMatBuffer.put(pose.m31());
                                transMatBuffer.put(pose.m32());
                                transMatBuffer.put(pose.m33());
                                transMatBuffer.flip();
                                GL20.glUniformMatrix4fv(transLoc, false, transMatBuffer);
                                transMatBuffer.clear();
                            }
                            GlStateManager._drawElements(accessor.getMode().asGLMode, status.vertexCount,
                                    accessor.invokeGetIndexType().asGLType, (long) status.vertexStart * accessor.invokeGetIndexType().bytes);
                            status.visible = false;
                            GL20.glUniform1i(override, 0);
                        }
                    } else {
                        vanillaShaderLightCurrent(lightmapUV, shader, status.pose.normal());
                        GlStateManager._drawElements(accessor.getMode().asGLMode, status.vertexCount,
                                accessor.invokeGetIndexType().asGLType, (long) status.vertexStart * accessor.invokeGetIndexType().bytes);
                        status.visible = false;
                    }
                }
            }
            glEnableVertexAttribArray(4);
            shader.clear();
            VertexBuffer.unbind();
            renderType.clearRenderState();
            renderingVertexCount = 0;
        }
    }

    public void vanillaRender(PoseStack poseStack, int lightmapUV, float partialTick, VertexConsumer vertexConsumer) {
        updateBoneRenderStatus(rootBone, poseStack, lightmapUV);
        for (BoneRenderStatus status : boneRenderStatusList) {
            if (status.vertexCount <= 0) {
                continue;
            }
            int vertexStart = status.vertexStart;
            int vertexEnd = status.vertexEnd;
            for (int i = vertexStart; i < vertexEnd; i++) {
                int posIndex = i * 3;
                vertexConsumer.addVertex(status.pose, positions[posIndex], positions[posIndex + 1], positions[posIndex + 2])
                        .setColor(1f, 1f, 1f, 1f)
                        .setUv(uvs[i * 2], uvs[i * 2 + 1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(lightmapUV)
                        .setNormal(status.pose, normals[posIndex], normals[posIndex + 1], normals[posIndex + 2]);
            }
        }
    }

    protected void setUpShaderLightCurrent(int light) {
        int u = light & '\uffff', v = light >> 16 & '\uffff';
        glDisableVertexAttribArray(4);
        glVertexAttribI2i(4, u, v);
    }

    protected void vanillaShaderLightCurrent(int packedLight, ShaderInstance shader, Matrix3f normalMat) {
        setUpShaderLightCurrent(packedLight);
        Vector3f[] shaderLightDirections = RenderSystemAccessor.getShaderLightDirections();

        Matrix3f normal = new Matrix3f(normalMat);
        normal.invert();

        Vector3f transform = normal.transform(new Vector3f(shaderLightDirections[0]));
        Vector3f transform1 = normal.transform(new Vector3f(shaderLightDirections[1]));

        shader.LIGHT0_DIRECTION.set(transform);
        shader.LIGHT1_DIRECTION.set(transform1);
        shader.LIGHT0_DIRECTION.upload();
        shader.LIGHT1_DIRECTION.upload();
    }

    protected void updateBoneRenderStatus(Bone root, PoseStack poseStack, int light) {
        if (root.visible) {
            poseStack.pushPose();
            root.translateAndRotate(poseStack);
            root.updateRenderStatus(poseStack, light);
            renderingVertexCount += root.vertexCount;
            for (Bone child : root.children.values()) {
                updateBoneRenderStatus(child, poseStack, light);
            }
            poseStack.popPose();
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class BoneRenderStatus {
        public final int boneIndex;
        //渲染状态控制：
        public PoseStack.Pose pose;
        public int lightmapUV;
        public final int vertexStart;
        public final int vertexEnd;
        public final int vertexCount;
        public boolean visible = false;

        public BoneRenderStatus(int boneIndex, int vertexStart, int vertexEnd) {
            this.boneIndex = boneIndex;
            this.vertexStart = vertexStart;
            this.vertexEnd = vertexEnd;
            this.vertexCount = vertexEnd - vertexStart;
            PoseStack poseStack = new PoseStack();
            this.pose = poseStack.last();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Bone implements IAnimatedModel{
        public boolean visible;
        public Bone parent;
        public final String name;
        public final int index;
        public PartPose initialPose;
        public float x, y, z;
        public float xRot, yRot, zRot;
        public float xScale, yScale, zScale;
        public final Map<String, Bone> children = new Object2ObjectArrayMap<>();
        public BoneRenderStatus boneRenderStatus;
        public final int vertexCount;

        public Bone(int index, String name, int vertexCount) {
            this.index = index;
            this.name = name;
            this.vertexCount = vertexCount;
            this.visible = true;
        }

        public void addChild(String name, Bone bone) {
            this.children.put(name, bone);
            bone.parent = this;
        }

        public void loadPose(MeshModelData meshModelData) {
            initialPose = meshModelData.getPose();
            resetPose();
        }

        public void updateRenderStatus(PoseStack poseStack, int light) {
            if (vertexCount <= 0) {
                return;
            }
            boneRenderStatus.pose = RenderUtils.copyPoseStack(poseStack).last();
            boneRenderStatus.lightmapUV = light;
            boneRenderStatus.visible = true;
        }

        @Override
        public Stream<IAnimatedModel> getAllParts() {
            return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(Bone::getAllParts));
        }

        @Override
        public boolean hasChild(String pName) {
            return children.containsKey(pName);
        }

        @Override
        public IAnimatedModel getChild(String pName) {
            return children.get(pName);
        }

        @Override
        public void offsetPos(Vector3f vector3f) {
            this.x += vector3f.x();
            this.y += vector3f.y();
            this.z += vector3f.z();
        }

        @Override
        public void offsetRotation(Vector3f vector3f) {
            this.xRot += vector3f.x();
            this.yRot += vector3f.y();
            this.zRot += vector3f.z();
        }

        @Override
        public void offsetScale(Vector3f vector3f) {
            this.xScale += vector3f.x();
            this.yScale += vector3f.y();
            this.zScale += vector3f.z();
        }

        public void translateAndRotate(PoseStack poseStack) {
            poseStack.translate(this.x , this.y, this.z);
            if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
                poseStack.mulPose((new Quaternionf()).rotationZYX(this.zRot, this.yRot, this.xRot));
            }
            if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
                poseStack.scale(this.xScale, this.yScale, this.zScale);
            }
        }

        public void resetPose() {
            x = initialPose.x;
            y = initialPose.y;
            z = initialPose.z;
            xRot = initialPose.xRot;
            yRot = initialPose.yRot;
            zRot = initialPose.zRot;
            xScale = 1.0F;
            yScale = 1.0F;
            zScale = 1.0F;
        }
    }

}

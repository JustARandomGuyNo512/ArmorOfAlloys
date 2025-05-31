package com.sheridan.aoas.model.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sheridan.aoas.model.IAnimatedModel;
import com.sheridan.aoas.model.MeshModelData;
import com.sheridan.aoas.model.Vertex;
import com.sheridan.aoas.utils.RenderUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.*;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class BufferedMeshModel{
    public static final int COMPILE_LIGHT = LightTexture.pack(15,15);

    final int BYTES_PER_PART =
                    64 +         // mat4 pose
                    48 +         // mat3 normal
                    4 * 5;
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

    protected RenderType renderType;
    protected VertexBuffer renderVertexBuffer;
    protected VertexBuffer rawDataVertexBuffer;

    private int boneCount = 0;
    private int vertexCount = 0;

    private int renderStatusBufferId;
    protected ByteBuffer renderStatusBuffer;

    public BufferedMeshModel(MeshModelData root) {
        Map<String, Bone> boneMap = new HashMap<>();
        Map<Bone, List<Vertex>> vertexMap = new HashMap<>();
        IndexToBone = new Object2ObjectArrayMap<>();
        root.depthFirstTraversal((modelData) -> {
            boolean isRoot = MeshModelData.ROOT.equals(modelData.getName());
            Bone bone = new Bone(boneCount, modelData.getName());
            bone.loadPose(modelData);
            boneMap.put(bone.name, bone);
            IndexToBone.put(bone.index, bone);
            MeshModelData parent = modelData.getParent();
            if (parent != null) {
                Bone parentBone = boneMap.get(parent.getName());
                parentBone.addChild(bone.name, bone);
            }
            List<Vertex> vertices = modelData.getVertices();
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
        renderStatusBufferId = GlStateManager._glGenBuffers();
        renderStatusBuffer = MemoryUtil.memAlloc(boneRenderStatusList.size() * BYTES_PER_PART);
    }

    public void compile(RenderType type, PoseStack.Pose pose) {
        if (renderVertexBuffer != null) {
            return;
        }
        renderType = type;
        rawDataVertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);

        ByteBufferBuilder rawBuilderBuffer = new ByteBufferBuilder(1024 * 256);
        BufferBuilder rawBuilder = new BufferBuilder(rawBuilderBuffer, type.mode, type.format);

        compileVertexToBuffer(rawBuilder, rootBone, pose);
        MeshData rawData = rawBuilder.build();

        if (rawData != null) {
            if (type.sortOnUpload()) {
                rawData.sortQuads(rawBuilderBuffer, RenderSystem.getVertexSorting());
            }
            rawDataVertexBuffer.bind();
            rawDataVertexBuffer.upload(rawData);
            VertexBuffer.unbind();
        }

        rawBuilderBuffer.discard();
        rawBuilderBuffer.close();

        renderVertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
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
        return renderVertexBuffer != null;
    }

    public void release() {
        if (renderVertexBuffer != null) {
            renderVertexBuffer.close();
            renderVertexBuffer = null;
        }
        if (rawDataVertexBuffer != null) {
            rawDataVertexBuffer.close();
            rawDataVertexBuffer = null;
        }
    }

    public void render(PoseStack poseStack, int lightmapUV) {
        if (renderVertexBuffer != null && renderType != null) {
            ShaderInstance shader = GameRenderer.getRendertypeEntityCutoutShader();
            if (shader == null) {
                return;
            }
            updateBoneRenderStatus(rootBone, poseStack, lightmapUV);
            renderStatusBuffer.flip();
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, renderStatusBufferId);
            RenderSystem.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, renderStatusBuffer, GL43.GL_DYNAMIC_DRAW);
            // unbind
            GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            //TODO: call compute shader

            renderType.setupRenderState();
            rawDataVertexBuffer.bind();
            rawDataVertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
            renderType.clearRenderState();

            renderStatusBuffer.clear();
        }
    }

    protected void updateBoneRenderStatus(Bone root, PoseStack poseStack, int light) {
        if (root.visible) {
            poseStack.pushPose();
            root.translateAndRotate(poseStack);
            root.updateRenderStatus(poseStack, light);
            root.boneRenderStatus.writeToBuffer(renderStatusBuffer);
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
        public int UOffest;
        public int VOffset;
        public final int vertexStart;
        public final int vertexEnd;

        public BoneRenderStatus(int boneIndex, int vertexStart, int vertexEnd) {
            this.boneIndex = boneIndex;
            this.vertexStart = vertexStart;
            this.vertexEnd = vertexEnd;
            PoseStack poseStack = new PoseStack();
            this.pose = poseStack.last();
        }

        public void writeToBuffer(ByteBuffer buffer) {
            Matrix4f pose = this.pose.pose();
            Matrix3f normal = this.pose.normal();

            buffer.putFloat(pose.m00());
            buffer.putFloat(pose.m01());
            buffer.putFloat(pose.m02());
            buffer.putFloat(pose.m03());
            buffer.putFloat(pose.m10());
            buffer.putFloat(pose.m11());
            buffer.putFloat(pose.m12());
            buffer.putFloat(pose.m13());
            buffer.putFloat(pose.m20());
            buffer.putFloat(pose.m21());
            buffer.putFloat(pose.m22());
            buffer.putFloat(pose.m23());
            buffer.putFloat(pose.m30());
            buffer.putFloat(pose.m31());
            buffer.putFloat(pose.m32());
            buffer.putFloat(pose.m33());

            buffer.putFloat(normal.m00);
            buffer.putFloat(normal.m01);
            buffer.putFloat(normal.m02);
            buffer.putFloat(normal.m10);
            buffer.putFloat(normal.m11);
            buffer.putFloat(normal.m12);
            buffer.putFloat(normal.m20);
            buffer.putFloat(normal.m21);
            buffer.putFloat(normal.m22);

            buffer.putInt(lightmapUV);
            buffer.putInt(UOffest);
            buffer.putInt(VOffset);
            buffer.putInt(vertexStart);
            buffer.putInt(vertexEnd);
            buffer.putInt(boneIndex);
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

        public Bone(int index, String name) {
            this.index = index;
            this.name = name;
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
            boneRenderStatus.pose = RenderUtils.copyPoseStack(poseStack).last();
            boneRenderStatus.lightmapUV = light;
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

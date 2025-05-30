package com.sheridan.aoas.model.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.sheridan.aoas.model.IAnimatedModel;
import com.sheridan.aoas.model.MeshModelData;
import com.sheridan.aoas.model.Vertex;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.*;

import java.util.*;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class BufferedMeshModel{
    public static final int COMPILE_LIGHT = LightTexture.pack(15,15);
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
            boneRenderStatusList.add(new BoneRenderStatus(entry.getKey().index));
        }
    }

    public void compile(RenderType type, PoseStack.Pose pose) {
        if (renderVertexBuffer != null) {
            return;
        }
        renderType = type;
        renderVertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        rawDataVertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(1024 * 256);
        BufferBuilder builder = new BufferBuilder(byteBufferBuilder, type.mode, type.format);
        compileVertexToBuffer(builder, rootBone, pose);
        MeshData data = builder.build();
        if (data != null) {
            if (type.sortOnUpload()) {
                data.sortQuads(byteBufferBuilder, RenderSystem.getVertexSorting());
            }
            renderVertexBuffer.bind();
            renderVertexBuffer.upload(data);
            VertexBuffer.unbind();
        }
        byteBufferBuilder.discard();
        byteBufferBuilder.close();
    }

    protected void compileVertexToBuffer(VertexConsumer vertexConsumer, Bone root, PoseStack.Pose pose) {
        int vertexStart = boneIndices[root.index];
        int vertexEnd = boneIndices[root.index + 1];
        for (int i = vertexStart; i < vertexEnd; i++) {
            int posIndex = i * 3;
            vertexConsumer
                    .addVertex(pose, positions[posIndex], positions[posIndex + 1], positions[posIndex + 2])
                    .setColor(1f, 1f, 1f, 1f)
                    .setUv(uvs[i * 2], uvs[i * 2 + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(COMPILE_LIGHT)
                    .setNormal(pose, normals[posIndex], normals[posIndex + 1], normals[posIndex + 2]);
        }
        for (Bone child : root.children.values()) {
            compileVertexToBuffer(vertexConsumer, child, pose);
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
            updateBoneRenderStatus();
            renderType.setupRenderState();
            renderVertexBuffer.bind();
            renderVertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
            renderType.clearRenderState();
        }
    }

    protected void updateBoneRenderStatus() {

    }

    @OnlyIn(Dist.CLIENT)
    public static class BoneRenderStatus {
        public PartPose pose;
        public int lightmapUV;
        public Vector2i UVOffset;
        public int boneIndex;

        public BoneRenderStatus(int boneIndex) {
            this.boneIndex = boneIndex;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Bone implements IAnimatedModel{
        public Bone parent;
        public final String name;
        public final int index;
        public PartPose initialPose;
        public float x, y, z;
        public float xRot, yRot, zRot;
        public float xScale, yScale, zScale;
        public final Map<String, Bone> children = new Object2ObjectArrayMap<>();

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

package com.sheridan.aoas.model.client;

import com.mojang.blaze3d.vertex.*;
import com.sheridan.aoas.model.IAnimatedModel;
import com.sheridan.aoas.model.MeshModelData;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class BufferedMeshModel{
    /**
     * 使用SoA结构更加高效
     * 原始数据段
     * */
    protected float[] positions;
    protected float[] normals;
    protected float[] uvs;
    protected int[] indices;

    /**
     * 骨骼数据段
     * */
    protected Bone bone;

    protected RenderType renderType;
    protected BufferBuilder buffer;
    protected VertexBuffer vertexBuffer;
    protected ResourceLocation texture;
    protected MultiBufferSource.BufferSource bufferSource;

    public BufferedMeshModel(MeshModelData root) {

    }

    public void compile(RenderType type) {

    }

    public static class Bone implements IAnimatedModel{
        public PartPose initialPose;
        public float x, y, z;
        public float xRot, yRot, zRot;
        public float xScale, yScale, zScale;
        public final Map<String, Bone> children = new Object2ObjectArrayMap<>();

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

package com.sheridan.aoas.model;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MeshModelData {
    protected final List<Vertex> vertices;
    private final Map<String, MeshModelData> children;
    private PartPose initialPose = PartPose.ZERO;
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    private MeshModelData parent;

    public MeshModelData() {
        this.children = new Object2ObjectArrayMap<>();
        this.vertices = new ArrayList<>();
    }


    public void addChild(String name, MeshModelData bone) {
        this.children.put(name, bone);
        bone.parent = this;
    }

    public void setInitialPose(PartPose partPose) {
        this.initialPose = partPose;
    }

    public void print() {
        System.out.println(this);
        System.out.println("vertex count: " + vertices.size());
        for (Map.Entry<String, MeshModelData> partEntry : children.entrySet()) {
            System.out.print(partEntry.getKey());
            partEntry.getValue().print();
        }
    }

    @Override
    public String toString() {
        return "{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", xRot=" + xRot +
                ", yRot=" + yRot +
                ", zRot=" + zRot +
                ", xScale=" + xScale +
                ", yScale=" + yScale +
                ", zScale=" + zScale +
                '}';
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

    public Map<String, MeshModelData> getChildren() {
        return children;
    }

    public MeshModelData getParent() {
        return parent;
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

    public void pushVertex(float v, float v1, float v2, float u, float v3, float nx, float ny, float nz, int index) {
        vertices.add(new Vertex(v, v1, v2, u, v3, nx, ny, nz, index));
    }
}

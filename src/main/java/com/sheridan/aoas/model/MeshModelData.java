package com.sheridan.aoas.model;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MeshModelData {
    // 根节点名称常量
    public static final String ROOT = "root";
    // 存储顶点数据的列表，包含位置、UV、法线等信息
    protected final List<Vertex> vertices;
    // 子骨骼模型数据的映射表，用于构建骨骼层级结构
    private final Map<String, MeshModelData> children;
    // 初始姿态，默认为零偏移
    private PartPose initialPose = PartPose.ZERO;
    // 位置坐标
    public float x;
    public float y;
    public float z;
    // 旋转角度（弧度）
    public float xRot;
    public float yRot;
    public float zRot;
    // 缩放比例
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    // 父级骨骼引用
    private MeshModelData parent;
    // 当前骨骼名称
    private String name;

    /**
     * 默认构造函数，初始化根骨骼
     */
    public MeshModelData() {
        this.children = new Object2ObjectArrayMap<>();
        this.vertices = new ArrayList<>();
        name = ROOT;
    }

    /**
     * 添加子骨骼到当前骨骼
     * @param name 子骨骼名称
     * @param bone 子骨骼对象
     */
    public void addChild(String name, MeshModelData bone) {
        this.children.put(name, bone);
        bone.parent = this;
        bone.name = name;
    }

    /**
     * 设置初始姿态
     * @param partPose 初始姿态配置
     */
    public void setInitialPose(PartPose partPose) {
        this.initialPose = partPose;
    }

    /**
     * 打印当前骨骼及其子骨骼的信息
     */
    public void print() {
        System.out.println(this);
        System.out.println("name: " + name);
        if (parent != null) {
            System.out.println("parent: " + parent.name);
        }
        System.out.println("vertex count: " + vertices.size());
        for (Map.Entry<String, MeshModelData> partEntry : children.entrySet()) {
            System.out.print(partEntry.getKey());
            partEntry.getValue().print();
        }
    }

    /**
     * 返回当前骨骼的位置和变换信息字符串表示
     */
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

    /**
     * 应用平移和旋转变换到给定的位姿栈上
     * @param poseStack 要应用变换的位姿栈
     */
    public void translateAndRotate(PoseStack poseStack) {
        poseStack.translate(this.x , this.y, this.z);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            poseStack.mulPose((new Quaternionf()).rotationZYX(this.zRot, this.yRot, this.xRot));
        }
        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    /**
     * 获取子骨骼映射表
     * @return 子骨骼映射
     */
    public Map<String, MeshModelData> getChildren() {
        return children;
    }

    /**
     * 获取父骨骼对象
     * @return 父骨骼
     */
    public MeshModelData getParent() {
        return parent;
    }

    /**
     * 重置当前骨骼的姿态到初始状态
     */
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

    /**
     * 添加一个顶点到顶点列表中
     * @param v X坐标
     * @param v1 Y坐标
     * @param v2 Z坐标
     * @param u U纹理坐标
     * @param v3 V纹理坐标
     * @param nx X方向法线
     * @param ny Y方向法线
     * @param nz Z方向法线
     * @param index 顶点索引
     */
    public void pushVertex(float v, float v1, float v2, float u, float v3, float nx, float ny, float nz, int index) {
        vertices.add(new Vertex(v, v1, v2, u, v3, nx, ny, nz, index));
    }
}

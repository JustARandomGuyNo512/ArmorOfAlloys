package com.sheridan.aoas.utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.aoas.mixin.LightTextureAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.lang.reflect.Field;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;

public class RenderUtils {

    /**
     * 仅在客户端环境中使用此方法
     * 此方法的目的是复制给定的PoseStack对象
     * 复制操作包括创建一个新的PoseStack，并将给定的PoseStack的当前状态（包括变换矩阵和法线矩阵）复制到新对象中
     *
     * @param stack 需要复制的PoseStack对象
     * @return 复制后的PoseStack对象，具有与原对象相同的当前状态
     */
    @OnlyIn(Dist.CLIENT)
    public static PoseStack copyPoseStack(PoseStack stack) {
        PoseStack result = new PoseStack();
        result.setIdentity();
        result.last().pose().set(stack.last().pose());
        result.last().normal().set(stack.last().normal());
        return result;
    }

    /**
     * 仅在客户端环境中使用此方法
     * 检查是否启用了Iris光影
     *
     * @return 如果Iris着色器被启用并正在使用自定义着色器包，则返回true；否则返回false
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean isIrisShaderEnabled() {
        try {
            Class<?> irisClass = Class.forName("net.coderbot.iris.Iris");
            Field currentPackField = irisClass.getDeclaredField("currentPack");
            currentPackField.setAccessible(true);
            Object currentPack = currentPackField.get(null);
            return currentPack != null;
        } catch (Exception e) {
            return false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void fullGLContextPrint(int programId, int shaderId) {
        System.out.println("context test start: programId:" + programId + " shaderId:" + shaderId);
        IntBuffer countBuffer = BufferUtils.createIntBuffer(1);
        GL20.glGetProgramiv(programId, GL20.GL_ACTIVE_UNIFORMS, countBuffer);
        int uniformCount = countBuffer.get(0);
        System.out.println("debug start");
        for (int i = 0; i < uniformCount; i++) {
            IntBuffer sizeBuffer = BufferUtils.createIntBuffer(1);
            IntBuffer typeBuffer = BufferUtils.createIntBuffer(1);
            String name = GL20.glGetActiveUniform(programId, i, 16, sizeBuffer, typeBuffer);
            int location = GL20.glGetUniformLocation(programId, name);

            System.out.println("Uniform #" + i + ": " + name +
                    " | Type: " + typeBuffer.get(0) +
                    " | Location: " + location);
        }
        printAllAttributes(shaderId);
        System.out.println("context test end: programId:" + programId + " shaderId:" + shaderId + "\n\n\n");
    }

    @OnlyIn(Dist.CLIENT)
    public static void printAllAttributes(int shaderProgram) {
        // 获取活动attribute数量
        int numAttributes = glGetProgrami(shaderProgram, GL_ACTIVE_ATTRIBUTES);

        System.out.println("Active Attributes: " + numAttributes);

        // 准备缓冲区
        IntBuffer sizeBuf = BufferUtils.createIntBuffer(1);
        IntBuffer typeBuf = BufferUtils.createIntBuffer(1);

        for (int i = 0; i < numAttributes; i++) {
            // 获取attribute信息
            String attrName = glGetActiveAttrib(shaderProgram, i, sizeBuf, typeBuf);
            int attrSize = sizeBuf.get(0);
            int attrType = typeBuf.get(0);


            // 获取attribute的位置(slot)
            int location = glGetAttribLocation(shaderProgram, attrName);

            System.out.printf("Attribute #%d: %s (location=%d, size=%d, type=%s)%n",
                    i, attrName, location, attrSize, getTypeName(attrType));
        }
    }

    private static String getTypeName(int type) {
        return switch (type) {
            case GL_FLOAT -> "float";
            case GL_INT_VEC2 -> "vec2i";
            case GL_FLOAT_VEC2 -> "vec2";
            case GL_FLOAT_VEC3 -> "vec3";
            case GL_FLOAT_VEC4 -> "vec4";
            case GL_FLOAT_MAT2 -> "mat2";
            case GL_FLOAT_MAT3 -> "mat3";
            case GL_FLOAT_MAT4 -> "mat4";
            default -> "Unknown(" + type + ")";
        };
    }

    /**
     * 返回一个在给定的投影矩阵中，绝对位于视锥体外的位置，可用于ndc裁剪隐藏的物体或者跳过光栅化和fragment shader对某些物体的处理
     * @param projectionMatrix 投影矩阵，用于将坐标从视图空间转换到剪裁空间
     * @return 返回一个在视锥体外部的位置，以Vector3f形式表示
     */
    public static Vector3f getOutsideFrustumPosition(Matrix4f projectionMatrix) {
        Vector4f clipSpace = new Vector4f(2.0f, 0.0f, 0.0f, 1.0f);
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Vector4f viewSpace = invProj.transform(new Vector4f(clipSpace));
        viewSpace.div(viewSpace.w);
        return new Vector3f(viewSpace.x, viewSpace.y, viewSpace.z);
    }

    static Vector3f NONE = new Vector3f(1, 1, 1);

    public static Vector3f vanillaLightColorUnpack(int packedLight) {
        LightTexture lightmap = Minecraft.getInstance().gameRenderer.lightTexture();
        NativeImage pixels = ((LightTextureAccessor) lightmap).getLightTexture().getPixels();
        if (pixels == null) {
            return NONE;
        }

        int u = packedLight & '\uffff';
        int v = packedLight >> 16 & '\uffff';

        int color = pixels.getPixelRGBA(u / 16, v / 16);

        float b = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float r = (color & 0xFF) / 255f;
        return new Vector3f(r, g, b);
    }
}

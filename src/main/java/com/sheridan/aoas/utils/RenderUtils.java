package com.sheridan.aoas.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
}

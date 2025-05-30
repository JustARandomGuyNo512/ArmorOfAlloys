package com.sheridan.aoas.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.lang.reflect.Field;

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

}

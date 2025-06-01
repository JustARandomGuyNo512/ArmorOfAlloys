package com.sheridan.aoas.events.common;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.aoas.ArmorOfAlloys;
import com.sheridan.aoas.client.render.RenderTypes;
import com.sheridan.aoas.model.MeshModelData;
import com.sheridan.aoas.model.client.BufferedMeshModel;
import com.sheridan.aoas.model.gltf.io.GltfModelLoader;
import net.irisshaders.iris.apiimpl.IrisApiV0Impl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.joml.Quaternionf;

public class TestEvents {
    static boolean test = false;
    static BufferedMeshModel bufferedMeshModel;
//
////    @SubscribeEvent
////    public static void testEvent0(RenderFrameEvent.Pre event) {
////        System.out.println("render start");
////    }
////
//    @SubscribeEvent
//    public static void testEvent1(RenderLivingEvent.Pre<?, ?> event) {
//        if (event.getEntity() instanceof Player) {
//
//            if (!test) {
//                MeshModelData meshModelData = GltfModelLoader.loadModel(ResourceLocation.fromNamespaceAndPath(ArmorOfAlloys.MODID, "model_assets/test/m1a2.gltf"));
//                if (meshModelData != null) {
//                    meshModelData.print();
//                    bufferedMeshModel = new BufferedMeshModel(meshModelData);
//                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ArmorOfAlloys.MODID, "model_assets/test/m1a2.png");
//                    PoseStack poseStack = new PoseStack();
//                    poseStack.translate(0,0, -2.5f);
//                    poseStack.scale(0.2f, 0.2f, 0.2f);
//                    bufferedMeshModel.compile(RenderTypes.getMeshCutOut(texture), poseStack.last());
//                }
//                test = true;
//            }
//            PoseStack poseStack = new PoseStack();
//            if (bufferedMeshModel != null) {
//                bufferedMeshModel.render(poseStack, 0);
//            }
//        }
//    }



    static ModelPart modelPart = BoatModel.createBodyModel().bakeRoot();
    @SubscribeEvent
    public static void testEvent(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            if (!test) {
                MeshModelData meshModelData = GltfModelLoader.loadModel(ResourceLocation.fromNamespaceAndPath(ArmorOfAlloys.MODID, "model_assets/test/m1a2.gltf"));
                if (meshModelData != null) {
                    meshModelData.print();
                    bufferedMeshModel = new BufferedMeshModel(meshModelData);
                    ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ArmorOfAlloys.MODID, "model_assets/test/m1a2.png");
                    PoseStack poseStack = new PoseStack();
                    poseStack.translate(0,0, -2.5f);
                    poseStack.scale(0.2f, 0.2f, 0.2f);
                    bufferedMeshModel.compile(RenderTypes.getMeshCutOut(texture), poseStack.last());
                }
                test = true;
            }
            PoseStack poseStack = new PoseStack();
            LocalPlayer player = Minecraft.getInstance().player;
            if (bufferedMeshModel != null && player != null && player.getMainHandItem().getItem() == Items.APPLE) {
                bufferedMeshModel.render(poseStack, 0);
            }


        }

    }
}

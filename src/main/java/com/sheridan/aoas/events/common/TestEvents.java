package com.sheridan.aoas.events.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.aoas.ArmorOfAlloys;
import com.sheridan.aoas.client.render.RenderTypes;
import com.sheridan.aoas.model.MeshModelData;
import com.sheridan.aoas.model.client.BufferedMeshModel;
import com.sheridan.aoas.model.gltf.io.GltfModelLoader;
import net.irisshaders.iris.apiimpl.IrisApiV0Impl;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class TestEvents {
    static boolean test = false;
    static BufferedMeshModel bufferedMeshModel;
    static int count = 0;
    static float timer = 0;
    static long lastTime = 0;
    static ResourceLocation texture;
    static int tag = 0;

    @SubscribeEvent
    public static void testEvent0(RenderFrameEvent.Pre event) {
        count = 0;
    }

    @SubscribeEvent
    public static void testEvent1(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Villager) {

            if (!test) {
                MeshModelData meshModelData = GltfModelLoader.loadModel(ResourceLocation.fromNamespaceAndPath(ArmorOfAlloys.MODID, "model_assets/test/m1a2.gltf"));
                if (meshModelData != null) {
                    bufferedMeshModel = new BufferedMeshModel(meshModelData);
                    texture = ResourceLocation.fromNamespaceAndPath(ArmorOfAlloys.MODID, "model_assets/test/m1a2.png");
                    bufferedMeshModel.compile(RenderTypes.getMeshCutOut(texture));
                }
                test = true;
            }
            if (lastTime == 0) {
                lastTime = System.currentTimeMillis();
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (bufferedMeshModel != null && player != null) {
                float timeDist = (System.currentTimeMillis() - lastTime) / 80f;
                timer += timeDist;
                lastTime = System.currentTimeMillis();
                if (player.getMainHandItem().getItem() == Items.APPLE) {
                    if (tag != 2) {
                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("new render"));
                        tag = 2;
                    }
                    PoseStack poseStack = event.getPoseStack();
                    poseStack.mulPose(new Quaternionf().rotateLocalY((float) Math.toRadians(timer % 360)));
                    bufferedMeshModel.render(poseStack, event.getPackedLight(), event.getPartialTick());
                } else {
                    if (tag != 1) {
                        Minecraft.getInstance().player.sendSystemMessage(Component.literal("vanilla render"));
                        tag = 1;
                    }
                    PoseStack poseStack = event.getPoseStack();
                    poseStack.mulPose(new Quaternionf().rotateLocalY((float) Math.toRadians(timer % 360)));
                    bufferedMeshModel.vanillaRender(
                            poseStack,
                            event.getPackedLight(),
                            event.getPartialTick(),
                            event.getMultiBufferSource().getBuffer(RenderTypes.getMeshCutOut(texture)));
                }
            }
        }
    }

}

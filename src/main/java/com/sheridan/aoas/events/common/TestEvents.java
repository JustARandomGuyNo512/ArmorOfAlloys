package com.sheridan.aoas.events.common;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.aoas.ArmorOfAlloys;
import com.sheridan.aoas.client.render.RenderTypes;
import com.sheridan.aoas.model.MeshModelData;
import com.sheridan.aoas.model.client.BufferedMeshModel;
import com.sheridan.aoas.model.gltf.io.GltfModelLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class TestEvents {
    static boolean test = false;
    static BufferedMeshModel bufferedMeshModel;

    @SubscribeEvent
    public static void testEvent(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
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
            if (bufferedMeshModel != null) {
                bufferedMeshModel.render(null, 0);
            }


        }
    }
}

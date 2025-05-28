package com.sheridan.aoas.events.common;

import com.sheridan.aoas.ArmorOfAlloys;
import com.sheridan.aoas.model.gltf.io.GltfModelLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public class TestEvents {

    @SubscribeEvent
    public static void testEvent(PlayerEvent.PlayerLoggedInEvent event) {
        System.out.printf("Test event fired");
    }
}

package com.sheridan.aoas.events.client;

import com.sheridan.aoas.ClientProxy;
import com.sheridan.aoas.compat.IrisCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@OnlyIn(Dist.CLIENT)
public class RenderEvents {
    @SubscribeEvent
    public static void onRenderTickStart(RenderFrameEvent.Pre event) {
        ClientProxy.isIrisShaderInUse = IrisCompat.isShaderPackInUse();
    }
}

package com.sheridan.aoas.mixin;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexBuffer.class)
@OnlyIn(Dist.CLIENT)
public interface VertexBufferAccessor {
    @Accessor("vertexBufferId")
    int getVertexBufferId();

    @Accessor("vertexBufferId")
    void setVertexBufferId(int id);

    @Accessor("indexCount")
    int getIndexCount();

    @Accessor("indexCount")
    void setIndexCount(int count);
}

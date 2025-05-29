package com.sheridan.aoas.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.PartPose;

import java.util.Map;

public class ModelData {
    private final Map<String, ModelData> children;
    private PartPose initialPose = PartPose.ZERO;

    public ModelData() {
        this.children = new Object2ObjectArrayMap<>();
    }
}

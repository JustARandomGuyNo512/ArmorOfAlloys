package com.sheridan.aoas.model.gltf.io;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.sheridan.aoas.ArmorOfAlloys;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

public class GltfAssetsLocator implements AssetLocator {

    @Override
    public void setRootPath(String rootPath) {}

    @Override
    public AssetInfo locate(AssetManager manager, AssetKey key) {
        ResourceLocation path = ResourceLocation.parse(key.getName());
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            Resource resource = resourceManager.getResource(path).orElse(null);
            if (resource == null) {
                System.err.println("can not find gltf/glb assets: " + path);
                return null;
            }

            InputStream stream = resource.open();

            return new AssetInfo(manager, key) {
                @Override
                public InputStream openStream() {
                    return stream;
                }
            };

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

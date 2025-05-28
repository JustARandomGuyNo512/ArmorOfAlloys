package com.sheridan.aoas.model.gltf.io;

import com.jme3.material.Material;
import com.jme3.scene.plugins.gltf.GltfLoader;

import java.lang.reflect.Field;

public class GltfLoaderProxy extends GltfLoader {

    public GltfLoaderProxy(){
        super();
        try {
            Field field = GltfLoader.class.getDeclaredField("defaultMat");
            field.setAccessible(true);
            field.set(this, new Material());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Material readMaterial(int materialIndex) {
        return new Material();
    }
}



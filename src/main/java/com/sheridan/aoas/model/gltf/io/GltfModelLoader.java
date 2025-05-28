package com.sheridan.aoas.model.gltf.io;

import com.jme3.anim.SkinningControl;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import net.minecraft.resources.ResourceLocation;

public class GltfModelLoader {
    private static AssetManager assetManager;

    private static void init() {
        assetManager = new DesktopAssetManager();
        assetManager.registerLoader(GltfLoaderProxy.class, "gltf", "glb");
        assetManager.registerLocator("", GltfAssetsLocator.class);
    }

    public static void loadModel(ResourceLocation location) {
        if (assetManager == null) {
            init();
        }
        Spatial model = assetManager.loadModel(location.toString());
        SkinningControl control = model.getControl(SkinningControl.class);
        if (control == null) {
            System.out.println("Skinning UnFound, please check your model, I only read gltf model that has skinning control bro! ^.^");
        }
        model.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry geometry) {
                Mesh mesh = geometry.getMesh();
                System.out.println("Mesh: " + spatial.getName());
                System.out.println(mesh.getVertexCount());
            }
        });
    }
}

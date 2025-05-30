package com.sheridan.aoas.model;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public interface IAnimatedModel {
    Stream<IAnimatedModel> getAllParts();

    boolean hasChild(String pName);

    IAnimatedModel getChild(String pName);

    void offsetPos(Vector3f vector3f);

    void offsetRotation(Vector3f vector3f);

    void offsetScale(Vector3f vector3f);
}

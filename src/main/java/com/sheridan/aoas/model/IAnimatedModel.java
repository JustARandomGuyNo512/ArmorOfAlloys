package com.sheridan.aoas.model;

import org.joml.Vector3f;

import java.util.stream.Stream;

public interface IAnimatedModel {
    Stream<IAnimatedModel> getAllParts();

    boolean hasChild(String pName);

    IAnimatedModel getChild(String pName);

    void offsetPos(Vector3f vector3f);

    void offsetRotation(Vector3f vector3f);

    void offsetScale(Vector3f vector3f);
}

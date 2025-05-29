package com.sheridan.aoas.model;

public class Vertex {
    public float x, y, z, u, v, normalX, normalY, normalZ;
    public int index;
    public Vertex(float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ, int index) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.index = index;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + ", " + u + ", " + v + ", " + normalX + ", " + normalY + ", " + normalZ + "] index: " + index;
    }
}

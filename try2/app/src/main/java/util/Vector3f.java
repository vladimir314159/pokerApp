package util;

import java.lang.Math;

public class Vector3f {

    public float x;
    public float y;
    public float z;

    public Vector3f() {

    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vector3f add(Vector3f a, Vector3f b) {
        Vector3f result = new Vector3f();

        result.x = a.x + b.x;
        result.y = a.y + b.y;
        result.z = a.z + b.z;

        return result;
    }

    public static Vector3f sub(Vector3f a, Vector3f b) {
        Vector3f result = new Vector3f();

        result.x = a.x - b.x;
        result.y = a.y - b.y;
        result.z = a.z - b.z;

        return result;
    }

    public static Vector3f mul(Vector3f a, float b) {
        Vector3f result = new Vector3f();

        result.x = a.x * b;
        result.y = a.y * b;
        result.z = a.z * b;

        return result;
    }

    public static Vector3f cross(Vector3f a, Vector3f b) {
        Vector3f result = new Vector3f();

        result.x = a.y * b.z - a.z * b.y ;
        result.y = a.z * b.x - a.x * b.z ;
        result.z = a.x * b.y - a.y * b.x ;

        return result;
    }

    public static Vector3f normalized(Vector3f a) {
        Vector3f result = new Vector3f();

        float length = (float)Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);

        result.x = a.x / length;
        result.y = a.y / length;
        result.z = a.z / length;

        return result;
    }
}




package myGameEngine.Helpers;

import ray.rml.Quaternion;
import ray.rml.Vector3;
import ray.rml.Vector3f;

// MathHelper is a function to make math calculations.
public class MathHelper {

    // Linear interpolate between a and b, determined by scalar.
    public static float lerp(float a, float b, double scalar) {
        return (float)(a * (1 - scalar) + b * scalar);
    }
    public static Vector3 lerp(Vector3 a, Vector3 b, float scalar) {
        return a.mult(1 - scalar).add(b.mult(scalar));
    }

    public static Quaternion slerp(Quaternion a, Quaternion b, float scalar) {
        Quaternion q = a.slerp(b, scalar);
        if (Float.isNaN(q.w()) || Float.isNaN(q.x()) || Float.isNaN(q.y()) || Float.isNaN(q.z())) {
            return a.lerp(b, scalar);
        } else {
            return q;
        }
    }
}

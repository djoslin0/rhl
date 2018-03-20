package myGameEngine.Helpers;

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
}

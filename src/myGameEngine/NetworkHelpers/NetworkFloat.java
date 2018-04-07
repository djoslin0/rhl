package myGameEngine.NetworkHelpers;

public class NetworkFloat {
    public static short encode(float f) { return (short)(f * 100); }
    public static float decode(short s) { return s / 100f; }
}

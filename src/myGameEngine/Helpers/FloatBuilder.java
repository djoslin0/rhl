package myGameEngine.Helpers;

// the FloatBuilder class simply builds up a float array
public class FloatBuilder
{
    public float[] array;
    private int index = 0;

    public FloatBuilder(int size) {
        array = new float[size];
    }

    public void add(float a) {
        array[index++] = a;
    }
    public void add(float a, float b) {
        array[index++] = a;
        array[index++] = b;
    }

    public void add(float a, float b, float c) {
        array[index++] = a;
        array[index++] = b;
        array[index++] = c;
    }
}

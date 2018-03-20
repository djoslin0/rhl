package myGameEngine.Helpers;

// the FloatBuilder class simply builds up an int array
public class IntBuilder {
    public int[] array;
    private int index = 0;

    public IntBuilder(int size) {
        array = new int[size];
    }

    public void add(int a) {
        array[index++] = a;
    }
    public void add(int a, int b) {
        array[index++] = a;
        array[index++] = b;
    }

    public void add(int a, int b, int c) {
        array[index++] = a;
        array[index++] = b;
        array[index++] = c;
    }
}

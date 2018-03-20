package myGameEngine.Singletons;

// The UniqueCounter is responsible for allowing dynamic lists of entities to have a unique name
public class UniqueCounter {
    private static final UniqueCounter instance = new UniqueCounter();
    private long counter;
    private UniqueCounter(){}
    public static long next(){
        return instance.counter++;
    }
}

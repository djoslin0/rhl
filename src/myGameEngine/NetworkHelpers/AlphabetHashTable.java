package myGameEngine.NetworkHelpers;

import java.util.Hashtable;

public class AlphabetHashTable extends Hashtable<String,Integer> {
    public AlphabetHashTable() {
        super();
        setupTable();
    }
    private void setupTable() {
        this.put("A", 0);
        this.put("B", 1);
        this.put("C", 2);
        this.put("D", 3);
        this.put("E", 4);
        this.put("F", 5);
        this.put("G", 6);
        this.put("H", 7);
        this.put("I", 8);
        this.put("J", 9);
        this.put("K", 10);
        this.put("L", 11);
        this.put("M", 12);
        this.put("N", 13);
        this.put("O", 14);
        this.put("P", 15);
        this.put("Q", 16);
        this.put("R", 17);
        this.put("S", 18);
        this.put("T", 19);
        this.put("U", 20);
        this.put("V", 21);
        this.put("W", 22);
        this.put("X", 23);
        this.put("Y", 24);
        this.put("Z", 25);
        this.put("join", 26);
        this.put("getPlayers", 27);
        this.put("Success", 28);
        this.put("Players", 29);
    }
}

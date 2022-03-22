package src;

import java.io.IOException;
import java.util.Scanner;

public class MapIO {
    public static void main(String[] args) {
        Map myMap = new Map();

        System.out.println(myMap);

        //for testing Purposes
        if (myMap.exportMap("exportedMap.txt")) System.err.println("Map wasn't exported correctly");
    }
}

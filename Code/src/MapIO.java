package src;

import java.io.IOException;
import java.util.Scanner;

public class MapIO {
    public static void main(String[] args) {
        Map myMap = new Map();

        System.out.println(myMap);

        //for testing Purposes
        try {
            myMap.exportMap("exportedMap.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

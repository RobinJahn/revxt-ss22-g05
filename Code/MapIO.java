import java.io.IOException;
import java.util.Scanner;

public class MapIO {
    public static void main(String[] args) {
        Map myMap = new Map();
        Scanner sc = new Scanner(System.in);
        String mapPath;

        System.out.println("Enter Path and Name where the Map is stored");
        mapPath = sc.nextLine();
        mapPath = mapPath.replace("\"", "");

        myMap.importMap(mapPath);
        myMap.print();

        try {
            myMap.exportMap("exportedMap.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

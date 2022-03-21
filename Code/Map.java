import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Map {
    private char[][] map; //main data structure to store the Map Infos

    //Data Structure and needed Variables to store Transitions
    ArrayList<char[]> transitions = new ArrayList<>(); //TODO: austauschen durch hash map
    int[] transitionsBuffer = new int[3];
    private boolean isFirst = true;

    //General Map Infos
    private int anzPlayers;
    private int anzOverwriteStones;
    private int anzBombsAtStrat;
    private int explosionRadius;
    private int height;
    private int width;

    //public Methods

    /**
     * @param x x corrdinate
     * @param y y coordinate
     * @return Returns the Character at the given x and y position in the Map.
     */
    public char getCharAt(int x, int y){
        return map[y][x];
    }

    /**
     * Method Imports a Map from the given File Name.
     * @param fileName Path and Name of the file to import.
     * @return Returns true if map was imported correctly and false otherwise.
     */
    public boolean importMap(String fileName) {
        //Variables
        FileReader fr = null;
        BufferedReader br = null;
        StreamTokenizer st = null;
        int tokenCounter = 0;
        int x = 0, y = 0;
        boolean readTransitionsNow = false;

        //Set up File reader
        try {
            fr = new FileReader(fileName);
        } catch (FileNotFoundException e) {
            System.err.println("File with the Name: " + fileName + " couldn't be found");
            return false;
        }
        br = new BufferedReader(fr);
        st = new StreamTokenizer(br);
        st.whitespaceChars(' ', ' ');
        st.wordChars('-','-');

        //read file
        tokenCounter = 0; //counts with which token it is currently at
        while (true){
            //cantch end of file and other exceptions
            try {
                if (st.nextToken() == StreamTokenizer.TT_EOF) break;
            } catch (IOException e) {
                e.printStackTrace(); //prints error to stderr
                return false;
            }
            //handle read token
            //TODO: handle false Tokens
            if (tokenCounter < 6) handleFirst5(st, tokenCounter);
            else if (tokenCounter < (width*height)+6) handleMap(st, tokenCounter);
            else handleTransitions(st, tokenCounter);

            tokenCounter++;
        }
        //TODO: check wether map was imported correctly
        return true;
    }

    //for testing purposes
    public boolean exportMap(String fileName) throws IOException {
        File newFile;
        FileWriter fw;

        newFile = new File(fileName);
        newFile.createNewFile();

        fw = new FileWriter(fileName, false);

        //write infos
        fw.write(((Integer)anzPlayers).toString() + '\r'+'\n');
        fw.write(((Integer)anzOverwriteStones).toString() + '\n'+'\r');
        fw.write( ((Integer)anzBombsAtStrat).toString() + ' ' + ((Integer)explosionRadius).toString() + '\n');
        fw.write( ((Integer)height).toString() + ' ' + ((Integer)width).toString() + '\n');

        //write map
        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                fw.write(((Character)getCharAt(x,y)).toString() + ' ');
            }
            fw.write('\r');
        }

        //write transitions
        for (char[] pair : transitions){
            int x1 = Transitions.getX(pair[0]);
            int y1 = Transitions.getY(pair[0]);
            int r1 = Transitions.getR(pair[0]);

            int x2 = Transitions.getX(pair[1]);
            int y2 = Transitions.getY(pair[1]);
            int r2 = Transitions.getR(pair[1]);

            fw.write(x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2 + '\r' + '\n');
        }

        fw.close();

        return true;
    }

    /**
     * Prints Infos, Map and Transitions in the std.out.
     */
    public void print() {
        System.out.println("Player count: " + anzPlayers);
        System.out.println("Overwrite-stones count: " + anzOverwriteStones);
        System.out.println("Bomb count: " + anzBombsAtStrat + ", Explosion radius: " + explosionRadius);
        System.out.println("Height: " + height + ", Width: " + width);

        System.out.println(Arrays.deepToString(map).replaceAll("],","]\n"));

        for (char[] pair : transitions){
            int x1 = Transitions.getX(pair[0]);
            int y1 = Transitions.getY(pair[0]);
            int r1 = Transitions.getR(pair[0]);

            int x2 = Transitions.getX(pair[1]);
            int y2 = Transitions.getY(pair[1]);
            int r2 = Transitions.getR(pair[1]);

            System.out.println(x1 + " " + y1 + " " + r1 + " <-> " + x2 + " " + y2 + " " + r2);
        }
    }

    //private Methodes

    /**
     * Sets given Char at the given x and y position in the Map.
     * Handles if Map wasn't initialized.
     * @param y y coordinate
     * @param x x corrdinate
     * @param charToChangeTo character to set at the given position
     * @return returns true if char was set correctly
     */
    private boolean setCharAt(int x, int y, char charToChangeTo){
        if (map == null) {
            System.err.println("The Map wasn't yet initialized");
            return false;
        }
        map[y][x] = charToChangeTo;
        return true;
    }

    /**
     * In progress...
     * @param st Stream Tokenizer to read the Char out of
     * @return Returns the current Token as a char
     */
    private char getCharFromStreamTokenizer(StreamTokenizer st){
        char result = 0;
        if (st.ttype == StreamTokenizer.TT_WORD) result = st.sval.charAt(0);
        if (st.ttype == StreamTokenizer.TT_NUMBER) result = ((Integer)((Double)st.nval).intValue()).toString().charAt(0); //TODO: check if all that is neccessarry
        if (st.ttype == 45) result = '-'; //TODO: do that right

        return result;
    }

    private void handleFirst5(StreamTokenizer st, int tokenCounter) {
        if (tokenCounter < 0 || tokenCounter > 5) {
            System.err.println("In Map.handleFirst5() the tokenCounter had an invalid Value");
            return;
        }
        switch (tokenCounter) {
            case 0:
                anzPlayers = ((Double)st.nval).intValue();
                break;
            case 1:
                anzOverwriteStones = ((Double)st.nval).intValue();
                break;
            case 2:
                anzBombsAtStrat = ((Double)st.nval).intValue();
                break;
            case 3:
                explosionRadius = ((Double)st.nval).intValue();
                break;
            case 4:
                height = ((Double)st.nval).intValue();
                break;
            case 5:
                width = ((Double)st.nval).intValue();
                map = new char[height][width];
                break;
        }
    }

    private void handleMap(StreamTokenizer st, int tokenCounter) {
        //calculates x and y coordinates out of token counter, width and height
        int x = (tokenCounter-6)%width;
        int y = (tokenCounter-6)/width;
        //save char in map
        if (st.ttype == StreamTokenizer.TT_WORD) {
            setCharAt(x, y, st.sval.charAt(0));
        }
        if (st.ttype == StreamTokenizer.TT_NUMBER) {
            setCharAt(x, y, ((Integer)((Double)st.nval).intValue()).toString().charAt(0)); //TODO: check if all that is neccessarry
        }
        if (st.ttype == 45) { //TODO: do that right
            setCharAt(x, y, '-');
        }
    }

    private void handleTransitions(StreamTokenizer st, int tokenCounter) {
        if (st.ttype != StreamTokenizer.TT_NUMBER) return;

        int posInTransitionBuffer = (tokenCounter - (width*height)+6) % 3; //TODO: übersprint <-> sind  3 - besser machen
        char buffer;

        transitionsBuffer[posInTransitionBuffer] = ((Double)st.nval).intValue(); //saves the 3 values of one transition end in the buffer

        //if one transition end is complete
        if (posInTransitionBuffer == 2) {
            //convert transition infos into a char
            buffer = Transitions.saveInChar(transitionsBuffer[0],transitionsBuffer[1],transitionsBuffer[2]);
            //Checks if transition end is the first or second one
            if (isFirst) { //if it's the first one it creates a new transition pair and saves it in the transition List
                char[] pair = new char[2];
                pair[0] = buffer;
                transitions.add(pair);
            }
            else { //If it's the second one it adds the second transition end to the pair in the transition List
                transitions.get(transitions.size()-1)[1] = buffer;
            }

            //toggle if transition is the first end or the second one
            isFirst = !isFirst;
        }
    }

}

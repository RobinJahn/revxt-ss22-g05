package src;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * This class is used as an Interface to the server. It provides all the functionality to communicate with the server.
 */
public class ServerMessenger {
    private final OutputStream out;
    private final InputStream in;
    private final int groupNumberAddition;

    /**
     * Constructs the Server Messenger
     * @param ip Target IP-Address
     * @param port Target Port-Number
     * @param groupNumberAddition GroupNumberAddition
     * @throws IOException Throws an IOException If no connection could be established
     */
    public ServerMessenger(String ip, int port, int groupNumberAddition) throws IOException {
        Socket server = new Socket(ip, port); //builds up a connection to the server socket
        // Get input and output stream:
        out = server.getOutputStream();
        in = server.getInputStream();

        this.groupNumberAddition = groupNumberAddition;

        out.write(assembleMassage(1,null));
    }

    //receiver

    /**
     * Waits for Messages from the Server.
     * @return Returns -1 if there was no Message, Otherwise the received Byte.
     */
    public int waitForMessage(){
        int readByte;
        try {
            readByte = in.read();
            return readByte;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Reads the Rest of the MoveRequest
     * @return Returns the Maximum Time and Depth for this Move
     */
    public int[] readRestOfMoveRequest(){
        int time;
        int depth;
        DataInputStream dis = new DataInputStream(in);
        try {
            in.readNBytes(4); //reads size and ignores it

            time = dis.readInt(); // is signed but in message it's unsigned
            if (time < 0) {
                System.err.println("Got to big value for time - overflow");
                time = Integer.MAX_VALUE;
            }
            depth = dis.readUnsignedByte();

            return new int[]{time, depth};
        }
        catch (IOException e) {
            e.printStackTrace();
            return new int[]{-1, -1};
        }
    }

    /**
     * Reads the Rest of the Move send by the Server
     * @return Returns the Position and Additional Info of the Move and the PlayerNumber, which made the Move.
     */
    public int[] readRestOfMove(){

        DataInputStream dis = new DataInputStream(in);
        int[] result = new int[4];

        try {
            in.readNBytes(4); //reads size and ignores it

            result[0] = dis.readShort(); // x
            result[1] = dis.readShort(); // y
            result[2] = dis.readUnsignedByte(); //extra info
            result[3] = dis.readUnsignedByte(); //player
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    /**
     * Reads the Rest of the Disqualification
     * @return Returns the Number of the Disqualified Player.
     */
    public int readRestOfDisqualification(){
        int player;
        DataInputStream dis = new DataInputStream(in);
        try {
            in.readNBytes(4); //reads size and ignores it
            player = dis.readUnsignedByte();
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return player;
    }

    /**
     * Reads the Rest and ignores it.
     */
    public void readRestOfNextPhase(){
        try {
            in.readNBytes(4); //reads size and ignores it
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the Input Stream and Returns the Map.
     * @param serverLog Boolean to toggle ServerLog with.
     * @return Returns the Map, send by the server.
     */
    public StaticMap getMap(boolean serverLog){
        int readByte;
        byte[] readByteArray;
        StringBuilder lengthStr = new StringBuilder();
        int length;
        StaticMap sMap;

        try {
            //check if it's the right type of message
            readByte = in.read();
            if (readByte != 2) return null;

            //read the length of the map
            readByteArray = in.readNBytes(4);
            for (byte aByte : readByteArray){
                lengthStr.append(String.format("%02x",aByte));
            }
            //parse the length from hex to decimal
            length = Integer.parseInt(lengthStr.toString(),16);

            //read the whole map sting
            readByteArray = in.readNBytes(length);
            //create a map with it
            sMap = new StaticMap(readByteArray, serverLog);

        }
        catch (SocketException e){
            System.err.println("Server has reset the connection");
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return sMap;
    }

    /**
     * Reads the Input Stream and Returns my Player Number.
     * @return Returns my Player Number.
     */
    public int getPlayerNumber() {
        int readByte;

        try {
            //check if it's the right type of message
            readByte = in.read();
            if (readByte != 3) return -1;

            in.readNBytes(4); //reads size and ignores it

            //reads send Player Number
            readByte = in.read();
            return readByte;

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    //sender

    /**
     * Sends a Move to the Server with the given Parameters
     * @param x X-Position of the Move
     * @param y Y-Position of the Move
     * @param additionalInfo Additional Info
     * @param myPlayerNr My Player Number
     */
    public void sendMove(int x, int y, int additionalInfo, int myPlayerNr){
        int[] arguments = new int[4];

        if (additionalInfo < 0 || additionalInfo > 21 || (additionalInfo > 8 && additionalInfo < 20)) {
            System.err.println("Move couldn't be sent because additional Info wasn't between 0-8 or 20 or 21. It was " + additionalInfo);
            return;
        }

        arguments[0] = x-1; //index shift
        arguments[1] = y-1;
        arguments[2] = additionalInfo;
        arguments[3] = myPlayerNr;
        byte[] message = assembleMassage(5,arguments);
        try {
            out.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //helper

    private byte[] assembleMassage(int messageType, int[] arguments){
        byte[] message = null;
        ByteBuffer messageBuffer;

        switch (messageType){
            case 1:
                messageBuffer = ByteBuffer.allocate(6);
                messageBuffer.put((byte)1); //MessageType
                messageBuffer.putInt(1); //length of message
                //GroupNumber
                if (groupNumberAddition == -1) messageBuffer.put((byte) 5 );
                else messageBuffer.put((byte)(50+groupNumberAddition));

                message = messageBuffer.array();
                break;
            case 5:
                messageBuffer = ByteBuffer.allocate(10);
                messageBuffer.put((byte)5); //MessageType
                messageBuffer.putInt(5); //length of message
                messageBuffer.putShort(Short.parseShort(Integer.toString(arguments[0]))); //x
                messageBuffer.putShort(Short.parseShort(Integer.toString(arguments[1]))); //y
                messageBuffer.put(Byte.parseByte(Integer.toString(arguments[2]))); //extra info
                message = messageBuffer.array();
                break;
        }

        return message;
    }

}

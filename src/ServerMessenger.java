package src;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class ServerMessenger {
    Socket server;
    OutputStream out;
    InputStream in;
    int groupNumberAddition;

    public ServerMessenger(String ip, int port, int groupNumberAddition) throws IOException {
        server = new Socket( ip, port ); //builds up a connection to the server socket
        // Get input and output stream:
        out = server.getOutputStream();
        in = server.getInputStream();

        this.groupNumberAddition = groupNumberAddition;

        out.write(assembleMassage(1,null));
    }

    //receiver

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

    public int readRestOfDisqualification(){
        int player = -1;
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

    public void readRestOfNextPhase(){
        try {
            in.readNBytes(4); //reads size and ignores it
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StaticMap getMap(){
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
            sMap = new StaticMap(readByteArray);

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
                message = new byte[6];
                messageBuffer = ByteBuffer.allocate(6);
                messageBuffer.put((byte)1); //MessageType
                messageBuffer.putInt(1); //length of message
                //GroupNumber
                if (groupNumberAddition == -1) messageBuffer.put((byte) 5 );
                else messageBuffer.put((byte)(50+groupNumberAddition));

                message = messageBuffer.array();
                break;
            case 5:
                message = new byte[10];
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

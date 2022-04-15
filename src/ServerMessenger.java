package src;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ServerMessenger {
    Socket server;
    OutputStream out;
    InputStream in;

    public ServerMessenger(String ip, int port) throws IOException {
        server = new Socket( ip, port ); //builds up a connection to the server socket
        // Get input and output stream:
        out = server.getOutputStream();
        in = server.getInputStream();

        out.write(assembleMassage(1,null));
    }

    //receiver

    public int waitForMessage(){
        int readByte;
        DataInputStream dis = new DataInputStream(in);
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

            time = dis.readInt(); //TODO: unsigned?
            depth = dis.readUnsignedByte();

            //TODO: if time is set ignore depth
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

    public Map getMap(){
        int readByte;
        byte[] readByteArray;
        StringBuilder lengthStr = new StringBuilder();
        int length;
        Map map;

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
            map = new Map(readByteArray);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return map;
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

    public void sendMove(int x, int y, char zusatzinfo, int myPlayerNr){
        int[] arguments = new int[4];
        arguments[0] = x-1; //index shift
        arguments[1] = y-1;
        if (Character.isDigit(zusatzinfo)) arguments[2] = zusatzinfo-'0';
        else {
            if (zusatzinfo == 'b') arguments[2] = 20;
            if (zusatzinfo == 'o') arguments[2] = 21;
        }
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
                messageBuffer.put((byte)1); //nachrichtentyp
                messageBuffer.putInt(1); //length of message
                messageBuffer.put((byte)5); //gruppennummer
                message = messageBuffer.array();
                break;
            case 5:
                message = new byte[10];
                messageBuffer = ByteBuffer.allocate(10);
                messageBuffer.put((byte)5); //nachrichtentyp
                messageBuffer.putInt(5); //length of message //TODO: IN HEX
                messageBuffer.putShort(Short.parseShort(Integer.toString(arguments[0]))); //x
                messageBuffer.putShort(Short.parseShort(Integer.toString(arguments[1]))); //y
                messageBuffer.put(Byte.parseByte(Integer.toString(arguments[2]))); //extra info
                message = messageBuffer.array();
                break;
        }

        return message;
    }

}

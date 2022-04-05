package src;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ServerMessager {
    Socket server;
    OutputStream out;
    InputStream in;

    public ServerMessager(String ip, int port) throws IOException {
        server = new Socket( ip, port ); //builds up a connection to the server socket
        // Get input and output stream:
        out = server.getOutputStream();
        in = server.getInputStream();

        out.write(assembleMassage(1,null));
    }

    public void waitForTurnMessage(){ //doesn't work - just for demonstration
        try {
            in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMove(int x, int y, char zusatzinfo){
        int[] arguments = new int[3];
        arguments[0] = x;
        arguments[1] = y;
        if (Character.isDigit(zusatzinfo)) arguments[2] = Integer.parseInt("" + zusatzinfo);
        else {
            if (zusatzinfo == 'b') arguments[2] = 20;
            if (zusatzinfo == 'c') arguments[2] = 21;
        }
        byte[] message = assembleMassage(5,arguments);
        try {
            out.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] assembleMassage(int messageType, int[] arguments){
        byte[] message = null;
        ByteBuffer messageBuffer;

        switch (messageType){
            case 1:
                message = new byte[6];
                messageBuffer = ByteBuffer.allocate(6);
                messageBuffer.put((byte)1); //nachrichtentyp
                messageBuffer.putInt(1); //l�nge der nachricht
                messageBuffer.put((byte)5); //gruppennummer
                message = messageBuffer.array();
                break;
            case 5:
                message = new byte[10];
                messageBuffer = ByteBuffer.allocate(10);
                messageBuffer.put((byte)5); //nachrichtentyp
                messageBuffer.putInt(5); //l�nge der nachricht
                messageBuffer.putShort(Short.parseShort(Integer.toString(arguments[0]))); //x
                messageBuffer.putShort(Short.parseShort(Integer.toString(arguments[1]))); //y
                messageBuffer.put(Byte.parseByte(Integer.toString(arguments[2]))); //extra info
                message = messageBuffer.array();
                break;
        }

        return message;
    }


}

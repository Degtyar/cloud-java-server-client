import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Network {

    private static final int MAX_OBJ_SIZE = 1024 * 1024 * 100;
    private Socket socket;
    private ObjectEncoderOutputStream out;
    private ObjectDecoderInputStream in;

    public static Network instance = new Network();

    private Network() {
    }

    public static Network getInstance() {
        return instance;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    public void connect(InetAddress address, int port) throws Exception {
        socket = new Socket(address,port);
        in = new ObjectDecoderInputStream(socket.getInputStream(),MAX_OBJ_SIZE);
        out = new ObjectEncoderOutputStream(socket.getOutputStream());

    }

    public void sendObject (AbsMsg msg) throws Exception {
        out.writeObject(msg);
    }

    public Object readObject () throws IOException, ClassNotFoundException {
        return in.readObject();
    }
}

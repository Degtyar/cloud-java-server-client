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

    /**
     * @param address
     * @param port
     * @throws Exception
     * Метод подключения к серверу
     */
    public void connect(InetAddress address, int port) throws Exception {
        socket = new Socket(address,port);
        in = new ObjectDecoderInputStream(socket.getInputStream(),MAX_OBJ_SIZE);
        out = new ObjectEncoderOutputStream(socket.getOutputStream());

    }

    /**
     * @param msg
     * @throws Exception
     * Метод отправки сообщений
     */
    public void sendObject (AbsMsg msg) throws Exception {
        out.writeObject(msg);
        out.flush();
    }

    /**
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * Метод чтения сообщений
     */
    public Object readObject () throws IOException, ClassNotFoundException {
        return in.readObject();
    }

    /**
     * @throws Exception
     * Метод закрытия соединений
     */
    public void disconnect () throws Exception {
        socket.close();
    }
}

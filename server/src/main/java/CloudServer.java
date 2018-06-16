import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;


public class CloudServer {

    private PropertyRead propertyRead = new PropertyRead("./server/src/main/java/server.properties");
    private int port;
    private int max_obj_size;
    private String rootDirUser;


    /**
     * В конструктаре читаем параметры из файла свойств
     */
    public CloudServer() {
        this.port = Integer.parseInt(propertyRead.getProperty("PORT"));
        this.max_obj_size = Integer.parseInt(propertyRead.getProperty("MAX_OBJ_SIZE"));
        this.rootDirUser = propertyRead.getProperty("rootDirUser");
    }

    public void run() throws Exception {
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();

            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(max_obj_size, ClassResolvers.cacheDisabled(null)),
                                    new CloudServerHandler(rootDirUser)
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(port).sync();
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            AuthService.disconnect();
        }
    }

}

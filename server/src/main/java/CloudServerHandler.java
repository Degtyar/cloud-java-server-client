import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {

    public static final int SIZE_SET = 512000; // должен совпадать на клиенте и сервере
    private String rootDir;
    private UserCloud user;
    private Boolean authorization = false;

    public CloudServerHandler(String rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected...");
        ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;
            controllerMsg(ctx, msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        authorization = false;
        user.setIsAuth(false);
        ctx.write(user);
        ctx.flush();
        ctx.write(new StatusInfo("Server disconnect"));
        ctx.flush();
        ctx.close();
    }

    /**
     * @param ctx
     * @param msg
     * @throws Exception
     * Метод обработки сообщений
     */
    private void controllerMsg (ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (((AbsMsg) msg).getTypeMsg()) {
            case "user" :
                user = (UserCloud) msg;
                if(user.isCreate()) {
                    if (createUserResource(user)){
                        ctx.write(new StatusInfo("Resource create"));
                    } else {
                        ctx.write(new StatusInfo("Error resource create"));
                    }
                }
                authorization = AuthService.tryAuth(user) != null &&
                                AuthService.tryAuth(user).equals(user.getLogin());
                if(authorization) {
                    ctx.write(user);
                    ctx.flush();
                    ctx.write(getUserFile());
                    ctx.flush();
                } else {
                    ctx.write(new StatusInfo("Password or login invalid"));
                }
                break;
            case "sync":
                if(!authorization) break; //добавил как псевдо защита
                writeFile(ctx, msg);
                ctx.flush();
                break;
            case "fileList":
                if(!authorization) break;
                FileList fileList = (FileList) msg;
                if(fileList.getTypeRequest() == FileList.TypeRequest.delete) {
                    delFile(ctx, fileList);
                    ctx.write(getUserFile());
                    ctx.flush();
                } else if (fileList.getTypeRequest() == FileList.TypeRequest.copy) {
                    copyFile(ctx, fileList);
                }
                break;
            case "info":
                StatusInfo statusInfo = (StatusInfo) msg;
                if (statusInfo.getMassage().equals("reset_connect")){
                    ctx.write(new StatusInfo("Server disconnect"));
                    ctx.flush();
                    ctx.channel().close();
                }

        }
    }

    /**
     * @param ctx
     * @param msg
     * Метод записи полученых файлов от пользователей
     */
    public void writeFile (ChannelHandlerContext ctx, Object msg) {
        FileMsg fileMsg = (FileMsg) msg;
        String folder = rootDir + "/" + user.getLogin();
        String name = fileMsg.getName();
        int size = fileMsg.getSize();
        int set = fileMsg.getSet();
        byte[] data = fileMsg.getData();
        System.out.println(folder  + "/" + name);
        try(RandomAccessFile file = new RandomAccessFile(folder  + "/" + name, "rw")){
            file.seek(set * SIZE_SET);
            file.write(data);
            if(set == size) {
                ctx.write(getUserFile());
                ctx.flush();
                ctx.write(new StatusInfo("copy: " + name));
                ctx.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param ctx
     * @param fileList
     * @throws Exception
     * Метод удаления файлов по запросу пользователя
     */
    private void delFile (ChannelHandlerContext ctx, FileList fileList) throws Exception {
        for (String fileName : fileList.getListFile()) {
            String fullFileName = rootDir + "/" + user.getLogin() + "/" + fileName;
            System.out.println(fullFileName);
            File file = new File(fullFileName);
            if (file.delete()) {
                ctx.write( new StatusInfo("File delete ок :" + fileName));
                ctx.flush();
            }else {
                ctx.write( new StatusInfo("File delete error :" + fileName));
                ctx.flush();
            }
        }
    }

    /**
     * @param ctx
     * @param fileList
     * @throws Exception\
     * Метод предачи заданных файлов в сторону пользователя
     */
    public void copyFile(ChannelHandlerContext ctx, FileList fileList) throws Exception {
        FileMsg file;
        byte[] data;
        int size, set;
        String currentFolder = rootDir + "/" + user.getLogin();
        for (String nameFile : fileList.getListFile()){
            Path fileLocation = Paths.get(currentFolder + "/"+ nameFile);
            data = Files.readAllBytes(fileLocation);
            size = (int) Math.ceil((float) data.length / SIZE_SET);
            for (set = 0; set < size; set++){
                int startCopy = set * SIZE_SET;
                int endCopy = (startCopy + SIZE_SET) > data.length ? data.length : startCopy + SIZE_SET;
                file = new FileMsg(nameFile, Arrays.copyOfRange(data, startCopy, endCopy),set ,size - 1);
                ctx.write(file);
                ctx.flush();
            }
        }
    }

    /**
     * @return
     * @throws Exception
     * Метод получения списка файлов пользователя
     */
    private FileList getUserFile() throws Exception {
        List<String> result;
        Path paths = Paths.get(rootDir + "/" + user.getLogin() );
        DirectoryStream<Path> stream = Files.newDirectoryStream(paths, path -> path.toFile().isFile());
        result = CommonClass.pathToList(stream);
        stream.close();
        return new FileList(result);
    }

    /**
     * @param user
     * @return
     * Метод создания ресурсов для нового пользователя
     */
    private boolean createUserResource(UserCloud user) {
        try{
            if(AuthService.createUser(user)) {
                File fileDirUser = new File(rootDir + user.getLogin());
                if (fileDirUser.mkdir()) return true;
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}

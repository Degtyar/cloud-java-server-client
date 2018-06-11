import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {

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
        ctx.close();
    }

    private void controllerMsg (ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (((AbsMsg) msg).getTypeMsg()) {
            case "user" :
                user = (UserCloud) msg;
                if(user.isCreare()) {
                    if (createUserResource(user)){
                        ctx.write(new StatusInfo("Resource create"));
                    } else {
                        ctx.write(new StatusInfo("Error resource create"));
                    }
                }
                authorization = AuthService.tryAuth(user).equals(user.getLogin());
                if(authorization) {
                    ctx.write(user);
                    ctx.flush();
                    ctx.write(getUserFile());
                    ctx.flush();
                } else {
                    ctx.write(new StatusInfo("Password or login invalide \n"));
                }
                break;
            case "sync":
                if(!authorization) break;
                ctx.write(writeFile(msg));
                ctx.flush();
                ctx.write(getUserFile());
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

        }
    }

    private StatusInfo writeFile (Object msg) throws Exception{
        FileMsg fileMsg = (FileMsg) msg;
        File dir = new File(rootDir + "/" + user.getLogin() );
        dir.mkdir();
        System.out.println(dir);
        File file = new File(dir + "/" + fileMsg.getName());
        System.out.println(file);
        file.createNewFile();
        FileOutputStream stream = new FileOutputStream(file,false);
        try {
            stream.write(fileMsg.getData());
        } catch (Exception e) {
            return new StatusInfo("error: " + fileMsg.getName() + "\n");
        }
            finally {
            stream.close();
        }
        return new StatusInfo("copy: " + fileMsg.getName() + "\n");
    }

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

    private void copyFile (ChannelHandlerContext ctx, FileList fileList){
        for (String fileName : fileList.getListFile()) {
            String carentFolder = rootDir + "/" + user.getLogin();
            try {
                FileMsg file = new FileMsg(carentFolder, fileName, AbsMsg.TypeMsg.sync);
                ctx.write(file);
                ctx.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private FileList getUserFile() throws Exception {
        List<String> result;
        Path paths = Paths.get(rootDir + "/" + user.getLogin() );
        DirectoryStream<Path> stream = Files.newDirectoryStream(paths, path -> path.toFile().isFile());
        result = CommonClass.pathToList(stream);
        return new FileList(result);
    }

    private boolean createUserResource(UserCloud user) {
        try{
            AuthService.createUser(user);
            File fileDirUser = new File(rootDir + user.getLogin());
            if (fileDirUser.mkdir()) return true;
            return false;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }
}


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * Класс обработки потока событий на клиенте
 */
public class Controller implements Initializable {
    public final int SIZE_SET = 512000;
    public PasswordField passField;
    public ListView<String> fileListServer;
    public ListView<String>fileListClient;
    public TextField loginField, pathField, serverField, portField;
    public TextArea statInfo;
    public Button onConnect, serverSync, openFolder, login;
    public ProgressBar progressBar;
    public CheckBox chkCreate;
    public Label labelInfo;

    private int serverPort;
    private InetAddress ipAddress;
    private UserCloud user;
    private String currentFolder;
    private PropertyRead propertyRead = new PropertyRead("./client/src/main/java/user.properties");

    /**
     * Функуия заполнения полей на клиенте
     */
    public void setField () {
       loginField.setText(propertyRead.getProperty("login"));
       passField.setText(propertyRead.getProperty("password"));
       pathField.setText(propertyRead.getProperty("rootDirectory"));
       serverField.setText(propertyRead.getProperty("serverIpAddress"));
       portField.setText(propertyRead.getProperty("serverPort"));
    }

    /**
     * Метод сохранения полей на клиенте
     */
    public void saveProperty () {
        propertyRead.setProperty("login",loginField.getText());
        propertyRead.setProperty("password",passField.getText());
        propertyRead.setProperty("rootDirectory",pathField.getText());
        propertyRead.setProperty("serverIpAddress",serverField.getText());
        propertyRead.setProperty("serverPort",portField.getText());
    }

    /**
     * Метод определения текущей деректории синхронзации
     */
    public void setCurrentFolder() {
        this.currentFolder = pathField.getText();
    }


    /**
     * @param trigger
     * Метод доступа к элементам управления блока клиента
     */

    private void GuiClient(Boolean trigger) {
        pathField.setDisable(trigger);
        serverSync.setDisable(trigger);
        openFolder.setDisable(trigger);
    }


    /**
     * @param trigger
     * Метод открытия кнопки создания клиента
     */
    private void buttonLoginVisible(Boolean trigger){
        login.setVisible(trigger);
        chkCreate.setVisible(trigger);
    }


    /**
     * @param trigger
     * Метод управления элементами подключения к серверу
     */
    private void GuiConnect (Boolean trigger) {
        loginField.setDisable(trigger);
        passField.setDisable(trigger);
        serverField.setDisable(trigger);
        portField.setDisable(trigger);

    }


    /**
     * @throws Exception
     * Метод чтения информации из папки
     */
    public void openFolder () throws Exception {
        setCurrentFolder();
        readClientFolder();
    }


    /**
     * Метод чтения содержимого папки пользователя
     */
    public void readClientFolder () {
        Platform.runLater( () -> {
            try {
                List<String> result;
                setCurrentFolder();
                Path paths = Paths.get(currentFolder);
                DirectoryStream<Path> stream = Files.newDirectoryStream(paths, path -> path.toFile().isFile());
                result = CommonClass.pathToList(stream);
                fileListClient.getItems().clear();
                ObservableList<String> list = FXCollections.observableArrayList(result);
                fileListClient.setItems(list);
                list.add("sgsfgsfdg");
            } catch (IOException e) {
                e.printStackTrace();
                infoMassage("Error read client folder");
            }
        });
    }


    /**
     * @return
     * Метод создания обекта пользователя на соновании логина и правлоя
     */
    private boolean setUser() {
        if(!loginField.getText().equals("") && !passField.getText().equals("")){
            user = new UserCloud(loginField.getText(), passField.getText());
            return true;
        }else {
            infoMassage("Error Login and password incorrect ");
            return false;
        }
    }

    /**
     * Метод задания параментов подключения к серверу
     */
    private void setParamConnect (){
        serverPort = Integer.parseInt(portField.getText());
        try {
            String address = serverField.getText();
            ipAddress = InetAddress.getByName(address);
        } catch (Exception e) {
            e.printStackTrace();
            infoMassage("Ip address not correct");
        }

    }

    /**
     * @throws Exception
     * Метод обработки кнопки подключения к серверу
     */
    public void tryConnect() throws Exception{
        connect();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(()-> {
            fileListClient.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            fileListServer.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            GuiClient(true);
            buttonLoginVisible(false);
            statInfo.setEditable(false);
            setField();
            setContextMenuClient();
            setContextMenuServer();
            progressBar.setVisible(false);
            labelInfo.setVisible(false);
            readClientFolder ();
            fileListClient.setEditable(true);
        });
    }

    /**
     * @throws Exception
     * Метод создания соедниения к серверу
     */
    private void connect() throws Exception {
        Thread thread = new Thread(() -> {
            try {
                while (Network.getInstance().isConnected() && !Thread.currentThread().isInterrupted()) {
                    Object msg = Network.getInstance().readObject();
                    if (msg instanceof AbsMsg) {
                        controllerMsg(msg);
                    }
                    Platform.runLater(() -> {
                        if (user.isAuth()) {
                            // Изменнение интерфейса
                            GuiClient(false);
                            GuiConnect(true);
                            buttonLoginVisible(false);
                        } else {
                            // Изменнение интерфейса
                            GuiClient(true);
                            GuiConnect(false);
                            buttonLoginVisible(true);
                        }
                    });
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    Network.getInstance().disconect();
                    System.out.println("disconnect");
                    infoMassage("Server disconnect");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if(!setUser()) return;
        setParamConnect();

        if (onConnect.getText().equals("Disconnect")) {
            thread.interrupt();
            user = null;
            Platform.runLater(() -> {
                fileListServer.getItems().clear();
                GuiClient(true);
                GuiConnect(false);
                buttonLoginVisible(false);
                onConnect.setText("Connect");
            });
            sendMsgToServer(new StatusInfo("reset_connect"));

        }

        if (!Network.getInstance().isConnected()) {
            try {
                Network.getInstance().connect(ipAddress, serverPort);
                infoMassage("Server connect");
                Platform.runLater( () -> onConnect.setText("Disconnect"));

                thread.setDaemon(true);
                thread.start();
                setUser();
                sendMsgToServer(user);

            } catch (IOException e) {
                infoMassage("Server not answer ");
                e.printStackTrace();
            }
        }

    }


    /**
     * @param msg
     * @throws Exception
     * Метод отправки сообщения в сторону сервера
     */
    private void sendMsgToServer(AbsMsg msg) throws Exception{
        Network.getInstance().sendObject(msg);
        System.out.println("Send massage to server");
    }

    /**
     * @throws Exception
     * Метод отправки содержимого папки клиента в сторону сервера
     */
    public void serverSync () throws Exception{
        ObservableList<String> filesInList =  fileListClient.getItems();

        sendFile(filesInList);
    }


    /**
     * @param filesInList
     * @throws Exception
     * Метод оправки файлов в сторону сервера
     */
    public void sendFile(ObservableList<String> filesInList) throws Exception {
        Thread thread;
        thread = new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    labelInfo.setVisible(true);
                });
                String info;
                FileMsg file;
                byte[] data;
                int size, set;
                for (String nameFile : filesInList) {
                    Path fileLocation = Paths.get(currentFolder + "/" + nameFile);
                    data = Files.readAllBytes(fileLocation);
                    size = (int) Math.ceil((float) data.length / SIZE_SET);
                    System.out.println("Send file : " + nameFile + "-" + data.length + " \\ " + size);
                    for (set = 0; set < size; set++) {
                        final double progress = (double) set / size;
                        Platform.runLater(() -> {
                            progressBar.setProgress(progress);

                            labelInfo.setText(nameFile.length() > 25 ? nameFile.substring(0,22) + "..." : nameFile);
                        });
                        int startCopy = set * SIZE_SET;
                        int endCopy = (startCopy + SIZE_SET) > data.length ? data.length : startCopy + SIZE_SET;
                        file = new FileMsg(nameFile, Arrays.copyOfRange(data, startCopy, endCopy), set, size - 1);
                        sendMsgToServer(file);
                    }
                }
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    labelInfo.setVisible(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * @param msg
     * Метод обработки событий от сервера
     */
    private void controllerMsg (Object msg) {
        switch (((AbsMsg) msg).getTypeMsg()) {
            case "user":
                user = (UserCloud) msg;
                break;
            case "info":
                infoMassage(((StatusInfo) msg).getMassage());
                break;
            case "fileList":
                Platform.runLater( () -> {
                    try {
                        fileListServer.getItems().clear();
                        fileListServer.setItems(FXCollections.observableArrayList(((FileList) msg).getListFile()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case "sync":
                try{
                    writeFile(msg);
                    openFolder();
                }catch (Exception e){
                    e.printStackTrace();
                }
        }
    }


    /**
     * Метод создания контекстного меню экрана клиента и обработки событий
     */
    private void setContextMenuClient () {
        ContextMenu contextMenuClient = new ContextMenu();
        MenuItem del = new MenuItem("Delete");
        del.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ObservableList<String> files = fileListClient.getSelectionModel().getSelectedItems();
                System.out.println(files.toString());
                for (String fileName : files) {
                    fileName = currentFolder + "/" + fileName;
                    System.out.println(fileName);
                    File file = new File(fileName);
                    if (file.delete()) {
                        infoMassage("File delete ок :" + fileName);
                    }else {
                        infoMassage("File delete error :" + fileName);
                    }
                }
                readClientFolder();
            }
        });

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (user != null && user.isAuth()) {
                    ObservableList<String> files = fileListClient.getSelectionModel().getSelectedItems();
                    try {
                        sendFile(files);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        contextMenuClient.getItems().addAll(del, copy);
        fileListClient.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
               contextMenuClient.show(fileListClient, event.getScreenX(), event.getScreenY());
            }
        });
    }

    /**
     * Метод создания контекстного меню экрана сервера и обработки событий
     */
    private void setContextMenuServer () {
        ContextMenu contextMenuClient = new ContextMenu();
        MenuItem del = new MenuItem("Delete");
        del.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                List<String> files = CommonClass.obsToList(fileListServer.getSelectionModel().getSelectedItems());
                System.out.println(files.toString());
                FileList fileList = new FileList(files, FileList.TypeRequest.delete);
                try {
                    sendMsgToServer(fileList);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                List<String> files = CommonClass.obsToList(fileListServer.getSelectionModel().getSelectedItems());
                System.out.println(files.toString());
                FileList fileList = new FileList(files, FileList.TypeRequest.copy);
                try {
                    sendMsgToServer(fileList);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        contextMenuClient.getItems().addAll(del, copy);
        fileListServer.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                if (user != null && user.isAuth()) {
                    contextMenuClient.show(fileListServer, event.getScreenX(), event.getScreenY());
                }
            }
        });
    }


    /**
     * @param massage
     * Метод отправки ниформационных сообщений клиенту
     */
    private void infoMassage(String massage) {
        Platform.runLater( () -> statInfo.appendText(massage + "\n"));
    }

    /**
     * @param msg
     * @throws Exception
     * Метод записи файла на клиента
     */
//    private void writeFile (Object msg) throws Exception{
//        FileMsg fileMsg = (FileMsg) msg;
//        File file = new File(currentFolder + fileMsg.getName());
//        System.out.println(file);
//        file.createNewFile();
//        FileOutputStream stream = new FileOutputStream(file,false);
//        try {
//            stream.write(fileMsg.getData());
//        } catch (Exception e) {
//            infoMassage("error: " + fileMsg.getName());
//        }
//        finally {
//            stream.close();
//        }
//        infoMassage("copy: " + fileMsg.getName());
//    }

    public void writeFile (Object msg) {
        FileMsg fileMsg = (FileMsg) msg;
        String folder = currentFolder;
        String name = fileMsg.getName();
        int size = fileMsg.getSize();
        int set = fileMsg.getSet();
        byte[] data = fileMsg.getData();
        System.out.println(folder  + "/" + name);
        try(RandomAccessFile file = new RandomAccessFile(folder  + "/" + name, "rw")){
            file.seek(set * SIZE_SET);
            file.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод создание нового пользователя на сервере
     */
    public void loginUser() {
        setUser();
        if(chkCreate.isSelected()) user.setCreare(true);
        try {
            sendMsgToServer(user);
        }catch (Exception e){
            e.printStackTrace();
            infoMassage("Error create user");
        }
    }

    public void onEdit(ListView.EditEvent<String> stringEditEvent) {
        String str = fileListClient.getSelectionModel().getSelectedItem();

        System.out.println(str);
    }
}

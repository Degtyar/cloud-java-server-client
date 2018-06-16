
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
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Класс обработки потока событий на клиенте
 */
public class Controller implements Initializable {
    public final int SIZE_SET = 512000;
    public PasswordField passField;
    public ListView<String> fileListServer;
    public ListView<String> fileListClient;
    public ObservableList<String> listViewClient = FXCollections.observableArrayList();
    public ObservableList<String> listViewServer = FXCollections.observableArrayList();

    public TextField loginField, pathField, serverField, portField;
    public TextArea statInfo;
    public Button onConnect, serverSync, clientSync, openFolder, login;
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
    //TODO реализовать сохранение настроек в случае соединения
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
     * @return
     * Метод создания обекта пользователя на соновании логина и правлоя
     */
    private boolean setUser() {
        user = null;
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
     * @param trigger
     * Метод доступа к элементам управления блока клиента
     */
    private void GuiClient(Boolean trigger) {
        pathField.setDisable(trigger);
        serverSync.setDisable(trigger);
        openFolder.setDisable(trigger);
        clientSync.setDisable(trigger);
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
            fileListClient.setItems(listViewClient);
            fileListServer.setItems(listViewServer);
            readClientFolder ();
        });
    }

    /**
     * @throws Exception
     * Метод чтения информации отображение информации в папке клиента
     */
    public void openFolder () {
        setCurrentFolder(); //чтение значения из поля папки
        readClientFolder(); //отображение информации в папке
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
                listViewClient.clear();
                listViewClient.addAll(result);
            } catch (IOException e) {
                e.printStackTrace();
                infoMassage("Error read client folder :" + currentFolder );
            }
        });
    }

    /**
     * @throws Exception
     * Метод обработки кнопки подключения к серверу
     */
    public void tryConnect() throws Exception{
        connect();
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
                Platform.runLater(() -> {
                    GuiClient(true);
                    GuiConnect(false);
                    onConnect.setText("Connect");
                    listViewServer.clear();
                    infoMassage("Connection server lost");
                });
            } finally {
                try {
                    Network.getInstance().disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if(!setUser()) return; // не подключаться если параметры не определены
        setParamConnect();
        // обработчик событий на отключение
        if (onConnect.getText().equals("Disconnect")) {
            thread.interrupt();
            user = null;
            Platform.runLater(() -> {
                listViewServer.clear();
                GuiClient(true);
                GuiConnect(false);
                buttonLoginVisible(false);
                onConnect.setText("Connect");
            });
            sendMsgToServer(new StatusInfo("reset_connect"));
        }
        // подключение
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
    public void serverSync() {
        sendFile(listViewClient);
    }

    /**
     * @param filesInList
     * @throws Exception
     * Метод оправки файлов в сторону сервера
     */
    public void sendFile(ObservableList<String> filesInList) {
        Thread thread;
        thread = new Thread(() -> { // в отдельный поток для работы интерфейса
            try {
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    labelInfo.setVisible(true);
                });
                FileMsg file;
                byte[] data;
                int size, set, startCopy, endCopy ;

                for (String nameFile : filesInList) {
                    Path fileLocation = Paths.get(currentFolder + "/" + nameFile);
                    data = Files.readAllBytes(fileLocation);
                    size = (int) Math.ceil((float) data.length / SIZE_SET);
                    for (set = 0; set < size; set++) {
                        final double progress = (double) set / size;

                        Platform.runLater(() -> {
                            progressBar.setProgress(progress);
                            labelInfo.setText(nameFile.length() > 25 ? nameFile.substring(0,22) + "..." : nameFile);
                        });
                        //задание параметров
                        startCopy = set * SIZE_SET;
                        endCopy = (startCopy + SIZE_SET) > data.length ? data.length : startCopy + SIZE_SET;
                        //отправка файлов на сервер
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
     * Метод обработки событий и реакции событий от сервера
     */
    private void controllerMsg (Object msg) {
        switch (((AbsMsg) msg).getTypeMsg()) {
            case "user":
                user = (UserCloud) msg;
                break;
            case "info":
                infoMassage(((StatusInfo) msg).getMassage());
                if(((StatusInfo) msg).getMassage().equals("Server disconnect"))
                break;
            case "fileList":
                Platform.runLater( () -> {
                    try {
                        listViewServer.clear();
                        listViewServer.addAll(((FileList) msg).getListFile());
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
                    fileName = currentFolder + fileName;
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
        Platform.runLater( () -> statInfo.appendText(new SimpleDateFormat("HH:mm").format(new Date()) +
                                                                                            " " + massage + "\n"));
    }

    /**
     * @param msg
     * @throws Exception
     * Метод записи файла на клиента
     */
    public void writeFile (Object msg) {
        FileMsg fileMsg = (FileMsg) msg;
        String folder = currentFolder;
        String nameFile= fileMsg.getName();
        int size = fileMsg.getSize();
        int set = fileMsg.getSet();
        byte[] data = fileMsg.getData();
        System.out.println(folder  + "/" + nameFile);
        try(RandomAccessFile file = new RandomAccessFile(folder  + "/" + nameFile, "rw")){
            if(size > 2){
                Platform.runLater(() -> {
                    progressBar.setVisible(true);
                    labelInfo.setVisible(true);
                    progressBar.setProgress((double) set / size);
                    labelInfo.setText(nameFile.length() > 25 ? nameFile.substring(0,22) + "..." : nameFile);
                });
            }
            file.seek(set * SIZE_SET);
            file.write(data);
            if(set == size) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    labelInfo.setVisible(false);
                });
                infoMassage("receive file: " + nameFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод создание нового пользователя на сервере
     */
    public void loginUser() {
        if(!setUser())return;
        if(chkCreate.isSelected()) user.setCreate(true);
        try {
            sendMsgToServer(user);
        }catch (Exception e){
            e.printStackTrace();
            infoMassage("Error create user");
        }
    }

    /**
     * Метод запроса всех файлов от клиента
     */
    public void clientSync() {
        FileList fileList = new FileList(CommonClass.obsToList(listViewServer), FileList.TypeRequest.copy);
        try {
            sendMsgToServer(fileList);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

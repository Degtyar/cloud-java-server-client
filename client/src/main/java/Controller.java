
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public PasswordField passField;
    public ListView<String> fileListClient, fileListServer;
    public TextField loginField, pathField, serverField, portField;
    public TextArea statInfo;
    public Button onConnect, serverSync, openFolder, create;
    public ProgressBar progressBar;

    private int serverPort;
    private InetAddress ipAddress;
    private UserCloud user;
    private String carentFolder;
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
     * Функция сохранения полей на клиенте
     */
    public void saveProperty () {
        propertyRead.setProperty("login",loginField.getText());
        propertyRead.setProperty("password",passField.getText());
        propertyRead.setProperty("rootDirectory",pathField.getText());
        propertyRead.setProperty("serverIpAddress",serverField.getText());
        propertyRead.setProperty("serverPort",portField.getText());
    }

    /**
     * Функция определения текущей деректории синхронзации
     */
    public void setCarentFolder() {
        this.carentFolder = pathField.getText();
    }


    /**
     * @param trigger
     * Функция доступа к элементам управления блока клиента
     */

    private void GuiClient(Boolean trigger) {
        pathField.setDisable(trigger);
        serverSync.setDisable(trigger);
        openFolder.setDisable(trigger);
    }


    /**
     * @param trigger
     * функция открытия кнопки создания клиента
     */
    private void buttonCreateVisible(Boolean trigger){
        create.setVisible(trigger);
    }


    /**
     * @param trigger
     * Функция управления элементами подключения к серверу
     */
    private void GuiConnect (Boolean trigger) {
        loginField.setDisable(trigger);
        passField.setDisable(trigger);
        serverField.setDisable(trigger);
        portField.setDisable(trigger);
        onConnect.setDisable(trigger);
    }


    /**
     * @throws Exception
     * Функция чтения информации из папки
     */
    public void openFolder () throws Exception {
        setCarentFolder();
        readClientFolder();
    }


    /**
     * Функция чтения содержимого папки пользователя
     */
    public void readClientFolder () {
        Platform.runLater( () -> {
            try {
                List<String> result;
                Path paths = Paths.get(carentFolder);
                DirectoryStream<Path> stream = Files.newDirectoryStream(paths, path -> path.toFile().isFile());
                result = CommonClass.pathToList(stream);
                fileListClient.getItems().clear();
                fileListClient.setItems(FXCollections.observableArrayList(result));
            } catch (IOException e) {
                e.printStackTrace();
                infoMassage("Error read client folder");
            }
        });
    }


    /**
     * @return
     * Функция создания обекта пользователя на соновании логина и правлоя
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
     * Функция задания параментов подключения к серверу
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
     * Фугкция обработки кнопки подключения к серверу
     */
    public void tryConnect() throws Exception{
        connect();
        sendMsgToServer(user);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileListClient.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileListServer.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        GuiClient(true);
        buttonCreateVisible(false);
        statInfo.setEditable(false);
        setField ();
        setContextMenuClient();
        setContextMenuServer();

    }

    /**
     * @throws Exception
     * Функция создания соедниения к серверу
     */
    private void connect() throws Exception {
        if(!setUser()) return;
        setParamConnect();
        if (!Network.getInstance().isConnected()) {
            try {
                Network.getInstance().connect(ipAddress, serverPort);
                Thread thread = new Thread(() -> {
                    try {
                        while (Network.getInstance().isConnected()) {
                            if (user.isAuth()){
                                // Изменнение интерфейса
                                GuiClient(false);
                                GuiConnect(true);
                                buttonCreateVisible(false);
                            } else {
                                // Изменнение интерфейса
                                GuiClient(true);
                                GuiConnect(false);
                                buttonCreateVisible(true);
                            }
                            Object msg = Network.getInstance().readObject();
                            if (msg instanceof AbsMsg) {
                                controllerMsg(msg);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                thread.setDaemon(true);
                thread.start();

            } catch (IOException e) {
                infoMassage("Server not answer ");
                e.printStackTrace();
            }
        }
    }


    /**
     * @param msg
     * @throws Exception
     * Функция отправки сообщения в сторону сервера
     */
    private void sendMsgToServer(AbsMsg msg) throws Exception{
        Network.getInstance().sendObject(msg);
        System.out.println("Send msg");
    }

    /**
     * @throws Exception
     * Функция отправки содержимого папки клиента в сторону сервера
     */
    public void serverSync () throws Exception{
        ObservableList<String> filesInList =  fileListClient.getItems();
        sendFile(filesInList);
    }


    /**
     * @param filesInList
     * @throws Exception
     * Функция оправки файлов в сторону сервера
     */
    public void sendFile(ObservableList<String> filesInList) throws Exception {
        FileMsg file;
        for (String nameFiles : filesInList){
            file = new FileMsg(carentFolder,nameFiles, AbsMsg.TypeMsg.sync);
            sendMsgToServer(file);
        }
        System.out.println(Arrays.toString(filesInList.toArray()));
    }

    /**
     * @param msg
     * функция обработки событий от сервера
     */
    private void controllerMsg (Object msg) {
        switch (((AbsMsg) msg).getTypeMsg()) {
            case "user":
                user = (UserCloud) msg;
                statInfo.appendText("Server connect \n" );
                break;
            case "info":
                statInfo.appendText(((StatusInfo) msg).getMassage());
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
     * Функция создания контекстного меню экрана клиента и обработки событий
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
                    fileName = carentFolder + "/" + fileName;
                    System.out.println(fileName);
                    File file = new File(fileName);
                    if (file.delete()) {
                        infoMassage("File delete ок :" + fileName);
                    }else {
                        infoMassage("File delete error :" + fileName);
                    }
                }
                readClientFolder ();
            }
        });

        MenuItem copy = new MenuItem("Copy");
        copy.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ObservableList<String> files = fileListClient.getSelectionModel().getSelectedItems();
                try{
                    sendFile(files);
                }catch (Exception e){
                    e.printStackTrace();
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
     * Функция создания контекстного меню экрана сервера и обработки событий
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
                contextMenuClient.show(fileListServer, event.getScreenX(), event.getScreenY());
            }
        });
    }


    private void infoMassage(String massage) {
        statInfo.appendText(massage + "\n");
    }

    private void writeFile (Object msg) throws Exception{
        FileMsg fileMsg = (FileMsg) msg;
        File file = new File(carentFolder + fileMsg.getName());
        System.out.println(file);
        file.createNewFile();
        FileOutputStream stream = new FileOutputStream(file,false);
        try {
            stream.write(fileMsg.getData());
        } catch (Exception e) {
            infoMassage("error: " + fileMsg.getName());
        }
        finally {
            stream.close();
        }
        infoMassage("copy: " + fileMsg.getName());
    }

    public void createUser() {
        setUser();
        user.setCreare(true);
        try {
            sendMsgToServer(user);
        }catch (Exception e){
            e.printStackTrace();
            infoMassage("Error create user");
        }
    }
}

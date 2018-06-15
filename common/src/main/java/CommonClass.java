import javafx.collections.ObservableList;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CommonClass {
    public static final int SIZE_SET = 512000;

    public static List<String> pathToList (DirectoryStream<Path> stream ) {
        List<String> result = new ArrayList<>();
        for (Path entry: stream) {
            result.add(entry.getFileName().toString());
        }
        return result;
    }

    public static List<String> obsToList (ObservableList<String> ods) {
        List<String> list = new LinkedList<>();
        for (String s : ods) {
            list.add(s);
        }
        return list;
    }

    public static void saveFileMsg (FileMsg fileMsg, String folder) {
        String name = fileMsg.getName();
        int size = fileMsg.getSize();
        int set = fileMsg.getSet();
        byte[] data = fileMsg.getData();
        try(RandomAccessFile file = new RandomAccessFile(folder + name, "rw")){
            file.write(data, set * SIZE_SET, SIZE_SET);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createFileMsg (Path path) {

        byte[] data;
    }

}

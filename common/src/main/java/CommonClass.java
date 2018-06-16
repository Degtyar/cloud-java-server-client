import javafx.collections.ObservableList;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Общие вспомогательные методы
 */
public class CommonClass {

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
}

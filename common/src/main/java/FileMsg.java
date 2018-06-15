import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileMsg extends AbsMsg {
    private String folder;
    private String name;
    private byte[] data;
    private int size,  set;

    public FileMsg(String folder, String name) throws Exception {
        super(TypeMsg.sync);
        this.folder = folder;
        Path fileLocation = Paths.get(folder + "/"+ name);
        this.name = name;
        this.data = Files.readAllBytes(fileLocation);
        //Files file = new Files(fileLocation);
        //file.copy()
    }

    public FileMsg(String name, byte[] data, int set, int size){
        super(TypeMsg.sync);
        this.name = name;
        this.data = data;
        this.set = set;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public int getSet() {
        return set;
    }

    public String getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}

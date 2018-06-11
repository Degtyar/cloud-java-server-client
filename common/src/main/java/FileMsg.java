import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileMsg extends AbsMsg {
    private String folder;
    private String name;
    private byte[] data;
    private TypeMsg typeMsg;

    public FileMsg(String folder, String name,TypeMsg typeMsg) throws Exception {
        super(typeMsg);
        this.folder = folder;
        this.typeMsg = typeMsg;
        Path fileLocation = Paths.get(folder + "/"+ name);
        this.name = name;
        this.data = Files.readAllBytes(fileLocation);
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

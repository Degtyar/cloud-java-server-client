

public class FileMsg extends AbsMsg {

    private String name;
    private byte[] data;
    private int size,  set;

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

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}

import java.util.List;

/**
 * Класс списков файлов для запроса удаления или обновления информации
 */
public class FileList extends AbsMsg {
    protected enum TypeRequest{
        info,
        copy,
        delete,
    }
    private TypeRequest typeRequest;

    private List<String> listFile;

    public FileList (List<String> listFile) {
        super(TypeMsg.fileList);
        this.listFile = listFile;
        this.typeRequest = TypeRequest.info;
    }

    public FileList (List<String> listFile, TypeRequest typeRequest) {
        super(TypeMsg.fileList);
        this.listFile = listFile;
        this.typeRequest = typeRequest;
    }

    public List<String> getListFile() {
        return listFile;
    }

    public TypeRequest getTypeRequest() {
        return typeRequest;
    }
}

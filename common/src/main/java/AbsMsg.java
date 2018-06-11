import java.io.Serializable;

public abstract class AbsMsg implements Serializable {
    protected enum TypeMsg {
        user,
        sync,
        info,
        fileList,
    }
    protected TypeMsg typeMsg;

    public AbsMsg (TypeMsg typeMsg){
        this.typeMsg = typeMsg;
    }

    public String getTypeMsg (){
        return this.typeMsg.toString();
    }
}

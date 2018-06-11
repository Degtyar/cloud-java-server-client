public class StatusInfo extends AbsMsg {
    private String massage;

    public StatusInfo (String massage){
        super(TypeMsg.info);
        this.massage = massage + "\n";
    }

    public String getMassage() {
        return massage;
    }
}

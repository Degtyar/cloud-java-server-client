public class StatusInfo extends AbsMsg {
    private String massage;

    /**
     * @param massage
     * Класс информационных сообщений
     */
    public StatusInfo (String massage){
        super(TypeMsg.info);
        this.massage = massage ;
    }
    public String getMassage() {
        return massage;
    }
}

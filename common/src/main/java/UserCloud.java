/**
 * Класс пользователя
 */
//TODO шифрование информаци о пользователе
public class UserCloud extends AbsMsg{
    private String login, pass;
    private boolean isAuth, create;

    public UserCloud(String login, String pass) {
        super(TypeMsg.user);
        this.login = login;
        this.pass = pass;
        this.isAuth = false;
        this.create = false;
    }

    public String getLogin() {
        return login;
    }

    public String getPass() {
        return pass;
    }

    public void setIsAuth (boolean isAuth){
        this.isAuth = isAuth;
    }

    public boolean isAuth() {
        return isAuth;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean creare) {
        this.create = creare;
    }
}

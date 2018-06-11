import sun.security.provider.MD5;

public class UserCloud extends AbsMsg{
    private String login, pass;
    private boolean isAuth, creare;

    public UserCloud(String login, String pass) {
        super(TypeMsg.user);
        this.login = login;
        this.pass = pass;
        this.isAuth = false;
        this.creare = false;
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

    public boolean isCreare() {
        return creare;
    }

    public void setCreare(boolean creare) {
        this.creare = creare;
    }
}

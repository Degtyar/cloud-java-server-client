import java.sql.*;


/**
 * Класс работы с базой данных
 */
//TODO Шифрование информации о пользователе
public class AuthService {
    private static final String selectUserCloud = "SELECT login FROM users WHERE login = ? AND password = ?";
    private static final String createDB = "CREATE TABLE IF NOT EXISTS \"users\" ( `id_users` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `login` TEXT NOT NULL, `password` TEXT NOT NULL )";
    private static final String createUserCloud = "INSERT INTO USERS (login,password) VALUES (? ,?)";
    private static final String checkUserCloud = "SELECT login FROM users WHERE login = ?";
    private static final String dbconnect = "jdbc:sqlite:users.db";
    private static Connection connection;
    private static PreparedStatement pstTryAuth, pstCreateUser, chkUserCloud, pstCreateDB ;
    private static ResultSet resultSet;

    /**
     * @return
     * Получение коннекта к базе
     */
    private static Connection connect() {
        try {
            if (connection == null) {
                connection = DriverManager.getConnection(dbconnect);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }


    /**
     * @param user
     * @return
     * Авторизация пользователя
     */
    public static String tryAuth (UserCloud user) {
        String login ;
        try {
            pstTryAuth = connect().prepareStatement(selectUserCloud);
            pstTryAuth.setString(1,user.getLogin());
            pstTryAuth.setString(2,user.getPass());
            resultSet = pstTryAuth.executeQuery();
            while (resultSet.next()) {
                System.out.println(resultSet.toString());
                login = resultSet.getString(1);
                user.setIsAuth(true);
                return login;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                pstCreateDB = connect().prepareStatement(createDB);
                pstCreateDB.executeUpdate();
            }catch (SQLException ex) {
                ex.printStackTrace();
            }

        }
        return null;
    }


    /**
     * @param user
     * @return
     * Метод создания пользователя
     */
    public static boolean createUser (UserCloud user) {
        try {
            if(!checkUser(user)) { // проверка на существование пользователя
                pstCreateUser = connect().prepareStatement(createUserCloud);
                pstCreateUser.setString(1, user.getLogin());
                pstCreateUser.setString(2, user.getPass());
                pstCreateUser.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param user
     * @return
     * Метод проверки пользователя на существование
     */
    public static boolean checkUser (UserCloud user) {
        String login ;
        try {
            chkUserCloud = connect().prepareStatement(checkUserCloud);
            chkUserCloud.setString(1, user.getLogin());
            resultSet = chkUserCloud.executeQuery();
            while (resultSet.next()) {
                login = resultSet.getString(1);
                return login.equals(user.getLogin());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Метод закрытия соединения с базой
     */
    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
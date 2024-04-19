package pet.project.DataBase;

import pet.project.utils.LogController;

import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;

public class DataBaseConnector {
    private static final String urlDB = "jdbc:postgresql://localhost:5432/Bank";
    private static final String userDB = "postgres";
    private static final String passwordDB = "159357";
    private static Connection connection;
    private static Statement statement;

    public static boolean connectToDataBase() {
        try {
            connection = DriverManager.getConnection(urlDB,userDB, passwordDB);
            statement = connection.createStatement();
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static boolean saveClient(String login, String password, SimpleDateFormat timeRequest) {
        try {
            statement.executeUpdate("INSERT INTO client VALUES(" + "'" + login + "'" + "," + "'" + password + "'" + ", " + new BigDecimal(0) + ")");
            statement.executeUpdate("INSERT INTO token (client_login, id) VALUES (" + "'" + login + "'" + ", DEFAULT)");
            LogController.log("Клиент " + login + " зарегистрирован.", timeRequest);
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public static void updateJwt(String login, String jwt) {
        try {
            statement.executeUpdate("UPDATE token SET jwt = '" + jwt + "' WHERE client_login = '" + login + "'");
        }
        catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static ResultSet clientByLogin(String login) {
        try {
            return statement.executeQuery("SELECT * FROM client WHERE login = '" + login + "'");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static ResultSet tokenByLogin(String login) {
        try {
            return statement.executeQuery("SELECT jwt FROM client WHERE login = '" + login + "'");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static boolean existsByLogin(String login) {
        try (ResultSet request = statement.executeQuery("SELECT login FROM client WHERE login = " + "'" + login + "'")) {
            return request.next();
        } catch (SQLException exception) {
            exception.printStackTrace();
            return true;
        }
    }

    public static String getLoginByToken(String token) {
        try (ResultSet login = statement.executeQuery("SELECT client_login FROM token WHERE jwt = " + "'" + token + "'")) {
            login.next();
            return login.getString("client_login");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static BigDecimal getBalanceByLogin(String login) {
        try (ResultSet balance = statement.executeQuery("SELECT balance FROM client WHERE login = " + "'" + login + "'")) {
            balance.next();
            return balance.getBigDecimal("balance");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static boolean updateBalance(String login, BigDecimal newBalance) {
        try {
            statement.executeUpdate("UPDATE client SET balance = '" + newBalance + "' WHERE login = '" + login + "'");
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public static void closeConnectToDataBase() {
        try {
            connection.close();
            statement.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}

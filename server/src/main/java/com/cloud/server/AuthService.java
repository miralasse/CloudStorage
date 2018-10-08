package com.cloud.server;

import java.sql.*;

public class AuthService {
    private static AuthService ourInstance;

    private Connection connection;
    private Statement statement;

    private AuthService(){}

    public static AuthService getOurInstance() {
        if (ourInstance == null)
            ourInstance = new AuthService();
        return ourInstance;
    }

    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:server/cloudDB.db");
        statement = connection.createStatement();
    }

    public boolean checkLoginAndPass(String login, String password){
        ResultSet set = null;
        try {
            set = statement.executeQuery("SELECT user_id FROM users WHERE login = '" + login + "' AND password = '" + password + "';");
            while (set.next()){
                System.out.println(set.getInt("user_id"));
                return (set.getInt("user_id") > 0);
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при работе с базой данных");
            e.printStackTrace();
        }
        return false;
    }


    public void disconnect(){
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("Ошибка при отключении сервиса авторизации");
            e.printStackTrace();
        }
    }


}

package com.cloud.server;

import java.sql.*;

public class AuthService {
    private static AuthService ourInstance;

    private Connection connection;
    private PreparedStatement preparedStatement;

    private AuthService(){}

    public static AuthService getOurInstance() {
        if (ourInstance == null)
            ourInstance = new AuthService();
        return ourInstance;
    }

    public void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:server/cloudDB.db");
    }

    public boolean checkLoginAndPass(String login, String password){
        ResultSet set = null;
        try {
            preparedStatement = connection.prepareStatement("SELECT user_id FROM users WHERE login = ? AND password = ?;");
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            set = preparedStatement.executeQuery();
            while (set.next()){
                System.out.println(set.getInt("user_id"));
                return (set.getInt("user_id") > 0);
            }
        } catch (SQLException e) {
            System.out.println("Error occurred while working with database");
            e.printStackTrace();
        }
        return false;
    }


    public void disconnect(){
        try {
            preparedStatement.close();
            connection.close();
        } catch (SQLException e) {
            System.out.println("Error occurred while disconnecting auth service");
            e.printStackTrace();
        }
    }
}
package org.example.db;

import org.example.logger.Logger;
import org.example.structures.NotificationInfo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.HexFormat;

public class DataBaseWrapper { //TODO: implement database wrapper
    String url = "jdbc:sqlite:sample.db";
    private Connection conn;

    public static void main(String[] args){
        demo();
    }

    public static void demo(){
        DataBaseWrapper db = new DataBaseWrapper();
        demo(db);
    }

    public static void demo(DataBaseWrapper db){
        db.setAuthenticationDB();
        db.setNotificationsDB();
        db.addClient("admin", "admin", 1);
        db.addClient("myuser", "mypass", 0);
        db.addClient("user", "user", 1);
        db.printAuthTable();
        db.printNotificationsTable();
//        db.closeDbConnection();
    }

    public DataBaseWrapper() {
        connect();
    }

    public DataBaseWrapper(String url_) {
        this.url = url_;
        connect();
    }

    public static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    public void closeDbConnection(){
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Closed database connection.");
            }
        } catch (SQLException e) {
            System.out.println("Failed to close database connection: " + e.getMessage());
        }
    }

    private void connect() {
        try {
            conn = DriverManager.getConnection(url);
            System.out.println("Connected to SQLite database.");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    public ArrayList<NotificationInfo> getDbForClient(int clientID) {

        NotificationInfo n = new NotificationInfo(clientID, 1, "title", "payload", 123456789);
        NotificationInfo n2 = new NotificationInfo(clientID, 2, "title2", "payload2", 123456789);
        ArrayList<NotificationInfo> notifications = new ArrayList<>();
        notifications.add(n);
        notifications.add(n2);

        return notifications;
    }

    public void setNotificationsDB() {
        String createAlarmsTable = """
        create table if not exists notifications(
            id integer primary key autoincrement,
            clientId integer not null,
            notificationId integer not null,
            title text not null,
            payload text,
            fire_at integer not null  -- epoch seconds (UTC)
        );
        """;






        assert conn != null;


        try(Statement stmt = conn.createStatement()){

            stmt.execute(createAlarmsTable);

        } catch (SQLException e) {
            System.out.println("Statement creation failed: " + e.getMessage());
        }
    }

    public void setAuthenticationDB() {
        String createAuthTable = """
        create table if not exists authentications(
            id integer primary key autoincrement,
            isAdmin integer,
            usernameHash text,
            passwordHash text
        );
        """;






        assert conn != null;


        try(Statement stmt = conn.createStatement()){

            stmt.execute(createAuthTable);

        } catch (SQLException e) {
            System.out.println("Statement creation failed: " + e.getMessage());
        }
    }

    public void addClient(String username, String password, int isAdmin) {
        try {

            if (findClient(username, password) != 0) return;

            String usernameHash = sha256(username);
            String passwordHash = sha256(password);

            String query = "INSERT INTO authentications (usernameHash, passwordHash, isAdmin) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, usernameHash);
            stmt.setString(2, passwordHash);
            stmt.setInt(3, isAdmin);

            stmt.executeUpdate();
            stmt.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int findClient(String username, String password) {
        try {
            String usernameHash = sha256(username);
            String passwordHash = sha256(password);

            if (usernameHash == null || passwordHash == null) return 0;
            Logger.info("Authenticating usernameHash =" + usernameHash + " passwordHash = " + passwordHash);

            String query = "SELECT * FROM authentications WHERE usernameHash = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, usernameHash);

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return 0; // no user found

            if (!rs.getString("passwordHash").equals(passwordHash)) return -1; // incorrect password

            return rs.getInt("isAdmin") == 1 ? 2 : 1; // 2 = admin, 1 = user

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void putNotifications(ArrayList<NotificationInfo> notifications, int clientID) {
        try {
            String query = "INSERT INTO notifications (clientId, notificationId, title, payload, fire_at) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            for (NotificationInfo n : notifications) {
                stmt.setInt(1, clientID);
                stmt.setInt(2, n.getNotificationID());
                stmt.setString(3, n.getTitle());
                stmt.setString(4, n.getPayload());
                stmt.setLong(5, n.getFireAt());
                stmt.addBatch();
            }
            stmt.executeBatch();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void printAuthTable() {
        String query = "SELECT * FROM authentications";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("=== AUTHENTICATIONS TABLE ===");
            while (rs.next()) {
                int id = rs.getInt("id");
                int isAdmin = rs.getInt("isAdmin");
                String usernameHash = rs.getString("usernameHash");
                String passwordHash = rs.getString("passwordHash");

                System.out.println("id=" + id +
                        ", isAdmin=" + isAdmin +
                        ", usernameHash=" + usernameHash +
                        ", passwordHash=" + passwordHash);
            }
        } catch (SQLException e) {
            System.out.println("Failed to read authentications: " + e.getMessage());
        }
    }

    public void printNotificationsTable() {
        String query = "SELECT * FROM notifications";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("=== NOTIFICATIONS TABLE ===");
            while (rs.next()) {
                int id = rs.getInt("id");
                int clientId = rs.getInt("clientId");
                int notificationId = rs.getInt("notificationId");
                String title = rs.getString("title");
                String payload = rs.getString("payload");
                long fireAt = rs.getLong("fire_at");

                System.out.println("id=" + id +
                        ", clientId=" + clientId +
                        ", notificationId=" + notificationId +
                        ", title=" + title +
                        ", payload=" + payload +
                        ", fire_at=" + fireAt);
            }
        } catch (SQLException e) {
            System.out.println("Failed to read notifications: " + e.getMessage());
        }
    }


}

package org.example.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;

import org.example.logger.Logger;
import org.example.structures.NotificationInfo;

public class DataBaseWrapper {
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

    private ResultSet queryUserByUsernameHash(String usernameHash) throws SQLException {
        String query = "SELECT id, username, usernameHash, passwordHash, isAdmin FROM authentications WHERE usernameHash = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, usernameHash);
        return stmt.executeQuery(); // ВАЖЛИВО: викликаючий код має закрити stmt/rs (через try-with-resources)
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
            username text,
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
        ensureAuthUsernameColumn();
    }

    private void ensureAuthUsernameColumn() {
        if (conn == null) return;

        if (hasColumn("authentications", "username")) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE authentications ADD COLUMN username text");
            Logger.info("Added username column to authentications table.");
        } catch (SQLException e) {
            Logger.warn("Failed to add username column to authentications table: " + e.getMessage());
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        if (conn == null) return false;

        String pragma = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(pragma)) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        } catch (SQLException e) {
            Logger.warn("Failed to inspect columns for table " + tableName + ": " + e.getMessage());
        }
        return false;
    }

    public void addClient(String username, String password, int isAdmin) {
        try {

            if (findClientStatus(username, password) != 0) return;

            String usernameHash = sha256(username);
            String passwordHash = sha256(password);

            String query = "INSERT INTO authentications ( isAdmin, usernameHash, passwordHash, username) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, isAdmin);
            stmt.setString(2, usernameHash);
            stmt.setString(3, passwordHash);
            stmt.setString(4, username);

            stmt.executeUpdate();
            stmt.close();

        } catch (Exception e) {
            Logger.error("addClient failed: " + e.getMessage());
        }
    }

    public void removeClient(String username, String password){
        int idToDelete = getClientID(username, password);

        if (idToDelete == -1){
            Logger.error("removeClient failed: client wasn't found");
            return;
        }

        String query = "DELETE FROM authentications WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idToDelete);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("removeClient failed: " + e.getMessage());
        }
    }

    public int findClientStatus(String username, String password) {
        try {
            String usernameHash = sha256(username);
            String passwordHash = sha256(password);

            if (usernameHash == null || passwordHash == null) return 0;
            Logger.info("finding client with  usernameHash -> " + usernameHash + " passwordHash -> " + passwordHash);

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, username, usernameHash, passwordHash, isAdmin FROM authentications WHERE usernameHash = ?")) {
                stmt.setString(1, usernameHash);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return 0; // no user found
                    Logger.info("Client was found " + username);
                    if (!rs.getString("passwordHash").equals(passwordHash)) return -1; // incorrect password
                    return rs.getInt("isAdmin") == 1 ? 2 : 1; // 2 = admin, 1 = user
                }
            }

        } catch (Exception e) {
            Logger.error("findClient failed: " + e.getMessage());
            return 0;
        }
    }

    public ArrayList<Integer> addNotifications(ArrayList<NotificationInfo> notifications, int clientID) {
        if (notifications == null || notifications.isEmpty()) {
            Logger.warn("addNotifications called with empty notifications list");
            return new ArrayList<>();
        }

        String query = "INSERT INTO notifications (clientId, notificationId, title, payload, fire_at) VALUES (?, ?, ?, ?, ?)";
        ArrayList<Integer> insertedIds = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            for (NotificationInfo n : notifications) {
                stmt.setInt(1, clientID);
                stmt.setInt(2, n.getNotificationID());
                stmt.setString(3, n.getTitle());
                stmt.setString(4, n.getPayload());
                stmt.setLong(5, n.getFireAt());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        insertedIds.add(rs.getInt(1));
                    }
                }
            }

            return insertedIds;

        } catch (SQLException e) {
            Logger.error("putNotifications failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void removeNotification(int clientID, int notificationID){
        if (clientID == -1) //is admin
        {
            Logger.info("removing notification " + notificationID + " by admin");
            String query = "DELETE FROM notifications WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, notificationID);
                stmt.executeUpdate();
            } catch (SQLException e) {
                Logger.error("removeNotifications failed: " + e.getMessage());
            }
            return;
        }

        Logger.info("removing notifications for client " + clientID + " notificationId " + notificationID);
        String query = "DELETE FROM notifications WHERE clientId = ? AND id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, clientID);
            stmt.setInt(2, notificationID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("removeNotifications failed: " + e.getMessage());
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
                String username = rs.getString("username");
                String usernameHash = rs.getString("usernameHash");
                String passwordHash = rs.getString("passwordHash");

                System.out.println("id=" + id +
                        ", isAdmin=" + isAdmin +
                        ", username=" + username +
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

    public int getClientID(String username, String password) {
        try {
            String usernameHash = sha256(username);
            String passwordHash = sha256(password);

            if (usernameHash == null || passwordHash == null) return 0;
            Logger.info("finding client with  usernameHash -> " + usernameHash + " passwordHash -> " + passwordHash);

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, username, usernameHash, passwordHash, isAdmin FROM authentications WHERE usernameHash = ?")) {
                stmt.setString(1, usernameHash);
                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        Logger.warn("Client wasn't found " + username);
                        return -1;
                    }

                    if (rs.getString("id") == null) {
                        Logger.error("Client was found, but id is missing (problem with db) " + username);
                        return -1;
                    }

                    Logger.info("Client was found " + username + " id: " + rs.getString("id"));
                    return Integer.parseInt(rs.getString("id"));
                }
            }

        } catch (Exception e) {
            Logger.error("getting id failed: " + e.getMessage());
            return -1;
        }
    }




}

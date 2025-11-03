package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;
import org.example.structures.NotificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import static org.example.server.HttpServer.sendHttpNotFound;



public class GetHandler {
    /*
    GET /notifications/get/<specifications> HTTP/1.1
    Host: 127.0.0.1:1488
    Authorization: Basic dXNlcjpwYXNz
    Connection-Type: application/json
    Content-Length: <cnt>

    {
    jsons
    }
     */





    static public void main(String[] args) {
    }

    public static void handleGet(Socket socket, DataBaseWrapper db, String[] parsedHTTP) throws IOException { //TODO: make the method static


        String path = parsedHTTP[1];


        // Нормалізуємо шлях
        String[] pathParts = path.replaceFirst("^/+", "").split("/");


        if (pathParts.length < 2){
            sendHttpNotFound(socket, "length of path is less than 2");
            Logger.error("length of path is less than 2");
            return;
        }

        if (pathParts[0].equals("notifications")){
            if (pathParts[1].equals("get")){
                int clientID = db.getClientID(parsedHTTP[3], parsedHTTP[4]);
                Logger.info("Getting notifications for client " + clientID);
                getNotificationsForClient(socket, clientID, db);
                Logger.info("Getting notifications for client " + clientID + " finished");
                return;
            } else {
                sendHttpNotFound(socket);
            }
        } else if (pathParts[0].equals("users")){
            if (pathParts[1].equals("status")){
                boolean isAdmin = db.findClientStatus(parsedHTTP[3], parsedHTTP[4]) == 2;
                Logger.info("Client is admin? = " + isAdmin);
                sendStatus(socket, isAdmin);
                Logger.info("Client is admin? = " + isAdmin + " finished");
                return;
            }
        } else {
            sendHttpNotFound(socket);


        }


    }



    public static void getNotificationsForClient(Socket socket, int clientID, DataBaseWrapper db) {

        ArrayList<NotificationInfo> notifications = db.getDbForClient(clientID);

        JSONArray jsonArray = new JSONArray();
        for (NotificationInfo n : notifications) {
            JSONObject obj = new JSONObject();
            obj.put("id", n.getNotificationID());
            obj.put("title", n.getTitle());
            obj.put("payload", n.getPayload());
            obj.put("fireAt", n.getFireAt());
            jsonArray.put(obj);
        }

        sendHttpJson(socket, jsonArray.toString());
    }

    public static void sendStatus(Socket socket, boolean status) {
        String json = "{\"isAdmin\": " + status + "}";
        try(OutputStream out = socket.getOutputStream()){
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + json.getBytes().length + "\r\n" +
                    "\r\n" +
                    json;
            out.write(response.getBytes());
            out.flush();
        } catch (IOException e) {
            Logger.error("Error while sending status: " + e.getMessage());
        }
    }

    public static void sendHttpJson(Socket socket, String json) {
        try (OutputStream out = socket.getOutputStream()) {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + json.getBytes().length + "\r\n" +
                    "\r\n" +
                    json;
            out.write(response.getBytes());
            out.flush();
        } catch (IOException e) {
            Logger.error("Error while sending json: " + e.getMessage());
        }
    }

}

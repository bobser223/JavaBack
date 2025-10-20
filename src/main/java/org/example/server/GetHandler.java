package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.structures.NotificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import static org.example.server.HttpServer.sendHttpAuthError;
import static org.example.server.HttpServer.sendHttpNotFound;

public class GetHandler {

    int CLIENT_ID = 1234;





    static void main(String[] args) {
    }

    void handleGet(Socket socket, DataBaseWrapper db, BufferedReader in, String[] basicInfo) throws IOException {


        String path = basicInfo[1];


        String[] pathParts = path.split("/");
        if (pathParts.length < 2){
            sendHttpNotFound(socket, "length of path is less than 2");
            return;
        }

        if (pathParts[0].equals("notifications")){
            if (pathParts[1].equals("get")){
                getNotificationsForClient(socket, Integer.parseInt(pathParts[2]), db);
                return;
            } else {
                sendHttpNotFound(socket);
            }
        }

        getNotificationsForClient(socket, CLIENT_ID, db);
    }



    static void getNotificationsForClient(Socket socket, int clientID, DataBaseWrapper db) {

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



    static void sendHttpJson(Socket socket, String json) {
        try (OutputStream out = socket.getOutputStream()) {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + json.getBytes().length + "\r\n" +
                    "\r\n" +
                    json;
            out.write(response.getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

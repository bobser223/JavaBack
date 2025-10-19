package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.structures.NotificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class GetHandler {

    int CLIENT_ID = 1234;





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

package org.example.server;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;
import static org.example.server.HttpServer.sendHttpNotFound;
import static org.example.server.HttpServer.sendHttpOk;
import org.example.structures.NotificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class PutHandler {

    public static ArrayList<NotificationInfo> fromByte2Array(String jsonBody, int clientID) {
        ArrayList<NotificationInfo> notifications = new ArrayList<>();

        JSONArray array = new JSONArray(jsonBody);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            NotificationInfo n = new NotificationInfo(clientID,
                    obj.getInt("id"),
                    obj.getString("title"),
                    obj.getString("payload"),
                    obj.getLong("fireAt")
            );

            notifications.add(n);
        }

        return notifications;

    }

    public static void handlePut(Socket socket, DataBaseWrapper db, int clientID, String[] parsedHTTP) { //TODO: make the method static
        String path = parsedHTTP[1];


        // Нормалізуємо шлях
        String[] pathParts = path.replaceFirst("^/+", "").split("/");

        if (pathParts.length < 2){
            sendHttpNotFound(socket, "length of path is less than 2");
            Logger.error("length of path is less than 2");
            return;
        }

        if (pathParts[0].equals("notifications")){
            if (pathParts[1].equals("put")){
                Logger.info("Putting notifications from client " + clientID);
                putNotificationFromClient(socket, db, parsedHTTP[5], clientID);
                Logger.info("Putting notifications from client " + clientID + " finished");
            } else {
                sendHttpNotFound(socket);
            }
        }

    }

    public static void  putNotificationFromClient(Socket socket, DataBaseWrapper db, String jsonBody, int clientID) {
        //jsonBody - bites of an array of notifications


        Logger.info("Putting notifications from client " + clientID);

        if (jsonBody == null || jsonBody.isEmpty()) {
            Logger.error("jsonBody is empty");
            sendHttpOk(socket, "but jsonBody is empty");
            return;
        }

        ArrayList<Integer> webIds = db.addNotifications(fromByte2Array(jsonBody, clientID), clientID);

        Logger.info("WebIds = " + webIds);

        try(OutputStream out = socket.getOutputStream()){
            JSONObject payload;
            String statusLine;

            
            payload = new JSONObject()
                    .put("status", "ok")
                    .put("clientId", clientID)
                    .put("webIds", webIds);
            statusLine = "HTTP/1.1 200 OK\r\n";
            

            byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);

            String headers =
                    statusLine +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(bodyBytes);
            out.flush();
        } catch (Exception e) {
            Logger.error("putting notifications failed: " + e.getMessage());
        }
    }


}

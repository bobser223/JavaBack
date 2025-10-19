package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.structures.NotificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.Socket;
import java.util.ArrayList;

public class PostHandler {
    int CLIENT_ID = 1234;


    void handlePost(Socket socket, DataBaseWrapper db, String jsonBody) {

    }


    ArrayList<NotificationInfo> postNotificationFromClient(Socket socket, DataBaseWrapper db, String jsonBody) {
        //jsonBody - bites of an array of notifications

        ArrayList<NotificationInfo> notifications = new ArrayList<>();

        JSONArray array = new JSONArray(jsonBody);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            NotificationInfo n = new NotificationInfo(this.CLIENT_ID,
                    obj.getInt("id"),
                    obj.getString("title"),
                    obj.getString("payload"),
                    obj.getLong("fireAt")
            );

            notifications.add(n);
        }

        return notifications;

        db.
    }
}

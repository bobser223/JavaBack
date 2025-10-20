package org.example.db;

import org.example.structures.NotificationInfo;

import java.util.ArrayList;

public class DataBaseWrapper { //TODO: implement database wrapper
    public static void main(String[] args) {}

    public ArrayList<NotificationInfo> getDbForClient(int clientID) {

        NotificationInfo n = new NotificationInfo(clientID, 1, "title", "payload", 123456789);
        ArrayList<NotificationInfo> notifications = new ArrayList<>();
        notifications.add(n);

        return notifications;
    }

    public void putNotifications(ArrayList<NotificationInfo> notifications, int clientID) {

    }

}

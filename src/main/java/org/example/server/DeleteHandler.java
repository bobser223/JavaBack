package org.example.server;

import static org.example.parsers.JsonParser.parseJson2NotificationsId;
import static org.example.parsers.JsonParser.parseJsons2Usernames;
import static org.example.server.HttpServer.sendHttpNotFound;
import static org.example.server.HttpServer.sendHttpOk;

import java.net.Socket;
import java.util.ArrayList;
import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;

public class DeleteHandler {

  public static void handleDelete(Socket socket, DataBaseWrapper db, String[] parsedHTTP) {
    handleDelete(socket, db, parsedHTTP, 1);
  }

  public static void handleDelete(
      Socket socket, DataBaseWrapper db, String[] parsedHTTP, int userStatus) {
    Logger.info("Deleting ...");

    String path = parsedHTTP[1];

    // Нормалізуємо шлях
    String[] pathParts = path.replaceFirst("^/+", "").split("/");

    if (pathParts.length < 3) {
      sendHttpNotFound(socket, "length of path is less than 3");
      Logger.error("length of path is less than 3");
      return;
    }

    if (pathParts[0].equals("users") && pathParts[1].equals("delete")) {

      if (pathParts[2].equals("superuser")) {
        if (userStatus != 2) {
          sendHttpNotFound(socket, "You are not superuser");
          Logger.warn("User is not superuser");
          return;
        }

        ArrayList<String> usernames = parseJsons2Usernames(parsedHTTP[5]);
        for (String username : usernames) {
          Logger.info("Deleting user " + username);
          db.removeClient(username, "");
          Logger.info("Deleting user " + username + " finished");
        }

        sendHttpOk(socket, "Users " + usernames.toString() + " deleted");
        Logger.info("Deleting users finished");

      } else {
        sendHttpNotFound(socket, "Unknown path");
        Logger.warn("Unknown path");
        return;
      }

    } else if (pathParts[0].equals("notifications") && pathParts[1].equals("delete")) {
      ArrayList<Integer> notificationsToDelete = parseJson2NotificationsId(parsedHTTP[5]);

      if (pathParts[2].equals("manually")) {
        Logger.info("Deleting notifications manually");
        int userID = db.getClientID(parsedHTTP[3], parsedHTTP[4]);

        for (Integer notificationId : notificationsToDelete)
          db.removeNotification(
              userStatus == 2 ? -1 : userID, notificationId); // TODO: make only userID

        sendHttpOk(socket, "Notifications deleted");
        Logger.info("Deleting notifications manually finished");
      } else if (pathParts[2].equals("superuser")) {
        if (userStatus != 2) {
          sendHttpNotFound(socket, "You are not superuser");
          Logger.warn("User is not superuser");
          return;
        }
        Logger.info("Deleting notifications by superuser");
        for (Integer notificationId : notificationsToDelete)
          db.removeNotification(-1, notificationId);
        Logger.info("Deleting notifications by superuser finished");
        sendHttpOk(socket, "Notifications deleted");

      } else {
        sendHttpNotFound(socket, "Unknown path");
        Logger.warn("Unknown path");
        return;
      }

    } else {
      sendHttpNotFound(socket, "Unknown path");
      Logger.warn("Unknown path");
    }
  }
}

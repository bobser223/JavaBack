package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;

import java.net.Socket;

import static org.example.server.HttpServer.sendHttpNotFound;

public class DeleteHandler {

    public void handleDelete(Socket socket, DataBaseWrapper db, String[] parsedHTTP) {
        handleDelete(socket, db, parsedHTTP, 1);
    }

    public void handleDelete(Socket socket, DataBaseWrapper db, String[] parsedHTTP, int userStatus) { //TODO: make the method static
        Logger.info("Deleting ...");

        String path = parsedHTTP[1];

        // Нормалізуємо шлях
        String[] pathParts = path.replaceFirst("^/+", "").split("/");

        if (pathParts.length < 3) {
            sendHttpNotFound(socket, "length of path is less than 2");
            Logger.error("length of path is less than 2");
            return;
        }

        if (pathParts[0].equals("users") && pathParts[1].equals("delete")) {

            if (pathParts[2].equals("superuser")) {
                if (userStatus != 2)
                {
                    sendHttpNotFound(socket, "You are not superuser");
                    Logger.warn("User is not superuser");
                    return;
                }

                    // TODO: import logic from PostHandler
                }
            else
            {
                sendHttpNotFound(socket, "Unknown path");
                Logger.warn("Unknown path");
                return;
            }

        } else if (pathParts[0].equals("notifications") && pathParts[1].equals("delete")) {

            if
            (pathParts[2].equals("manually")) {


            }
            else if (pathParts[2].equals("superuser"))
            {

            }
            else
            {
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

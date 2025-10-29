package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;
import org.example.structures.NotificationInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.Socket;
import java.util.ArrayList;

import static org.example.parsers.JsonParser.parseJson2Auth;
import static org.example.server.HttpServer.*;

public class PostHandler {

    public static void handlePost(Socket socket, DataBaseWrapper db, String[] parsedHTTP) {
        handlePost(socket, db, parsedHTTP, 1);
    }

    public static void handlePost(Socket socket, DataBaseWrapper db, String[] parsedHTTP, int userStatus) { //TODO: make the method static
        Logger.info("Putting notifications from client ");

        String path = parsedHTTP[1];


        String[] pathParts = path.replaceFirst("^/+", "").split("/");

        if (pathParts.length < 2){
            sendHttpNotFound(socket, "length of path is less than 2");
            Logger.error("length of path is less than 2");
            return;
        }


        if (pathParts[0].equals("users")){
            if (pathParts[1].equals("add")){
                if (pathParts.length < 3){
                    sendHttpNotFound(socket, "Your path is too short");
                    Logger.warn("User's path is too short");
                    return;
                }

                String[] authData = parseJson2Auth(parsedHTTP[5]);

                if (Authenticator.isAuthenticated(authData[0], authData[1], db) != 0){
                    sendHttpAccepted(socket, "User already exists");
                    return;
                }

                if (pathParts[2].equals("manually")){
                    Logger.info("Adding user " + authData[0]);
                    Logger.info("Admin status = " + authData[2]);
                    db.addClient(authData[0], authData[1], 0);
                    Logger.info("Adding user " + authData[0] + " finished");

                    sendHttpOk(socket, "user " + authData[0] + " added");
                }

                else if (pathParts[2].equals("superuser")){
                    if (userStatus != 2){
                        sendHttpAuthError(socket, "You are not superuser");
                        Logger.warn("User is not superuser");
                        return;
                    }

                    Logger.info("Adding user " + authData[0]);
                    Logger.info("Admin status = " + authData[2]);
                    db.addClient(authData[0], authData[1], authData[2].equals("1") ? 1 : 0);
                    Logger.info("Adding user " + authData[0] + " finished");

                    sendHttpOk(socket, "user " + authData[0] + " added");

                } else {
                    sendHttpNotFound(socket, "Unknown path");
                    Logger.warn("Unknown path");
                    return;
                }


            }
            else {
                Logger.warn("Unknown path");
                sendHttpNotFound(socket, "Unknown path");
                return;
            }
        } else {
            Logger.warn("Unknown path");
            sendHttpNotFound(socket, "Unknown path");
        }


    }



}

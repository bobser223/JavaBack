package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;
import org.json.JSONObject;

import java.net.Socket;

import static org.example.server.HttpServer.*;

public class PostHandler {

    public void handlePost(Socket socket, DataBaseWrapper db, String[] parsedHTTP) { //TODO: make the method static
        Logger.info("Putting notifications from client ");

        String path = parsedHTTP[1];

        // Нормалізуємо шлях
        String[] pathParts = path.replaceFirst("^/+", "").split("/");

        if (pathParts.length < 2){
            sendHttpNotFound(socket, "length of path is less than 2");
            Logger.error("length of path is less than 2");
            return;
        }


        if (pathParts[0].equals("users")){
            if (pathParts[1].equals("add")){

                String[] authData = parseJson2Auth(parsedHTTP[5]);

                if (Authenticator.isAuthenticated(authData[0], authData[1], db) != 0){
                    sendHttpAccepted(socket, "User already exists");
                    return;
                }

                Logger.info("Adding user " + authData[0]);
                Logger.info("Admin status = " + authData[2]);
                db.addClient(authData[0], authData[1], Integer.parseInt(authData[2])); //BUG: user can add an admin!!!
                Logger.info("Adding user " + authData[0] + " finished");

                sendHttpOk(socket, "user " + authData[0] + " added");
            }
        }


    }



    static String[] parseJson2Auth(String jsonBody) {
        /*
        {
            "username": "volodymyr",
            "password": "secret123",
            "isAdmin": 1 -shit
        }
         */

        String[] authData = new String[3];  // Array to hold username and password

        try {

            // Parse the string into a JSONObject
            JSONObject obj = new JSONObject(jsonBody);

            // Extract the username and password
            String username = obj.getString("username");
            String password = obj.getString("password");
            String isAdmin = String.valueOf(obj.getInt("isAdmin"));


            Logger.info("username -> " + username);
            Logger.info("password -> " + password);
            Logger.info("isAdmin -> " + isAdmin);

            // Store the values in the array
            authData[0] = username;
            authData[1] = password;
            authData[2] = isAdmin;
        } catch (Exception e) {
            Logger.error("Parsing jsonBody failed: " + e.getMessage());
        }

        return authData;
    }
}

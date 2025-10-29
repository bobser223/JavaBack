package org.example.parsers;

import org.example.logger.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class JsonParser {

    public static String[] parseJson2Auth(String jsonBody) {
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

    public static ArrayList<String> parseJsons2Usernames(String jsonBody){

        ArrayList<String> usernames = new ArrayList<>();

        JSONArray array = new JSONArray(jsonBody);
        for (int i = 0; i < array.length(); i++) {

            usernames.add(array.getJSONObject(i).getString("username"));
        }

        return usernames;

    }

    public static ArrayList<Integer> parseJson2NotificationsId(String jsonBody){
        /*
        [
        {"notificationId": 1"},
        {"notificationId": 2"}
        ]


         */
        ArrayList<Integer> usernames = new ArrayList<>();

        JSONArray array = new JSONArray(jsonBody);
        for (int i = 0; i < array.length(); i++) {

            usernames.add(array.getJSONObject(i).getInt("notificationId"));
        }

        return usernames;
    }
}

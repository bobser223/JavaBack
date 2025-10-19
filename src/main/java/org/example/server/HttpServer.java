package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.structures.NotificationInfo;

import java.io.*;
import java.net.*;


import org.json.JSONArray;
import org.json.JSONObject;


import java.util.ArrayList;

public class HttpServer {

    int CLIENT_ID = 1234;

    String host = "1488";
    static int port = 1488;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket socket = server.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }


    static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // Зчитуємо перший рядок (HTTP-запит)
            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);

            if (requestLine.startsWith("GET /")) {

            } else if (requestLine.startsWith("PUT /")) {

            }  else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                out.flush();
                return;
            }

            // Пропускаємо заголовки
            while (in.ready()) in.readLine();


            // Відповідь
            String response = """
                HTTP/1.1 200 OK
                Content-Type: text/plain
                Content-Length: 13

                Hello, pipka!
                """;
            out.write(response.getBytes());
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
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

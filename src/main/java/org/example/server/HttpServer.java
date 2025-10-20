package org.example.server;

import org.example.db.DataBaseWrapper;

import java.io.*;
import java.net.*;


public class HttpServer {

    int CLIENT_ID = 1234;

    String host = "1488";
    static int port = 1488;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        DataBaseWrapper db = new DataBaseWrapper();
        Authenticator au = new Authenticator();

        while (true) {
            Socket socket = server.accept();
            new Thread(() -> handleClient(socket, db, au)).start();
        }
    }

    static void sendHttpNotFound(Socket socket, String message) {
        try (OutputStream out = socket.getOutputStream()) {
            byte[] body = message.getBytes();
            String response = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(response.getBytes());
            out.write(body);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendHttpNotFound(Socket socket) {
        sendHttpNotFound(socket, "");
    }


    static void sendHttpAuthError(Socket socket, String message) {
        try (OutputStream out = socket.getOutputStream()) {
            byte[] body = message.getBytes();
            String response = "HTTP/1.1 401 Unauthorized\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(response.getBytes());
            out.write(body);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendHttpAuthError(Socket socket) {
        sendHttpAuthError(socket, "");
    }

    public static String[] getBasicInfo(BufferedReader in, Socket socket) throws IOException {
        String requestLine = in.readLine();  // e.g. "PUT /notifications/add HTTP/1.1"
        String[] parts = requestLine.split(" "); // split by spaces

        String method = parts[0]; // "PUT"
        String path   = parts[1]; // "/notifications/get"
        String version = parts.length > 2 ? parts[2] : ""; // "HTTP/1.1"

        in.readLine(); // skip host
        String[] auth = in.readLine().split(" ");
        if (auth.length != 3){
            sendHttpAuthError(socket, "wrong parameters length");
        }

        String username = auth[1];
        String password = auth[2];

        return new String[]{method, path, version, username, password};
    } //fixme: shity parser


    static void handleClient(Socket socket, DataBaseWrapper db, Authenticator au) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            String[] basicInfo = getBasicInfo(in, socket);


                                                      //user        password
            byte isAuthenticated = au.isAuthenticated(basicInfo[3], basicInfo[4], db);
            if (isAuthenticated == 0) {
                sendHttpAuthError(socket, "wrong credentials");
                return;
            }


                //method
            if (basicInfo[0].equals("GET")) {
                System.out.println("GET");
                        //method
            } else if (basicInfo[0].equals("PUT /")) {
                System.out.println("PUT");
            }  else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                out.flush();
                return;
            }

            // Пропускаємо заголовки
            while (in.ready())
                System.out.println(in.readLine());


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
}

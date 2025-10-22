package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;

import java.io.*;
import java.net.*;




public class HttpServer {


    String host = "1488";
    static int port = 1488;
    static boolean isRunning = true;

    public static void main(String[] args){
        try (ServerSocket server = new ServerSocket(port)){
            System.out.println("Server started on port " + port);

            DataBaseWrapper db = new DataBaseWrapper();
            DataBaseWrapper.demo(db);



            while (isRunning) {
                Socket socket = server.accept();
                new Thread(() -> handleClient(socket, db)).start();
            }
        } catch (IOException e) {
            Logger.error("Error while starting server: " + e.getMessage());
        }

    }

    static void handleClient(Socket socket, DataBaseWrapper db) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            String[] parsedHTTP = HttpParser.parseHTTP(in, socket);


                                                                 //user        password
            byte isAuthenticated = Authenticator.isAuthenticated(parsedHTTP[3], parsedHTTP[4], db);
            if (isAuthenticated == 0) {
                sendHttpAuthError(socket, "wrong credentials (authenticator)");
                return;
            }
            int clientID = db.getClientID(parsedHTTP[3], parsedHTTP[4]);
            if (clientID == -1) {
                sendHttpAuthError(socket, "wrong credentials (id finder)");
                return;
            }

            for (String parts: parsedHTTP){
                System.out.println("Info " + parts);
            }

                //method
            if (parsedHTTP[0].equals("GET")) {
                Logger.info("GET");

                GetHandler getHandler = new GetHandler();
                getHandler.handleGet(socket, db, parsedHTTP);

                return;

                      //method
            } else if (parsedHTTP[0].equals("PUT")) {
                Logger.info("PUT");
                PutHandler putHandler = new PutHandler();
                putHandler.handlePut(socket, db, clientID, parsedHTTP);
                return;
            }  else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                out.flush();
                return;
            }
        } catch (IOException e) {
            Logger.error("Error while handling client: " + e.getMessage());
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
            Logger.error("Error while sending 404: " + e.getMessage());
        }
    }

    static void sendHttpNotFound(Socket socket) {
        sendHttpNotFound(socket, "");
    }

    static void sendHttpOk(Socket socket, String message){
        try (OutputStream out = socket.getOutputStream()) {
            byte[] body = message.getBytes();
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            out.write(response.getBytes());
            out.write(body);
            out.flush();
        } catch (IOException e) {
            Logger.error("Error while sending 200: " + e.getMessage());
        }
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
            Logger.error("Error while sending 401: " + e.getMessage());
        }

    }

    static void sendHttpAuthError(Socket socket) {
        sendHttpAuthError(socket, "");
    }

}

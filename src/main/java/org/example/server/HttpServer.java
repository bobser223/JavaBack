package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;

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
        DataBaseWrapper.demo(db);



        while (true) {
            Socket socket = server.accept();
            new Thread(() -> handleClient(socket, db)).start();
        }
    }

    static void handleClient(Socket socket, DataBaseWrapper db) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            String[] basicInfo = parseHTTP(in, socket);


                                                                 //user        password
            byte isAuthenticated = Authenticator.isAuthenticated(basicInfo[3], basicInfo[4], db);
            if (isAuthenticated == 0) {
                sendHttpAuthError(socket, "wrong credentials");
                return;
            }

            for (String parts: basicInfo){
                System.out.println("Info " + parts);
            }

            //method
            if (basicInfo[0].equals("GET")) {
                System.out.println("GET");

                GetHandler getHandler = new GetHandler();
                getHandler.handleGet(socket, db, in, basicInfo);

                socket.close();
                return;

                //method
            } else if (basicInfo[0].equals("PUT")) {
                System.out.println("PUT");
                PutHandler putHandler = new PutHandler();

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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    static void sendHttpAuthError(Socket socket) {
        sendHttpAuthError(socket, "");
    }

    static String[] parseHTTP(BufferedReader in, Socket socket) throws IOException {
        String requestLine = in.readLine();  // "GET /... HTTP/1.1"
        String[] parts = requestLine.split(" ", 3);
        String method  = parts[0];
        String path    = parts.length > 1 ? parts[1] : "/";
        String version = parts.length > 2 ? parts[2] : "";

        String authorization = null;

        // Читаємо заголовки до порожнього рядка
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.regionMatches(true, 0, "Authorization:", 0, "Authorization:".length())) {
                authorization = line.substring("Authorization:".length()).trim(); // "Basic <b64>"
            }
        }

        if (authorization == null || !authorization.startsWith("Basic ")) {
            sendHttpAuthError(socket, "Missing Basic auth");
            return new String[]{method, path, version, "", ""};
        }

        String b64 = authorization.substring("Basic ".length()).trim();
        String userPass = new String(java.util.Base64.getDecoder().decode(b64), java.nio.charset.StandardCharsets.UTF_8);

        // розбиваємо тільки по першій двокрапці
        int colon = userPass.indexOf(':');
        if (colon < 0) {
            sendHttpAuthError(socket, "Malformed credentials");
            return new String[]{method, path, version, "", ""};
        }
        String username = userPass.substring(0, colon);
        String password = userPass.substring(colon + 1);

        Logger.info("parced HTTP request: " + method + " " + path + " " + version + " " + username + " " + password);

        return new String[]{method, path, version, username, password};
    }




}

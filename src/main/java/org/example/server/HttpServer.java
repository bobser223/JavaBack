package org.example.server;

import static org.example.parsers.HttpParser.parseHTTP;

import java.io.*;
import java.net.*;
import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;

public class HttpServer {

  String host = "1488";
  static int port = 1488;
  static boolean isRunning = true;

  public static void main(String[] args) {
    try (ServerSocket server = new ServerSocket(port)) {
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

      String[] parsedHTTP = parseHTTP(in, socket);

      int[] AuthAndId = produceAuthAndGetId(parsedHTTP[3], parsedHTTP[4], db);
      int isAuthenticated = AuthAndId[0];
      int clientID = AuthAndId[1];

      for (String parts : parsedHTTP) {
        Logger.info(parts);
      }

      // method
      switch (parsedHTTP[0]) {
        case "GET" -> {
          if (isAuthenticated == 0) {
            sendHttpAuthError(socket, "wrong credentials (authenticator)");
            return;
          }

          Logger.info("GET");
          GetHandler.handleGet(socket, db, parsedHTTP);

          Logger.info("############### GET finished ################");
        }

        // method
        case "PUT" -> {
          if (isAuthenticated == 0) {
            sendHttpAuthError(socket, "wrong credentials (authenticator)");
            return;
          }

          Logger.info("PUT");
          PutHandler.handlePut(socket, db, clientID, parsedHTTP);

          Logger.info("############### PUT finished ################");
        }
        case "POST" -> {
          Logger.info("POST");
          PostHandler.handlePost(socket, db, parsedHTTP, isAuthenticated);

          Logger.info("############## POST finished ################");
        }
        case "DELETE" -> {
          Logger.info("DELETE");
          DeleteHandler.handleDelete(socket, db, parsedHTTP, isAuthenticated);
          Logger.info("############## DELETE finished ################");
        }
        default -> {
          out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
          out.flush();
          Logger.info("############## Method not found ###################");
        }
      }
    } catch (IOException e) {
      Logger.error("Error while handling client: " + e.getMessage());
    }
  }

  static int[] produceAuthAndGetId(String name, String pass, DataBaseWrapper db) {
    byte isAuthenticated = Authenticator.isAuthenticated(name, pass, db);

    if (isAuthenticated == 0) return new int[] {isAuthenticated, -1};
    int id = db.getClientID(name, pass);
    return new int[] {isAuthenticated, id};
  }

  static void sendHttpNotFound(Socket socket, String message) {
    try (OutputStream out = socket.getOutputStream()) {
      byte[] body = message.getBytes();
      String response =
          "HTTP/1.1 404 Not Found\r\n"
              + "Content-Type: text/plain\r\n"
              + "Content-Length: "
              + body.length
              + "\r\n"
              + "Connection: close\r\n"
              + "\r\n";

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

  static void sendHttpOk(Socket socket, String message) {
    try (OutputStream out = socket.getOutputStream()) {
      byte[] body = message.getBytes();
      String response =
          "HTTP/1.1 200 OK\r\n"
              + "Content-Type: text/plain\r\n"
              + "Content-Length: "
              + body.length
              + "\r\n"
              + "Connection: close\r\n"
              + "\r\n";

      out.write(response.getBytes());
      out.write(body);
      out.flush();
    } catch (IOException e) {
      Logger.error("Error while sending 200: " + e.getMessage());
    }
  }

  public static void sendHttpAuthError(Socket socket, String message) {
    try (OutputStream out = socket.getOutputStream()) {
      byte[] body = message.getBytes();
      String response =
          "HTTP/1.1 401 Unauthorized\r\n"
              + "Content-Type: text/plain\r\n"
              + "Content-Length: "
              + body.length
              + "\r\n"
              + "Connection: close\r\n"
              + "\r\n";

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

  static void sendHttpAccepted(Socket socket, String message) {
    try (OutputStream out = socket.getOutputStream()) {
      byte[] body = message.getBytes();
      String response =
          "HTTP/1.1 202 ACCEPTED\r\n"
              + "Content-Type: text/plain\r\n"
              + "Content-Length: "
              + body.length
              + "\r\n"
              + "Connection: close\r\n"
              + "\r\n";

      out.write(response.getBytes());
      out.write(body);
      out.flush();
    } catch (IOException e) {
      Logger.error("Error while sending 202: " + e.getMessage());
    }
  }

  static void sendHttpAccepted(Socket socket) {
    sendHttpAccepted(socket, "");
  }
}

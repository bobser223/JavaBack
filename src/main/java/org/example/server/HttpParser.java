package org.example.server;

import org.example.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

import static org.example.server.HttpServer.sendHttpAuthError;

public class HttpParser {
    static String[] parseHTTP(BufferedReader in, Socket socket) throws IOException {
        String requestLine = in.readLine();  // "GET /... HTTP/1.1"
        if (requestLine == null) {
            sendHttpAuthError(socket, "Empty request");
            return new String[]{"", "/", "", "", "", ""};
        }

        String[] parts = requestLine.split(" ", 3);
        String method  = parts.length > 0 ? parts[0] : "";
        String path    = parts.length > 1 ? parts[1] : "/";
        String version = parts.length > 2 ? parts[2] : "";

        String authorization = null;
        Integer contentLength = null;
        boolean chunked = false;

        // Читаємо заголовки до порожнього рядка
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            // Authorization
            if (line.regionMatches(true, 0, "Authorization:", 0, "Authorization:".length())) {
                authorization = line.substring("Authorization:".length()).trim(); // "Basic <b64>"
            }
            // Content-Length
            if (line.regionMatches(true, 0, "Content-Length:", 0, "Content-Length:".length())) {
                try {
                    contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                } catch (NumberFormatException ignore) {}
            }
            // Transfer-Encoding
            if (line.regionMatches(true, 0, "Transfer-Encoding:", 0, "Transfer-Encoding:".length())) {
                String v = line.substring("Transfer-Encoding:".length()).trim();
                if (v.toLowerCase().contains("chunked")) chunked = true;
            }
        }

        if (authorization == null || !authorization.startsWith("Basic ")) {
            sendHttpAuthError(socket, "Missing Basic auth");
            return new String[]{method, path, version, "", "", ""};
        }

        String b64 = authorization.substring("Basic ".length()).trim();
        String userPass = new String(java.util.Base64.getDecoder().decode(b64), java.nio.charset.StandardCharsets.UTF_8);

        // розбиваємо тільки по першій двокрапці
        int colon = userPass.indexOf(':');
        if (colon < 0) {
            sendHttpAuthError(socket, "Malformed credentials");
            return new String[]{method, path, version, "", "", ""};
        }
        String username = userPass.substring(0, colon);
        String password = userPass.substring(colon + 1);

        // Зчитуємо тіло запиту (JSON, текст тощо)
        String body = "";
        if (chunked) {
            body = readChunkedBody(in);
        } else if (contentLength != null && contentLength > 0) {
            body = readFixedLengthBody(in, contentLength);
        } else {
            // для деяких клієнтів можуть надсилатись тіла й без цих заголовків — тут
            // краще не блокуватись; залишимо порожнім.
        }

        Logger.info("parsed HTTP request: " + method + " " + path + " " + version + " " + username);

        return new String[]{method, path, version, username, password, body};
    }

    private static String readFixedLengthBody(BufferedReader in, int length) throws IOException {
        // Content-Length — це байти, але ми читаємо символи.
        // Для чистого ASCII/UTF-8 JSON зазвичай збігається. Для 100% точності краще читати з InputStream.
        char[] buf = new char[length];
        int off = 0;
        while (off < length) {
            int r = in.read(buf, off, length - off);
            if (r < 0) break;
            off += r;
        }
        return new String(buf, 0, off);
    }

    private static String readChunkedBody(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String sizeLine = in.readLine();           // розмір у шістнадцятковому вигляді
            if (sizeLine == null) break;
            int semicolon = sizeLine.indexOf(';');     // відкидаємо chunk-extensions
            String hex = (semicolon >= 0) ? sizeLine.substring(0, semicolon) : sizeLine;
            int size = Integer.parseInt(hex.trim(), 16);
            if (size == 0) {
                // зчитуємо можливі трейлери до порожнього рядка
                String trailer;
                while ((trailer = in.readLine()) != null && !trailer.isEmpty()) { /* ignore trailers */ }
                break;
            }
            // читаємо рівно size символів + CRLF після чанку
            char[] buf = new char[size];
            int off = 0;
            while (off < size) {
                int r = in.read(buf, off, size - off);
                if (r < 0) throw new IOException("Unexpected end of stream in chunked body");
                off += r;
            }
            sb.append(buf, 0, size);
            // з’їдаємо CRLF після чанку
            in.readLine();
        }
        return sb.toString();
    }

}

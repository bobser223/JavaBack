package org.example.server;

import org.example.db.DataBaseWrapper;
import org.example.logger.Logger;

public class Authenticator {

    static byte isAuthenticated(String username, String password, DataBaseWrapper db) { //TODO:

        // 0 - failed authentication
        // 1 - ok
        // 2 - superuser
        Logger.info("Authenticating: username -> " + username + " ;password -> " + password);
       return db.findClient(username, password) == -1 ? 0: (byte) db.findClient(username, password);

        //return 1;
    }



}

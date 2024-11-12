package com.solv.websockets;

import javax.enterprise.context.Dependent;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@Dependent
@ServerEndpoint(value = "/push")
public class PushEndpoint {

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    private static final Logger LOGGER = Logger.getLogger(PushEndpoint.class.getName());

    @OnOpen
    public void onOpen(Session session) {

        sessions.add(session);
        LOGGER.info(String.format("Session with id %s opened", session.getId()));
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.info(String.format("Session with id %s closed", session.getId()));
    }

    @OnError
    public void error(Session session, Throwable error) {
        LOGGER.info("ERROR");
        LOGGER.info("SessionId " + session.getId());
        LOGGER.info("ErrorMsg " + error.getMessage());
    }

    /**
     * Broadcasts the message to all sessions.
     *
     */
    public static void broadcast(String message) {

        LOGGER.info(String.format("Broadcasting \"%s\" to %d sessions", message, sessions.size()));

        try {
            for (Session session1 : sessions) {
                if (session1.isOpen())
                    session1.getBasicRemote().sendText("Message from server: " + message);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
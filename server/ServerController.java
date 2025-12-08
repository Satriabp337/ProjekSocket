package server;

import common.Message;
import common.MessageType;
import java.util.HashMap;

public class ServerController {
    // simpan datauser
    private static HashMap<String, ClientHandler> onlineUsers = new HashMap<>();

    public static synchronized void addUser(String username, ClientHandler handler) {
        onlineUsers.put(username, handler);
        System.out.println("[SERVER] User registered: " + username);

        broadcastMessage("Server ", username + " has joined the chat.");

        broadcastUserList();
    }

    public static synchronized void removeUser(String username) {
        if (onlineUsers.containsKey(username)) {
            onlineUsers.remove(username);
            System.out.println("[SERVER] user removed : " + username);
            broadcastMessage("Server ", username + "has left the chat");

            broadcastUserList();
        }

    }

    public static synchronized void broadcastMessage(String senderName, String textContent) {
        Message msg = new Message(MessageType.BROADCAST_CHAT);
        msg.setSender(senderName);
        msg.setContent(textContent);
        msg.setRecipient("ALL");

        for (ClientHandler client : onlineUsers.values()) {
            client.sendMessage(msg);
        }
    }

    public static synchronized void sendPrivateMessage(String senderName, String recipientName, String textContent) {
        ClientHandler targetClient = onlineUsers.get(recipientName);

        if (targetClient != null) {
            Message msg = new Message(MessageType.PRIVATE_CHAT);
            msg.setSender(senderName);
            msg.setContent(textContent);
            msg.setRecipient(recipientName);

            targetClient.sendMessage(msg);
            System.out.println("[PRIVATE] " + senderName + " -->" + recipientName);
        } else {
            System.out.println("[GAGAL] User " + recipientName + " tidak ditemukan/offline");
        }
    }

    public static synchronized void relayFilePacket(Message msg) {
        String target = msg.getRecipient();

        if (target.equals("ALL")) {
            for (ClientHandler client : onlineUsers.values()) {
                if (!client.getUsername().equals(msg.getSender())) {
                    client.sendMessage(msg);
                }
            }
        } else {
            ClientHandler targetClient = onlineUsers.get(target);
            if (targetClient != null) {
                targetClient.sendMessage(msg);
            } else {
                //
            }
        }
    }

    public static synchronized void sendPrivateFile(String senderName, String recipientName, String fileName,
            byte[] data) {
        ClientHandler targetClient = onlineUsers.get(recipientName);

        if (targetClient != null) {
            Message msg = new Message(MessageType.FILE_REQUEST);
            msg.setSender(senderName);
            msg.setRecipient(recipientName);
            msg.setContent(fileName);
            msg.setFileChunk(data);

            targetClient.sendMessage(msg);
            System.out.println("[FILE PRIV] " + senderName + " sent file '" + fileName + "' to " + recipientName);
        } else {
            System.out.println("[GAGAL] Kirim file gagal. User " + recipientName + "offline");
        }
    }

    public static synchronized void broadcastUserList() {
        // ambil nama user
        StringBuilder sb = new StringBuilder();

        for (String user : onlineUsers.keySet()) {
            sb.append(user).append(",");

        }

        // hapus koma terakhir jika ada
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        String listString = sb.toString();

        Message msg = new Message(MessageType.USER_LIST_UPDATE);
        msg.setSender("Server");
        msg.setRecipient("ALL");
        msg.setContent(listString);

        for (ClientHandler client : onlineUsers.values()) {
            client.sendMessage(msg);
        }
        System.out.println("[SYSTEM] Broadcasting User List : " + listString);
    }

}

package server;

import common.Message;
import common.MessageType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Setup Stream (Urutan: Output dulu baru Input biar gak macet)
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());

            // Loop membaca pesan dari client
            while (socket.isConnected()) {
                Message msg = (Message) in.readObject();

                switch (msg.getType()) {
                    case LOGIN:
                        this.username = msg.getSender();
                        ServerController.addUser(this.username, this);
                        break;

                    case TEXT:
                        String tujuan = msg.getRecipient();

                        if (tujuan.equals("ALL")) {
                            System.out.println("[CHAT] " + msg.getSender() + ": " + msg.getContent());
                            ServerController.broadcastMessage(msg.getSender(), msg.getContent());
                        } else {
                            System.out.println("[CHAT PRIV]" + msg.getSender() + " --> " + tujuan);
                            ServerController.sendPrivateMessage(msg.getSender(), tujuan, msg.getContent());
                        }
                        break;

                    case FILE:
                        String target = msg.getRecipient();
                        String fName = msg.getFileName();
                        byte[] fData = msg.getFileData();

                        if(target.equals("ALL")) {
                            ServerController.broadcastFile(this.username, fName, fData);
                        } else {
                            ServerController.sendPrivateFile(this.username, target, fName, fData);
                        }
                        
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            closeConnection();
        }
    }

    public String getUsername() {
        return this.username;
    }

    public void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        if (username != null) {
            ServerController.removeUser(username);
        }

        try {
            socket.close();
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

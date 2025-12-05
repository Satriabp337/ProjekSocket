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
                // [BLOCKING I/O] Thread diam disini sampai ada pesan masuk
                Message msg = (Message) in.readObject(); 

                // --- ROUTING LOGIC BARU (Sesuai Protokol Novran) ---
                switch (msg.getType()) {
                    
                    // 1. LOGIN diganti jadi CONNECT
                    case CONNECT:
                        this.username = msg.getSender();
                        ServerController.addUser(this.username, this);
                        break;

                    // 2. DISCONNECT (User Keluar)
                    case DISCONNECT:
                        closeConnection();
                        break;

                    // 3. BROADCAST CHAT (Langsung panggil broadcast)
                    case BROADCAST_CHAT:
                        System.out.println("[CHAT ALL] " + msg.getSender() + ": " + msg.getContent());
                        ServerController.broadcastMessage(msg.getSender(), msg.getContent());
                        break;

                    // 4. PRIVATE CHAT (Langsung panggil private)
                    case PRIVATE_CHAT:
                        String targetUser = msg.getRecipient();
                        System.out.println("[CHAT PRIV] " + msg.getSender() + " -> " + targetUser);
                        ServerController.sendPrivateMessage(msg.getSender(), targetUser, msg.getContent());
                        break;
                        
                    // 5. FILE REQUEST (Header File / Pengiriman File Simple)
                    case FILE_REQUEST:
                        String target = msg.getRecipient();
                        String fName = msg.getFileName();
                        byte[] fData = msg.getFileChunk(); 
                        
                        // Logika Routing File
                        if (target.equals("ALL")) {
                            ServerController.broadcastFile(this.username, fName, fData);
                        } else {
                            ServerController.sendPrivateFile(this.username, target, fName, fData);
                        }
                        break;

                    // 6. BUZZ (Fitur Getar)
                    case BUZZ:
                        // Server hanya meneruskan (Forwarding) signal Buzz
                        // Kalau targetnya ALL -> Broadcast
                        // Kalau targetnya Nama -> Private
                        String buzzTarget = msg.getRecipient();
                        if ("ALL".equals(buzzTarget)) {
                            // Untuk saat ini kita pakai broadcastMessage dulu atau bikin method khusus
                            // Kita pakai broadcastMessage dengan konten khusus "BUZZ"
                            ServerController.broadcastMessage(msg.getSender(), "BUZZ");
                        } else {
                            ServerController.sendPrivateMessage(msg.getSender(), buzzTarget, "BUZZ");
                        }
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

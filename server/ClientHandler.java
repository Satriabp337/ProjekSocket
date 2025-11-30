package server;

import common.Message;
import common.MessageType;
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
                Message msg = (Message) in.readObject(); // BACA PESAN

                // --- LOGIKA UTAMA SATRIA DISINI ---
                if (msg.getType() == MessageType.LOGIN) {
                    this.username = msg.getSender();
                    ServerMain.listClients.put(this.username, this);
                    System.out.println("[LOGIN] User terdaftar: " + username);
                } 
                else if (msg.getType() == MessageType.TEXT) {
                    System.out.println("[CHAT] " + msg.getSender() + ": " + msg.getContent());
                    // TODO: Satria harus bikin kode broadcast disini nanti
                }
            }
        } catch (Exception e) {
            // Handle disconnect
            System.out.println("[DISCONNECT] " + username + " keluar.");
            ServerMain.listClients.remove(username);
        }
    }
}

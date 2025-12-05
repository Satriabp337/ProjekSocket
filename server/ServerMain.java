package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServerMain {
    private static final int PORT = 50125;

    // Hashmap untuk menyimpan User yang Online (Kunci Routing!)
    public static HashMap<String, ClientHandler> listClients = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Sedang berjalan di port " + PORT + "...");

            while (true) {
                // 1. Tunggu ada yang connect
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] Ada klien baru masuk!");

                // 2. Buat thread pelayan baru untuk klien ini
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
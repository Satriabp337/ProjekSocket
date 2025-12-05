package client;

import common.Message;
import common.MessageType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import javax.swing.SwingUtilities;

/**
 * Menangani koneksi Socket, pengiriman, dan penerimaan pesan dari server.
 * Ini adalah thread yang bertanggung jawab atas operasi I/O jaringan.
 */
public class ClientService {

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ClientMain gui; // Referensi ke GUI utama untuk update

    // --- STATE MANAGEMENT UNTUK FILE RECEIVING ---
    private FileOutputStream currentFileWriter;
    private String receivingFileName;
    private long totalBytesReceived = 0;
    private long expectedFileSize = 0;
    // ---------------------------------------------

    // --- Konstruktor & Koneksi ---

    public ClientService(ClientMain gui) {
        this.gui = gui;
    }

    /**
     * Membangun koneksi ke server. Harus dipanggil di thread non-EDT.
     */
    public boolean connect(String host, int port, String username) {
        try {
            // 1. Inisialisasi Socket dan Streams
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            // Flush header output-stream sebelum membuat input-stream (Penting!)
            output.flush(); 
            input = new ObjectInputStream(socket.getInputStream());
            
            // 2. Kirim pesan CONNECT sebagai handshake
            Message connectMsg = new Message(MessageType.CONNECT);
            connectMsg.setSender(username);
            sendMessage(connectMsg); 
            
            // 3. Start Listener Thread (Penerima Pesan)
            new Thread(new ServerListener(), "ClientListenerThread").start();
            
            return true;

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> gui.logMessage("ERROR: Gagal terhubung: " + e.getMessage()));
            return false;
        }
    }

    // --- Pengiriman Pesan ---

    /**
     * Method sinkron untuk mengirim objek Message ke server.
     * Menggunakan 'synchronized' untuk mencegah masalah penulisan bersamaan.
     */
    public synchronized void sendMessage(Message message) {
        try {
            if (output != null && socket != null && !socket.isClosed()) {
                output.writeObject(message);
                output.flush();
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> gui.logMessage("ERROR: Gagal mengirim pesan. Koneksi mungkin terputus."));
            disconnect();
        }
    }
    
    // Metode Helper untuk Chat Teks
    public void sendTextMessage(String recipient, String content) {
        Message msg = new Message(
            recipient.equalsIgnoreCase("all") ? MessageType.BROADCAST_CHAT : MessageType.PRIVATE_CHAT
        );
        msg.setRecipient(recipient);
        msg.setContent(content);
        msg.setSender(gui.getUsername()); // Set sender eksplisit
        sendMessage(msg);
    }
    
    /**
     * Mengirim pesan BUZZ ke penerima tertentu.
     */
    public void sendBuzz(String recipient) {
        Message msg = new Message(MessageType.BUZZ);
        msg.setRecipient(recipient);
        msg.setSender(gui.getUsername());
        sendMessage(msg);
    }

    /**
     * Mengirim file dengan memecahnya menjadi potongan-potongan (chunks).
     * HARUS dijalankan di thread terpisah.
     */
    public void sendFile(String recipient, File file) {
        if (!file.exists() || !file.isFile()) {
            gui.logMessage("ERROR: File tidak ditemukan atau tidak valid.");
            return;
        }

        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                
                // 1. Kirim Pesan Permintaan File (Header: FILE_REQUEST)
                Message requestMsg = new Message(MessageType.FILE_REQUEST);
                requestMsg.setRecipient(recipient);
                requestMsg.setContent(file.getName()); 
                requestMsg.setFileSize(file.length());
                requestMsg.setSender(gui.getUsername());
                sendMessage(requestMsg);

                gui.logMessage(String.format("⏳ Mengirim file '%s' (%d bytes) ke %s...", 
                    file.getName(), file.length(), recipient));

                byte[] buffer = new byte[8192]; // Ukuran buffer 8KB
                int bytesRead;

                // 2. Kirim Potongan-potongan Data (Chunks: FILE_CHUNK)
                while ((bytesRead = fis.read(buffer)) > 0) {
                    Message chunkMsg = new Message(MessageType.FILE_CHUNK);
                    chunkMsg.setRecipient(recipient);
                    
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);
                    chunkMsg.setFileChunk(data);
                    
                    sendMessage(chunkMsg);
                }

                // 3. Kirim Pesan Konfirmasi Selesai (FILE_COMPLETE)
                Message finishedMsg = new Message(MessageType.FILE_COMPLETE);
                finishedMsg.setRecipient(recipient);
                finishedMsg.setContent(file.getName());
                finishedMsg.setSender(gui.getUsername());
                sendMessage(finishedMsg);
                
                gui.logMessage("✅ Pengiriman file selesai.");

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> gui.logMessage("ERROR saat mengirim file: " + e.getMessage()));
            }
        }, "ClientFileSenderThread").start();
    }


    // --- Penerima Pesan (ServerListener Thread) ---

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                Object receivedObject;
                while (!socket.isClosed() && (receivedObject = input.readObject()) != null) {
                    if (receivedObject instanceof Message) {
                        Message receivedMsg = (Message) receivedObject;
                        handleMessage(receivedMsg);
                    }
                }
            } catch (SocketException e) {
                 if (!"Socket closed".equalsIgnoreCase(e.getMessage())) {
                    SwingUtilities.invokeLater(() -> gui.connectionLost());
                 }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> gui.connectionLost());
            } catch (ClassNotFoundException e) {
                SwingUtilities.invokeLater(() -> gui.logMessage("ERROR: Objek tak dikenal diterima."));
            } finally {
                 disconnect(); 
            }
        }
    }

    /**
     * Memproses objek Message yang diterima dari server.
     */
    private void handleMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case BROADCAST_CHAT:
                    gui.displayMessage(String.format("💬 [%s (BROADCAST)]: %s", msg.getSender(), msg.getContent()));
                    break;
                case PRIVATE_CHAT:
                    gui.displayMessage(String.format("✉️ [%s (PRIVATE)]: %s", msg.getSender(), msg.getContent()));
                    break;
                case USER_LIST_UPDATE:
                    if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                        gui.updateUserList(msg.getContent().split(","));
                    }
                    break;
                case BUZZ:
                    gui.triggerBuzz(msg.getSender()); 
                    break;
                case DISCONNECT:
                    gui.logMessage("Server meminta disconnect. " + msg.getContent());
                    disconnect();
                    break;
                    
                // --- FILE RECEIVER LOGIC ---
                case FILE_REQUEST:
                    this.receivingFileName = msg.getContent();
                    this.expectedFileSize = msg.getFileSize();
                    this.totalBytesReceived = 0;

                    try {
                        File downloadDir = new File("downloads");
                        if (!downloadDir.exists()) downloadDir.mkdir();
                        File outputFile = new File(downloadDir, this.receivingFileName);
                        
                        currentFileWriter = new FileOutputStream(outputFile);
                        gui.logMessage(String.format("Menerima file '%s' (%d bytes) dari %s. Menyimpan ke: %s", 
                            receivingFileName, expectedFileSize, msg.getSender(), outputFile.getAbsolutePath()));
                        
                    } catch (IOException e) {
                        gui.logMessage("ERROR: Gagal membuat file untuk penerimaan: " + e.getMessage());
                    }
                    break;

                case FILE_CHUNK:
                    if (currentFileWriter != null) {
                        // Tulis ke disk di thread terpisah agar Listener Thread (yang membaca dari socket) tidak terblokir
                        new Thread(() -> { 
                            try {
                                byte[] chunk = msg.getFileChunk();
                                if (chunk != null) {
                                    currentFileWriter.write(chunk);
                                    SwingUtilities.invokeLater(() -> totalBytesReceived += chunk.length);
                                }
                            } catch (IOException e) {
                                SwingUtilities.invokeLater(() -> gui.logMessage("ERROR: Gagal menulis chunk file: " + e.getMessage()));
                                try { if (currentFileWriter != null) currentFileWriter.close(); } catch (IOException ignored) {}
                                currentFileWriter = null;
                            }
                        }).start();
                    }
                    break;

                case FILE_COMPLETE:
                    if (currentFileWriter != null) {
                        try {
                            currentFileWriter.close();
                            gui.logMessage(String.format("✅ Penerimaan file '%s' selesai. Total %d bytes.", 
                                receivingFileName, totalBytesReceived));
                            currentFileWriter = null;
                            receivingFileName = null;
                            expectedFileSize = 0;
                            totalBytesReceived = 0;
                        } catch (IOException e) {
                            gui.logMessage("ERROR saat menutup file stream: " + e.getMessage());
                        }
                    }
                    break;
                // ---------------------------------------------------
                case CONNECT:
                    gui.logMessage("Status: Berhasil terhubung. Menunggu User List...");
                    break;
                default:
                    gui.logMessage("Pesan tipe tidak dikenal diterima: " + msg.getType());
            }
        });
    }

    // --- Penutupan Koneksi ---

    /**
     * Menutup koneksi secara bersih.
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                Message disconnectMsg = new Message(MessageType.DISCONNECT);
                disconnectMsg.setSender(gui.getUsername());
                try {
                    output.writeObject(disconnectMsg); 
                    output.flush();
                } catch (Exception ignored) {}
                
                input.close();
                output.close();
                socket.close();
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> gui.logMessage("ERROR saat menutup koneksi: " + e.getMessage()));
        } finally {
             SwingUtilities.invokeLater(() -> gui.connectionClosed());
        }
    }
}
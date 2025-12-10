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
    // Diubah menjadi volatile untuk penulisan antar-thread (Listener Thread vs File
    // Writer Thread)
    private volatile FileOutputStream currentFileWriter;
    private String receivingFileName;
    private volatile long totalBytesReceived = 0;
    private long expectedFileSize = 0;

    // **FITUR BARU: STATE UNTUK FILE SENDING**
    private volatile long totalBytesSent = 0;
    private long expectedFileToSendSize = 0;
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

    // **FITUR BARU: Helper untuk Cek Koneksi**
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
    // ----------------------------------------

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
                recipient.equalsIgnoreCase("all") ? MessageType.BROADCAST_CHAT : MessageType.PRIVATE_CHAT);
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

    // **FITUR BARU: Method Pengiriman Status Mengetik**
    public void sendTypingStart(String recipient) {
        Message msg = new Message(MessageType.TYPING_START);
        msg.setRecipient(recipient);
        msg.setSender(gui.getUsername());
        sendMessage(msg);
    }

    public void sendTypingStop(String recipient) {
        Message msg = new Message(MessageType.TYPING_STOP);
        msg.setRecipient(recipient);
        msg.setSender(gui.getUsername());
        sendMessage(msg);
    }
    // ---------------------------------------------------

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
            // Inisialisasi state pengiriman
            this.expectedFileToSendSize = file.length();
            this.totalBytesSent = 0;

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

                    // **FITUR PROGRESS BAR: UPDATE PENGIRIMAN**
                    totalBytesSent += bytesRead;
                    int percentage = (int) ((totalBytesSent * 100) / expectedFileToSendSize);
                    String statusText = String.format("%d%% Sent", percentage);

                    // Panggil method GUI di Event Dispatch Thread
                    SwingUtilities.invokeLater(() -> gui.updateFileProgress(true, percentage, statusText));
                }

                // **FITUR PROGRESS BAR: Selesai Pengiriman**
                SwingUtilities.invokeLater(() -> gui.updateFileProgress(false, 100, ""));

                // 3. Kirim Pesan Konfirmasi Selesai (FILE_COMPLETE)
                Message finishedMsg = new Message(MessageType.FILE_COMPLETE);
                finishedMsg.setRecipient(recipient);
                finishedMsg.setContent(file.getName());
                finishedMsg.setSender(gui.getUsername());
                sendMessage(finishedMsg);

                gui.logMessage("✅ Pengiriman file selesai.");

            } catch (Exception e) {
                // Sembunyikan progress bar jika ada error
                SwingUtilities.invokeLater(() -> gui.updateFileProgress(false, 0, ""));
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
        // Logika file writing (I/O intensif) harus dijalankan di thread terpisah.
        // Logika GUI (perintah Swing) harus dijalankan di EDT (menggunakan
        // invokeLater).

        switch (msg.getType()) {
            case BROADCAST_CHAT:
                // Panggil method baru GUI yang mendukung Tab
                SwingUtilities.invokeLater(() -> gui.incomingChat(msg.getSender(), msg.getContent(), false));
                break;

            case PRIVATE_CHAT:
                // Panggil method baru GUI yang mendukung Tab
                SwingUtilities.invokeLater(() -> gui.incomingChat(msg.getSender(), msg.getContent(), true));
                break;

            // **FITUR BARU: Handle Typing Indicator**
            case TYPING_START:
                String typingSender = msg.getSender();
                SwingUtilities.invokeLater(() -> {
                    // Pastikan tab pengirim ada
                    if (msg.getRecipient() != null && !msg.getRecipient().equalsIgnoreCase("ALL")) {
                        gui.openPrivateTab(typingSender);
                    }
                    gui.updateTypingIndicator(typingSender, true);
                });
                break;

            case TYPING_STOP:
                String stoppingSender = msg.getSender();
                SwingUtilities.invokeLater(() -> gui.updateTypingIndicator(stoppingSender, false));
                break;
            // -----------------------------------------

            case USER_LIST_UPDATE:
                if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                    SwingUtilities.invokeLater(() -> gui.updateUserList(msg.getContent().split(",")));
                }
                break;
            case BUZZ:
                SwingUtilities.invokeLater(() -> gui.triggerBuzz(msg.getSender()));
                break;
            case DISCONNECT:
                SwingUtilities.invokeLater(() -> {
                    gui.logMessage("Server meminta disconnect. " + msg.getContent());
                    disconnect();
                });
                break;

            // --- FILE RECEIVER LOGIC ---
            case FILE_REQUEST:
                // 1. Ambil Info File
                String sender = msg.getSender();
                String fileName = msg.getContent();
                long size = msg.getFileSize();

                this.receivingFileName = fileName;
                this.expectedFileSize = size;
                this.totalBytesReceived = 0;

                // 2. LANGSUNG BUKA STREAM KE FILE SEMENTARA (.part)
                // Jangan tunggu user klik Yes/No, keburu datanya lewat!
                try {
                    File downloadDir = new File("downloads");
                    if (!downloadDir.exists())
                        downloadDir.mkdir();

                    // Simpan sebagai .part dulu
                    File tempFile = new File(downloadDir, fileName + ".part");
                    currentFileWriter = new FileOutputStream(tempFile);

                    // 3. Tampilkan Dialog Konfirmasi (Non-Blocking / Async)
                    // Gunakan Thread terpisah agar tidak mengganggu aliran data masuk
                    SwingUtilities.invokeLater(() -> {
                        int choice = javax.swing.JOptionPane.showConfirmDialog(gui,
                                "Terima file '" + fileName + "' (" + (size / 1024) + " KB) dari " + sender + "?",
                                "File Masuk",
                                javax.swing.JOptionPane.YES_NO_OPTION);

                        if (choice != javax.swing.JOptionPane.YES_OPTION) {
                            // JIKA USER MENOLAK:
                            try {
                                // Tutup keran
                                if (currentFileWriter != null)
                                    currentFileWriter.close();
                                currentFileWriter = null;
                                // Hapus file .part yang sudah terlanjur ditulis
                                tempFile.delete();
                                gui.logMessage("❌ File ditolak & dihapus.");
                            } catch (Exception e) {
                            }
                        } else {
                            gui.logMessage("Menerima file...");
                        }
                    });

                } catch (IOException e) {
                    gui.logMessage("ERROR Init File: " + e.getMessage());
                }
                break;

            case FILE_CHUNK:
                // --- PERBAIKAN: HAPUS "new Thread", Tulis Langsung (Synchronous) ---
                if (currentFileWriter != null) {
                    try {
                        byte[] chunk = msg.getFileChunk();
                        if (chunk != null) {
                            // 1. Tulis Langsung (Dijamin Urut karena satu thread)
                            currentFileWriter.write(chunk);

                            // 2. Update Counter
                            totalBytesReceived += chunk.length;

                            // 3. Update Progress Bar (Tetap pakai invokeLater buat GUI)
                            if (expectedFileSize > 0) {
                                int percentage = (int) ((totalBytesReceived * 100) / expectedFileSize);
                                String statusText = String.format("Downloading... %d%%", percentage);
                                SwingUtilities.invokeLater(() -> gui.updateFileProgress(true, percentage, statusText));
                            }
                        }
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() -> {
                            gui.logMessage("ERROR Write: " + e.getMessage());
                            gui.updateFileProgress(false, 0, "");
                        });
                        try {
                            if (currentFileWriter != null)
                                currentFileWriter.close();
                        } catch (IOException ex) {
                        }
                        currentFileWriter = null;
                    }
                }
                break;

            case FILE_COMPLETE:
                SwingUtilities.invokeLater(() -> {
                    if (currentFileWriter != null) {
                        try {
                            currentFileWriter.close();
                            
                            // PROSES RENAME (.part -> Asli)
                            File downloadDir = new File("downloads");
                            File tempFile = new File(downloadDir, receivingFileName + ".part");
                            File finalFile = new File(downloadDir, receivingFileName);
                            
                            // Hapus file lama jika ada (overwrite)
                            if (finalFile.exists()) finalFile.delete();
                            
                            boolean success = tempFile.renameTo(finalFile);
                            
                            if (success) {
                                gui.logMessage("✅ File tersimpan: " + receivingFileName);
                            } else {
                                gui.logMessage("⚠️ Gagal rename file .part, cek folder downloads.");
                            }

                            // Update UI Selesai
                            gui.updateFileProgress(false, 100, "");
                            
                            // Reset
                            currentFileWriter = null;
                            receivingFileName = null;
                            expectedFileSize = 0;
                            totalBytesReceived = 0;
                            
                        } catch (IOException e) {
                            gui.logMessage("ERROR Finalizing: " + e.getMessage());
                        }
                    }
                });
                break;
            // ---------------------------------------------------
            case CONNECT:
                SwingUtilities.invokeLater(() -> gui.logMessage("Status: Berhasil terhubung. Menunggu User List..."));
                break;
            default:
                SwingUtilities.invokeLater(() -> gui.logMessage("Pesan tipe tidak dikenal diterima: " + msg.getType()));
        }
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
                    // Coba kirim sinyal DISCONNECT ke server
                    output.writeObject(disconnectMsg);
                    output.flush();
                } catch (Exception ignored) {
                } // Jika gagal kirim, mungkin socket sudah setengah tertutup

                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
                if (socket != null)
                    socket.close();
            }
            // Tutup file stream jika masih terbuka saat disconnect mendadak
            if (currentFileWriter != null) {
                try {
                    currentFileWriter.close();
                } catch (IOException ignored) {
                }
                currentFileWriter = null;
            }

            // **PROGRESS BAR: Sembunyikan saat disconnect**
            SwingUtilities.invokeLater(() -> gui.updateFileProgress(false, 0, ""));

        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> gui.logMessage("ERROR saat menutup koneksi: " + e.getMessage()));
        } finally {
            SwingUtilities.invokeLater(() -> gui.connectionClosed());
        }
    }
}
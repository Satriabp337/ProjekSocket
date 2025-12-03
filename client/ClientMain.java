package client;

import common.Message;
import common.MessageType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientMain extends JFrame {
    
    // --- 1. Variabel GUI ---
    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private JButton sendButton = new JButton("Kirim");
    private JButton buzzButton = new JButton("BUZZ!"); 
    private JButton fileButton = new JButton("Kirim File"); 

    // --- 2. Variabel Jaringan dan I/O ---
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in; 
    private String username;

    // --- 3. Variabel Konfigurasi ---
    private final String SERVER_IP = "localhost";
    private final int SERVER_PORT = 50125;

    public ClientMain() {
        // Mendapatkan username saat awal
        username = JOptionPane.showInputDialog("Masukkan Username:");
        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        // Pastikan frame ditutup saat program exit
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                sendLogout();
                System.exit(0);
            }
        });

        setupGUI();
        connectToServer();
    }
    
    /**
     * Membangun dan menampilkan Antarmuka Pengguna (GUI).
     */
    private void setupGUI() {
        setTitle("SecureLAN Client - " + username);
        setSize(550, 400);
        setLayout(new BorderLayout());

        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        bottomPanel.add(inputField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(buzzButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);
        
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Menambahkan Action Listeners
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage()); 
        buzzButton.addActionListener(e -> sendBuzz());
        fileButton.addActionListener(e -> sendFile());

        setVisible(true);
    }
    
    /**
     * Mencoba koneksi ke Server dan inisialisasi I/O Streams.
     */
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            
            // Urutan Object Stream penting: OutputStream harus diinisialisasi duluan
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream()); 

            // Kirim pesan LOGIN otomatis
            Message loginMsg = new Message(MessageType.LOGIN);
            loginMsg.setSender(username);
            out.writeObject(loginMsg);
            out.flush();
            out.reset(); // Reset cache setelah LOGIN

            chatArea.append("Terhubung ke server sebagai " + username + "\n");
            
            // Memulai thread baru untuk mendengarkan pesan dari server
            new Thread(new ClientListener()).start(); 

        } catch (Exception e) {
            chatArea.append("Gagal konek ke server: " + e.getMessage() + "\n");
            // Nonaktifkan input jika koneksi gagal
            sendButton.setEnabled(false);
            inputField.setEditable(false);
            buzzButton.setEnabled(false);
            fileButton.setEnabled(false);
        }
    }

    /**
     * Mengirim pesan teks ke Server.
     */
    private void sendMessage() {
        try {
            String text = inputField.getText();
            if (text.trim().isEmpty()) return;
            
            Message msg = new Message(MessageType.TEXT);
            msg.setSender(username);
            msg.setContent(text);
            msg.setRecipient("ALL"); 

            out.writeObject(msg); 
            out.flush();
            out.reset(); // PENTING: Mencegah masalah Object Stream Caching

            // Tampilkan pesan Anda sendiri di GUI secara lokal
            chatArea.append("Saya: " + text + "\n"); 
            inputField.setText("");
            
        } catch (IOException e) {
            chatArea.append("[ERROR] Gagal mengirim pesan: " + e.getMessage() + "\n");
            closeResources();
        }
    }
    
    /**
     * Mengirim perintah LOGOUT ke Server.
     */
    private void sendLogout() {
        try {
            if (out != null && socket.isConnected() && !socket.isClosed()) {
                Message msg = new Message(MessageType.LOGOUT);
                msg.setSender(username);
                out.writeObject(msg);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            // Abaikan, karena kita akan menutup socket tak lama setelah ini
        } finally {
            closeResources();
        }
    }

    /**
     * Mengirim perintah BUZZ ke Server.
     */
    private void sendBuzz() {
        try {
            Message msg = new Message(MessageType.BUZZ);
            msg.setSender(username);
            msg.setRecipient("ALL"); 

            out.writeObject(msg);
            out.flush();
            out.reset(); // PENTING: Mencegah masalah Object Stream Caching

            chatArea.append("Anda mengirim BUZZ!\n");
        } catch (IOException e) {
            chatArea.append("[ERROR] Gagal mengirim BUZZ: " + e.getMessage() + "\n");
            closeResources();
        }
    }
    
    /**
     * Memilih file dan mengirimkannya ke Server.
     */
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File fileToSend = fileChooser.getSelectedFile();
            if (fileToSend.length() > 50 * 1024 * 1024) { // Batas 50MB
                JOptionPane.showMessageDialog(this, "Ukuran file terlalu besar (Max 50MB).", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // 1. Kirim Notifikasi START ke Server
                Message fileNotification = new Message(MessageType.FILE_TRANSFER_START);
                fileNotification.setSender(username);
                // Isi pesan berupa Nama File dan Ukuran File
                fileNotification.setContent(fileToSend.getName() + "|" + fileToSend.length());
                out.writeObject(fileNotification);
                out.flush();
                out.reset(); 

                chatArea.append("[FILE] Mengirim: " + fileToSend.getName() + " (" + fileToSend.length() + " bytes)...\n");

                // 2. Kirim Data Biner File (Harus sinkron dengan server!)
                FileInputStream fileIn = new FileInputStream(fileToSend);
                byte[] buffer = new byte[8192]; // Buffer 8KB
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) > 0) {
                    // Tulis data BINER langsung ke ObjectOutputStream.
                    out.write(buffer, 0, bytesRead);
                    out.flush(); // Kirim segera agar server tidak terblokir
                }

                fileIn.close();
                
                // 3. Kirim notifikasi Selesai ke Server (Opsional, tergantung protokol Server)
                Message completeMsg = new Message(MessageType.FILE_TRANSFER_COMPLETE);
                completeMsg.setSender(username);
                completeMsg.setContent(fileToSend.getName());
                out.writeObject(completeMsg);
                out.flush();
                out.reset();

                chatArea.append("[FILE] Pengiriman Selesai: " + fileToSend.getName() + "\n");

            } catch (IOException e) {
                chatArea.append("[ERROR] Gagal mengirim File: " + e.getMessage() + "\n");
                closeResources();
            }
        }
    }
    
    /**
     * Logika untuk menggoyangkan jendela (Efek BUZZ).
     */
    private void handleBuzzCommand(String sender) {
        chatArea.append("*** " + sender + " mengirim BUZZ! ***\n");
        
        final int originalX = getLocationOnScreen().x;
        final int originalY = getLocationOnScreen().y;
        final int SHAKE_DISTANCE = 10;
        final int SHAKE_COUNT = 5;

        Timer timer = new Timer(50, new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count < SHAKE_COUNT * 2) {
                    int x = originalX;
                    x += (count % 2 == 0) ? SHAKE_DISTANCE : -SHAKE_DISTANCE; 
                    setLocation(x, originalY);
                    count++;
                } else {
                    ((Timer) e.getSource()).stop();
                    setLocation(originalX, originalY); 
                }
            }
        });
        timer.start();
    }
    
    /**
     * Metode pembantu untuk menutup semua streams dan socket.
     */
    private void closeResources() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Abaikan error saat menutup resource
        }
    }


    // -------------------------------------------------------------------------
    // --- KELAS CLIENT THREAD (CLIENT LISTENER) ---
    // -------------------------------------------------------------------------

    private class ClientListener implements Runnable {
        @Override
        public void run() {
            try {
                Message receivedMsg;
                // Loop terus menerus membaca Object dari Server
                while ((receivedMsg = (Message) in.readObject()) != null) {
                
                    // Semua pembaruan GUI harus dilakukan di EDT
                    SwingUtilities.invokeLater(() -> {
                        switch (receivedMsg.getType()) {
                            case TEXT:
                                chatArea.append(receivedMsg.getSender() + ": " + receivedMsg.getContent() + "\n");
                                break;
                            case BUZZ:
                                handleBuzzCommand(receivedMsg.getSender());
                                break;
                            case LOGIN:
                                chatArea.append("[INFO] " + receivedMsg.getSender() + " Telah Bergabung.\n");
                                break;
                            case LOGOUT:
                                chatArea.append("[INFO] " + receivedMsg.getSender() + " Telah Keluar.\n");
                                break;
                            case FILE_TRANSFER_START:
                                // Server memberi tahu klien lain bahwa transfer sedang berlangsung.
                                chatArea.append("[FILE] " + receivedMsg.getSender() + " Mulai mengirim file: " + receivedMsg.getContent() + "\n");
                                break;
                            case FILE_TRANSFER_COMPLETE:
                                chatArea.append("[FILE] " + receivedMsg.getSender() + " Selesai mengirim file: " + receivedMsg.getContent() + "\n");
                                break;
                            default:
                                chatArea.append("[SERVER] Pesan tak dikenal: " + receivedMsg.getType() + "\n");
                                break;
                        }
                    });
                }
            // Tangani putusnya koneksi dari server (EOF/SocketException)
            } catch (EOFException | SocketException e) { 
                SwingUtilities.invokeLater(() -> chatArea.append("[DISCONNECT] Koneksi server terputus.\n"));
            } catch (ClassNotFoundException | IOException e) {
                // Tangani kegagalan membaca objek atau masalah I/O lainnya
                SwingUtilities.invokeLater(() -> chatArea.append("[ERROR] Gagal memproses data: " + e.getMessage() + "\n"));
            } finally {
                // Pastikan resource ditutup
                closeResources();
            }
        }
    }


    // -------------------------------------------------------------------------
    // --- MAIN METHOD ---
    // -------------------------------------------------------------------------
    
    public static void main(String[] args) {
        // Memastikan GUI dibuat di Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(ClientMain::new);
    }
}
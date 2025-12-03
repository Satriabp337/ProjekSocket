package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class ChatClientGUI {
    // --- Variabel GUI ---
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton buzzButton;
    private JButton fileButton;

    // --- Variabel Jaringan dan I/O ---
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    // --- Variabel Konfigurasi ---
    private final String SERVER_IP = "127.0.0.1"; // Ganti dengan IP Server Anda
    private final int SERVER_PORT = 12345;     // Ganti dengan Port Server Anda
    private String username;

    public ChatClientGUI() {
        // Mendapatkan username dari pengguna
        this.username = JOptionPane.showInputDialog(null, "Masukkan Username Anda:", "Login Chat", JOptionPane.PLAIN_MESSAGE);
        if (this.username == null || this.username.trim().isEmpty()) {
            System.exit(0); // Keluar jika username tidak dimasukkan
        }
        
        // Membangun GUI
        setupGUI();
        
        // Menghubungkan ke Server
        connectToServer();
    }
    
    // ... (Metode setupGUI(), connectToServer(), actionListener, dll. akan dijelaskan di bawah)
    
    public static void main(String[] args) {
        // Memastikan GUI dibuat di Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }

/**
     * Membuat dan menampilkan Antarmuka Pengguna (GUI).
     */
    private void setupGUI() {
        frame = new JFrame("Chat Client - " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout());

        // 1. Chat Area (Menampilkan Pesan)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // 2. Input Panel (Mengirim Pesan dan Kontrol)
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Kirim");
        buzzButton = new JButton("BUZZ!");
        fileButton = new JButton("Kirim File");

        // 3. Panel Kontrol (BUZZ dan File)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(buzzButton);
        controlPanel.add(fileButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(controlPanel, BorderLayout.WEST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        // Menambahkan Action Listeners
        addListeners();

        frame.setVisible(true);
    }
    /**
     * Mencoba koneksi ke Server dan inisialisasi I/O Streams.
     */
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            
            // I/O Streams untuk Komunikasi Jaringan
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Mengirim username ke server segera setelah koneksi
            writer.println(username);

            // Memulai thread baru untuk mendengarkan pesan dari server
            new Thread(new ClientReceiverThread()).start();
            
            chatArea.append("Terhubung ke Server di " + SERVER_IP + ":" + SERVER_PORT + "\n");
            
        } catch (IOException e) {
            chatArea.append("Gagal terhubung ke server: " + e.getMessage() + "\n");
            // Nonaktifkan input jika koneksi gagal
            sendButton.setEnabled(false);
            messageField.setEditable(false);
        }
    }

    /**
     * Client Thread (Mendengarkan Pesan dari Server)
     */
    private class ClientReceiverThread implements Runnable {
        @Override
        public void run() {
            String serverResponse;
            try {
                // Mendengarkan respon dari server secara terus menerus
                while ((serverResponse = reader.readLine()) != null) {
                    
                    // --- Logika Penanganan Pesan Masuk ---
                    
                    // 1. Fitur BUZZ: Asumsi Server mengirimkan "CMD:BUZZ" saat ada BUZZ
                    if (serverResponse.startsWith("CMD:BUZZ")) {
                        handleBuzzCommand(); 
                    } 
                    // 2. File Transfer (Jika server mengirimkan notifikasi file)
                    else if (serverResponse.startsWith("FILE:")) {
                         // Implementasi download file di sini
                         chatArea.append("[INFO] Notifikasi File Masuk: " + serverResponse.substring(5) + "\n");
                    }
                    // 3. Pesan Chat Biasa
                    else {
                        chatArea.append(serverResponse + "\n");
                    }
                }
            } catch (IOException e) {
                // Koneksi terputus
                chatArea.append("[ERROR] Koneksi ke Server terputus.\n");
            } finally {
                // Memastikan resources ditutup saat thread selesai
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
    /**
     * Menambahkan Listeners untuk semua komponen interaktif.
     */
    private void addListeners() {
        // Aksi Tombol KIRIM dan Enter di Text Field
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
            }
        };
        sendButton.addActionListener(sendAction);
        messageField.addActionListener(sendAction); // Tekan ENTER

        // Aksi Tombol BUZZ
        buzzButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Mengirim perintah BUZZ ke server (Asumsi Server tahu cara menanganinya)
                sendMessage("CMD:BUZZ"); 
            }
        });

        // Aksi Tombol KIRIM FILE
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Memanggil fungsi untuk mengirim file
                sendFile(); 
            }
        });
    }

    /**
     * Mengirim pesan ke Server melalui PrintWriter.
     * @param message Pesan yang akan dikirim.
     */
    private void sendMessage(String message) {
        if (writer != null && !message.trim().isEmpty()) {
            writer.println(message); // Mengirim pesan ke server
            messageField.setText(""); // Mengosongkan field input
        }
    }

    /**
     * Implementasi Fitur BUZZ (Menggoyangkan window).
     */
    private void handleBuzzCommand() {
        chatArea.append("*** MENDAPATKAN BUZZ! ***\n");
        // Logika menggoyangkan jendela
        final int originalX = frame.getLocationOnScreen().x;
        final int originalY = frame.getLocationOnScreen().y;
        final int SHAKE_DISTANCE = 10;
        final int SHAKE_COUNT = 5;

        Timer timer = new Timer(50, new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count < SHAKE_COUNT * 2) {
                    int x = originalX;
                    if (count % 2 == 0) {
                        x += SHAKE_DISTANCE; // Geser ke kanan
                    } else {
                        x -= SHAKE_DISTANCE; // Geser ke kiri
                    }
                    frame.setLocation(x, originalY);
                    count++;
                } else {
                    ((Timer) e.getSource()).stop();
                    frame.setLocation(originalX, originalY); // Kembali ke posisi awal
                }
            }
        });
        timer.start();
    }
    /**
     * Memilih file dan mengirimkannya ke Server.
     * Menggunakan File I/O Streams.
     */
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(frame);
        
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File fileToSend = fileChooser.getSelectedFile();
            chatArea.append("[INFO] Mencoba mengirim file: " + fileToSend.getName() + "\n");

            try {
                // 1. Kirim notifikasi ke server melalui PrintWriter (Stream Text)
                // Asumsi Server mendengarkan perintah ini untuk membuka File I/O Stream
                writer.println("FILE_TRANSFER:" + fileToSend.getName() + ":" + fileToSend.length());

                // 2. Menggunakan FileOutputStream untuk mengirim data BINER
                // PENTING: Server Anda harus siap MENDENGARKAN stream biner SEGERA setelah notifikasi di atas.

                FileInputStream fis = new FileInputStream(fileToSend);
                OutputStream os = socket.getOutputStream(); // Mendapatkan raw output stream
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, bytesRead);
                }
                
                // Penting: Server Anda harus memiliki mekanisme untuk tahu kapan file selesai (misalnya berdasarkan panjang file)
                // os.flush(); // Perlu hati-hati dengan flush di sini, karena dapat mempengaruhi stream pesan biasa
                fis.close();
                
                chatArea.append("[INFO] File '" + fileToSend.getName() + "' berhasil dikirim.\n");

            } catch (IOException ex) {
                chatArea.append("[ERROR] Gagal mengirim file: " + ex.getMessage() + "\n");
            }
        }
    }
}
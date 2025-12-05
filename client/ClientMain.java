package client;

import common.Message;
import common.MessageType;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ClientMain extends JFrame {

    // --- Variabel Baru ---
    private ClientService clientService; // Objek yang menangani logika koneksi
    private String currentUsername;

    // --- Komponen GUI ---
    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();

    // VARIABEL BARU
    private JButton sendButton = new JButton("Kirim Pesan"); // Ubah nama tombol Kirim
    private JButton buzzButton = new JButton("BUZZ"); // TOMBOL BARU
    private JButton fileButton = new JButton("Kirim File"); // TOMBOL BARU

    private JButton connectButton = new JButton("Connect");
    private JList<String> userList = new JList<>();
    private DefaultListModel<String> userListModel = new DefaultListModel<>();

    public ClientMain() {
        // 1. Setup GUI Sederhana
        setTitle("SecureLAN Client");
        setSize(650, 400); // Ukuran lebih besar
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Setup Panels
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel chatPanel = new JPanel(new BorderLayout());
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // PANEL BARU untuk menampung semua tombol aksi (Kirim, BUZZ, File)
        JPanel actionPanel = new JPanel(new GridLayout(1, 3));

        // Chat Area
        chatArea.setEditable(false);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Input Area
        bottomPanel.add(inputField, BorderLayout.CENTER);

        // Tambahkan tombol aksi ke panel aksi
        actionPanel.add(sendButton);
        actionPanel.add(buzzButton);
        actionPanel.add(fileButton);

        bottomPanel.add(actionPanel, BorderLayout.EAST); // Ganti sendButton dengan actionPanel

        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        // User List (Kanan)
        userList.setModel(userListModel);
        userList.setPreferredSize(new Dimension(150, getHeight()));

        // Kontrol Koneksi (Atas)
        JPanel topControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topControlPanel.add(connectButton);

        // Gabungkan semuanya
        mainPanel.add(topControlPanel, BorderLayout.NORTH);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(new JScrollPane(userList), BorderLayout.EAST);

        add(mainPanel);

        // 2. Aksi Tombol
        // onSendButtonClick akan mengambil recipient dari userList.getSelectedValue()
        sendButton.addActionListener(e -> onSendButtonClick());
        connectButton.addActionListener(e -> showConnectDialog());

        // Aksi Tombol Baru
        buzzButton.addActionListener(e -> onBuzzButtonClick());
        fileButton.addActionListener(e -> onFileButtonClick());

        // Inisialisasi service
        clientService = new ClientService(this);

        // Setup state awal
        updateGuiState(false);

        setVisible(true);
    }

    // --- Implementasi Koneksi (TETAP SAMA) ---
    // ... showConnectDialog, onConnectButtonClick, dan semua method callback
    // (displayMessage, logMessage, dll.) ...

    // --- Implementasi Pengiriman Pesan (DIMODIFIKASI) ---

    private void onSendButtonClick() {
        String recipient = userList.getSelectedValue(); // Ambil recipient dari JList
        if (recipient == null) {
            recipient = "ALL (Broadcast)"; // Default ke Broadcast jika tidak ada yang dipilih
        }

        // Hapus "(Broadcast)" jika ada
        String actualRecipient = recipient.replace(" (Broadcast)", "");

        String text = inputField.getText();
        if (clientService != null && text != null && !text.trim().isEmpty()) {

            clientService.sendTextMessage(actualRecipient, text);
            displayMessage(String.format("[Saya -> %s]: %s", actualRecipient.toUpperCase(), text));
            inputField.setText("");
        } else if (clientService == null) {
            logMessage("Anda belum terhubung ke server.");
        }
    }

    // --- FITUR BARU: BUZZ ---

    private void onBuzzButtonClick() {
        String recipient = userList.getSelectedValue();
        if (recipient == null || recipient.equalsIgnoreCase("ALL (Broadcast)")) {
            logMessage("Pilih pengguna spesifik untuk fitur BUZZ.");
            return;
        }

        if (clientService != null) {
            // Hapus "(Broadcast)" jika ada
            String actualRecipient = recipient.replace(" (Broadcast)", "");
            clientService.sendBuzz(actualRecipient);
        }
    }

    // --- FITUR BARU: FILE TRANSFER ---

    private void onFileButtonClick() {
        String recipient = userList.getSelectedValue();
        if (recipient == null || recipient.equalsIgnoreCase("ALL (Broadcast)")) {
            logMessage("Pilih pengguna spesifik untuk Kirim File.");
            return;
        }

        // Buka File Chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File untuk Dikirim ke " + recipient);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.length() > 50 * 1024 * 1024) { // Contoh: Batas 50MB
                logMessage("ERROR: File terlalu besar (Max 50MB).");
                return;
            }
            if (clientService != null) {
                // Hapus "(Broadcast)" jika ada
                String actualRecipient = recipient.replace(" (Broadcast)", "");
                clientService.sendFile(actualRecipient, selectedFile);
            }
        }
    }

    // --- Implementasi Method Callback dari ClientService (TETAP SAMA) ---

    /**
     * Menampilkan pesan di area chat/log. Aman dipanggil dari ClientService (sudah
     * di-invokeLater).
     */
    public void displayMessage(String message) {
        chatArea.append(message + "\n");
    }

    /**
     * Menampilkan pesan log (untuk status koneksi/error).
     */
    public void logMessage(String message) {
        chatArea.append("[LOG] " + message + "\n");
    }

    /**
     * Memperbarui JList pengguna yang terkoneksi.
     */
    public void updateUserList(String[] users) {
        userListModel.clear();
        userListModel.addElement("ALL (Broadcast)");
        for (String user : users) {
            if (!user.equalsIgnoreCase(this.currentUsername)) {
                userListModel.addElement(user);
            }
        }
    }

    /**
     * Memicu fitur "BUZZ" (Window Shake).
     */
    public void triggerBuzz(String sender) {
        displayMessage(String.format("!!! BUZZ diterima dari %s !!!", sender));
        Point originalLoc = getLocation();
        try {
            for (int i = 0; i < 5; i++) {
                setLocation(originalLoc.x + 5, originalLoc.y);
                Thread.sleep(50);
                setLocation(originalLoc.x - 5, originalLoc.y);
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            setLocation(originalLoc); // Kembalikan ke posisi semula
        }
    }

    /**
     * Dipanggil saat koneksi terputus secara tak terduga
     * (IOException/SocketException).
     */
    public void connectionLost() {
        logMessage("Koneksi ke server terputus secara tak terduga!");
        connectionClosed();
    }

    /**
     * Dipanggil saat koneksi berhasil ditutup secara bersih.
     */
    public void connectionClosed() {
        logMessage("Koneksi telah ditutup.");
        updateGuiState(false);
        userListModel.clear();
        userListModel.addElement("ALL (Broadcast)");
    }

    /**
     * Mengatur state tombol dan input field berdasarkan status koneksi.
     */
    private void updateGuiState(boolean isConnected) {
        connectButton.setEnabled(!isConnected);
        sendButton.setEnabled(isConnected);
        buzzButton.setEnabled(isConnected); // Aktifkan BUZZ saat terhubung
        fileButton.setEnabled(isConnected); // Aktifkan File saat terhubung
        inputField.setEnabled(isConnected);
        inputField.setText(isConnected ? "" : "Terputus. Silakan Connect.");
    }

    // --- Getter & Connect Logic (Untuk konsistensi) ---

    public String getUsername() {
        return this.currentUsername;
    }

    private void showConnectDialog() {
        String usernameInput = JOptionPane.showInputDialog(this, "Masukkan Username:", "Koneksi",
                JOptionPane.PLAIN_MESSAGE);

        if (usernameInput != null && !usernameInput.trim().isEmpty()) {
            this.currentUsername = usernameInput.trim();
            onConnectButtonClick("localhost", 50125, this.currentUsername);
        }
    }

    private void onConnectButtonClick(String host, int port, String username) {
        logMessage("Mencoba koneksi ke server...");
        connectButton.setEnabled(false);
        new Thread(() -> {
            boolean success = clientService.connect(host, port, username);
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    logMessage("✅ Berhasil terhubung sebagai: " + username);
                    updateGuiState(true);
                } else {
                    logMessage("❌ Gagal konek ke server. Cek Server Satria.");
                    connectButton.setEnabled(true);
                }
            });
        }).start();
    }

    // --- Main Method ---

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientMain());
    }
}
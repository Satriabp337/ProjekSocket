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
    private JButton sendButton = new JButton("Kirim Pesan"); 
    private JButton buzzButton = new JButton("BUZZ"); 
    private JButton fileButton = new JButton("Kirim File"); 

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

        bottomPanel.add(actionPanel, BorderLayout.EAST); 

        chatPanel.add(bottomPanel, BorderLayout.SOUTH);

        // User List (Kanan)
        userList.setModel(userListModel);
        userList.setPreferredSize(new Dimension(150, getHeight()));
        
        // --- INI ADALAH IMPLEMENTASI TAMPILAN STATUS ONLINE ---
        userList.setCellRenderer(new UserListRenderer());
        // ----------------------------------------------------

        // Kontrol Koneksi (Atas)
        JPanel topControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topControlPanel.add(connectButton);

        // Gabungkan semuanya
        mainPanel.add(topControlPanel, BorderLayout.NORTH);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(new JScrollPane(userList), BorderLayout.EAST);

        add(mainPanel);

        // 2. Aksi Tombol
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

    // --- FITUR BARU: UserListRenderer untuk Tampilan Status Online ---
    
    /**
     * Renderer kustom untuk JList yang menampilkan status 'Online' dengan ikon.
     */
    private class UserListRenderer extends DefaultListCellRenderer {
        private final ImageIcon onlineIcon;
        private final ImageIcon broadcastIcon;

        public UserListRenderer() {
            // Buat ikon titik hijau (Online)
            onlineIcon = createDotIcon(Color.GREEN); 
            // Buat ikon titik biru/abu-abu (Broadcast)
            broadcastIcon = createDotIcon(Color.GRAY.darker()); 
        }

        private ImageIcon createDotIcon(Color color) {
            int size = 10;
            // Gunakan BufferedImage untuk menggambar lingkaran
            Image image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) image.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.dispose();
            return new ImageIcon(image);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                      boolean isSelected, boolean cellHasFocus) {
            
            // Panggil implementasi default (penting untuk menangani seleksi warna)
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            String user = (String) value;
            
            if (user.equalsIgnoreCase("ALL (Broadcast)")) {
                label.setIcon(broadcastIcon);
                // Kita juga bisa mengubah warna teks untuk Broadcast
                label.setForeground(Color.BLUE.darker());
            } else {
                // Semua user yang ada di list dianggap online, jadi beri ikon hijau
                label.setIcon(onlineIcon);
            }
            
            // Tambahkan spasi di depan teks agar tidak terlalu mepet ikon
            label.setText(" " + user); 
            
            return label;
        }
    }
    // --------------------------------------------------------------------------

    // --- Implementasi Pengiriman Pesan (TETAP SAMA) ---

    private void onSendButtonClick() {
        String recipient = userList.getSelectedValue(); 
        if (recipient == null) {
            recipient = "ALL (Broadcast)"; 
        }

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

    // --- FITUR BARU: BUZZ (TETAP SAMA) ---

    private void onBuzzButtonClick() {
        String recipient = userList.getSelectedValue();
        if (recipient == null || recipient.equalsIgnoreCase("ALL (Broadcast)")) {
            logMessage("Pilih pengguna spesifik untuk fitur BUZZ.");
            return;
        }

        if (clientService != null) {
            String actualRecipient = recipient.replace(" (Broadcast)", "");
            clientService.sendBuzz(actualRecipient);
        }
    }

    // --- FITUR BARU: FILE TRANSFER (TETAP SAMA) ---

    private void onFileButtonClick() {
        String recipient = userList.getSelectedValue();
        if (recipient == null || recipient.equalsIgnoreCase("ALL (Broadcast)")) {
            logMessage("Pilih pengguna spesifik untuk Kirim File.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File untuk Dikirim ke " + recipient);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.length() > 50 * 1024 * 1024) { 
                logMessage("ERROR: File terlalu besar (Max 50MB).");
                return;
            }
            if (clientService != null) {
                String actualRecipient = recipient.replace(" (Broadcast)", "");
                clientService.sendFile(actualRecipient, selectedFile);
            }
        }
    }

    // --- Implementasi Method Callback dari ClientService (TETAP SAMA) ---

    public void displayMessage(String message) {
        chatArea.append(message + "\n");
    }

    public void logMessage(String message) {
        chatArea.append("[LOG] " + message + "\n");
    }

    /**
     * Memperbarui JList pengguna yang terkoneksi.
     * Logika ini menerima string array nama pengguna dari server dan memuatnya.
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
            setLocation(originalLoc); 
        }
    }

    public void connectionLost() {
        logMessage("Koneksi ke server terputus secara tak terduga!");
        connectionClosed();
    }

    public void connectionClosed() {
        logMessage("Koneksi telah ditutup.");
        updateGuiState(false);
        userListModel.clear();
        userListModel.addElement("ALL (Broadcast)");
    }

    private void updateGuiState(boolean isConnected) {
        connectButton.setEnabled(!isConnected);
        sendButton.setEnabled(isConnected);
        buzzButton.setEnabled(isConnected); 
        fileButton.setEnabled(isConnected); 
        inputField.setEnabled(isConnected);
        inputField.setText(isConnected ? "" : "Terputus. Silakan Connect.");
    }

    // --- Getter & Connect Logic (TETAP SAMA) ---

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
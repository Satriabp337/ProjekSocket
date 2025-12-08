package client;

import common.Message;
import common.MessageType;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;

public class ClientMain extends JFrame {

    // --- Konfigurasi ---
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // Limit 50 MB

    // --- State ---
    private ClientService clientService;
    private String currentUsername;
    private HashMap<String, JTextArea> chatPanels = new HashMap<>(); // Simpan area chat per user

    // --- Komponen GUI ---
    private JTabbedPane tabbedPane = new JTabbedPane(); // Tab System
    private JTextField inputField = new JTextField();
    private JLabel statusLabel = new JLabel("Status: Ready"); // Status Bar Bawah

    // Tombol
    private JButton sendButton = new JButton("Kirim");
    private JButton buzzButton = new JButton("BUZZ!");
    private JButton fileButton = new JButton("File");
    private JButton connectButton = new JButton("Connect");

    // List User
    private JList<String> userList = new JList<>();
    private DefaultListModel<String> userListModel = new DefaultListModel<>();

    public ClientMain() {
        // 1. Setup Window
        setTitle("SecureLAN Client (Tabbed Edition)");
        setSize(750, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- PANEL TENGAH (Tabs) ---
        // Tab Default: Lobby
        createChatTab("Lobby");
        add(tabbedPane, BorderLayout.CENTER);

        // --- PANEL KANAN (User List) ---
        userList.setModel(userListModel);
        userList.setCellRenderer(new UserListRenderer()); // Pakai renderer ikon titik hijau
        userList.setPreferredSize(new Dimension(180, 0));

        // Fitur: Double Click nama user untuk buka Tab Chat Private
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selected = userList.getSelectedValue();
                    if (selected != null && !selected.contains("Broadcast")) {
                        openPrivateTab(selected);
                    }
                }
            }
        });

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel(" Online Users:"), BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // --- PANEL ATAS (Connect) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(connectButton);
        add(topPanel, BorderLayout.NORTH);

        // --- PANEL BAWAH (Input & Status) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Area Input & Tombol
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.add(fileButton);
        btnPanel.add(buzzButton);
        btnPanel.add(sendButton);
        inputPanel.add(btnPanel, BorderLayout.EAST);

        bottomPanel.add(inputPanel, BorderLayout.NORTH);

        // Status Bar (Biar log bersih)
        statusLabel.setBorder(new EmptyBorder(2, 5, 2, 5));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // --- Event Listeners ---
        sendButton.addActionListener(e -> onSendButtonClick());
        inputField.addActionListener(e -> onSendButtonClick()); // Enter = Kirim
        connectButton.addActionListener(e -> showConnectDialog());
        buzzButton.addActionListener(e -> onBuzzButtonClick());
        fileButton.addActionListener(e -> onFileButtonClick());

        // Inisialisasi Service
        clientService = new ClientService(this);
        updateGuiState(false);

        setVisible(true);
    }

    // --- LOGIKA TAB SYSTEM (JANTUNG GUI BARU) ---

    /**
     * Membuat tab baru jika belum ada, atau fokus ke tab yang sudah ada.
     */
    private void createChatTab(String title) {
        if (chatPanels.containsKey(title)) {
            // Kalau sudah ada, langsung fokus ke sana
            tabbedPane.setSelectedComponent(chatPanels.get(title).getParent().getParent());
            return;
        }

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Margin teks biar tidak mepet pinggir
        area.setBorder(new EmptyBorder(5, 5, 5, 5));

        chatPanels.put(title, area);
        tabbedPane.addTab(title, new JScrollPane(area));

        // Auto-scroll ke bawah saat ada chat baru
        // (Default behavior JScrollPane biasanya sudah handle, tapi bisa diperkuat)
    }

    private void openPrivateTab(String targetUser) {
        if (!chatPanels.containsKey(targetUser)) {
            createChatTab(targetUser);
        }
        // Pindah fokus ke tab tersebut
        int index = tabbedPane.indexOfTab(targetUser);
        if (index != -1)
            tabbedPane.setSelectedIndex(index);
    }

    /**
     * Mendapatkan nama penerima berdasarkan TAB yang sedang aktif.
     * 
     * @return "ALL" jika di Lobby, atau "NamaUser" jika di tab private.
     */
    private String getActiveRecipient() {
        int index = tabbedPane.getSelectedIndex();
        if (index == -1)
            return "ALL";

        String title = tabbedPane.getTitleAt(index);
        if (title.equals("Lobby"))
            return "ALL";
        return title;
    }

    // --- LOGIKA PENGIRIMAN ---

    private void onSendButtonClick() {
        String text = inputField.getText().trim();
        if (text.isEmpty())
            return;

        // Kirim ke siapa? Tergantung Tab mana yang lagi dibuka!
        String recipient = getActiveRecipient();

        if (clientService != null) {
            clientService.sendTextMessage(recipient, text);

            // Tampilkan chat sendiri di layar (kecuali broadcast, karena server akan
            // memantulkan balik)
            if (!recipient.equals("ALL")) {
                appendToChat(recipient, "Saya: " + text);
            }
            inputField.setText("");
        } else {
            logMessage("Anda belum terhubung.");
        }
    }

    private void onBuzzButtonClick() {
        String recipient = getActiveRecipient();
        if (recipient.equals("ALL")) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Yakin ingin BUZZ semua orang di Lobby?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return;
        }

        if (clientService != null)
            clientService.sendBuzz(recipient);
    }

    private void onFileButtonClick() {
        String recipient = getActiveRecipient();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Kirim File ke: " + (recipient.equals("ALL") ? "Semua Orang" : recipient));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // Cek Limit Size (Pakai Konstanta)
            if (file.length() > MAX_FILE_SIZE) {
                JOptionPane.showMessageDialog(this,
                        "File terlalu besar! Maksimum " + (MAX_FILE_SIZE / 1024 / 1024) + "MB.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (clientService != null) {
                clientService.sendFile(recipient, file);
            }
        }
    }

    // --- LOGIKA PENERIMAAN (CALLBACKS) ---

    /**
     * Method baru yang dipanggil ClientService saat ada chat masuk.
     * Otomatis routing ke Tab yang benar.
     */
    public void incomingChat(String sender, String message, boolean isPrivate) {
        String tabName;

        if (!isPrivate) {
            tabName = "Lobby"; // Broadcast masuk ke Lobby
        } else {
            // Private Chat
            // Jika pengirimnya saya sendiri (pantulan dari server), abaikan (sudah diprint
            // saat send)
            if (sender.equals(currentUsername))
                return;

            tabName = sender; // Pesan dari Novran masuk ke Tab "Novran"
            if (!chatPanels.containsKey(tabName)) {
                createChatTab(tabName); // Bikin tab baru otomatis kalau ada yang chat!
            }
        }

        // Format pesan
        String displayMsg = String.format("[%s]: %s", sender, message);
        appendToChat(tabName, displayMsg);
    }

    private void appendToChat(String tabName, String text) {
        JTextArea area = chatPanels.get(tabName);
        if (area != null) {
            area.append(text + "\n");
            // Auto scroll
            area.setCaretPosition(area.getDocument().getLength());
        }
    }

    // --- Helper Lainnya ---

    public void logMessage(String message) {
        // Log masuk ke Status Bar bawah, bukan mengotori chat area
        statusLabel.setText(message);

        // Kalau error fatal, tampilkan popup juga
        if (message.startsWith("ERROR")) {
            JOptionPane.showMessageDialog(this, message, "Error Koneksi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void displayMessage(String msg) {
        // Backward compatibility kalau masih ada yang panggil ini
        appendToChat("Lobby", msg);
    }

    public void updateUserList(String[] users) {
        userListModel.clear();
        for (String user : users) {
            if (!user.equalsIgnoreCase(this.currentUsername)) {
                userListModel.addElement(user);
            }
        }
    }

    public void triggerBuzz(String sender) {
        // Efek getar + Pindah ke tab pengirim
        if (!sender.equals(currentUsername) && chatPanels.containsKey(sender)) {
            tabbedPane.setSelectedComponent(chatPanels.get(sender).getParent().getParent());
        }

        Point original = getLocation();
        new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    SwingUtilities.invokeLater(() -> setLocation(original.x + 10, original.y));
                    Thread.sleep(40);
                    SwingUtilities.invokeLater(() -> setLocation(original.x - 10, original.y));
                    Thread.sleep(40);
                }
                SwingUtilities.invokeLater(() -> setLocation(original));
            } catch (Exception e) {
            }
        }).start();

        logMessage("BUZZ!!! dari " + sender);
    }

    public void connectionClosed() {
        logMessage("Status: Terputus.");
        updateGuiState(false);
        userListModel.clear();
        chatPanels.clear();
        tabbedPane.removeAll();
        createChatTab("Lobby"); // Reset ke lobby kosong
    }

    public void connectionLost() {
        logMessage("Status: Koneksi Hilang!");
        connectionClosed();
    }

    // --- State Management ---

    private void updateGuiState(boolean isConnected) {
        connectButton.setEnabled(!isConnected);
        sendButton.setEnabled(isConnected);
        buzzButton.setEnabled(isConnected);
        fileButton.setEnabled(isConnected);
        inputField.setEnabled(isConnected);

        if (isConnected) {
            inputField.requestFocusInWindow();
            statusLabel.setForeground(new Color(0, 150, 0)); // Hijau
        } else {
            statusLabel.setForeground(Color.RED);
        }
    }

    public String getUsername() {
        return currentUsername;
    }

    private void showConnectDialog() {
        String usernameInput = JOptionPane.showInputDialog(this, "Masukkan Username:", "Login",
                JOptionPane.PLAIN_MESSAGE);
        if (usernameInput != null && !usernameInput.trim().isEmpty()) {
            this.currentUsername = usernameInput.trim();
            onConnectButtonClick("localhost", 50125, this.currentUsername);
        }
    }

    private void onConnectButtonClick(String host, int port, String username) {
        logMessage("Status: Menghubungi server...");
        connectButton.setEnabled(false);
        new Thread(() -> {
            boolean success = clientService.connect(host, port, username);
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    logMessage("Status: Terhubung sebagai " + username);
                    updateGuiState(true);
                } else {
                    logMessage("Status: Gagal konek.");
                    connectButton.setEnabled(true);
                }
            });
        }).start();
    }

    // --- Renderer List (Ikon Online) ---
    private class UserListRenderer extends DefaultListCellRenderer {
        private ImageIcon onlineIcon;

        public UserListRenderer() {
            // Bikin ikon bulat hijau sederhana pakai kode
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.GREEN);
            g.fillOval(0, 0, 10, 10);
            g.dispose();
            onlineIcon = new ImageIcon(img);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setIcon(onlineIcon);
            label.setText(" " + value.toString());
            label.setBorder(new EmptyBorder(5, 5, 5, 5)); // Padding biar cantik
            return label;
        }
    }

    public static void main(String[] args) {
        // Ganti LookAndFeel biar mirip Windows asli, bukan Java jadul
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(ClientMain::new);
    }
}
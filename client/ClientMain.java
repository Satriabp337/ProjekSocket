package client;

import common.Message;
import common.MessageType;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.*;
import java.awt.*;

public class ClientMain extends JFrame {
    // Komponen GUI
    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private JButton sendButton = new JButton("Kirim");

    // Socket
    private Socket socket;
    private ObjectOutputStream out;
    private String username;

    public ClientMain() {
        // 1. Setup GUI Sederhana
        setTitle("SecureLAN Client");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 2. Aksi Tombol Kirim
        sendButton.addActionListener(e -> sendMessage());

        // 3. Setup Koneksi Awal (Langsung connect saat aplikasi dibuka)
        connectToServer();

        setVisible(true);
    }

    private void connectToServer() {
        try {
            username = JOptionPane.showInputDialog("Masukkan Username:");
            // Connect ke Localhost dulu
            socket = new Socket("localhost", 50125);
            out = new ObjectOutputStream(socket.getOutputStream());

            // Kirim pesan LOGIN otomatis
            Message loginMsg = new Message(MessageType.LOGIN);
            loginMsg.setSender(username);
            out.writeObject(loginMsg);

            chatArea.append("Terhubung ke server sebagai " + username + "\n");
        } catch (Exception e) {
            chatArea.append("Gagal konek ke server: " + e.getMessage() + "\n");
        }
    }

    private void sendMessage() {
        try {
            String text = inputField.getText();
            Message msg = new Message(MessageType.TEXT);
            msg.setSender(username);
            msg.setContent(text);
            msg.setRecipient("ALL"); // Default broadcast

            out.writeObject(msg); // KIRIM KE SERVER

            chatArea.append("Saya: " + text + "\n");
            inputField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Menjalankan di Thread GUI
        SwingUtilities.invokeLater(() -> new ClientMain());
    }
}
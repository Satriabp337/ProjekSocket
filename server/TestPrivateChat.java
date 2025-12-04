package server;

import common.Message;
import common.MessageType;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class TestPrivateChat {
    public static void main(String[] args) {
        try {
            // 1. Konek ke Server
            Socket socket = new Socket("localhost", 50125);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            
            // 2. Login sebagai "Misterius"
            System.out.println("Login sebagai 'Misterius'...");
            Message login = new Message(MessageType.LOGIN);
            login.setSender("Misterius");
            out.writeObject(login);
            out.flush();

            // 3. Persiapan Kirim Pesan Private
            // Kita coba kirim ke "Satria" (Pastikan kamu sudah nyalakan ClientMain login sbg Satria)
            Message msg = new Message(MessageType.TEXT);
            msg.setSender("Misterius");
            msg.setContent("Ssst... ini pesan rahasia, jangan bilang siapa-siapa.");
            
            // INI KUNCINYA: Kita set recipient BUKAN "ALL", tapi nama spesifik
            msg.setRecipient("Satria"); 

            // 4. Kirim!
            out.writeObject(msg);
            out.flush();
            System.out.println("Pesan private terkirim ke Satria!");
            
            // Tutup
            Thread.sleep(1000);
            socket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
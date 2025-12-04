package server;

import common.Message;
import common.MessageType;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestFileTransfer {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 50125);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            // 1. Login dulu
            Message login = new Message(MessageType.LOGIN);
            login.setSender("PengirimFile");
            out.writeObject(login);

            // 2. Siapkan "File Bohongan" (Byte Array)
            String isiFile = "Ini adalah isi file PDF ceritanya. Isinya data penting.";
            byte[] fileBytes = isiFile.getBytes(StandardCharsets.UTF_8);

            // 3. Bikin Amplop FILE
            Message msg = new Message(MessageType.FILE);
            msg.setSender("PengirimFile");
            msg.setRecipient("ALL"); // Coba Broadcast dulu
            msg.setFileName("Rahasia.txt");
            msg.setFileData(fileBytes);

            // 4. Kirim
            System.out.println("Sedang mengirim file...");
            out.writeObject(msg);
            out.flush();
            System.out.println("Selesai kirim.");

            System.out.println("Menunggu respons server sebentar...");
            Thread.sleep(5000);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
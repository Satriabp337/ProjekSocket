package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String recipient; // "ALL" atau nama user tujuan
    private String content; // Isi pesan teks

    // --- FIELD KHUSUS FILE TRANSFER (GABUNGAN SATRIA & NOVRAN) ---
    // Kita tetap butuh fileName yang eksplisit biar server Satria ga bingung
    private String fileName;

    // Kita butuh fitur chunking Novran biar bisa kirim file besar
    private long fileSize; // Ukuran total file
    private byte[] fileChunk; // Potongan data (Ganti nama fileData jadi fileChunk biar sesuai protokol)

    // Konstruktor utama
    public Message(MessageType type) {
        this.type = type;
    }

    // --- GETTER & SETTER LENGKAP ---
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getFileChunk() {
        return fileChunk;
    }

    public void setFileChunk(byte[] fileChunk) {
        this.fileChunk = fileChunk;
    }
}
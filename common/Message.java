package common;

import java.io.Serializable;

public class Message implements Serializable {
    // ID unik untuk memastikan kompatibilitas serialisasi (JANGAN DIUBAH)
    private static final long serialVersionUID = 1L; 

    private MessageType type;
    private String sender;
    private String recipient; // Biasanya "ALL" atau nama user tujuan
    private String content; // Digunakan untuk teks pesan, nama file, atau pesan status

    // --- FIELD BARU UNTUK FILE TRANSFER ---
    private long fileSize;        // Ukuran total file (hanya diisi pada FILE_REQUEST)
    private byte[] fileChunk;     // Potongan data biner dari file (hanya diisi pada FILE_CHUNK)
    // ------------------------------------

    // Konstruktor utama
    public Message(MessageType type) {
        this.type = type;
    }

    // --- Getter dan Setter ---

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
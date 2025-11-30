package common;

import java.io.Serializable;

// IMPLEMENTS SERIALIZABLE WAJIB! Biar bisa dikirim lewat kabel
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String recipient; // "ALL" atau nama user tertentu
    private String content;   // Isi pesan teks
    
    // Nanti Novran akan nambahin field byte[] fileData disini
    
    public Message(MessageType type) {
        this.type = type;
    }

    // Getter & Setter (Bisa generate otomatis di VS Code: Klik Kanan -> Source Action)
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
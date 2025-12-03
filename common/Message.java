package common;

import java.io.Serializable;
public class Message implements Serializable {
    // ID unik untuk memastikan kompatibilitas serialisasi
    private static final long serialVersionUID = 1L; 

    private MessageType type;
    private String sender;
    private String recipient; // Biasanya "ALL" untuk pesan publik
    private String content;

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
}
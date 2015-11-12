package io.github.arpankapoor.message;

public class Message {
    private int messageId;
    private int senderId;
    private String type;
    private String filename;
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", senderId=" + senderId +
                ", type='" + type + '\'' +
                ", filename='" + filename + '\'' +
                ", data='" + data + '\'' +
                '}';
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}

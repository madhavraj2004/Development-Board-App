package com.example.whatapp;

public class ChatMessage {
    private String message;
    private boolean isSent;
    private String senderName;
    private String senderMac;
    private boolean isGroupMessage;
    private String groupId;

    // Constructor for simple messages (backward compatibility)
    public ChatMessage(String message, boolean isSentByUser) {
        this.message = message;
        this.isSent = isSentByUser;
        this.senderName = isSentByUser ? "Me" : "Unknown";
        this.senderMac = isSentByUser ? "SELF" : "UNKNOWN";
        this.isGroupMessage = false;
        this.groupId = null;
    }

    // Full constructor with all fields
    public ChatMessage(String message, boolean isSent, String senderName,
                       String senderMac, boolean isGroupMessage, String groupId) {
        this.message = message;
        this.isSent = isSent;
        this.senderName = senderName;
        this.senderMac = senderMac;
        this.isGroupMessage = isGroupMessage;
        this.groupId = groupId;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public boolean isSent() {
        return isSent;
    }

    // For backward compatibility
    public boolean isSentByUser() {
        return isSent;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderMac() {
        return senderMac;
    }

    public boolean isGroupMessage() {
        return isGroupMessage;
    }

    public String getGroupId() {
        return groupId;
    }

    // Setters
    public void setMessage(String message) {
        this.message = message;
    }

    public void setSent(boolean sent) {
        this.isSent = sent;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setSenderMac(String senderMac) {
        this.senderMac = senderMac;
    }

    public void setGroupMessage(boolean groupMessage) {
        this.isGroupMessage = groupMessage;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
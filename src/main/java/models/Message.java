package models;

import org.bson.Document;

public class Message {
    private String id;
    private String userId;
    private String content;
    private long timestamp;

    // Constructors
    public Message() {}

    public Message(String userId, String content, long timestamp) {
        this.userId = userId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Convert Message to MongoDB Document
    public Document toDocument() {
        Document doc = new Document("userId", userId)
                .append("content", content)
                .append("timestamp", timestamp);
        if (id != null) {
            doc.append("_id", id);
        }
        return doc;
    }
}

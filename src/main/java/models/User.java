
package models;

import org.bson.Document;

public class User {
    private String id;
    private String username;
    private String password;

    // Constructors
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Convert User to MongoDB Document
    public Document toDocument() {
        Document doc = new Document("username", username)
                .append("password", password);
        if (id != null) {
            doc.append("_id", id);
        }
        return doc;
    }
}

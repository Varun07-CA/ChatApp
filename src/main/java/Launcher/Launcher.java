package Launcher;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import javax.swing.*;
import java.awt.*;

public class Launcher {
    private static void checkMongoDBConnection() throws Exception {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        mongoClient.getDatabase("chat_app");
        mongoClient.close();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Launcher");
            frame.setLayout(new FlowLayout());
            frame.setSize(300, 150);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JButton startServerButton = new JButton("Start Server");
            JButton addClientButton = new JButton("Add Client");

            startServerButton.addActionListener(e -> {
                new Thread(() -> {
                    try {
                        checkMongoDBConnection();
                        server.ChatServer.main(new String[]{});
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                                "Failed to connect to MongoDB: " + ex.getMessage()));
                    }
                }).start();
                startServerButton.setEnabled(false);
            });

            addClientButton.addActionListener(e -> {
                String username = JOptionPane.showInputDialog(frame, "Enter username for the new client:");
                if (username != null && !username.trim().isEmpty()) {
                    new Thread(() -> {
                        try {
                            client.ChatClient clientInstance = new client.ChatClient(username);
                            SwingUtilities.invokeLater(clientInstance::launch); // Ensure UI updates on the Swing thread
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            SwingUtilities.invokeLater(() ->
                                    JOptionPane.showMessageDialog(frame, "Failed to start client: " + ex.getMessage()));
                        }
                    }).start();
                } else {
                    JOptionPane.showMessageDialog(frame, "Username cannot be empty!");
                }
            });

            frame.add(startServerButton);
            frame.add(addClientButton);
            frame.setVisible(true);
        });
    }
}
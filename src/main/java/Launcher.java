import javax.swing.*;
import java.awt.*;

public class Launcher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Launcher");
            frame.setLayout(new FlowLayout());
            frame.setSize(300, 150);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JButton startServerButton = new JButton("Start Server");
            JButton addClientButton = new JButton("Add Client");

            startServerButton.addActionListener(e -> {
                // Start the ChatServer
                new Thread(() -> {
                    try {
                        server.ChatServer.main(new String[]{});
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
                startServerButton.setEnabled(false); // Disable after starting the server
            });

            addClientButton.addActionListener(e -> {
                // Prompt for username and start a new ChatClient
                String username = JOptionPane.showInputDialog(frame, "Enter username for the new client:");
                if (username != null && !username.trim().isEmpty()) {
                    new Thread(() -> {
                        try {
                            client.ChatClient clientInstance = new client.ChatClient(username);
                            clientInstance.launch();
                        } catch (Exception ex) {
                            ex.printStackTrace();
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

package client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private BufferedReader in;
    private PrintWriter out;

    // GUI components
    private JFrame frame = new JFrame("Chat Client");
    private JTextField textField = new JTextField(50);
    private JTextArea messageArea = new JTextArea(16, 50);
    private JButton sendButton = new JButton("Send");
    private JButton emojiButton = new JButton("😊");
    private JButton fileButton = new JButton("📎");

    public ChatClient() {
        // Configure UI
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        textField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textField.setBorder(new EmptyBorder(5, 5, 5, 5));

        sendButton.setBackground(new Color(0, 123, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        emojiButton.setFocusPainted(false);
        fileButton.setFocusPainted(false);

        // Panel for bottom input
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(textField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(fileButton);
        buttonPanel.add(emojiButton);
        buttonPanel.add(sendButton);

        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add event listeners
        sendButton.addActionListener(e -> sendMessage());
        textField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> selectFile());
        emojiButton.addActionListener(e -> showEmojiPicker());
    }

    private void sendMessage() {
        String message = textField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message);
            textField.setText("");
        }
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String fileMessage = "📄 File: " + file.getName();
            out.println(fileMessage); // Send the file message
            appendMessage(fileMessage + " [Uploaded]");
        }
    }

    private void showEmojiPicker() {
        String[] emojis = {"😊", "😂", "❤️", "👍", "🎉", "😢"};
        String selectedEmoji = (String) JOptionPane.showInputDialog(
                frame, "Choose an emoji:", "Emoji Picker",
                JOptionPane.PLAIN_MESSAGE, null, emojis, emojis[0]);
        if (selectedEmoji != null) {
            textField.setText(textField.getText() + selectedEmoji);
        }
    }

    public void connect() throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        new Thread(new IncomingReader()).start();

        frame.setVisible(true);
    }

    private void appendMessage(String message) {
        messageArea.append(message + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private class IncomingReader implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("DELETE:")) {
                        String timestamp = message.substring(7);
                        fadeOutMessage(timestamp);
                    } else {
                        appendMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fadeOutMessage(String timestamp) {
        Timer timer = new Timer(100, new ActionListener() {
            float opacity = 1.0f;

            @Override
            public void actionPerformed(ActionEvent e) {
                opacity -= 0.1f;
                if (opacity <= 0) {
                    ((Timer) e.getSource()).stop();
                    removeMessageFromGUI(timestamp);
                }
            }
        });
        timer.start();
    }

    private void removeMessageFromGUI(String timestamp) {
        String[] lines = messageArea.getText().split("\n");
        StringBuilder newContent = new StringBuilder();
        for (String line : lines) {
            if (!line.startsWith("[" + timestamp + "]")) {
                newContent.append(line).append("\n");
            }
        }
        messageArea.setText(newContent.toString());
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.connect();
    }
}

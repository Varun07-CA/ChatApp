package client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Chat Client");
    private JTextField textField = new JTextField(50);
    private JTextArea messageArea = new JTextArea(16, 50);
    private JButton sendButton = new JButton("Send");

    public ChatClient() {
        textField.setEditable(true);
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(sendButton, BorderLayout.NORTH);
        frame.pack();

        sendButton.addActionListener(e -> sendMessage());
        textField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String message = textField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message);
            textField.setText("");
        }
    }

    public void connect() throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        new Thread(new IncomingReader()).start();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private class IncomingReader implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("DELETE:")) {
                        String timestamp = message.substring(7);
                        deleteMessageFromGUI(timestamp);
                    } else if (message.startsWith("NOTIFICATION:")) {
                        String notification = message.substring(13);
                        JOptionPane.showMessageDialog(frame, notification);
                    } else {
                        messageArea.append(message + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteMessageFromGUI(String timestamp) {
        try {
            String[] lines = messageArea.getText().split("\n");
            StringBuilder newContent = new StringBuilder();
            for (String line : lines) {
                if (!line.startsWith("[" + timestamp + "]")) {
                    newContent.append(line).append("\n");
                }
            }
            messageArea.setText(newContent.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.connect();
    }
}
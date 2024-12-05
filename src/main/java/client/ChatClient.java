package client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
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
    private JButton privateMessageButton = new JButton("Private Message");
    private String username;

    public ChatClient(String username) {
        this.username = username;

        // Configure UI
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sendButton.setBackground(new Color(0, 123, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(textField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(privateMessageButton);
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sendButton.addActionListener(e -> sendMessage());
        textField.addActionListener(e -> sendMessage());

        privateMessageButton.addActionListener(e -> sendPrivateMessage());
    }

    public void launch() {
        try {
            connect();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = textField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message);
            textField.setText("");
        }
    }

    private void sendPrivateMessage() {
        String recipient = JOptionPane.showInputDialog(frame, "Enter the recipient's username:");
        String message = JOptionPane.showInputDialog(frame, "Enter your private message:");
        if (recipient != null && message != null && !recipient.trim().isEmpty() && !message.trim().isEmpty()) {
            out.println("PRIVATE:" + recipient + ":" + message);
        } else {
            JOptionPane.showMessageDialog(frame, "Recipient and message cannot be empty!");
        }
    }

    private void connect() throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Send username to server
        out.println("USER:" + username);

        new Thread(new IncomingReader()).start();
    }

    private class IncomingReader implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    messageArea.append(message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

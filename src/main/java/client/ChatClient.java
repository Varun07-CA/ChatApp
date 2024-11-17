package client;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost"; // Server address
    private static final int SERVER_PORT = 12345;  // Server port number
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Chat Client");
    private JTextField textField = new JTextField(50);
    private JTextArea messageArea = new JTextArea(16, 50);
    private JButton sendButton = new JButton("Send");

    public ChatClient() {
        // Setup GUI components
        textField.setEditable(true);
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(sendButton, BorderLayout.NORTH);

        frame.pack();

        // Add action listeners
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Send the message when the Send button is pressed
                sendMessage();
            }
        });

        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Send the message when Enter is pressed
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String message = textField.getText();
        if (!message.trim().isEmpty()) {
            // Append the message to messageArea immediately (client-side feedback)
            out.println(message);  // Send message to server
            textField.setText("");  // Clear the text field
        }
    }

    public void connect() throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Start a new thread to listen for incoming messages
        new Thread(new IncomingReader()).start();

        // Set up the JFrame to show the chat window
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private class IncomingReader implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    // Only display new messages from the server (messages sent by other clients)
                    if (!message.startsWith("INITIAL:")) {
                        messageArea.append(message + "\n");  // Append new message to message area
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.connect();
    }
}

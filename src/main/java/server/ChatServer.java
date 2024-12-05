package server;

import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<String, PrintWriter> clients = new ConcurrentHashMap<>(); // Tracks usernames and writers
    private static JTextArea logArea = new JTextArea(20, 30);
    private static DefaultListModel<String> clientListModel = new DefaultListModel<>();
    private static JList<String> clientList = new JList<>(clientListModel);
    private static DefaultListModel<String> messageListModel = new DefaultListModel<>();
    private static JList<String> messageList = new JList<>(messageListModel);
    private static MongoCollection<Document> messagesCollection;
    private static int totalMessages = 0;

    public static void main(String[] args) throws IOException {
        try {
            MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
            MongoDatabase database = mongoClient.getDatabase("chat_app");
            messagesCollection = database.getCollection("messages");
            System.out.println("Connected to MongoDB");

            setupGUI();

            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started...");
            try {
                while (true) {
                    new ClientHandler(serverSocket.accept()).start();
                }
            } finally {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupGUI() {
        JFrame frame = new JFrame("Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLayout(new BorderLayout());

        // Left Pane: Logs
        JPanel leftPane = new JPanel(new BorderLayout());
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));
        leftPane.add(logScrollPane, BorderLayout.CENTER);

        // Right Pane: Clients and Actions
        JPanel rightPane = new JPanel(new BorderLayout());
        clientList.setBorder(BorderFactory.createTitledBorder("Connected Clients"));
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        rightPane.add(clientScrollPane, BorderLayout.NORTH);

        // Messages and Delete Button
        JPanel actionsPane = new JPanel(new BorderLayout());
        messageList.setBorder(BorderFactory.createTitledBorder("Messages"));
        JScrollPane messageScrollPane = new JScrollPane(messageList);
        actionsPane.add(messageScrollPane, BorderLayout.CENTER);

        JButton deleteButton = new JButton("Delete Selected Messages");
        deleteButton.addActionListener(e -> deleteSelectedMessages());
        actionsPane.add(deleteButton, BorderLayout.SOUTH);
        rightPane.add(actionsPane, BorderLayout.CENTER);

        frame.add(leftPane, BorderLayout.WEST);
        frame.add(rightPane, BorderLayout.CENTER);
        frame.setVisible(true);

        // Load messages from database
        loadMessagesFromDatabase();
    }

    private static void loadMessagesFromDatabase() {
        messageListModel.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy hh:mm a");
        for (Document doc : messagesCollection.find()) {
            String message = doc.getString("message");
            Long timestamp = doc.getLong("timestamp");
            String formattedTimestamp = sdf.format(new java.util.Date(timestamp));
            messageListModel.addElement("[" + timestamp + "] " + formattedTimestamp + " - " + message);
            totalMessages++;
        }
    }

    private static void deleteSelectedMessages() {
        List<String> selectedMessages = messageList.getSelectedValuesList();
        if (selectedMessages.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No messages selected for deletion.");
            return;
        }

        for (String selected : selectedMessages) {
            Long timestamp = extractTimestampFromMessage(selected);
            if (timestamp != null) {
                deleteMessageFromDatabase(timestamp);
                messageListModel.removeElement(selected);
                broadcastDeletion(timestamp);
            }
        }
    }

    private static Long extractTimestampFromMessage(String message) {
        try {
            int start = message.indexOf('[') + 1;
            int end = message.indexOf(']');
            if (start > 0 && end > start) {
                String timestampString = message.substring(start, end);
                return Long.parseLong(timestampString);
            }
        } catch (Exception e) {
            System.out.println("Error extracting timestamp from message: " + e.getMessage());
        }
        return null;
    }

    private static void deleteMessageFromDatabase(Long timestamp) {
        if (timestamp != null) {
            Document filter = new Document("timestamp", timestamp);
            DeleteResult result = messagesCollection.deleteOne(filter);
            if (result.getDeletedCount() > 0) {
                logArea.append("Deleted message with timestamp: " + timestamp + "\n");
                totalMessages--;
            } else {
                logArea.append("No message found with timestamp: " + timestamp + "\n");
            }
        }
    }

    private static void broadcastDeletion(Long timestamp) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println("DELETE:" + timestamp);
            }
        }
    }

    private static class ClientHandler extends Thread {
        private PrintWriter out;
        private BufferedReader in;
        private Socket socket;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Handle username
                String userInfo = in.readLine();
                if (userInfo != null && userInfo.startsWith("USER:")) {
                    username = userInfo.substring(5);
                    clients.put(username, out);
                    clientListModel.addElement(username);
                    logArea.append(username + " connected.\n");
                }

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(username + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }

                if (username != null) {
                    clients.remove(username);
                    clientListModel.removeElement(username);
                    logArea.append(username + " disconnected.\n");
                }
            }
        }

        private static void handlePrivateMessage(String sender, String recipient, String message) {
            PrintWriter recipientWriter = clients.get(recipient);
            if (recipientWriter != null) {
                recipientWriter.println("PRIVATE from " + sender + ": " + message);
            } else {
                PrintWriter senderWriter = clients.get(sender);
                if (senderWriter != null) {
                    senderWriter.println("Recipient " + recipient + " not found.");
                }
            }
        }

        // Modify the broadcastMessage method to handle private messages
        private void broadcastMessage(String message) {
            if (message.startsWith("PRIVATE:")) {
                String[] parts = message.split(":", 3);
                if (parts.length == 3) {
                    handlePrivateMessage(username, parts[1], parts[2]);
                }
            } else {
                long timestamp = System.currentTimeMillis();
                String formattedMessage = "[" + timestamp + "] " + message;

                synchronized (clientWriters) {
                    for (PrintWriter writer : clientWriters) {
                        writer.println(formattedMessage);
                    }
                }

                messageListModel.addElement(formattedMessage);
                saveMessageToDatabase(message, timestamp);
            }
        }


        private void saveMessageToDatabase(String message, long timestamp) {
            Document doc = new Document("message", message).append("timestamp", timestamp);
            messagesCollection.insertOne(doc);
            totalMessages++;
        }
    }
}



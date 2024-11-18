package server;

import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;





public class ChatServer {
    private static final int PORT = 12345;
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static JTextArea messageArea = new JTextArea(20, 50);
    private static JFrame frame = new JFrame("Chat Server");
    private static MongoCollection<Document> messagesCollection;
    private static String selectedMessage = null;
    private static Long selectedMessageTimestamp = null;

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
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JButton deleteButton = new JButton("Delete Message");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedMessageTimestamp != null) {
                    deleteMessageFromDatabase(selectedMessageTimestamp);
                    deleteMessageFromGUI();
                    broadcastDeletion(selectedMessageTimestamp);
                    selectedMessage = null;
                    selectedMessageTimestamp = null;
                } else {
                    System.out.println("No message selected for deletion.");
                }
            }
        });
        frame.getContentPane().add(deleteButton, BorderLayout.SOUTH);

        messageArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectMessageForDeletion(e);
            }
        });

        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        loadMessagesFromDatabase();
    }

    private static void loadMessagesFromDatabase() {
        messageArea.setText("");
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy hh:mm a");
        for (Document doc : messagesCollection.find()) {
            String message = doc.getString("message");
            Long timestamp = doc.getLong("timestamp");
            String formattedTimestamp = sdf.format(new java.util.Date(timestamp));
            messageArea.append("[" + timestamp + "] " + formattedTimestamp + " - " + message + "\n");
        }
    }

    private static void deleteMessageFromDatabase(Long timestamp) {
        if (timestamp != null) {
            Document filter = new Document("timestamp", timestamp);
            DeleteResult result = messagesCollection.deleteOne(filter);
            if (result.getDeletedCount() > 0) {
                System.out.println("Deleted message with timestamp: " + timestamp);
            } else {
                System.out.println("No message found with timestamp: " + timestamp);
            }
        } else {
            System.out.println("Error: Timestamp is null. Cannot delete message from the database.");
        }
    }

    private static void deleteMessageFromGUI() {
        loadMessagesFromDatabase();
    }

    private static void broadcastDeletion(Long timestamp) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println("DELETE:" + timestamp);
            }
        }
    }

    private static void selectMessageForDeletion(MouseEvent e) {
        int offset = messageArea.viewToModel(e.getPoint());
        try {
            int start = messageArea.getLineStartOffset(messageArea.getLineOfOffset(offset));
            int end = messageArea.getLineEndOffset(messageArea.getLineOfOffset(offset));
            String selectedText = messageArea.getText(start, end - start).trim();
            selectedMessageTimestamp = extractTimestampFromMessage(selectedText);
            if (selectedMessageTimestamp != null) {
                selectedMessage = selectedText;
                System.out.println("Selected message: " + selectedMessage);
                System.out.println("Selected timestamp: " + selectedMessageTimestamp);
            } else {
                System.out.println("Error: Could not extract timestamp from selected message.");
                selectedMessage = null;
            }
        } catch (Exception ex) {
            System.out.println("Error selecting message: " + ex.getMessage());
            selectedMessage = null;
            selectedMessageTimestamp = null;
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
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.out.println("Error extracting timestamp from message: " + e.getMessage() + ". Message: " + message);
        }
        return null;
    }

    private static void broadcastMessage(String message) {
        long timestamp = System.currentTimeMillis();
        String formattedMessage = "[" + timestamp + "] " + message;
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(formattedMessage);
            }
        }
        messageArea.append(formattedMessage + "\n");
        saveMessageToDatabase(message, timestamp);
    }

    private static void saveMessageToDatabase(String message, long timestamp) {
        Document doc = new Document("message", message).append("timestamp", timestamp);
        messagesCollection.insertOne(doc);
    }

    private static class ClientHandler extends Thread {
        private PrintWriter out;
        private BufferedReader in;
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }
                sendAllMessagesToClient();
                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(message);
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
            }
        }

        private void sendAllMessagesToClient() {
            for (Document doc : messagesCollection.find()) {
                String message = doc.getString("message");
                Long timestamp = doc.getLong("timestamp");
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy hh:mm a");
                String formattedTimestamp = sdf.format(new java.util.Date(timestamp));
                out.println("[" + timestamp + "] " + formattedTimestamp + " - " + message);
            }
        }
    }
}
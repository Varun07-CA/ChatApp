package server;

import com.mongodb.client.*;
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
    private static Set<PrintWriter> clientWriters = new HashSet<>(); // Store client writers
    private static JTextArea messageArea = new JTextArea(20, 50); // JTextArea for displaying messages in JFrame
    private static JFrame frame = new JFrame("Chat Server");
    private static MongoCollection<Document> messagesCollection; // MongoDB collection to store messages
    private static String selectedMessage = null; // To store the selected message for deletion
    private static Long selectedMessageTimestamp = null; // To store the selected timestamp for deletion

    public static void main(String[] args) throws IOException {
        try {
            // Connect to MongoDB
            MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017"); // Default MongoDB URI
            MongoDatabase database = mongoClient.getDatabase("chat_app");
            messagesCollection = database.getCollection("messages");

            System.out.println("Connected to MongoDB");

            // Setup the GUI
            setupGUI();

            // Start the server
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started...");
            try {
                while (true) {
                    new ClientHandler(serverSocket.accept()).start(); // Handle each new client in a separate thread
                }
            } finally {
                serverSocket.close(); // Ensure the server socket is closed when exiting
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setupGUI() {
        // Setup the message area
        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);

        // Add a delete button
        JButton deleteButton = new JButton("Delete Message");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedMessageTimestamp != null) {
                    deleteMessageFromDatabase(selectedMessageTimestamp); // Delete from database
                    deleteMessageFromGUI(); // Refresh the GUI
                } else {
                    System.out.println("No message selected for deletion.");
                }
            }
        });
        frame.getContentPane().add(deleteButton, BorderLayout.SOUTH);

        // Add MouseListener to JTextArea for message selection
        messageArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectMessageForDeletion(e);
            }
        });

        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Load all previous messages from the database
        loadMessagesFromDatabase();
    }

    private static void loadMessagesFromDatabase() {
        // Clear the message area before reloading
        messageArea.setText("");

        // SimpleDateFormat to format timestamp into a human-readable format
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy hh:mm a");

        // Fetch all messages from MongoDB and display in message area
        for (Document doc : messagesCollection.find()) {
            String message = doc.getString("message");
            Long timestamp = doc.getLong("timestamp"); // timestamp is stored as int64
            String formattedTimestamp = sdf.format(new java.util.Date(timestamp)); // Convert to readable format

            // Append to the JTextArea in the GUI
            messageArea.append("[" + timestamp + "] " + formattedTimestamp + " - " + message + "\n");
        }
    }

    private static void deleteMessageFromDatabase(Long timestamp) {
        if (timestamp != null) {
            // Construct the filter to find the document with the matching timestamp
            Document filter = new Document("timestamp", timestamp);

            // Attempt to delete the document from the collection
            messagesCollection.deleteOne(filter);

            System.out.println("Deleted message with timestamp: " + timestamp);
        } else {
            System.out.println("Error: Timestamp is null. Cannot delete message from the database.");
        }
    }

    private static void deleteMessageFromGUI() {
        // Refresh the GUI by reloading all messages from the database
        loadMessagesFromDatabase();
    }

    private static void selectMessageForDeletion(MouseEvent e) {
        int offset = messageArea.viewToModel(e.getPoint());
        try {
            int start = messageArea.getLineStartOffset(messageArea.getLineOfOffset(offset));
            int end = messageArea.getLineEndOffset(messageArea.getLineOfOffset(offset));
            String selectedText = messageArea.getText(start, end - start).trim();

            selectedMessage = selectedText; // Store the selected message
            selectedMessageTimestamp = extractTimestampFromMessage(selectedText);
            System.out.println("Selected message: " + selectedMessage);
            System.out.println("Selected timestamp: " + selectedMessageTimestamp);
        } catch (Exception ex) {
            System.out.println("Error selecting message: " + ex.getMessage());
        }
    }

    private static Long extractTimestampFromMessage(String message) {
        // Assuming the message format includes the timestamp in this format "[timestamp] formatted_date - message"
        try {
            int start = message.indexOf('[') + 1;
            int end = message.indexOf(']');
            String timestampString = message.substring(start, end);
            return Long.parseLong(timestampString);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.out.println("Error extracting timestamp from message: " + e.getMessage());
            return null;
        }
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

                // Add the client to the set of writers
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                // Broadcast all previous messages to the client
                sendAllMessagesToClient();

                String message;
                while ((message = in.readLine()) != null) {
                    // Broadcast the new message to all clients
                    broadcastMessage(message);
                    saveMessageToDatabase(message); // Save message to MongoDB
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close(); // Close the socket when done
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clientWriters) {
                    clientWriters.remove(out); // Remove client from the set when disconnected
                }
            }
        }

        private static void broadcastMessage(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message); // Send the message to each connected client
                }
            }

            // Optionally, append the message to the GUI message area for server-side view
            messageArea.append(message + "\n");
        }

        private void sendAllMessagesToClient() {
            // Send all previous messages from MongoDB to the new client
            for (Document doc : messagesCollection.find()) {
                String message = doc.getString("message");
                Long timestamp = doc.getLong("timestamp");
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy hh:mm a");
                String formattedTimestamp = sdf.format(new java.util.Date(timestamp));

                out.println("[" + timestamp + "] " + formattedTimestamp + " - " + message); // Send to the client
            }
        }

        private void saveMessageToDatabase(String message) {
            // Save the message to MongoDB with timestamp
            Document doc = new Document("message", message)
                    .append("timestamp", System.currentTimeMillis());
            messagesCollection.insertOne(doc);
        }
    }
}

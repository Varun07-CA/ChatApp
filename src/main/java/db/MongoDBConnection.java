
package db;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBConnection {
    private static final String URI = "mongodb://localhost:27017"; // Change as per your MongoDB URI
    private static final String DATABASE_NAME = "chat_app";

    private static MongoClient mongoClient;
    private static MongoDatabase database;




    static {
        mongoClient = MongoClients.create(URI);
        database = mongoClient.getDatabase(DATABASE_NAME);
    }

    public static MongoDatabase getDatabase() {
        return database;
    }
}

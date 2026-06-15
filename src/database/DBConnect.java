package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

public class DBConnect {
    // Always open a fresh connection per caller to avoid shared-connection locking across threads.
    public static Connection connect() {
        Connection conn = null;
        try {
            // Prefer project-local database file if present
            File dbFile = new File("database" + File.separator + "attendance.db");
            if (!dbFile.exists()) {
                // fall back to home directory
                dbFile = new File(System.getProperty("user.home") + File.separator + "attendance.db");
            }
            String abs = dbFile.getAbsolutePath();
            String url = "jdbc:sqlite:" + abs;
            System.out.println("Opening SQLite DB at: " + abs);
            conn = DriverManager.getConnection(url);

            // set pragmas to reduce locking issues
            try (Statement s = conn.createStatement()) {
                // enable foreign keys
                s.execute("PRAGMA foreign_keys = ON;");
                // use WAL mode to improve concurrency
                s.execute("PRAGMA journal_mode = WAL;");
                // set busy timeout (milliseconds)
                s.execute("PRAGMA busy_timeout = 5000;");
            } catch (Exception e) {
                // non-fatal; continue
                System.out.println("DB PRAGMA setup warning: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }

        return conn;
    }
}
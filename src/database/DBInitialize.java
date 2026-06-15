package database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class DBInitialize {

    public static void initialize() {

        try {

            Connection conn = DBConnect.connect();
            Statement stmt = conn.createStatement();
            
            // USERS TABLE
            
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");"
            );

            // STUDENTS TABLE
            
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS students ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "student_id TEXT UNIQUE NOT NULL,"
                + "full_name TEXT NOT NULL,"
                + "course TEXT,"
                + "year_level TEXT,"
                + "email TEXT"
                + ");"
            );

            
            // TEACHERS TABLE
            
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS teachers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "teacher_id TEXT UNIQUE NOT NULL,"
                + "full_name TEXT NOT NULL,"
                + "email TEXT"
                + ");"
            );

            
            // CLASSES TABLE (NEW CORE TABLE)
            
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS classes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "class_code TEXT UNIQUE NOT NULL,"
                + "class_name TEXT NOT NULL,"
                + "teacher_id TEXT NOT NULL,"
                + "schedule TEXT,"
                + "room TEXT,"
                + "FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id)"
                + ");"
            );

            // ENROLLMENTS TABLE
            
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS enrollments ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "student_id TEXT NOT NULL,"
                + "class_id INTEGER NOT NULL,"
                + "date_enrolled TEXT DEFAULT (datetime('now')),"
                + "status TEXT DEFAULT 'Active',"
                + "FOREIGN KEY (student_id) REFERENCES students(student_id),"
                + "FOREIGN KEY (class_id) REFERENCES classes(id)"
                + ");"
            );

            
            // ATTENDANCE SESSIONS
            
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS attendance_sessions ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "class_id INTEGER NOT NULL,"
                + "session_date TEXT NOT NULL,"
                + "created_by TEXT,"
                + "FOREIGN KEY (class_id) REFERENCES classes(id)"
                + ");"
            );

            
            // ATTENDANCE RECORDS
            
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS attendance_records ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "session_id INTEGER NOT NULL,"
                + "student_id TEXT NOT NULL,"
                + "status TEXT NOT NULL,"
                + "timestamp TEXT,"
                + "FOREIGN KEY (session_id) REFERENCES attendance_sessions(id),"
                + "FOREIGN KEY (student_id) REFERENCES students(student_id)"
                + ");"
            );

            // Run migrations for existing DBs: ensure columns exist
            ensureColumnExists(conn, "enrollments", "date_enrolled", "TEXT DEFAULT (datetime('now'))");
            ensureColumnExists(conn, "enrollments", "status", "TEXT DEFAULT 'Active'");
            ensureColumnExists(conn, "attendance_records", "timestamp", "TEXT");
            // Ensure users table has full_name and email columns for admin UI
            ensureColumnExists(conn, "users", "full_name", "TEXT");
            ensureColumnExists(conn, "users", "email", "TEXT");

            System.out.println("Database initialized successfully!");
            
            // INSERT DEFAULT ADMIN USER (if not exists)
            insertDefaultUsers(stmt);

        } catch (Exception e) {

            System.out.println("DB Init Error: " + e.getMessage());
        }
    }
    
    private static void insertDefaultUsers(Statement stmt) {
        try {
            String checkSQL = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            java.sql.ResultSet rs = stmt.executeQuery(checkSQL);
            
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("DEBUG: Admin count in DB = " + count); // DAGDAG
                
                if (count == 0) {
                    String hashedPassword = util.PasswordUtil.hashPassword("admin123");
                    System.out.println("DEBUG: Inserting admin with hash = " + hashedPassword); // DAGDAG
                    
                    String insertSQL = String.format(
                        "INSERT INTO users (username, password, role) VALUES ('admin', '%s', 'admin')",
                        hashedPassword.replace("'", "''")
                    );
                    stmt.executeUpdate(insertSQL);
                    System.out.println("✓ Default admin user created");
                } else {
                    // DAGDAG - tingnan kung ano ang nakastore
                    java.sql.ResultSet adminRS = stmt.executeQuery("SELECT username, password FROM users WHERE username = 'admin'");
                    if (adminRS.next()) {
                        System.out.println("DEBUG: Existing admin hash = " + adminRS.getString("password"));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("insertDefaultUsers ERROR: " + e.getMessage()); // DAGDAG
        }
    }

    private static void ensureColumnExists(Connection conn, String tableName, String columnName, String columnDef) {
        try {
            // Check PRAGMA table_info
            ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(" + tableName + ")");
            boolean found = false;
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null && name.equalsIgnoreCase(columnName)) {
                    found = true;
                    break;
                }
            }
            rs.close();

            if (!found) {
               
                String typeOnly = columnDef;
                String defaultExpr = null;

                // Try to split out DEFAULT part if present
                int idx = columnDef.toUpperCase().indexOf("DEFAULT");
                if (idx != -1) {
                    typeOnly = columnDef.substring(0, idx).trim();
                    defaultExpr = columnDef.substring(idx + "DEFAULT".length()).trim();
                }

                String addSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + typeOnly;
                conn.createStatement().executeUpdate(addSql);
                System.out.println("Added column '" + columnName + "' to table '" + tableName + "'.");

                // If there was a default expression/value, try to backfill existing rows
                if (defaultExpr != null && !defaultExpr.isEmpty()) {
                    try {
                        // Remove surrounding parentheses if present
                        String expr = defaultExpr;
                        if (expr.startsWith("(" ) && expr.endsWith(")")) {
                            expr = expr.substring(1, expr.length()-1).trim();
                        }
                        String updateSql = "UPDATE " + tableName + " SET " + columnName + " = " + expr + " WHERE " + columnName + " IS NULL";
                        conn.createStatement().executeUpdate(updateSql);
                        System.out.println("Backfilled column '" + columnName + "' on table '" + tableName + "' with default expression.");
                    } catch (Exception ex) {
                        System.out.println("Backfill for " + columnName + " failed: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Migration error for " + tableName + ": " + e.getMessage());
        }
    }
}
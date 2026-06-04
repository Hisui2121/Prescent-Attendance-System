package database;

import java.sql.Connection;
import java.sql.Statement;

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
                + "FOREIGN KEY (session_id) REFERENCES attendance_sessions(id),"
                + "FOREIGN KEY (student_id) REFERENCES students(student_id)"
                + ");"
            );

            System.out.println("Database initialized successfully!");
            
            // INSERT DEFAULT ADMIN USER (if not exists)
            insertDefaultUsers(stmt);

        } catch (Exception e) {

            System.out.println("DB Init Error: " + e.getMessage());
        }
    }
    
    private static void insertDefaultUsers(Statement stmt) {
        try {
            // Check if admin user exists
            String checkSQL = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            java.sql.ResultSet rs = stmt.executeQuery(checkSQL);
            
            if (rs.next() && rs.getInt(1) == 0) {
                // Hash password using PasswordUtil
                util.PasswordUtil password = new util.PasswordUtil();
                String hashedPassword = util.PasswordUtil.hashPassword("admin123");
                
                String insertSQL = String.format(
                    "INSERT INTO users (username, password, role) VALUES ('admin', '%s', 'admin')",
                    hashedPassword.replace("'", "''")
                );
                stmt.executeUpdate(insertSQL);
                System.out.println("✓ Default admin user created (username: admin, password: admin123)");
                
                // Insert a sample teacher
                String teacherPassword = util.PasswordUtil.hashPassword("teacher123");
                String insertTeacher = String.format(
                    "INSERT INTO users (username, password, role) VALUES ('teacher1', '%s', 'teacher')",
                    teacherPassword.replace("'", "''")
                );
                stmt.executeUpdate(insertTeacher);
                System.out.println("✓ Sample teacher user created (username: teacher1, password: teacher123)");
            }
        } catch (Exception e) {
            // Users may already exist, ignore error
        }
    }
}
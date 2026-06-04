package util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    
    private static final String LOG_FILE = "system_logs.txt";
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String action, String user, String details, String logType) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = String.format("[%s] [%s] [%s] User: %s | Details: %s",
            timestamp, logType.toUpperCase(), action, user, details);
        
        writeToFile(logMessage);
        System.out.println("📋 " + logMessage);
    }

    public static void logLogin(String username, boolean success) {
        String details = success ? "Login successful" : "Login failed - invalid credentials";
        log("LOGIN", username, details, success ? "INFO" : "WARN");
    }

    public static void logLogout(String username) {
        log("LOGOUT", username, "User logged out", "INFO");
    }

    public static void logAction(String username, String action, String details) {
        log(action, username, details, "INFO");
    }

    public static void logError(String username, String action, String errorMessage) {
        log(action, username, "ERROR: " + errorMessage, "ERROR");
    }

    public static void logUnauthorizedAccess(String username, String action) {
        log(action, username, "UNAUTHORIZED ACCESS ATTEMPT", "WARN");
    }

    public static void logStudentAdded(String username, String studentId, String studentName) {
        String details = String.format("Added student %s (%s)", studentName, studentId);
        log("ADD_STUDENT", username, details, "INFO");
    }

    public static void logAttendanceRecorded(String username, String sessionId, String details) {
        log("RECORD_ATTENDANCE", username, "Session ID: " + sessionId + " | " + details, "INFO");
    }

    public static void logClassCreated(String username, String classCode, String className) {
        String details = String.format("Created class %s (%s)", className, classCode);
        log("ADD_CLASS", username, details, "INFO");
    }

    private static void writeToFile(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(message);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    public static void viewLogs() {
        try (BufferedReader br = new BufferedReader(new FileReader(LOG_FILE))) {
            System.out.println("\n===== SYSTEM LOGS =====");
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                count++;
            }
            if (count == 0) {
                System.out.println("No logs found.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("No logs available yet.");
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
    }
}

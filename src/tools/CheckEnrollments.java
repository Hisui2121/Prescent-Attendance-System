package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckEnrollments {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:database/attendance.db";
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Connected to DB: " + url);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, student_id, class_id, date_enrolled, status FROM enrollments ORDER BY id DESC LIMIT 50");
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println("id=" + rs.getInt("id") + " student_id=" + rs.getString("student_id") + " class_id=" + rs.getInt("class_id") + " date=" + rs.getString("date_enrolled") + " status=" + rs.getString("status"));
            }
            System.out.println("Total rows printed=" + count);
        } catch (Exception e) {
            System.out.println("Error reading DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

package tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.File;

public class SchemaDump {
    public static void main(String[] args) {
        try {
            File dbFile = new File("database/attendance.db");
            String abs = dbFile.getAbsolutePath();
            String url = "jdbc:sqlite:" + abs;
            System.out.println("Opening DB: " + abs);
            try (Connection conn = DriverManager.getConnection(url)) {
                System.out.println("Connected.");
                Statement stmt = conn.createStatement();
                System.out.println("Table columns for 'enrollments':");
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(enrollments);");
                int cnt = 0;
                while (rs.next()) {
                    cnt++;
                    System.out.println("  cid=" + rs.getInt("cid") + " name=" + rs.getString("name") + " type=" + rs.getString("type") + " notnull=" + rs.getInt("notnull") + " dflt_value=" + rs.getString("dflt_value") + " pk=" + rs.getInt("pk"));
                }
                if (cnt == 0) System.out.println("  (no table 'enrollments' found)");

                System.out.println("\nList first 10 rows (if table exists):");
                try {
                    ResultSet rs2 = stmt.executeQuery("SELECT * FROM enrollments LIMIT 10");
                    int r = 0;
                    while (rs2.next()) {
                        r++;
                        StringBuilder sb = new StringBuilder();
                        sb.append("row ").append(r).append(": ");
                        for (int i = 1; i <= rs2.getMetaData().getColumnCount(); i++) {
                            sb.append(rs2.getMetaData().getColumnName(i)).append("=").append(rs2.getString(i));
                            if (i < rs2.getMetaData().getColumnCount()) sb.append(", ");
                        }
                        System.out.println(sb.toString());
                    }
                    if (r == 0) System.out.println("  (no rows)");
                } catch (Exception ex) {
                    System.out.println("Cannot list rows: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

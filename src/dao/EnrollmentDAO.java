package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import database.DBConnect;
import model.Enrollment;

public class EnrollmentDAO {

    // ENROLL STUDENT TO LESSON
    public boolean enrollStudent(Enrollment enrollment) {

    	String sql =
    	    "INSERT INTO enrollments (student_id, class_id, date_enrolled) "
    	  + "VALUES (?, ?, datetime('now'))";

        try {

            Connection conn = DBConnect.connect();

            // ensure migration columns exist (date_enrolled, status)
            ensureColumns(conn);
            
            PreparedStatement pstmt =
                    conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, enrollment.getStudentId());
            pstmt.setInt(2, enrollment.getClassId());

            int updated = pstmt.executeUpdate();

            // try to read generated id
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys != null && keys.next()) {
                    int id = keys.getInt(1);
                    enrollment.setId(id);
                    System.out.println("Generated enrollment id=" + id);
                }
            } catch (Exception ex) {
                // ignore
            }
            
            System.out.println("Student enrolled successfully! student_id=" + enrollment.getStudentId() + " class_id=" + enrollment.getClassId() + " rowsUpdated=" + updated);

            return true;

        } catch (Exception e) {

            System.out.println("Enroll error: " + e.getMessage());
            return false;
        }
    }

    // VIEW ALL ENROLLMENTS
    public ArrayList<Enrollment> getAllEnrollments() {

        ArrayList<Enrollment> list = new ArrayList<>();

        String sql = "SELECT id, student_id, class_id, date_enrolled, status FROM enrollments";

        try {

            Connection conn = DBConnect.connect();

            // Ensure columns exist to avoid SQL errors if DB is older schema
            ensureColumns(conn);
            
            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                Enrollment e = new Enrollment();

                e.setId(rs.getInt("id"));
                e.setStudentId(rs.getString("student_id"));
                e.setClassId(rs.getInt("class_id"));
                e.setDateEnrolled(rs.getString("date_enrolled"));
                e.setStatus(rs.getString("status"));

                list.add(e);
            }

            System.out.println("Loaded enrollments: " + list.size());
            for (Enrollment en : list) {
                System.out.println("  enrollment id=" + en.getId() + " student_id=" + en.getStudentId() + " class_id=" + en.getClassId() + " date=" + en.getDateEnrolled() + " status=" + en.getStatus());
            }

        } catch (Exception e) {

            System.out.println("Read enroll error: " + e.getMessage());
        }

        return list;
    }

    // Ensure migrations: add date_enrolled and status columns if missing
    private void ensureColumns(Connection conn) {
        try {
            ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(enrollments);");
            boolean hasDate = false;
            boolean hasStatus = false;
            while (rs.next()) {
                String name = rs.getString("name");
                if ("date_enrolled".equalsIgnoreCase(name)) hasDate = true;
                if ("status".equalsIgnoreCase(name)) hasStatus = true;
            }
            rs.close();

            Statement stmt = conn.createStatement();
            if (!hasDate) {
                try {
                    // SQLite does not allow non-constant defaults in ALTER TABLE on some versions.
                    // Add the column without a default, then backfill existing rows.
                    stmt.executeUpdate("ALTER TABLE enrollments ADD COLUMN date_enrolled TEXT");
                    stmt.executeUpdate("UPDATE enrollments SET date_enrolled = datetime('now') WHERE date_enrolled IS NULL");
                    System.out.println("Migration: added column date_enrolled to enrollments (backfilled existing rows)");
                } catch (Exception ex) { System.out.println("Migration add date_enrolled failed: " + ex.getMessage()); }
            }
            if (!hasStatus) {
                try {
                    stmt.executeUpdate("ALTER TABLE enrollments ADD COLUMN status TEXT DEFAULT 'Active'");
                    System.out.println("Migration: added column status to enrollments");
                } catch (Exception ex) { System.out.println("Migration add status failed: " + ex.getMessage()); }
            }
        } catch (Exception e) {
            System.out.println("ensureColumns error: " + e.getMessage());
        }
    }

    // REMOVE ENROLLMENT
    public boolean removeEnrollment(int id) {

        String sql = "DELETE FROM enrollments WHERE id=?";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setInt(1, id);

            pstmt.executeUpdate();

            System.out.println("Enrollment removed!");

            return true;

        } catch (Exception e) {

            System.out.println("Delete enroll error: " + e.getMessage());
            return false;
        }
    }
}
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import database.DBConnect;
import model.AttendanceRecord;

public class AttendanceRecordDAO {

    // Backwards-compatible overload
    public boolean generateAttendanceSheet(int sessionId, int classId) {
        return generateAttendanceSheet(sessionId, classId, null, null);
    }

    // =====================================
    // GENERATE ATTENDANCE SHEET
    // =====================================
    public boolean generateAttendanceSheet(
            int sessionId,
            int classId,
            String courseFilter,
            String yearFilter) {

        String enrollmentQuery =
            "SELECT e.student_id FROM enrollments e "
          + "JOIN students s ON e.student_id = s.student_id "
          + "WHERE e.class_id = ?";

        // If filters are provided, append them
        if (courseFilter != null && !courseFilter.isEmpty()) {
            enrollmentQuery += " AND s.course = ?";
        }
        if (yearFilter != null && !yearFilter.isEmpty()) {
            enrollmentQuery += " AND s.year_level = ?";
        }

        String insertQuery =
            "INSERT INTO attendance_records "
          + "(session_id, student_id, status, timestamp) "
          + "VALUES (?, ?, ?, ?)";

        try {

            Connection conn = DBConnect.connect();

            // Ensure attendance_records has required migration columns (timestamp)
            ensureAttendanceColumns(conn);

            // GET ENROLLED STUDENTS
            PreparedStatement getStudents =
                    conn.prepareStatement(enrollmentQuery);

            getStudents.setInt(1, classId);

            int idx = 2;
            if (courseFilter != null && !courseFilter.isEmpty()) {
                getStudents.setString(idx++, courseFilter);
            }
            if (yearFilter != null && !yearFilter.isEmpty()) {
                getStudents.setString(idx++, yearFilter);
            }

            ResultSet rs = getStudents.executeQuery();

            // CREATE DEFAULT RECORDS
            while (rs.next()) {

                String studentId =
                        rs.getString("student_id");
                
                System.out.println("Loaded student: " + studentId);

                PreparedStatement insertRecord =
                        conn.prepareStatement(insertQuery);

                insertRecord.setInt(1, sessionId);

                insertRecord.setString(2, studentId);

                // DEFAULT STATUS
                insertRecord.setString(3, "Pending");

                // default timestamp
                insertRecord.setString(4, java.time.LocalDateTime.now().toString());

                insertRecord.executeUpdate();
            }

            System.out.println(
                "Attendance sheet generated!"
            );

            return true;

        } catch (Exception e) {

            System.out.println(
                "Generate sheet error: "
                + e.getMessage()
            );

            return false;
        }
    }

    // =====================================
    // UPDATE ATTENDANCE STATUS
    // =====================================
    public boolean updateAttendanceStatus(
            int sessionId,
            String studentId,
            String status) {

        String sql =
            "UPDATE attendance_records "
          + "SET status=?, timestamp=? "
          + "WHERE session_id=? "
          + "AND student_id=?";

        try {

            Connection conn = DBConnect.connect();

            // Ensure attendance_records has required migration columns
            ensureAttendanceColumns(conn);

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setString(1, status);

            pstmt.setString(2, java.time.LocalDateTime.now().toString());

            pstmt.setInt(3, sessionId);

            pstmt.setString(4, studentId);

            pstmt.executeUpdate();

            return true;

        } catch (Exception e) {

            System.out.println(
                "Update attendance error: "
                + e.getMessage()
            );

            return false;
        }
    }

    // =====================================
    // VIEW ATTENDANCE SHEET
    // =====================================
    public ArrayList<AttendanceRecord>
    getAttendanceSheet(int sessionId) {

        ArrayList<AttendanceRecord> list =
                new ArrayList<>();

        String sql =
            "SELECT ar.id, "
          + "ar.session_id, "
          + "ar.student_id, "
          + "s.full_name, "
          + "ar.status, "
          + "ar.timestamp "
          + "FROM attendance_records ar "
          + "JOIN students s "
          + "ON ar.student_id = s.student_id "
          + "WHERE ar.session_id=?";

        try {

            Connection conn = DBConnect.connect();

            // Ensure attendance_records has required migration columns
            ensureAttendanceColumns(conn);

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setInt(1, sessionId);

            ResultSet rs =
                    pstmt.executeQuery();

            while (rs.next()) {

                AttendanceRecord record =
                        new AttendanceRecord();

                record.setId(
                    rs.getInt("id")
                );

                record.setSessionId(
                    rs.getInt("session_id")
                );

                record.setStudentId(
                    rs.getString("student_id")
                );

                // NEW
                record.setFullName(
                    rs.getString("full_name")
                );

                record.setStatus(
                    rs.getString("status")
                );

                record.setTimestamp(
                    rs.getString("timestamp")
                );

                list.add(record);
            }

        } catch (Exception e) {

            System.out.println(
                "Get sheet error: "
                + e.getMessage()
            );
        }

        return list;
    }
    
    public void viewAttendanceSheet(int sessionId) {

        String sql =
            "SELECT ar.student_id, "
          + "s.full_name, "
          + "ar.status, "
          + "ar.timestamp "
          + "FROM attendance_records ar "
          + "JOIN students s "
          + "ON ar.student_id = s.student_id "
          + "WHERE ar.session_id = ?";

        try {

            Connection conn = DBConnect.connect();

            // Ensure attendance_records has required migration columns
            ensureAttendanceColumns(conn);

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setInt(1, sessionId);

            ResultSet rs =
                    pstmt.executeQuery();

            System.out.println(
                "\n===== ATTENDANCE SHEET ====="
            );

            System.out.println(
                "Session ID: " + sessionId
            );

            System.out.println(
                "----------------------------------------"
            );

            while (rs.next()) {

                String studentId =
                        rs.getString("student_id");

                String fullName =
                        rs.getString("full_name");

                String status =
                        rs.getString("status");

                String timestamp = rs.getString("timestamp");

                System.out.println(
                        studentId + " | "
                      + fullName + " | "
                      + status + " | "
                      + timestamp
                );
            }

        } catch (Exception e) {

            System.out.println(
                "View sheet error: "
                + e.getMessage()
            );
        }
    }

    // =====================================
    // ATTENDANCE REPORT (FILTERED)
    // =====================================
    public ArrayList<AttendanceRecord> getAttendanceReport(
            String professorId,
            Integer classId,
            String courseFilter,
            String yearFilter,
            String sessionDate) {

        ArrayList<AttendanceRecord> list = new ArrayList<>();

        String sql =
            "SELECT ar.id AS id, ar.session_id AS session_id, ar.student_id AS student_id, s.full_name AS full_name, s.course AS course, s.year_level AS year_level, s.email AS email, ar.status AS status, ar.timestamp AS timestamp "
          + "FROM attendance_records ar "
          + "JOIN students s ON ar.student_id = s.student_id "
          + "JOIN attendance_sessions ss ON ar.session_id = ss.id "
          + "JOIN classes c ON ss.class_id = c.id "
          + "WHERE 1=1 ";

        if (professorId != null && !professorId.isEmpty()) sql += " AND c.teacher_id = ?";
        if (classId != null) sql += " AND c.id = ?";
        if (courseFilter != null && !courseFilter.isEmpty()) sql += " AND s.course = ?";
        if (yearFilter != null && !yearFilter.isEmpty()) sql += " AND s.year_level = ?";
        if (sessionDate != null && !sessionDate.isEmpty()) sql += " AND ss.session_date = ?";

        try {
            Connection conn = DBConnect.connect();

            // Ensure attendance_records has required migration columns
            ensureAttendanceColumns(conn);

            PreparedStatement pstmt = conn.prepareStatement(sql);

            int idx = 1;
            if (professorId != null && !professorId.isEmpty()) pstmt.setString(idx++, professorId);
            if (classId != null) pstmt.setInt(idx++, classId);
            if (courseFilter != null && !courseFilter.isEmpty()) pstmt.setString(idx++, courseFilter);
            if (yearFilter != null && !yearFilter.isEmpty()) pstmt.setString(idx++, yearFilter);
            if (sessionDate != null && !sessionDate.isEmpty()) pstmt.setString(idx++, sessionDate);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                AttendanceRecord r = new AttendanceRecord();
                r.setId(rs.getInt("id"));
                r.setSessionId(rs.getInt("session_id")); // FIX: was missing — chart date lookup now works
                r.setStudentId(rs.getString("student_id"));
                r.setFullName(rs.getString("full_name"));
                // populate reporting fields
                r.setCourse(rs.getString("course"));
                r.setYearLevel(rs.getString("year_level"));
                r.setEmail(rs.getString("email"));
                r.setStatus(rs.getString("status"));
                r.setTimestamp(rs.getString("timestamp"));
                list.add(r);
            }

        } catch (Exception e) {
            System.out.println("Get attendance report error: " + e.getMessage());
        }

        return list;
    }

    // NEW
    // =====================================
    // ATTENDANCE REPORT BY SESSION ID
    // =====================================
    public ArrayList<AttendanceRecord> getAttendanceReportBySessionId(
            Integer sessionId,
            String professorId,
            Integer classId,
            String courseFilter,
            String yearFilter) {

        ArrayList<AttendanceRecord> list = new ArrayList<>();

        String sql =
            "SELECT ar.student_id AS student_id, s.full_name AS full_name, s.course AS course, s.year_level AS year_level, s.email AS email, ar.status AS status, ar.timestamp AS timestamp "
          + "FROM attendance_records ar "
          + "JOIN students s ON ar.student_id = s.student_id "
          + "JOIN attendance_sessions ss ON ar.session_id = ss.id "
          + "JOIN classes c ON ss.class_id = c.id "
          + "WHERE 1=1 ";

        if (sessionId != null) sql += " AND ss.id = ?";
        if (professorId != null && !professorId.isEmpty()) sql += " AND c.teacher_id = ?";
        if (classId != null) sql += " AND c.id = ?";
        if (courseFilter != null && !courseFilter.isEmpty()) sql += " AND s.course = ?";
        if (yearFilter != null && !yearFilter.isEmpty()) sql += " AND s.year_level = ?";

        try {
            Connection conn = DBConnect.connect();

            // Ensure attendance_records has required migration columns
            ensureAttendanceColumns(conn);

            PreparedStatement pstmt = conn.prepareStatement(sql);

            int idx = 1;
            if (sessionId != null) pstmt.setInt(idx++, sessionId);
            if (professorId != null && !professorId.isEmpty()) pstmt.setString(idx++, professorId);
            if (classId != null) pstmt.setInt(idx++, classId);
            if (courseFilter != null && !courseFilter.isEmpty()) pstmt.setString(idx++, courseFilter);
            if (yearFilter != null && !yearFilter.isEmpty()) pstmt.setString(idx++, yearFilter);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                AttendanceRecord r = new AttendanceRecord();
                r.setStudentId(rs.getString("student_id"));
                r.setFullName(rs.getString("full_name"));
                // populate reporting fields
                r.setCourse(rs.getString("course"));
                r.setYearLevel(rs.getString("year_level"));
                r.setEmail(rs.getString("email"));
                r.setStatus(rs.getString("status"));
                r.setTimestamp(rs.getString("timestamp"));
                list.add(r);
            }

        } catch (Exception e) {
            System.out.println("Get attendance report by session id error: " + e.getMessage());
        }

        return list;
    }

    // =====================================
    // GET COUNTS FOR A SESSION (present, late, absent)
    // =====================================
    public int[] getCountsForSession(int sessionId) {
        int[] counts = new int[]{0,0,0};
        String sql =
            "SELECT "
          + "SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) AS present, "
          + "SUM(CASE WHEN status = 'Late' THEN 1 ELSE 0 END) AS late, "
          + "SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) AS absent "
          + "FROM attendance_records WHERE session_id = ?";

        try {
            Connection conn = DBConnect.connect();
            ensureAttendanceColumns(conn);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                counts[0] = rs.getInt("present");
                counts[1] = rs.getInt("late");
                counts[2] = rs.getInt("absent");
            }
        } catch (Exception e) {
            System.out.println("Get counts error: " + e.getMessage());
        }

        return counts;
    }

    // =====================================
    // DELETE RECORDS FOR A SESSION
    // =====================================
    public boolean deleteRecordsBySessionId(int sessionId) {
        String sql = "DELETE FROM attendance_records WHERE session_id = ?";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, sessionId);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("Delete records error: " + e.getMessage());
            return false;
        }
    }

    // =====================================
    // GET STUDENTS WITH LOW ATTENDANCE (per class)
    // =====================================
    public java.util.ArrayList<model.AttendanceRecord> getLowAttendanceStudents(int threshold) {
        java.util.ArrayList<model.AttendanceRecord> list = new java.util.ArrayList<>();

        String sql =
            "SELECT ar.student_id AS student_id, s.full_name AS full_name, c.id AS class_id, c.class_name AS class_name, c.class_code AS class_code, c.teacher_id AS professor_id, t.full_name AS professor_name, "
          + "SUM(CASE WHEN ar.status = 'Absent' THEN 1 ELSE 0 END) AS absent_count "
          + "FROM attendance_records ar "
          + "JOIN attendance_sessions ss ON ar.session_id = ss.id "
          + "JOIN classes c ON ss.class_id = c.id "
          + "JOIN students s ON ar.student_id = s.student_id "
          + "LEFT JOIN teachers t ON c.teacher_id = t.teacher_id "
          + "GROUP BY ar.student_id, c.id "
          + "HAVING absent_count > ?";

        try {
            Connection conn = DBConnect.connect();
            ensureAttendanceColumns(conn);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, threshold);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.AttendanceRecord r = new model.AttendanceRecord();
                r.setStudentId(rs.getString("student_id"));
                r.setFullName(rs.getString("full_name"));
                r.setClassId(rs.getInt("class_id"));
                r.setClassName(rs.getString("class_name"));
                r.setClassCode(rs.getString("class_code"));
                r.setProfessorName(rs.getString("professor_name"));
                r.setProfessorId(rs.getString("professor_id"));
                r.setAbsentCount(rs.getInt("absent_count"));

                // derive a human readable alert type
                r.setAlertType(r.getAbsentCount() + " Consecutive Absences");

                list.add(r);
            }
        } catch (Exception e) {
            System.out.println("Get low attendance students error: " + e.getMessage());
        }

        return list;
    }

    // GET STUDENTS WITH LOW ATTENDANCE FOR A SPECIFIC TEACHER
    public java.util.ArrayList<model.AttendanceRecord> getLowAttendanceStudentsForTeacher(String teacherId, int threshold) {
        java.util.ArrayList<model.AttendanceRecord> list = new java.util.ArrayList<>();

        String sql =
            "SELECT ar.student_id AS student_id, s.full_name AS full_name, c.id AS class_id, c.class_name AS class_name, c.class_code AS class_code, c.teacher_id AS professor_id, t.full_name AS professor_name, "
          + "SUM(CASE WHEN ar.status = 'Absent' THEN 1 ELSE 0 END) AS absent_count "
          + "FROM attendance_records ar "
          + "JOIN attendance_sessions ss ON ar.session_id = ss.id "
          + "JOIN classes c ON ss.class_id = c.id "
          + "JOIN students s ON ar.student_id = s.student_id "
          + "LEFT JOIN teachers t ON c.teacher_id = t.teacher_id "
          + "WHERE c.teacher_id = ? "
          + "GROUP BY ar.student_id, c.id "
          + "HAVING absent_count > ?";

        try {
            Connection conn = DBConnect.connect();
            ensureAttendanceColumns(conn);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacherId);
            pstmt.setInt(2, threshold);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.AttendanceRecord r = new model.AttendanceRecord();
                r.setStudentId(rs.getString("student_id"));
                r.setFullName(rs.getString("full_name"));
                r.setClassId(rs.getInt("class_id"));
                r.setClassName(rs.getString("class_name"));
                r.setClassCode(rs.getString("class_code"));
                r.setProfessorName(rs.getString("professor_name"));
                r.setProfessorId(rs.getString("professor_id"));
                r.setAbsentCount(rs.getInt("absent_count"));
                r.setAlertType(r.getAbsentCount() + " Consecutive Absences");
                list.add(r);
            }
        } catch (Exception e) {
            System.out.println("Get low attendance students for teacher error: " + e.getMessage());
        }

        return list;
    }

    // =====================================
    // CLEAR ABSENCE NOTIFICATIONS FOR A STUDENT IN A CLASS
    // This deletes 'Absent' records for the given student and class so they won't appear again
    // =====================================
    public boolean clearAbsenceNotifications(String studentId, Integer classId) {
        if (studentId == null || classId == null) return false;
        String sql =
            "DELETE FROM attendance_records "
          + "WHERE student_id = ? "
          + "AND status = 'Absent' "
          + "AND session_id IN (SELECT id FROM attendance_sessions WHERE class_id = ?)";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.setInt(2, classId);
            int affected = pstmt.executeUpdate();
            System.out.println("Cleared " + affected + " absent record(s) for " + studentId + " in class " + classId);
            return true;
        } catch (Exception e) {
            System.out.println("Clear absence notifications error: " + e.getMessage());
            return false;
        }
    }

    // Ensure attendance_records has migration columns (timestamp)
    private void ensureAttendanceColumns(Connection conn) {
        try {
            ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(attendance_records);");
            boolean hasTimestamp = false;
            while (rs.next()) {
                String name = rs.getString("name");
                if ("timestamp".equalsIgnoreCase(name)) { hasTimestamp = true; break; }
            }
            rs.close();

            if (!hasTimestamp) {
                try {
                    conn.createStatement().executeUpdate("ALTER TABLE attendance_records ADD COLUMN timestamp TEXT");
                    conn.createStatement().executeUpdate("UPDATE attendance_records SET timestamp = datetime('now') WHERE timestamp IS NULL");
                    System.out.println("Migration: added column timestamp to attendance_records (backfilled existing rows)");
                } catch (Exception ex) {
                    System.out.println("Migration add timestamp failed: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("ensureAttendanceColumns error: " + e.getMessage());
        }
    }
}
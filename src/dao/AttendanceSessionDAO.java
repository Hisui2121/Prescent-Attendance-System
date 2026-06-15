package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import database.DBConnect;
import model.AttendanceSession;

public class AttendanceSessionDAO {

    // CREATE SESSION
    public int createSession(AttendanceSession session) {

        String sql =
            "INSERT INTO attendance_sessions "
          + "(class_id, session_date, created_by) "
          + "VALUES (?, ?, ?)";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                conn.prepareStatement(
                    sql,
                    PreparedStatement.RETURN_GENERATED_KEYS
                );

            // INSERT CLASS ID
            pstmt.setInt(1, session.getClassId());

            // INSERT DATE
            pstmt.setString(2, session.getSessionDate());

            // INSERT CREATOR
            pstmt.setString(3, session.getCreatedBy());

            pstmt.executeUpdate();

            // GET AUTO GENERATED SESSION ID
            ResultSet rs = pstmt.getGeneratedKeys();

            if (rs.next()) {

                return rs.getInt(1);
            }

        } catch (Exception e) {

            System.out.println(
                "Session error: "
                + e.getMessage()
            );
        }

        return -1;
    }

    // VIEW ALL SESSIONS
    public ArrayList<AttendanceSession> getAllSessions() {

        ArrayList<AttendanceSession> list =
                new ArrayList<>();

        String sql =
            "SELECT * FROM attendance_sessions";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                AttendanceSession s =
                        new AttendanceSession();

                s.setId(rs.getInt("id"));

                s.setClassId(
                    rs.getInt("class_id")
                );

                s.setSessionDate(
                    rs.getString("session_date")
                );

                s.setCreatedBy(
                    rs.getString("created_by")
                );

                list.add(s);
            }

        } catch (Exception e) {

            System.out.println(
                "Read session error: "
                + e.getMessage()
            );
        }

        return list;
    }

    // =====================================
    // DELETE SESSION (and its attendance records)
    // =====================================
    public boolean deleteSession(int sessionId) {
        String deleteRecords = "DELETE FROM attendance_records WHERE session_id = ?";
        String deleteSession = "DELETE FROM attendance_sessions WHERE id = ?";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement p1 = conn.prepareStatement(deleteRecords);
            p1.setInt(1, sessionId);
            p1.executeUpdate();

            PreparedStatement p2 = conn.prepareStatement(deleteSession);
            p2.setInt(1, sessionId);
            p2.executeUpdate();

            return true;
        } catch (Exception e) {
            System.out.println("Delete session error: " + e.getMessage());
            return false;
        }
    }
}
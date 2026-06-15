package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import database.DBConnect;
import model.Teacher;

public class TeacherDAO {

    public Teacher getTeacherById(String teacherId) {
        if (teacherId == null) return null;
        try {
            Connection conn = DBConnect.connect();
            String sql = "SELECT id, teacher_id, full_name, email FROM teachers WHERE teacher_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Teacher t = new Teacher();
                t.setId(rs.getInt("id"));
                t.setTeacherId(rs.getString("teacher_id"));
                t.setFullName(rs.getString("full_name"));
                t.setEmail(rs.getString("email"));
                return t;
            }
        } catch (Exception e) {
            System.out.println("TeacherDAO.getTeacherById error: " + e.getMessage());
        }
        return null;
    }

    public List<Teacher> getAllTeachers() {
        List<Teacher> list = new ArrayList<>();
        try {
            Connection conn = DBConnect.connect();
            String sql = "SELECT id, teacher_id, full_name, email FROM teachers";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Teacher t = new Teacher();
                t.setId(rs.getInt("id"));
                t.setTeacherId(rs.getString("teacher_id"));
                t.setFullName(rs.getString("full_name"));
                t.setEmail(rs.getString("email"));
                list.add(t);
            }
        } catch (Exception e) {
            System.out.println("TeacherDAO.getAllTeachers error: " + e.getMessage());
        }
        return list;
    }

    // Insert a teacher row if not exists
    public boolean addTeacher(Teacher t) {
        if (t == null || t.getTeacherId() == null) return false;
        try {
            Connection conn = DBConnect.connect();
            // check exists
            PreparedStatement chk = conn.prepareStatement("SELECT COUNT(*) FROM teachers WHERE teacher_id = ?");
            chk.setString(1, t.getTeacherId());
            ResultSet rs = chk.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true; // already present

            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO teachers (teacher_id, full_name, email) VALUES (?, ?, ?)");
            pstmt.setString(1, t.getTeacherId());
            pstmt.setString(2, t.getFullName());
            pstmt.setString(3, t.getEmail());
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("TeacherDAO.addTeacher error: " + e.getMessage());
            return false;
        }
    }
}
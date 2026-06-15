package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import database.DBConnect;
import model.Person;
import model.Teacher;

public class TeacherDAO {

    private Person mapRow(ResultSet rs) throws Exception {
        Teacher t = new Teacher();
        t.setId(rs.getInt("id"));
        t.setTeacherId(rs.getString("teacher_id"));
        t.setFullName(rs.getString("full_name"));
        t.setEmail(rs.getString("email"));
        return t;   // returned as Person — polymorphism in action
    }

    // =========================================================
    // GET TEACHER BY ID
    // =========================================================
    public Teacher getTeacherById(String teacherId) {
        if (teacherId == null) return null;
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id, teacher_id, full_name, email FROM teachers WHERE teacher_id = ?");
            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return (Teacher) mapRow(rs);
        } catch (Exception e) {
            System.out.println("TeacherDAO.getTeacherById error: " + e.getMessage());
        }
        return null;
    }

    // =========================================================
    // GET ALL TEACHERS
    // =========================================================
    public List<Teacher> getAllTeachers() {
        List<Teacher> list = new ArrayList<>();
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id, teacher_id, full_name, email FROM teachers");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add((Teacher) mapRow(rs));
        } catch (Exception e) {
            System.out.println("TeacherDAO.getAllTeachers error: " + e.getMessage());
        }
        return list;
    }

    public List<Person> getAllPersons() {
        List<Person> persons = new ArrayList<>();
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id, teacher_id, full_name, email FROM teachers");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) persons.add(mapRow(rs));   // stored as Person
        } catch (Exception e) {
            System.out.println("TeacherDAO.getAllPersons error: " + e.getMessage());
        }
        return persons;
    }

    // =========================================================
    // ADD TEACHER
    // =========================================================
    public boolean addTeacher(Teacher t) {
        if (t == null || t.getPersonId() == null) return false;   // polymorphic call
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement chk = conn.prepareStatement(
                "SELECT COUNT(*) FROM teachers WHERE teacher_id = ?");
            chk.setString(1, t.getPersonId());   // polymorphic call
            ResultSet rs = chk.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) return true; // already exists

            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO teachers (teacher_id, full_name, email) VALUES (?, ?, ?)");
            pstmt.setString(1, t.getPersonId());   // polymorphic call
            pstmt.setString(2, t.getFullName());
            pstmt.setString(3, t.getEmail());
            pstmt.executeUpdate();
            System.out.println("Teacher added: " + t.getDisplayInfo()); // polymorphic
            return true;
        } catch (Exception e) {
            System.out.println("TeacherDAO.addTeacher error: " + e.getMessage());
            return false;
        }
    }
}
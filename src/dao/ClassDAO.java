package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import database.DBConnect;
import model.Class;

public class ClassDAO {

    // CREATE CLASS
    public boolean addClass(Class c) {

        String sql =
            "INSERT INTO classes "
          + "(class_code, class_name, teacher_id, schedule, room) "
          + "VALUES (?, ?, ?, ?, ?)";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setString(1, c.getClassCode());
            pstmt.setString(2, c.getClassName());
            pstmt.setString(3, c.getTeacherId());
            pstmt.setString(4, c.getSchedule());
            pstmt.setString(5, c.getRoom());

            pstmt.executeUpdate();

            System.out.println("Class created successfully!");

            return true;

        } catch (Exception e) {

            System.out.println("Add class error: " + e.getMessage());
            return false;
        }
    }

    // READ ALL CLASSES
    public ArrayList<Class> getAllClasses() {

        ArrayList<Class> list = new ArrayList<>();

        String sql = "SELECT * FROM classes";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                Class c = new Class();

                c.setId(rs.getInt("id"));
                c.setClassCode(rs.getString("class_code"));
                c.setClassName(rs.getString("class_name"));
                c.setTeacherId(rs.getString("teacher_id"));
                c.setSchedule(rs.getString("schedule"));
                c.setRoom(rs.getString("room"));

                list.add(c);
            }

        } catch (Exception e) {

            System.out.println("Read class error: " + e.getMessage());
        }

        return list;
    }

    // GET CLASSES BY TEACHER ID
    public ArrayList<Class> getClassesByTeacher(String teacherId) {
        ArrayList<Class> list = new ArrayList<>();
        String sql = "SELECT * FROM classes WHERE teacher_id = ?";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, teacherId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Class c = new Class();
                c.setId(rs.getInt("id"));
                c.setClassCode(rs.getString("class_code"));
                c.setClassName(rs.getString("class_name"));
                c.setTeacherId(rs.getString("teacher_id"));
                c.setSchedule(rs.getString("schedule"));
                c.setRoom(rs.getString("room"));
                list.add(c);
            }
        } catch (Exception e) {
            System.out.println("Read classes by teacher error: " + e.getMessage());
        }
        return list;
    }

    // DELETE CLASS
    public boolean deleteClass(int id) {

        String sql = "DELETE FROM classes WHERE id=?";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setInt(1, id);

            pstmt.executeUpdate();

            System.out.println("Class deleted!");

            return true;

        } catch (Exception e) {

            System.out.println("Delete class error: " + e.getMessage());
            return false;
        }
    }
    
    public int getClassIdByCode(String classCode) {

        String sql =
            "SELECT id FROM classes "
          + "WHERE class_code=?";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setString(1, classCode);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                return rs.getInt("id");
            }

        } catch (Exception e) {

            System.out.println(
                "Find class error: "
                + e.getMessage()
            );
        }

        return -1;
    }

    // UPDATE CLASS
    public boolean updateClass(Class c) {

        String sql =
            "UPDATE classes SET "
          + "class_code=?, class_name=?, teacher_id=?, schedule=?, room=? "
          + "WHERE id=?";

        try {

            Connection conn = DBConnect.connect();

            PreparedStatement pstmt =
                    conn.prepareStatement(sql);

            pstmt.setString(1, c.getClassCode());
            pstmt.setString(2, c.getClassName());
            pstmt.setString(3, c.getTeacherId());
            pstmt.setString(4, c.getSchedule());
            pstmt.setString(5, c.getRoom());
            pstmt.setInt(6, c.getId());

            pstmt.executeUpdate();

            System.out.println("Class updated successfully!");

            return true;

        } catch (Exception e) {

            System.out.println("Update class error: " + e.getMessage());
            return false;
        }
    }
}
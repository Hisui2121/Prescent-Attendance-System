package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import database.DBConnect;
import model.Person;
import model.Student;

public class StudentDAO {

    private Person mapRow(ResultSet rs) throws Exception {
        Student s = new Student();
        s.setId(rs.getInt("id"));
        s.setStudentId(rs.getString("student_id"));
        s.setFullName(rs.getString("full_name"));
        s.setCourse(rs.getString("course"));
        s.setYearLevel(rs.getString("year_level"));
        s.setEmail(rs.getString("email"));
        return s;   // returned as Person — polymorphism in action
    }

    // =========================================================
    // ADD STUDENT
    // =========================================================
    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students "
                   + "(student_id, full_name, course, year_level, email) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getPersonId());   // polymorphic call
            pstmt.setString(2, student.getFullName());
            pstmt.setString(3, student.getCourse());
            pstmt.setString(4, student.getYearLevel());
            pstmt.setString(5, student.getEmail());
            pstmt.executeUpdate();
            System.out.println("Student added: " + student.getDisplayInfo()); // polymorphic
            return true;
        } catch (Exception e) {
            System.out.println("Add student error: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // GET ALL STUDENTS
    // =========================================================
    public ArrayList<Student> getAllStudents() {
        ArrayList<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                
                students.add((Student) mapRow(rs));
            }
        } catch (Exception e) {
            System.out.println("Read error: " + e.getMessage());
        }
        return students;
    }

    public List<Person> getAllPersons() {
        List<Person> persons = new ArrayList<>();
        String sql = "SELECT * FROM students";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                persons.add(mapRow(rs));   // stored as Person, is a Student
            }
        } catch (Exception e) {
            System.out.println("getAllPersons error: " + e.getMessage());
        }
        return persons;
    }

    // =========================================================
    // UPDATE STUDENT
    // =========================================================
    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET "
                   + "full_name=?, course=?, year_level=?, email=? "
                   + "WHERE student_id=?";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, student.getFullName());
            pstmt.setString(2, student.getCourse());
            pstmt.setString(3, student.getYearLevel());
            pstmt.setString(4, student.getEmail());
            pstmt.setString(5, student.getPersonId());   // polymorphic call
            pstmt.executeUpdate();
            System.out.println("Student updated: " + student.getDisplayInfo()); // polymorphic
            return true;
        } catch (Exception e) {
            System.out.println("Update error: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // DELETE STUDENT
    // =========================================================
    public boolean deleteStudent(String studentId) {
        String sql = "DELETE FROM students WHERE student_id=?";
        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, studentId);
            pstmt.executeUpdate();
            System.out.println("Student deleted: " + studentId);
            return true;
        } catch (Exception e) {
            System.out.println("Delete error: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // GET STUDENTS FOR A SPECIFIC TEACHER
    // =========================================================
    public ArrayList<Student> getStudentsForTeacher(String teacherId,
                                                     Integer classId,
                                                     String courseFilter,
                                                     String yearFilter) {
        ArrayList<Student> students = new ArrayList<>();
        String sql =
            "SELECT DISTINCT s.id, s.student_id, s.full_name, s.course, s.year_level, s.email "
          + "FROM students s "
          + "JOIN enrollments e ON s.student_id = e.student_id "
          + "JOIN classes c ON e.class_id = c.id "
          + "WHERE c.teacher_id = ?";

        if (classId != null)                                  sql += " AND c.id = ?";
        if (courseFilter != null && !courseFilter.isEmpty())  sql += " AND s.course = ?";
        if (yearFilter   != null && !yearFilter.isEmpty())    sql += " AND s.year_level = ?";

        try {
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            int idx = 1;
            pstmt.setString(idx++, teacherId);
            if (classId != null)                                  pstmt.setInt(idx++, classId);
            if (courseFilter != null && !courseFilter.isEmpty())  pstmt.setString(idx++, courseFilter);
            if (yearFilter   != null && !yearFilter.isEmpty())    pstmt.setString(idx++, yearFilter);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                students.add((Student) mapRow(rs));
            }
        } catch (Exception e) {
            System.out.println("Get students for teacher error: " + e.getMessage());
        }
        return students;
    }
}
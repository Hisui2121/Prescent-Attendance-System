package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import dao.StudentDAO;
import dao.ClassDAO;
import database.DBConnect;
import model.Student;
import report.ReportGenerator;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import report.JasperReportEngine;
import report.ReportEngine;

public class StudentReportUI {

    private StudentDAO studentDAO = new StudentDAO();
    private ClassDAO classDAO = new ClassDAO();

    public Scene getScene() {
        VBox mainPane = new VBox(20);
        mainPane.setPadding(new Insets(25));
        mainPane.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Student Masterlist");
        title.setFont(new Font("Arial", 20));

        ComboBox<String> subjectDropdown = new ComboBox<>();
        subjectDropdown.getItems().add("All Students");
        subjectDropdown.setValue("All Students");
        subjectDropdown.setPrefWidth(300);
        
        ArrayList<model.Class> classes = classDAO.getAllClasses();
        for (model.Class c : classes) {
            subjectDropdown.getItems().add(c.getId() + " | " + c.getClassCode() + " - " + c.getClassName());
        }

        Button btnGenerate = new Button("Generate PDF");
        btnGenerate.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        btnGenerate.setOnAction(e -> handleStudentReport(mainPane, subjectDropdown.getValue()));

        mainPane.getChildren().addAll(title, new Label("Select Target Audience:"), subjectDropdown, btnGenerate);
        return new Scene(mainPane, 400, 250);
    }

    private void handleStudentReport(VBox pane, String selectedSubject) {
        File file = getSaveLocation(pane, "Student_Masterlist.pdf");
        if (file == null) return;

        ArrayList<Student> students = new ArrayList<>();

        if ("All Students".equals(selectedSubject)) {
            students = studentDAO.getAllStudents();
        } else {
            int classId = Integer.parseInt(selectedSubject.split(" \\| ")[0]);
            String sql = "SELECT s.* FROM students s JOIN enrollments e ON s.student_id = e.student_id WHERE e.class_id = ?";
                         
            try (Connection conn = DBConnect.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, classId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Student student = new Student();
                    student.setId(rs.getInt("id"));
                    student.setStudentId(rs.getString("student_id"));
                    student.setFullName(rs.getString("full_name"));
                    student.setCourse(rs.getString("course"));
                    student.setYearLevel(rs.getString("year_level"));
                    student.setEmail(rs.getString("email"));
                    students.add(student);
                }
            } catch (Exception e) {
                System.out.println("Error fetching enrolled students: " + e.getMessage());
            }
        }

        if (students.isEmpty()) {
            System.out.println("No students found for this selection.");
            return;
        }

        try {
        	new ReportGenerator(new JasperReportEngine()).generateStudentPDF(students, file.getAbsolutePath());
            System.out.println("Success! Report saved at: " + file.getAbsolutePath());
        } catch (Exception ex) {
            System.out.println("Error generating Jasper Report: " + ex.getMessage());
        }
    }

    private File getSaveLocation(VBox pane, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fileChooser.showSaveDialog(pane.getScene().getWindow());
    }
}
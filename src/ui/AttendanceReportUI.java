package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import dao.AttendanceRecordDAO;
import database.DBConnect;
import model.AttendanceRecord;
import report.ReportGenerator;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import report.ReportEngine;
import report.JasperReportEngine;

public class AttendanceReportUI {

    private AttendanceRecordDAO recordDAO = new AttendanceRecordDAO();
    private ObservableList<SessionData> sessionObservableList = FXCollections.observableArrayList();

    public Scene getScene() {
        VBox mainPane = new VBox(15);
        mainPane.setPadding(new Insets(20));
        mainPane.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Attendance Session Management");
        title.setFont(new Font("Arial", 20));

        // --- TABLE VIEW ---
        TableView<SessionData> table = new TableView<>();
        table.setItems(sessionObservableList);
        table.setPrefHeight(250);

        TableColumn<SessionData, String> colSubject = new TableColumn<>("Class / Subject");
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        colSubject.setPrefWidth(220);

        // BAGONG COLUMN: Professor Name
        TableColumn<SessionData, String> colProf = new TableColumn<>("Professor Name");
        colProf.setCellValueFactory(new PropertyValueFactory<>("professor"));
        colProf.setPrefWidth(180);

        TableColumn<SessionData, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDate.setPrefWidth(120);

        // Tinanggal natin yung Session ID column sa pagpapakita para malinis, pero nasa likod pa rin siya
        table.getColumns().addAll(colSubject, colProf, colDate);

        refreshSessionTable();

        // --- BUTTONS ---
        HBox sessionButtons = new HBox(10);
        sessionButtons.setAlignment(Pos.CENTER);
        Button btnAttendanceReport = new Button("Generate Selected PDF");
        Button btnDeleteSession = new Button("Delete Selected Session");
        
        btnAttendanceReport.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        btnDeleteSession.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");

        btnAttendanceReport.setOnAction(e -> {
            SessionData selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleAttendanceReport(mainPane, String.valueOf(selected.getId()));
            } else {
                showAlert("Selection Error", "Please select a session from the table first.");
            }
        });

        btnDeleteSession.setOnAction(e -> {
            SessionData selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteSession(selected.getId());
                refreshSessionTable();
            } else {
                showAlert("Selection Error", "Please select a session to delete.");
            }
        });

        sessionButtons.getChildren().addAll(btnAttendanceReport, btnDeleteSession);
        mainPane.getChildren().addAll(title, new Label("Select a session below:"), table, sessionButtons);
        
        return new Scene(mainPane, 650, 450); // Pinalaki ang width para sa bagong column
    }

    private void refreshSessionTable() {
        sessionObservableList.clear();
        // Kinuha natin ang teacher_id para ipasa sa professor column
        String sql = "SELECT s.id, c.class_code, c.class_name, c.teacher_id, s.session_date FROM attendance_sessions s " +
                     "JOIN classes c ON s.class_id = c.id ORDER BY s.id DESC";
                     
        try (Connection conn = DBConnect.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String subject = rs.getString("class_code") + ": " + rs.getString("class_name");
                String prof = rs.getString("teacher_id"); // Ito yung nilagay natin na Professor
                String date = rs.getString("session_date");
                
                sessionObservableList.add(new SessionData(id, subject, prof, date));
            }
        } catch (Exception e) {
            System.out.println("Error loading sessions: " + e.getMessage());
        }
    }

    private void deleteSession(int sessionId) {
        String sqlDeleteRecords = "DELETE FROM attendance_records WHERE session_id = ?";
        String sqlDeleteSession = "DELETE FROM attendance_sessions WHERE id = ?";
        
        try (Connection conn = DBConnect.connect()) {
            try (PreparedStatement pstmt1 = conn.prepareStatement(sqlDeleteRecords)) {
                pstmt1.setInt(1, sessionId);
                pstmt1.executeUpdate();
            }
            try (PreparedStatement pstmt2 = conn.prepareStatement(sqlDeleteSession)) {
                pstmt2.setInt(1, sessionId);
                pstmt2.executeUpdate();
            }
            System.out.println("Session deleted.");
        } catch (Exception e) {
            System.out.println("Error deleting session: " + e.getMessage());
        }
    }

    private void handleAttendanceReport(VBox pane, String sessionIdStr) {
        try {
            int sessionId = Integer.parseInt(sessionIdStr);
            String subjectName = "N/A", sessionDate = "N/A", professor = "N/A", classroom = "N/A", schedule = "N/A";

            String sql = "SELECT c.class_name, s.session_date, c.teacher_id, c.room, c.schedule FROM attendance_sessions s " +
                         "JOIN classes c ON s.class_id = c.id WHERE s.id = ?";

            try (Connection conn = DBConnect.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, sessionId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    subjectName = rs.getString("class_name");
                    sessionDate = rs.getString("session_date");
                    professor = rs.getString("teacher_id");
                    classroom = rs.getString("room");
                    schedule = rs.getString("schedule");
                }
            }

            File file = getSaveLocation(pane, "Attendance_Report_Session_" + sessionId + ".pdf");
            if (file == null) return;

            ArrayList<AttendanceRecord> records = recordDAO.getAttendanceSheet(sessionId);
            if (records.isEmpty()) return;

            new ReportGenerator(new JasperReportEngine()).generateAttendancePDF(records, file.getAbsolutePath(), subjectName, sessionDate, professor, classroom, schedule);
            System.out.println("Report generated!");
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private File getSaveLocation(VBox pane, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF Report");
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fileChooser.showSaveDialog(pane.getScene().getWindow());
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- INNER CLASS DATA MODEL ---
    public static class SessionData {
        private int id;
        private String subjectName;
        private String professor;
        private String date;

        // Idinagdag ang professor sa constructor
        public SessionData(int id, String subjectName, String professor, String date) {
            this.id = id;
            this.subjectName = subjectName;
            this.professor = professor;
            this.date = date;
        }

        public int getId() { return id; }
        public String getSubjectName() { return subjectName; }
        public String getProfessor() { return professor; } // Getter for Professor
        public String getDate() { return date; }
    }
}
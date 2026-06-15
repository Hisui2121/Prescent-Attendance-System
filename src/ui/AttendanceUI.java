package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleBooleanProperty; // added to fix compile error
import javafx.scene.control.cell.CheckBoxTableCell; // added to fix compile error

import model.AttendanceRecord;
import model.Class;
import model.AttendanceSession;
import dao.ClassDAO;
import dao.AttendanceSessionDAO;
import dao.AttendanceRecordDAO;
import dao.StudentDAO;
import dao.EnrollmentDAO;

import java.util.Set;
import java.util.stream.Collectors;

public class AttendanceUI {

    private TableView<AttendanceRecord> table;
    private ClassDAO classDAO = new ClassDAO();
    private AttendanceSessionDAO sessionDAO = new AttendanceSessionDAO();
    private AttendanceRecordDAO recordDAO = new AttendanceRecordDAO();
    private StudentDAO studentDAO = new StudentDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    private ComboBox<Class> classDropdown;
    private DatePicker datePicker;
    private Button btnStart;
    private Button btnPresent;
    private Button btnAbsent;
    private Button btnLate;
    private Button btnEnd;

    // Filters
    private ComboBox<String> courseFilterCombo;
    private ComboBox<String> yearFilterCombo;
    private ComboBox<String> professorCombo;

    private int currentSessionId = -1;
    private boolean sessionActive = false;

    // Cache the root view so the same UI (and its state) is reused when navigating
    private VBox rootView = null;

    public VBox getView() {
        if (rootView != null) return rootView;

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        Label title = new Label("Daily Attendance Tracking");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Filter row
        HBox filterRow = new HBox(10);
        professorCombo = new ComboBox<>();
        professorCombo.setPromptText("Select Professor");
        professorCombo.setPrefWidth(200);

        classDropdown = new ComboBox<>();
        classDropdown.setPromptText("Select Class");
        classDropdown.setPrefWidth(220);

        courseFilterCombo = new ComboBox<>();
        courseFilterCombo.setPromptText("Filter by Course");
        courseFilterCombo.setPrefWidth(160);

        yearFilterCombo = new ComboBox<>();
        yearFilterCombo.setPromptText("Filter by Year");
        yearFilterCombo.setPrefWidth(120);

        datePicker = new DatePicker();

        btnStart = new Button("Load Session");
        btnStart.getStyleClass().add("primary-button");

        filterRow.getChildren().addAll(professorCombo, classDropdown, courseFilterCombo, yearFilterCombo, datePicker, btnStart);

     // Table setup - layout copied from ClassUI for consistent spacing and resize behavior
        table = new TableView<>();
        table.setEditable(true);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Selection column (layout only - mirrors ClassUI structure)
        TableColumn<AttendanceRecord, Boolean> colSelect = new TableColumn<>("");
        colSelect.setPrefWidth(40);
        // use a transient selection map so checkboxes render (layout only)
        final java.util.Map<Integer, SimpleBooleanProperty> selectionMap = new java.util.HashMap<>();
        colSelect.setCellValueFactory(cell -> {
            AttendanceRecord r = cell.getValue();
            if (r == null) return new SimpleBooleanProperty(false);
            int idHash = r.hashCode();
            SimpleBooleanProperty p = selectionMap.get(idHash);
            if (p == null) { p = new SimpleBooleanProperty(false); selectionMap.put(idHash, p); }
            return p;
        });
        colSelect.setEditable(true);
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));

        TableColumn<AttendanceRecord, String> colStudId = new TableColumn<>("STUDENT ID");
        TableColumn<AttendanceRecord, String> colFullName = new TableColumn<>("FULL NAME");
        TableColumn<AttendanceRecord, String> colTime = new TableColumn<>("TIMESTAMP");
        TableColumn<AttendanceRecord, String> colStatus = new TableColumn<>("STATUS");

        // Bind to properties on AttendanceRecord model
        colStudId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Responsive Column Widths (mirror ClassUI proportions)
        colStudId.prefWidthProperty().bind(table.widthProperty().multiply(0.18));
        colFullName.prefWidthProperty().bind(table.widthProperty().multiply(0.40));
        colTime.prefWidthProperty().bind(table.widthProperty().multiply(0.22));
        colStatus.prefWidthProperty().bind(table.widthProperty().multiply(0.18));

        table.getColumns().addAll(colSelect, colStudId, colFullName, colTime, colStatus);

        // Layout / rendering tweaks to avoid compression
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(240);
        table.setPrefHeight(400);
        table.setMaxHeight(Double.MAX_VALUE);
        table.setPlaceholder(new Label("No attendance records"));

        HBox bottomControls = new HBox(10);
        btnPresent = new Button("Mark Present");
        btnPresent.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
        btnPresent.setDisable(true);
        
        btnAbsent = new Button("Mark Absent");
        btnAbsent.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAbsent.setDisable(true);

        btnLate = new Button("Mark Late");
        btnLate.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-font-weight: bold;");
        btnLate.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnEnd = new Button("End Session & Save");
        btnEnd.getStyleClass().add("primary-button");
        btnEnd.setDisable(true);

        bottomControls.getChildren().addAll(btnPresent, btnLate, btnAbsent, spacer, btnEnd);
 
         card.getChildren().addAll(filterRow, table, bottomControls);
         layout.getChildren().addAll(title, card);
 
         // Actions
        // When professor is selected, load classes taught by that professor
        professorCombo.setOnAction(e -> {
            String prof = professorCombo.getValue();
            updateClassesForProfessor(prof);
            // clear course/year options until class chosen
            courseFilterCombo.getItems().clear();
            yearFilterCombo.getItems().clear();
        });

        // When class is selected, populate course/year filters based on enrolled students
        classDropdown.setOnAction(e -> {
            Class c = classDropdown.getValue();
            if (c != null) updateCourseYearForClass(c.getId());
        });

        btnStart.setOnAction(e -> loadSession());

        btnPresent.setOnAction(e -> markSelected("Present"));
        btnAbsent.setOnAction(e -> markSelected("Absent"));
        btnLate.setOnAction(e -> markSelected("Late"));

        btnEnd.setOnAction(e -> endSession());

        loadFilters();
 
         rootView = layout;
         return rootView;
     }

    private void updateClassesForProfessor(String professorId) {
        ObservableList<Class> classes = FXCollections.observableArrayList();
        for (Class c : classDAO.getAllClasses()) {
            if (professorId == null || professorId.isEmpty()) continue;
            if (professorId.equals(c.getTeacherId())) classes.add(c);
        }
        classDropdown.setItems(classes);
    }

    private void updateCourseYearForClass(int classId) {
        // find enrolled students for this class and extract distinct courses and years
        ObservableList<String> courses = FXCollections.observableArrayList();
        ObservableList<String> years = FXCollections.observableArrayList();

        for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
            if (en.getClassId() == classId) {
                String sid = en.getStudentId();
                for (model.Student s : studentDAO.getAllStudents()) {
                    if (sid.equals(s.getStudentId())) {
                        if (s.getCourse() != null && !s.getCourse().isEmpty() && !courses.contains(s.getCourse())) courses.add(s.getCourse());
                        if (s.getYearLevel() != null && !s.getYearLevel().isEmpty() && !years.contains(s.getYearLevel())) years.add(s.getYearLevel());
                    }
                }
            }
        }

        courseFilterCombo.setItems(courses);
        yearFilterCombo.setItems(years);
    }

    private void loadFilters() {
        // Professors (unique teacher IDs from classes)
        ObservableList<Class> classes = FXCollections.observableArrayList(classDAO.getAllClasses());
        Set<String> profs = classes.stream()
                .map(Class::getTeacherId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        ObservableList<String> profList = FXCollections.observableArrayList(profs);
        professorCombo.setItems(profList);

        // Initially, no professor selected => class dropdown empty until professor chosen
        classDropdown.setItems(FXCollections.observableArrayList());

        // Course and year will be populated when a class is selected (based on enrolled students)
        courseFilterCombo.setItems(FXCollections.observableArrayList());
        yearFilterCombo.setItems(FXCollections.observableArrayList());
    }

    private void loadSessionData(int sessionId) {
        ObservableList<AttendanceRecord> items = FXCollections.observableArrayList(recordDAO.getAttendanceSheet(sessionId));
        table.setItems(items);
    }

    private void loadSession() {
        Class selected = classDropdown.getValue();
        if (selected == null) {
            showAlert("Validation", "Please select a class first (choose professor then class).");
            return;
        }
        if (datePicker.getValue() == null) {
            showAlert("Validation", "Please select a date.");
            return;
        }

        String courseFilter = courseFilterCombo.getValue();
        String yearFilter = yearFilterCombo.getValue();
        String profFilter = professorCombo.getValue();

        // If there's an active session, prevent starting a new one until user ends it
        if (sessionActive && currentSessionId != -1) {
            showAlert("Active Session", "There is an active attendance session. Please end the current session before starting a new one.");
            return;
        }

        // Create session
        AttendanceSession s = new AttendanceSession();
        s.setClassId(selected.getId());
        s.setSessionDate(datePicker.getValue().toString());
        s.setCreatedBy(profFilter == null ? "admin" : profFilter);

        int sessionId = sessionDAO.createSession(s);
        if (sessionId == -1) {
            showAlert("Error", "Failed to create attendance session. Check console for details.");
            return;
        }

        // Generate attendance sheet with filters (course/year optional)
        boolean ok = recordDAO.generateAttendanceSheet(sessionId, selected.getId(), courseFilter, yearFilter);
        if (!ok) {
            showAlert("Error", "Failed to generate attendance sheet. Check console for details.");
            return;
        }

        currentSessionId = sessionId;
        sessionActive = true;

        // Enable controls
        btnPresent.setDisable(false);
        btnAbsent.setDisable(false);
        btnLate.setDisable(false);
        btnEnd.setDisable(false);
        btnStart.setDisable(true);
        professorCombo.setDisable(true);
        classDropdown.setDisable(true);
        datePicker.setDisable(true);

        loadSessionData(sessionId);

        showAlert("Session Loaded", "Attendance session loaded. You can mark attendance now.");

        // Note: Removed automatic shutdown hook that ended active sessions. Sessions must be ended by clicking End Session.
    }

    private void markSelected(String status) {
        if (!sessionActive || currentSessionId == -1) {
            showAlert("No Active Session", "Load a session first.");
            return;
        }

        AttendanceRecord sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("No Selection", "Please select a student to mark.");
            return;
        }

        boolean ok = recordDAO.updateAttendanceStatus(currentSessionId, sel.getStudentId(), status);
        if (!ok) {
            showAlert("Error", "Failed to update attendance. Check console for details.");
            return;
        }

        // Update local model and refresh table
        sel.setStatus(status);
        sel.setTimestamp(java.time.LocalDateTime.now().toString());
        table.refresh();
    }

    private void endSession() {
        if (!sessionActive) return;

        // For now, DB records are already updated. We just lock the session in UI.
        sessionActive = false;
        btnPresent.setDisable(true);
        btnAbsent.setDisable(true);
        btnLate.setDisable(true);
        btnEnd.setDisable(true);
        btnStart.setDisable(false);
        professorCombo.setDisable(false);
        classDropdown.setDisable(false);
        datePicker.setDisable(false);

        // Reset currentSessionId to indicate no active session
        currentSessionId = -1;

        showAlert("Session Ended", "Attendance session ended and saved.");
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
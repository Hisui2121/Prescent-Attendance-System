package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;
import javafx.beans.property.SimpleStringProperty;

import dao.AttendanceSessionDAO;
import dao.AttendanceRecordDAO;
import dao.ClassDAO;
import model.AttendanceSession;
import model.AttendanceRecord;
import model.Class;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import report.ReportGenerator;
import net.sf.jasperreports.engine.JRException;
import model.Student;

public class ReportUI {

    private AttendanceSessionDAO sessionDAO = new AttendanceSessionDAO();
    private AttendanceRecordDAO recordDAO = new AttendanceRecordDAO();
    private ClassDAO classDAO = new ClassDAO();

    public VBox getView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        Label title = new Label("Report Generation");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        VBox card = new VBox(20);
        card.getStyleClass().add("card");

        Label subtitle = new Label("Available Templates");
        subtitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ComboBox<String> reportType = new ComboBox<>();
        reportType.getItems().addAll("Attendance", "Masterlist");
        reportType.setValue("Attendance");
        reportType.setPrefWidth(300);

        // Attendance-specific controls -> replaced with filter controls + result table
        // Use FlowPane so controls wrap when window is narrow and don't force the table to shrink
        FlowPane attendanceControls = new FlowPane(10, 10);
        attendanceControls.setPrefWrapLength(800); // allow wrapping

        // Filters: professor, class, course, year, session date
        ComboBox<String> profFilter = new ComboBox<>();
        profFilter.setPromptText("Professor");
        profFilter.setPrefWidth(180);

        ComboBox<Class> classFilter = new ComboBox<>();
        classFilter.setPromptText("Class");
        classFilter.setPrefWidth(240);
        classFilter.setItems(FXCollections.observableArrayList(classDAO.getAllClasses()));

        ComboBox<String> courseFilter = new ComboBox<>();
        courseFilter.setPromptText("Course");
        courseFilter.setPrefWidth(160);

        ComboBox<String> yearFilter = new ComboBox<>();
        yearFilter.setPromptText("Year");
        yearFilter.setPrefWidth(120);

        ComboBox<AttendanceSession> sessionDateFilter = new ComboBox<>();
        sessionDateFilter.setPromptText("Session");
        sessionDateFilter.setPrefWidth(300);

        // Professors from classes
        ObservableList<Class> allClasses = FXCollections.observableArrayList(classDAO.getAllClasses());
        ObservableList<String> profs = FXCollections.observableArrayList();
        for (Class c : allClasses) {
            if (c.getTeacherId() != null && !profs.contains(c.getTeacherId())) profs.add(c.getTeacherId());
        }
        profFilter.setItems(profs);

        // Populate session objects
        ObservableList<AttendanceSession> sessions = FXCollections.observableArrayList(sessionDAO.getAllSessions());
        // Configure how sessions are shown: date + class code
        sessionDateFilter.setConverter(new StringConverter<AttendanceSession>() {
            @Override
            public String toString(AttendanceSession ss) {
                if (ss == null) return "";
                String classCode = "";
                for (Class c : allClasses) {
                    if (c.getId() == ss.getClassId()) { classCode = c.getClassCode(); break; }
                }
                return ss.getSessionDate() + (classCode.isEmpty() ? "" : " (" + classCode + ")");
            }

            @Override
            public AttendanceSession fromString(String string) {
                return null;
            }
        });

        // Add a 'View all sessions' sentinel option at the top, then populate the combo
        AttendanceSession sentinel = new AttendanceSession();
        sentinel.setId(-1);
        sentinel.setSessionDate("View all sessions");
        sessions.add(0, sentinel);
        sessionDateFilter.setItems(sessions);

        // When professor selected, limit classes to those taught by that professor
        profFilter.setOnAction(ev -> {
            String prof = profFilter.getValue();
            ObservableList<Class> cls = FXCollections.observableArrayList();
            for (Class c : allClasses) {
                if (prof != null && prof.equals(c.getTeacherId())) cls.add(c);
            }
            classFilter.setItems(cls);

            // Populate course/year lists based on enrollments for the selected professor's classes
            ObservableList<String> courses = FXCollections.observableArrayList();
            ObservableList<String> years = FXCollections.observableArrayList();
            dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
            dao.StudentDAO sdao = new dao.StudentDAO();
            for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                for (Class c : cls) {
                    if (en.getClassId() == c.getId()) {
                        for (model.Student s : sdao.getAllStudents()) {
                            if (s.getStudentId().equals(en.getStudentId())) {
                                if (s.getCourse() != null && !s.getCourse().isEmpty() && !courses.contains(s.getCourse())) courses.add(s.getCourse());
                                if (s.getYearLevel() != null && !s.getYearLevel().isEmpty() && !years.contains(s.getYearLevel())) years.add(s.getYearLevel());
                                break;
                            }
                        }
                    }
                }
            }
            courseFilter.setItems(courses);
            yearFilter.setItems(years);

            // Update session dates using the unified helper (will only show sessions for this professor + optional class/course/year)
            updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses);
        });

        // When class selection changes, populate course/year lists based on enrolled students and session dates for that class
        classFilter.setOnAction(ev -> {
            Class selected = classFilter.getValue();
            if (selected == null) {
                // even if class cleared, update session dates to reflect current professor/course/year
                updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses);
                return;
            }
            ObservableList<String> courses = FXCollections.observableArrayList();
            ObservableList<String> years = FXCollections.observableArrayList();
            dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
            dao.StudentDAO sdao = new dao.StudentDAO();
            for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                if (en.getClassId() == selected.getId()) {
                    for (model.Student s : sdao.getAllStudents()) {
                        if (s.getStudentId().equals(en.getStudentId())) {
                            if (s.getCourse() != null && !s.getCourse().isEmpty() && !courses.contains(s.getCourse())) courses.add(s.getCourse());
                            if (s.getYearLevel() != null && !s.getYearLevel().isEmpty() && !years.contains(s.getYearLevel())) years.add(s.getYearLevel());
                        }
                    }
                }
            }
            courseFilter.setItems(courses);
            yearFilter.setItems(years);

            // Populate session dates filtered by professor + class + optional course/year
            updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses);
        });

        // When course or year changes, also narrow down session dates to those where enrolled students match
        courseFilter.setOnAction(ev -> updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses));
        yearFilter.setOnAction(ev -> updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses));

        Button btnApply = new Button("Apply Filter");
        btnApply.getStyleClass().add("secondary-button");

        // Export button top-right
        Button btnExport = new Button("Export Report");
        btnExport.getStyleClass().add("primary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Do not force buttons to grow horizontally; they will size to text. Keep uniform height via CSS.
        attendanceControls.getChildren().addAll(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, btnApply, btnExport);
        // Put spacer below so the control row doesn't shrink the table; spacer not needed in FlowPane

        // Report Table for Attendance (combine course and year into single column "COURSE - YEAR")
        TableView<AttendanceRecord> reportTable = new TableView<>();
        TableColumn<AttendanceRecord, String> colId = new TableColumn<>("STUDENT ID");
        TableColumn<AttendanceRecord, String> colName = new TableColumn<>("FULL NAME");
        TableColumn<AttendanceRecord, String> colCourseLevel = new TableColumn<>("COURSE - YEAR");
        TableColumn<AttendanceRecord, String> colEmail = new TableColumn<>("EMAIL");
        TableColumn<AttendanceRecord, String> colStatus = new TableColumn<>("STATUS");
        TableColumn<AttendanceRecord, String> colTime = new TableColumn<>("TIMESTAMP");

        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        // create a cell value factory that combines course and year
        colCourseLevel.setCellValueFactory(cell -> {
            AttendanceRecord r = cell.getValue();
            String course = r.getCourse() == null ? "" : r.getCourse();
            String year = r.getYearLevel() == null ? "" : r.getYearLevel();
            String combined = course.isEmpty() ? year : (year.isEmpty() ? course : course + " - " + year);
            return new javafx.beans.property.SimpleStringProperty(combined);
        });
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        reportTable.getColumns().addAll(colId, colName, colCourseLevel, colEmail, colStatus, colTime);
        // Make the table fill available width and height
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportTable.prefWidthProperty().bind(layout.widthProperty());
        reportTable.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(reportTable, Priority.ALWAYS);
        reportTable.setMinHeight(200);
        reportTable.setPlaceholder(new Label("No report data. Apply filters and click 'Apply Filter'."));
        
        // Summary table for "View All Sessions"
        TableView<SessionSummary> summaryTable = new TableView<>();
        TableColumn<SessionSummary, Boolean> colSelect = new TableColumn<>("Select");
        TableColumn<SessionSummary, String> colDateSummary = new TableColumn<>("Date");
        TableColumn<SessionSummary, String> colCourseYear = new TableColumn<>("Course - Year");
        TableColumn<SessionSummary, String> colClassName = new TableColumn<>("Class");
        TableColumn<SessionSummary, String> colProfessor = new TableColumn<>("Professor");
        TableColumn<SessionSummary, Integer> colPresent = new TableColumn<>("Present");
        TableColumn<SessionSummary, Integer> colLate = new TableColumn<>("Late");
        TableColumn<SessionSummary, Integer> colAbsent = new TableColumn<>("Absent");

        colSelect.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        // bind the checkbox cell to the underlying SessionSummary.selected property
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(index -> {
            // defensive checks
            if (summaryTable.getItems() == null || index == null) return new SimpleBooleanProperty(false);
            int i = index.intValue();
            if (i < 0 || i >= summaryTable.getItems().size()) return new SimpleBooleanProperty(false);
            return summaryTable.getItems().get(i).selectedProperty();
        }));
        colSelect.setEditable(true);

        colDateSummary.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSession().getSessionDate()));
        colCourseYear.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCourseYear()));
        colClassName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClassName()));
        colProfessor.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProfessor()));
        colPresent.setCellValueFactory(cell -> cell.getValue().presentProperty().asObject());
        colLate.setCellValueFactory(cell -> cell.getValue().lateProperty().asObject());
        colAbsent.setCellValueFactory(cell -> cell.getValue().absentProperty().asObject());

        summaryTable.getColumns().addAll(colSelect, colDateSummary, colCourseYear, colClassName, colProfessor, colPresent, colLate, colAbsent);
        summaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        summaryTable.setPlaceholder(new Label("No sessions to display."));
        // allow checkbox editing
        summaryTable.setEditable(true);

        // delete and export selected sessions buttons
        Button btnDeleteSelected = new Button("Delete Selected Sessions");
        btnDeleteSelected.getStyleClass().add("danger-button");

        VBox dynamicArea = new VBox(10);
        // Bind report table width to container so it expands to full width
        reportTable.prefWidthProperty().bind(dynamicArea.widthProperty());
        reportTable.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(reportTable, Priority.ALWAYS);
        reportTable.setMinHeight(200);
        reportTable.setPlaceholder(new Label("No report data. Apply filters and click 'Apply Filter'."));
        VBox right = new VBox(8, summaryTable, btnDeleteSelected);
        VBox.setVgrow(summaryTable, Priority.ALWAYS);
        dynamicArea.getChildren().addAll(attendanceControls, reportTable);

        // When the session combo selection changes: if sentinel (id==-1) is chosen, show summary table; otherwise show detailed report table
        sessionDateFilter.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            if (newV.getId() == -1) {
                // hide the top Export Report when showing the multi-session summary
                btnExport.setVisible(false);
                // populate summaryTable using the current items (excluding sentinel)
                ObservableList<AttendanceSession> items = sessionDateFilter.getItems();
                ObservableList<SessionSummary> sums = FXCollections.observableArrayList();
                for (AttendanceSession ss : items) {
                    if (ss == null || ss.getId() == -1) continue;
                    int[] counts = recordDAO.getCountsForSession(ss.getId());
                    // derive class/course/professor info
                    String courseYear = "";
                    String className = "";
                    String professor = "";
                    Class ssClass = null;
                    for (Class c : allClasses) { if (c.getId() == ss.getClassId()) { ssClass = c; break; } }
                    if (ssClass != null) {
                        className = ssClass.getClassName() == null ? ssClass.getClassCode() : ssClass.getClassName();
                        professor = ssClass.getTeacherId() == null ? "" : ssClass.getTeacherId();
                        // collect course-year combos from enrollments
                        dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
                        dao.StudentDAO sdao = new dao.StudentDAO();
                        java.util.Set<String> combos = new java.util.LinkedHashSet<>();
                        for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                            if (en.getClassId() != ssClass.getId()) continue;
                            for (model.Student s : sdao.getAllStudents()) {
                                if (!s.getStudentId().equals(en.getStudentId())) continue;
                                String course = s.getCourse() == null ? "" : s.getCourse();
                                String yr = s.getYearLevel() == null ? "" : s.getYearLevel();
                                String comb = course.isEmpty() ? yr : (yr.isEmpty() ? course : course + " - " + yr);
                                if (!comb.isEmpty()) combos.add(comb);
                                break;
                            }
                        }
                        courseYear = String.join(", ", combos);
                    }
                    sums.add(new SessionSummary(ss, counts[0], counts[1], counts[2], courseYear, className, professor));
                }
                summaryTable.setItems(sums);
                dynamicArea.getChildren().clear();
                VBox rightBox = new VBox(8, summaryTable, new HBox(8, btnDeleteSelected));
                VBox.setVgrow(summaryTable, Priority.ALWAYS);
                dynamicArea.getChildren().addAll(attendanceControls, rightBox);
            } else {
                // ensure top Export Report is visible for single-session detailed view
                btnExport.setVisible(true);
                // normal session selected: ensure detailed report table is visible
                dynamicArea.getChildren().clear();
                dynamicArea.getChildren().addAll(attendanceControls, reportTable);
            }
        });

        // clicking a summary row should open that session's detailed report
        summaryTable.setRowFactory(tv -> {
            TableRow<SessionSummary> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (row.isEmpty()) return;
                if (ev.getClickCount() >= 1) {
                    SessionSummary ss = row.getItem();
                    if (ss == null) return;
                    // select the underlying AttendanceSession in the combo; this triggers the detailed view
                    try { sessionDateFilter.setValue(ss.getSession()); } catch (Exception ex) { /* ignore */ }
                }
            });
            return row;
        });

        // delete selected sessions handler
        btnDeleteSelected.setOnAction(ev -> {
            ObservableList<SessionSummary> rows = summaryTable.getItems();
            if (rows == null || rows.isEmpty()) { showAlert("No Selection", "No sessions to delete."); return; }
            List<SessionSummary> toDelete = new ArrayList<>();
            for (SessionSummary ss : rows) if (ss.isSelected()) toDelete.add(ss);
            if (toDelete.isEmpty()) { showAlert("No Selection", "Select sessions to delete."); return; }

            Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
            conf.setTitle("Confirm Delete");
            conf.setHeaderText(null);
            conf.setContentText("Delete " + toDelete.size() + " selected session(s)? This action cannot be undone.");
            java.util.Optional<ButtonType> res = conf.showAndWait();
            if (!res.isPresent() || res.get() != ButtonType.OK) return;

            int success = 0;
            for (SessionSummary s : toDelete) {
                try { if (sessionDAO.deleteSession(s.getSession().getId())) success++; } catch (Exception ex) { /* ignore single failure */ }
            }

            showAlert("Deleted", "Deleted " + success + " session(s).");
            // refresh session list
            ObservableList<AttendanceSession> refreshed = FXCollections.observableArrayList(sessionDAO.getAllSessions());
            AttendanceSession sent = new AttendanceSession(); sent.setId(-1); sent.setSessionDate("View all sessions");
            refreshed.add(0, sent);
            sessionDateFilter.setItems(refreshed);
            sessionDateFilter.getSelectionModel().selectFirst();
        });

        // Masterlist-specific controls (course, year, class) and table
        // Use FlowPane for master controls so Apply button is visible on small widths
        FlowPane masterControls = new FlowPane(10, 10);
        masterControls.setPrefWrapLength(800);
        ComboBox<Class> masterClassCombo = new ComboBox<>();
        masterClassCombo.setPrefWidth(300);
        masterClassCombo.setItems(FXCollections.observableArrayList(classDAO.getAllClasses()));
        masterClassCombo.setPromptText("Select class (code - name)");

        ComboBox<String> masterCourseCombo = new ComboBox<>();
        masterCourseCombo.setPromptText("Course");
        masterCourseCombo.setPrefWidth(160);

        ComboBox<String> masterYearCombo = new ComboBox<>();
        masterYearCombo.setPromptText("Year");
        masterYearCombo.setPrefWidth(120);

        Button btnExportMasterPDF = new Button("Export Masterlist (PDF)");
        btnExportMasterPDF.getStyleClass().add("primary-button");

        // Create an Apply button for masterlist (separate node) and reuse the same handler by firing the common btnApply
        Button btnApplyMaster = new Button("Apply Filter");
        btnApplyMaster.getStyleClass().add("secondary-button");
        btnApplyMaster.setOnAction(ev -> btnApply.fire());

        // ensure buttons have uniform height and visible
        btnApplyMaster.setMinHeight(36);
        btnExportMasterPDF.setMinHeight(36);
        masterControls.getChildren().addAll(masterClassCombo, masterCourseCombo, masterYearCombo, btnApplyMaster, btnExportMasterPDF);

        TableView<Student> masterTable = new TableView<>();
        TableColumn<Student, String> mColId = new TableColumn<>("STUDENT ID");
        TableColumn<Student, String> mColName = new TableColumn<>("FULL NAME");
        TableColumn<Student, String> mColEmail = new TableColumn<>("EMAIL");

        mColId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        mColName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        mColEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        masterTable.getColumns().addAll(mColId, mColName, mColEmail);
        // Make master table fill available width and height
        masterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        masterTable.prefWidthProperty().bind(dynamicArea.widthProperty());
        masterTable.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(masterTable, Priority.ALWAYS);
        masterTable.setMinHeight(200);
        masterTable.setPlaceholder(new Label("No students. Choose filters and click Apply Filter."));

        reportType.setOnAction(e -> {
            dynamicArea.getChildren().clear();
            if (reportType.getValue().equals("Attendance")) {
                dynamicArea.getChildren().addAll(attendanceControls, reportTable);
            } else {
                // masterlist view
                dynamicArea.getChildren().addAll(masterControls, masterTable);
            }
        });

        // When master class selected, populate course/year filters
        masterClassCombo.setOnAction(ev -> {
            Class selected = masterClassCombo.getValue();
            if (selected == null) return;
            ObservableList<String> courses = FXCollections.observableArrayList();
            ObservableList<String> years = FXCollections.observableArrayList();
            dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
            dao.StudentDAO sdao = new dao.StudentDAO();
            for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                if (en.getClassId() == selected.getId()) {
                    for (model.Student s : sdao.getAllStudents()) {
                        if (s.getStudentId().equals(en.getStudentId())) {
                            if (s.getCourse() != null && !s.getCourse().isEmpty() && !courses.contains(s.getCourse())) courses.add(s.getCourse());
                            if (s.getYearLevel() != null && !s.getYearLevel().isEmpty() && !years.contains(s.getYearLevel())) years.add(s.getYearLevel());
                        }
                    }
                }
            }
            masterCourseCombo.setItems(courses);
            masterYearCombo.setItems(years);
        });

        // Repurpose the top button: make filters auto-apply for Attendance and turn this button into Clear Filter
        btnApply.setText("Clear Filter");
        // Clear filters handler
        btnApply.setOnAction(ev -> {
            profFilter.getSelectionModel().clearSelection();
            classFilter.getSelectionModel().clearSelection();
            courseFilter.getSelectionModel().clearSelection();
            yearFilter.getSelectionModel().clearSelection();
            sessionDateFilter.getSelectionModel().clearSelection();
            reportTable.getItems().clear();
            summaryTable.getItems().clear();
            dynamicArea.getChildren().clear();
            // restoring default: show top export
            btnExport.setVisible(true);
            dynamicArea.getChildren().addAll(attendanceControls, reportTable);
            // refresh session list
            ObservableList<AttendanceSession> refreshed = FXCollections.observableArrayList(sessionDAO.getAllSessions());
            AttendanceSession sent = new AttendanceSession(); sent.setId(-1); sent.setSessionDate("View all sessions");
            refreshed.add(0, sent);
            sessionDateFilter.setItems(refreshed);
        });

        // attendance auto-apply helper: run when filters or session selection change
        Runnable attendanceAutoApply = () -> {
            if (!"Attendance".equals(reportType.getValue())) return;
            AttendanceSession selectedSession = sessionDateFilter.getValue();
            if (selectedSession == null) return;
            if (selectedSession.getId() == -1) {
                // show summary
                ObservableList<AttendanceSession> items = sessionDateFilter.getItems();
                ObservableList<SessionSummary> sums = FXCollections.observableArrayList();
                for (AttendanceSession ss : items) {
                    if (ss == null || ss.getId() == -1) continue;
                    int[] counts = recordDAO.getCountsForSession(ss.getId());
                    // derive class/course/professor info
                    String courseYear = "";
                    String className = "";
                    String professor = "";
                    Class ssClass = null;
                    for (Class c : allClasses) { if (c.getId() == ss.getClassId()) { ssClass = c; break; } }
                    if (ssClass != null) {
                        className = ssClass.getClassName() == null ? ssClass.getClassCode() : ssClass.getClassName();
                        professor = ssClass.getTeacherId() == null ? "" : ssClass.getTeacherId();
                        // collect course-year combos from enrollments
                        dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
                        dao.StudentDAO sdao = new dao.StudentDAO();
                        java.util.Set<String> combos = new java.util.LinkedHashSet<>();
                        for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                            if (en.getClassId() != ssClass.getId()) continue;
                            for (model.Student s : sdao.getAllStudents()) {
                                if (!s.getStudentId().equals(en.getStudentId())) continue;
                                String course = s.getCourse() == null ? "" : s.getCourse();
                                String yr = s.getYearLevel() == null ? "" : s.getYearLevel();
                                String comb = course.isEmpty() ? yr : (yr.isEmpty() ? course : course + " - " + yr);
                                if (!comb.isEmpty()) combos.add(comb);
                                break;
                            }
                        }
                        courseYear = String.join(", ", combos);
                    }
                    sums.add(new SessionSummary(ss, counts[0], counts[1], counts[2], courseYear, className, professor));
                }
                summaryTable.setItems(sums);
                dynamicArea.getChildren().clear();
                VBox rightBox = new VBox(8, summaryTable, new HBox(8, btnDeleteSelected));
                VBox.setVgrow(summaryTable, Priority.ALWAYS);
                dynamicArea.getChildren().addAll(attendanceControls, rightBox);
                return;
            }

            // detailed session view: query and populate
            String prof = profFilter.getValue();
            Class cls = classFilter.getValue();
            Integer clsId = cls == null ? null : cls.getId();
            String course = courseFilter.getValue();
            String year = yearFilter.getValue();
            java.util.List<AttendanceRecord> rows = recordDAO.getAttendanceReportBySessionId(selectedSession.getId(), prof, clsId, course, year);
            reportTable.setItems(FXCollections.observableArrayList(rows));
            if (rows == null || rows.isEmpty()) {
                int targetClassId = clsId == null ? selectedSession.getClassId() : clsId;
                boolean genOk = false;
                try { genOk = recordDAO.generateAttendanceSheet(selectedSession.getId(), targetClassId, course, year); } catch (Exception ex) { genOk = false; }
                if (genOk) {
                    java.util.List<AttendanceRecord> rows2 = recordDAO.getAttendanceReportBySessionId(selectedSession.getId(), prof, clsId, course, year);
                    reportTable.setItems(FXCollections.observableArrayList(rows2));
                    if (rows2 == null || rows2.isEmpty()) showAlert("No Data", "No attendance records matching the filters were found.");
                } else {
                    showAlert("No Data", "No attendance records matching the filters were found.");
                }
            }
        };

        // Attach auto-apply listeners (filters and session selection)
        profFilter.valueProperty().addListener((obs,o,n) -> { updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses); attendanceAutoApply.run(); });
        classFilter.valueProperty().addListener((obs,o,n) -> { updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses); attendanceAutoApply.run(); });
        courseFilter.valueProperty().addListener((obs,o,n) -> { updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses); attendanceAutoApply.run(); });
        yearFilter.valueProperty().addListener((obs,o,n) -> { updateSessionDatesFromFilters(profFilter, classFilter, courseFilter, yearFilter, sessionDateFilter, sessions, allClasses); attendanceAutoApply.run(); });
        sessionDateFilter.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            if (newV.getId() == -1) {
                // hide the top Export Report when showing the multi-session summary
                btnExport.setVisible(false);
                // populate summaryTable using the current items (excluding sentinel)
                ObservableList<AttendanceSession> items = sessionDateFilter.getItems();
                ObservableList<SessionSummary> sums = FXCollections.observableArrayList();
                for (AttendanceSession ss : items) {
                    if (ss == null || ss.getId() == -1) continue;
                    int[] counts = recordDAO.getCountsForSession(ss.getId());
                    
                    // derive class/course/professor info
                    String courseYear = "";
                    String className = "";
                    String professor = "";
                    Class ssClass = null;
                    for (Class c : allClasses) { if (c.getId() == ss.getClassId()) { ssClass = c; break; } }
                    if (ssClass != null) {
                        className = ssClass.getClassName() == null ? ssClass.getClassCode() : ssClass.getClassName();
                        professor = ssClass.getTeacherId() == null ? "" : ssClass.getTeacherId();
                        // collect course-year combos from enrollments
                        dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
                        dao.StudentDAO sdao = new dao.StudentDAO();
                        java.util.Set<String> combos = new java.util.LinkedHashSet<>();
                        for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                            if (en.getClassId() != ssClass.getId()) continue;
                            for (model.Student s : sdao.getAllStudents()) {
                                if (!s.getStudentId().equals(en.getStudentId())) continue;
                                String course = s.getCourse() == null ? "" : s.getCourse();
                                String yr = s.getYearLevel() == null ? "" : s.getYearLevel();
                                String comb = course.isEmpty() ? yr : (yr.isEmpty() ? course : course + " - " + yr);
                                if (!comb.isEmpty()) combos.add(comb);
                                break;
                            }
                        }
                        courseYear = String.join(", ", combos);
                    }

                    sums.add(new SessionSummary(ss, counts[0], counts[1], counts[2], courseYear, className, professor));
                }
                summaryTable.setItems(sums);
                dynamicArea.getChildren().clear();
                VBox rightBox = new VBox(8, summaryTable, new HBox(8, btnDeleteSelected));
                VBox.setVgrow(summaryTable, Priority.ALWAYS);
                dynamicArea.getChildren().addAll(attendanceControls, rightBox);
            } else {
                btnExport.setVisible(true);
                dynamicArea.getChildren().clear();
                dynamicArea.getChildren().addAll(attendanceControls, reportTable);
                // run auto-apply to load the selected session
                attendanceAutoApply.run();
            }
        });

        // btnApplyMaster should apply the Masterlist filters directly (do not fire the Clear Filter)
        btnApplyMaster.setOnAction(ev -> {
            Class c = masterClassCombo.getValue();
            if (c == null) { showAlert("Validation", "Select a class to view masterlist."); return; }
            String course = masterCourseCombo.getValue();
            String year = masterYearCombo.getValue();
            dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
            dao.StudentDAO sdao = new dao.StudentDAO();
            ArrayList<Student> students = new ArrayList<>();
            for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                if (en.getClassId() != c.getId()) continue;
                for (model.Student s : sdao.getAllStudents()) {
                    if (s.getStudentId().equals(en.getStudentId())) {
                        boolean ok = true;
                        if (course != null && !course.isEmpty()) ok = ok && course.equals(s.getCourse());
                        if (year != null && !year.isEmpty()) ok = ok && year.equals(s.getYearLevel());
                        if (ok) students.add(s);
                        break;
                    }
                }
            }
            masterTable.setItems(FXCollections.observableArrayList(students));
            if (students.isEmpty()) showAlert("No Data", "No students found for the selected filters.");
        });

        // Attendance export -> prompt Save dialog then generate PDF using JasperReports
        btnExport.setOnAction(ev -> {
            if (!"Attendance".equals(reportType.getValue())) {
                showAlert("Mode Error", "Switch to Attendance report to export attendance PDF.");
                return;
            }

            String prof = profFilter.getValue();
            Class cls = classFilter.getValue();
            Integer clsId = cls == null ? null : cls.getId();
            String course = courseFilter.getValue();
            String year = yearFilter.getValue();
            AttendanceSession selectedSession = sessionDateFilter.getValue();
            if (selectedSession == null) {
                showAlert("Validation", "Please select a session before exporting.");
                return;
            }

            java.util.List<AttendanceRecord> rows = recordDAO.getAttendanceReportBySessionId(selectedSession.getId(), prof, clsId, course, year);
            if (rows == null || rows.isEmpty()) {
                // attempt generation before failing
                int targetClassId = clsId == null ? selectedSession.getClassId() : clsId;
                boolean genOk = false;
                try {
                    genOk = recordDAO.generateAttendanceSheet(selectedSession.getId(), targetClassId, course, year);
                } catch (Exception ex) {
                    genOk = false;
                }
                if (genOk) {
                    rows = recordDAO.getAttendanceReportBySessionId(selectedSession.getId(), prof, clsId, course, year);
                }
                if (rows == null || rows.isEmpty()) {
                    showAlert("No Data", "No attendance records to export.");
                    return;
                }
            }

            // Ask user where to save
            Window window = layout.getScene() != null ? layout.getScene().getWindow() : null;
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Attendance Report PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            String defaultName = "attendance_report_" + System.currentTimeMillis() + ".pdf";
            fc.setInitialFileName(defaultName);
            File dest = fc.showSaveDialog(window instanceof Stage ? (Stage) window : null);
            if (dest == null) return; // user cancelled

            try {
                String out = dest.getAbsolutePath();
                String subjectName = (cls == null) ? "" : cls.getClassName();
                String professor = prof == null ? "" : prof;
                String classroom = cls == null ? "" : cls.getRoom();
                String schedule = cls == null ? "" : cls.getSchedule();
                String sDate = selectedSession.getSessionDate();

                ReportGenerator.generateAttendancePDF(rows, out, subjectName, sDate, professor, classroom, schedule);
                showAlert("Exported", "Attendance PDF saved to: " + out);
                openFileInBrowser(new File(out));
            } catch (JRException jre) {
                showAlert("Export Error", "Failed to generate PDF: " + jre.getMessage());
            } catch (Exception ex) {
                showAlert("Export Error", "Failed to open PDF: " + ex.getMessage());
            }
        });

        // Masterlist PDF export -> prompt Save dialog and call ReportGenerator
        btnExportMasterPDF.setOnAction(e -> {
            Class c = masterClassCombo.getValue();
            if (c == null) {
                showAlert("Validation", "Select a class to export masterlist PDF.");
                return;
            }

            // Use current items in masterTable as source
            ObservableList<Student> items = masterTable.getItems();
            if (items == null || items.isEmpty()) {
                showAlert("No Data", "No students to export. Apply filters first.");
                return;
            }

            Window window = layout.getScene() != null ? layout.getScene().getWindow() : null;
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Masterlist PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            String defaultName = "masterlist_" + c.getClassCode() + ".pdf";
            fc.setInitialFileName(defaultName);
            File dest = fc.showSaveDialog(window instanceof Stage ? (Stage) window : null);
            if (dest == null) return;

            try {
                String out = dest.getAbsolutePath();
                ArrayList<Student> students = new ArrayList<>(items);
                String classTitle = c.getClassCode() + " - " + c.getClassName();
                ReportGenerator.generateStudentPDF(students, out, classTitle);
                showAlert("Exported", "Masterlist PDF saved to: " + out);
                openFileInBrowser(new File(out));
            } catch (JRException jre) {
                showAlert("Export Error", "Failed to generate masterlist PDF: " + jre.getMessage());
            } catch (Exception ex) {
                showAlert("Export Error", "Failed to open PDF: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(subtitle, new Label("Select Report Type:"), reportType, dynamicArea);
        layout.getChildren().addAll(title, card);

        return layout;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // Helper to open a file with the system default application (PDF viewer)
    private void openFileInBrowser(File f) {
        try {
            if (f == null) return;
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
            }
        } catch (Exception e) {
            System.out.println("Open file error: " + e.getMessage());
            try {
                showAlert("Open Error", "Could not open file: " + e.getMessage());
            } catch (Exception ex) {
                // ignore UI errors
            }
        }
    }

    // Narrow session dates based on selected professor + class and optional course/year filters
    private void updateSessionDatesFromFilters(
            ComboBox<String> profFilter,
            ComboBox<Class> classFilter,
            ComboBox<String> courseFilter,
            ComboBox<String> yearFilter,
            ComboBox<AttendanceSession> sessionDateFilter,
            ObservableList<AttendanceSession> sessions,
            ObservableList<Class> allClasses) {

        String prof = profFilter.getValue();
        Class selected = classFilter.getValue();
        ObservableList<AttendanceSession> filtered = FXCollections.observableArrayList();

        String course = courseFilter.getValue();
        String year = yearFilter.getValue();

        dao.EnrollmentDAO enrollDAO = new dao.EnrollmentDAO();
        dao.StudentDAO sdao = new dao.StudentDAO();

        // Iterate sessions and include only those that:
        // - belong to classes taught by the selected professor (if professor chosen)
        // - belong to the selected class (if chosen)
        // - have at least one enrolled student matching optional course/year filters
        for (AttendanceSession ss : sessions) {
            if (ss.getSessionDate() == null) continue;

            // check class/prof constraints
            boolean classMatches = false;
            Class ssClass = null;
            for (Class c : allClasses) { if (c.getId() == ss.getClassId()) { ssClass = c; break; } }
            if (ssClass == null) continue;

            if (selected != null) {
                if (ss.getClassId() != selected.getId()) continue; // session not for selected class
                // if professor selected, ensure class teacher matches professor
                if (prof != null && !prof.isEmpty()) {
                    if (selected.getTeacherId() == null || !prof.equals(selected.getTeacherId())) continue;
                }
                classMatches = true;
            } else {
                // no class selected: if professor selected, ensure this session belongs to one of professor's classes
                if (prof != null && !prof.isEmpty()) {
                    if (!prof.equals(ssClass.getTeacherId())) continue; // session not taught by selected professor
                }
                classMatches = true;
            }

            if (!classMatches) continue;

            // If no course/year filters, include session (it passed prof/class checks)
            if ((course == null || course.isEmpty()) && (year == null || year.isEmpty())) {
                if (!filtered.contains(ss)) filtered.add(ss);
                continue;
            }

            // Otherwise, ensure there exists at least one enrolled student in that session's class matching course/year
            boolean hasMatching = false;
            for (model.Enrollment en : enrollDAO.getAllEnrollments()) {
                if (en.getClassId() != ss.getClassId()) continue;
                for (model.Student s : sdao.getAllStudents()) {
                    if (!s.getStudentId().equals(en.getStudentId())) continue;
                    boolean ok = true;
                    if (course != null && !course.isEmpty()) ok = ok && course.equals(s.getCourse());
                    if (year != null && !year.isEmpty()) ok = ok && year.equals(s.getYearLevel());
                    if (ok) { hasMatching = true; break; }
                }
                if (hasMatching) break;
            }

            if (hasMatching) {
                // Double-check that attendance_records actually exist for this session with the given filters.
                java.util.List<AttendanceRecord> check = recordDAO.getAttendanceReportBySessionId(ss.getId(), prof, (selected==null?null:selected.getId()), course, year);
                if (check != null && !check.isEmpty() && !filtered.contains(ss)) filtered.add(ss);
            }
         }

         // Prepend sentinel entry so "View all sessions" remains available in the combo
         AttendanceSession sentinel = new AttendanceSession();
         sentinel.setId(-1);
         sentinel.setSessionDate("View all sessions");
         ObservableList<AttendanceSession> withSentinel = FXCollections.observableArrayList();
         withSentinel.add(sentinel);
         for (AttendanceSession s : filtered) withSentinel.add(s);
         sessionDateFilter.setItems(withSentinel);
     }

     // Inner class to represent a summary of attendance sessions for the "View All Sessions" feature
     public static class SessionSummary {
         private final AttendanceSession session;
         private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
         private final SimpleIntegerProperty present = new SimpleIntegerProperty(0);
         private final SimpleIntegerProperty late = new SimpleIntegerProperty(0);
         private final SimpleIntegerProperty absent = new SimpleIntegerProperty(0);
         private final SimpleStringProperty courseYear = new SimpleStringProperty("");
         private final SimpleStringProperty className = new SimpleStringProperty("");
         private final SimpleStringProperty professor = new SimpleStringProperty("");

         public SessionSummary(AttendanceSession session, int present, int late, int absent) {
             this.session = session;
             this.present.set(present);
             this.late.set(late);
             this.absent.set(absent);
         }

         public SessionSummary(AttendanceSession session, int present, int late, int absent, String courseYear, String className, String professor) {
             this.session = session;
             this.present.set(present);
             this.late.set(late);
             this.absent.set(absent);
             this.courseYear.set(courseYear);
             this.className.set(className);
             this.professor.set(professor);
         }

         public AttendanceSession getSession() { return session; }
         public boolean isSelected() { return selected.get(); }
         public void setSelected(boolean value) { selected.set(value); }
         public SimpleBooleanProperty selectedProperty() { return selected; }
         public int getPresent() { return present.get(); }
         public SimpleIntegerProperty presentProperty() { return present; }
         public int getLate() { return late.get(); }
         public SimpleIntegerProperty lateProperty() { return late; }
         public int getAbsent() { return absent.get(); }
         public SimpleIntegerProperty absentProperty() { return absent; }
         public String getCourseYear() { return courseYear.get(); }
         public SimpleStringProperty courseYearProperty() { return courseYear; }
         public String getClassName() { return className.get(); }
         public SimpleStringProperty classNameProperty() { return className; }
         public String getProfessor() { return professor.get(); }
         public SimpleStringProperty professorProperty() { return professor; }
     }
}

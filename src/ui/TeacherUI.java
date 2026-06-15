package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import dao.ClassDAO;
import dao.StudentDAO;
import dao.EnrollmentDAO;
import dao.AttendanceSessionDAO;
import dao.AttendanceRecordDAO;
import dao.TeacherDAO;
import model.Teacher;
import model.User;

import model.Class;
import model.Student;
import model.AttendanceSession;
import model.AttendanceRecord;

import service.AuthenticationService;
import app.Main;

import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import util.EventBus;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class TeacherUI {

    private BorderPane mainLayout;
    private StackPane contentArea;
    private VBox sidebar;
    private Button currentActiveBtn;

    private ClassDAO classDAO = new ClassDAO();
    private StudentDAO studentDAO = new StudentDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private AttendanceSessionDAO sessionDAO = new AttendanceSessionDAO();
    private AttendanceRecordDAO recordDAO = new AttendanceRecordDAO();
    private TeacherDAO teacherDAO = new TeacherDAO();

    private String teacherId;
    private String teacherName; // new: display friendly name in UI

    // Notification badge reference
    private Label notificationBadge;

    // Session state for teacher attendance actions
    private int currentSessionId = -1;
    private boolean sessionActive = false;

    public TeacherUI() {
        AuthenticationService auth = AuthenticationService.getInstance();
        this.teacherId = auth.getCurrentUsername();
        // Prefer full name if available (avoids showing raw username like 'admin')
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getFullName() != null && !auth.getCurrentUser().getFullName().isEmpty()) {
            this.teacherName = auth.getCurrentUser().getFullName();
        } else {
            this.teacherName = this.teacherId;
        }
    }

    public Scene getScene() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");

        // Top bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 12 20;");
        Label title = new Label("Teacher Portal");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        topBar.getChildren().add(title);
        
        // Notification bell with badge
        StackPane bellPane = new StackPane();
        Button bellBtn = new Button();
        bellBtn.getStyleClass().add("icon-button");
        bellBtn.setFocusTraversable(false);
        try {
            java.io.InputStream bis = getClass().getResourceAsStream("/assets/bell.png");
            if (bis != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(bis));
                iv.setFitWidth(22);
                iv.setFitHeight(22);
                bellBtn.setGraphic(iv);
            } else {
                bellBtn.setText("\uD83D\uDD14");
            }
        } catch (Exception ex) { bellBtn.setText("\uD83D\uDD14"); }

        Label badge = new Label();
        badge.getStyleClass().add("badge");
        badge.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 2 6; -fx-background-radius: 10; -fx-min-width: 20; -fx-alignment: center;");
        badge.setVisible(false);
        this.notificationBadge = badge;

        bellBtn.setOnAction(e -> {
            setActiveBtn(null);
            switchContent(new NotificationUI(teacherId).getView());
        });

        bellPane.getChildren().addAll(bellBtn, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(0,0,12,12));

        // Logout button
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-button");
        logoutBtn.setOnAction(ev -> {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to log out?", ButtonType.YES, ButtonType.NO);
            conf.setTitle("Confirm Logout");
            conf.setHeaderText(null);
            conf.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    AuthenticationService.getInstance().logout();
                    EventBus.fireNotificationChanged();
                    Main.getInstance().showLoginScene();
                }
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(title, spacer, bellPane, logoutBtn);
        mainLayout.setTop(topBar);

        // Register notification change listener so badge updates
        EventBus.addNotificationChangeListener(() -> Platform.runLater(() -> updateBadgeCount()));
        // initial badge count
        updateBadgeCount();

        // Sidebar
        sidebar = new VBox(6);
        sidebar.setPadding(new Insets(18));
        sidebar.setPrefWidth(220);
        Label brand = new Label("ACE Teacher");
        brand.setFont(Font.font("System", FontWeight.BOLD, 20));
        brand.setStyle("-fx-padding: 0 0 20 0;");

        Button btnDashboard = createSidebarBtn("Dashboard");
        Button btnAttendance = createSidebarBtn("Attendance Log");
        Button btnReports = createSidebarBtn("Reports");
        Button btnNotifications = createSidebarBtn("Notifications");

        // Only show teacher-relevant items in the sidebar
        sidebar.getChildren().addAll(brand, btnDashboard, btnAttendance, btnReports, btnNotifications);
        mainLayout.setLeft(sidebar);

        // Content area
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        mainLayout.setCenter(contentArea);

        // Nav actions
        btnDashboard.setOnAction(e -> { setActiveBtn(btnDashboard); switchContent(getStatsView()); });
        btnAttendance.setOnAction(e -> { setActiveBtn(btnAttendance); switchContent(getAttendanceLogView()); });
        btnReports.setOnAction(e -> { setActiveBtn(btnReports); switchContent(getReportsView()); });
        btnNotifications.setOnAction(e -> { setActiveBtn(btnNotifications); switchContent(new NotificationUI(teacherId).getView()); });

        // Default
        setActiveBtn(btnDashboard);
        switchContent(getStatsView());

        Scene scene = new Scene(mainLayout, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        return scene;
    }

    private Button createSidebarBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("sidebar-button");
        return btn;
    }

    private void setActiveBtn(Button btn) {
        if (currentActiveBtn != null) {
            currentActiveBtn.getStyleClass().remove("sidebar-button-active");
            currentActiveBtn.getStyleClass().add("sidebar-button");
        }
        if (btn == null) { currentActiveBtn = null; return; }
        btn.getStyleClass().remove("sidebar-button");
        btn.getStyleClass().add("sidebar-button-active");
        currentActiveBtn = btn;
    }

    private void switchContent(Pane view) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(view);
    }

    // --- Dashboard for Teacher ---
    private VBox getStatsView() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(10));

        Label header = new Label("Welcome, " + (teacherName == null ? "Teacher" : teacherName));
        header.setFont(Font.font("System", FontWeight.BOLD, 22));

        int classCount = 0;
        int studentCount = 0;
        double avgAttendance = 0.0;
        int pendingSessions = 0;
        try {
            List<Class> classes = classDAO.getAllClasses();
            List<Class> mine = new ArrayList<>();
            for (Class c: classes) if (ownsClass(c)) mine.add(c);
            classCount = mine.size();
            Set<String> sids = new HashSet<>();
            for (Class c: mine) {
                for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
                    if (en.getClassId() == c.getId()) sids.add(en.getStudentId());
                }
            }
            studentCount = sids.size();

            // Compute attendance stats only for sessions belonging to this teacher's classes
            int totalRecords = 0;
            int totalPresent = 0;
            for (model.AttendanceSession s : sessionDAO.getAllSessions()) {
                boolean mineSession = false;
                for (Class c : mine) if (c.getId() == s.getClassId()) { mineSession = true; break; }
                if (!mineSession) continue;
                pendingSessions++;
                ArrayList<model.AttendanceRecord> sheet = recordDAO.getAttendanceSheet(s.getId());
                for (model.AttendanceRecord r : sheet) {
                    totalRecords++;
                    if ("Present".equalsIgnoreCase(r.getStatus())) totalPresent++;
                }
            }
            if (totalRecords > 0) {
                avgAttendance = ((double) totalPresent / (double) totalRecords) * 100.0;
            } else {
                avgAttendance = 0.0;
            }
        } catch (Exception ex) { /* ignore */ }

        HBox stats = new HBox(12);
        stats.getChildren().addAll(
            createSmallStat("My Classes", String.valueOf(classCount)),
            createSmallStat("Students in My Classes", String.valueOf(studentCount)),
            createSmallStat("Avg Attendance (My Students)", String.format("%.1f%%", avgAttendance)),
            createSmallStat("My Sessions", String.valueOf(pendingSessions))
        );

        layout.getChildren().addAll(header, stats);
        return layout;
    }

    private VBox createSmallStat(String title, String value) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        Label t = new Label(title);
        t.getStyleClass().add("stat-title");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        card.getChildren().addAll(t, v);
        return card;
    }

    // Student Directory and Class Schedule views were intentionally removed for teacher users.

    // --- Attendance Log (teacher-specific) ---
    private VBox getAttendanceLogView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(10));
        Label title = new Label("Attendance Log");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox filters = new HBox(8);
        ComboBox<Class> classFilter = new ComboBox<>();
        classFilter.setPromptText("Class");
        List<Class> mine = new ArrayList<>();
        for (Class c : classDAO.getAllClasses()) if (ownsClass(c)) mine.add(c);
        classFilter.setItems(FXCollections.observableArrayList(mine));

        ComboBox<String> courseFilter = new ComboBox<>();
        courseFilter.setPromptText("Course");
        ComboBox<String> yearFilter = new ComboBox<>();
        yearFilter.setPromptText("Year");
        ComboBox<AttendanceSession> sessionDate = new ComboBox<>();
        sessionDate.setPromptText("Session Date");

        Button btnApply = new Button("Apply");
        Button btnExport = new Button("Export CSV");

        filters.getChildren().addAll(new Label("Filters:"), classFilter, courseFilter, yearFilter, sessionDate, btnApply, btnExport);

        TableView<AttendanceRecord> table = new TableView<>();
        TableColumn<AttendanceRecord, String> colId = new TableColumn<>("STUDENT ID");
        TableColumn<AttendanceRecord, String> colName = new TableColumn<>("FULL NAME");
        TableColumn<AttendanceRecord, String> colCourseYear = new TableColumn<>("COURSE - YEAR");
        TableColumn<AttendanceRecord, String> colStatus = new TableColumn<>("STATUS");
        TableColumn<AttendanceRecord, String> colTime = new TableColumn<>("TIMESTAMP");

        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCourseYear.setCellValueFactory(cell -> {
            AttendanceRecord r = cell.getValue();
            String course = r.getCourse() == null ? "" : r.getCourse();
            String yr = r.getYearLevel() == null ? "" : r.getYearLevel();
            String comb = course.isEmpty() ? yr : (yr.isEmpty() ? course : course + " - " + yr);
            return new SimpleStringProperty(comb);
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        table.getColumns().addAll(colId, colName, colCourseYear, colStatus, colTime);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // populate sessionDate options for teacher
        ObservableList<AttendanceSession> sessions = FXCollections.observableArrayList();
        for (AttendanceSession ss : sessionDAO.getAllSessions()) {
            // attach only sessions where the class is owned by teacher
            for (Class c : classDAO.getAllClasses()) {
                if (!ownsClass(c)) continue;
                if (c.getId() == ss.getClassId()) { sessions.add(ss); break; }
            }
        }
        sessionDate.setItems(sessions);

        // Populate course/year lists from students in teacher classes
        Set<String> courses = new LinkedHashSet<>();
        Set<String> years = new LinkedHashSet<>();
        for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
            for (Class c : mine) {
                if (en.getClassId() != c.getId()) continue;
                for (Student s : studentDAO.getAllStudents()) {
                    if (!s.getStudentId().equals(en.getStudentId())) continue;
                    if (s.getCourse() != null && !s.getCourse().isEmpty()) courses.add(s.getCourse());
                    if (s.getYearLevel() != null && !s.getYearLevel().isEmpty()) years.add(s.getYearLevel());
                    break;
                }
            }
        }
        courseFilter.setItems(FXCollections.observableArrayList(courses));
        yearFilter.setItems(FXCollections.observableArrayList(years));

        // Controls for teacher to start/end session and mark students
        HBox teacherControls = new HBox(8);
        teacherControls.setAlignment(Pos.CENTER_LEFT);
        Button btnStartSession = new Button("Start Session");
        btnStartSession.getStyleClass().add("primary-button");
        Button btnMarkPresent = new Button("Mark Present"); btnMarkPresent.setDisable(true);
        Button btnMarkAbsent = new Button("Mark Absent"); btnMarkAbsent.setDisable(true);
        Button btnMarkLate = new Button("Mark Late"); btnMarkLate.setDisable(true);
        Button btnEndSession = new Button("End Session"); btnEndSession.setDisable(true);
        teacherControls.getChildren().addAll(btnStartSession, btnMarkPresent, btnMarkLate, btnMarkAbsent, btnEndSession);

        Runnable refreshSessions = () -> {
            ObservableList<AttendanceSession> sss = FXCollections.observableArrayList();
            for (AttendanceSession ss : sessionDAO.getAllSessions()) {
                for (Class c : mine) if (c.getId() == ss.getClassId()) sss.add(ss);
            }
            sessionDate.setItems(sss);
        };

        Runnable apply = () -> {
            Integer classId = (classFilter.getValue() == null) ? null : classFilter.getValue().getId();
            String course = courseFilter.getValue();
            String year = yearFilter.getValue();
            String sdate = (sessionDate.getValue() == null) ? null : sessionDate.getValue().getSessionDate();
            java.util.ArrayList<AttendanceRecord> rows = recordDAO.getAttendanceReport(teacherId, classId, course, year, sdate);
            table.setItems(FXCollections.observableArrayList(rows));
        };

        btnApply.setOnAction(e -> apply.run());

        btnExport.setOnAction(e -> {
            if (table.getItems() == null || table.getItems().isEmpty()) { showAlert("Export", "No rows to export"); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Export Attendance CSV");
            fc.setInitialFileName("attendance_report.csv");
            File f = fc.showSaveDialog(null);
            if (f == null) return;
            try (FileWriter fw = new FileWriter(f)) {
                fw.write("student_id,full_name,course,year_level,status,timestamp\n");
                for (AttendanceRecord r : table.getItems()) {
                    fw.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        safe(r.getStudentId()), safe(r.getFullName()), safe(r.getCourse()), safe(r.getYearLevel()), safe(r.getStatus()), safe(r.getTimestamp())));
                }
                showAlert("Exported", "CSV exported to " + f.getAbsolutePath());
            } catch (Exception ex) { showAlert("Error", "Unable to export: " + ex.getMessage()); }
        });

        // Start Session action: create a session for selected class and date (use today's date if none)
        btnStartSession.setOnAction(ev -> {
            Class sel = classFilter.getValue();
            if (sel == null) { showAlert("Validation", "Please select a class to start a session."); return; }
            AttendanceSession s = new AttendanceSession();
            s.setClassId(sel.getId());
            s.setSessionDate(java.time.LocalDate.now().toString());
            s.setCreatedBy(teacherId);
            int sid = sessionDAO.createSession(s);
            if (sid == -1) { showAlert("Error", "Failed to create session. See console."); return; }
            boolean ok = recordDAO.generateAttendanceSheet(sid, sel.getId(), null, null);
            if (!ok) { showAlert("Error", "Failed to generate attendance sheet."); return; }
            currentSessionId = sid; sessionActive = true;
            btnMarkPresent.setDisable(false); btnMarkAbsent.setDisable(false); btnMarkLate.setDisable(false); btnEndSession.setDisable(false); btnStartSession.setDisable(true);
            refreshSessions.run();
            // load the newly created session in the combo
            for (AttendanceSession ss : sessionDate.getItems()) if (ss.getId() == sid) { sessionDate.setValue(ss); break; }
            apply.run();
            showAlert("Session Started", "Attendance session created and loaded.");
        });

        // Marking actions
        btnMarkPresent.setOnAction(ev -> {
            AttendanceRecord sel = table.getSelectionModel().getSelectedItem();
            if (!sessionActive || currentSessionId == -1) { showAlert("No Session", "Start a session first."); return; }
            if (sel == null) { showAlert("No Selection", "Please select a student to mark."); return; }
            boolean ok = recordDAO.updateAttendanceStatus(currentSessionId, sel.getStudentId(), "Present");
            if (!ok) { showAlert("Error", "Failed to update attendance."); return; }
            sel.setStatus("Present"); sel.setTimestamp(java.time.LocalDateTime.now().toString()); table.refresh();
        });
        btnMarkAbsent.setOnAction(ev -> {
            AttendanceRecord sel = table.getSelectionModel().getSelectedItem();
            if (!sessionActive || currentSessionId == -1) { showAlert("No Session", "Start a session first."); return; }
            if (sel == null) { showAlert("No Selection", "Please select a student to mark."); return; }
            boolean ok = recordDAO.updateAttendanceStatus(currentSessionId, sel.getStudentId(), "Absent");
            if (!ok) { showAlert("Error", "Failed to update attendance."); return; }
            sel.setStatus("Absent"); sel.setTimestamp(java.time.LocalDateTime.now().toString()); table.refresh();
        });
        btnMarkLate.setOnAction(ev -> {
            AttendanceRecord sel = table.getSelectionModel().getSelectedItem();
            if (!sessionActive || currentSessionId == -1) { showAlert("No Session", "Start a session first."); return; }
            if (sel == null) { showAlert("No Selection", "Please select a student to mark."); return; }
            boolean ok = recordDAO.updateAttendanceStatus(currentSessionId, sel.getStudentId(), "Late");
            if (!ok) { showAlert("Error", "Failed to update attendance."); return; }
            sel.setStatus("Late"); sel.setTimestamp(java.time.LocalDateTime.now().toString()); table.refresh();
        });

        // End Session
        btnEndSession.setOnAction(ev -> {
            if (!sessionActive) return;
            sessionActive = false; currentSessionId = -1;
            btnMarkPresent.setDisable(true); btnMarkAbsent.setDisable(true); btnMarkLate.setDisable(true); btnEndSession.setDisable(true); btnStartSession.setDisable(false);
            showAlert("Session Ended", "Attendance session ended and saved.");
        });

        layout.getChildren().addAll(title, filters, teacherControls, table);
        return layout;
    }

    // --- Reports (Attendance & Masterlist) ---
    private VBox getReportsView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(10));
        Label title = new Label("Reports");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        ComboBox<String> reportType = new ComboBox<>();
        reportType.getItems().addAll("Attendance", "Masterlist");
        reportType.setValue("Attendance");

        HBox filters = new HBox(8);
        ComboBox<Class> classFilter = new ComboBox<>();
        List<Class> mine = new ArrayList<>();
        for (Class c : classDAO.getAllClasses()) if (ownsClass(c)) mine.add(c);
        classFilter.setItems(FXCollections.observableArrayList(mine));
        classFilter.setPromptText("Class");

        ComboBox<String> courseFilter = new ComboBox<>();
        courseFilter.setPromptText("Course");
        ComboBox<String> yearFilter = new ComboBox<>();
        yearFilter.setPromptText("Year");
        DatePicker dp = new DatePicker();
        dp.setPromptText("Session Date");

        Button btnApply = new Button("Apply");
        Button btnExport = new Button("Export");

        filters.getChildren().addAll(new Label("Filters:"), classFilter, courseFilter, yearFilter, dp, btnApply, btnExport);

        TableView<AttendanceRecord> reportTable = new TableView<>();
        TableColumn<AttendanceRecord, String> colId = new TableColumn<>("STUDENT ID");
        TableColumn<AttendanceRecord, String> colName = new TableColumn<>("FULL NAME");
        TableColumn<AttendanceRecord, String> colCourseYear = new TableColumn<>("COURSE - YEAR");
        TableColumn<AttendanceRecord, String> colEmail = new TableColumn<>("EMAIL");
        TableColumn<AttendanceRecord, String> colStatus = new TableColumn<>("STATUS");

        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCourseYear.setCellValueFactory(cell -> {
            AttendanceRecord r = cell.getValue();
            String course = r.getCourse() == null ? "" : r.getCourse();
            String yr = r.getYearLevel() == null ? "" : r.getYearLevel();
            String comb = course.isEmpty() ? yr : (yr.isEmpty() ? course : course + " - " + yr);
            return new SimpleStringProperty(comb);
        });
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        reportTable.getColumns().addAll(colId, colName, colCourseYear, colEmail, colStatus);
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        // populate course/year for teacher classes
        Set<String> courses = new LinkedHashSet<>();
        Set<String> years = new LinkedHashSet<>();
        for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
            for (Class c : mine) {
                if (en.getClassId() != c.getId()) continue;
                for (Student s : studentDAO.getAllStudents()) {
                    if (!s.getStudentId().equals(en.getStudentId())) continue;
                    if (s.getCourse() != null && !s.getCourse().isEmpty()) courses.add(s.getCourse());
                    if (s.getYearLevel() != null && !s.getYearLevel().isEmpty()) years.add(s.getYearLevel());
                    break;
                }
            }
        }
        courseFilter.setItems(FXCollections.observableArrayList(courses));
        yearFilter.setItems(FXCollections.observableArrayList(years));

        btnApply.setOnAction(ev -> {
            String type = reportType.getValue();
            Integer classId = (classFilter.getValue() == null) ? null : classFilter.getValue().getId();
            String course = courseFilter.getValue();
            String year = yearFilter.getValue();
            String sdate = dp.getValue() == null ? null : dp.getValue().toString();

            if ("Attendance".equals(type)) {
                java.util.ArrayList<AttendanceRecord> rows = recordDAO.getAttendanceReport(teacherId, classId, course, year, sdate);
                reportTable.setItems(FXCollections.observableArrayList(rows));
            } else {
                // Masterlist: build student list under teacher constraints
                List<Student> rows = new ArrayList<>();
                for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
                    if (classId != null && en.getClassId() != classId) continue;
                    for (Student s : studentDAO.getAllStudents()) {
                        if (!s.getStudentId().equals(en.getStudentId())) continue;
                        if (course != null && !course.isEmpty() && !course.equals(s.getCourse())) continue;
                        if (year != null && !year.isEmpty() && !year.equals(s.getYearLevel())) continue;
                        rows.add(s);
                        break;
                    }
                }
                // convert to AttendanceRecord like objects for table (show id,name,course-year,email,status blank)
                List<AttendanceRecord> conv = new ArrayList<>();
                for (Student s : rows) {
                    AttendanceRecord ar = new AttendanceRecord();
                    ar.setStudentId(s.getStudentId());
                    ar.setFullName(s.getFullName());
                    ar.setCourse(s.getCourse());
                    ar.setYearLevel(s.getYearLevel());
                    ar.setEmail(s.getEmail());
                    ar.setStatus("");
                    conv.add(ar);
                }
                reportTable.setItems(FXCollections.observableArrayList(conv));
            }
        });

        btnExport.setOnAction(ev -> {
            if (reportTable.getItems() == null || reportTable.getItems().isEmpty()) { showAlert("Export", "No rows to export"); return; }
            FileChooser fc = new FileChooser();
            fc.setTitle("Export CSV");
            fc.setInitialFileName("report.csv");
            File f = fc.showSaveDialog(null);
            if (f == null) return;
            try (FileWriter fw = new FileWriter(f)) {
                // header
                fw.write("student_id,full_name,course,year_level,email,status\n");
                for (AttendanceRecord r : reportTable.getItems()) {
                    fw.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        safe(r.getStudentId()), safe(r.getFullName()), safe(r.getCourse()), safe(r.getYearLevel()), safe(r.getEmail()), safe(r.getStatus())));
                }
                showAlert("Exported", "CSV exported to " + f.getAbsolutePath());
            } catch (Exception ex) { showAlert("Error", "Unable to export: " + ex.getMessage()); }
        });

        layout.getChildren().addAll(title, reportType, filters, reportTable);
        return layout;
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String safe(String s) { return s == null ? "" : s.replaceAll(",", " "); }

    private void updateBadgeCount() {
        if (notificationBadge == null) return;
        try {
            java.util.ArrayList<model.AttendanceRecord> low = recordDAO.getLowAttendanceStudentsForTeacher(teacherId, 3);
            if (low != null && low.size() > 0) {
                notificationBadge.setText(String.valueOf(low.size()));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        } catch (Exception ex) {
            notificationBadge.setVisible(false);
        }
    }

    // Helper: determine whether a Class row belongs to the currently authenticated teacher
    private boolean ownsClass(Class c) {
        if (c == null) return false;
        try {
            if (teacherId != null && teacherId.equalsIgnoreCase(c.getTeacherId())) return true;
            // Try matching by teacher full name or email stored in teachers table
            if (c.getTeacherId() != null) {
                Teacher t = teacherDAO.getTeacherById(c.getTeacherId());
                if (t != null) {
                    if (teacherName != null && teacherName.equalsIgnoreCase(t.getFullName())) return true;
                    User cu = AuthenticationService.getInstance().getCurrentUser();
                    if (cu != null && cu.getEmail() != null && cu.getEmail().equalsIgnoreCase(t.getEmail())) return true;
                }
            }
        } catch (Exception ex) {
            // ignore and treat as not owned
        }
        return false;
    }
}

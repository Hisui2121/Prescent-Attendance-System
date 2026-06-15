package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.application.Platform;

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
import util.EventBus;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private String teacherName;

    private Label notificationBadge;

    // Session state for attendance actions
    private int currentSessionId = -1;
    private boolean sessionActive = false;

    // Cached list of classes owned by this teacher (populated once per login)
    private List<Class> myClasses = null;

    public TeacherUI() {
        AuthenticationService auth = AuthenticationService.getInstance();
        this.teacherId = auth.getCurrentUsername();
        User cu = auth.getCurrentUser();
        if (cu != null && cu.getFullName() != null && !cu.getFullName().trim().isEmpty()) {
            this.teacherName = cu.getFullName().trim();
        } else {
            this.teacherName = this.teacherId;
        }
    }

    // -----------------------------------------------------------------
    // Resolve teacher's classes once and cache for the session
    // -----------------------------------------------------------------
    private List<Class> getMyClasses() {
        if (myClasses != null) return myClasses;
        myClasses = new ArrayList<>();
        try {
            for (Class c : classDAO.getAllClasses()) {
                if (ownsClass(c)) myClasses.add(c);
            }
        } catch (Exception ex) {
            System.out.println("TeacherUI.getMyClasses error: " + ex.getMessage());
        }
        return myClasses;
    }

    // -----------------------------------------------------------------
    // Collect all sessions that belong to this teacher's classes
    // -----------------------------------------------------------------
    private List<AttendanceSession> getMySessions() {
        List<AttendanceSession> result = new ArrayList<>();
        Set<Integer> myClassIds = new HashSet<>();
        for (Class c : getMyClasses()) myClassIds.add(c.getId());
        try {
            for (AttendanceSession s : sessionDAO.getAllSessions()) {
                if (myClassIds.contains(s.getClassId())) result.add(s);
            }
        } catch (Exception ex) {
            System.out.println("TeacherUI.getMySessions error: " + ex.getMessage());
        }
        return result;
    }

    // -----------------------------------------------------------------
    // Build the JavaFX scene
    // -----------------------------------------------------------------
    public Scene getScene() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");

        // ── Top bar ──────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 12 20;");

        Label portalTitle = new Label("Teacher Portal");
        portalTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        portalTitle.setStyle("-fx-text-fill: #1E293B;");

        // Notification bell
        StackPane bellPane = new StackPane();
        Button bellBtn = new Button();
        bellBtn.getStyleClass().add("icon-button");
        bellBtn.setFocusTraversable(false);
        try {
            java.io.InputStream bis = getClass().getResourceAsStream("/assets/bell.png");
            if (bis != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(bis));
                iv.setFitWidth(22); iv.setFitHeight(22);
                bellBtn.setGraphic(iv);
            } else { bellBtn.setText("\uD83D\uDD14"); }
        } catch (Exception ex) { bellBtn.setText("\uD83D\uDD14"); }

        Label badge = new Label();
        badge.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 2 6; -fx-background-radius: 10; -fx-min-width: 20; -fx-alignment: center;");
        badge.setVisible(false);
        this.notificationBadge = badge;

        bellBtn.setOnAction(e -> {
            setActiveBtn(null);
            switchContent(new NotificationUI(teacherId).getView());
        });

        bellPane.getChildren().addAll(bellBtn, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 0, 12, 12));

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-button");
        logoutBtn.setOnAction(ev -> {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to log out?", ButtonType.YES, ButtonType.NO);
            conf.setTitle("Confirm Logout"); conf.setHeaderText(null);
            conf.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    AuthenticationService.getInstance().logout();
                    EventBus.fireNotificationChanged();
                    Main.getInstance().showLoginScene();
                }
            });
        });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBar.getChildren().addAll(portalTitle, topSpacer, bellPane, logoutBtn);
        mainLayout.setTop(topBar);

        EventBus.addNotificationChangeListener(() -> Platform.runLater(this::updateBadgeCount));
        updateBadgeCount();

        // ── Sidebar ──────────────────────────────────────────────────
        sidebar = new VBox(6);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(18));
        sidebar.setPrefWidth(220);

        Label brand = new Label("ACE Teacher");
        brand.setFont(Font.font("System", FontWeight.BOLD, 20));
        brand.setStyle("-fx-text-fill: #1E293B; -fx-padding: 0 0 20 0;");

        Button btnDashboard     = createSidebarBtn("Dashboard");
        Button btnAttendance    = createSidebarBtn("Attendance Log");
        Button btnReports       = createSidebarBtn("Reports");
        Button btnNotifications = createSidebarBtn("Notifications");

        sidebar.getChildren().addAll(brand, btnDashboard, btnAttendance, btnReports, btnNotifications);
        mainLayout.setLeft(sidebar);

        // ── Content area ─────────────────────────────────────────────
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(20));
        mainLayout.setCenter(contentArea);

        // ── Navigation ───────────────────────────────────────────────
        btnDashboard.setOnAction(e     -> { setActiveBtn(btnDashboard);     switchContent(getDashboardView()); });
        btnAttendance.setOnAction(e    -> { setActiveBtn(btnAttendance);    switchContent(getAttendanceLogView()); });
        btnReports.setOnAction(e       -> { setActiveBtn(btnReports);       switchContent(getReportsView()); });
        btnNotifications.setOnAction(e -> { setActiveBtn(btnNotifications); switchContent(new NotificationUI(teacherId).getView()); });

        setActiveBtn(btnDashboard);
        switchContent(getDashboardView());

        Scene scene = new Scene(mainLayout, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        return scene;
    }

    // =================================================================
    // 1. DASHBOARD – stats & chart scoped to THIS teacher only
    // =================================================================
    private VBox getDashboardView() {
        VBox layout = new VBox(22);
        layout.setPadding(new Insets(10));

        // Header
        VBox headerText = new VBox(4);
        Label welcome = new Label("Welcome back, " + (teacherName == null ? "Teacher" : teacherName));
        welcome.setFont(Font.font("System", FontWeight.BOLD, 24));
        welcome.setStyle("-fx-text-fill: #1E293B;");
        Label sub = new Label("Here's an overview of your classes and student attendance.");
        sub.setStyle("-fx-text-fill: #64748B;");
        headerText.getChildren().addAll(welcome, sub);

        // ── Stat cards ───────────────────────────────────────────────
        int classCount = 0, studentCount = 0, sessionCount = 0;
        double avgAttendance = 0.0;
        try {
            List<Class> mine = getMyClasses();
            classCount = mine.size();

            Set<String> sids = new HashSet<>();
            for (Class c : mine) {
                for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
                    if (en.getClassId() == c.getId()) sids.add(en.getStudentId());
                }
            }
            studentCount = sids.size();

            int totalRecords = 0, totalPresent = 0;
            for (AttendanceSession s : getMySessions()) {
                sessionCount++;
                for (AttendanceRecord r : recordDAO.getAttendanceSheet(s.getId())) {
                    totalRecords++;
                    if ("Present".equalsIgnoreCase(r.getStatus())) totalPresent++;
                }
            }
            if (totalRecords > 0) avgAttendance = ((double) totalPresent / totalRecords) * 100.0;
        } catch (Exception ex) { /* ignore */ }

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
            createStatCard("MY CLASSES",           String.valueOf(classCount)),
            createStatCard("MY STUDENTS",          String.valueOf(studentCount)),
            createStatCard("AVG. ATTENDANCE",      String.format("%.1f%%", avgAttendance)),
            createStatCard("TOTAL SESSIONS",       String.valueOf(sessionCount))
        );

        // ── Attendance trend chart ────────────────────────────────────
        VBox chartPane = buildDashboardChartPane();

        layout.getChildren().addAll(headerText, statsRow, chartPane);
        return layout;
    }

    private VBox createStatCard(String titleText, String valueText) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label t = new Label(titleText); t.getStyleClass().add("stat-title");
        Label v = new Label(valueText); v.getStyleClass().add("stat-value");
        card.getChildren().addAll(t, v);
        return card;
    }

    /** Attendance trend chart – data scoped to teacher's classes */
    private VBox buildDashboardChartPane() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(14));

        // Header row with filters
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label h = new Label("Attendance Trends — My Classes");
        h.setFont(Font.font("System", FontWeight.BOLD, 15));
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);

        // Class filter (only teacher's classes)
        ComboBox<Class> classFilter = new ComboBox<>();
        classFilter.setPromptText("Class (required)");
        classFilter.setItems(FXCollections.observableArrayList(getMyClasses()));

        // Month & year selectors
        ComboBox<String> monthSelector = new ComboBox<>();
        monthSelector.setPromptText("Month");
        monthSelector.getItems().add("All Months");
        monthSelector.getItems().addAll("January","February","March","April","May","June",
                                        "July","August","September","October","November","December");
        monthSelector.setValue("All Months");

        ComboBox<String> yearSelector = new ComboBox<>();
        int thisYear = LocalDate.now().getYear();
        for (int y = 2022; y <= thisYear; y++) yearSelector.getItems().add(String.valueOf(y));
        yearSelector.setValue(String.valueOf(thisYear));

        Button refreshBtn = new Button("Refresh Chart");
        refreshBtn.setDisable(true);

        headerRow.getChildren().addAll(h, hSpacer, classFilter, monthSelector, yearSelector, refreshBtn);

        // Chart
        CategoryAxis xAxis = new CategoryAxis(); xAxis.setLabel("Period");
        NumberAxis yAxis = new NumberAxis();     yAxis.setLabel("% of Records");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Attendance Percentage Over Time");
        chart.setAnimated(false); chart.setCreateSymbols(true); chart.setPrefHeight(340);
        chart.setLegendVisible(false);

        VBox chartHolder = new VBox(8);
        chartHolder.setFillWidth(true);
        chartHolder.getChildren().add(chart);

        final String[] MONTH_NAMES = {"January","February","March","April","May","June",
                                      "July","August","September","October","November","December"};
        final String[] WEEK_NAMES  = {"1st Week","2nd Week","3rd Week"};

        final Runnable[] doRefresh = {null};

        Runnable updateRefreshState = () -> refreshBtn.setDisable(classFilter.getValue() == null);

        classFilter.valueProperty().addListener((obs, o, n) -> { updateRefreshState.run(); if (!refreshBtn.isDisabled() && doRefresh[0] != null) doRefresh[0].run(); });
        monthSelector.valueProperty().addListener((obs, o, n) -> { if (!refreshBtn.isDisabled() && doRefresh[0] != null) doRefresh[0].run(); });
        yearSelector.valueProperty().addListener((obs, o, n)  -> { if (!refreshBtn.isDisabled() && doRefresh[0] != null) doRefresh[0].run(); });

        doRefresh[0] = () -> {
            chart.getData().clear();
            chartHolder.getChildren().clear();

            Class selClass = classFilter.getValue();
            if (selClass == null) { chartHolder.getChildren().add(chart); return; }

            String selectedMonth = monthSelector.getValue();
            int selectedYear = thisYear;
            try { selectedYear = Integer.parseInt(yearSelector.getValue()); } catch (Exception ignore) {}

            // Collect sessions for this specific class (already teacher-scoped via getMySessions)
            List<AttendanceSession> classSessions = new ArrayList<>();
            for (AttendanceSession s : getMySessions()) {
                if (s.getClassId() == selClass.getId()) classSessions.add(s);
            }

            // Build a map of sessionId -> session_date for date-based bucketing
            Map<Integer, String> sessionDateMap = new HashMap<>();
            for (AttendanceSession s : classSessions) sessionDateMap.put(s.getId(), s.getSessionDate());

            // Fetch all attendance records for matching sessions
            List<AttendanceRecord> records = new ArrayList<>();
            for (AttendanceSession s : classSessions) {
                records.addAll(recordDAO.getAttendanceSheet(s.getId()));
            }
            // Attach session_date to each record via sessionId lookup (getAttendanceSheet sets sessionId)
            // We'll key bucket from the session's session_date, not the record timestamp

            boolean isSpecificMonth = selectedMonth != null && !selectedMonth.equals("All Months");
            Integer specificMonthIdx = null;
            if (isSpecificMonth) {
                for (int i = 0; i < MONTH_NAMES.length; i++) if (MONTH_NAMES[i].equals(selectedMonth)) { specificMonthIdx = i + 1; break; }
            }

            Map<String, int[]> totals = new LinkedHashMap<>();
            List<String> buckets = new ArrayList<>();

            if (isSpecificMonth) {
                for (String w : WEEK_NAMES) { totals.put(w, new int[]{0,0,0}); buckets.add(w); }
            } else {
                for (String m : MONTH_NAMES) { totals.put(m, new int[]{0,0,0}); buckets.add(m); }
            }

            final int finalSelectedYear = selectedYear;
            final Integer finalMonthIdx = specificMonthIdx;

            for (AttendanceRecord r : records) {
                // Use session_date for bucketing (reliable date the teacher chose)
                String sdate = sessionDateMap.get(r.getSessionId());
                if (sdate == null) continue;
                LocalDate sd;
                try { sd = LocalDate.parse(sdate.substring(0, 10)); } catch (Exception ex) { continue; }
                if (sd.getYear() != finalSelectedYear) continue;

                String bucketKey;
                if (isSpecificMonth) {
                    if (finalMonthIdx == null || sd.getMonthValue() != finalMonthIdx) continue;
                    int day = sd.getDayOfMonth();
                    if      (day <= 7)  bucketKey = WEEK_NAMES[0];
                    else if (day <= 14) bucketKey = WEEK_NAMES[1];
                    else if (day <= 21) bucketKey = WEEK_NAMES[2];
                    else               bucketKey = WEEK_NAMES[2]; // last week falls into 3rd week bucket
                } else {
                    bucketKey = MONTH_NAMES[sd.getMonthValue() - 1];
                }

                int[] arr = totals.get(bucketKey);
                if (arr == null) continue;
                String status = r.getStatus() == null ? "" : r.getStatus().trim();
                if      ("Present".equalsIgnoreCase(status)) arr[0]++;
                else if ("Late"   .equalsIgnoreCase(status)) arr[1]++;
                else if ("Absent" .equalsIgnoreCase(status)) arr[2]++;
            }

            xAxis.setCategories(FXCollections.observableArrayList(buckets));

            XYChart.Series<String,Number> presentSeries = new XYChart.Series<>(); presentSeries.setName("Present");
            XYChart.Series<String,Number> lateSeries    = new XYChart.Series<>(); lateSeries.setName("Late");
            XYChart.Series<String,Number> absentSeries  = new XYChart.Series<>(); absentSeries.setName("Absent");

            final int[] overall = {0,0,0};
            for (String bk : buckets) {
                int[] arr = totals.get(bk); int p=arr[0], l=arr[1], a=arr[2], tot=p+l+a;
                overall[0]+=p; overall[1]+=l; overall[2]+=a;
                double pPct=(tot==0)?0:((double)p/tot)*100;
                double lPct=(tot==0)?0:((double)l/tot)*100;
                double aPct=(tot==0)?0:((double)a/tot)*100;
                presentSeries.getData().add(new XYChart.Data<>(bk, pPct));
                lateSeries   .getData().add(new XYChart.Data<>(bk, lPct));
                absentSeries .getData().add(new XYChart.Data<>(bk, aPct));
            }

            chart.getData().addAll(presentSeries, lateSeries, absentSeries);
            chart.setLegendVisible(true);

            // Summary row
            HBox summaryRow = new HBox(18); summaryRow.setPadding(new Insets(6,0,0,0)); summaryRow.setAlignment(Pos.CENTER_LEFT);
            summaryRow.getChildren().addAll(
                makeSummaryBox("Present", String.valueOf(overall[0]), "#16a34a"),
                makeSummaryBox("Late",    String.valueOf(overall[1]), "#d97706"),
                makeSummaryBox("Absent",  String.valueOf(overall[2]), "#dc2626")
            );

            // Apply node colours after layout
            Platform.runLater(() -> {
                try {
                    for (XYChart.Series<String,Number> srs : Arrays.asList(presentSeries, lateSeries, absentSeries)) {
                        String clr = "Present".equals(srs.getName()) ? "#16a34a" : "Late".equals(srs.getName()) ? "#d97706" : "#dc2626";
                        Node ln = srs.getNode(); if (ln != null) ln.setStyle("-fx-stroke: "+clr+"; -fx-stroke-width: 2px;");
                        for (XYChart.Data<String,Number> d : srs.getData()) {
                            Node n = d.getNode();
                            if (n != null) {
                                n.setStyle("-fx-background-color: "+clr+", white;");
                                String bk = (String) d.getXValue();
                                int[] arr = totals.get(bk); int p2=0,l2=0,a2=0;
                                if (arr!=null){p2=arr[0];l2=arr[1];a2=arr[2];}
                                Tooltip.install(n, new Tooltip("Period: "+bk+"\nPresent: "+p2+"\nLate: "+l2+"\nAbsent: "+a2+"\n"+srs.getName()+": "+String.format("%.1f%%",d.getYValue().doubleValue())));
                            }
                        }
                    }
                } catch (Exception ex2) { System.out.println("Chart style error: " + ex2.getMessage()); }
            });

            chartHolder.getChildren().addAll(chart, summaryRow);
            VBox.setVgrow(chart, Priority.ALWAYS);
            if (!box.getChildren().contains(chartHolder)) box.getChildren().add(chartHolder);
        };

        refreshBtn.setOnAction(e -> { if (doRefresh[0] != null) doRefresh[0].run(); });
        box.getChildren().addAll(headerRow, chartHolder);
        return box;
    }

    private VBox makeSummaryBox(String label, String value, String color) {
        VBox v = new VBox(3);
        Label t = new Label(label); t.setStyle("-fx-font-weight: bold;");
        Label n = new Label(value); n.setStyle("-fx-font-size: 15; -fx-text-fill: " + color + ";");
        v.getChildren().addAll(t, n);
        return v;
    }

    // =================================================================
    // 2. ATTENDANCE LOG – teacher can start/end sessions only for own classes
    // =================================================================
    private VBox getAttendanceLogView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(10));

        Label title = new Label("Attendance Log");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        // ── Filters ──────────────────────────────────────────────────
        HBox filters = new HBox(8);
        filters.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Class> classFilter = new ComboBox<>();
        classFilter.setPromptText("Class");
        classFilter.setItems(FXCollections.observableArrayList(getMyClasses()));

        ComboBox<String> courseFilter = new ComboBox<>(); courseFilter.setPromptText("Course");
        ComboBox<String> yearFilter   = new ComboBox<>(); yearFilter.setPromptText("Year Level");
        DatePicker sessionDatePicker = new DatePicker(); sessionDatePicker.setPromptText("Session Date");
        sessionDatePicker.setEditable(false);

        Button btnApply  = new Button("Apply");

        filters.getChildren().addAll(new Label("Filters:"), classFilter, courseFilter, yearFilter, sessionDatePicker, btnApply);

        // ── Table ────────────────────────────────────────────────────
        TableView<AttendanceRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AttendanceRecord,String> colId       = new TableColumn<>("STUDENT ID");
        TableColumn<AttendanceRecord,String> colName     = new TableColumn<>("FULL NAME");
        TableColumn<AttendanceRecord,String> colCourseYr = new TableColumn<>("COURSE – YEAR");
        TableColumn<AttendanceRecord,String> colStatus   = new TableColumn<>("STATUS");
        TableColumn<AttendanceRecord,String> colTime     = new TableColumn<>("TIMESTAMP");

        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCourseYr.setCellValueFactory(cell -> {
            AttendanceRecord r = cell.getValue();
            String c2 = r.getCourse() == null ? "" : r.getCourse();
            String y2 = r.getYearLevel() == null ? "" : r.getYearLevel();
            return new SimpleStringProperty(c2.isEmpty() ? y2 : (y2.isEmpty() ? c2 : c2 + " – " + y2));
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        table.getColumns().addAll(colId, colName, colCourseYr, colStatus, colTime);

        // ── Session controls ─────────────────────────────────────────
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER_LEFT);
        Button btnStart  = new Button("▶ Start Session"); btnStart.getStyleClass().add("primary-button");
        Button btnEnd    = new Button("■ End Session");   btnEnd.setDisable(true);
        Button btnPresent= new Button("✔ Mark Present"); btnPresent.setDisable(true);
        Button btnLate   = new Button("⏱ Mark Late");    btnLate.setDisable(true);
        Button btnAbsent = new Button("✖ Mark Absent");  btnAbsent.setDisable(true);
        Label sessionLabel = new Label();
        sessionLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
        controls.getChildren().addAll(btnStart, btnEnd, new Separator(javafx.geometry.Orientation.VERTICAL), btnPresent, btnLate, btnAbsent, sessionLabel);

        // Populate course/year dropdowns from teacher's students
        populateCourseYear(getMyClasses(), courseFilter, yearFilter);

        // Apply filter → load attendance records using the chosen calendar date
        Runnable applyFilter = () -> {
            Integer classId = classFilter.getValue() == null ? null : classFilter.getValue().getId();
            String  course  = courseFilter.getValue();
            String  year    = yearFilter.getValue();
            String  sdate   = sessionDatePicker.getValue() == null ? null : sessionDatePicker.getValue().toString();
            List<AttendanceRecord> rows = recordDAO.getAttendanceReport(teacherId, classId, course, year, sdate);
            table.setItems(FXCollections.observableArrayList(rows));
        };

        btnApply.setOnAction(e -> applyFilter.run());

        // Start session (only for teacher's own class)
        btnStart.setOnAction(ev -> {
            Class sel = classFilter.getValue();
            if (sel == null) { showAlert("Validation", "Please select one of your classes first."); return; }
            if (!ownsClass(sel)) { showAlert("Access Denied", "You can only start sessions for your own classes."); return; }
            AttendanceSession s = new AttendanceSession();
            s.setClassId(sel.getId());
            LocalDate sessionDate = sessionDatePicker.getValue() != null ? sessionDatePicker.getValue() : LocalDate.now();
            s.setSessionDate(sessionDate.toString());
            s.setCreatedBy(teacherId);
            int sid = sessionDAO.createSession(s);
            if (sid == -1) { showAlert("Error", "Failed to create session."); return; }
            if (!recordDAO.generateAttendanceSheet(sid, sel.getId(), null, null)) { showAlert("Error", "Failed to generate attendance sheet."); return; }
            currentSessionId = sid; sessionActive = true;
            btnStart.setDisable(true); btnEnd.setDisable(false);
            btnPresent.setDisable(false); btnLate.setDisable(false); btnAbsent.setDisable(false);
            sessionLabel.setText("Session #" + sid + " active  |  " + sessionDate);
            applyFilter.run();
            showAlert("Session Started", "Attendance session created for " + sel.getClassCode() + " on " + sessionDate + ".");
        });

        btnPresent.setOnAction(ev -> markStudent(table, "Present"));
        btnLate   .setOnAction(ev -> markStudent(table, "Late"));
        btnAbsent .setOnAction(ev -> markStudent(table, "Absent"));

        // End session
        btnEnd.setOnAction(ev -> {
            sessionActive = false; currentSessionId = -1;
            btnStart.setDisable(false); btnEnd.setDisable(true);
            btnPresent.setDisable(true); btnLate.setDisable(true); btnAbsent.setDisable(true);
            sessionLabel.setText("Session ended.");
            showAlert("Session Ended", "Attendance session saved successfully.");
        });

        layout.getChildren().addAll(title, filters, controls, table);
        return layout;
    }

    private void markStudent(TableView<AttendanceRecord> table, String status) {
        if (!sessionActive || currentSessionId == -1) { showAlert("No Active Session", "Please start a session first."); return; }
        AttendanceRecord sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("No Selection", "Please select a student row to mark."); return; }
        if (recordDAO.updateAttendanceStatus(currentSessionId, sel.getStudentId(), status)) {
            sel.setStatus(status); sel.setTimestamp(LocalDateTime.now().toString()); table.refresh();
        } else { showAlert("Error", "Failed to update attendance. Please try again."); }
    }

    // =================================================================
    // 3. REPORTS – masterlist & attendance sessions for teacher's classes
    // =================================================================
    private VBox getReportsView() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(10));

        Label title = new Label("Reports");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        ComboBox<String> reportType = new ComboBox<>();
        reportType.getItems().addAll("Attendance", "Masterlist");
        reportType.setValue("Attendance");

        // Filters
        HBox filters = new HBox(8); filters.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Class> classFilter = new ComboBox<>();
        classFilter.setPromptText("Class");
        classFilter.setItems(FXCollections.observableArrayList(getMyClasses()));

        ComboBox<String> courseFilter = new ComboBox<>(); courseFilter.setPromptText("Course");
        ComboBox<String> yearFilter   = new ComboBox<>(); yearFilter.setPromptText("Year Level");

        // Session picker shows only saved attendance sessions for this teacher's classes
        ComboBox<AttendanceSession> sessionPicker = new ComboBox<>();
        sessionPicker.setPromptText("Session Date");
        sessionPicker.setItems(FXCollections.observableArrayList(getMySessions()));
        // Show session date + class info in the dropdown
        sessionPicker.setCellFactory(lv -> new ListCell<AttendanceSession>() {
            @Override protected void updateItem(AttendanceSession s, boolean empty) {
                super.updateItem(s, empty); setText(empty || s == null ? null : s.getSessionDate() + "  (Session #" + s.getId() + ")"); }
        });
        sessionPicker.setButtonCell(new ListCell<AttendanceSession>() {
            @Override protected void updateItem(AttendanceSession s, boolean empty) {
                super.updateItem(s, empty); setText(empty || s == null ? null : s.getSessionDate() + "  (Session #" + s.getId() + ")"); }
        });

        Button btnApply  = new Button("Apply");
        Button btnExport = new Button("Export CSV");
        filters.getChildren().addAll(new Label("Filters:"), classFilter, courseFilter, yearFilter, sessionPicker, btnApply, btnExport);

        // Table
        TableView<AttendanceRecord> reportTable = new TableView<>();
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        TableColumn<AttendanceRecord,String> colId       = new TableColumn<>("STUDENT ID");
        TableColumn<AttendanceRecord,String> colName     = new TableColumn<>("FULL NAME");
        TableColumn<AttendanceRecord,String> colCourseYr = new TableColumn<>("COURSE – YEAR");
        TableColumn<AttendanceRecord,String> colEmail    = new TableColumn<>("EMAIL");
        TableColumn<AttendanceRecord,String> colStatus   = new TableColumn<>("STATUS");

        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCourseYr.setCellValueFactory(cell -> {
            AttendanceRecord r = cell.getValue();
            String c2 = r.getCourse() == null ? "" : r.getCourse();
            String y2 = r.getYearLevel() == null ? "" : r.getYearLevel();
            return new SimpleStringProperty(c2.isEmpty() ? y2 : (y2.isEmpty() ? c2 : c2 + " – " + y2));
        });
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        reportTable.getColumns().addAll(colId, colName, colCourseYr, colEmail, colStatus);

        populateCourseYear(getMyClasses(), courseFilter, yearFilter);

        btnApply.setOnAction(ev -> {
            String type    = reportType.getValue();
            Integer classId = classFilter.getValue() == null ? null : classFilter.getValue().getId();
            String  course  = courseFilter.getValue();
            String  year    = yearFilter.getValue();
            String  sdate   = sessionPicker.getValue() == null ? null : sessionPicker.getValue().getSessionDate();

            if ("Attendance".equals(type)) {
                List<AttendanceRecord> rows = recordDAO.getAttendanceReport(teacherId, classId, course, year, sdate);
                reportTable.setItems(FXCollections.observableArrayList(rows));
            } else {
                // Masterlist: students enrolled in teacher's classes only
                List<AttendanceRecord> conv = new ArrayList<>();
                Set<Integer> filterClassIds = new HashSet<>();
                if (classId != null) {
                    filterClassIds.add(classId);
                } else {
                    for (Class c : getMyClasses()) filterClassIds.add(c.getId());
                }
                Set<String> seen = new HashSet<>();
                for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
                    if (!filterClassIds.contains(en.getClassId())) continue;
                    for (Student s : studentDAO.getAllStudents()) {
                        if (!s.getStudentId().equals(en.getStudentId())) continue;
                        if (course != null && !course.isEmpty() && !course.equals(s.getCourse())) continue;
                        if (year   != null && !year.isEmpty()   && !year.equals(s.getYearLevel())) continue;
                        if (seen.add(s.getStudentId())) {
                            AttendanceRecord ar = new AttendanceRecord();
                            ar.setStudentId(s.getStudentId());
                            ar.setFullName(s.getFullName());
                            ar.setCourse(s.getCourse());
                            ar.setYearLevel(s.getYearLevel());
                            ar.setEmail(s.getEmail());
                            ar.setStatus("");
                            conv.add(ar);
                        }
                        break;
                    }
                }
                reportTable.setItems(FXCollections.observableArrayList(conv));
            }
        });

        layout.getChildren().addAll(title, reportType, filters, reportTable);
        btnExport.setOnAction(ev -> {
            if (reportTable.getItems() == null || reportTable.getItems().isEmpty()) { showAlert("Export", "No rows to export."); return; }
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Export CSV");
            String reportName = "Attendance".equals(reportType.getValue()) ? "attendance_report" : "masterlist";
            fc.setInitialFileName(reportName + ".csv");
            java.io.File f = fc.showSaveDialog(null); if (f == null) return;
            try (java.io.FileWriter fw = new java.io.FileWriter(f)) {
                fw.write("student_id,full_name,course,year_level,email,status\n");
                for (AttendanceRecord r : reportTable.getItems())
                    fw.write(String.format("%s,%s,%s,%s,%s,%s\n",
                        safe(r.getStudentId()), safe(r.getFullName()), safe(r.getCourse()),
                        safe(r.getYearLevel()), safe(r.getEmail()), safe(r.getStatus())));
                showAlert("Exported", "CSV saved to: " + f.getAbsolutePath());
            } catch (Exception ex) { showAlert("Error", "Export failed: " + ex.getMessage()); }
        });
        return layout;
    }

    // =================================================================
    // Helpers
    // =================================================================

    /** Populate course + year dropdowns from students in the given classes */
    private void populateCourseYear(List<Class> classes, ComboBox<String> courseBox, ComboBox<String> yearBox) {
        Set<String> courses = new LinkedHashSet<>();
        Set<String> years   = new LinkedHashSet<>();
        try {
            Set<Integer> ids = new HashSet<>();
            for (Class c : classes) ids.add(c.getId());
            for (model.Enrollment en : enrollmentDAO.getAllEnrollments()) {
                if (!ids.contains(en.getClassId())) continue;
                for (Student s : studentDAO.getAllStudents()) {
                    if (!s.getStudentId().equals(en.getStudentId())) continue;
                    if (s.getCourse()    != null && !s.getCourse().isEmpty())    courses.add(s.getCourse());
                    if (s.getYearLevel() != null && !s.getYearLevel().isEmpty()) years.add(s.getYearLevel());
                    break;
                }
            }
        } catch (Exception ex) { System.out.println("populateCourseYear error: " + ex.getMessage()); }
        courseBox.setItems(FXCollections.observableArrayList(courses));
        yearBox.setItems(FXCollections.observableArrayList(years));
    }

    /** Determine whether a class belongs to the currently logged-in teacher */
    private boolean ownsClass(Class c) {
        if (c == null) return false;
        try {
            if (teacherId != null && teacherId.equalsIgnoreCase(c.getTeacherId())) return true;
            if (c.getTeacherId() != null) {
                Teacher t = teacherDAO.getTeacherById(c.getTeacherId());
                if (t != null) {
                    if (teacherName != null && teacherName.equalsIgnoreCase(t.getFullName())) return true;
                    User cu = AuthenticationService.getInstance().getCurrentUser();
                    if (cu != null && cu.getEmail() != null && cu.getEmail().equalsIgnoreCase(t.getEmail())) return true;
                }
            }
        } catch (Exception ex) { /* treat as not owned */ }
        return false;
    }

    private void updateBadgeCount() {
        if (notificationBadge == null) return;
        try {
            List<AttendanceRecord> low = recordDAO.getLowAttendanceStudentsForTeacher(teacherId, 3);
            if (low != null && !low.isEmpty()) {
                notificationBadge.setText(String.valueOf(low.size()));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        } catch (Exception ex) { notificationBadge.setVisible(false); }
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

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private String safe(String s) { return s == null ? "" : s.replaceAll(",", " "); }
}
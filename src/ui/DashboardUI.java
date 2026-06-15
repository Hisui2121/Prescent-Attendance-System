package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import dao.StudentDAO;
import dao.ClassDAO;
import dao.AttendanceSessionDAO;
import dao.AttendanceRecordDAO;

import java.util.ArrayList;
import java.util.*;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tooltip;
import javafx.scene.Node;
import javafx.application.Platform; // added to fix Platform unresolved error
import javafx.collections.FXCollections; // added to fix FXCollections unresolved error
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import util.EventBus;
import service.AuthenticationService;
import app.Main;

public class DashboardUI {
    private BorderPane mainLayout;
    private StackPane contentArea;
    private VBox sidebar;
    private Button currentActiveBtn; // Para ma-track ang dilaw na highlight
    // notification badge label reference
    private Label notificationBadge;

    private StudentDAO studentDAO = new StudentDAO();
    private ClassDAO classDAO = new ClassDAO();
    private AttendanceSessionDAO sessionDAO = new AttendanceSessionDAO();
    private AttendanceRecordDAO recordDAO = new AttendanceRecordDAO();

    // Keep a single AttendanceUI instance so its UI state (active session) is preserved when navigating
    private AttendanceUI attendanceUI = new AttendanceUI();
    // Keep a single ReportUI instance so the report page is always present and stateful
    private ReportUI reportUI = new ReportUI();

    public Scene getScene() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");
        
        // --- TOP BAR (Optional, pang pa-ganda) ---
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 15 30;");
        Label breadcrumb = new Label("Dashboard / Overview");
        breadcrumb.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");

        // Notification bell with badge
        StackPane bellPane = new StackPane();
        Button bellBtn = new Button();
        bellBtn.getStyleClass().add("icon-button");
        bellBtn.setFocusTraversable(false);
        // try to load image asset if available
        try {
            java.io.InputStream bis = getClass().getResourceAsStream("/assets/bell.png");
            if (bis != null) {
                ImageView iv = new ImageView(new Image(bis));
                iv.setFitWidth(22);
                iv.setFitHeight(22);
                bellBtn.setGraphic(iv);
            } else {
                bellBtn.setText("\uD83D\uDD14"); // bell emoji fallback
            }
        } catch (Exception ex) {
            bellBtn.setText("\uD83D\uDD14");
        }

        Label badge = new Label();
        badge.getStyleClass().add("badge");
        badge.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 2 6; -fx-background-radius: 10; -fx-min-width: 20; -fx-alignment: center;");
        badge.setVisible(false);

        // compute notification count
        try {
            java.util.ArrayList<model.AttendanceRecord> low = recordDAO.getLowAttendanceStudents(3);
            if (low != null && low.size() > 0) {
                badge.setText(String.valueOf(low.size()));
                badge.setVisible(true);
            } else {
                badge.setVisible(false);
             }
        } catch (Exception ex) {
            // ignore
        }

        // keep reference for later updates and register listener
        this.notificationBadge = badge;
        EventBus.addNotificationChangeListener(() -> Platform.runLater(() -> updateBadgeCount()));

        // clicking bell opens notifications page
        bellBtn.setOnAction(e -> {
            setActiveBtn(null); // remove sidebar highlight since bell is separate
            switchContent(new NotificationUI().getView());
        });

        bellPane.getChildren().addAll(bellBtn, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 0, 12, 12));

        // Logout button beside bell
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-button");
        logoutBtn.setOnAction(ev -> {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to log out?", ButtonType.YES, ButtonType.NO);
            conf.setTitle("Confirm Logout");
            conf.setHeaderText(null);
            conf.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    AuthenticationService.getInstance().logout();
                    // notify and go back to login
                    EventBus.fireNotificationChanged();
                    Main.getInstance().showLoginScene();
                }
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(breadcrumb, spacer, bellPane, logoutBtn);
        mainLayout.setTop(topBar);

        // --- SIDEBAR ---
        sidebar = new VBox(5);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setPadding(new Insets(20, 15, 20, 15));

        Label brand = new Label("ACE Admin");
        brand.setFont(Font.font("System", FontWeight.BOLD, 20));
        brand.setStyle("-fx-text-fill: #1E293B; -fx-padding: 0 0 30 5;");

        Button btnDashboard = createSidebarBtn("Dashboard");
        Button btnStudent = createSidebarBtn("Student Directory");
        Button btnClass = createSidebarBtn("Class Schedules");
        Button btnUsers = createSidebarBtn("System Users");
        Button btnEnroll = createSidebarBtn("Enrollments");
        Button btnAttendance = createSidebarBtn("Attendance Log");
        Button btnReport = createSidebarBtn("Reports");
        Button btnNotifications = createSidebarBtn("Notifications");

        sidebar.getChildren().addAll(brand, btnDashboard, btnStudent, btnClass, btnUsers, btnEnroll, btnAttendance, btnReport, btnNotifications);
        mainLayout.setLeft(sidebar);

        // --- CONTENT AREA ---
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(30));
        mainLayout.setCenter(contentArea);

        // --- NAVIGATION LOGIC ---
        btnDashboard.setOnAction(e -> { setActiveBtn(btnDashboard); switchContent(getStatsView()); });
        btnStudent.setOnAction(e -> { setActiveBtn(btnStudent); switchContent(new StudentUI().getView()); });
        btnClass.setOnAction(e -> { setActiveBtn(btnClass); switchContent(new ClassUI().getView()); });
        btnUsers.setOnAction(e -> { setActiveBtn(btnUsers); switchContent(new SystemUserUI().getView()); });
        btnEnroll.setOnAction(e -> { setActiveBtn(btnEnroll); switchContent(new EnrollmentUI().getView()); });
        btnAttendance.setOnAction(e -> { setActiveBtn(btnAttendance); switchContent(attendanceUI.getView()); });
        btnReport.setOnAction(e -> { setActiveBtn(btnReport); switchContent(reportUI.getView()); });
        btnNotifications.setOnAction(e -> { setActiveBtn(btnNotifications); switchContent(new NotificationUI().getView()); });

        // Default Load
        setActiveBtn(btnDashboard);
        switchContent(getStatsView());

        Scene scene = new Scene(mainLayout, 1200, 750);
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
        if(currentActiveBtn != null) {
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

    // --- THE STATS DASHBOARD ---
    private VBox getStatsView() {
        VBox layout = new VBox(25);

        // Header
        HBox header = new HBox();
        VBox texts = new VBox(5);
        Label title = new Label("Welcome back, Admin");
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setStyle("-fx-text-fill: #1E293B;");
        Label subtitle = new Label("Here's an overview of ACE University's Academic Performance.");
        subtitle.setStyle("-fx-text-fill: #64748B;");
        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(texts, spacer);
        header.setAlignment(Pos.CENTER_LEFT);

        // Compute dynamic stats fresh from DB every time this view is opened
        int totalStudents = 0;
        int activeClasses = 0;
        double avgAttendance = 0.0;
        int totalSessions = 0;

        try { totalStudents = studentDAO.getAllStudents().size(); } catch (Exception ex) { totalStudents = 0; }
        try { activeClasses = classDAO.getAllClasses().size(); }   catch (Exception ex) { activeClasses = 0; }

        try {
            ArrayList<model.AttendanceSession> sessions = sessionDAO.getAllSessions();
            totalSessions = sessions.size();
            int totalRecords = 0, totalPresent = 0;
            for (model.AttendanceSession s : sessions) {
                for (model.AttendanceRecord r : recordDAO.getAttendanceSheet(s.getId())) {
                    totalRecords++;
                    if ("Present".equalsIgnoreCase(r.getStatus())) totalPresent++;
                }
            }
            avgAttendance = totalRecords > 0 ? ((double) totalPresent / totalRecords) * 100.0 : 0.0;
        } catch (Exception ex) { avgAttendance = 0.0; totalSessions = 0; }

        // 4 Stat Cards Row - values reflect latest DB state
        HBox statsRow = new HBox(20);
        statsRow.getChildren().addAll(
            createStatCard("TOTAL STUDENTS",  String.valueOf(totalStudents)),
            createStatCard("AVG. ATTENDANCE", String.format("%.1f%%", avgAttendance)),
            createStatCard("ACTIVE CLASSES",  String.valueOf(activeClasses)),
            createStatCard("TOTAL SESSIONS",  String.valueOf(totalSessions))
        );

        // Attendance chart pane (chartPane contains a blank chart by default)
        VBox chartPane = createAttendanceChartPane();

        layout.getChildren().addAll(header, statsRow, chartPane);
        return layout;
    }

    private VBox createStatCard(String titleText, String valueText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS); // Makes all cards equal width
        
        Label title = new Label(titleText);
        title.getStyleClass().add("stat-title");
        
        Label value = new Label(valueText);
        value.getStyleClass().add("stat-value");

        card.getChildren().addAll(title, value);
        return card;
    }

    // -- Attendance chart UI and logic --
    private VBox createAttendanceChartPane() {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(12));

        HBox header = new HBox(10);
        Label h = new Label("Attendance Trends");
        h.setFont(Font.font("System", FontWeight.BOLD, 16));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filters
        ComboBox<String> courseFilter = new ComboBox<>();
        courseFilter.setPromptText("Course (required)");
        ComboBox<String> yearFilter = new ComboBox<>();
        yearFilter.setPromptText("Year Level (required)");
        ComboBox<String> classFilter = new ComboBox<>();
        classFilter.setPromptText("Class (required)");

        // Month selector (All or specific month)
        ComboBox<String> monthSelector = new ComboBox<>();
        monthSelector.setPromptText("Month");
        monthSelector.getItems().add("All Months");
        monthSelector.getItems().addAll("January","February","March","April","May","June","July","August","September","October","November","December");
        monthSelector.setValue("All Months");

        // Year selector for choosing which year to display (keeps monthly buckets)
        ComboBox<String> yearSelector = new ComboBox<>();
        int thisYear = java.time.LocalDate.now().getYear();
        for (int y = 2022; y <= thisYear; y++) yearSelector.getItems().add(String.valueOf(y));
        yearSelector.setValue(String.valueOf(thisYear));
        yearSelector.setPromptText("Year");

        Button refresh = new Button("Refresh Chart");
        refresh.setDisable(true); // make refresh disabled until required selections made

        header.getChildren().addAll(h, spacer, courseFilter, yearFilter, classFilter, monthSelector, yearSelector, refresh);

        // prepare chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Period");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("% of Records");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Attendance Percentage Over Time");
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setPrefHeight(360);
        // start with an empty (blank) chart and hide legend; legend will be shown when data is added on refresh
        chart.setLegendVisible(false);

        // chartHolder prevents accumulation of multiple chart rows on refresh
        VBox chartHolder = new VBox(8);
        chartHolder.setFillWidth(true);
        HBox.setHgrow(chart, Priority.ALWAYS);

        // show the empty chart area by default (no series yet). When user applies filters and refreshes,
        // doRefreshHolder will clear and repopulate this chartHolder including showing the legend.
        chartHolder.getChildren().add(chart);

        // populate filter options from DAOs (no blank entries)
        try {
            List<model.Class> classes = classDAO.getAllClasses();
            List<String> classLabels = new ArrayList<>();
            for (model.Class c : classes) classLabels.add(c.getId() + ": " + c.getClassCode());
            classFilter.getItems().addAll(classLabels);
            Set<String> courses = new HashSet<>();
            Set<String> years = new HashSet<>();
            for (model.Student s : studentDAO.getAllStudents()) {
                if (s.getCourse() != null) courses.add(s.getCourse());
                if (s.getYearLevel() != null) years.add(s.getYearLevel());
            }
            courseFilter.getItems().addAll(courses);
            yearFilter.getItems().addAll(years);
        } catch (Exception ex) {
            System.out.println("Chart filter population error: " + ex.getMessage());
        }

        final String[] MONTH_NAMES = new String[]{"January","February","March","April","May","June","July","August","September","October","November","December"};

        // Enable refresh only when required filters have values
        final Runnable[] doRefreshHolder = new Runnable[1];

        Runnable updateRefreshState = () -> {
            boolean ok = (courseFilter.getValue() != null && yearFilter.getValue() != null && classFilter.getValue() != null);
            refresh.setDisable(!ok);
        };

        courseFilter.valueProperty().addListener((obs, o, n) -> { updateRefreshState.run(); if (!refresh.isDisabled() && doRefreshHolder[0] != null) doRefreshHolder[0].run(); });
        yearFilter.valueProperty().addListener((obs, o, n) -> { updateRefreshState.run(); if (!refresh.isDisabled() && doRefreshHolder[0] != null) doRefreshHolder[0].run(); });
        classFilter.valueProperty().addListener((obs, o, n) -> { updateRefreshState.run(); if (!refresh.isDisabled() && doRefreshHolder[0] != null) doRefreshHolder[0].run(); });
        monthSelector.valueProperty().addListener((obs, o, n) -> { if (!refresh.isDisabled() && doRefreshHolder[0] != null) doRefreshHolder[0].run(); });
        yearSelector.valueProperty().addListener((obs, o, n) -> { if (!refresh.isDisabled() && doRefreshHolder[0] != null) doRefreshHolder[0].run(); });

        doRefreshHolder[0] = () -> {
            // clear previous content to prevent duplication
            chart.getData().clear();
            chartHolder.getChildren().clear();

            String course = courseFilter.getValue();
            String yearLevel = yearFilter.getValue();
            Integer classId = null;
            String classVal = classFilter.getValue();
            if (classVal != null && !classVal.isEmpty()) {
                try { classId = Integer.parseInt(classVal.split(":" )[0].trim()); } catch (Exception ignore) { classId = null; }
            }
            String selectedMonth = monthSelector.getValue();

            // Build sessionId -> session_date map for this class so we bucket by session_date
            // (reliable date chosen by the teacher, not the record creation timestamp)
            final Map<Integer, String> sessionDateMap = new HashMap<>();
            try {
                for (model.AttendanceSession s : sessionDAO.getAllSessions()) {
                    if (classId == null || s.getClassId() == classId) {
                        sessionDateMap.put(s.getId(), s.getSessionDate());
                    }
                }
            } catch (Exception ex2) { System.out.println("sessionDateMap error: " + ex2.getMessage()); }

            List<model.AttendanceRecord> records = recordDAO.getAttendanceReport(null, classId, course, yearLevel, null);

            // bucket -> [present, late, absent]
            Map<String, int[]> totals = new HashMap<>();
            List<String> bucketsOrder = new ArrayList<>();

            int selectedYear = thisYear;
            try { selectedYear = Integer.parseInt(yearSelector.getValue()); } catch (Exception ignore) {}
            boolean isSpecificMonth = selectedMonth != null && !selectedMonth.equals("All Months");
            Integer specificMonthIndex = null;
            if (isSpecificMonth) {
                for (int i = 0; i < MONTH_NAMES.length; i++) if (MONTH_NAMES[i].equals(selectedMonth)) { specificMonthIndex = i+1; break; }
            }

            final String[] WEEK_NAMES = new String[]{"1st Week","2nd Week","3rd Week"};
            final int finalSelectedYear = selectedYear;
            final Integer finalSpecificMonthIndex = specificMonthIndex;

            if (isSpecificMonth) {
                for (String w : WEEK_NAMES) totals.putIfAbsent(w, new int[]{0,0,0});
                for (model.AttendanceRecord r : records) {
                    // Use session_date for bucketing
                    String sdate = sessionDateMap.get(r.getSessionId());
                    if (sdate == null) continue;
                    java.time.LocalDate sd;
                    try { sd = java.time.LocalDate.parse(sdate.substring(0, 10)); } catch (Exception ex) { continue; }
                    if (sd.getYear() != finalSelectedYear) continue;
                    if (finalSpecificMonthIndex == null || sd.getMonthValue() != finalSpecificMonthIndex) continue;
                    int day = sd.getDayOfMonth();
                    String bucketKey;
                    if (day <= 7) bucketKey = WEEK_NAMES[0];
                    else if (day <= 14) bucketKey = WEEK_NAMES[1];
                    else bucketKey = WEEK_NAMES[2];
                    totals.putIfAbsent(bucketKey, new int[]{0,0,0});
                    int[] counts = totals.get(bucketKey);
                    String status = r.getStatus() == null ? "" : r.getStatus().trim();
                    if ("Present".equalsIgnoreCase(status)) counts[0]++;
                    else if ("Late".equalsIgnoreCase(status)) counts[1]++;
                    else if ("Absent".equalsIgnoreCase(status)) counts[2]++;
                }
                bucketsOrder.addAll(Arrays.asList(WEEK_NAMES));
            } else {
                for (model.AttendanceRecord r : records) {
                    String sdate = sessionDateMap.get(r.getSessionId());
                    if (sdate == null) continue;
                    java.time.LocalDate sd;
                    try { sd = java.time.LocalDate.parse(sdate.substring(0, 10)); } catch (Exception ex) { continue; }
                    if (sd.getYear() != finalSelectedYear) continue;
                    String bucketKey = MONTH_NAMES[sd.getMonthValue() - 1];
                    totals.putIfAbsent(bucketKey, new int[]{0,0,0});
                    int[] counts = totals.get(bucketKey);
                    String status = r.getStatus() == null ? "" : r.getStatus().trim();
                    if ("Present".equalsIgnoreCase(status)) counts[0]++;
                    else if ("Late".equalsIgnoreCase(status)) counts[1]++;
                    else if ("Absent".equalsIgnoreCase(status)) counts[2]++;
                }
                bucketsOrder.addAll(Arrays.asList(MONTH_NAMES));
            }

            xAxis.setCategories(FXCollections.observableArrayList(bucketsOrder));

            XYChart.Series<String, Number> presentSeries = new XYChart.Series<>(); presentSeries.setName("Present");
            XYChart.Series<String, Number> lateSeries = new XYChart.Series<>(); lateSeries.setName("Late");
            XYChart.Series<String, Number> absentSeries = new XYChart.Series<>(); absentSeries.setName("Absent");

            final int[] totalsOverall = new int[3];

            for (String bkey : bucketsOrder) {
                int p = 0, l = 0, a = 0;
                if (totals.containsKey(bkey)) { int[] arr = totals.get(bkey); p = arr[0]; l = arr[1]; a = arr[2]; }
                int total = p + l + a;
                totalsOverall[0] += p; totalsOverall[1] += l; totalsOverall[2] += a;
                double pPct = (total == 0) ? 0.0 : ((double) p / (double) total) * 100.0;
                double lPct = (total == 0) ? 0.0 : ((double) l / (double) total) * 100.0;
                double aPct = (total == 0) ? 0.0 : ((double) a / (double) total) * 100.0;
                presentSeries.getData().add(new XYChart.Data<>(bkey, pPct));
                lateSeries.getData().add(new XYChart.Data<>(bkey, lPct));
                absentSeries.getData().add(new XYChart.Data<>(bkey, aPct));
            }

            chart.getData().addAll(presentSeries, lateSeries, absentSeries);
            // once data is added, show legend so users can identify the lines
            chart.setLegendVisible(true);

            // summary below chart
            HBox summaryRow = new HBox(18);
            summaryRow.setPadding(new Insets(6,0,0,0));
            summaryRow.setAlignment(Pos.CENTER_LEFT);

            VBox vPresent = new VBox(4);
            Label lblPTitle = new Label("Present"); lblPTitle.setStyle("-fx-font-weight: bold;");
            Label lblPCount = new Label(String.valueOf(totalsOverall[0])); lblPCount.setStyle("-fx-font-size: 16; -fx-text-fill: #16a34a;");
            vPresent.getChildren().addAll(lblPTitle, lblPCount);

            VBox vLate = new VBox(4);
            Label lblLTitle = new Label("Late"); lblLTitle.setStyle("-fx-font-weight: bold;");
            Label lblLCount = new Label(String.valueOf(totalsOverall[1])); lblLCount.setStyle("-fx-font-size: 16; -fx-text-fill: #d97706;");
            vLate.getChildren().addAll(lblLTitle, lblLCount);

            VBox vAbsent = new VBox(4);
            Label lblATitle = new Label("Absent"); lblATitle.setStyle("-fx-font-weight: bold;");
            Label lblACount = new Label(String.valueOf(totalsOverall[2])); lblACount.setStyle("-fx-font-size: 16; -fx-text-fill: #dc2626;");
            vAbsent.getChildren().addAll(lblATitle, lblACount);

            summaryRow.getChildren().addAll(vPresent, vLate, vAbsent);

            Platform.runLater(() -> {
                try {
                    Node pNode = presentSeries.getNode(); if (pNode != null) pNode.setStyle("-fx-stroke: #16a34a; -fx-stroke-width: 2px;");
                    Node lNode = lateSeries.getNode(); if (lNode != null) lNode.setStyle("-fx-stroke: #d97706; -fx-stroke-width: 2px;");
                    Node aNode = absentSeries.getNode(); if (aNode != null) aNode.setStyle("-fx-stroke: #dc2626; -fx-stroke-width: 2px;");

                    for (XYChart.Series<String, Number> srs : Arrays.asList(presentSeries, lateSeries, absentSeries)) {
                        String name = srs.getName();
                        for (XYChart.Data<String, Number> d : srs.getData()) {
                            Node node = d.getNode();
                            if (node != null) {
                                String color = "#000000";
                                if ("Present".equalsIgnoreCase(name)) color = "#16a34a";
                                else if ("Late".equalsIgnoreCase(name)) color = "#d97706";
                                else if ("Absent".equalsIgnoreCase(name)) color = "#dc2626";
                                node.setStyle(String.format("-fx-background-color: %s, white;", color));
                                String period = (String) d.getXValue();
                                int p = 0, l = 0, a = 0;
                                if (totals.containsKey(period)) { int[] arr = totals.get(period); p = arr[0]; l = arr[1]; a = arr[2]; }
                                int total = p + l + a;
                                String tip = "Period: " + period + "\nTotal: " + total + "\nPresent: " + p + "\nLate: " + l + "\nAbsent: " + a + "\n" + name + ": " + String.format("%.1f%%", d.getYValue().doubleValue());
                                Tooltip.install(node, new Tooltip(tip));
                            }
                        }
                    }

                    lblPCount.setText(String.valueOf(totalsOverall[0]));
                    lblLCount.setText(String.valueOf(totalsOverall[1]));
                    lblACount.setText(String.valueOf(totalsOverall[2]));
                } catch (Exception ex) { System.out.println("Chart styling error: " + ex.getMessage()); }
            });

            chartHolder.getChildren().addAll(chart, summaryRow);
            VBox.setVgrow(chart, Priority.ALWAYS);
            if (!box.getChildren().contains(chartHolder)) box.getChildren().add(chartHolder);
         };

        refresh.setOnAction(e -> { if (doRefreshHolder[0] != null) doRefreshHolder[0].run(); });

        // initial layout: header + chartHolder placeholder
        box.getChildren().addAll(header, chartHolder);
        return box;
    }

    private void updateBadgeCount() {
        try {
            java.util.ArrayList<model.AttendanceRecord> low = recordDAO.getLowAttendanceStudents(3);
            if (low != null && low.size() > 0) {
                notificationBadge.setText(String.valueOf(low.size()));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        } catch (Exception ex) {
            // ignore
        }
    }
}
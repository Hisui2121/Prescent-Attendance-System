Presentation Details — Frontend (Taglish + Technical)

Context
- Defense tomorrow. Your part: frontend (JavaFX) of the Prescent Attendance Management System.
- Files created: presentation_slides.md (slide content), presentation_script.md (speaker script). This file contains the detailed information you can use for Q&A or deep-dive during the defense.

1) High-level summary
- Frontend stack: Java (JavaFX) UI, organized into classes under src/ui. Each screen is a class returning a Scene or Pane.
- Data access: DAOs in src/dao call database.DBConnect to open SQLite connections. UI delegates persistence to DAOs.
- Services: AuthenticationService is a singleton that provides login, currentUser and permission checks.
- Reports: new report layer (src/report) with ReportEngine interface and JasperReportEngine implementation. ReportGenerator has static wrappers for backward compatibility.
- Utilities: util.EventBus used as observer for cross-component updates (notifications, session changes).

2) File-by-file frontend reference (useful for code citations)
- src/ui/LoginUI.java
  - Key method: getScene(Main mainApp)
  - Event: btnLogin.setOnAction -> AuthenticationService.getInstance().login(user, pass)
  - On success: mainApp.loadDashboard()

- src/ui/DashboardUI.java
  - Key methods: getScene(), setActiveBtn(Button), switchContent(Pane), getStatsView(), createAttendanceChartPane()
  - Uses StudentDAO, ClassDAO, AttendanceSessionDAO, AttendanceRecordDAO to compute stats and populate chart filters.
  - EventBus.register: EventBus.addNotificationChangeListener(() -> Platform.runLater(() -> updateBadgeCount()))
  - Keeps instances: private AttendanceUI attendanceUI = new AttendanceUI(); private ReportUI reportUI = new ReportUI(); (stateful UIs)

- src/ui/StudentUI.java
  - Typical methods: getView(), loadStudents(), showAddEditDialog(Student)
  - DAO calls: studentDAO.getAllStudents(), studentDAO.addStudent(Student), updateStudent/deleteStudent
  - UI: TableView<Student> backed by ObservableList for efficient updates

- src/ui/AttendanceUI.java
  - Purpose: create sessions, generate attendance sheets, mark attendance
  - Key flows: sessionDAO.createSession(...), recordDAO.generateAttendanceSheet(sessionId,...), recordDAO.updateAttendanceStatus(sessionId, studentId, status)
  - UI state: currentSessionId, sessionActive; preserved when Dashboard keeps a single instance

- src/ui/ReportUI.java
  - Filters and tables for attendance and masterlist
  - Exports: calls ReportGenerator.generateAttendancePDF(...) and ReportGenerator.generateStudentPDF(...)
  - Note: ReportGenerator now uses ReportEngine/JasperReportEngine; static wrappers keep existing UI invocations working.

- src/ui/NotificationUI.java
  - Loads low-attendance notifications via recordDAO.getLowAttendanceStudents()
  - Dismiss action calls recordDAO.clearAbsenceNotifications(...) and EventBus.fireNotificationChanged()

- src/ui/SystemUserUI.java, TeacherUI.java, ClassUI.java, EnrollmentUI.java
  - CRUD UIs for users, teachers, classes and enrollments. Use respective DAOs (UserDAO, TeacherDAO, ClassDAO, EnrollmentDAO).
  - Teacher and Student models extend Person; useful OOP example for polymorphism in StudentDAO.mapRow.

3) OOP patterns and where to point to them during defense
- Inheritance: model.Person (abstract) -> model.Student, model.Teacher. Cite model/Student.java getPersonId() override.
- Polymorphism: StudentDAO.mapRow returns Person but creates Student; DAOs use getPersonId() polymorphically.
- Encapsulation: models expose only getters/setters and domain methods like getDisplayInfo().
- Composition: DashboardUI contains other UI objects rather than inheriting from them; preserves state for AttendanceUI and ReportUI.
- Observer pattern: util.EventBus provides addNotificationChangeListener and fireNotificationChanged — cite DashboardUI and NotificationUI usage.
- Singleton: AuthenticationService.getInstance() — central session state and permission checks.

4) Data flow (detailed example)
- Example: Generate attendance PDF from ReportUI
  1. User selects session and clicks Export.
  2. ReportUI collects filters and queries AttendanceRecordDAO.getAttendanceReportBySessionId(...).
  3. If records missing, ReportUI may call AttendanceRecordDAO.generateAttendanceSheet(sessionId, classId, course, year).
  4. When records exist, ReportUI opens FileChooser and calls ReportGenerator.generateAttendancePDF(rows, out,...).
  5. ReportGenerator static wrapper creates JasperReportEngine and calls JasperReportEngine.generatePDF(template, rows, params, out).
  6. JasperReportEngine compiles the template, fills it with JRBeanCollectionDataSource(rows), and exports PDF.

5) Report templates and compatibility
- Current workspace templates: reports/attendance_report.xml and reports/student_list_report.xml.
- JasperReportEngine currently compiles the template at runtime; it accepts .jrxml or .xml if templates are valid JRXML content.
- Recommendation: precompile templates to .jasper or place them on classpath and load via getResourceAsStream for reliable packaging.

6) Common defense questions & short answers (Taglish)
- Q: "Where is business logic?"
  A: "Services package for app rules; DAOs for persistence; UI only handles presentation and input validation."
- Q: "How do you avoid UI freeze during DB calls?"
  A: "Currently many calls run on JavaFX thread; recommended fix is to use JavaFX Task/Service for background DB reads and Platform.runLater for UI updates."
- Q: "Why the ReportEngine abstraction?"
  A: "Separation of concerns — ReportGenerator delegates to an engine interface to allow swapping implementations or testing."
- Q: "How are notifications handled?"
  A: "util.EventBus implements observer pattern; components subscribe and get notified via fireNotificationChanged()."

7) Run & demo checklist (practical)
- Make sure lib/ contains jasperreports and sqlite-jdbc (present in workspace lib/).
- Check reports/ templates exist (attendance_report.xml, student_list_report.xml).
- In Eclipse: Project → Clean, then Run app.Main.
- Login with admin/test account (use credentials in DB or sample users).
- Walkthrough: Dashboard → Student Directory (add student) → Attendance (create session) → Report (export PDF) → Notifications.

8) Troubleshooting common issues
- "EventBus cannot be resolved" errors: ensure src/util/EventBus.java exists and package declared as util; then Project Clean in IDE.
- "ReportGenerator method errors": ensure ReportGenerator.java was updated and static wrappers exist (this workspace already updated).
- JasperReports runtime errors: check template path and that template is valid JRXML; ensure lib/jasperreports-*.jar is on classpath.

9) Appendix: quick code references to show during defense
- Login button handler: src/ui/LoginUI.java — btnLogin.setOnAction
- Student mapping & polymorphism: src/dao/StudentDAO.java — mapRow(ResultSet rs)
- Model override example: src/model/Student.java — getPersonId(), getDisplayInfo()
- Event bus registration: src/ui/DashboardUI.java — EventBus.addNotificationChangeListener(...)
- Report engine: src/report/JasperReportEngine.java — generatePDF(...)

End of details. Use this document for deep-dive Q&A and for copy-pasting exact file references during your defense.

Good luck on your defense tomorrow.
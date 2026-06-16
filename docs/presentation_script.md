Presentation Script — Frontend (Taglish)

Slide 1 — Title
- "Magandang araw. Ako si [Your Name]. Ang part ko ngayong defense ay ang frontend ng Prescent Attendance Management System."

Slide 2 — Agenda
- "Ito ang agenda: mabilis na app overview, frontend architecture, key screens, OOP concepts na ginamit, data flow, demo plan, at improvements."

Slide 3 — App overview
- "Ang app ay isang desktop JavaFX application para sa attendance management. Frontend code nasa src/ui. Backend persistence gamit ang SQLite at DAOs sa src/dao."

Slide 4 — Frontend architecture
- "Ipakita ko ang layers: UI, services, DAOs, models, utilities. UI classes mga individual screen classes na nagre-return ng Scene/Pane. Services tulad ng AuthenticationService ang nagho-handle ng session & permissions. DAOs ang nagha-handle ng SQL at mapping sa model objects. Utilities gaya ng EventBus para sa decoupled notifications."

Slide 5 — UI class structure
- "Bawat UI class nagbuo ng JavaFX layout (VBox/HBox/BorderPane). May getScene() o getView() method. DashboardUI ang parent shell at nagco-compose ng sub-UIs. AttendanceUI at ReportUI ay sinasave bilang instance fields para preserve state kapag nag-browse."

Slide 6 — Login flow
- "Ipakita ang LoginUI code handler: btnLogin.setOnAction calls AuthenticationService.getInstance().login(user, pass). Sa success, Main.loadDashboard() ang pinapasa. Ito magandang example ng separation of concerns — UI delegates authentication to service."

Slide 7 — Dashboard
- "DashboardUI: sidebar navigation, topbar na may notification bell at Logout. Notification badge galing sa AttendanceRecordDAO.getLowAttendanceStudents(...) at nare-refresh via EventBus listeners. Chart pane uses DAOs to populate LineChart for attendance trends."

Slide 8 — Important UIs
- "StudentUI: TableView, CRUD operations through StudentDAO.
  AttendanceUI: create sessions, generate sheets, mark present/absent via AttendanceSessionDAO and AttendanceRecordDAO.
  ReportUI: filters + export via ReportGenerator/JasperReportEngine.
  NotificationUI: low-attendance alerts and dismiss actions.
  SystemUserUI: user and role management."

Slide 9 — OOP concepts mapping
- "Inheritance: Person <- Student/Teacher.
  Polymorphism: mapRow returns Person but actually creates Student.
  Encapsulation: private fields + getters/setters in models.
  Composition: DashboardUI composes other UIs.
  Observer: EventBus for decoupled updates."

Slide 10 — Data flow example
- "Practical flow: LoginUI -> AuthenticationService -> UserDAO -> Main -> DashboardUI -> StudentUI -> StudentDAO -> DBConnect.connect() -> SQLite. UI renders TableView from returned Student objects."

Slide 11 — Reports
- "I-refactor ang reports: ReportGenerator delegates to ReportEngine; JasperReportEngine implements engine. UI calls ReportGenerator.generateAttendancePDF or generateStudentPDF (static wrappers kept for backward compatibility). Templates in reports/ (student_list_report.xml, attendance_report.xml)."

Slide 12 — Demo plan
- "1) Login
  2) Dashboard overview (stats, notification badge)
  3) Student Directory: add a student
  4) Attendance: create session, mark attendance
  5) Generate attendance PDF and masterlist PDF
  6) Show NotificationUI and badge update"

Slide 13 — Limitations & improvements
- "Move heavy DB ops to JavaFX Task.
  Introduce DAO interfaces for DI and testing.
  Precompile Jasper templates to .jasper and load via classpath.
  Improve validation and logging."

Slide 14 — Closing
- "Frontend is OOP-driven, modular, and integrates through DAOs and services. Ready for questions."

Speaker tips (brief)
- "Keep code snippets short on slides; reference full files in docs. During demo, narrate the OOP concept you’re showing (inheritance/polymorphism/eventbus)."
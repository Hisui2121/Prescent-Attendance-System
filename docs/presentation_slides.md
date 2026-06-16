Presentation Slides — Frontend (Taglish)

Slide 1 — Title
- Title: "Frontend — Prescent Attendance Management System"
- Subtitle: "Object-Oriented Programming (OOP) defense — Frontend component"
- Presenter: [Your Name]
- Date: Tomorrow

Slide 2 — Agenda
- Quick app overview
- Frontend architecture (layers)
- Key UI screens & flows
- OOP concepts in the frontend
- Data flow & DB integration
- Demo plan (what I'll show)
- Limitations & improvements
- Q&A

Slide 3 — App overview (1 slide)
- Desktop JavaFX app that manages students, classes, attendance, reports.
- Frontend: src/ui — each screen is a class returning a Scene/Pane.
- Backend/DB: SQLite via dao package and database.DBConnect.

Slide 4 — Frontend architecture (diagram bullets)
- UI layer: src/ui/*.java (LoginUI, DashboardUI, StudentUI, AttendanceUI, ReportUI, NotificationUI, etc.)
- Services: AuthenticationService (singleton) — session & permission checks
- DAOs: dao/*.java — StudentDAO, AttendanceRecordDAO, AttendanceSessionDAO, ClassDAO
- Models: model/*.java — Person, Student, Teacher, AttendanceSession, AttendanceRecord
- Utilities: util.EventBus (observer), util.Logger

Slide 5 — How UI classes are structured
- Each UI class builds a JavaFX layout (BorderPane/VBox/StackPane)
- Public method: getScene() or getView() returns the UI node
- Use composition: DashboardUI composes StudentUI, AttendanceUI, ReportUI
- Keep stateful UIs as instance fields (attendanceUI preserved in Dashboard)

Slide 6 — Login flow (visual)
- LoginUI collects credentials
- Calls AuthenticationService.getInstance().login(user, pass)
- On success: Main.loadDashboard(); on fail: Alert
- OOP: separation of concerns — UI delegates auth

Slide 7 — Dashboard (main features)
- Sidebar navigation, notification bell with badge
- Content area swaps views via switchContent(Pane)
- Stats aggregated via DAOs
- Charting (LineChart) for attendance trends
- Uses EventBus to update notification badge

Slide 8 — Important UIs (one-liners)
- StudentUI: TableView, add/edit/delete, StudentDAO
- AttendanceUI: create sessions, generate sheets, mark presence, AttendanceRecordDAO
- ReportUI: filters + export (JasperReports via ReportGenerator)
- NotificationUI: low-attendance alerts, dismiss actions
- SystemUserUI: manage users, roles, permissions

Slide 9 — OOP concepts mapping
- Inheritance: Person <- Student, Teacher
- Polymorphism: DAO.mapRow returns Person, uses getPersonId()
- Encapsulation: models with private fields + getters/setters
- Composition: DashboardUI contains sub-UIs
- Observer: EventBus for loose coupling

Slide 10 — Data flow example (Login → Student List)
- LoginUI -> AuthenticationService -> UserDAO
- Main -> DashboardUI -> new StudentUI().getView()
- StudentUI -> StudentDAO.getAllStudents() -> DBConnect.connect()
- UI populates TableView with model.Student objects

Slide 11 — Reports & JasperReports (overview)
- ReportGenerator delegates to ReportEngine (JasperReportEngine)
- Templates: reports/student_list_report.xml and attendance_report.xml
- UI triggers ReportGenerator.generate... which compiles and exports PDF

Slide 12 — Demo plan (concise)
- Show LoginUI -> Dashboard
- Open Student Directory, add student
- Create attendance session, mark a few students
- Generate attendance PDF and masterlist PDF
- Show notification badge update and NotificationUI

Slide 13 — Limitations & improvements
- Move heavy DB ops to JavaFX Task/Service
- Introduce DAO interfaces for testability
- Precompile Jasper templates (.jasper) and load via classpath
- Add better validation & logging

Slide 14 — Closing / Key takeaways
- UI is modular, OOP-driven, and data-backed via DAOs
- EventBus decouples components cleanly
- Report system refactored to use engine interface

Slide 15 — Q&A
- Ready to answer implementation and design questions

---
Notes: Use this file to create slides. Each slide above is 1 slide content block.
package ui;

import java.util.ArrayList;

import java.util.Scanner;

import dao.StudentDAO;
import model.Student;

import dao.LessonDAO;
import model.Lesson;

import dao.EnrollmentDAO;
import model.Enrollment;

import dao.ClassDAO;
import model.Class;

import dao.AttendanceRecordDAO;
import dao.AttendanceSessionDAO;
import model.AttendanceSession;
import model.AttendanceRecord;
import report.ReportGenerator;

import java.sql.Connection;

import report.JasperReportEngine;

public class ConsoleUI {

    private Scanner scanner;
    private StudentDAO studentDAO;
    
    private LessonDAO lessonDAO;
    
    private EnrollmentDAO enrollmentDAO;
    
    private ClassDAO classDAO;
    private final ReportGenerator reportGenerator = new ReportGenerator(new JasperReportEngine());

    // CONSTRUCTOR
    public ConsoleUI() {

        scanner = new Scanner(System.in);

        studentDAO = new StudentDAO();
        
        lessonDAO = new LessonDAO();
        
        enrollmentDAO = new EnrollmentDAO();
        
        classDAO = new ClassDAO();
    }

    // START SYSTEM
    public void start() {

        showAdminMenu();
    }

    // =========================
    // ADMIN MENU
    // =========================
    private void showAdminMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n=====> ADMIN MENU <=====");

            System.out.println("1. Student Management");
            System.out.println("2. Class Management");
            System.out.println("3. Attendance Management");
            System.out.println("4. Enrollment Mangement");
            System.out.println("5. Reports");
            System.out.println("6. Exit");

            System.out.print("Enter Choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    studentManagementMenu();
                    break;

                case 2:
                    classManagementMenu();
                    break;
                    
                case 3:
                    attendanceMenu();
                    break;
                 
                case 4:
                    enrollmentManagementMenu();
                    break;
                    
                case 5:
                    reportsMenu();
                    break;

                case 6:
                    System.out.println("Exiting System...");
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // =========================
    // STUDENT MANAGEMENT MENU
    // =========================
    private void studentManagementMenu() {

        boolean running = true;

        while (running) {

            System.out.println(
                "\n===== STUDENT MANAGEMENT =====");

            System.out.println("1. Add Student");
            System.out.println("2. View Students");
            System.out.println("3. Update Student");
            System.out.println("4. Delete Student");
            System.out.println("5. Back");

            System.out.print("Enter Choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    addStudent();
                    break;

                case 2:
                    viewStudents();
                    break;

                case 3:
                    updateStudent();
                    break;

                case 4:
                    deleteStudent();
                    break;

                case 5:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    // =========================
    // ADD STUDENT
    // =========================
    private void addStudent() {

        System.out.println("\n=== ADD STUDENT ===");

        System.out.print("Student ID: ");
        String studentId = scanner.nextLine();

        System.out.print("Full Name: ");
        String name = scanner.nextLine();

        System.out.print("Course: ");
        String course = scanner.nextLine();

        System.out.print("Year Level: ");
        String year = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        Student student = new Student(
                studentId,
                name,
                course,
                year,
                email
        );

        boolean success =
                studentDAO.addStudent(student);

        if (success) {

            System.out.println(
                "Student added successfully!");

        } else {

            System.out.println(
                "Failed to add student!");
        }
    }

    // =========================
    // VIEW STUDENTS
    // =========================
    private void viewStudents() {

        System.out.println("\n=== STUDENT LIST ===");

        ArrayList<Student> students =
                studentDAO.getAllStudents();

        if (students.isEmpty()) {

            System.out.println("No students found.");

            return;
        }

        for (Student s : students) {

            System.out.println(
                    s.getId() + " | "
                  + s.getStudentId() + " | "
                  + s.getFullName() + " | "
                  + s.getCourse() + " | "
                  + s.getYearLevel() + " | "
                  + s.getEmail()
            );
        }
    }

    // =========================
    // UPDATE STUDENT
    // =========================
    private void updateStudent() {

        System.out.println("\n=== UPDATE STUDENT ===");

        System.out.print("Enter Student ID: ");
        String studentId = scanner.nextLine();

        System.out.print("New Full Name: ");
        String name = scanner.nextLine();

        System.out.print("New Course: ");
        String course = scanner.nextLine();

        System.out.print("New Year Level: ");
        String year = scanner.nextLine();

        System.out.print("New Email: ");
        String email = scanner.nextLine();

        Student student = new Student(
                studentId,
                name,
                course,
                year,
                email
        );

        boolean success =
                studentDAO.updateStudent(student);

        if (success) {

            System.out.println("Student updated!");

        } else {

            System.out.println("Update failed!");
        }
    }

    // =========================
    // DELETE STUDENT
    // =========================
    private void deleteStudent() {

        System.out.println("\n=== DELETE STUDENT ===");

        System.out.print("Enter Student ID: ");

        String studentId = scanner.nextLine();

        boolean success =
                studentDAO.deleteStudent(studentId);

        if (success) {

            System.out.println("Student deleted!");

        } else {

            System.out.println("Delete failed!");
        }
    }
    
    private void lessonManagementMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n===== LESSON MANAGEMENT =====");

            System.out.println("1. Add Lesson");
            System.out.println("2. View Lessons");
            System.out.println("3. Delete Lesson");
            System.out.println("4. Back");

            System.out.print("Enter Choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    addLesson();
                    break;

                case 2:
                    viewLessons();
                    break;

                case 3:
                    deleteLesson();
                    break;

                case 4:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
    
    private void addLesson() {

        System.out.println("\n=== ADD LESSON ===");

        System.out.print("Lesson Code: ");
        String code = scanner.nextLine();

        System.out.print("Lesson Name: ");
        String name = scanner.nextLine();

        System.out.print("Teacher ID: ");
        String teacherId = scanner.nextLine();

        System.out.print("Schedule: ");
        String schedule = scanner.nextLine();

        System.out.print("Room: ");
        String room = scanner.nextLine();

        Lesson lesson = new Lesson(
            code, name, teacherId, schedule, room
        );

        boolean success = lessonDAO.addLesson(lesson);

        if (success) {
            System.out.println("Lesson added!");
        } else {
            System.out.println("Failed to add lesson!");
        }
    }
    private void viewLessons() {

        System.out.println("\n=== LESSON LIST ===");

        ArrayList<Lesson> lessons =
                lessonDAO.getAllLessons();

        if (lessons.isEmpty()) {
            System.out.println("No lessons found.");
            return;
        }

        for (Lesson l : lessons) {

            System.out.println(
                    l.getId() + " | "
                  + l.getLessonCode() + " | "
                  + l.getLessonName() + " | "
                  + l.getTeacherId() + " | "
                  + l.getSchedule() + " | "
                  + l.getRoom()
            );
        }
    }
    private void deleteLesson() {

        System.out.println("\n=== DELETE LESSON ===");

        System.out.print("Enter Lesson ID: ");
        int id = scanner.nextInt();

        boolean success = lessonDAO.deleteLesson(id);

        if (success) {
            System.out.println("Lesson deleted!");
        } else {
            System.out.println("Delete failed!");
        }
    }
    
    private void enrollmentManagementMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n===== ENROLLMENT MANAGEMENT =====");

            System.out.println("1. Enroll Student");
            System.out.println("2. View Enrollments");
            System.out.println("3. Remove Enrollment");
            System.out.println("4. Back");

            System.out.print("Enter Choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    enrollStudent();
                    break;

                case 2:
                    viewEnrollments();
                    break;

                case 3:
                    removeEnrollment();
                    break;

                case 4:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
    
    private void enrollStudent() {

        System.out.println("\n=== ENROLL STUDENT ===");

        System.out.print("Student ID: ");
        String studentId = scanner.nextLine();

        System.out.print("Class Code: ");
        String classCode = scanner.nextLine();

        int classId =
                classDAO.getClassIdByCode(classCode);

        if (classId == -1) {

            System.out.println("Class not found!");
            return;
        }

        Enrollment enrollment =
                new Enrollment(studentId, classId);

        boolean success =
                enrollmentDAO.enrollStudent(enrollment);

        if (success) {
            System.out.println("Enrollment successful!");
        } else {
            System.out.println("Enrollment failed!");
        }
    }
    
    private void viewEnrollments() {

        System.out.println("\n=== ENROLLMENT LIST ===");

        ArrayList<Enrollment> list =
                enrollmentDAO.getAllEnrollments();

        if (list.isEmpty()) {

            System.out.println("No enrollments found.");
            return;
        }

        for (Enrollment e : list) {

            System.out.println(
                    e.getId() + " | "
                  + e.getStudentId() + " | "
                  + e.getClassId()
            );
        }
    }
    
    private void removeEnrollment() {

        System.out.println("\n=== REMOVE ENROLLMENT ===");

        System.out.print("Enter Enrollment ID: ");
        int id = scanner.nextInt();

        boolean success =
                enrollmentDAO.removeEnrollment(id);

        if (success) {
            System.out.println("Removed!");
        } else {
            System.out.println("Failed!");
        }
    }
    private void classManagementMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n===== CLASS MANAGEMENT =====");

            System.out.println("1. Add Class");
            System.out.println("2. View Classes");
            System.out.println("3. Delete Class");
            System.out.println("4. Back");

            System.out.print("Enter Choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    addClass();
                    break;

                case 2:
                    viewClasses();
                    break;

                case 3:
                    deleteClass();
                    break;

                case 4:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
    
    private void addClass() {

        System.out.println("\n=== ADD CLASS ===");

        System.out.print("Class Code (e.g. COMP-003): ");
        String code = scanner.nextLine();

        System.out.print("Class Name: ");
        String name = scanner.nextLine();

        System.out.print("Teacher ID: ");
        String teacherId = scanner.nextLine();

        System.out.print("Schedule: ");
        String schedule = scanner.nextLine();

        System.out.print("Room: ");
        String room = scanner.nextLine();

        Class c = new Class(
            code, name, teacherId, schedule, room
        );

        boolean success = classDAO.addClass(c);

        if (success) {
            System.out.println("Class added!");
        } else {
            System.out.println("Failed to add class!");
        }
    }
    
    private void viewClasses() {

        System.out.println("\n=== CLASS LIST ===");

        ArrayList<Class> list =
                classDAO.getAllClasses();

        if (list.isEmpty()) {
            System.out.println("No classes found.");
            return;
        }

        for (Class c : list) {

            System.out.println(
                    c.getId() + " | "
                  + c.getClassCode() + " | "
                  + c.getClassName() + " | "
                  + c.getTeacherId() + " | "
                  + c.getSchedule() + " | "
                  + c.getRoom()
            );
        }
    }
    
    private void deleteClass() {

        System.out.println("\n=== DELETE CLASS ===");

        System.out.print("Enter Class ID: ");
        int id = scanner.nextInt();

        boolean success = classDAO.deleteClass(id);

        if (success) {
            System.out.println("Class deleted!");
        } else {
            System.out.println("Delete failed!");
        }
    }
    
//    attendance menu
    
    private void attendanceMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n===== ATTENDANCE SYSTEM =====");

            System.out.println("1. Create Attendance Session");
            System.out.println("2. Mark Attendance");
            System.out.println("3. View Sessions");
            System.out.println("4. View Attendance Sheet");
            System.out.println("5. Exit");

            System.out.print("Enter Choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    createSession();
                    break;

                case 2:
                    markAttendanceAutomatically();
                    break;

                case 3:
                    viewSessions();
                    break;

                case 4:
                    viewAttendanceSheet();
                    break;

                case 5:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }
    
    private AttendanceSessionDAO sessionDAO =
            new AttendanceSessionDAO();

    private AttendanceRecordDAO recordDAO =
            new AttendanceRecordDAO();

    private void createSession() {

        System.out.println(
            "\n=== CREATE SESSION ==="
        );

        System.out.print("Class ID: ");
        int classId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Date (YYYY-MM-DD): ");
        String date = scanner.nextLine();

        System.out.print("Created By: ");
        String createdBy = scanner.nextLine();

        AttendanceSession session =
                new AttendanceSession(
                    classId,
                    date,
                    createdBy
                );

        // CREATE SESSION
        int sessionId =
                sessionDAO.createSession(session);

        if (sessionId != -1) {

            System.out.println(
                "Session created! ID: "
                + sessionId
            );

            // AUTO GENERATE ATTENDANCE SHEET
            boolean generated =
                recordDAO.generateAttendanceSheet(
                    sessionId,
                    classId
                );

            if (generated) {

                System.out.println(
                    "Attendance sheet generated!"
                );

            } else {

                System.out.println(
                    "Failed to generate attendance sheet!"
                );
            }

        } else {

            System.out.println(
                "Failed to create session!"
            );
        }
    }
    
    private void markAttendance() {

        System.out.println(
            "\n=== UPDATE ATTENDANCE ==="
        );

        System.out.print("Session ID: ");
        int sessionId = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Student ID: ");
        String studentId = scanner.nextLine();

        System.out.print(
            "Status (Present/Absent/Late): "
        );

        String status = scanner.nextLine();

        boolean success =
            recordDAO.updateAttendanceStatus(
                sessionId,
                studentId,
                status
            );

        if (success) {

            System.out.println(
                "Attendance updated!"
            );

        } else {

            System.out.println(
                "Failed to update!"
            );
        }
    }
    
    private void markAttendanceAutomatically() {

        System.out.println(
            "\n=== ATTENDANCE SHEET ==="
        );

        System.out.print("Enter Session ID: ");

        int sessionId = scanner.nextInt();
        scanner.nextLine();

        ArrayList<AttendanceRecord> records =
            recordDAO.getAttendanceSheet(sessionId);

        if (records.isEmpty()) {

            System.out.println(
                "No attendance records found!"
            );

            return;
        }

        // LOOP THROUGH STUDENTS
        for (AttendanceRecord record : records) {

            System.out.println(
                "\nStudent: "
                + record.getStudentId()
                + " | "
                + record.getFullName()
            );

            System.out.println(
                "Current Status: "
                + record.getStatus()
            );

            System.out.print(
                "New Status "
              + "(Present/Absent/Late): "
            );

            String status =
                    scanner.nextLine();

            boolean success =
                recordDAO.updateAttendanceStatus(
                    sessionId,
                    record.getStudentId(),
                    status
                );

            if (success) {

                System.out.println(
                    "Updated!"
                );

            } else {

                System.out.println(
                    "Failed!"
                );
            }
        }

        System.out.println(
            "\nAttendance completed!"
        );
    }
    
    private void viewSessions() {

        System.out.println("\n=== ATTENDANCE SESSIONS ===");

        ArrayList<AttendanceSession> list =
                sessionDAO.getAllSessions();

        for (AttendanceSession s : list) {

            System.out.println(
                    s.getId() + " | "
                  + s.getClassId() + " | "
                  + s.getSessionDate() + " | "
                  + s.getCreatedBy()
            );
        }
    }
    private void viewAttendanceSheet() {

        System.out.println(
            "\n=== VIEW ATTENDANCE SHEET ==="
        );

        System.out.print("Enter Session ID: ");

        int sessionId = scanner.nextInt();
        scanner.nextLine();

        recordDAO.viewAttendanceSheet(sessionId);
    }
    
	 // =========================
	 // REPORTS MENU
	 // =========================
	 private void reportsMenu() {
	
	     boolean running = true;
	
	     while (running) {
	
	         System.out.println("\n===== REPORTS =====");
	         System.out.println("1. Attendance Report by Session");
	         System.out.println("2. Student List Report");
	         System.out.println("3. Back");
	
	         System.out.print("Enter Choice: ");
	         int choice = scanner.nextInt();
	         scanner.nextLine();
	
	         switch (choice) {
	
	             case 1:
	                 generateAttendanceReport();
	                 break;
	
	             case 2:
	                 generateStudentReport();
	                 break;
	
	             case 3:
	                 running = false;
	                 break;
	
	             default:
	                 System.out.println("Invalid choice!");
	         }
	     }
	 }
	
	 private void generateAttendanceReport() {

		    System.out.print("Enter Session ID: ");
		    int sessionId = scanner.nextInt();
		    scanner.nextLine();

		    // Fetch session to get classId and date
		    String sessionSql =
		    	    "SELECT ats.session_date, c.class_name, t.full_name AS teacher_name, c.room, c.schedule " +
		    	    "FROM attendance_sessions ats " +
		    	    "JOIN classes c ON ats.class_id = c.id " +
		    	    "LEFT JOIN teachers t ON c.teacher_id = t.teacher_id " +
		    	    "WHERE ats.id = ?";
		    
		    String subjectName = "", sessionDate = "", professor = "", classroom = "", schedule = "";

		    try {
		        Connection conn = database.DBConnect.connect();
		        java.sql.PreparedStatement pstmt = conn.prepareStatement(sessionSql);
		        pstmt.setInt(1, sessionId);
		        java.sql.ResultSet rs = pstmt.executeQuery();

		        if (rs.next()) {
		            subjectName = rs.getString("class_name");
		            sessionDate = rs.getString("session_date");
		            professor = rs.getString("teacher_name") != null ? rs.getString("teacher_name") : "N/A";
		            classroom   = rs.getString("room");
		            schedule    = rs.getString("schedule");
		        } else {
		            System.out.println("Session not found.");
		            return;
		        }
		    } catch (Exception e) {
		        System.out.println("Failed to fetch session details: " + e.getMessage());
		        return;
		    }

		    ArrayList<AttendanceRecord> records = recordDAO.getAttendanceSheet(sessionId);

		    if (records.isEmpty()) {
		        System.out.println("No records found for that session.");
		        return;
		    }

		    try {
		        String outputPath = "attendance_session_" + sessionId + ".pdf";
		        reportGenerator.generateAttendancePDF(
		            records, outputPath,
		            subjectName, sessionDate, professor, classroom, schedule
		        );
		        System.out.println("Report saved: " + outputPath);
		    } catch (Exception e) {
		        System.out.println("Failed to generate report: " + e.getMessage());
		    }
		}
	
	 private void generateStudentReport() {
	
	     ArrayList<Student> students = studentDAO.getAllStudents();
	
	     if (students.isEmpty()) {
	         System.out.println("No students found.");
	         return;
	     }
	
	     try {
	         String outputPath = "student_list_report.pdf";
	         reportGenerator.generateStudentPDF(students, outputPath);
	         System.out.println("Report saved: " + outputPath);
	     } catch (Exception e) {
	         System.out.println("Failed to generate report: " + e.getMessage());
	     }
	 }
}
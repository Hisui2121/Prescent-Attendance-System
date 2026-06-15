package model;

public class AttendanceRecord {

    private int id;
    private int sessionId;
    private String studentId;
    private String fullName;
    private String status;
    private String timestamp;
    // Additional fields for reporting
    private String course;
    private String yearLevel;
    private String email;

    // New notification related fields
    private Integer classId;
    private String className;
    private String classCode;
    private String professorName;
    private String professorId;
    private Integer absentCount;
    private String alertType;

    public AttendanceRecord() {

    }

    public AttendanceRecord(int sessionId,
                            String studentId,
                            String status) {

        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // New reporting getters/setters
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getYearLevel() { return yearLevel; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Notification / class related getters/setters
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public String getProfessorName() { return professorName; }
    public void setProfessorName(String professorName) { this.professorName = professorName; }
    public String getProfessorId() { return professorId; }
    public void setProfessorId(String professorId) { this.professorId = professorId; }

    public Integer getAbsentCount() { return absentCount; }
    public void setAbsentCount(Integer absentCount) { this.absentCount = absentCount; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
}
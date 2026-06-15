package model;

public class Student {

    private int id;
    private String studentId;
    private String fullName;
    private String course;
    private String yearLevel;
    private String email;

    // EMPTY CONSTRUCTOR
    public Student() {

    }

    // CONSTRUCTOR WITHOUT DB ID
    public Student(String studentId,
                   String fullName,
                   String course,
                   String yearLevel,
                   String email) {

        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.email = email;
    }

    // FULL CONSTRUCTOR
    public Student(int id,
                   String studentId,
                   String fullName,
                   String course,
                   String yearLevel,
                   String email) {

        this.id = id;
        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.email = email;
    }

    // =========================
    // GETTERS & SETTERS
    // =========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(String yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        if (fullName != null && studentId != null) {
            return fullName + " (" + studentId + ")";
        }
        return studentId != null ? studentId : "";
    }
}
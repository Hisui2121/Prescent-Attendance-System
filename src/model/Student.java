package model;

public class Student extends Person {

    private int    id;
    private String studentId;
    private String course;
    private String yearLevel;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================
    public Student() {
        super();
    }

    /** Constructor without DB auto-increment id */
    public Student(String studentId,
                   String fullName,
                   String course,
                   String yearLevel,
                   String email) {
        super(fullName, email);   // calls Person(fullName, email)
        this.studentId = studentId;
        this.course    = course;
        this.yearLevel = yearLevel;
    }

    /** Full constructor */
    public Student(int id,
                   String studentId,
                   String fullName,
                   String course,
                   String yearLevel,
                   String email) {
        super(fullName, email);
        this.id        = id;
        this.studentId = studentId;
        this.course    = course;
        this.yearLevel = yearLevel;
    }

    // =========================================================
    // OVERRIDDEN ABSTRACT METHODS (Method Overriding)
    // =========================================================

   
    @Override
    public String getPersonId() {
        return studentId;
    }

  
    @Override
    public String getRole() {
        return "Student";
    }

  
    @Override
    public String getDisplayInfo() {
        return "[Student] " + fullName
             + " | ID: "         + studentId
             + " | Course: "     + course
             + " | Year: "       + yearLevel
             + " | Email: "      + email;
    }

   
    public int    getId()                      { return id; }
    public void   setId(int id)                { this.id = id; }

    public String getStudentId()               { return studentId; }
    public void   setStudentId(String sid)     { this.studentId = sid; }

    public String getCourse()                  { return course; }
    public void   setCourse(String course)     { this.course = course; }

    public String getYearLevel()               { return yearLevel; }
    public void   setYearLevel(String yl)      { this.yearLevel = yl; }

   
    @Override
    public String toString() {
        if (fullName != null && studentId != null)
            return fullName + " (" + studentId + ")";
        return studentId != null ? studentId : "";
    }
}
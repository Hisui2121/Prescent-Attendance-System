package model;

public class Teacher extends Person {

    private int    id;
    private String teacherId;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================
    public Teacher() {
        super();
    }

    public Teacher(int id, String teacherId, String fullName, String email) {
        super(fullName, email);   // calls Person(fullName, email)
        this.id        = id;
        this.teacherId = teacherId;
    }

    // =========================================================
    // OVERRIDDEN ABSTRACT METHODS (Method Overriding)
    // =========================================================

    @Override
    public String getPersonId() {
        return teacherId;
    }

    @Override
    public String getRole() {
        return "Teacher";
    }

   
    @Override
    public String getDisplayInfo() {
        return "[Teacher] " + fullName
             + " | Teacher ID: " + teacherId
             + " | Email: "      + email;
    }

    // =========================================================
    // TEACHER-SPECIFIC GETTERS & SETTERS
    // =========================================================
    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }

    public String getTeacherId()                 { return teacherId; }
    public void   setTeacherId(String tid)       { this.teacherId = tid; }

    // =========================================================
    // toString (method overriding)
    // =========================================================
    @Override
    public String toString() {
        if (fullName != null && teacherId != null)
            return fullName + " (" + teacherId + ")";
        return teacherId != null ? teacherId : "";
    }
}
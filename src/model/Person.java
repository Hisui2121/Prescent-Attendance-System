package model;

public abstract class Person {

    // =========================================================
    // SHARED FIELDS — inherited by Student, Teacher, and User
    // =========================================================
    protected String fullName;
    protected String email;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================
    public Person() {}

    public Person(String fullName, String email) {
        this.fullName = fullName;
        this.email    = email;
    }

    // =========================================================
    // CONCRETE GETTERS / SETTERS — shared by all subclasses
    // =========================================================
    public String getFullName()            { return fullName; }
    public void   setFullName(String name) { this.fullName = name; }

    public String getEmail()               { return email; }
    public void   setEmail(String email)   { this.email = email; }

   
    public abstract String getPersonId();
    public abstract String getRole();

    public String getDisplayInfo() {
        return "[" + getRole() + "] " + fullName
             + " | ID: " + getPersonId()
             + " | Email: " + email;
    }

    // =========================================================
    // toString
    // =========================================================
    @Override
    public String toString() {
        return fullName != null ? fullName : getPersonId();
    }
}
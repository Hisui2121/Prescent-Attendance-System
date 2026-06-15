package model;

public class User extends Person {

    private int    userId;
    private String username;
    private String password;
    private String role;      // "Admin", "Teacher", etc.

    // =========================================================
    // CONSTRUCTORS
    // =========================================================
    public User() {
        super();
    }

    public User(String username, String password, String role) {
        super();
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    // =========================================================
    // OVERRIDDEN ABSTRACT METHODS (Method Overriding)
    // =========================================================

   
    @Override
    public String getPersonId() {
        return username;
    }

   
    @Override
    public String getRole() {
        return role != null ? role : "Unknown";
    }

   
    @Override
    public String getDisplayInfo() {
        return "[User] " + fullName
             + " | Username: " + username
             + " | Role: "     + role
             + " | Email: "    + email;
    }

    // =========================================================
    // USER-SPECIFIC GETTERS & SETTERS
    // =========================================================
    public int    getUserId()                    { return userId; }
    public void   setUserId(int userId)          { this.userId = userId; }

    public String getUsername()                  { return username; }
    public void   setUsername(String u)          { this.username = u; }

    public String getPassword()                  { return password; }
    public void   setPassword(String p)          { this.password = p; }

    public void   setRole(String role)           { this.role = role; }

    // =========================================================
    // toString (method overriding)
    // =========================================================
    @Override
    public String toString() {
        return fullName != null ? fullName
             : username != null ? username : "";
    }
}
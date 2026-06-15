package model;

public class Teacher {
    private int id;
    private String teacherId;
    private String fullName;
    private String email;

    public Teacher() {}

    public Teacher(int id, String teacherId, String fullName, String email) {
        this.id = id;
        this.teacherId = teacherId;
        this.fullName = fullName;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        if (fullName != null && teacherId != null) return fullName + " (" + teacherId + ")";
        return teacherId != null ? teacherId : "";
    }
}

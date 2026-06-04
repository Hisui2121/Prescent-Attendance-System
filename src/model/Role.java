package model;

public enum Role {
    ADMIN("admin", "Administrator - Full system access"),
    TEACHER("teacher", "Teacher - Manage attendance for own classes"),
    STUDENT("student", "Student - View own attendance only");

    private final String code;
    private final String description;

    Role(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Role fromCode(String code) {
        for (Role role : Role.values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + code);
    }
}

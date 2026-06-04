package model;

public enum Permission {
    // Student Management
    VIEW_STUDENTS("view_students"),
    ADD_STUDENT("add_student"),
    EDIT_STUDENT("edit_student"),
    DELETE_STUDENT("delete_student"),

    // Class Management
    VIEW_CLASSES("view_classes"),
    ADD_CLASS("add_class"),
    EDIT_CLASS("edit_class"),
    DELETE_CLASS("delete_class"),

    // Attendance Management
    VIEW_ATTENDANCE("view_attendance"),
    CREATE_ATTENDANCE_SESSION("create_attendance_session"),
    RECORD_ATTENDANCE("record_attendance"),
    EDIT_ATTENDANCE("edit_attendance"),
    DELETE_ATTENDANCE("delete_attendance"),

    // Enrollment Management
    VIEW_ENROLLMENTS("view_enrollments"),
    ADD_ENROLLMENT("add_enrollment"),
    REMOVE_ENROLLMENT("remove_enrollment"),

    // Reports
    VIEW_REPORTS("view_reports"),
    GENERATE_REPORTS("generate_reports"),

    // User Management
    VIEW_USERS("view_users"),
    ADD_USER("add_user"),
    EDIT_USER("edit_user"),
    DELETE_USER("delete_user"),

    // Logs
    VIEW_LOGS("view_logs");

    private final String code;

    Permission(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

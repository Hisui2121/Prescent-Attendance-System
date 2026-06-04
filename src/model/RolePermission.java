package model;

import java.util.*;

public class RolePermission {

    private static final Map<Role, Set<Permission>> rolePermissions = new HashMap<>();

    static {
        initializePermissions();
    }

    private static void initializePermissions() {
        // ADMIN - Full access to everything
        Set<Permission> adminPermissions = new HashSet<>(Arrays.asList(
            Permission.VIEW_STUDENTS,
            Permission.ADD_STUDENT,
            Permission.EDIT_STUDENT,
            Permission.DELETE_STUDENT,
            Permission.VIEW_CLASSES,
            Permission.ADD_CLASS,
            Permission.EDIT_CLASS,
            Permission.DELETE_CLASS,
            Permission.VIEW_ATTENDANCE,
            Permission.CREATE_ATTENDANCE_SESSION,
            Permission.RECORD_ATTENDANCE,
            Permission.EDIT_ATTENDANCE,
            Permission.DELETE_ATTENDANCE,
            Permission.VIEW_ENROLLMENTS,
            Permission.ADD_ENROLLMENT,
            Permission.REMOVE_ENROLLMENT,
            Permission.VIEW_REPORTS,
            Permission.GENERATE_REPORTS,
            Permission.VIEW_USERS,
            Permission.ADD_USER,
            Permission.EDIT_USER,
            Permission.DELETE_USER,
            Permission.VIEW_LOGS
        ));
        rolePermissions.put(Role.ADMIN, adminPermissions);

        // TEACHER - Can manage attendance and view related info
        Set<Permission> teacherPermissions = new HashSet<>(Arrays.asList(
            Permission.VIEW_STUDENTS,
            Permission.VIEW_CLASSES,
            Permission.VIEW_ATTENDANCE,
            Permission.CREATE_ATTENDANCE_SESSION,
            Permission.RECORD_ATTENDANCE,
            Permission.EDIT_ATTENDANCE,
            Permission.VIEW_ENROLLMENTS,
            Permission.VIEW_REPORTS
        ));
        rolePermissions.put(Role.TEACHER, teacherPermissions);

        // STUDENT - Can only view their own attendance
        Set<Permission> studentPermissions = new HashSet<>(Arrays.asList(
            Permission.VIEW_ATTENDANCE,
            Permission.VIEW_REPORTS
        ));
        rolePermissions.put(Role.STUDENT, studentPermissions);
    }

    public static boolean hasPermission(Role role, Permission permission) {
        Set<Permission> permissions = rolePermissions.get(role);
        return permissions != null && permissions.contains(permission);
    }

    public static Set<Permission> getPermissions(Role role) {
        Set<Permission> permissions = rolePermissions.get(role);
        return permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }

    public static void printPermissions(Role role) {
        System.out.println("\n===== PERMISSIONS FOR " + role.name() + " =====");
        Set<Permission> permissions = getPermissions(role);
        if (permissions.isEmpty()) {
            System.out.println("No permissions assigned.");
        } else {
            for (Permission perm : permissions) {
                System.out.println("  ✓ " + perm.getCode());
            }
        }
    }
}

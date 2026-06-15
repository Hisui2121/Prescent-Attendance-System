package service;

import model.User;
import model.Role;
import model.Permission;
import model.RolePermission;
import dao.UserDAO;
import util.Logger;

public class AuthenticationService {

    private static AuthenticationService instance;

    private UserDAO userDAO;
    private User currentUser;

    private AuthenticationService() {
        this.userDAO = new UserDAO();
        this.currentUser = null;
    }

    public static synchronized AuthenticationService getInstance() {
        if (instance == null) instance = new AuthenticationService();
        return instance;
    }

    public boolean login(String username, String password) {
        User user = userDAO.authenticateUser(username, password);
        
        if (user != null) {
            // normalize stored values (trim)
            if (user.getRole() != null) user.setRole(user.getRole().trim());
            if (user.getUsername() != null) user.setUsername(user.getUsername().trim());

            this.currentUser = user;
            Logger.logLogin(username, true);
            System.out.println("\n✓ Welcome, " + user.getUsername() + " (" + user.getRole() + ")!");
            return true;
        } else {
            Logger.logLogin(username, false);
            System.out.println("\n✗ Invalid username or password!");
            return false;
        }
    }

    public void logout() {
        if (currentUser != null) {
            Logger.logLogout(currentUser.getUsername());
            System.out.println("✓ Logged out successfully!");
            currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean hasPermission(Permission permission) {
        if (currentUser == null) {
            return false;
        }
        
        Role role = getCurrentRole();
        if (role == null) return false;
        boolean hasAccess = RolePermission.hasPermission(role, permission);
        
        if (!hasAccess) {
            Logger.logUnauthorizedAccess(currentUser.getUsername(), permission.getCode());
        }
        
        return hasAccess;
    }

    public Role getCurrentRole() {
        if (currentUser == null) {
            return null;
        }
        String r = currentUser.getRole();
        if (r == null) return null;
        r = r.trim();
        try {
            return Role.fromCode(r);
        } catch (IllegalArgumentException ex) {
            System.out.println("Warning: unknown role code '" + r + "' for user " + getCurrentUsername() + ", defaulting to null.");
            return null;
        }
    }

    public String getCurrentUsername() {
        if (currentUser == null) return "Unknown";
        // prefer username field; if empty try email
        String u = currentUser.getUsername();
        if (u != null && !u.trim().isEmpty()) return u.trim();
        try {
            String email = currentUser.getEmail();
            if (email != null && !email.trim().isEmpty()) return email.trim();
        } catch (Exception ex) { }
        return "Unknown";
    }
}
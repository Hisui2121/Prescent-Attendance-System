package service;

import model.User;
import model.Role;
import model.Permission;
import model.RolePermission;
import dao.UserDAO;
import util.Logger;

public class AuthenticationService {

    private UserDAO userDAO;
    private User currentUser;

    public AuthenticationService() {
        this.userDAO = new UserDAO();
        this.currentUser = null;
    }

    public boolean login(String username, String password) {
        User user = userDAO.authenticateUser(username, password);
        
        if (user != null) {
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
        
        Role role = Role.fromCode(currentUser.getRole());
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
        return Role.fromCode(currentUser.getRole());
    }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : "Unknown";
    }
}

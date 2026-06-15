package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import database.DBConnect;
import model.Person;
import model.User;
import util.PasswordUtil;

public class UserDAO {

    // =========================================================
    // PRIVATE HELPER — maps a ResultSet row to a User (as Person)
    // =========================================================
    private Person mapRow(ResultSet rs) throws Exception {
        User u = new User();
        u.setUserId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setRole(rs.getString("role"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        return u;   // returned as Person — polymorphism in action
    }

    // =========================================================
    // ADD USER
    // =========================================================
    public void addUser(User user) {
        try {
            String sql = "INSERT INTO users (username, password, role, full_name, email) "
                       + "VALUES (?, ?, ?, ?, ?)";
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getPersonId());   // polymorphic — returns username
            pstmt.setString(2, PasswordUtil.hashPassword(user.getPassword()));
            pstmt.setString(3, user.getRole());       // polymorphic call
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getEmail());
            pstmt.executeUpdate();
            System.out.println("User added: " + user.getDisplayInfo()); // polymorphic
        } catch (SQLException e) {
            System.out.println("Error adding user: " + e.getMessage());
        }
    }

    // =========================================================
    // AUTHENTICATE USER
    // =========================================================
    public User authenticateUser(String username, String password) {
        try {
            String sql = "SELECT * FROM users WHERE username = ? OR email = ?";
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    return (User) mapRow(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("Error authenticating user: " + e.getMessage());
        }
        return null;
    }

    // =========================================================
    // GET USER BY USERNAME
    // =========================================================
    public User getUserByUsername(String username) {
        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return (User) mapRow(rs);
        } catch (Exception e) {
            System.out.println("Error retrieving user: " + e.getMessage());
        }
        return null;
    }

    // =========================================================
    // GET ALL USERS
    // =========================================================
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try {
            String sql = "SELECT * FROM users";
            Connection conn = DBConnect.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) users.add((User) mapRow(rs));
        } catch (Exception e) {
            System.out.println("Error retrieving users: " + e.getMessage());
        }
        return users;
    }

    public List<Person> getAllPersons() {
        List<Person> persons = new ArrayList<>();
        try {
            String sql = "SELECT * FROM users";
            Connection conn = DBConnect.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) persons.add(mapRow(rs));   // stored as Person
        } catch (Exception e) {
            System.out.println("Error in getAllPersons: " + e.getMessage());
        }
        return persons;
    }

    // =========================================================
    // UPDATE USER
    // =========================================================
    public void updateUser(User user) {
        try {
            String sql;
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                sql = "UPDATE users SET role=?, full_name=?, email=?, password=? WHERE id=?";
            } else {
                sql = "UPDATE users SET role=?, full_name=?, email=? WHERE id=?";
            }
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getRole());   // polymorphic call
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getEmail());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                pstmt.setString(4, PasswordUtil.hashPassword(user.getPassword()));
                pstmt.setInt(5, user.getUserId());
            } else {
                pstmt.setInt(4, user.getUserId());
            }
            pstmt.executeUpdate();
            System.out.println("User updated: " + user.getDisplayInfo()); // polymorphic
        } catch (SQLException e) {
            System.out.println("Error updating user: " + e.getMessage());
        }
    }

    // =========================================================
    // DELETE USER
    // =========================================================
    public void deleteUser(int userId) {
        try {
            String sql = "DELETE FROM users WHERE id=?";
            Connection conn = DBConnect.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            System.out.println("User deleted: id=" + userId);
        } catch (SQLException e) {
            System.out.println("Error deleting user: " + e.getMessage());
        }
    }
}
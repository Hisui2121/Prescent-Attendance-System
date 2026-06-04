# Authentication & Role-Based Access Control System

## Overview
This document describes the newly implemented authentication, role-based access control (RBAC), logging, and password security system for the Presence Attendance System.

## System Architecture

### 1. **Authentication System**
- **Location**: `src/service/AuthenticationService.java`
- **Purpose**: Manages user login/logout and session management
- **Key Methods**:
  - `login(username, password)`: Authenticates user credentials
  - `logout()`: Ends user session
  - `isLoggedIn()`: Checks if user is currently authenticated
  - `hasPermission(Permission)`: Checks if user has specific permission

### 2. **Role-Based Access Control (RBAC)**

#### Roles Defined (`src/model/Role.java`):
- **ADMIN**: Full system access - manage students, classes, attendance, enrollments, reports, users, and view logs
- **TEACHER**: Limited access - manage attendance, view classes and students, record attendance
- **STUDENT**: Read-only access - view own attendance and reports

#### Permissions (`src/model/Permission.java`):
The system defines 29 granular permissions including:
- Student Management: VIEW, ADD, EDIT, DELETE
- Class Management: VIEW, ADD, EDIT, DELETE
- Attendance Management: VIEW, CREATE_SESSION, RECORD, EDIT, DELETE
- Enrollment Management: VIEW, ADD, REMOVE
- Reports: VIEW, GENERATE
- User Management: VIEW, ADD, EDIT, DELETE
- Logs: VIEW

#### Permission Mapping (`src/model/RolePermission.java`):
Maps each role to its allowed permissions:
```
ADMIN    → All 29 permissions
TEACHER  → 8 permissions (attendance-focused)
STUDENT  → 2 permissions (view-only)
```

### 3. **User Management**
- **DAO**: `src/dao/UserDAO.java`
- **Features**:
  - Add new users with roles
  - Authenticate users with password verification
  - Retrieve user information
  - Update user roles
  - Delete users

### 4. **Security Components**

#### Password Hashing (`src/util/PasswordUtil.java`):
- Uses SHA-256 with salt for secure password storage
- Generates random salt (16 bytes) for each password
- Implements constant-time comparison to prevent timing attacks
- Methods:
  - `hashPassword(password)`: Hashes password with salt
  - `verifyPassword(password, storedHash)`: Securely verifies password

#### Logging System (`src/util/Logger.java`):
- Logs all system activities to `system_logs.txt`
- Timestamp: YYYY-MM-DD HH:MM:SS format
- Log Types: INFO, WARN, ERROR
- Tracked Events:
  - Login attempts (success/failure)
  - Logout events
  - User actions (add/edit/delete)
  - Attendance records
  - Unauthorized access attempts
  - System errors

### 5. **User Interface**
- **LoginUI** (`src/ui/LoginUI.java`):
  - Login screen with username/password input
  - Display available roles and permissions
  - Exit option
  - Integrated into ConsoleUI

## Default Users

The system creates default users on first initialization:

| Username | Password | Role | Purpose |
|----------|----------|------|---------|
| admin | admin123 | ADMIN | System administrator |
| teacher1 | teacher123 | TEACHER | Sample teacher account |

⚠️ **IMPORTANT**: Change default passwords after first login!

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## How to Use

### 1. **Starting the System**
```
Run Main.java
↓
Database initializes with users table
↓
LoginUI displays login screen
↓
User enters credentials
↓
Authentication Service verifies password
↓
Log entry created in system_logs.txt
↓
Access appropriate menu based on role
```

### 2. **Access Control in Code**
```java
// Check permission before action
if (authService.hasPermission(Permission.ADD_STUDENT)) {
    // Allow adding student
    Logger.logStudentAdded(username, studentId, studentName);
} else {
    // Deny access and log attempt
    Logger.logUnauthorizedAccess(username, "ADD_STUDENT");
}
```

### 3. **Adding New Users**
- Admin can add users through user management menu
- Passwords are automatically hashed before storage
- Roles are assigned during user creation

### 4. **Viewing Logs**
- Admin can view system logs in the Reports menu
- Logs show timestamp, action, user, and details
- File location: `system_logs.txt` (in project root)

## File Structure

```
src/
├── model/
│   ├── User.java (enhanced with role support)
│   ├── Role.java (NEW - Enum for roles)
│   ├── Permission.java (NEW - Enum for permissions)
│   └── RolePermission.java (NEW - RBAC mapping)
├── dao/
│   └── UserDAO.java (NEW - User data access)
├── service/
│   └── AuthenticationService.java (NEW - Auth logic)
├── util/
│   ├── Logger.java (NEW - Activity logging)
│   └── PasswordUtil.java (NEW - Secure password handling)
├── ui/
│   ├── LoginUI.java (NEW - Login interface)
│   └── ConsoleUI.java (modified to integrate login)
├── database/
│   └── DBInitialize.java (modified to add users table)
└── app/
    └── Main.java (unchanged entry point)
```

## Security Considerations

1. **Password Storage**: Passwords are salted and hashed, never stored in plaintext
2. **Timing Attack Prevention**: Constant-time comparison prevents password guessing via timing analysis
3. **Logging**: All access attempts are logged for audit trails
4. **Role-Based Access**: Permissions are checked before allowing sensitive operations
5. **Unauthorized Access Tracking**: Attempts to access unauthorized features are logged

## Future Enhancements

- [ ] Password reset/recovery functionality
- [ ] User session timeout after inactivity
- [ ] Two-factor authentication (2FA)
- [ ] Role creation/modification UI for admins
- [ ] Encrypted log file storage
- [ ] User activity dashboard
- [ ] IP address logging for login attempts
- [ ] Account lockout after failed login attempts

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Login fails | Check username/password - default is admin/admin123 |
| Cannot add students | Verify user role has ADD_STUDENT permission |
| Logs not appearing | Check if system_logs.txt file is created in project root |
| Permission denied | Check current user role and required permission |

## Testing

To verify the system:
1. Login with admin (admin123) - should succeed
2. Login with invalid credentials - should fail and log warning
3. Login as teacher1 (teacher123) - should only see teacher menu
4. Attempt unauthorized action as teacher - should log unauthorized access
5. Logout and view logs - should show activity trail


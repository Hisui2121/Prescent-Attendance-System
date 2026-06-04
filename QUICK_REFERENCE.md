# Quick Reference - Authentication System

## 🎯 What Was Created?

### Core Components (6 New Classes)
1. **Role.java** - Enum (ADMIN, TEACHER, STUDENT)
2. **Permission.java** - Enum (29 permissions)
3. **RolePermission.java** - RBAC mapping
4. **AuthenticationService.java** - Auth logic
5. **UserDAO.java** - User database operations
6. **LoginUI.java** - Login screen

### Utilities (2 New Classes)
1. **Logger.java** - Activity logging (system_logs.txt)
2. **PasswordUtil.java** - Secure password hashing

### Total: 8 New Classes + 2 Modified Files

---

## 🔑 Default Login Credentials

```
Admin:
  Username: admin
  Password: admin123
  Role: ADMIN (Full Access)

Teacher:
  Username: teacher1
  Password: teacher123
  Role: TEACHER (Limited Access)
```

---

## 📊 Role Permissions

### ADMIN (29 permissions)
- All operations on Students, Classes, Attendance, Enrollments
- View Reports & Logs
- User Management (Add/Edit/Delete users)

### TEACHER (8 permissions)
- View Students & Classes
- Create & Record Attendance
- View Enrollments & Reports
- Cannot manage users or delete data

### STUDENT (2 permissions)
- View Attendance
- View Reports
- Cannot modify any data

---

## 📂 File Locations

```
Model Layer:
  src/model/Role.java
  src/model/Permission.java
  src/model/RolePermission.java

Data Access:
  src/dao/UserDAO.java

Service Layer:
  src/service/AuthenticationService.java

UI Layer:
  src/ui/LoginUI.java

Utilities:
  src/util/Logger.java
  src/util/PasswordUtil.java

Database:
  src/database/DBInitialize.java (modified)

Documentation:
  AUTHENTICATION_SYSTEM.md
  AUTH_IMPLEMENTATION_SUMMARY.md (this file location's parent)
```

---

## 🔐 How It Works

### 1. User Logs In
```
User enters username/password
↓
AuthenticationService.login() validates
↓
Password verified using PasswordUtil (SHA-256 + salt)
↓
Logger records login attempt
↓
Session stored if successful
```

### 2. Permission Check
```
User attempts action (e.g., add student)
↓
AuthenticationService.hasPermission() checks
↓
Role looked up in RolePermission
↓
Access granted/denied
↓
Logger records if unauthorized
```

### 3. All Activities Logged
```
system_logs.txt contains:
  [2026-06-04 12:30:45] [INFO] [LOGIN] User: admin | Details: Login successful
  [2026-06-04 12:31:10] [INFO] [ADD_STUDENT] User: admin | Details: Added student S001 (John Doe)
  [2026-06-04 12:32:20] [WARN] [ADD_STUDENT] User: teacher1 | Details: UNAUTHORIZED ACCESS ATTEMPT
```

---

## 💻 Usage in Code

### Initialize Auth Service
```java
AuthenticationService authService = new AuthenticationService();
```

### Login User
```java
if (authService.login("admin", "admin123")) {
    System.out.println("Login successful!");
}
```

### Check Permission
```java
import model.Permission;

if (authService.hasPermission(Permission.ADD_STUDENT)) {
    // Allow action
} else {
    // Deny action
}
```

### Log Action
```java
import util.Logger;

Logger.logStudentAdded(authService.getCurrentUsername(), "S001", "John");
Logger.logAction("admin", "VIEW_STUDENTS", "List accessed");
Logger.logUnauthorizedAccess("teacher1", "DELETE_USER");
```

### Get Current User
```java
String username = authService.getCurrentUsername();
Role role = authService.getCurrentRole();
boolean loggedIn = authService.isLoggedIn();
```

### Logout
```java
authService.logout();
```

---

## 📋 Database Schema

### Users Table (Auto-created)
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Default entries inserted on first run:
- admin / (hashed) admin123 / admin
- teacher1 / (hashed) teacher123 / teacher

---

## 🔍 View Logs

```java
// In AdminMenu or Reports section:
Logger.viewLogs();
```

Displays system_logs.txt with:
- Timestamp
- Log type (INFO, WARN, ERROR)
- Action
- User
- Details

---

## ⚠️ Security Notes

✅ Passwords hashed with SHA-256 + salt
✅ Never stored in plaintext
✅ Constant-time comparison (no timing attacks)
✅ All access attempts logged
✅ Unauthorized access tracked
✅ Audit trail available

❌ Default passwords must be changed!
❌ Do not hardcode credentials in production!

---

## 🎮 System Flow

```
Start Application (Main.java)
↓
Database Initialize (users table + defaults)
↓
Show LoginUI
↓
User logs in with credentials
↓
AuthenticationService validates
↓
Logger records attempt
↓
If valid → Show role-appropriate menu
↓
Each menu action checked for permissions
↓
Logger tracks all operations
↓
User logs out
```

---

## 📝 Next Integration Steps

1. Modify `ConsoleUI.start()` to call `LoginUI.showLoginMenu()`
2. Pass `AuthenticationService` instance to all menu methods
3. Add permission checks before sensitive operations
4. Integrate `Logger` calls in CRUD operations
5. Test with all three user roles

---

## 🆘 Troubleshooting

| Problem | Solution |
|---------|----------|
| Login fails | Use admin/admin123 or teacher1/teacher123 |
| Can't see logs | Check if system_logs.txt exists in project root |
| Permission denied | Check user role has required permission |
| Database error | Ensure DBInitialize.initialize() is called in Main |

---

**Created**: 2026-06-04
**Author**: AI Copilot
**Status**: ✅ Ready for Integration


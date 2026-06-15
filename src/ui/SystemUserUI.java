package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

import dao.UserDAO;
import model.User;
import dao.TeacherDAO;
import model.Teacher;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class SystemUserUI {

    private TableView<User> table;
    private ObservableList<User> data = FXCollections.observableArrayList();
    private UserDAO userDAO = new UserDAO();
    private TeacherDAO teacherDAO = new TeacherDAO();

    public Pane getView() {
        // Layout: place form on top and user table below for requested UX
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(12));

        // Top: form card
        VBox left = new VBox(10);
        left.setPrefWidth(760);
        Label title = new Label("Create System User");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setPadding(new Insets(8));
        form.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 6; -fx-padding: 12;");

        // Username removed from input; it will be auto-generated from Full Name
        TextField txtFullName = new TextField();
        TextField txtEmail = new TextField();
        PasswordField txtPassword = new PasswordField();
        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("admin","teacher");
        cbRole.setValue("teacher");

        form.add(new Label("Full Name"), 0, 0);
        form.add(txtFullName, 1, 0);
        form.add(new Label("Email"), 0, 1);
        form.add(txtEmail, 1, 1);
        form.add(new Label("Password"), 0, 2);
        form.add(txtPassword, 1, 2);
        form.add(new Label("Role"), 0, 3);
        form.add(cbRole, 1, 3);

        Button btnAdd = new Button("Create User");
        btnAdd.getStyleClass().add("primary-button");
        HBox btnWrap = new HBox(btnAdd);
        btnWrap.setAlignment(Pos.CENTER_RIGHT);
        form.add(btnWrap, 1, 4);

        left.getChildren().addAll(title, form);

        // Below form: users table and controls
        VBox tableContainer = new VBox(8);
        tableContainer.setPadding(new Insets(6, 0, 0, 0));
        Label tblTitle = new Label("System Users");
        tblTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<User, String> colName = new TableColumn<>("FULL NAME");
        TableColumn<User, String> colEmail = new TableColumn<>("EMAIL");
        TableColumn<User, String> colRole = new TableColumn<>("ROLE");
        TableColumn<User, String> colUsername = new TableColumn<>("USERNAME");

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Make table more prominent: larger row height and column proportions
        table.setFixedCellSize(40); // larger cell height
        table.setPrefHeight(420);
        table.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setPrefHeight(40);
            return row;
        });

        // Column width proportions
        colName.prefWidthProperty().bind(table.widthProperty().multiply(0.35));
        colEmail.prefWidthProperty().bind(table.widthProperty().multiply(0.30));
        colRole.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        colUsername.prefWidthProperty().bind(table.widthProperty().multiply(0.20));

        table.getColumns().addAll(colName, colEmail, colRole, colUsername);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Edit/Delete controls (placed above the table but below the form)
        HBox adminControls = new HBox(8);
        adminControls.setAlignment(Pos.CENTER_RIGHT);
        Button btnEdit = new Button("Edit Selected");
        btnEdit.getStyleClass().add("secondary-button");
        Button btnDelete = new Button("Delete Selected");
        btnDelete.getStyleClass().add("danger-button");
        adminControls.getChildren().addAll(btnEdit, btnDelete);

        tableContainer.getChildren().addAll(tblTitle, adminControls, table);

        layout.getChildren().addAll(left, tableContainer);

        // Edit handler
        btnEdit.setOnAction(ev -> {
            User sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("No Selection","Please select a user to edit."); return; }

            Dialog<User> dialog = new Dialog<>();
            dialog.setTitle("Edit User");
            ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(12));
            TextField tfFullName = new TextField(sel.getFullName());
            TextField tfEmail = new TextField(sel.getEmail());
            PasswordField tfPassword = new PasswordField();
            ComboBox<String> cbRoleEdit = new ComboBox<>(); cbRoleEdit.getItems().addAll("admin","teacher"); cbRoleEdit.setValue(sel.getRole());

            grid.add(new Label("Full Name"), 0, 0); grid.add(tfFullName, 1, 0);
            grid.add(new Label("Email"), 0, 1); grid.add(tfEmail, 1, 1);
            grid.add(new Label("New Password (leave blank to keep)"), 0, 2); grid.add(tfPassword, 1, 2);
            grid.add(new Label("Role"), 0, 3); grid.add(cbRoleEdit, 1, 3);

            dialog.getDialogPane().setContent(grid);
            dialog.setResultConverter(btn -> {
                if (btn == saveBtn) {
                    sel.setFullName(tfFullName.getText().trim());
                    sel.setEmail(tfEmail.getText().trim());
                    String p = tfPassword.getText();
                    if (p != null && !p.isEmpty()) sel.setPassword(p);
                    sel.setRole(cbRoleEdit.getValue());
                    return sel;
                }
                return null;
            });

            dialog.showAndWait().ifPresent(updated -> {
                userDAO.updateUser(updated);
                if ("teacher".equalsIgnoreCase(updated.getRole())) {
                    Teacher t = new Teacher(); t.setTeacherId(updated.getUsername()); t.setFullName(updated.getFullName()); t.setEmail(updated.getEmail());
                    teacherDAO.addTeacher(t);
                }
                refresh();
            });
        });

        // Delete handler
        btnDelete.setOnAction(ev -> {
            User sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("No Selection","Please select a user to delete."); return; }
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Delete user " + sel.getUsername() + "? This cannot be undone.", ButtonType.YES, ButtonType.NO);
            conf.setTitle("Confirm Delete"); conf.setHeaderText(null);
            conf.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) {
                    userDAO.deleteUser(sel.getUserId());
                    refresh();
                }
            });
        });

        // Add user action: auto-generate username from full name and ensure uniqueness
        btnAdd.setOnAction(e -> {
            String fn = txtFullName.getText().trim();
            String em = txtEmail.getText().trim();
            String pw = txtPassword.getText();
            String role = cbRole.getValue();
            if (fn.isEmpty() || pw.isEmpty() || role == null) {
                showAlert("Validation","Please provide full name, password and role.");
                return;
            }

            String genUsername = generateUsername(fn);

            User user = new User();
            user.setUsername(genUsername);
            user.setPassword(pw);
            user.setRole(role);
            user.setFullName(fn);
            user.setEmail(em);

            boolean added = false;
            try {
                userDAO.addUser(user);
                added = true;
            } catch (Exception ex) {
                System.out.println("Error adding user: " + ex.getMessage());
            }

            if (!added) {
                showAlert("Error", "Unable to create user. See console for details.");
                return;
            }

            // If teacher, create teachers table entry
            if ("teacher".equalsIgnoreCase(role)) {
                Teacher t = new Teacher();
                t.setTeacherId(genUsername);
                t.setFullName(fn);
                t.setEmail(em);
                boolean ok = teacherDAO.addTeacher(t);
                if (!ok) System.out.println("Warning: failed to add teacher record for " + genUsername);
            }

            refresh();
            txtFullName.clear(); txtEmail.clear(); txtPassword.clear(); cbRole.setValue("teacher");

            // show the generated username so admin knows credentials
            showAlert("User Created", "User created with username: " + genUsername);
        });

        refresh();
        return layout;
    }

    // Generate a simple username from full name and ensure uniqueness in users table
    private String generateUsername(String fullName) {
        if (fullName == null) fullName = "user";
        String base = fullName.trim().toLowerCase().replaceAll("[^a-z0-9]+", ".");
        base = base.replaceAll("\\.{2,}", ".");
        if (base.startsWith(".")) base = base.substring(1);
        if (base.endsWith(".")) base = base.substring(0, base.length()-1);
        if (base.isEmpty()) base = "user";

        String candidate = base;
        int suffix = 1;
        while (userDAO.getUserByUsername(candidate) != null) {
            candidate = base + suffix;
            suffix++;
            if (suffix > 1000) break; // safety
        }
        return candidate;
    }

    private void refresh() {
        data.clear();
        try {
            data.addAll(userDAO.getAllUsers());
        } catch (Exception ex) {
            System.out.println("User list error: " + ex.getMessage());
        }
        table.setItems(data);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
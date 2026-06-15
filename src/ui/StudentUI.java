package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.SelectionMode;

import model.Student;
import dao.StudentDAO;
import util.EventBus;

import java.util.HashMap;
import java.util.Map;

public class StudentUI {

    private TableView<Student> table;
    private StudentDAO studentDAO = new StudentDAO();

    private ObservableList<Student> masterData = FXCollections.observableArrayList();
    private FilteredList<Student> filteredData;

    // selection tracking for multi-select delete
    private Map<String, SimpleBooleanProperty> studentSelectionMap = new HashMap<>();

    // expose filters so we can refresh their options after add/edit
    private ComboBox<String> courseFilterCombo;
    private ComboBox<String> yearFilterCombo;

    public VBox getView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(20));

        Label title = new Label("Student Directory");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Filters: Course and Year
        HBox filterRow = new HBox(10);
        courseFilterCombo = new ComboBox<>();
        courseFilterCombo.setPromptText("Filter by Course");
        yearFilterCombo = new ComboBox<>();
        yearFilterCombo.setPromptText("Filter by Year");
        Button btnClearFilters = new Button("Clear Filters");
        btnClearFilters.getStyleClass().add("secondary-button");
        filterRow.getChildren().addAll(new Label("Filters:"), courseFilterCombo, yearFilterCombo, btnClearFilters);

        // Table setup
        table = new TableView<>();
        table.setEditable(true); // enable editing so checkbox cells are clickable
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Student, String> colId = new TableColumn<>("STUDENT ID");
        TableColumn<Student, String> colName = new TableColumn<>("FULL NAME");
        TableColumn<Student, String> colCourse = new TableColumn<>("COURSE / PROGRAM");
        TableColumn<Student, String> colYear = new TableColumn<>("YEAR LEVEL");
        TableColumn<Student, String> colStatus = new TableColumn<>("EMAIL");

        // Selection column for multi-select
        TableColumn<Student, Boolean> colSelect = new TableColumn<>("");
        colSelect.setPrefWidth(40);
        colSelect.setCellValueFactory(cell -> {
            Student s = cell.getValue();
            if (s == null || s.getStudentId() == null) return new SimpleBooleanProperty(false);
            SimpleBooleanProperty prop = studentSelectionMap.get(s.getStudentId());
            if (prop == null) {
                prop = new SimpleBooleanProperty(false);
                studentSelectionMap.put(s.getStudentId(), prop);
            }
            return prop;
        });
        colSelect.setEditable(true);
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));

        // Bind columns to model properties
        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Responsive Column Widths
        colId.prefWidthProperty().bind(table.widthProperty().multiply(0.12));
        colName.prefWidthProperty().bind(table.widthProperty().multiply(0.30));
        colCourse.prefWidthProperty().bind(table.widthProperty().multiply(0.25));
        colYear.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        colStatus.prefWidthProperty().bind(table.widthProperty().multiply(0.14));

        table.getColumns().addAll(colSelect, colId, colName, colCourse, colYear, colStatus);

        // Controls
        HBox controls = new HBox(10);
        Button btnAdd = new Button("Add Student");
        btnAdd.getStyleClass().add("primary-button");
        
        Button btnEdit = new Button("Edit Selected");
        btnEdit.getStyleClass().add("secondary-button");
        
        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("secondary-button");
        controls.getChildren().addAll(btnAdd, btnEdit, btnDelete);
        
        // place Delete button to the right
        Region ctlSpacer = new Region();
        HBox.setHgrow(ctlSpacer, Priority.ALWAYS);
        controls.getChildren().addAll(ctlSpacer);

        card.getChildren().addAll(filterRow, table, controls);
        layout.getChildren().addAll(title, card);

        // Initial load
        refreshTable();

        // populate filters
        updateFilterOptions(courseFilterCombo, yearFilterCombo);

        // filter listeners
        courseFilterCombo.setOnAction(e -> applyFilters(courseFilterCombo.getValue(), yearFilterCombo.getValue()));
        yearFilterCombo.setOnAction(e -> applyFilters(courseFilterCombo.getValue(), yearFilterCombo.getValue()));
        btnClearFilters.setOnAction(e -> {
            courseFilterCombo.getSelectionModel().clearSelection();
            yearFilterCombo.getSelectionModel().clearSelection();
            applyFilters(null, null);
        });

        // Button actions
        btnAdd.setOnAction(e -> showStudentDialog(null));

        btnEdit.setOnAction(e -> {
            Student sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("No Selection", "Please select a student to edit.");
                return;
            }
            showStudentDialog(sel);
        });

        btnDelete.setOnAction(e -> {
            // collect selected ids
            java.util.List<String> toDelete = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, SimpleBooleanProperty> en : studentSelectionMap.entrySet()) {
                if (en.getValue().get()) toDelete.add(en.getKey());
            }

            Student single = table.getSelectionModel().getSelectedItem();
            if (toDelete.isEmpty() && single == null) {
                showAlert("No Selection", "Please select student(s) to delete.");
                return;
            }

            if (toDelete.isEmpty() && single != null) {
                toDelete.add(single.getStudentId());
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText(null);
            confirm.setContentText("Delete " + toDelete.size() + " student(s)?");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    boolean allOk = true;
                    for (String sid : toDelete) {
                        boolean ok = studentDAO.deleteStudent(sid);
                        if (!ok) allOk = false;
                    }
                    refreshTable();
                    updateFilterOptions(courseFilterCombo, yearFilterCombo);
                    EventBus.fireStudentChanged();
                    if (!allOk) showAlert("Partial Error", "Some deletions failed. Check console.");
                }
            });
        });

        return layout;
    }

    private void applyFilters(String course, String year) {
        if (filteredData == null) return;
        filteredData.setPredicate(s -> {
            boolean okCourse = true;
            boolean okYear = true;
            if (course != null && !course.isEmpty()) {
                okCourse = course.equals(s.getCourse());
            }
            if (year != null && !year.isEmpty()) {
                okYear = year.equals(s.getYearLevel());
            }
            return okCourse && okYear;
        });
        // keep any sorting
        SortedList<Student> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
    }

    private void updateFilterOptions(ComboBox<String> courseFilter, ComboBox<String> yearFilter) {
        ObservableList<Student> all = FXCollections.observableArrayList(studentDAO.getAllStudents());
        ObservableList<String> courses = FXCollections.observableArrayList();
        ObservableList<String> years = FXCollections.observableArrayList();
        for (Student s : all) {
            if (s.getCourse() != null && !s.getCourse().isEmpty() && !courses.contains(s.getCourse())) courses.add(s.getCourse());
            if (s.getYearLevel() != null && !s.getYearLevel().isEmpty() && !years.contains(s.getYearLevel())) years.add(s.getYearLevel());
        }
        courseFilter.setItems(courses);
        yearFilter.setItems(years);
    }

    private void refreshTable() {
        masterData = FXCollections.observableArrayList(studentDAO.getAllStudents());
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Student> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // ensure selection map contains entries for current students (reset selections)
        studentSelectionMap.clear();
        for (Student s : masterData) {
            studentSelectionMap.put(s.getStudentId(), new SimpleBooleanProperty(false));
        }
    }

    private String nextStudentId() {
        // Generate next ID S-001 style
        int max = 0;
        for (Student s : studentDAO.getAllStudents()) {
            String id = s.getStudentId();
            if (id == null) continue;
            id = id.trim();
            if (id.startsWith("S-")) {
                try {
                    int num = Integer.parseInt(id.substring(2));
                    if (num > max) max = num;
                } catch (Exception ex) { }
            }
        }
        int next = max + 1;
        return String.format("S-%03d", next);
    }

    private void showStudentDialog(Student editing) {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle(editing == null ? "Add Student" : "Edit Student");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField tfStudentId = new TextField();
        tfStudentId.setPromptText("e.g. S-001");
        TextField tfFullName = new TextField();
        TextField tfCourse = new TextField();
        TextField tfYear = new TextField();
        TextField tfEmail = new TextField();

        grid.add(new Label("Student ID:"), 0, 0);
        grid.add(tfStudentId, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(tfFullName, 1, 1);
        grid.add(new Label("Course:"), 0, 2);
        grid.add(tfCourse, 1, 2);
        grid.add(new Label("Year Level:"), 0, 3);
        grid.add(tfYear, 1, 3);
        grid.add(new Label("Email:"), 0, 4);
        grid.add(tfEmail, 1, 4);

        if (editing != null) {
            tfStudentId.setText(editing.getStudentId());
            tfStudentId.setDisable(true); // Do not allow changing primary student id
            tfFullName.setText(editing.getFullName());
            tfCourse.setText(editing.getCourse());
            tfYear.setText(editing.getYearLevel());
            tfEmail.setText(editing.getEmail());
        } else {
            // Auto-generate the next student ID and prevent editing
            tfStudentId.setText(nextStudentId());
            tfStudentId.setDisable(true);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtn) {
                String sid = tfStudentId.getText().trim();
                String name = tfFullName.getText().trim();
                String course = tfCourse.getText().trim();
                String year = tfYear.getText().trim();
                String email = tfEmail.getText().trim();

                if (sid.isEmpty() || name.isEmpty()) {
                    showAlert("Validation", "Student ID and Full Name are required.");
                    return null;
                }

                if (editing == null) {
                    Student s = new Student(sid, name, course, year, email);
                    boolean ok = studentDAO.addStudent(s);
                    if (!ok) {
                        showAlert("Error", "Failed to add student. Check console for details.");
                    }
                    refreshTable();
                    // update filters to include newly added student's course/year
                    updateFilterOptions(courseFilterCombo, yearFilterCombo);
                    EventBus.fireStudentChanged();
                    return s;
                } else {
                    editing.setFullName(name);
                    editing.setCourse(course);
                    editing.setYearLevel(year);
                    editing.setEmail(email);

                    boolean ok = studentDAO.updateStudent(editing);
                    if (!ok) {
                        showAlert("Error", "Failed to update student. Check console for details.");
                    }
                    refreshTable();
                    // update filters in case course/year changed
                    updateFilterOptions(courseFilterCombo, yearFilterCombo);
                    EventBus.fireStudentChanged();
                    return editing;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
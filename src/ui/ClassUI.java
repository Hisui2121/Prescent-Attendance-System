package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

import model.Class;
import dao.ClassDAO;
import util.EventBus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.cell.CheckBoxTableCell;
import java.util.HashMap;
import java.util.Map;
import dao.TeacherDAO;
import model.Teacher;

public class ClassUI {

    private TableView<Class> table;
    private ClassDAO classDAO = new ClassDAO();
    // selection tracking for multi-delete
    private Map<Integer, SimpleBooleanProperty> classSelectionMap = new HashMap<>();
    private TeacherDAO teacherDAO = new TeacherDAO();

    public VBox getView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        Label title = new Label("Classes Management");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        Label subtitle = new Label("Organize academic schedules and assign faculty members.");
        subtitle.setStyle("-fx-text-fill: #64748B;");

        VBox headerBox = new VBox(5);
        headerBox.getChildren().addAll(title, subtitle);

        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Table setup (no search, no pagination)
        table = new TableView<>();
        table.setEditable(true); // enable checkboxes to be interactive
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Class, String> colCode = new TableColumn<>("CODE");
        TableColumn<Class, String> colClassName = new TableColumn<>("CLASS NAME");
        TableColumn<Class, String> colTeacher = new TableColumn<>("TEACHER");
        TableColumn<Class, String> colSched = new TableColumn<>("SCHEDULE");
        TableColumn<Class, String> colRoom = new TableColumn<>("ROOM");

        // Selection column for multi-select delete
        TableColumn<Class, Boolean> colSelect = new TableColumn<>("");
        colSelect.setPrefWidth(40);
        colSelect.setCellValueFactory(cell -> {
            Class c = cell.getValue();
            if (c == null) return new SimpleBooleanProperty(false);
            SimpleBooleanProperty prop = classSelectionMap.get(c.getId());
            if (prop == null) {
                prop = new SimpleBooleanProperty(false);
                classSelectionMap.put(c.getId(), prop);
            }
            return prop;
        });
        colSelect.setEditable(true);
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));

        // Bind to properties
        colCode.setCellValueFactory(new PropertyValueFactory<>("classCode"));
        colClassName.setCellValueFactory(new PropertyValueFactory<>("className"));
        // display teacher full name if available, otherwise show teacherId
        colTeacher.setCellValueFactory(cell -> {
            Class c = cell.getValue();
            if (c == null) return new javafx.beans.property.SimpleStringProperty("");
            String tid = c.getTeacherId();
            if (tid == null) return new javafx.beans.property.SimpleStringProperty("");
            try {
                model.Teacher t = teacherDAO.getTeacherById(tid);
                if (t != null && t.getFullName() != null && !t.getFullName().isEmpty()) return new javafx.beans.property.SimpleStringProperty(t.getFullName());
            } catch (Exception ex) { /* ignore */ }
            return new javafx.beans.property.SimpleStringProperty(tid);
        });
        colSched.setCellValueFactory(new PropertyValueFactory<>("schedule"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("room"));

        // Responsive Column Widths
        colCode.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        colClassName.prefWidthProperty().bind(table.widthProperty().multiply(0.30));
        colTeacher.prefWidthProperty().bind(table.widthProperty().multiply(0.15));
        colSched.prefWidthProperty().bind(table.widthProperty().multiply(0.25));
        colRoom.prefWidthProperty().bind(table.widthProperty().multiply(0.14));

        table.getColumns().addAll(colSelect, colCode, colClassName, colTeacher, colSched, colRoom);

        // Controls
        HBox controls = new HBox(10);
        Button btnAdd = new Button("+ Add Class");
        btnAdd.getStyleClass().add("primary-button");
        
        Button btnEdit = new Button("Edit Selected");
        btnEdit.getStyleClass().add("secondary-button");
        
        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("secondary-button");

        // Spacer to push buttons to the right (optional design choice)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        controls.getChildren().addAll(spacer, btnDelete, btnEdit, btnAdd);

        card.getChildren().addAll(controls, table);
        layout.getChildren().addAll(headerBox, card);

        // Actions
        btnAdd.setOnAction(e -> showClassDialog(null));

        btnEdit.setOnAction(e -> {
            Class sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("No Selection", "Please select a class to edit.");
                return;
            }
            showClassDialog(sel);
        });

        btnDelete.setOnAction(evt -> {
            java.util.List<Integer> toDelete = new java.util.ArrayList<>();
            for (java.util.Map.Entry<Integer, SimpleBooleanProperty> en : classSelectionMap.entrySet()) {
                if (en.getValue().get()) toDelete.add(en.getKey());
            }

            Class single = table.getSelectionModel().getSelectedItem();
            if (toDelete.isEmpty() && single == null) {
                showAlert("No Selection", "Please select class(es) to delete.");
                return;
            }

            if (toDelete.isEmpty() && single != null) {
                toDelete.add(single.getId());
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Delete");
            confirm.setHeaderText(null);
            confirm.setContentText("Delete " + toDelete.size() + " class(es)?");

            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.OK) {
                    boolean allOk = true;
                    for (Integer id : toDelete) {
                        boolean ok = classDAO.deleteClass(id);
                        if (!ok) allOk = false;
                    }
                    refreshTable();
                    EventBus.fireClassChanged();
                    if (!allOk) showAlert("Partial Error", "Some deletions failed. Check console.");
                }
            });
        });

        // Initial load
        refreshTable();

        return layout;
    }

    private void refreshTable() {
        ObservableList<Class> items = FXCollections.observableArrayList(classDAO.getAllClasses());
        table.setItems(items);
        // reset selection map
        classSelectionMap.clear();
        for (Class c : items) {
            classSelectionMap.put(c.getId(), new SimpleBooleanProperty(false));
        }
    }

    private void showClassDialog(Class editing) {
        Dialog<Class> dialog = new Dialog<>();
        dialog.setTitle(editing == null ? "Add Class" : "Edit Class");

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField tfCode = new TextField();
        TextField tfName = new TextField();
        ComboBox<Teacher> cbTeacher = new ComboBox<>();
        TextField tfSchedule = new TextField();
        TextField tfRoom = new TextField();

        // populate teacher list
        try { cbTeacher.setItems(FXCollections.observableArrayList(teacherDAO.getAllTeachers())); } catch (Exception ex) { /* ignore */ }

        grid.add(new Label("Class Code:"), 0, 0);
        grid.add(tfCode, 1, 0);
        grid.add(new Label("Class Name:"), 0, 1);
        grid.add(tfName, 1, 1);
        grid.add(new Label("Teacher:"), 0, 2);
        grid.add(cbTeacher, 1, 2);
        grid.add(new Label("Schedule:"), 0, 3);
        grid.add(tfSchedule, 1, 3);
        grid.add(new Label("Room:"), 0, 4);
        grid.add(tfRoom, 1, 4);

        if (editing != null) {
            tfCode.setText(editing.getClassCode());
            tfCode.setDisable(true); // avoid changing code
            tfName.setText(editing.getClassName());
            // select teacher in combo if exists
            try {
                if (editing.getTeacherId() != null) {
                    Teacher tsel = teacherDAO.getTeacherById(editing.getTeacherId());
                    if (tsel != null) cbTeacher.getSelectionModel().select(tsel);
                }
            } catch (Exception ex) { /* ignore */ }
            tfSchedule.setText(editing.getSchedule());
            tfRoom.setText(editing.getRoom());
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtn) {
                String code = tfCode.getText().trim();
                String name = tfName.getText().trim();
                Teacher teacherSel = cbTeacher.getValue();
                String teacher = teacherSel == null ? null : teacherSel.getTeacherId();
                String sched = tfSchedule.getText().trim();
                String room = tfRoom.getText().trim();

                if (code.isEmpty() || name.isEmpty()) {
                    showAlert("Validation", "Class code and name are required.");
                    return null;
                }

                if (editing == null) {
                    Class c = new Class(code, name, teacher, sched, room);
                    boolean ok = classDAO.addClass(c);
                    if (!ok) showAlert("Error", "Failed to add class. Check console for details.");
                    refreshTable();
                    EventBus.fireClassChanged();
                    return c;
                } else {
                    editing.setClassName(name);
                    editing.setTeacherId(teacher);
                    editing.setSchedule(sched);
                    editing.setRoom(room);

                    boolean ok = classDAO.updateClass(editing);
                    if (!ok) showAlert("Error", "Failed to update class. Check console for details.");
                    refreshTable();
                    EventBus.fireClassChanged();
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
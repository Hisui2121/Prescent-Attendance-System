package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.StringConverter;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.text.Text; // added for wrapping text in table cells
// model / dao / util imports
import model.Enrollment;
import model.Student;
import model.Class;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.ClassDAO;
import util.EventBus;

public class EnrollmentUI {

    private TableView<Enrollment> table;
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private StudentDAO studentDAO = new StudentDAO();
    private ClassDAO classDAO = new ClassDAO();

    // Mode selector: choose which view to show
    private ComboBox<String> modeCombo;

    // Enrolled students list view
    private TableView<Enrollment> enrolledTable;
    private ObservableList<Enrollment> enrolledItems = FXCollections.observableArrayList();
    private java.util.Map<Integer, SimpleBooleanProperty> enrolledSelectionMap = new java.util.HashMap<>();

    // Filters for enrolled list
    private ComboBox<String> courseFilterCombo;
    private ComboBox<String> yearFilterCombo;
    private ComboBox<String> classFilterCombo;

    // Enroll-a-student view
    private TableView<Student> studentsTable;
    private ObservableList<Student> studentsList = FXCollections.observableArrayList();
    private java.util.Map<String, SimpleBooleanProperty> studentSelectionMap = new java.util.HashMap<>();
    private ComboBox<Class> classSelectCombo; // class to enroll into for enroll-mode

    private ComboBox<Student> studentDropdownField; // added to allow loadCombos to update
    private ComboBox<Class> classDropdownField;     // added to allow loadCombos to update

    // Replace complex multi-mode UI with a single enroll-student table view
    public VBox getView() {
        try {
            EventBus.addStudentChangeListener(this::loadCombos);
            EventBus.addClassChangeListener(this::loadCombos);
            EventBus.addEnrollmentChangeListener(this::refreshTable);

            VBox layout = new VBox(12);
            layout.setPadding(new Insets(20));

            Label title = new Label("Enrollment Management");
            title.setFont(Font.font("System", FontWeight.BOLD, 24));

            // Initialize combos that may be used/updated elsewhere to avoid NPEs
            courseFilterCombo = new ComboBox<>();
            courseFilterCombo.setPromptText("Course Filter");
            yearFilterCombo = new ComboBox<>();
            yearFilterCombo.setPromptText("Year Filter");
            classFilterCombo = new ComboBox<>();
            classFilterCombo.setPromptText("Class Filter");
            classSelectCombo = new ComboBox<>();
            classSelectCombo.setPromptText("Select Class");

            // Dropdowns to select a student and a class for enrollment
            HBox enrollControls = new HBox(10);
            studentDropdownField = new ComboBox<>();
            studentDropdownField.setPromptText("Select Student");
            classDropdownField = new ComboBox<>();
            classDropdownField.setPromptText("Select Class");
            Button enrollBtn = new Button("Enroll"); enrollBtn.getStyleClass().add("primary-button");
            Button deleteBtn = new Button("Delete Selected"); deleteBtn.getStyleClass().add("secondary-button");
            enrollControls.getChildren().addAll(new Label("Student:"), studentDropdownField, new Label("Class:"), classDropdownField, enrollBtn, deleteBtn);

            // Build enrolled table
            enrolledTable = new TableView<>();
            enrolledTable.setEditable(true);

            // Bind the table to the shared enrolledItems list so updates refresh the view
            enrolledTable.setItems(enrolledItems);
            // Use default cell sizing and default row factory to ensure rows render
            // increase fixed cell size so rows are taller and won't cut text
            enrolledTable.setFixedCellSize(48);
            enrolledTable.setRowFactory(tv -> {
                TableRow<Enrollment> row = new TableRow<>();
                row.setPrefHeight(48);
                return row;
            });
            enrolledTable.setPlaceholder(new Label("No enrollments"));

            TableColumn<Enrollment, Boolean> selectCol = new TableColumn<>("");
            selectCol.setPrefWidth(40);
            selectCol.setCellValueFactory(cell -> {
                Enrollment en = cell.getValue();
                if (en == null) return new SimpleBooleanProperty(false);
                int id = en.getId();
                SimpleBooleanProperty prop = enrolledSelectionMap.get(id);
                if (prop == null) { prop = new SimpleBooleanProperty(false); enrolledSelectionMap.put(id, prop); }
                return prop;
            });
            selectCol.setEditable(true);
            selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));

            TableColumn<Enrollment, Integer> idCol = new TableColumn<>("ENROLLMENT ID");
            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            // make id clickable via Hyperlink cell
            idCol.setCellFactory(col -> new TableCell<Enrollment, Integer>() {
                private final Hyperlink link = new Hyperlink();
                @Override protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); setText(null); }
                    else {
                        link.setText(String.valueOf(item));
                        link.setStyle("-fx-text-fill: black; -fx-underline: true;");
                        link.setOnAction(a -> {
                            Enrollment en = (Enrollment) getTableRow().getItem();
                            if (en == null) return;
                            String msg = "Enrollment ID: " + en.getId() + "\nStudent: " + lookupStudentName(en.getStudentId()) + "\nClass: " + lookupClassDisplay(en.getClassId()) + "\nDate: " + (en.getDateEnrolled()==null?"":en.getDateEnrolled());
                            showAlert("Enrollment Details", msg);
                        });
                        setGraphic(link);
                        setText(null);
                    }
                }
            });

            TableColumn<Enrollment, String> nameCol = new TableColumn<>("NAME");
            nameCol.setCellValueFactory(cell -> {
                Enrollment en = cell.getValue();
                if (en == null || en.getStudentId() == null) return new SimpleStringProperty("");
                return new SimpleStringProperty(lookupStudentName(en.getStudentId()));
            });
            // Add explicit Student ID column (shows raw student id) to ensure data is visible
            TableColumn<Enrollment, String> studentIdCol = new TableColumn<>("STUDENT ID");
            studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            studentIdCol.setPrefWidth(140);
             // use Text nodes to allow wrapping so long names won't be clipped
            nameCol.setCellFactory(col -> new TableCell<Enrollment, String>() {
                private final Text text = new Text();
                {
                    text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                    setGraphic(text);
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { text.setText(null); setGraphic(null); }
                    else { text.setText(item); setGraphic(text); }
                }
            });
              // set preferred widths to ensure columns are visible
              idCol.setPrefWidth(120);
              nameCol.setPrefWidth(220);

             TableColumn<Enrollment, String> classCol = new TableColumn<>("CLASS");
            classCol.setCellValueFactory(cell -> {
                Enrollment en = cell.getValue();
                if (en == null) return new SimpleStringProperty("");
                return new SimpleStringProperty(lookupClassDisplay(en.getClassId()));
            });
            classCol.setCellFactory(col -> new TableCell<Enrollment, String>() {
                private final Text text = new Text();
                {
                    text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                    setGraphic(text);
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { text.setText(null); setGraphic(null); }
                    else { text.setText(item); setGraphic(text); }
                }
            });
             classCol.setPrefWidth(240);

             TableColumn<Enrollment, String> dateCol = new TableColumn<>("DATE ENROLLED");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("dateEnrolled"));
             dateCol.setCellFactory(col -> new TableCell<Enrollment, String>() {
                 private final Text text = new Text();
                 {
                     text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                     setGraphic(text);
                 }
                 @Override protected void updateItem(String item, boolean empty) {
                     super.updateItem(item, empty);
                     if (empty || item == null) { text.setText(null); setGraphic(null); }
                     else { text.setText(item); setGraphic(text); }
                 }
             });
             dateCol.setPrefWidth(180);

             enrolledTable.getColumns().addAll(selectCol, idCol, studentIdCol, nameCol, classCol, dateCol);

            // Ensure internal table reference points to the enrolledTable so refreshTable can use it
            table = enrolledTable;

            // Force a refresh shortly after layout to ensure cells render
            Platform.runLater(() -> {
                try {
                    enrolledTable.refresh();
                    if (!enrolledItems.isEmpty()) System.out.println("DEBUG: first enrollment in UI: id="+enrolledItems.get(0).getId()+" student="+enrolledItems.get(0).getStudentId());
                } catch (Exception ex) { System.out.println("DEBUG: post-layout refresh failed: " + ex.getMessage()); }
            });

            // Apply explicit visual styles to ensure rows and text are visible regardless of external CSS
            enrolledTable.setStyle("-fx-background-color: white; -fx-control-inner-background: white;");
            enrolledTable.getStylesheets().removeIf(s -> s != null && s.contains("/css/")); // avoid app stylesheet interference

            // Ensure items are bound and visible after layout
            Platform.runLater(() -> {
                try {
                    enrolledTable.setItems(enrolledItems);
                    enrolledTable.refresh();
                    System.out.println("DEBUG: post-bind enrolledItems size=" + enrolledItems.size());
                } catch (Exception ex) { System.out.println("DEBUG: post-bind failed: " + ex.getMessage()); }
            });

            // Ensure TableView has a visible preferred height so rows render in constrained layouts
            enrolledTable.setMinHeight(200);
            enrolledTable.setPrefHeight(400);
             enrolledTable.setMaxHeight(Double.MAX_VALUE);
             // Ensure items are bound after columns are set
             enrolledTable.setItems(enrolledItems);

            // Adjust table preferred height when items change so rows become visible
            enrolledItems.addListener((javafx.collections.ListChangeListener.Change<? extends Enrollment> ch) -> {
                int size = enrolledItems.size();
                double newPref = Math.min(800, Math.max(200, size * enrolledTable.getFixedCellSize() + 100));
                Platform.runLater(() -> {
                    enrolledTable.setPrefHeight(newPref);
                    enrolledTable.refresh();
                });
            });

             // Make the table expand to fill the available space
             enrolledTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
             // allow the table to grow to fill parent VBox; set very large pref/max to force expansion
            enrolledTable.setPrefHeight(Double.MAX_VALUE);
            enrolledTable.setMaxHeight(Double.MAX_VALUE);
             VBox.setVgrow(enrolledTable, Priority.ALWAYS);

            // Use a BorderPane inside the VBox so the table occupies the full remaining area
            BorderPane bp = new BorderPane();
            VBox topBox = new VBox(6, title, enrollControls);
            topBox.setPadding(new Insets(0));
            bp.setTop(topBox);
            bp.setCenter(enrolledTable);
            bp.setPrefHeight(Double.MAX_VALUE);
            VBox.setVgrow(bp, Priority.ALWAYS);
            layout.getChildren().add(bp);

            // Wire controls
            enrollBtn.setOnAction(a -> {
                Student s = studentDropdownField.getValue();
                Class c = classDropdownField.getValue();
                if (s == null) { showAlert("Validation","Select a student"); return; }
                if (c == null) { showAlert("Validation","Select a class"); return; }
                boolean ok = enrollmentDAO.enrollStudent(new Enrollment(s.getStudentId(), c.getId()));
                if (ok) {
                    showAlert("Success","Student enrolled");
                    // clear selection
                    studentDropdownField.getSelectionModel().clearSelection();
                    classDropdownField.getSelectionModel().clearSelection();
                    // refresh
                    loadCombos(); refreshTable(); EventBus.fireEnrollmentChanged();
                } else showAlert("Error","Enrollment failed");
            });

            deleteBtn.setOnAction(a -> {
                java.util.List<Integer> toRemove = new java.util.ArrayList<>();
                for (java.util.Map.Entry<Integer, SimpleBooleanProperty> en : enrolledSelectionMap.entrySet()) if (en.getValue().get()) toRemove.add(en.getKey());
                if (toRemove.isEmpty()) { showAlert("No Selection","Please check enrollments to delete."); return; }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION); confirm.setTitle("Confirm Delete"); confirm.setHeaderText(null); confirm.setContentText("Delete " + toRemove.size() + " enrollments?");
                confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) { boolean allOk=true; for (Integer id : toRemove) if (!enrollmentDAO.removeEnrollment(id)) allOk=false; loadCombos(); refreshTable(); EventBus.fireEnrollmentChanged(); if (!allOk) showAlert("Partial Error","Some deletions failed."); }});
            });

            // initialize dropdowns with current DAO data; loadCombos will keep these updated
            studentsList = FXCollections.observableArrayList(studentDAO.getAllStudents());
            studentDropdownField.setItems(studentsList);
            ObservableList<Class> classes = FXCollections.observableArrayList(classDAO.getAllClasses());
            classDropdownField.setItems(classes);

            // initial load
            loadCombos();
            refreshTable();

            return layout;
        } catch (Exception ex) {
            ex.printStackTrace();
            VBox err = new VBox(10);
            err.setPadding(new Insets(20));
            Label l = new Label("Failed to load Enrollment UI: " + (ex.getMessage() == null ? "(see console)" : ex.getMessage()));
            err.getChildren().add(l);
            Platform.runLater(() -> { try { showAlert("Error", "Failed to initialize Enrollment page. See console for details."); } catch (Exception ignore) {} });
            return err;
        }
    }

    private void loadCombos() {
         try {
            // update the existing studentsList so UI bindings remain intact
            java.util.List<Student> sdao = studentDAO.getAllStudents();
            if (studentsList == null) studentsList = FXCollections.observableArrayList(sdao); else { studentsList.clear(); studentsList.addAll(sdao); }
            if (studentDropdownField != null) studentDropdownField.setItems(studentsList);

             // populate selection map
             studentSelectionMap.clear();
             for (Student s : studentsList) {
                 studentSelectionMap.put(s.getStudentId(), new SimpleBooleanProperty(false));
             }

            ObservableList<Class> classes = FXCollections.observableArrayList(classDAO.getAllClasses());
            // guard against null combo
            if (classSelectCombo != null) classSelectCombo.setItems(classes);
            if (classDropdownField != null) classDropdownField.setItems(classes);
             // classCombo.setItems(classes); // classCombo was removed; classSelectCombo is the combo used for selecting a Class to enroll into

            // For filters, distinct values only
            ObservableList<String> allCourses = FXCollections.observableArrayList();
            ObservableList<String> allYears = FXCollections.observableArrayList();
            ObservableList<String> allClassCodes = FXCollections.observableArrayList();
            // Build filter lists based on current enrollments and student info
            java.util.List<Enrollment> enrolls = enrollmentDAO.getAllEnrollments();
            for (Enrollment en : enrolls) {
                // add class codes from enrollment
                if (en.getClassId() > 0) {
                    String code = lookupClassDisplay(en.getClassId());
                    if (code != null && !allClassCodes.contains(code)) allClassCodes.add(code);
                }
                // find student info for course/year
                for (Student st : studentsList) {
                    if (st.getStudentId() != null && st.getStudentId().equals(en.getStudentId())) {
                        if (st.getCourse() != null && !allCourses.contains(st.getCourse())) allCourses.add(st.getCourse());
                        if (st.getYearLevel() != null && !allYears.contains(st.getYearLevel())) allYears.add(st.getYearLevel());
                        break;
                    }
                }
            }
            if (courseFilterCombo != null) courseFilterCombo.setItems(allCourses);
            if (yearFilterCombo != null) yearFilterCombo.setItems(allYears);
            if (classFilterCombo != null) classFilterCombo.setItems(allClassCodes);

        } catch (Exception ex) {
            System.out.println("DEBUG: loadCombos error: " + ex.getMessage());
        }
      }
 
      private void refreshTable() {
          // Run on FX thread to ensure TableView updates correctly
         Platform.runLater(() -> {
             try {
            if (table == null) {
                // fallback to enrolledTable if table wasn't wired yet
                table = enrolledTable;
                if (table == null) {
                    System.out.println("DEBUG: refreshTable called before table initialized; ignoring.");
                    return;
                }
            }

             java.util.List<Enrollment> list = enrollmentDAO.getAllEnrollments();
              System.out.println("DEBUG: enrollmentDAO.getAllEnrollments returned " + (list == null ? 0 : list.size()) + " items");
              if (list != null) {
                  for (int i = 0; i < Math.min(10, list.size()); i++) {
                      Enrollment en = list.get(i);
                      System.out.println("  DAO row " + i + ": id=" + en.getId() + " student_id=" + en.getStudentId() + " class_id=" + en.getClassId() + " date=" + en.getDateEnrolled());
                  }
              }
 
              // update the bound list so UI updates properly
              if (list == null) enrolledItems.clear(); else { enrolledItems.setAll(list); }
            // ensure the table is bound and refreshed with the same list
            enrolledTable.setItems(enrolledItems);
            System.out.println("DEBUG: enrolledItems size after update = " + enrolledItems.size());

             // no per-row checkbox map anymore; multi-delete uses table selection

             // Force a rendering refresh and nudge column visibility to ensure cells are painted
             try {
                 System.out.println("DEBUG: after table.setItems, before refresh, table.getItems().size()=" + table.getItems().size());
                 table.refresh();
                 // toggle columns visibility to force layout repaint
                 for (TableColumn<?, ?> col : table.getColumns()) {
                     col.setVisible(false);
                     col.setVisible(true);
                 }
                 table.layout();
                 System.out.println("DEBUG: after table.refresh/layout");
             } catch (Exception ex) {
                 System.out.println("DEBUG: refresh/layout failed: " + ex.getMessage());
             }

             // If there are items, select and scroll to the most recent (last) entry so the user sees newly enrolled students
             if (enrolledItems != null && !enrolledItems.isEmpty()) {
                 int last = enrolledItems.size() - 1;
                 table.getSelectionModel().select(last);
                 table.scrollTo(last);
             }
            } catch (Exception ex) {
                System.out.println("DEBUG: refreshTable error: " + ex.getMessage());
            }
         });
      }

    private String lookupStudentName(String studentId) {
        for (Student s : studentDAO.getAllStudents()) {
            if (s.getStudentId().equals(studentId)) return s.getFullName();
        }
        return studentId;
    }

    private String lookupClassCode(int classId) {
        for (Class c : classDAO.getAllClasses()) {
            if (c.getId() == classId) return c.getClassCode();
        }
        return String.valueOf(classId);
    }

    // New: return "CODE - NAME" when available
    private String lookupClassDisplay(int classId) {
        for (Class c : classDAO.getAllClasses()) {
            if (c.getId() == classId) {
                if (c.getClassCode() != null && c.getClassName() != null) return c.getClassCode() + " - " + c.getClassName();
                if (c.getClassCode() != null) return c.getClassCode();
                if (c.getClassName() != null) return c.getClassName();
                return String.valueOf(classId);
            }
        }
        return String.valueOf(classId);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void applyEnrolledFilters() {
        try {
            String courseFilter = courseFilterCombo.getValue();
            String yearFilter = yearFilterCombo.getValue();
            String classFilter = classFilterCombo.getValue();

            java.util.List<Enrollment> all = enrollmentDAO.getAllEnrollments();
            java.util.List<Enrollment> filtered = new java.util.ArrayList<>();
            for (Enrollment en : all) {
                boolean keep = true;
                if (courseFilter != null && !courseFilter.isEmpty()) {
                    String sc = lookupStudentCourse(en.getStudentId());
                    if (sc == null || !sc.equals(courseFilter)) keep = false;
                }
                if (yearFilter != null && !yearFilter.isEmpty()) {
                    String sy = lookupStudentYear(en.getStudentId());
                    if (sy == null || !sy.equals(yearFilter)) keep = false;
                }
                if (classFilter != null && !classFilter.isEmpty()) {
                    String cd = lookupClassDisplay(en.getClassId());
                    if (cd == null || !cd.equals(classFilter)) keep = false;
                }
                if (keep) filtered.add(en);
            }
            // Update bound list so the table stays bound to enrolledItems
            enrolledItems.setAll(filtered);

         } catch (Exception ex) {
             System.out.println("DEBUG: applyEnrolledFilters error: " + ex.getMessage());
         }
     }

    private String lookupStudentCourse(String studentId) {
        // Lookup course from Student record
        for (Student s : studentDAO.getAllStudents()) {
            if (s.getStudentId() != null && s.getStudentId().equals(studentId)) return s.getCourse();
        }
        return null;
    }

    private String lookupStudentYear(String studentId) {
        // Lookup year level from Student record
        for (Student s : studentDAO.getAllStudents()) {
            if (s.getStudentId() != null && s.getStudentId().equals(studentId)) return s.getYearLevel();
        }
        return null;
    }
 }
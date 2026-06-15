package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import dao.AttendanceRecordDAO;
import dao.UserDAO;
import model.AttendanceRecord;
import model.User;
import util.EventBus;

public class NotificationUI {

    private TableView<AttendanceRecord> table;
    private AttendanceRecordDAO recordDAO = new AttendanceRecordDAO();
    private ObservableList<AttendanceRecord> data = FXCollections.observableArrayList();
    private String teacherId = null;

    public NotificationUI() {}

    public NotificationUI(String teacherId) {
        this.teacherId = teacherId;
    }

    public VBox getView() {
        VBox layout = new VBox(12);
        layout.setPadding(new Insets(12));

        Label title = new Label("Notifications");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(title);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AttendanceRecord, String> colId = new TableColumn<>("STUDENT ID");
        TableColumn<AttendanceRecord, String> colName = new TableColumn<>("FULL NAME");
        TableColumn<AttendanceRecord, String> colClass = new TableColumn<>("CLASS NAME / CODE");
        TableColumn<AttendanceRecord, String> colProf = new TableColumn<>("PROFESSOR");
        TableColumn<AttendanceRecord, String> colAlert = new TableColumn<>("ALERT TYPE");
        TableColumn<AttendanceRecord, Void> colAction = new TableColumn<>("ACTION");

        colId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colClass.setCellValueFactory(cell -> {
            AttendanceRecord r = cell.getValue();
            String val = "";
            if (r.getClassName() != null && !r.getClassName().isEmpty()) {
                val = r.getClassName();
            }
            if (r.getClassCode() != null && !r.getClassCode().isEmpty()) {
                if (!val.isEmpty()) val += " / ";
                val += r.getClassCode();
            }
            return new javafx.beans.property.SimpleStringProperty(val);
        });
        colProf.setCellValueFactory(new PropertyValueFactory<>("professorName"));
        colAlert.setCellValueFactory(new PropertyValueFactory<>("alertType"));

        // Action button cell
        Callback<TableColumn<AttendanceRecord, Void>, TableCell<AttendanceRecord, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<AttendanceRecord, Void> call(final TableColumn<AttendanceRecord, Void> param) {
                final TableCell<AttendanceRecord, Void> cell = new TableCell<>() {

                    private final Button btn = new Button("Mark as Notified");

                    {
                        btn.getStyleClass().add("secondary-button");
                        btn.setOnAction((e) -> {
                            AttendanceRecord r = getTableView().getItems().get(getIndex());
                            if (r == null) return;
                            boolean ok = recordDAO.clearAbsenceNotifications(r.getStudentId(), r.getClassId());
                            if (ok) {
                                // notify other UI components (dashboard badge) to refresh
                                EventBus.fireNotificationChanged();
                                showAlert("Marked", "Notification cleared for " + r.getStudentId());
                                refresh();
                            } else {
                                showAlert("Error", "Unable to clear notification. See console.");
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        colAction.setCellFactory(cellFactory);
        colAction.setPrefWidth(160);

        // Width proportions
        colId.setMaxWidth(1f * Integer.MAX_VALUE * 12); // 12%
        colName.setMaxWidth(1f * Integer.MAX_VALUE * 30);
        colClass.setMaxWidth(1f * Integer.MAX_VALUE * 28);
        colProf.setMaxWidth(1f * Integer.MAX_VALUE * 18);
        colAlert.setMaxWidth(1f * Integer.MAX_VALUE * 12);

        table.getColumns().addAll(colId, colName, colClass, colProf, colAlert, colAction);

        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        layout.getChildren().addAll(header, table);

        refresh();
        return layout;
    }

    private void refresh() {
        data.clear();
        try {
            java.util.ArrayList<AttendanceRecord> list;
            if (teacherId != null && !teacherId.isEmpty()) {
                list = recordDAO.getLowAttendanceStudentsForTeacher(teacherId, 3);
            } else {
                list = recordDAO.getLowAttendanceStudents(3);
            }
             if (list != null) data.addAll(list);
        } catch (Exception ex) {
            System.out.println("Notification refresh error: " + ex.getMessage());
        }

        // Ensure professorName is populated; fallback to users table if necessary
        try {
            UserDAO udao = new UserDAO();
            for (AttendanceRecord r : data) {
                if ((r.getProfessorName() == null || r.getProfessorName().isEmpty()) && r.getProfessorId() != null) {
                    User u = udao.getUserByUsername(r.getProfessorId());
                    if (u != null && u.getFullName() != null && !u.getFullName().isEmpty()) {
                        r.setProfessorName(u.getFullName());
                    } else {
                        // fallback to professorId string so UI shows something
                        r.setProfessorName(r.getProfessorId());
                    }
                }
            }
        } catch (Exception ex) {
            // ignore lookup errors
        }

        table.setItems(data);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
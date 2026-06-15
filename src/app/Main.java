// =========================================================
// ATTENDANCE MANAGEMENT SYSTEM
// Developed by: ACE
// Members: Bryze Ayapana, Jade Casano, Jaedee Manalang, Ritz Paredes
// =========================================================
package app;

import javafx.application.Application;
import javafx.stage.Stage;
import ui.LoginUI;
import ui.DashboardUI;
import javafx.scene.image.Image;

public class Main extends Application {
    private Stage window;
    private static Main instance; // singleton reference

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        instance = this;
        window.setTitle("ACE University Portal");
        
        database.DBInitialize.initialize();


        try {
            java.io.InputStream is = getClass().getResourceAsStream("/assets/logo.png");
            if (is == null) {
                // fallback to legacy jpg
                is = getClass().getResourceAsStream("/assets/logo.jpg");
            }
            if (is != null) {
                Image icon = new Image(is);
                window.getIcons().add(icon);
            }
        } catch (Exception ex) {
            System.out.println("Icon load failed: " + ex.getMessage());
        }
        
        // Load Login First
        LoginUI login = new LoginUI();
        window.setScene(login.getScene(this)); 
        window.centerOnScreen();
        window.show();
    }

    public static Main getInstance() {
        return instance;
    }
    
    public void showLoginScene() {
        try {
            LoginUI login = new LoginUI();
            window.setScene(login.getScene(this));
            window.centerOnScreen();
        } catch (Exception ex) {
            System.out.println("Error showing login scene: " + ex.getMessage());
        }
    }

    public void loadDashboard() {
        service.AuthenticationService auth = service.AuthenticationService.getInstance();
        // If the logged-in user is a teacher, load the TeacherUI. Otherwise, load the Admin DashboardUI.
        try {
            model.Role role = auth.getCurrentRole();
            if (role == model.Role.TEACHER) {
                ui.TeacherUI teacherUI = new ui.TeacherUI();
                window.setScene(teacherUI.getScene());
            } else {
                ui.DashboardUI dashboard = new ui.DashboardUI();
                window.setScene(dashboard.getScene());
            }
            window.centerOnScreen();
        } catch (Exception ex) {
            // Fallback to admin dashboard on any unexpected error
            ui.DashboardUI dashboard = new ui.DashboardUI();
            window.setScene(dashboard.getScene());
            window.centerOnScreen();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package app;

import javafx.stage.Stage;

public class AppContext {
    private static Stage primaryStage;
    private static Main mainApp;

    public static void setPrimaryStage(Stage stage) { primaryStage = stage; }
    public static Stage getPrimaryStage() { return primaryStage; }

    public static void setMain(Main m) { mainApp = m; }
    public static Main getMain() { return mainApp; }
}

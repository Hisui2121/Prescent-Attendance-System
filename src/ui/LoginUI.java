package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import app.Main; // Para makatawag tayo pabalik sa Main class para mag-switch scene

public class LoginUI {

    public Scene getScene(Main mainApp) {
        // Main container (StackPane for background image)
        StackPane root = new StackPane();
        root.getStyleClass().add("login-bg");

        // Split Layout using HBox
        HBox splitLayout = new HBox();
        splitLayout.setAlignment(Pos.CENTER);

        // --- LEFT SIDE (Branding) ---
        VBox leftSide = new VBox(10);
        leftSide.setAlignment(Pos.CENTER_LEFT);
        leftSide.setPadding(new Insets(50));
        HBox.setHgrow(leftSide, Priority.ALWAYS); // Takes up remaining space
        
        try {
            // Siguraduhing tama ang pangalan ng file mo dito
            javafx.scene.image.Image logoImage = new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/logo.png"));
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logoImage);
            logoView.setFitWidth(100); // Pwede mong lakihan o liitan ito
            logoView.setPreserveRatio(true);
            leftSide.getChildren().add(logoView); // I-add muna ang logo sa VBox
        } catch (Exception e) {
            System.out.println("Logo not found in assets folder.");
        }

        Label brandTitle = new Label("MyAcePortal");
        brandTitle.setFont(Font.font("System", FontWeight.BOLD, 48));
        brandTitle.setStyle("-fx-text-fill: #FFC107; -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.8), 5, 0, 0, 2);");
        
        Label brandSub = new Label("Sign in to your account");
        brandSub.setFont(Font.font("System", FontWeight.BOLD, 20));
        brandSub.setStyle("-fx-text-fill: white; -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.8), 5, 0, 0, 2);");

        leftSide.getChildren().addAll(brandTitle, brandSub);

        // --- RIGHT SIDE (Login Form) ---
        VBox rightSide = new VBox();
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPadding(new Insets(0, 80, 0, 40));

        // The White Card
        VBox loginCard = new VBox(20);
        loginCard.getStyleClass().add("login-card");
        loginCard.setMaxWidth(450);
        loginCard.setAlignment(Pos.TOP_LEFT);

        Label lblSignIn = new Label("Sign In");
        lblSignIn.setFont(Font.font("System", FontWeight.BOLD, 28));
        
        Label lblWelcome = new Label("Welcome back admin! To get started, enter the following:");
        lblWelcome.setWrapText(true);
        lblWelcome.setStyle("-fx-text-fill: #475569;");

        VBox emailBox = new VBox(5);
        Label lblEmail = new Label("Email Address");
        lblEmail.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        TextField txtEmail = new TextField();
        txtEmail.getStyleClass().add("login-input");
        emailBox.getChildren().addAll(lblEmail, txtEmail);

        VBox passBox = new VBox(5);
        Label lblPass = new Label("Password");
        lblPass.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        PasswordField txtPass = new PasswordField();
        txtPass.getStyleClass().add("login-input");
        passBox.getChildren().addAll(lblPass, txtPass);

        Button btnLogin = new Button("Sign in");
        btnLogin.getStyleClass().add("dark-button");
        
        // LOGIN ACTION LOGIC
        btnLogin.setOnAction(e -> {
            service.AuthenticationService auth = service.AuthenticationService.getInstance();
            String user = txtEmail.getText().trim();
            String pass = txtPass.getText();
            boolean ok = auth.login(user, pass);
            if (ok) {
                mainApp.loadDashboard();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid Credentials!");
                alert.show();
            }
            
        });

        loginCard.getChildren().addAll(lblSignIn, lblWelcome, emailBox, passBox, btnLogin);
        rightSide.getChildren().add(loginCard);

        splitLayout.getChildren().addAll(leftSide, rightSide);
        root.getChildren().add(splitLayout);

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        return scene;
    }
}
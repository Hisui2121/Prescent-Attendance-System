package ui;

import java.util.Scanner;
import service.AuthenticationService;
import model.Role;
import model.RolePermission;
import util.Logger;

public class LoginUI {
    
    private Scanner scanner;
    private AuthenticationService authService;
    
    public LoginUI(Scanner scanner, AuthenticationService authService) {
        this.scanner = scanner;
        this.authService = authService;
    }
    
    public boolean showLoginMenu() {
        boolean running = true;
        
        while (running) {
            System.out.println("\n");
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║  PRESENCE ATTENDANCE SYSTEM - LOGIN    ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println("\n1. Login");
            System.out.println("2. View Available Roles");
            System.out.println("3. Exit System");
            System.out.print("\nEnter Choice: ");
            
            int choice = getIntInput();
            
            switch (choice) {
                case 1:
                    if (performLogin()) {
                        return true;
                    }
                    break;
                    
                case 2:
                    showAvailableRoles();
                    break;
                    
                case 3:
                    System.out.println("\n✓ Exiting system. Goodbye!");
                    return false;
                    
                default:
                    System.out.println("\n✗ Invalid choice! Please try again.");
            }
        }
        
        return false;
    }
    
    private boolean performLogin() {
        System.out.print("\nEnter Username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();
        
        if (authService.login(username, password)) {
            return true;
        } else {
            System.out.println("Please try again or check credentials.");
            return false;
        }
    }
    
    private void showAvailableRoles() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║         AVAILABLE USER ROLES           ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        for (Role role : Role.values()) {
            System.out.println("📋 " + role.name() + " - " + role.getDescription());
            RolePermission.printPermissions(role);
            System.out.println();
        }
    }
    
    private int getIntInput() {
        try {
            int value = scanner.nextInt();
            scanner.nextLine();
            return value;
        } catch (Exception e) {
            scanner.nextLine();
            return -1;
        }
    }
}

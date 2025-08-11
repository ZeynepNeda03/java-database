package com.zeynepneda.userdata;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        File file = new File("user.bin");
        UserService userService = new UserService();

        while (true) {
            System.out.println("\nMENU");
            System.out.println("1. Add User");
            System.out.println("2. Search User");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int choice;
            try {
                choice = Integer.parseInt(scan.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.print("Please enter username: ");
                    String username = scan.nextLine().trim();
                    if (username.isEmpty()) {
                        System.out.println("Username cannot be empty");
                        break;
                    }

                    System.out.print("Please enter email: ");
                    String email = scan.nextLine().trim();
                    if (email.isEmpty()) {
                        System.out.println("Email cannot be empty");
                        break;
                    }

                    try {
                        userService.addUser(username, email, file);
                    } catch (IOException e) {
                        System.out.println("Error adding user: " + e.getMessage());
                    }
                    break;

                case 2:
                    System.out.print("Enter username or ID to search: ");
                    String searchInput = scan.nextLine().trim();
                    if (searchInput.isEmpty()) {
                        System.out.println("Search input cannot be empty");
                        break;
                    }

                    Object input;
                    try {
                        input = Integer.valueOf(searchInput);
                    } catch (NumberFormatException e) {
                        input = searchInput;
                    }

                    try {
                        int foundId = userService.findUser(input, file);
                        if (foundId > 0) {
                            while (true) {
                                System.out.println("\n1. Edit User");
                                System.out.println("2. Delete User");
                                System.out.println("3. Return to Main Menu");
                                System.out.print("Choose an option: ");

                                String subChoice = scan.nextLine().trim();
                                if (subChoice.equals("1")) {
                                    System.out.print("Enter new username: ");
                                    String newUsername = scan.nextLine().trim();
                                    System.out.print("Enter new email: ");
                                    String newEmail = scan.nextLine().trim();
                                    userService.editUser(foundId, newUsername, newEmail, file);
                                } else if (subChoice.equals("2")) {
                                    userService.deleteUser(foundId, file);
                                    break;
                                } else if (subChoice.equals("3")) {
                                    break;
                                } else {
                                    System.out.println("Invalid choice!");
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Error searching user: " + e.getMessage());
                    }
                    break;

                case 3:
                    System.out.println("Exiting...");
                    scan.close();
                    return;

                default:
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }
        }
    }
}

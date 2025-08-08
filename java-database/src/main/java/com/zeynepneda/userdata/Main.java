package com.zeynepneda.userdata;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        File file = new File("user.bin");
        UserService userService = new UserService();

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Add User");
            System.out.println("2. Search User");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int choose = scan.nextInt();
            scan.nextLine();

            switch (choose) {
                case 1:
                    System.out.println("Please enter username:");
                    String username = scan.nextLine();
                    System.out.println("Please enter email:");
                    String email = scan.nextLine();
                    userService.addUser(username, email, file);
                    break;
                case 2:
                    System.out.println("Enter username or ID to search:");
                    String searchInput = scan.next();
                    Object input;
                    try {
                        input = Integer.valueOf(searchInput);
                    } catch (NumberFormatException e) {
                        input = searchInput;
                    }
                    userService.findUser(input, file);
                    break;

                case 3:
                    System.out.println("Exiting...");
                    scan.close();
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }
        }
    }

}

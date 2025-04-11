package org.example.client;

import org.example.front_end.DSMSInterface;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.URL;
import java.util.Scanner;

public class DSMSClientController {

    private DSMSInterface dsms;
    private final Scanner scanner = new Scanner(System.in);

    public void startClient() {
        connectToFrontEnd();

        System.out.print("Enter your User ID (e.g., NYKA1234 / TOKB5678): ");
        String userID = scanner.nextLine().toUpperCase().trim();

        if (!userID.matches("^(NYK|TOK|LON)(A|B)\\d{4}$")) {
            System.out.println("Invalid User ID format.");
            return;
        }

        if (userID.contains("A")) {
            handleAdmin(userID);
        } else {
            handleBuyer(userID);
        }
    }

    private void connectToFrontEnd() {
        try {
            URL url = new URL("http://192.168.230.151:4555/FrontEnd?wsdl");
            QName qname = new QName("http://front_end.example.org/", "DSMSInterfaceImplService");
            Service service = Service.create(url, qname);
            dsms = service.getPort(DSMSInterface.class);
        } catch (Exception e) {
            System.err.println("Could not connect to Front-End: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleAdmin(String userID) {
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add Share");
            System.out.println("2. Remove Share");
            System.out.println("3. List Share Availability");
            System.out.println("4. Exit");
            System.out.print("Select option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter Share ID: ");
                    String shareID = scanner.nextLine();
                    System.out.print("Enter Share Type: ");
                    String shareType = scanner.nextLine();
                    System.out.print("Enter Capacity: ");
                    int cap = scanner.nextInt();
                    scanner.nextLine();
                    System.out.println(dsms.addShare(userID, shareID, shareType, cap));
                }
                case 2 -> {
                    System.out.print("Enter Share ID: ");
                    String shareID = scanner.nextLine();
                    System.out.print("Enter Share Type: ");
                    String shareType = scanner.nextLine();
                    System.out.println(dsms.removeShare(userID, shareID, shareType));
                }
                case 3 -> {
                    System.out.print("Enter Share Type: ");
                    String shareType = scanner.nextLine();
                    System.out.println(dsms.listShareAvailability(userID, shareType));
                }
                case 4 -> System.exit(0);
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void handleBuyer(String userID) {
        while (true) {
            System.out.println("\n--- Buyer Menu ---");
            System.out.println("1. Purchase Share");
            System.out.println("2. Get My Shares");
            System.out.println("3. Sell Share");
            System.out.println("4. Swap Shares");
            System.out.println("5. Exit");
            System.out.print("Select option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter Share ID: ");
                    String shareID = scanner.nextLine();
                    System.out.print("Enter Share Type: ");
                    String shareType = scanner.nextLine();
                    System.out.print("Enter Quantity: ");
                    int qty = scanner.nextInt();
                    scanner.nextLine();
                    System.out.println(dsms.purchaseShare(userID, userID, shareID, shareType, qty));
                }
                case 2 -> {
                    System.out.println(dsms.getShares(userID, userID));
                }
                case 3 -> {
                    System.out.print("Enter Share ID: ");
                    String shareID = scanner.nextLine();
                    System.out.print("Enter Quantity: ");
                    int qty = scanner.nextInt();
                    scanner.nextLine();
                    System.out.println(dsms.sellShare(userID, userID, shareID, qty));
                }
                case 4 -> {
                    System.out.print("Old Share ID: ");
                    String oldID = scanner.nextLine();
                    System.out.print("Old Share Type: ");
                    String oldType = scanner.nextLine();
                    System.out.print("New Share ID: ");
                    String newID = scanner.nextLine();
                    System.out.print("New Share Type: ");
                    String newType = scanner.nextLine();
                    System.out.println(dsms.swapShare(userID, userID, oldID, oldType, newID, newType));
                }
                case 5 -> System.exit(0);
                default -> System.out.println("Invalid option.");
            }
        }
    }
}

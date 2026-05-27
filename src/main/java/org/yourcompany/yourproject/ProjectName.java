package org.yourcompany.yourproject;

import java.time.LocalDate;
import java.util.List;

public class ProjectName {
    public static void main(String[] args) {
        ProductService products = new ProductService();
        CartService cart = new CartService();
        OrderService orders = new OrderService();
        InventoryService inventory = new InventoryService();
        ShippingNoticeService shipping = new ShippingNoticeService();
        ManagerService manager = new ManagerService();

        try {
            System.out.println("========================================");
            System.out.println("   FULL eMART + eDEPOT INTEGRATION TEST");
            System.out.println("========================================");

            System.out.println("\n=== 1. Product Search ===");
            products.listAllProducts();

            System.out.println("\nSearch by manufacturer: HP");
            products.searchByManufacturer("HP");

            System.out.println("\nSearch by category: Laptop");
            products.searchByCategory("Laptop");

            System.out.println("\n=== 2. Cart + Checkout ===");
            cart.clearCart("Lkim");

            cart.addItem("Lkim", "AA00101", 1);
            cart.addItem("Lkim", "AA00201", 1);

            cart.displayCart("Lkim");

            System.out.println("\n=== 3. Inventory Before Fill ===");
            int laptopBeforeFill = inventory.getInventoryQuantity("AA00101");
            int desktopBeforeFill = inventory.getInventoryQuantity("AA00201");

            System.out.println("AA00101 before fill: " + laptopBeforeFill);
            System.out.println("AA00201 before fill: " + desktopBeforeFill);

            System.out.println("\n=== 4. Checkout Creates Pending Order ===");
            Integer createdOrderId = orders.checkout("Lkim");

            if (createdOrderId == null) {
                System.out.println("Checkout failed. Stopping demo.");
                return;
            }

            System.out.println("Created order ID: " + createdOrderId);
            orders.displayOrder(createdOrderId);

            System.out.println("\n=== 5. eDEPOT Fill Order ===");
            inventory.fillOrder(createdOrderId);

            System.out.println("\n=== 6. Inventory After Fill ===");
            int laptopAfterFill = inventory.getInventoryQuantity("AA00101");
            int desktopAfterFill = inventory.getInventoryQuantity("AA00201");

            System.out.println("AA00101 after fill: " + laptopAfterFill);
            System.out.println("AA00201 after fill: " + desktopAfterFill);

            System.out.println("\nExpected AA00101 decrease: 1");
            System.out.println("Actual AA00101 decrease: " + (laptopBeforeFill - laptopAfterFill));

            System.out.println("\nExpected AA00201 decrease: 1");
            System.out.println("Actual AA00201 decrease: " + (desktopBeforeFill - desktopAfterFill));

            System.out.println("\n=== 7. Order After Fill Should Be FILLED ===");
            orders.displayOrder(createdOrderId);

            System.out.println("\n=== 8. Double Fill Protection Test ===");
            System.out.println("Trying to fill the same order again. This should be refused.");

            try {
                inventory.fillOrder(createdOrderId);
                System.out.println("ERROR: Double fill was allowed.");
            } catch (IllegalStateException e) {
                System.out.println("Correctly refused double fill: " + e.getMessage());
            }

            System.out.println("\n=== 9. Shipping Notice + Receive Shipment ===");

            String noticeId = "N" + System.currentTimeMillis();

            List<NoticeItem> noticeItems = List.of(
                new NoticeItem("HP", "K435", 2)
            );

            int cameraBeforeNotice = inventory.getInventoryQuantity("AA00601");
            System.out.println("AA00601 before shipping notice/receive: " + cameraBeforeNotice);

            shipping.receiveShippingNotice(
                noticeId,
                "HP",
                "UPS",
                noticeItems
            );

            System.out.println("Inserted shipping notice: " + noticeId);

            shipping.receiveShipment(noticeId);

            System.out.println("Received shipment for notice: " + noticeId);

            int cameraAfterReceive = inventory.getInventoryQuantity("AA00601");
            System.out.println("AA00601 after receive: " + cameraAfterReceive);
            System.out.println("Expected AA00601 increase from shipment: 2");
            System.out.println("Actual AA00601 increase: " + (cameraAfterReceive - cameraBeforeNotice));

            System.out.println("\n=== 10. Manager Report ===");

            LocalDate today = LocalDate.now();

            manager.printMonthlySalesReport(
                today.getYear(),
                today.getMonthValue()
            );

            System.out.println("\n=== 11. Manager Price Change ===");

            manager.changePrice(
                "AA00101",
                1599.99
            );

            System.out.println("\n=== 12. Manual Customer Status Update ===");

            manager.updateCustomerStatusManually(
                "Lkim",
                "GOLD"
            );

            System.out.println("\n========================================");
            System.out.println("   INTEGRATION TEST FINISHED");
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("Integration test failed:");
            e.printStackTrace();
        }
    }
}

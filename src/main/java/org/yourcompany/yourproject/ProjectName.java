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

            System.out.println("\nSearch by manufacturer: Logitech");
            products.searchByManufacturer("Logitech");

            System.out.println("\nSearch by category: computers");
            products.searchByCategory("computers");

            System.out.println("\n=== 2. Cart + Checkout ===");
            cart.clearCart("C001");

            cart.addItem("C001", "CP00001", 1);
            cart.addItem("C001", "KB00001", 2);

            cart.displayCart("C001");

            System.out.println("\n=== 3. Inventory Before Fill ===");
            int cpBeforeFill = inventory.getInventoryQuantity("CP00001");
            int kbBeforeFill = inventory.getInventoryQuantity("KB00001");

            System.out.println("CP00001 before fill: " + cpBeforeFill);
            System.out.println("KB00001 before fill: " + kbBeforeFill);

            System.out.println("\n=== 4. Checkout Creates Pending Order ===");
            Integer createdOrderId = orders.checkout("C001");

            if (createdOrderId == null) {
                System.out.println("Checkout failed. Stopping demo.");
                return;
            }

            System.out.println("Created order ID: " + createdOrderId);
            orders.displayOrder(createdOrderId);

            System.out.println("\n=== 5. eDEPOT Fill Order ===");
            inventory.fillOrder(createdOrderId);

            System.out.println("\n=== 6. Inventory After Fill ===");
            int cpAfterFill = inventory.getInventoryQuantity("CP00001");
            int kbAfterFill = inventory.getInventoryQuantity("KB00001");

            System.out.println("CP00001 after fill: " + cpAfterFill);
            System.out.println("KB00001 after fill: " + kbAfterFill);

            System.out.println("\nExpected CP00001 decrease: 1");
            System.out.println("Actual CP00001 decrease: " + (cpBeforeFill - cpAfterFill));

            System.out.println("\nExpected KB00001 decrease: 2");
            System.out.println("Actual KB00001 decrease: " + (kbBeforeFill - kbAfterFill));

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
                new NoticeItem("Logitech", "MX-Keys-S", 3)
            );

            int kbBeforeNotice = inventory.getInventoryQuantity("KB00001");
            System.out.println("KB00001 before shipping notice/receive: " + kbBeforeNotice);

            shipping.receiveShippingNotice(
                noticeId,
                "Logitech",
                "UPS",
                noticeItems
            );

            System.out.println("Inserted shipping notice: " + noticeId);

            shipping.receiveShipment(noticeId);

            System.out.println("Received shipment for notice: " + noticeId);

            int kbAfterReceive = inventory.getInventoryQuantity("KB00001");
            System.out.println("KB00001 after receive: " + kbAfterReceive);
            System.out.println("Expected KB00001 increase from shipment: 3");
            System.out.println("Actual KB00001 increase: " + (kbAfterReceive - kbBeforeNotice));

            System.out.println("\n=== 10. Manager Report ===");

            LocalDate today = LocalDate.now();

            manager.printMonthlySalesReport(
                today.getYear(),
                today.getMonthValue()
            );

            System.out.println("\n=== 11. Manager Price Change ===");

            manager.changePrice(
                "CP00001",
                1899.99
            );

            System.out.println("\n=== 12. Manual Customer Status Update ===");

            manager.updateCustomerStatusManually(
                "C001",
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
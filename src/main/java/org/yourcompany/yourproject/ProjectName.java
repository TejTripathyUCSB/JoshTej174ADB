package org.yourcompany.yourproject;

public class ProjectName {
    public static void main(String[] args) {
        CartService cart = new CartService();
        OrderService orders = new OrderService();
        ManagerService manager = new ManagerService();

        System.out.println("=== eMART Demo Run ===");

        cart.addItem("C001", "CP00001", 1);
        cart.addItem("C001", "KB00001", 2);

        cart.displayCart("C001");

        Integer createdOrderId = orders.checkout("C001");
        if (createdOrderId != null) {
            orders.displayOrder(createdOrderId);
        }

        manager.changePrice("CP00001", 1899.99);

        manager.printMonthlySalesReport(2026, 5);

        manager.updateCustomerStatusManually("C001", "GOLD");
    }
}

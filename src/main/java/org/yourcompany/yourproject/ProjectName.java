package org.yourcompany.yourproject;

public class ProjectName {
    public static void main(String[] args) {
        CartService cart = new CartService();
        OrderService orders = new OrderService();

        // Start fresh for repeatable testing.
        cart.clearCart("C001");

        cart.addItem("C001", "CP00001", 1);
        cart.addItem("C001", "KB00001", 2);

        cart.displayCart("C001");

        orders.checkout("C001");

        cart.displayCart("C001");
    }
}

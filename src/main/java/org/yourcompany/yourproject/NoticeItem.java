package org.yourcompany.yourproject;

public class NoticeItem {
    private String manufacturer;
    private String modelNumber;
    private int quantity;

    public NoticeItem(String manufacturer, String modelNumber, int quantity) {
        this.manufacturer = manufacturer;
        this.modelNumber = modelNumber;
        this.quantity = quantity;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public int getQuantity() {
        return quantity;
    }
}

package org.yourcompany.yourproject;

public class NoticeItem {
    private String stockNumber;
    private String manufacturer;
    private String modelNumber;
    private final int quantity;

    public static NoticeItem forStockNumber(String stockNumber, int quantity) {
        NoticeItem item = new NoticeItem(null, null, quantity);
        item.stockNumber = stockNumber;
        return item;
    }

    public NoticeItem(String manufacturer, String modelNumber, int quantity) {
        this.manufacturer = manufacturer;
        this.modelNumber = modelNumber;
        this.quantity = quantity;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getStockNumber() {
        return stockNumber;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public int getQuantity() {
        return quantity;
    }
}

package com.example.electricity.model;

public class Payment {
    private final int billId;
    private final String customerId;
    private final double amount;
    private final String method;
    private final String status = "Paid";

    public Payment(int billId, String customerId, double amount, String method) {
        if (amount < 0) throw new IllegalArgumentException("Payment cannot be negative.");
        this.billId = billId;
        this.customerId = customerId;
        this.amount = amount;
        this.method = (method == null || method.isBlank()) ? "Demo UPI" : method;
    }

    public int getBillId() { return billId; }
    public String getCustomerId() { return customerId; }
    public double getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
}

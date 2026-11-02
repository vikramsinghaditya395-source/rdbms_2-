package com.example.electricity.model;

public class Bill {
    private final String customerId;
    private final double previousReading;
    private final double currentReading;
    private final double rate;

    public Bill(String customerId, double previousReading,
                double currentReading, double rate) {
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("Customer ID is required.");
        if (previousReading < 0 || currentReading < 0)
            throw new IllegalArgumentException("Meter readings cannot be negative.");
        if (currentReading < previousReading)
            throw new IllegalArgumentException("Current reading cannot be less than previous reading.");
        if (rate <= 0)
            throw new IllegalArgumentException("Rate must be greater than zero.");

        this.customerId = customerId;
        this.previousReading = previousReading;
        this.currentReading = currentReading;
        this.rate = rate;
    }

    public String getCustomerId() { return customerId; }
    public double getPreviousReading() { return previousReading; }
    public double getCurrentReading() { return currentReading; }
    public double getRate() { return rate; }
    public double getUnits() { return currentReading - previousReading; }
    public double getAmount() { return getUnits() * rate; }
}

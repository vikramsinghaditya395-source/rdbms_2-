package com.example.electricity.model;

public class Customer extends User {
    private final String name;
    private final String mobile;
    private final String address;
    private final String connectionType;

    public Customer(String customerId, String name, String mobile,
                    String address, String connectionType) {
        super(customerId);
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.connectionType = connectionType;
    }

    public String getName() { return name; }
    public String getMobile() { return mobile; }
    public String getAddress() { return address; }
    public String getConnectionType() { return connectionType; }

    public double getRate() {
        if ("Residential".equalsIgnoreCase(connectionType)) return 5.0;
        if ("Commercial".equalsIgnoreCase(connectionType)) return 8.0;
        throw new IllegalArgumentException("Connection type must be Residential or Commercial.");
    }

    @Override
    public String getRole() {
        return "Customer";
    }
}

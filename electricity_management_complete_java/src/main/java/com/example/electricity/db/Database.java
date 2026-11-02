package com.example.electricity.db;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.sql.*;

@Component
public class Database {
    public Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:electricity.db");
    }

    @PostConstruct
    public void initialize() {
        try (Connection con = connect(); Statement st = con.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS customers(
                  customer_id TEXT PRIMARY KEY, name TEXT NOT NULL,
                  mobile TEXT NOT NULL, address TEXT NOT NULL,
                  connection_type TEXT NOT NULL)
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bills(
                  bill_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  customer_id TEXT NOT NULL, previous_reading REAL NOT NULL,
                  current_reading REAL NOT NULL, units REAL NOT NULL,
                  rate REAL NOT NULL, amount REAL NOT NULL,
                  status TEXT NOT NULL DEFAULT 'Pending',
                  created_at TEXT DEFAULT CURRENT_TIMESTAMP)
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS payments(
                  payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                  bill_id INTEGER NOT NULL, customer_id TEXT NOT NULL,
                  amount REAL NOT NULL, method TEXT NOT NULL,
                  status TEXT NOT NULL, paid_at TEXT DEFAULT CURRENT_TIMESTAMP)
            """);
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM customers")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    st.executeUpdate("""
                      INSERT INTO customers(customer_id,name,mobile,address,connection_type) VALUES
                      ('CUST1001','Aarav Sharma','9876543210','Jaipur, Rajasthan','Residential'),
                      ('CUST1002','Priya Verma','9123456780','Ajmer, Rajasthan','Commercial'),
                      ('CUST1003','Rahul Mehta','9988776655','Jaipur, Rajasthan','Residential')
                    """);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }
}

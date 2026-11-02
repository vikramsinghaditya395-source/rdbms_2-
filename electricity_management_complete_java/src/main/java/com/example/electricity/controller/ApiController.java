package com.example.electricity.controller;

import com.example.electricity.db.Database;
import com.example.electricity.model.*;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Font;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final Database database;
    private static final String OTP = "123456";
    private static final String ADMIN_ID = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    public ApiController(Database database) { this.database = database; }

    @PostMapping("/customer/login")
    public ResponseEntity<?> customerLogin(@RequestBody Map<String,String> body) throws SQLException {
        String id = body.getOrDefault("customerId", "").trim();
        String mobile = body.getOrDefault("mobile", "").trim();
        try (Connection c = database.connect();
             PreparedStatement p = c.prepareStatement("SELECT * FROM customers WHERE customer_id=? AND mobile=?")) {
            p.setString(1,id); p.setString(2,mobile);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return bad("Customer ID or mobile number is incorrect.");
                return ResponseEntity.ok(Map.of("message","Customer verified. Enter demo OTP.","customerId",id));
            }
        }
    }

    @PostMapping("/customer/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String,String> body) {
        if (!OTP.equals(body.get("otp"))) return bad("Invalid OTP. Use 123456.");
        return ResponseEntity.ok(Map.of("message","OTP verified."));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String,String> body) {
        Admin admin = new Admin(body.getOrDefault("adminId",""), body.getOrDefault("password",""));
        if (admin.authenticate(ADMIN_ID, ADMIN_PASSWORD))
            return ResponseEntity.ok(Map.of("message","Admin login successful."));
        return bad("Incorrect admin ID or password.");
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<?> customer(@PathVariable String id) throws SQLException {
        List<Map<String,Object>> rows = query("SELECT * FROM customers WHERE customer_id=?", id);
        if (rows.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(rows.get(0));
    }

    @GetMapping("/customer/{id}/bills")
    public List<Map<String,Object>> customerBills(@PathVariable String id) throws SQLException {
        return query("SELECT * FROM bills WHERE customer_id=? ORDER BY bill_id DESC", id);
    }

    @GetMapping("/customer/{id}/payments")
    public List<Map<String,Object>> customerPayments(@PathVariable String id) throws SQLException {
        return query("SELECT * FROM payments WHERE customer_id=? ORDER BY payment_id DESC", id);
    }

    @GetMapping("/admin/customers")
    public List<Map<String,Object>> customers() throws SQLException {
        return query("SELECT * FROM customers ORDER BY customer_id");
    }

    @PostMapping("/admin/customers")
    public ResponseEntity<?> addCustomer(@RequestBody Map<String,String> b) throws SQLException {
        String id=b.getOrDefault("customerId","").trim();
        String name=b.getOrDefault("name","").trim();
        String mobile=b.getOrDefault("mobile","").trim();
        String address=b.getOrDefault("address","").trim();
        String type=b.getOrDefault("connectionType","").trim();
        if(id.isBlank()||name.isBlank()||mobile.isBlank()||address.isBlank())
            return bad("Please fill in every customer field.");
        if(!type.equals("Residential")&&!type.equals("Commercial"))
            return bad("Choose Residential or Commercial.");
        try(Connection c=database.connect(); PreparedStatement p=c.prepareStatement(
            "INSERT INTO customers(customer_id,name,mobile,address,connection_type) VALUES(?,?,?,?,?)")) {
            p.setString(1,id);p.setString(2,name);p.setString(3,mobile);p.setString(4,address);p.setString(5,type);p.executeUpdate();
            return ResponseEntity.ok(Map.of("message","Customer added."));
        } catch(SQLException e) { return bad("Could not add customer. Check that the Customer ID is unique."); }
    }

    @GetMapping("/admin/bills")
    public List<Map<String,Object>> bills() throws SQLException {
        return query("SELECT bills.*,customers.name,customers.connection_type FROM bills JOIN customers ON bills.customer_id=customers.customer_id ORDER BY bill_id DESC");
    }

    @GetMapping("/admin/payments")
    public List<Map<String,Object>> payments() throws SQLException {
        return query("SELECT payments.*,customers.name FROM payments JOIN customers ON payments.customer_id=customers.customer_id ORDER BY payment_id DESC");
    }

    @PostMapping("/admin/generate-bill")
    public ResponseEntity<?> generateBill(@RequestBody Map<String,Object> b) throws SQLException {
        try {
            String id=String.valueOf(b.get("customerId"));
            double previous=Double.parseDouble(String.valueOf(b.get("previousReading")));
            double current=Double.parseDouble(String.valueOf(b.get("currentReading")));
            Customer customer=findCustomer(id);
            if(customer==null) return bad("Customer ID does not exist.");
            Bill bill=new Bill(id,previous,current,customer.getRate());
            try(Connection c=database.connect();PreparedStatement p=c.prepareStatement(
                "INSERT INTO bills(customer_id,previous_reading,current_reading,units,rate,amount,status) VALUES(?,?,?,?,?,?,'Pending')")) {
                p.setString(1,id);p.setDouble(2,previous);p.setDouble(3,current);p.setDouble(4,bill.getUnits());p.setDouble(5,bill.getRate());p.setDouble(6,bill.getAmount());p.executeUpdate();
            }
            return ResponseEntity.ok(Map.of("message","Bill generated.","units",bill.getUnits(),"rate",bill.getRate(),"amount",bill.getAmount()));
        } catch(NumberFormatException | IllegalArgumentException e) {
            return bad(e.getMessage()==null?"Invalid reading values.":e.getMessage());
        }
    }

    @PostMapping("/customer/{id}/pay/{billId}")
    public ResponseEntity<?> pay(@PathVariable String id,@PathVariable int billId,@RequestBody Map<String,String> b) throws SQLException {
        try(Connection c=database.connect()) {
            c.setAutoCommit(false);
            try {
                double amount;
                try(PreparedStatement p=c.prepareStatement("SELECT amount,status FROM bills WHERE bill_id=? AND customer_id=?")) {
                    p.setInt(1,billId);p.setString(2,id);
                    try(ResultSet r=p.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("Bill not found.");
                        if("Paid".equals(r.getString("status"))) throw new IllegalArgumentException("Bill is already paid.");
                        amount=r.getDouble("amount");
                    }
                }
                Payment payment=new Payment(billId,id,amount,b.get("method"));
                try(PreparedStatement p=c.prepareStatement("INSERT INTO payments(bill_id,customer_id,amount,method,status) VALUES(?,?,?,?,?)")) {
                    p.setInt(1,payment.getBillId());p.setString(2,payment.getCustomerId());p.setDouble(3,payment.getAmount());p.setString(4,payment.getMethod());p.setString(5,payment.getStatus());p.executeUpdate();
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE bills SET status='Paid' WHERE bill_id=?")) {p.setInt(1,billId);p.executeUpdate();}
                c.commit();
                return ResponseEntity.ok(Map.of("message","Demo payment successful."));
            } catch(IllegalArgumentException e) {
                c.rollback(); return bad(e.getMessage());
            } catch(SQLException e) { c.rollback(); throw e; }
        }
    }

    @GetMapping("/customer/{id}/bill/{billId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id,@PathVariable int billId) throws SQLException {
        Customer customer=findCustomer(id);
        if(customer==null) return ResponseEntity.notFound().build();
        List<Map<String,Object>> rows=query("SELECT * FROM bills WHERE customer_id=? AND bill_id=?",id,String.valueOf(billId));
        if(rows.isEmpty()) return ResponseEntity.notFound().build();
        Map<String,Object> b=rows.get(0);
        try {
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            Document doc=new Document();
            PdfWriter.getInstance(doc,output);doc.open();
            Font titleFont=new Font(Font.HELVETICA,20,Font.BOLD);
            Paragraph title=new Paragraph("ELECTRICITY BILL",titleFont);
            title.setAlignment(Element.ALIGN_CENTER);doc.add(title);doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Bill ID: "+b.get("bill_id")));
            doc.add(new Paragraph("Customer ID: "+customer.getUserId()));
            doc.add(new Paragraph("Customer Name: "+customer.getName()));
            doc.add(new Paragraph("Mobile: "+customer.getMobile()));
            doc.add(new Paragraph("Address: "+customer.getAddress()));
            doc.add(new Paragraph("Connection Type: "+customer.getConnectionType()));
            doc.add(new Paragraph("Previous Reading: "+b.get("previous_reading")));
            doc.add(new Paragraph("Current Reading: "+b.get("current_reading")));
            doc.add(new Paragraph("Units Consumed: "+b.get("units")));
            doc.add(new Paragraph("Rate Per Unit: Rs. "+b.get("rate")));
            doc.add(new Paragraph("Total Amount: Rs. "+b.get("amount")));
            doc.add(new Paragraph("Payment Status: "+b.get("status")));
            doc.add(new Paragraph("Bill Date: "+b.get("created_at")));
            doc.close();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=electricity_bill_"+billId+".pdf")
                .body(output.toByteArray());
        } catch(Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    private Customer findCustomer(String id) throws SQLException {
        try(Connection c=database.connect();PreparedStatement p=c.prepareStatement("SELECT * FROM customers WHERE customer_id=?")) {
            p.setString(1,id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) return null;
                return new Customer(r.getString("customer_id"),r.getString("name"),r.getString("mobile"),r.getString("address"),r.getString("connection_type"));
            }
        }
    }

    private List<Map<String,Object>> query(String sql,String... params) throws SQLException {
        List<Map<String,Object>> result=new ArrayList<>();
        try(Connection c=database.connect();PreparedStatement p=c.prepareStatement(sql)) {
            for(int i=0;i<params.length;i++) p.setString(i+1,params[i]);
            try(ResultSet r=p.executeQuery()) {
                ResultSetMetaData m=r.getMetaData();
                while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=m.getColumnCount();i++) row.put(m.getColumnLabel(i),r.getObject(i));
                    result.add(row);
                }
            }
        }
        return result;
    }

    private ResponseEntity<Map<String,String>> bad(String message) {
        return ResponseEntity.badRequest().body(Map.of("error",message));
    }
}

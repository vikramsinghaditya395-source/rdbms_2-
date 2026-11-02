ELECTRICITY MANAGEMENT SYSTEM - COMPLETE JAVA PROJECT

Requirements:
- JDK 17 or newer
- Maven installed
- VS Code or IntelliJ IDEA

Run:
1. Extract this ZIP.
2. Open the extracted folder in VS Code.
3. Open Terminal in the project folder.
4. Run: mvn spring-boot:run
5. Visit: http://localhost:8080

CUSTOMER DEMO:
Customer ID: CUST1001
Mobile: 9876543210
OTP: 123456

Other sample customers:
CUST1002 / 9123456780
CUST1003 / 9988776655

ADMIN DEMO:
Admin ID: admin
Password: admin123

FEATURES:
- Customer login using ID and mobile, then demo OTP
- Admin login
- Add customers with Residential or Commercial connection
- Residential sample rate: Rs. 5 per unit
- Commercial sample rate: Rs. 8 per unit
- Generate bills from previous/current meter readings
- View customers, bills, payment records
- Simulated bill payment
- Download each bill as PDF

OOP FILES:
model/User.java - parent class, encapsulation
model/Customer.java - extends User, rate selection and overriding
model/Admin.java - extends User, authentication and overriding
model/Bill.java - encapsulated readings, validation and calculation
model/Payment.java - encapsulated payment information

Backend/API:
controller/ApiController.java
db/Database.java

Frontend:
src/main/resources/static/index.html
src/main/resources/static/style.css
src/main/resources/static/app.js

HOW METER READINGS WORK:
The admin form in index.html collects previous/current readings.
app.js sends them to POST /api/admin/generate-bill.
ApiController.java creates a Bill object.
Bill.java calculates units = current - previous and amount = units * rate.
Customer.java supplies the rate based on connection type.
The controller stores the values in SQLite.

Database file electricity.db is created automatically at project root.

SECURITY NOTE:
This is a local educational demo. OTP is fixed, admin credentials are hardcoded, and payment is simulated. Do not deploy it publicly or use real customer data without implementing proper authentication, authorization, and payment security.
The rates are sample rates for demonstration, not actual utility tariffs.

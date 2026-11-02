var activeCustomer = null;

// Get HTML element
function getElement(id) {
    return document.getElementById(id);
}

// Show message
function message(text, error) {
    getElement("message").textContent = text;

    if (error) {
        getElement("message").className = "message error";
    } else {
        getElement("message").className = "message success";
    }
}

// Clear message
function clearMessage() {
    getElement("message").className = "message hidden";
}

// Send request to backend
async function api(url, method, data) {
    if (method == null) {
        method = "GET";
    }

    var options = {
        method: method,
        headers: {
            "Content-Type": "application/json"
        }
    };

    if (data != null) {
        options.body = JSON.stringify(data);
    }

    var response = await fetch(url, options);
    var result = {};

    if (response.status != 204) {
        result = await response.json();
    }

    if (!response.ok) {
        throw new Error(result.error || "Request failed");
    }

    return result;
}

// Show required screen
function view(name) {
    getElement("loginView").classList.add("hidden");
    getElement("customerView").classList.add("hidden");
    getElement("adminView").classList.add("hidden");

    getElement(name).classList.remove("hidden");
}

// Create table
function table(id, headers, rows) {
    var html = "<tr>";

    for (var i = 0; i < headers.length; i++) {
        html += "<th>" + headers[i] + "</th>";
    }

    html += "</tr>";

    if (rows.length == 0) {
        html += '<tr><td colspan="' + headers.length +
                '">No records found.</td></tr>';
    } else {
        for (var j = 0; j < rows.length; j++) {
            html += "<tr>";

            for (var k = 0; k < rows[j].length; k++) {
                var value = rows[j][k];

                if (value == null) {
                    value = "";
                }

                html += "<td>" + value + "</td>";
            }

            html += "</tr>";
        }
    }

    getElement(id).innerHTML = html;
}

// Convert amount into rupees
function money(amount) {
    return "₹" + Number(amount || 0).toFixed(2);
}

// Customer Login
getElement("customerLogin").addEventListener("submit", async function(event) {
    event.preventDefault();

    try {
        activeCustomer = getElement("customerId").value.trim();

        await api("/api/customer/login", "POST", {
            customerId: activeCustomer,
            mobile: getElement("mobile").value.trim()
        });

        getElement("otpBox").classList.remove("hidden");

        message("Customer verified. Enter demo OTP 123456.", false);
    } catch (error) {
        message(error.message, true);
    }
});

// OTP Verification
getElement("otpForm").addEventListener("submit", async function(event) {
    event.preventDefault();

    try {
        await api("/api/customer/verify", "POST", {
            otp: getElement("otp").value.trim()
        });

        await loadCustomer();
    } catch (error) {
        message(error.message, true);
    }
});

// Admin Login
getElement("adminLogin").addEventListener("submit", async function(event) {
    event.preventDefault();

    try {
        await api("/api/admin/login", "POST", {
            adminId: getElement("adminId").value,
            password: getElement("adminPassword").value
        });

        await loadAdmin();
    } catch (error) {
        message(error.message, true);
    }
});

// Load Customer Dashboard
async function loadCustomer() {
    try {
        var url = "/api/customer/" +
                  encodeURIComponent(activeCustomer);

        var customer = await api(url);
        var bills = await api(url + "/bills");
        var payments = await api(url + "/payments");

        getElement("welcome").textContent =
            "Welcome, " + customer.name;

        getElement("customerInfo").textContent =
            customer.customer_id + " · " +
            customer.mobile + " · " +
            customer.address + " · " +
            customer.connection_type;

        var rate = "₹5";

        if (customer.connection_type == "Commercial") {
            rate = "₹8";
        }

        var pendingBills = 0;

        for (var i = 0; i < bills.length; i++) {
            if (bills[i].status == "Pending") {
                pendingBills++;
            }
        }

        getElement("customerStats").innerHTML =
            '<div class="stat"><span>Connection</span><b>' +
            customer.connection_type + '</b></div>' +

            '<div class="stat"><span>Rate per unit</span><b>' +
            rate + '</b></div>' +

            '<div class="stat"><span>Total bills</span><b>' +
            bills.length + '</b></div>' +

            '<div class="stat"><span>Pending bills</span><b>' +
            pendingBills + '</b></div>';

        var billRows = [];

        for (var j = 0; j < bills.length; j++) {
            var bill = bills[j];

            var actions =
                '<button class="tiny" onclick="downloadPdf(' +
                bill.bill_id + ')">Download PDF</button>';

            if (bill.status == "Pending") {
                actions +=
                    ' <button class="tiny" onclick="payBill(' +
                    bill.bill_id + ')">Pay Now</button>';
            }

            billRows.push([
                bill.bill_id,
                bill.previous_reading,
                bill.current_reading,
                bill.units,
                money(bill.rate),
                money(bill.amount),
                bill.status,
                actions
            ]);
        }

        table("customerBills", [
            "Bill ID", "Previous", "Current", "Units",
            "Rate", "Amount", "Status", "Actions"
        ], billRows);

        var paymentRows = [];

        for (var k = 0; k < payments.length; k++) {
            var payment = payments[k];

            paymentRows.push([
                payment.payment_id,
                payment.bill_id,
                money(payment.amount),
                payment.method,
                payment.status,
                payment.paid_at
            ]);
        }

        table("customerPayments", [
            "Payment ID", "Bill ID", "Amount",
            "Method", "Status", "Date"
        ], paymentRows);

        view("customerView");
        clearMessage();

    } catch (error) {
        message(error.message, true);
    }
}

// Download Bill PDF
async function downloadPdf(id) {
    try {
        var url = "/api/customer/" +
                  encodeURIComponent(activeCustomer) +
                  "/bill/" + id + "/pdf";

        var response = await fetch(url);

        if (!response.ok) {
            throw new Error("Could not download PDF.");
        }

        var file = await response.blob();
        var fileUrl = URL.createObjectURL(file);

        var link = document.createElement("a");
        link.href = fileUrl;
        link.download = "electricity_bill_" + id + ".pdf";
        link.click();

        URL.revokeObjectURL(fileUrl);

    } catch (error) {
        message(error.message, true);
    }
}

// Pay Electricity Bill
async function payBill(id) {
    try {
        var url = "/api/customer/" +
                  encodeURIComponent(activeCustomer) +
                  "/pay/" + id;

        await api(url, "POST", {
            method: "Demo UPI"
        });

        message("Demo payment successful.", false);

        await loadCustomer();

    } catch (error) {
        message(error.message, true);
    }
}

// Load Admin Dashboard
async function loadAdmin() {
    try {
        var customers = await api("/api/admin/customers");
        var bills = await api("/api/admin/bills");
        var payments = await api("/api/admin/payments");

        var pendingBills = 0;

        for (var i = 0; i < bills.length; i++) {
            if (bills[i].status == "Pending") {
                pendingBills++;
            }
        }

        getElement("adminStats").innerHTML =
            '<div class="stat"><span>Customers</span><b>' +
            customers.length + '</b></div>' +

            '<div class="stat"><span>Total bills</span><b>' +
            bills.length + '</b></div>' +

            '<div class="stat"><span>Pending bills</span><b>' +
            pendingBills + '</b></div>' +

            '<div class="stat"><span>Payments</span><b>' +
            payments.length + '</b></div>';

        var options = "";

        for (var j = 0; j < customers.length; j++) {
            var customer = customers[j];

            options += '<option value="' + customer.customer_id + '">' +
                customer.customer_id + " — " +
                customer.name + " (" +
                customer.connection_type + ")" +
                "</option>";
        }

        getElement("billCustomer").innerHTML = options;
        getElement("billCustomer").onchange = updateRate;

        updateRate();

        var customerRows = [];

        for (var k = 0; k < customers.length; k++) {
            var c = customers[k];

            customerRows.push([
                c.customer_id,
                c.name,
                c.mobile,
                c.address,
                c.connection_type
            ]);
        }

        table("allCustomers", [
            "Customer ID", "Name", "Mobile",
            "Address", "Connection"
        ], customerRows);

        var billRows = [];

        for (var m = 0; m < bills.length; m++) {
            var b = bills[m];

            billRows.push([
                b.bill_id,
                b.name + " (" + b.customer_id + ")",
                b.connection_type,
                b.previous_reading,
                b.current_reading,
                b.units,
                money(b.rate),
                money(b.amount),
                b.status,
                b.created_at
            ]);
        }

        table("allBills", [
            "Bill ID", "Customer", "Type", "Previous",
            "Current", "Units", "Rate", "Amount",
            "Status", "Date"
        ], billRows);

        var paymentRows = [];

        for (var n = 0; n < payments.length; n++) {
            var p = payments[n];

            paymentRows.push([
                p.payment_id,
                p.name + " (" + p.customer_id + ")",
                p.bill_id,
                money(p.amount),
                p.method,
                p.status,
                p.paid_at
            ]);
        }

        table("allPayments", [
            "Payment ID", "Customer", "Bill",
            "Amount", "Method", "Status", "Date"
        ], paymentRows);

        view("adminView");
        clearMessage();

    } catch (error) {
        message(error.message, true);
    }
}

// Update Electricity Rate
function updateRate() {
    var dropdown = getElement("billCustomer");
    var selected = dropdown.options[dropdown.selectedIndex];

    if (selected == null) {
        getElement("ratePreview").value = "";
    } else if (selected.textContent.includes("(Commercial)")) {
        getElement("ratePreview").value = "₹8/unit";
    } else {
        getElement("ratePreview").value = "₹5/unit";
    }
}

// Add New Customer
getElement("addCustomer").addEventListener("submit", async function(event) {
    event.preventDefault();

    try {
        await api("/api/admin/customers", "POST", {
            customerId: getElement("newId").value,
            name: getElement("newName").value,
            mobile: getElement("newMobile").value,
            address: getElement("newAddress").value,
            connectionType: getElement("newType").value
        });

        getElement("addCustomer").reset();

        message("Customer added successfully.", false);

        await loadAdmin();

    } catch (error) {
        message(error.message, true);
    }
});

// Generate Electricity Bill
getElement("billForm").addEventListener("submit", async function(event) {
    event.preventDefault();

    try {
        var result = await api("/api/admin/generate-bill", "POST", {
            customerId: getElement("billCustomer").value,
            previousReading: getElement("previous").value,
            currentReading: getElement("current").value
        });

        message(
            "Bill generated: " + result.units +
            " units × " + money(result.rate) +
            " = " + money(result.amount),
            false
        );

        await loadAdmin();

    } catch (error) {
        message(error.message, true);
    }
});

// Logout
function logout() {
    activeCustomer = null;

    getElement("otpBox").classList.add("hidden");
    getElement("otp").value = "";

    view("loginView");
    clearMessage();
}
package com.wahab.db

data class Customer(
    val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String,
    val item: String,
    val amountYer: Double = 0.0,
    val amountUsd: Double = 0.0
)

data class InventoryItem(
    val id: Int = 0,
    val name: String,
    val quantity: Double,
    val price: Double,
    val currency: String
)

data class Sale(
    val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val productName: String,
    val quantity: Double,
    val price: Double,
    val total: Double,
    val currency: String,
    val dateStr: String
)

data class Payment(
    val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val amount: Double,
    val currency: String,
    val notes: String,
    val dateStr: String,
    val remainingYer: Double,
    val remainingUsd: Double
)

class CustomerDao {
    fun getAll(): List<Customer> {
        val list = mutableListOf<Customer>()
        DatabaseManager.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM customers ORDER BY id DESC")
                while (rs.next()) {
                    list.add(
                        Customer(
                            id = rs.getInt("id"),
                            name = rs.getString("name"),
                            phone = rs.getString("phone") ?: "",
                            address = rs.getString("address") ?: "",
                            item = rs.getString("item") ?: "",
                            amountYer = rs.getDouble("amount_yer"),
                            amountUsd = rs.getDouble("amount_usd")
                        )
                    )
                }
            }
        }
        return list
    }

    fun getById(id: Int): Customer? {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement("SELECT * FROM customers WHERE id = ?")
            pstmt.setInt(1, id)
            val rs = pstmt.executeQuery()
            if (rs.next()) {
                return Customer(
                    id = rs.getInt("id"),
                    name = rs.getString("name"),
                    phone = rs.getString("phone") ?: "",
                    address = rs.getString("address") ?: "",
                    item = rs.getString("item") ?: "",
                    amountYer = rs.getDouble("amount_yer"),
                    amountUsd = rs.getDouble("amount_usd")
                )
            }
        }
        return null
    }

    fun insert(customer: Customer): Int {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement(
                "INSERT INTO customers (name, phone, address, item, amount_yer, amount_usd) VALUES (?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            pstmt.setString(1, customer.name)
            pstmt.setString(2, customer.phone)
            pstmt.setString(3, customer.address)
            pstmt.setString(4, customer.item)
            pstmt.setDouble(5, customer.amountYer)
            pstmt.setDouble(6, customer.amountUsd)
            pstmt.executeUpdate()
            val rs = pstmt.generatedKeys
            if (rs.next()) return rs.getInt(1)
        }
        return 0
    }

    fun updateBalances(customerId: Int, deltaYer: Double, deltaUsd: Double) {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement(
                "UPDATE customers SET amount_yer = amount_yer + ?, amount_usd = amount_usd + ? WHERE id = ?"
            )
            pstmt.setDouble(1, deltaYer)
            pstmt.setDouble(2, deltaUsd)
            pstmt.setInt(3, customerId)
            pstmt.executeUpdate()
        }
    }

    fun delete(id: Int) {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement("DELETE FROM customers WHERE id = ?")
            pstmt.setInt(1, id)
            pstmt.executeUpdate()
        }
    }
}

class InventoryDao {
    fun getAll(): List<InventoryItem> {
        val list = mutableListOf<InventoryItem>()
        DatabaseManager.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM inventory ORDER BY id DESC")
                while (rs.next()) {
                    list.add(
                        InventoryItem(
                            id = rs.getInt("id"),
                            name = rs.getString("name"),
                            quantity = rs.getDouble("quantity"),
                            price = rs.getDouble("price"),
                            currency = rs.getString("currency") ?: "YER"
                        )
                    )
                }
            }
        }
        return list
    }

    fun insert(item: InventoryItem): Int {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement(
                "INSERT INTO inventory (name, quantity, price, currency) VALUES (?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            pstmt.setString(1, item.name)
            pstmt.setDouble(2, item.quantity)
            pstmt.setDouble(3, item.price)
            pstmt.setString(4, item.currency)
            pstmt.executeUpdate()
            val rs = pstmt.generatedKeys
            if (rs.next()) return rs.getInt(1)
        }
        return 0
    }

    fun deductQuantity(id: Int, quantityToDeduct: Double) {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement("UPDATE inventory SET quantity = MAX(0, quantity - ?) WHERE id = ?")
            pstmt.setDouble(1, quantityToDeduct)
            pstmt.setInt(2, id)
            pstmt.executeUpdate()
        }
    }

    fun delete(id: Int) {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement("DELETE FROM inventory WHERE id = ?")
            pstmt.setInt(1, id)
            pstmt.executeUpdate()
        }
    }
}

class SaleDao {
    fun getAll(): List<Sale> {
        val list = mutableListOf<Sale>()
        DatabaseManager.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM sales ORDER BY id DESC")
                while (rs.next()) {
                    list.add(
                        Sale(
                            id = rs.getInt("id"),
                            customerId = rs.getInt("customer_id"),
                            customerName = rs.getString("customer_name") ?: "",
                            productName = rs.getString("product_name") ?: "",
                            quantity = rs.getDouble("quantity"),
                            price = rs.getDouble("price"),
                            total = rs.getDouble("total"),
                            currency = rs.getString("currency") ?: "YER",
                            dateStr = rs.getString("date_str") ?: ""
                        )
                    )
                }
            }
        }
        return list
    }

    fun insert(sale: Sale): Int {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement(
                "INSERT INTO sales (customer_id, customer_name, product_name, quantity, price, total, currency, date_str) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            pstmt.setInt(1, sale.customerId)
            pstmt.setString(2, sale.customerName)
            pstmt.setString(3, sale.productName)
            pstmt.setDouble(4, sale.quantity)
            pstmt.setDouble(5, sale.price)
            pstmt.setDouble(6, sale.total)
            pstmt.setString(7, sale.currency)
            pstmt.setString(8, sale.dateStr)
            pstmt.executeUpdate()
            val rs = pstmt.generatedKeys
            if (rs.next()) return rs.getInt(1)
        }
        return 0
    }
}

class PaymentDao {
    fun getAll(): List<Payment> {
        val list = mutableListOf<Payment>()
        DatabaseManager.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM payments ORDER BY id DESC")
                while (rs.next()) {
                    list.add(
                        Payment(
                            id = rs.getInt("id"),
                            customerId = rs.getInt("customer_id"),
                            customerName = rs.getString("customer_name") ?: "",
                            amount = rs.getDouble("amount"),
                            currency = rs.getString("currency") ?: "YER",
                            notes = rs.getString("notes") ?: "",
                            dateStr = rs.getString("date_str") ?: "",
                            remainingYer = rs.getDouble("remaining_yer"),
                            remainingUsd = rs.getDouble("remaining_usd")
                        )
                    )
                }
            }
        }
        return list
    }

    fun insert(payment: Payment): Int {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement(
                "INSERT INTO payments (customer_id, customer_name, amount, currency, notes, date_str, remaining_yer, remaining_usd) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
            )
            pstmt.setInt(1, payment.customerId)
            pstmt.setString(2, payment.customerName)
            pstmt.setDouble(3, payment.amount)
            pstmt.setString(4, payment.currency)
            pstmt.setString(5, payment.notes)
            pstmt.setString(6, payment.dateStr)
            pstmt.setDouble(7, payment.remainingYer)
            pstmt.setDouble(8, payment.remainingUsd)
            pstmt.executeUpdate()
            val rs = pstmt.generatedKeys
            if (rs.next()) return rs.getInt(1)
        }
        return 0
    }
}

class SettingsDao {
    fun get(key: String): String {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement("SELECT value FROM settings WHERE key = ?")
            pstmt.setString(1, key)
            val rs = pstmt.executeQuery()
            if (rs.next()) return rs.getString("value")
        }
        return ""
    }

    fun set(key: String, value: String) {
        DatabaseManager.getConnection().use { conn ->
            val pstmt = conn.prepareStatement("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)")
            pstmt.setString(1, key)
            pstmt.setString(2, value)
            pstmt.executeUpdate()
        }
    }

    fun resetAll() {
        DatabaseManager.getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DELETE FROM customers")
                stmt.execute("DELETE FROM inventory")
                stmt.execute("DELETE FROM sales")
                stmt.execute("DELETE FROM payments")
            }
        }
    }
}

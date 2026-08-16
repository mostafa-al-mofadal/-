package com.wahab.service

import com.wahab.db.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object AccountingService {
    val customerDao = CustomerDao()
    val inventoryDao = InventoryDao()
    val saleDao = SaleDao()
    val paymentDao = PaymentDao()
    val settingsDao = SettingsDao()

    fun formatCurrency(amount: Double, currency: String = "YER"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        val formatted = formatter.format(amount)

        return if (currency == "USD") {
            "$$formatted"
        } else {
            "$formatted ر.ي"
        }
    }

    fun getCurrentDateStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    // إضافة عميل
    fun addCustomer(name: String, phone: String, address: String, item: String, initialAmount: Double, currency: String) {
        val amountYer = if (currency == "YER") initialAmount else 0.0
        val amountUsd = if (currency == "USD") initialAmount else 0.0

        val customer = Customer(
            name = name,
            phone = phone,
            address = address,
            item = item,
            amountYer = amountYer,
            amountUsd = amountUsd
        )
        customerDao.insert(customer)
    }

    // إضافة مبيعات وفاتورة
    fun processSale(customerId: Int, productId: Int?, productName: String, quantity: Double, price: Double, currency: String): Sale? {
        val customer = customerDao.getById(customerId) ?: return null
        val total = quantity * price

        // إن وجد المنتج في المخزن نخصم الكمية
        if (productId != null && productId > 0) {
            inventoryDao.deductQuantity(productId, quantity)
        }

        // تحديث مديونية العميل
        val deltaYer = if (currency == "YER") total else 0.0
        val deltaUsd = if (currency == "USD") total else 0.0
        customerDao.updateBalances(customerId, deltaYer, deltaUsd)

        val sale = Sale(
            customerId = customerId,
            customerName = customer.name,
            productName = productName,
            quantity = quantity,
            price = price,
            total = total,
            currency = currency,
            dateStr = getCurrentDateStr()
        )
        val id = saleDao.insert(sale)
        return sale.copy(id = id)
    }

    // إضافة دفعات وسند تسديد
    fun processPayment(customerId: Int, amount: Double, currency: String, notes: String): Payment? {
        val customer = customerDao.getById(customerId) ?: return null
        if (amount <= 0) return null

        // خصم المبلغ من دين العميل
        val deltaYer = if (currency == "YER") -amount else 0.0
        val deltaUsd = if (currency == "USD") -amount else 0.0
        customerDao.updateBalances(customerId, deltaYer, deltaUsd)

        val updatedCustomer = customerDao.getById(customerId) ?: customer

        val payment = Payment(
            customerId = customerId,
            customerName = customer.name,
            amount = amount,
            currency = currency,
            notes = notes,
            dateStr = getCurrentDateStr(),
            remainingYer = updatedCustomer.amountYer,
            remainingUsd = updatedCustomer.amountUsd
        )
        val id = paymentDao.insert(payment)
        return payment.copy(id = id)
    }

    // إضافة منتج للمخزن
    fun addInventoryItem(name: String, quantity: Double, price: Double, currency: String) {
        val item = InventoryItem(
            name = name,
            quantity = quantity,
            price = price,
            currency = currency
        )
        inventoryDao.insert(item)
    }
}

package com.wahab

import com.wahab.db.DatabaseManager
import com.wahab.service.AccountingService
import com.wahab.ui.UiComponents
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun main() {
    // تهيئة قاعدة البيانات SQLite عند بدء التشغيل
    DatabaseManager.getConnection()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            // الملفات الثابتة CSS
            get("/styles.css") {
                val cssFile = File("styles.css")
                if (cssFile.exists()) {
                    call.respondFile(cssFile)
                } else {
                    call.respondText("", ContentType.Text.CSS)
                }
            }

            // عرض التطبيق الرئيسي
            get("/") {
                val section = call.request.queryParameters["section"] ?: "dashboard"
                val html = UiComponents.renderMainApp(section)
                call.respondText(html, ContentType.Text.Html)
            }

            // إضافة عميل
            post("/api/customers/add") {
                val params = call.receiveParameters()
                val name = params["name"] ?: ""
                val phone = params["phone"] ?: ""
                val address = params["address"] ?: ""
                val item = params["item"] ?: ""
                val initialAmount = params["initialAmount"]?.toDoubleOrNull() ?: 0.0
                val currency = params["currency"] ?: "YER"

                if (name.isNotBlank()) {
                    AccountingService.addCustomer(name, phone, address, item, initialAmount, currency)
                }
                call.respondRedirect("/?section=customers")
            }

            // حذف عميل
            post("/api/customers/delete") {
                val params = call.receiveParameters()
                val id = params["id"]?.toIntOrNull()
                if (id != null) {
                    AccountingService.customerDao.delete(id)
                }
                call.respondRedirect("/?section=customers")
            }

            // إضافة بيع/فاتورة
            post("/api/sales/add") {
                val params = call.receiveParameters()
                val customerId = params["customerId"]?.toIntOrNull() ?: 0
                val productId = params["productId"]?.toIntOrNull()
                val productName = params["productName"] ?: ""
                val quantity = params["quantity"]?.toDoubleOrNull() ?: 1.0
                val price = params["price"]?.toDoubleOrNull() ?: 0.0
                val currency = params["currency"] ?: "YER"

                if (customerId > 0 && productName.isNotBlank()) {
                    AccountingService.processSale(customerId, productId, productName, quantity, price, currency)
                }
                call.respondRedirect("/?section=sales")
            }

            // إضافة تسديد/سند
            post("/api/payments/add") {
                val params = call.receiveParameters()
                val customerId = params["customerId"]?.toIntOrNull() ?: 0
                val amount = params["amount"]?.toDoubleOrNull() ?: 0.0
                val currency = params["currency"] ?: "YER"
                val notes = params["notes"] ?: ""

                if (customerId > 0 && amount > 0) {
                    AccountingService.processPayment(customerId, amount, currency, notes)
                }
                call.respondRedirect("/?section=payments")
            }

            // إضافة منتج للمخزن
            post("/api/inventory/add") {
                val params = call.receiveParameters()
                val name = params["name"] ?: ""
                val quantity = params["quantity"]?.toDoubleOrNull() ?: 0.0
                val price = params["price"]?.toDoubleOrNull() ?: 0.0
                val currency = params["currency"] ?: "YER"

                if (name.isNotBlank()) {
                    AccountingService.addInventoryItem(name, quantity, price, currency)
                }
                call.respondRedirect("/?section=inventory")
            }

            // حذف منتج من المخزن
            post("/api/inventory/delete") {
                val params = call.receiveParameters()
                val id = params["id"]?.toIntOrNull()
                if (id != null) {
                    AccountingService.inventoryDao.delete(id)
                }
                call.respondRedirect("/?section=inventory")
            }

            // حفظ الإعدادات
            post("/api/settings/save") {
                val params = call.receiveParameters()
                val storeName = params["storeName"] ?: ""
                val storeAddress = params["storeAddress"] ?: ""

                AccountingService.settingsDao.set("storeName", storeName)
                AccountingService.settingsDao.set("storeAddress", storeAddress)

                call.respondRedirect("/?section=settings")
            }
        }
    }.start(wait = true)
}

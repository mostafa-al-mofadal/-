package com.wahab.ui

import com.wahab.service.AccountingService
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

object UiComponents {

    fun renderMainApp(activeSection: String = "dashboard"): String {
        val customers = AccountingService.customerDao.getAll()
        val sales = AccountingService.saleDao.getAll()
        val payments = AccountingService.paymentDao.getAll()
        val inventory = AccountingService.inventoryDao.getAll()

        val yerSales = sales.filter { it.currency == "YER" }.sumOf { it.total }
        val yerPayments = payments.filter { it.currency == "YER" }.sumOf { it.amount }
        val yerDebts = customers.sumOf { it.amountYer }

        val usdSales = sales.filter { it.currency == "USD" }.sumOf { it.total }
        val usdPayments = payments.filter { it.currency == "USD" }.sumOf { it.amount }
        val usdDebts = customers.sumOf { it.amountUsd }

        val storeName = AccountingService.settingsDao.get("storeName").ifEmpty { "وهب للمحاسبة والتجارة" }
        val storeAddress = AccountingService.settingsDao.get("storeAddress").ifEmpty { "اليمن - صنعاء / هاتف: 770000000" }

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n")
        sb.appendHTML().html {
            attributes["lang"] = "ar"
            attributes["dir"] = "rtl"

            head {
                meta { charset = "UTF-8" }
                meta {
                    name = "viewport"
                    content = "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"
                }
                title { +"تطبيق وهب المحاسبي" }
                link(rel = "preconnect", href = "https://fonts.googleapis.com")
                link(rel = "preconnect", href = "https://fonts.gstatic.com") {
                    attributes["crossorigin"] = ""
                }
                link(
                    rel = "stylesheet",
                    href = "https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap"
                )
                link(
                    rel = "stylesheet",
                    href = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css"
                )
                link(rel = "stylesheet", href = "/styles.css")
            }

            body {
                header("app-header") {
                    div("logo-area") {
                        i("fa-solid fa-wallet brand-icon")
                        span("app-title") { +"وهب المحاسبي" }
                    }
                    div("header-actions") {
                        span("active-section-title") {
                            id = "current-section-title"
                            +getSectionTitle(activeSection)
                        }
                    }
                }

                main("main-container") {

                    // لوحة التحكم
                    section("app-section " + if (activeSection == "dashboard") "active" else "") {
                        id = "dashboard"
                        div("section-header") {
                            h2 {
                                i("fa-solid fa-chart-pie")
                                +" لوحة التحكم"
                            }
                        }

                        div("currency-group-title") {
                            i("fa-solid fa-coins")
                            +" ملخص بالريال اليمني (ر.ي)"
                        }
                        div("stats-grid") {
                            statCard("revenue-icon", "fa-arrow-down-left-and-up-right-to-center", "إجمالي الإيرادات", AccountingService.formatCurrency(yerPayments, "YER"))
                            statCard("expense-icon", "fa-arrow-up-right-from-square", "إجمالي المصروفات", AccountingService.formatCurrency(0.0, "YER"))
                            statCard("profit-icon", "fa-scale-balanced", "الأرباح", AccountingService.formatCurrency(yerSales, "YER"))
                            statCard("balance-icon", "fa-vault", "الرصيد المتبقي لدى العملاء", AccountingService.formatCurrency(yerDebts, "YER"))
                        }

                        div("currency-group-title") {
                            i("fa-solid fa-dollar-sign")
                            +" ملخص بالدولار الأمريكي ($)"
                        }
                        div("stats-grid") {
                            statCard("revenue-icon", "fa-arrow-down-left-and-up-right-to-center", "إجمالي الإيرادات", AccountingService.formatCurrency(usdPayments, "USD"))
                            statCard("expense-icon", "fa-arrow-up-right-from-square", "إجمالي المصروفات", AccountingService.formatCurrency(0.0, "USD"))
                            statCard("profit-icon", "fa-scale-balanced", "الأرباح", AccountingService.formatCurrency(usdSales, "USD"))
                            statCard("balance-icon", "fa-vault", "الرصيد المتبقي لدى العملاء", AccountingService.formatCurrency(usdDebts, "USD"))
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-clock-rotate-left")
                                    +" أحدث التحصيلات والمدفوعات"
                                }
                            }
                            div("table-responsive") {
                                table("app-table") {
                                    thead {
                                        tr {
                                            th { +"العميل" }
                                            th { +"المبلغ المدفوع" }
                                            th { +"العملة" }
                                            th { +"التاريخ" }
                                        }
                                    }
                                    tbody {
                                        if (payments.isEmpty()) {
                                            tr {
                                                td {
                                                    colSpan = "4"
                                                    style = "text-align:center;"
                                                    classes = setOf("text-muted")
                                                    +"لا توجد عمليات تحصيل بعد"
                                                }
                                            }
                                        } else {
                                            payments.take(5).forEach { p ->
                                                tr {
                                                    td { +p.customerName }
                                                    td { strong { +AccountingService.formatCurrency(p.amount, p.currency) } }
                                                    td {
                                                        span("badge-currency " + if (p.currency == "USD") "badge-usd" else "badge-yer") {
                                                            +(if (p.currency == "USD") "$" else "ر.ي")
                                                        }
                                                    }
                                                    td { +p.dateStr }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // العملاء
                    section("app-section " + if (activeSection == "customers") "active" else "") {
                        id = "customers"
                        div("section-header") {
                            h2 {
                                i("fa-solid fa-users")
                                +" إدارة العملاء"
                            }
                        }
                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-user-plus")
                                    +" إضافة عميل جديد"
                                }
                            }
                            form(action = "/api/customers/add", method = FormMethod.post) {
                                div("form-group") {
                                    label { +"اسم العميل" }
                                    input(type = InputType.text, name = "name", classes = "form-control") {
                                        placeholder = "أدخل اسم العميل الكامل"
                                        required = true
                                    }
                                }
                                div("form-group") {
                                    label { +"رقم الهاتف" }
                                    input(type = InputType.tel, name = "phone", classes = "form-control") {
                                        placeholder = "مثال: 771234567"
                                        required = true
                                    }
                                }
                                div("form-group") {
                                    label { +"العنوان" }
                                    input(type = InputType.text, name = "address", classes = "form-control") {
                                        placeholder = "أدخل العنوان السكني أو التجاري"
                                    }
                                }
                                div("form-group") {
                                    label { +"البيان / الحاجة المشتراة" }
                                    input(type = InputType.text, name = "item", classes = "form-control") {
                                        placeholder = "وصف المشتريات أو الخدمة"
                                    }
                                }
                                div("form-row") {
                                    div("form-group half") {
                                        label { +"المبلغ المتبقي السابِق" }
                                        input(type = InputType.number, name = "initialAmount", classes = "form-control") {
                                            placeholder = "0"
                                            value = "0"
                                        }
                                    }
                                    div("form-group half") {
                                        label { +"العملة" }
                                        select("form-control") {
                                            name = "currency"
                                            option { value = "YER"; +"ريال يمني (ر.ي)" }
                                            option { value = "USD"; +"دولار أمريكي ($)" }
                                        }
                                    }
                                }
                                button(type = ButtonType.submit, classes = "btn btn-primary btn-block") {
                                    i("fa-solid fa-plus")
                                    +" حفظ العميل"
                                }
                            }
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-address-book")
                                    +" قائمة العملاء"
                                }
                            }
                            div("table-responsive") {
                                table("app-table") {
                                    thead {
                                        tr {
                                            th { +"اسم العميل" }
                                            th { +"رقم الهاتف" }
                                            th { +"العنوان" }
                                            th { +"البيان" }
                                            th { +"المبلغ المتبقي" }
                                            th { +"إجراءات" }
                                        }
                                    }
                                    tbody {
                                        if (customers.isEmpty()) {
                                            tr {
                                                td {
                                                    colSpan = "6"
                                                    style = "text-align:center;"
                                                    classes = setOf("text-muted")
                                                    +"لا يوجد عملاء مسجلون"
                                                }
                                            }
                                        } else {
                                            customers.forEach { c ->
                                                tr {
                                                    td { strong { +c.name } }
                                                    td { +(if (c.phone.isNotEmpty()) c.phone else "-") }
                                                    td { +(if (c.address.isNotEmpty()) c.address else "-") }
                                                    td { +(if (c.item.isNotEmpty()) c.item else "-") }
                                                    td {
                                                        div { span("badge-currency badge-yer") { +AccountingService.formatCurrency(c.amountYer, "YER") } }
                                                        div { style = "margin-top:2px;"; span("badge-currency badge-usd") { +AccountingService.formatCurrency(c.amountUsd, "USD") } }
                                                    }
                                                    td {
                                                        form(action = "/api/customers/delete", method = FormMethod.post) {
                                                            style = "display:inline;"
                                                            input(type = InputType.hidden, name = "id") { value = c.id.toString() }
                                                            button(type = ButtonType.submit, classes = "btn btn-danger btn-sm") {
                                                                i("fa-solid fa-trash-can")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // الفواتير والمبيعات
                    section("app-section " + if (activeSection == "sales") "active" else "") {
                        id = "sales"
                        div("section-header") {
                            h2 {
                                i("fa-solid fa-file-invoice-dollar")
                                +" قائمة البيع والفواتير"
                            }
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-cart-plus")
                                    +" إنشاء عملية بيع جديدة"
                                }
                            }
                            form(action = "/api/sales/add", method = FormMethod.post) {
                                div("form-group") {
                                    label { +"اختر العميل" }
                                    select("form-control") {
                                        name = "customerId"
                                        required = true
                                        option { value = ""; +"اختر العميل..." }
                                        customers.forEach { c ->
                                            option { value = c.id.toString(); +c.name }
                                        }
                                    }
                                }
                                div("form-group") {
                                    label { +"اختر المنتج من المخزن (اختياري)" }
                                    select("form-control") {
                                        name = "productId"
                                        option { value = "0"; +"اختر المنتج..." }
                                        inventory.forEach { item ->
                                            option {
                                                value = item.id.toString()
                                                +"${item.name} - المتبقي (${item.quantity}) - ${AccountingService.formatCurrency(item.price, item.currency)}"
                                            }
                                        }
                                    }
                                }
                                div("form-group") {
                                    label { +"اسم الصنف / البيان" }
                                    input(type = InputType.text, name = "productName", classes = "form-control") {
                                        placeholder = "اسم المنتج أو وصف الخدمة"
                                        required = true
                                    }
                                }
                                div("form-row") {
                                    div("form-group half") {
                                        label { +"الكمية" }
                                        input(type = InputType.number, name = "quantity", classes = "form-control") {
                                            placeholder = "1"
                                            value = "1"
                                            required = true
                                        }
                                    }
                                    div("form-group half") {
                                        label { +"سعر الوحدة" }
                                        input(type = InputType.number, name = "price", classes = "form-control") {
                                            placeholder = "0"
                                            required = true
                                        }
                                    }
                                }
                                div("form-group") {
                                    label { +"العملة" }
                                    select("form-control") {
                                        name = "currency"
                                        option { value = "YER"; +"ريال يمني (ر.ي)" }
                                        option { value = "USD"; +"دولار أمريكي ($)" }
                                    }
                                }
                                button(type = ButtonType.submit, classes = "btn btn-primary btn-block") {
                                    i("fa-solid fa-check")
                                    +" اعتماد البيع وحفظ الفاتورة"
                                }
                            }
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-receipt")
                                    +" سجل المبيعات والفواتير"
                                }
                            }
                            div("table-responsive") {
                                table("app-table") {
                                    thead {
                                        tr {
                                            th { +"اسم العميل" }
                                            th { +"اسم المنتج" }
                                            th { +"الكمية" }
                                            th { +"السعر" }
                                            th { +"المجموع" }
                                            th { +"التاريخ" }
                                        }
                                    }
                                    tbody {
                                        if (sales.isEmpty()) {
                                            tr {
                                                td {
                                                    colSpan = "6"
                                                    style = "text-align:center;"
                                                    classes = setOf("text-muted")
                                                    +"لا توجد فواتير مبيعات سابقة"
                                                }
                                            }
                                        } else {
                                            sales.forEach { s ->
                                                tr {
                                                    td { strong { +s.customerName } }
                                                    td { +s.productName }
                                                    td { +s.quantity.toString() }
                                                    td { +AccountingService.formatCurrency(s.price, s.currency) }
                                                    td { strong { +AccountingService.formatCurrency(s.total, s.currency) } }
                                                    td { +s.dateStr }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // تسديد الأقساط
                    section("app-section " + if (activeSection == "payments") "active" else "") {
                        id = "payments"
                        div("section-header") {
                            h2 {
                                i("fa-solid fa-money-bill-wave")
                                +" تسديد الأقساط والديون"
                            }
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-hand-holding-dollar")
                                    +" تسجيل دفعة جديدة"
                                }
                            }
                            form(action = "/api/payments/add", method = FormMethod.post) {
                                div("form-group") {
                                    label { +"اختر العميل" }
                                    select("form-control") {
                                        name = "customerId"
                                        required = true
                                        option { value = ""; +"اختر العميل..." }
                                        customers.forEach { c ->
                                            option { value = c.id.toString(); +c.name }
                                        }
                                    }
                                }
                                div("form-row") {
                                    div("form-group half") {
                                        label { +"المبلغ المدفوع" }
                                        input(type = InputType.number, name = "amount", classes = "form-control") {
                                            placeholder = "أدخل المبلغ"
                                            required = true
                                        }
                                    }
                                    div("form-group half") {
                                        label { +"العملة" }
                                        select("form-control") {
                                            name = "currency"
                                            option { value = "YER"; +"ريال يمني (ر.ي)" }
                                            option { value = "USD"; +"دولار أمريكي ($)" }
                                        }
                                    }
                                }
                                div("form-group") {
                                    label { +"ملاحظات / بيان الدفعة" }
                                    input(type = InputType.text, name = "notes", classes = "form-control") {
                                        placeholder = "مثال: دفعة قسط شهر سبتمبر"
                                    }
                                }
                                button(type = ButtonType.submit, classes = "btn btn-success btn-block") {
                                    i("fa-solid fa-print")
                                    +" تسديد وحفظ السند"
                                }
                            }
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-list-check")
                                    +" سجل المقبوضات والسندات"
                                }
                            }
                            div("table-responsive") {
                                table("app-table") {
                                    thead {
                                        tr {
                                            th { +"اسم العميل" }
                                            th { +"المبلغ المدفوع" }
                                            th { +"العملة" }
                                            th { +"التاريخ" }
                                            th { +"ملاحظات" }
                                        }
                                    }
                                    tbody {
                                        if (payments.isEmpty()) {
                                            tr {
                                                td {
                                                    colSpan = "5"
                                                    style = "text-align:center;"
                                                    classes = setOf("text-muted")
                                                    +"لا يوجد سجل مدفوعات"
                                                }
                                            }
                                        } else {
                                            payments.forEach { p ->
                                                tr {
                                                    td { strong { +p.customerName } }
                                                    td { strong { +AccountingService.formatCurrency(p.amount, p.currency) } }
                                                    td {
                                                        span("badge-currency " + if (p.currency == "USD") "badge-usd" else "badge-yer") {
                                                            +(if (p.currency == "USD") "دولار" else "ريال يمني")
                                                        }
                                                    }
                                                    td { +p.dateStr }
                                                    td { +(if (p.notes.isNotEmpty()) p.notes else "-") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // المخزن
                    section("app-section " + if (activeSection == "inventory") "active" else "") {
                        id = "inventory"
                        div("section-header") {
                            h2 {
                                i("fa-solid fa-boxes-stacked")
                                +" إدارة المخزن والمنتجات"
                            }
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-box-open")
                                    +" إضافة منتج جديد"
                                }
                            }
                            form(action = "/api/inventory/add", method = FormMethod.post) {
                                div("form-group") {
                                    label { +"اسم المنتج" }
                                    input(type = InputType.text, name = "name", classes = "form-control") {
                                        placeholder = "أدخل اسم المنتج"
                                        required = true
                                    }
                                }
                                div("form-row") {
                                    div("form-group half") {
                                        label { +"الكمية المتوفرة" }
                                        input(type = InputType.number, name = "quantity", classes = "form-control") {
                                            placeholder = "0"
                                            required = true
                                        }
                                    }
                                    div("form-group half") {
                                        label { +"سعر البيع" }
                                        input(type = InputType.number, name = "price", classes = "form-control") {
                                            placeholder = "0"
                                            required = true
                                        }
                                    }
                                }
                                div("form-group") {
                                    label { +"العملة" }
                                    select("form-control") {
                                        name = "currency"
                                        option { value = "YER"; +"ريال يمني (ر.ي)" }
                                        option { value = "USD"; +"دولار أمريكي ($)" }
                                    }
                                }
                                button(type = ButtonType.submit, classes = "btn btn-primary btn-block") {
                                    i("fa-solid fa-plus")
                                    +" إضافة المنتج إلى المخزن"
                                }
                            }
                        }

                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-warehouse")
                                    +" المنتجات في المخزن"
                                }
                            }
                            div("table-responsive") {
                                table("app-table") {
                                    thead {
                                        tr {
                                            th { +"اسم المنتج" }
                                            th { +"الكمية" }
                                            th { +"السعر" }
                                            th { +"العملة" }
                                            th { +"إجراءات" }
                                        }
                                    }
                                    tbody {
                                        if (inventory.isEmpty()) {
                                            tr {
                                                td {
                                                    colSpan = "5"
                                                    style = "text-align:center;"
                                                    classes = setOf("text-muted")
                                                    +"المخزن فارغ حالياً"
                                                }
                                            }
                                        } else {
                                            inventory.forEach { item ->
                                                tr {
                                                    td { strong { +item.name } }
                                                    td { +item.quantity.toString() }
                                                    td { +AccountingService.formatCurrency(item.price, item.currency) }
                                                    td {
                                                        span("badge-currency " + if (item.currency == "USD") "badge-usd" else "badge-yer") {
                                                            +(if (item.currency == "USD") "دولار" else "ريال يمني")
                                                        }
                                                    }
                                                    td {
                                                        form(action = "/api/inventory/delete", method = FormMethod.post) {
                                                            style = "display:inline;"
                                                            input(type = InputType.hidden, name = "id") { value = item.id.toString() }
                                                            button(type = ButtonType.submit, classes = "btn btn-danger btn-sm") {
                                                                i("fa-solid fa-trash-can")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // التقارير
                    section("app-section " + if (activeSection == "reports") "active" else "") {
                        id = "reports"
                        div("section-header") {
                            h2 {
                                i("fa-solid fa-chart-line")
                                +" التقارير الماليّة الشاملة"
                            }
                        }
                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-file-contract")
                                    +" ملخص الكشف المالي"
                                }
                            }
                            div("report-summary") {
                                div("report-box") {
                                    h4 { +"الريال اليمني (ر.ي)" }
                                    p { +"إجمالي المبيعات: "; strong { +AccountingService.formatCurrency(yerSales, "YER") } }
                                    p { +"إجمالي التحصيلات: "; strong { +AccountingService.formatCurrency(yerPayments, "YER") } }
                                    p { +"إجمالي الديون المتبقية: "; strong { +AccountingService.formatCurrency(yerDebts, "YER") } }
                                }
                                div("report-box") {
                                    h4 { +"الدولار الأمريكي ($)" }
                                    p { +"إجمالي المبيعات: "; strong { +AccountingService.formatCurrency(usdSales, "USD") } }
                                    p { +"إجمالي التحصيلات: "; strong { +AccountingService.formatCurrency(usdPayments, "USD") } }
                                    p { +"إجمالي الديون المتبقية: "; strong { +AccountingService.formatCurrency(usdDebts, "USD") } }
                                }
                            }
                        }
                    }

                    // الإعدادات
                    section("app-section " + if (activeSection == "settings") "active" else "") {
                        id = "settings"
                        div("section-header") {
                            h2 {
                                i("fa-solid fa-sliders")
                                +" إعدادات التطبيق والمحل"
                            }
                        }
                        div("card-container") {
                            div("card-header") {
                                h3 {
                                    i("fa-solid fa-store")
                                    +" بيانات المحل / المؤسسة"
                                }
                            }
                            form(action = "/api/settings/save", method = FormMethod.post) {
                                div("form-group") {
                                    label { +"اسم المحل / النشاط" }
                                    input(type = InputType.text, name = "storeName", classes = "form-control") {
                                        value = storeName
                                    }
                                }
                                div("form-group") {
                                    label { +"العنوان ورقم التواصل" }
                                    input(type = InputType.text, name = "storeAddress", classes = "form-control") {
                                        value = storeAddress
                                    }
                                }
                                button(type = ButtonType.submit, classes = "btn btn-primary btn-block") {
                                    i("fa-solid fa-floppy-disk")
                                    +" حفظ الإعدادات"
                                }
                            }
                        }
                    }

                }

                // شريط الملاحة السفلي
                nav("bottom-nav") {
                    navItem("dashboard", "fa-house", "الرئيسية", activeSection)
                    navItem("customers", "fa-users", "العملاء", activeSection)
                    navItem("sales", "fa-receipt", "الفواتير", activeSection)
                    navItem("payments", "fa-money-bill-wave", "الأقساط", activeSection)
                    navItem("inventory", "fa-boxes-stacked", "المخزن", activeSection)
                    navItem("reports", "fa-chart-line", "التقارير", activeSection)
                    navItem("settings", "fa-gear", "الإعدادات", activeSection)
                }
            }
        }

        return sb.toString()
    }

    private fun NAV.navItem(section: String, iconClass: String, labelText: String, activeSection: String) {
        a(href = "/?section=$section", classes = "nav-item " + if (activeSection == section) "active" else "") {
            i("fa-solid $iconClass")
            span { +labelText }
        }
    }

    private fun DIV.statCard(iconClass: String, faIcon: String, titleText: String, valueText: String) {
        div("stat-card") {
            div("stat-icon $iconClass") {
                i("fa-solid $faIcon")
            }
            div("stat-info") {
                span { +titleText }
                h3 { +valueText }
            }
        }
    }

    private fun getSectionTitle(section: String): String {
        return when (section) {
            "dashboard" -> "الرئيسية"
            "customers" -> "العملاء"
            "sales" -> "الفواتير"
            "payments" -> "الأقساط"
            "inventory" -> "المخزن"
            "reports" -> "التقارير"
            "settings" -> "الإعدادات"
            else -> "تطبيق وهب"
        }
    }
}

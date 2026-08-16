/* ==========================================================================
   المنطق البرمجي وإدارة البيانات لتطبيق وهب المحاسبي
   ========================================================================== */

// حالة التطبيق المخزنة محلياً
let customersData = loadData('wahab_customers') || [];
let inventoryData = loadData('wahab_inventory') || [];
let salesData = loadData('wahab_sales') || [];
let paymentsData = loadData('wahab_payments') || [];
let settingsData = loadData('wahab_settings') || {
    storeName: 'وهب للمحاسبة والتجارة',
    storeAddress: 'اليمن - صنعاء / هاتف: 770000000'
};

// --------------------------------------------------------------------------
// التبديل بين الأقسام المفتوحة
// --------------------------------------------------------------------------
function showSection(sectionId, element) {
    document.querySelectorAll('.app-section').forEach(sec => sec.classList.remove('active'));
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        targetSection.classList.add('active');
    }

    if (element) {
        document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
        element.classList.add('active');
    }

    // تحديث عنوان الهيدر
    const titles = {
        'dashboard': 'الرئيسية',
        'customers': 'العملاء',
        'sales': 'الفواتير',
        'payments': 'الأقساط',
        'inventory': 'المخزن',
        'reports': 'التقارير',
        'settings': 'الإعدادات'
    };
    document.getElementById('current-section-title').textContent = titles[sectionId] || 'تطبيق وهب';

    // إعادة تحديث القوائم عند الانتقال
    refreshAllViews();
}

// --------------------------------------------------------------------------
// دالة تنسيق الأرقام والعملة بضمان استخدام الأرقام الإنجليزية (0-9)
// --------------------------------------------------------------------------
function formatCurrency(amount, currency = 'YER') {
    const num = parseFloat(amount) || 0;
    // استخدام en-US لضمان عدم تحويل الأرقام إلى عربية أو هندية
    const formattedNum = num.toLocaleString('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    });

    if (currency === 'USD') {
        return `$${formattedNum}`;
    }
    return `${formattedNum} ر.ي`;
}

// --------------------------------------------------------------------------
// إضافة عميل جديد
// --------------------------------------------------------------------------
function addCustomer(event) {
    event.preventDefault();
    const name = document.getElementById('customer-name').value.trim();
    const phone = document.getElementById('customer-phone').value.trim();
    const address = document.getElementById('customer-address').value.trim();
    const item = document.getElementById('customer-item').value.trim();
    const amount = parseFloat(document.getElementById('customer-initial-amount').value) || 0;
    const currency = document.getElementById('customer-currency').value;

    if (!name) {
        alert('يرجى إدخال اسم العميل.');
        return;
    }

    const newCustomer = {
        id: Date.now(),
        name,
        phone,
        address,
        item,
        amountYER: currency === 'YER' ? amount : 0,
        amountUSD: currency === 'USD' ? amount : 0
    };

    customersData.push(newCustomer);
    saveData('wahab_customers', customersData);

    // إعادة تعيين النموذج
    document.getElementById('add-customer-form').reset();
    document.getElementById('customer-initial-amount').value = 0;

    refreshAllViews();
    alert('تم إضافة العميل بنجاح.');
}

// --------------------------------------------------------------------------
// عرض وإدارة العملاء
// --------------------------------------------------------------------------
function showCustomers(filterText = '') {
    const customersList = document.getElementById('customers-list');
    customersList.innerHTML = '';

    const filtered = customersData.filter(c =>
        c.name.toLowerCase().includes(filterText.toLowerCase()) ||
        c.phone.includes(filterText)
    );

    if (filtered.length === 0) {
        customersList.innerHTML = `<tr><td colspan="6" style="text-align:center;" class="text-muted">لا يوجد عملاء مطابقون</td></tr>`;
        return;
    }

    filtered.forEach(customer => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><strong>${customer.name}</strong></td>
            <td>${customer.phone || '-'}</td>
            <td>${customer.address || '-'}</td>
            <td>${customer.item || '-'}</td>
            <td>
                <div><span class="badge-currency badge-yer">${formatCurrency(customer.amountYER || 0, 'YER')}</span></div>
                <div style="margin-top:2px;"><span class="badge-currency badge-usd">${formatCurrency(customer.amountUSD || 0, 'USD')}</span></div>
            </td>
            <td>
                <button onclick="deleteCustomer(${customer.id})" class="btn btn-danger btn-sm">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </td>
        `;
        customersList.appendChild(row);
    });
}

function filterCustomers() {
    const text = document.getElementById('search-customer').value;
    showCustomers(text);
}

function deleteCustomer(id) {
    if (confirm('هل أنت تأكد من رغبتك في حذف هذا العميل؟')) {
        customersData = customersData.filter(c => c.id !== id);
        saveData('wahab_customers', customersData);
        refreshAllViews();
    }
}

// --------------------------------------------------------------------------
// تغذية القوائم المنسدلة بالعملاء والمنتجات
// --------------------------------------------------------------------------
function populateSelects() {
    const saleCustomerSelect = document.getElementById('sale-customer-select');
    const paymentCustomerSelect = document.getElementById('payment-customer-select');
    const saleProductSelect = document.getElementById('sale-product-select');

    saleCustomerSelect.innerHTML = '<option value="">اختر العميل...</option>';
    paymentCustomerSelect.innerHTML = '<option value="">اختر العميل...</option>';

    customersData.forEach(customer => {
        const option = document.createElement('option');
        option.value = customer.id;
        option.textContent = customer.name;

        saleCustomerSelect.appendChild(option);
        paymentCustomerSelect.appendChild(option.cloneNode(true));
    });

    saleProductSelect.innerHTML = '<option value="">اختر المنتج...</option>';
    inventoryData.forEach(product => {
        const option = document.createElement('option');
        option.value = product.id;
        option.textContent = `${product.name} - المتبقي (${product.quantity}) - ${formatCurrency(product.price, product.currency)}`;
        saleProductSelect.appendChild(option);
    });
}

// --------------------------------------------------------------------------
// إدارة المبيعات والفواتير
// --------------------------------------------------------------------------
function onSaleProductChange() {
    const productId = parseInt(document.getElementById('sale-product-select').value);
    const product = inventoryData.find(p => p.id === productId);
    if (product) {
        document.getElementById('sale-product-name').value = product.name;
        document.getElementById('sale-price').value = product.price;
        document.getElementById('sale-currency').value = product.currency || 'YER';
        calculateSaleTotal();
    }
}

function calculateSaleTotal() {
    const quantity = parseFloat(document.getElementById('sale-quantity').value) || 0;
    const price = parseFloat(document.getElementById('sale-price').value) || 0;
    const currency = document.getElementById('sale-currency').value;
    const total = quantity * price;

    document.getElementById('sale-total-display').textContent = formatCurrency(total, currency);
}

function addSale(event) {
    event.preventDefault();
    const customerId = parseInt(document.getElementById('sale-customer-select').value);
    const productId = parseInt(document.getElementById('sale-product-select').value);
    const productName = document.getElementById('sale-product-name').value.trim();
    const quantity = parseFloat(document.getElementById('sale-quantity').value) || 0;
    const price = parseFloat(document.getElementById('sale-price').value) || 0;
    const currency = document.getElementById('sale-currency').value;

    const customer = customersData.find(c => c.id === customerId);
    if (!customer) {
        alert('يرجى اختيار العميل.');
        return;
    }

    const total = quantity * price;

    // خصم الكمية من المخزن إن وجد المنتج
    if (productId) {
        const product = inventoryData.find(p => p.id === productId);
        if (product) {
            product.quantity = Math.max(0, product.quantity - quantity);
            saveData('wahab_inventory', inventoryData);
        }
    }

    // زيادة مديونية العميل بالعملة المناسبة
    if (currency === 'USD') {
        customer.amountUSD = (customer.amountUSD || 0) + total;
    } else {
        customer.amountYER = (customer.amountYER || 0) + total;
    }
    saveData('wahab_customers', customersData);

    const newSale = {
        id: Date.now(),
        customerId: customer.id,
        customerName: customer.name,
        productName,
        quantity,
        price,
        total,
        currency,
        date: new Date().toLocaleDateString('en-US')
    };

    salesData.unshift(newSale);
    saveData('wahab_sales', salesData);

    document.getElementById('add-sale-form').reset();
    document.getElementById('sale-quantity').value = 1;
    calculateSaleTotal();

    refreshAllViews();
    printInvoice(newSale);
}

function showSales() {
    const salesList = document.getElementById('sales-list');
    salesList.innerHTML = '';

    if (salesData.length === 0) {
        salesList.innerHTML = `<tr><td colspan="7" style="text-align:center;" class="text-muted">لا توجد فواتير مبيعات سابقة</td></tr>`;
        return;
    }

    salesData.forEach(sale => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><strong>${sale.customerName}</strong></td>
            <td>${sale.productName}</td>
            <td>${sale.quantity}</td>
            <td>${formatCurrency(sale.price, sale.currency)}</td>
            <td><strong>${formatCurrency(sale.total, sale.currency)}</strong></td>
            <td>${sale.date}</td>
            <td>
                <button onclick="reprintSaleInvoice(${sale.id})" class="btn btn-primary btn-sm">
                    <i class="fa-solid fa-print"></i>
                </button>
            </td>
        `;
        salesList.appendChild(row);
    });
}

function reprintSaleInvoice(saleId) {
    const sale = salesData.find(s => s.id === saleId);
    if (sale) {
        printInvoice(sale);
    }
}

// --------------------------------------------------------------------------
// تسديد الأقساط
// --------------------------------------------------------------------------
function onPaymentCustomerChange() {
    const customerId = parseInt(document.getElementById('payment-customer-select').value);
    const infoBox = document.getElementById('payment-customer-info');
    const customer = customersData.find(c => c.id === customerId);

    if (customer) {
        infoBox.style.display = 'block';
        infoBox.innerHTML = `
            <strong>العميل: ${customer.name}</strong><br>
            المبلغ المتبقي (ريال يمني): <strong>${formatCurrency(customer.amountYER || 0, 'YER')}</strong><br>
            المبلغ المتبقي (دولار أمريكي): <strong>${formatCurrency(customer.amountUSD || 0, 'USD')}</strong>
        `;
    } else {
        infoBox.style.display = 'none';
    }
}

function submitPayment(event) {
    event.preventDefault();
    const customerId = parseInt(document.getElementById('payment-customer-select').value);
    const amount = parseFloat(document.getElementById('payment-amount').value) || 0;
    const currency = document.getElementById('payment-currency').value;
    const notes = document.getElementById('payment-notes').value.trim();

    const customer = customersData.find(c => c.id === customerId);
    if (!customer || amount <= 0) {
        alert('يرجى اختيار العميل وتحديد مبلغ صحيح.');
        return;
    }

    // خصم المبلغ من دين العميل
    if (currency === 'USD') {
        customer.amountUSD = (customer.amountUSD || 0) - amount;
    } else {
        customer.amountYER = (customer.amountYER || 0) - amount;
    }
    saveData('wahab_customers', customersData);

    const payment = {
        id: Date.now(),
        customerId: customer.id,
        customerName: customer.name,
        amount,
        currency,
        notes,
        date: new Date().toLocaleDateString('en-US'),
        remainingYER: customer.amountYER || 0,
        remainingUSD: customer.amountUSD || 0
    };

    paymentsData.unshift(payment);
    saveData('wahab_payments', paymentsData);

    document.getElementById('payment-form').reset();
    document.getElementById('payment-customer-info').style.display = 'none';

    refreshAllViews();
    printPaymentReceipt(payment);
}

function showPayments() {
    const paymentList = document.getElementById('payment-history-list');
    const recentList = document.getElementById('recent-payments-list');

    paymentList.innerHTML = '';
    if (recentList) recentList.innerHTML = '';

    if (paymentsData.length === 0) {
        paymentList.innerHTML = `<tr><td colspan="6" style="text-align:center;" class="text-muted">لا يوجد سجل مدفوعات</td></tr>`;
        if (recentList) recentList.innerHTML = `<tr><td colspan="4" style="text-align:center;" class="text-muted">لا توجد عمليات تحصيل بعد</td></tr>`;
        return;
    }

    paymentsData.forEach((p, idx) => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><strong>${p.customerName}</strong></td>
            <td><strong>${formatCurrency(p.amount, p.currency)}</strong></td>
            <td><span class="badge-currency ${p.currency === 'USD' ? 'badge-usd' : 'badge-yer'}">${p.currency === 'USD' ? 'دولار' : 'ريال يمني'}</span></td>
            <td>${p.date}</td>
            <td>${p.notes || '-'}</td>
            <td>
                <button onclick="reprintPaymentReceipt(${p.id})" class="btn btn-success btn-sm">
                    <i class="fa-solid fa-print"></i>
                </button>
            </td>
        `;
        paymentList.appendChild(row);

        if (recentList && idx < 5) {
            const recentRow = document.createElement('tr');
            recentRow.innerHTML = `
                <td>${p.customerName}</td>
                <td><strong>${formatCurrency(p.amount, p.currency)}</strong></td>
                <td><span class="badge-currency ${p.currency === 'USD' ? 'badge-usd' : 'badge-yer'}">${p.currency === 'USD' ? '$' : 'ر.ي'}</span></td>
                <td>${p.date}</td>
            `;
            recentList.appendChild(recentRow);
        }
    });
}

function reprintPaymentReceipt(paymentId) {
    const payment = paymentsData.find(p => p.id === paymentId);
    if (payment) {
        printPaymentReceipt(payment);
    }
}

// --------------------------------------------------------------------------
// إدارة المخزن
// --------------------------------------------------------------------------
function addProduct(event) {
    event.preventDefault();
    const name = document.getElementById('product-name').value.trim();
    const quantity = parseFloat(document.getElementById('product-quantity').value) || 0;
    const price = parseFloat(document.getElementById('product-price').value) || 0;
    const currency = document.getElementById('product-currency').value;

    if (!name) {
        alert('يرجى إدخال اسم المنتج.');
        return;
    }

    const product = {
        id: Date.now(),
        name,
        quantity,
        price,
        currency
    };

    inventoryData.push(product);
    saveData('wahab_inventory', inventoryData);

    document.getElementById('add-product-form').reset();
    refreshAllViews();
    alert('تم إضافة المنتج للمخزن.');
}

function showInventory() {
    const inventoryList = document.getElementById('inventory-list');
    inventoryList.innerHTML = '';

    if (inventoryData.length === 0) {
        inventoryList.innerHTML = `<tr><td colspan="5" style="text-align:center;" class="text-muted">المخزن فارغ حالياً</td></tr>`;
        return;
    }

    inventoryData.forEach(product => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td><strong>${product.name}</strong></td>
            <td>${product.quantity}</td>
            <td>${formatCurrency(product.price, product.currency)}</td>
            <td><span class="badge-currency ${product.currency === 'USD' ? 'badge-usd' : 'badge-yer'}">${product.currency === 'USD' ? 'دولار' : 'ريال يمني'}</span></td>
            <td>
                <button onclick="deleteProduct(${product.id})" class="btn btn-danger btn-sm">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </td>
        `;
        inventoryList.appendChild(row);
    });
}

function deleteProduct(id) {
    if (confirm('هل أنت تأكد من حذف المنتج من المخزن؟')) {
        inventoryData = inventoryData.filter(p => p.id !== id);
        saveData('wahab_inventory', inventoryData);
        refreshAllViews();
    }
}

// --------------------------------------------------------------------------
// لوحة التحكم والتقارير المالية
// --------------------------------------------------------------------------
function updateDashboardAndReports() {
    // حسابات الريال اليمني
    const yerSales = salesData.filter(s => s.currency === 'YER').reduce((sum, s) => sum + (s.total || 0), 0);
    const yerPayments = paymentsData.filter(p => p.currency === 'YER').reduce((sum, p) => sum + (p.amount || 0), 0);
    const yerDebts = customersData.reduce((sum, c) => sum + (c.amountYER || 0), 0);

    // حسابات الدولار
    const usdSales = salesData.filter(s => s.currency === 'USD').reduce((sum, s) => sum + (s.total || 0), 0);
    const usdPayments = paymentsData.filter(p => p.currency === 'USD').reduce((sum, p) => sum + (p.amount || 0), 0);
    const usdDebts = customersData.reduce((sum, c) => sum + (c.amountUSD || 0), 0);

    // تحديث الواجهة الرئيسية - الريال اليمني
    document.getElementById('yer-revenue').textContent = formatCurrency(yerPayments, 'YER');
    document.getElementById('yer-expenses').textContent = formatCurrency(0, 'YER');
    document.getElementById('yer-profit').textContent = formatCurrency(yerSales, 'YER');
    document.getElementById('yer-balance').textContent = formatCurrency(yerDebts, 'YER');

    // تحديث الواجهة الرئيسية - الدولار
    document.getElementById('usd-revenue').textContent = formatCurrency(usdPayments, 'USD');
    document.getElementById('usd-expenses').textContent = formatCurrency(0, 'USD');
    document.getElementById('usd-profit').textContent = formatCurrency(usdSales, 'USD');
    document.getElementById('usd-balance').textContent = formatCurrency(usdDebts, 'USD');

    // تحديث قسم التقارير
    document.getElementById('report-yer-sales').textContent = formatCurrency(yerSales, 'YER');
    document.getElementById('report-yer-payments').textContent = formatCurrency(yerPayments, 'YER');
    document.getElementById('report-yer-debts').textContent = formatCurrency(yerDebts, 'YER');

    document.getElementById('report-usd-sales').textContent = formatCurrency(usdSales, 'USD');
    document.getElementById('report-usd-payments').textContent = formatCurrency(usdPayments, 'USD');
    document.getElementById('report-usd-debts').textContent = formatCurrency(usdDebts, 'USD');
}

// --------------------------------------------------------------------------
// طباعة فاتورة بيع وسند تسديد بتصميم عربي RTL متناسق
// --------------------------------------------------------------------------
function printInvoice(sale) {
    const printWin = window.open('', '_blank', 'width=650,height=600');
    printWin.document.write(`
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <title>فاتورة بيع - ${sale.id}</title>
            <style>
                body { font-family: 'Cairo', Arial, sans-serif; direction: rtl; padding: 20px; color: #0F172A; }
                .invoice-box { max-width: 500px; margin: auto; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; }
                .header { text-align: center; border-bottom: 2px dashed #CBD5E1; padding-bottom: 12px; margin-bottom: 15px; }
                .header h2 { margin: 0; color: #0F172A; }
                .header p { margin: 4px 0; color: #64748B; font-size: 0.9rem; }
                .details-table { width: 100%; border-collapse: collapse; margin: 15px 0; }
                .details-table th, .details-table td { border: 1px solid #E2E8F0; padding: 8px 10px; text-align: right; font-size: 0.9rem; }
                .details-table th { background-color: #F8FAFC; color: #475569; }
                .total-row { font-weight: bold; background-color: #D1FAE5; }
                .footer { text-align: center; margin-top: 20px; font-size: 0.85rem; color: #64748B; border-top: 1px solid #E2E8F0; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="invoice-box">
                <div class="header">
                    <h2>${settingsData.storeName}</h2>
                    <p>${settingsData.storeAddress}</p>
                    <p><strong>فاتورة بيع رقم:</strong> ${sale.id}</p>
                    <p><strong>التاريخ:</strong> ${sale.date}</p>
                </div>
                <div>
                    <p><strong>اسم العميل:</strong> ${sale.customerName}</p>
                </div>
                <table class="details-table">
                    <thead>
                        <tr>
                            <th>الصنف / البيان</th>
                            <th>الكمية</th>
                            <th>السعر</th>
                            <th>المجموع</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>${sale.productName}</td>
                            <td>${sale.quantity}</td>
                            <td>${formatCurrency(sale.price, sale.currency)}</td>
                            <td>${formatCurrency(sale.total, sale.currency)}</td>
                        </tr>
                        <tr class="total-row">
                            <td colspan="3">الإجمالي الكلي</td>
                            <td>${formatCurrency(sale.total, sale.currency)}</td>
                        </tr>
                    </tbody>
                </table>
                <div class="footer">
                    <p>شكراً لتعاملكم معنا!</p>
                </div>
            </div>
        </body>
        </html>
    `);
    printWin.document.close();
    printWin.focus();
    setTimeout(() => { printWin.print(); }, 250);
}

function printPaymentReceipt(payment) {
    const printWin = window.open('', '_blank', 'width=650,height=600');
    printWin.document.write(`
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <title>سند قبض - ${payment.id}</title>
            <style>
                body { font-family: 'Cairo', Arial, sans-serif; direction: rtl; padding: 20px; color: #0F172A; }
                .receipt-box { max-width: 500px; margin: auto; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px; }
                .header { text-align: center; border-bottom: 2px dashed #CBD5E1; padding-bottom: 12px; margin-bottom: 15px; }
                .header h2 { margin: 0; color: #059669; }
                .header p { margin: 4px 0; color: #64748B; font-size: 0.9rem; }
                .details { margin: 15px 0; font-size: 0.95rem; line-height: 1.8; }
                .amount-box { background-color: #D1FAE5; color: #047857; text-align: center; font-size: 1.2rem; font-weight: bold; padding: 10px; border-radius: 8px; margin: 15px 0; }
                .footer { text-align: center; margin-top: 20px; font-size: 0.85rem; color: #64748B; border-top: 1px solid #E2E8F0; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="receipt-box">
                <div class="header">
                    <h2>${settingsData.storeName}</h2>
                    <p>${settingsData.storeAddress}</p>
                    <p><strong>سند قبض رقم:</strong> ${payment.id}</p>
                    <p><strong>التاريخ:</strong> ${payment.date}</p>
                </div>
                <div class="details">
                    <p>وصلنا من الأخ / الأخوات: <strong>${payment.customerName}</strong></p>
                    <p>مبلغ وقدره: <strong>${formatCurrency(payment.amount, payment.currency)}</strong></p>
                    <p>وذلك مقابل: <strong>${payment.notes || 'تسديد قسط / دفعة من الحساب'}</strong></p>
                </div>
                <div class="amount-box">
                    المبلغ المدفوع: ${formatCurrency(payment.amount, payment.currency)}
                </div>
                <div class="details">
                    <p>المبلغ المتبقي بالريال اليمني: <strong>${formatCurrency(payment.remainingYER, 'YER')}</strong></p>
                    <p>المبلغ المتبقي بالدولار: <strong>${formatCurrency(payment.remainingUSD, 'USD')}</strong></p>
                </div>
                <div class="footer">
                    <p>توقيع المستلم: ...............................</p>
                </div>
            </div>
        </body>
        </html>
    `);
    printWin.document.close();
    printWin.focus();
    setTimeout(() => { printWin.print(); }, 250);
}

// --------------------------------------------------------------------------
// الإعدادات وإدارة البيانات
// --------------------------------------------------------------------------
function saveSettings(event) {
    event.preventDefault();
    settingsData.storeName = document.getElementById('store-name').value.trim();
    settingsData.storeAddress = document.getElementById('store-address').value.trim();
    saveData('wahab_settings', settingsData);
    alert('تم حفظ إعدادات المحل بنجاح.');
}

function resetData() {
    if (confirm('تحذير: هل أنت متأكد من مسح كافة البيانات بشكل نهائي؟ لا يمكن التراجع عن هذه العملية.')) {
        localStorage.clear();
        customersData = [];
        inventoryData = [];
        salesData = [];
        paymentsData = [];
        refreshAllViews();
        alert('تم مسح جميع البيانات.');
    }
}

// --------------------------------------------------------------------------
// الدعم والتخزين المحلي localStorage
// --------------------------------------------------------------------------
function saveData(key, data) {
    try {
        localStorage.setItem(key, JSON.stringify(data));
    } catch (e) {
        console.error('فشل حفظ البيانات في LocalStorage', e);
    }
}

function loadData(key) {
    try {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : null;
    } catch (e) {
        console.error('فشل قراءة البيانات من LocalStorage', e);
        return null;
    }
}

function refreshAllViews() {
    showCustomers();
    populateSelects();
    showSales();
    showPayments();
    showInventory();
    updateDashboardAndReports();
}

// بداية تشغيل التطبيق عند تحميل المستند
document.addEventListener('DOMContentLoaded', () => {
    // تعبئة بيانات الإعدادات
    if (settingsData.storeName) document.getElementById('store-name').value = settingsData.storeName;
    if (settingsData.storeAddress) document.getElementById('store-address').value = settingsData.storeAddress;

    refreshAllViews();
});

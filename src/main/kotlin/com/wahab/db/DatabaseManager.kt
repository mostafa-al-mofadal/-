package com.wahab.db

import java.sql.Connection
import java.sql.DriverManager

object DatabaseManager {
    private const val DB_URL = "jdbc:sqlite:wahab_database.db"

    init {
        Class.forName("org.sqlite.JDBC")
        initDatabase()
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection(DB_URL)
    }

    private fun initDatabase() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // جدول العملاء
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS customers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT,
                        address TEXT,
                        item TEXT,
                        amount_yer REAL DEFAULT 0.0,
                        amount_usd REAL DEFAULT 0.0
                    );
                """.trimIndent())

                // جدول المخزون
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS inventory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        quantity REAL DEFAULT 0.0,
                        price REAL DEFAULT 0.0,
                        currency TEXT DEFAULT 'YER'
                    );
                """.trimIndent())

                // جدول المبيعات والفواتير
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS sales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        customer_id INTEGER,
                        customer_name TEXT,
                        product_name TEXT,
                        quantity REAL,
                        price REAL,
                        total REAL,
                        currency TEXT,
                        date_str TEXT,
                        FOREIGN KEY (customer_id) REFERENCES customers(id)
                    );
                """.trimIndent())

                // جدول المقبوضات والأقساط
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        customer_id INTEGER,
                        customer_name TEXT,
                        amount REAL,
                        currency TEXT,
                        notes TEXT,
                        date_str TEXT,
                        remaining_yer REAL,
                        remaining_usd REAL,
                        FOREIGN KEY (customer_id) REFERENCES customers(id)
                    );
                """.trimIndent())

                // جدول الإعدادات
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT
                    );
                """.trimIndent())

                // إدخال الإعدادات الافتراضية إن لم تكن موجودة
                stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('storeName', 'وهب للمحاسبة والتجارة');")
                stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('storeAddress', 'اليمن - صنعاء / هاتف: 770000000');")
            }
        }
    }
}

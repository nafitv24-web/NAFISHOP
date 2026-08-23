package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "মুদি",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stockQuantity: Double = 0.0,
    val unit: String = "পিস",
    val minStockAlert: Double = 5.0,
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "SALE", "STOCK_IN", "PURCHASE", "DAMAGE"
    val invoiceNumber: String = "",
    val productId: Long = 0,
    val productName: String,
    val quantity: Double,
    val unit: String = "পিস",
    val unitPrice: Double,
    val costPrice: Double = 0.0,
    val totalAmount: Double,
    val profitAmount: Double = 0.0,
    val customerName: String = "",
    val customerPhone: String = "",
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, BKASH, NAGAD, DUE
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val totalDue: Double = 0.0,
    val totalPurchased: Double = 0.0,
    val lastTransactionDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "due_logs")
data class DueLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String = "",
    val type: String, // "DUE_GIVEN", "DUE_COLLECTED"
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "অন্যান্য",
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "cash_logs")
data class CashLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "DAY_END_CLOSING", "DEPOSIT", "WITHDRAWAL", "MANUAL_ADJUST"
    val amount: Double,
    val balanceAfter: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class CartItem(
    val product: Product,
    var quantity: Double = 1.0,
    var customPrice: Double = product.sellPrice
) {
    val total: Double
        get() = quantity * customPrice

    val profit: Double
        get() = quantity * (customPrice - product.buyPrice)
}

data class DashboardSummary(
    val mainBalance: Double = 25000.0,
    val todaySales: Double = 0.0,
    val todayCashSales: Double = 0.0,
    val todayPurchases: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val todayNetCashFlow: Double = 0.0,
    val totalOutstandingDue: Double = 0.0,
    val totalStockValue: Double = 0.0,
    val totalProductsCount: Int = 0,
    val lowStockCount: Int = 0
)

data class ShopInfo(
    val shopName: String = "ভাই ভাই স্টোর",
    val ownerName: String = "মোঃ রফিকুল ইসলাম",
    val phone: String = "01712345678",
    val address: String = "নিউ মার্কেট, ঢাকা",
    val currency: String = "৳",
    val mainBalance: Double = 25000.0,
    val userEmail: String = "nafitv24@gmail.com",
    val isGoogleLinked: Boolean = true,
    val lastBackupDate: String = "আজ, ৩:৪৫ PM"
)

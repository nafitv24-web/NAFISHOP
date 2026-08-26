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
    val expiryDate: Long = 0L, // 0L means no expiry date specified
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
    val imageUri: String = "",
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
    val todayDueSales: Double = 0.0,
    val todayCollectedDue: Double = 0.0,
    val todayNewDueGiven: Double = 0.0,
    val todayEstimatedDrawerCash: Double = 0.0,
    val todayPurchases: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todayRealizedProfit: Double = 0.0,
    val todayDueProfit: Double = 0.0,
    val todayProfitMargin: Double = 0.0,
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

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val productCount: Int = 0,
    val customerCount: Int = 0,
    val transactionCount: Int = 0,
    val expenseCount: Int = 0,
    val dueLogCount: Int = 0,
    val cashLogCount: Int = 0,
    val restoredShopInfo: ShopInfo? = null
)

data class AppUpdateInfo(
    val versionName: String = "2.4.0",
    val versionCode: Int = 24,
    val downloadUrl: String = "https://drive.google.com",
    val releaseNotes: String = "নতুন দ্রুত বিক্রয় (POS), কাস্টমার ট্রানজেকশন হিস্ট্রি ও মেয়াদ ট্র্যাকিং সুবিধা যুক্ত করা হয়েছে।",
    val isForceUpdate: Boolean = false,
    val isUpdateActive: Boolean = true,
    val releaseDate: Long = System.currentTimeMillis()
)

data class AppNotice(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val type: String = "INFO", // "INFO", "ALERT", "OFFER", "FEATURE"
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val actionUrl: String = ""
)


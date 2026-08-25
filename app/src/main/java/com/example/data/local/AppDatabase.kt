package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        TransactionRecord::class,
        Customer::class,
        DueLog::class,
        Expense::class,
        CashLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun customerDao(): CustomerDao
    abstract fun dueLogDao(): DueLogDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashLogDao(): CashLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shop_khata_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Start with a clean slate (no mock or demo data)
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            // 1. Initial Products (Popular items in Bangladeshi stores)
            val sampleProducts = listOf(
                Product(
                    name = "মিনিকেট চাল (২৫ কেজি)",
                    barcode = "89411001",
                    category = "চাল ও ডাল",
                    buyPrice = 1650.0,
                    sellPrice = 1800.0,
                    stockQuantity = 14.0,
                    unit = "বস্তা",
                    minStockAlert = 3.0
                ),
                Product(
                    name = "তীর সয়াবিন তেল (৫ লিটার)",
                    barcode = "89411002",
                    category = "তেল ও ঘি",
                    buyPrice = 860.0,
                    sellPrice = 920.0,
                    stockQuantity = 8.0,
                    unit = "বোতল",
                    minStockAlert = 4.0
                ),
                Product(
                    name = "ফ্রেশ সাদা চিনি (১ কেজি)",
                    barcode = "89411003",
                    category = "মুদি সামগ্রী",
                    buyPrice = 125.0,
                    sellPrice = 135.0,
                    stockQuantity = 22.0,
                    unit = "প্যাকেট",
                    minStockAlert = 5.0
                ),
                Product(
                    name = "দেশি মসুর ডাল (১ কেজি)",
                    barcode = "89411004",
                    category = "চাল ও ডাল",
                    buyPrice = 130.0,
                    sellPrice = 145.0,
                    stockQuantity = 18.0,
                    unit = "কেজি",
                    minStockAlert = 5.0
                ),
                Product(
                    name = "ইস্পাহানি মির্জাপুর চা পাতা (৪০০ গ্রাম)",
                    barcode = "89411005",
                    category = "চা ও পানীয়",
                    buyPrice = 210.0,
                    sellPrice = 240.0,
                    stockQuantity = 15.0,
                    unit = "প্যাকেট",
                    minStockAlert = 4.0
                ),
                Product(
                    name = "ফার্মের লাল ডিম (১ ডজন)",
                    barcode = "89411006",
                    category = "ডিম ও দুগ্ধজাত",
                    buyPrice = 135.0,
                    sellPrice = 150.0,
                    stockQuantity = 6.0,
                    unit = "ডজন",
                    minStockAlert = 5.0
                ),
                Product(
                    name = "লাক্স সাবান ১০০ গ্রাম",
                    barcode = "89411007",
                    category = "প্রসাধন",
                    buyPrice = 62.0,
                    sellPrice = 75.0,
                    stockQuantity = 2.0, // Low stock test
                    unit = "পিস",
                    minStockAlert = 5.0
                ),
                Product(
                    name = "হুইল ডিটারজেন্ট পাউডার (১ কেজি)",
                    barcode = "89411008",
                    category = "পরিষ্কারক",
                    buyPrice = 120.0,
                    sellPrice = 140.0,
                    stockQuantity = 1.0, // Low stock test
                    unit = "প্যাকেট",
                    minStockAlert = 3.0
                )
            )
            database.productDao().insertAll(sampleProducts)

            // 2. Initial Customers (Due ledger / বাকি খাতা)
            val sampleCustomers = listOf(
                Customer(
                    name = "আব্দুর রহিম",
                    phone = "01819234567",
                    address = "রোড #৩, বাসা #১২",
                    totalDue = 1250.0,
                    totalPurchased = 8400.0,
                    lastTransactionDate = now - (dayMillis * 1)
                ),
                Customer(
                    name = "কামাল হোসেন",
                    phone = "01711889900",
                    address = "উত্তর পাড়া",
                    totalDue = 650.0,
                    totalPurchased = 3200.0,
                    lastTransactionDate = now - (dayMillis * 2)
                ),
                Customer(
                    name = "মোঃ শফিকুল ইসলাম",
                    phone = "01912334455",
                    address = "স্কুল রোড",
                    totalDue = 3500.0,
                    totalPurchased = 15400.0,
                    lastTransactionDate = now - (dayMillis * 3)
                )
            )
            database.customerDao().insertAll(sampleCustomers)

            // 3. Initial Transactions (Recent sales & stock-in)
            val sampleTransactions = listOf(
                TransactionRecord(
                    type = "SALE",
                    invoiceNumber = "INV-1001",
                    productId = 1,
                    productName = "মিনিকেট চাল (২৫ কেজি)",
                    quantity = 1.0,
                    unit = "বস্তা",
                    unitPrice = 1800.0,
                    costPrice = 1650.0,
                    totalAmount = 1800.0,
                    profitAmount = 150.0,
                    customerName = "আব্দুর রহিম",
                    customerPhone = "01819234567",
                    paidAmount = 1000.0,
                    dueAmount = 800.0,
                    paymentMethod = "CASH",
                    timestamp = now - (dayMillis * 1)
                ),
                TransactionRecord(
                    type = "SALE",
                    invoiceNumber = "INV-1002",
                    productId = 2,
                    productName = "তীর সয়াবিন তেল (৫ লিটার)",
                    quantity = 2.0,
                    unit = "বোতল",
                    unitPrice = 920.0,
                    costPrice = 860.0,
                    totalAmount = 1840.0,
                    profitAmount = 120.0,
                    customerName = "ক্যাশ কাস্টমার",
                    customerPhone = "",
                    paidAmount = 1840.0,
                    dueAmount = 0.0,
                    paymentMethod = "BKASH",
                    timestamp = now - (1000 * 60 * 60 * 2) // 2 hours ago
                ),
                TransactionRecord(
                    type = "SALE",
                    invoiceNumber = "INV-1003",
                    productId = 3,
                    productName = "ফ্রেশ সাদা চিনি (১ কেজি)",
                    quantity = 3.0,
                    unit = "প্যাকেট",
                    unitPrice = 135.0,
                    costPrice = 125.0,
                    totalAmount = 405.0,
                    profitAmount = 30.0,
                    customerName = "ক্যাশ কাস্টমার",
                    customerPhone = "",
                    paidAmount = 405.0,
                    dueAmount = 0.0,
                    paymentMethod = "CASH",
                    timestamp = now - (1000 * 60 * 30) // 30 mins ago
                ),
                TransactionRecord(
                    type = "STOCK_IN",
                    invoiceNumber = "STK-501",
                    productId = 6,
                    productName = "ফার্মের লাল ডিম (১ ডজন)",
                    quantity = 10.0,
                    unit = "ডজন",
                    unitPrice = 135.0,
                    costPrice = 135.0,
                    totalAmount = 1350.0,
                    profitAmount = 0.0,
                    paymentMethod = "CASH",
                    note = "নতুন চালান গ্রহণ",
                    timestamp = now - (1000 * 60 * 60 * 5)
                )
            )
            database.transactionDao().insertAllTransactions(sampleTransactions)

            // 4. Sample Expenses
            val sampleExpenses = listOf(
                Expense(
                    title = "দোকানের বিদ্যুৎ বিল",
                    category = "বিদ্যুৎ বিল",
                    amount = 850.0,
                    timestamp = now - (dayMillis * 1),
                    note = "চলতি মাসের বিল"
                ),
                Expense(
                    title = "দোকানের চা-নাস্তা খরচ",
                    category = "চা-নাস্তা",
                    amount = 60.0,
                    timestamp = now - (1000 * 60 * 60 * 4),
                    note = "কাস্টমার আপ্যায়ন"
                )
            )
            database.expenseDao().insertAll(sampleExpenses)

            // 5. Initial Due Logs
            val sampleDueLogs = listOf(
                DueLog(
                    customerId = 1,
                    customerName = "আব্দুর রহিম",
                    customerPhone = "01819234567",
                    type = "DUE_GIVEN",
                    amount = 800.0,
                    note = "চাল ক্রয় বাবদ বাকি",
                    timestamp = now - (dayMillis * 1)
                ),
                DueLog(
                    customerId = 1,
                    customerName = "আব্দুর রহিম",
                    customerPhone = "01819234567",
                    type = "DUE_COLLECTED",
                    amount = 500.0,
                    note = "ক্যাশ জমা",
                    timestamp = now - (1000 * 60 * 60 * 6)
                )
            )
            database.dueLogDao().insertAll(sampleDueLogs)
        }
    }
}

package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockQuantity <= minStockAlert ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    fun observeProductByBarcode(barcode: String): Flow<Product?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Double)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :qty WHERE id = :productId")
    suspend fun increaseStock(productId: Long, qty: Double)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :qty WHERE id = :productId")
    suspend fun decreaseStock(productId: Long, qty: Double)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 25): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE customerName = :name OR (customerPhone != '' AND customerPhone = :phone) ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(name: String, phone: String): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionRecord?

    @Query("SELECT * FROM transactions WHERE invoiceNumber = :invoiceNo")
    suspend fun getTransactionsByInvoice(invoiceNo: String): List<TransactionRecord>

    @Query("DELETE FROM transactions WHERE invoiceNumber = :invoiceNo")
    suspend fun deleteTransactionsByInvoice(invoiceNo: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(list: List<TransactionRecord>)

    @Update
    suspend fun updateTransaction(tx: TransactionRecord)

    @Delete
    suspend fun deleteTransaction(tx: TransactionRecord)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY totalDue DESC, name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?

    @Query("SELECT * FROM customers WHERE (phone != '' AND phone = :phone) OR (LOWER(TRIM(name)) = LOWER(TRIM(:name))) LIMIT 1")
    suspend fun findExistingCustomer(name: String, phone: String): Customer?

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllCustomersList(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("UPDATE customers SET totalDue = totalDue + :dueDiff, totalPurchased = totalPurchased + :purchaseAdd, lastTransactionDate = :time WHERE id = :customerId")
    suspend fun updateCustomerBalance(customerId: Long, dueDiff: Double, purchaseAdd: Double, time: Long)

    @Query("UPDATE customers SET totalDue = :newDue, lastTransactionDate = :time WHERE id = :customerId")
    suspend fun setCustomerDue(customerId: Long, newDue: Double, time: Long)

    @Query("UPDATE due_logs SET customerId = :newCustomerId, customerName = :newName WHERE customerId = :oldCustomerId")
    suspend fun reassignDueLogs(oldCustomerId: Long, newCustomerId: Long, newName: String)

    @Query("UPDATE transactions SET customerName = :newName WHERE customerName = :oldName")
    suspend fun reassignTransactionsByName(oldName: String, newName: String)

    @Query("DELETE FROM customers")
    suspend fun clearAll()
}

@Dao
interface DueLogDao {
    @Query("SELECT * FROM due_logs ORDER BY timestamp DESC")
    fun getAllDueLogs(): Flow<List<DueLog>>

    @Query("SELECT * FROM due_logs WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getDueLogsForCustomer(customerId: Long): Flow<List<DueLog>>

    @Query("SELECT * FROM due_logs WHERE id = :id LIMIT 1")
    suspend fun getDueLogById(id: Long): DueLog?

    @Query("SELECT * FROM due_logs")
    suspend fun getAllDueLogsList(): List<DueLog>

    @Query("SELECT * FROM due_logs WHERE customerId = :customerId")
    suspend fun getDueLogsListForCustomer(customerId: Long): List<DueLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDueLog(dueLog: DueLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<DueLog>)

    @Update
    suspend fun updateDueLog(dueLog: DueLog)

    @Delete
    suspend fun deleteDueLog(dueLog: DueLog)

    @Query("DELETE FROM due_logs WHERE customerId = :customerId")
    suspend fun deleteDueLogsForCustomer(customerId: Long)

    @Query("DELETE FROM due_logs WHERE note LIKE :pattern")
    suspend fun deleteDueLogsByNotePattern(pattern: String)

    @Query("DELETE FROM due_logs")
    suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}

@Dao
interface CashLogDao {
    @Query("SELECT * FROM cash_logs ORDER BY timestamp DESC")
    fun getAllCashLogs(): Flow<List<CashLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashLog(cashLog: CashLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<CashLog>)

    @Update
    suspend fun updateCashLog(cashLog: CashLog)

    @Delete
    suspend fun deleteCashLog(cashLog: CashLog)

    @Query("DELETE FROM cash_logs WHERE id = :id")
    suspend fun deleteCashLogById(id: Long)

    @Query("DELETE FROM cash_logs")
    suspend fun clearAll()
}


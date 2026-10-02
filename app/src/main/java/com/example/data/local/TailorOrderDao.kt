package com.example.data.local

import androidx.room.*
import com.example.data.model.TailorOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface TailorOrderDao {
    @Query("SELECT * FROM tailor_orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<TailorOrder>>

    @Query("SELECT * FROM tailor_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): TailorOrder?

    @Query("SELECT * FROM tailor_orders WHERE customerName LIKE '%' || :query || '%' OR customerPhone LIKE '%' || :query || '%' OR orderNumber LIKE '%' || :query || '%' ORDER BY orderDate DESC")
    fun searchOrders(query: String): Flow<List<TailorOrder>>

    @Query("SELECT * FROM tailor_orders WHERE status = :status ORDER BY deliveryDate ASC")
    fun getOrdersByStatus(status: String): Flow<List<TailorOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: TailorOrder): Long

    @Update
    suspend fun updateOrder(order: TailorOrder)

    @Delete
    suspend fun deleteOrder(order: TailorOrder)

    @Query("UPDATE tailor_orders SET status = :newStatus WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, newStatus: String)

    @Query("UPDATE tailor_orders SET advancePaid = advancePaid + :amount, dueAmount = (totalAmount - (advancePaid + :amount)) WHERE id = :orderId")
    suspend fun collectPayment(orderId: Long, amount: Double)

    @Query("SELECT COUNT(*) FROM tailor_orders")
    suspend fun getCount(): Int

    @Query("SELECT * FROM tailor_orders ORDER BY orderDate DESC")
    suspend fun getAllOrdersDirect(): List<TailorOrder>
}

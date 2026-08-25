package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CashLog
import com.example.data.model.Customer
import com.example.data.model.DueLog
import com.example.data.model.Product
import com.example.data.model.TransactionRecord
import com.example.ui.components.toIntOrNull
import com.example.ui.viewmodel.InvoiceDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    /**
     * Generates a clean, professional PDF Invoice / Cash Memo
     */
    fun generateInvoicePdf(
        context: Context,
        invoice: InvoiceDetails,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard 72 dpi
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            color = Color.rgb(30, 41, 59)
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(16, 185, 129) // Emerald header
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        // Header Background bar
        canvas.drawRect(0f, 0f, 595f, 15f, headerBgPaint)

        // Store Header
        var y = 55f
        canvas.drawText(invoice.shopName, 297.5f, y, titlePaint)
        y += 18f
        if (invoice.shopAddress.isNotBlank()) {
            canvas.drawText(invoice.shopAddress, 297.5f, y, subPaint)
            y += 16f
        }
        canvas.drawText("Phone: ${invoice.shopPhone}", 297.5f, y, subPaint)
        y += 24f

        // Document Title Badge
        val memoBadgePaint = Paint().apply {
            color = Color.rgb(236, 253, 245)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(210f, y - 14f, 385f, y + 10f), 6f, 6f, memoBadgePaint)
        val memoTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(4, 120, 87)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CASH MEMO / ডিজিটাল ক্যাশ মেমো", 297.5f, y + 2f, memoTextPaint)
        y += 30f

        // Divider
        canvas.drawLine(35f, y, 560f, y, linePaint)
        y += 18f

        // Customer & Invoice Details
        val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", 35f, y, boldPaint)
        canvas.drawText("Customer: ${invoice.customerName}", 340f, y, boldPaint)
        y += 16f
        canvas.drawText("Date: $dateStr", 35f, y, textPaint)
        if (invoice.customerPhone.isNotBlank()) {
            canvas.drawText("Phone: ${invoice.customerPhone}", 340f, y, textPaint)
        }
        y += 24f

        // Table Header
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(35f, y - 14f, 560f, y + 12f), 4f, 4f, tableHeaderPaint)

        canvas.drawText("SL", 45f, y, boldPaint)
        canvas.drawText("Item / Description", 80f, y, boldPaint)
        canvas.drawText("Qty", 320f, y, boldPaint)
        canvas.drawText("Rate ($currency)", 400f, y, boldPaint)
        canvas.drawText("Total ($currency)", 480f, y, boldPaint)
        y += 22f

        // Items List
        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        invoice.items.forEachIndexed { index, item ->
            if (index % 2 == 1) {
                canvas.drawRect(RectF(35f, y - 12f, 560f, y + 8f), rowBgAlt)
            }
            canvas.drawText("${index + 1}", 45f, y, textPaint)
            val prodTitle = if (item.product.name.length > 30) item.product.name.take(28) + ".." else item.product.name
            canvas.drawText(prodTitle, 80f, y, textPaint)
            canvas.drawText("${item.quantity.toIntOrNull() ?: item.quantity} ${item.product.unit}", 320f, y, textPaint)
            canvas.drawText("${item.customPrice.toIntOrNull() ?: item.customPrice}", 400f, y, textPaint)
            canvas.drawText("${item.total.toIntOrNull() ?: item.total}", 480f, y, boldPaint)

            y += 20f
        }

        y += 10f
        canvas.drawLine(35f, y, 560f, y, linePaint)
        y += 18f

        // Financial Summary on Right Side
        val summaryX = 350f
        canvas.drawText("Sub Total:", summaryX, y, textPaint)
        canvas.drawText("$currency${invoice.subTotal.toIntOrNull() ?: invoice.subTotal}", 480f, y, textPaint)
        y += 16f

        if (invoice.discount > 0) {
            canvas.drawText("Discount:", summaryX, y, textPaint)
            canvas.drawText("- $currency${invoice.discount.toIntOrNull() ?: invoice.discount}", 480f, y, textPaint)
            y += 16f
        }

        canvas.drawText("Grand Total:", summaryX, y, boldPaint)
        canvas.drawText("$currency${invoice.grandTotal.toIntOrNull() ?: invoice.grandTotal}", 480f, y, boldPaint)
        y += 16f

        canvas.drawText("Paid Amount:", summaryX, y, textPaint)
        canvas.drawText("$currency${invoice.paidAmount.toIntOrNull() ?: invoice.paidAmount}", 480f, y, textPaint)
        y += 16f

        if (invoice.dueAmount > 0) {
            val duePaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(234, 88, 12)
            }
            canvas.drawText("Due Balance:", summaryX, y, duePaint)
            canvas.drawText("$currency${invoice.dueAmount.toIntOrNull() ?: invoice.dueAmount}", 480f, y, duePaint)
            y += 16f
        }

        canvas.drawText("Payment Mode: ${invoice.paymentMethod}", summaryX, y, textPaint)
        y += 40f

        // Thank you footer & signature
        canvas.drawLine(35f, y + 40f, 150f, y + 40f, linePaint)
        canvas.drawText("Customer Signature", 35f, y + 54f, subPaint)

        canvas.drawLine(440f, y + 40f, 560f, y + 40f, linePaint)
        canvas.drawText("Authorized Signature", 440f, y + 54f, subPaint)

        val footerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            color = Color.rgb(5, 150, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for your business! ধন্যবাদ, আবার আসবেন।", 297.5f, 790f, footerTextPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache
        return savePdfToFile(context, pdfDocument, "Invoice_${invoice.invoiceNumber}.pdf")
    }

    /**
     * Generates a Periodic Business & Profit Loss Report PDF
     */
    fun generateReportPdf(
        context: Context,
        shopName: String,
        periodTitle: String,
        totalSales: Double,
        salesCost: Double,
        grossProfit: Double,
        expenses: Double,
        netProfit: Double,
        purchases: Double,
        dueAmount: Double,
        transactions: List<TransactionRecord>,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        // Top banner
        val topBarPaint = Paint().apply { color = Color.rgb(16, 185, 129); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, 595f, 12f, topBarPaint)

        var y = 45f
        canvas.drawText(shopName, 297.5f, y, titlePaint)
        y += 16f
        canvas.drawText("Financial & Profit-Loss Summary - $periodTitle", 297.5f, y, subPaint)
        y += 14f
        val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $genDate", 297.5f, y, subPaint)
        y += 20f

        // Financial Overview Box
        val boxPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(35f, y, 560f, y + 115f), 8f, 8f, boxPaint)
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(35f, y, 560f, y + 115f), 8f, 8f, borderPaint)

        y += 22f
        canvas.drawText("1. Total Sales (মোট বিক্রি):", 50f, y, textPaint)
        canvas.drawText("$currency${totalSales.toIntOrNull() ?: totalSales}", 240f, y, boldPaint)

        canvas.drawText("2. Cost of Goods (ক্রয়মূল্য):", 320f, y, textPaint)
        canvas.drawText("- $currency${salesCost.toIntOrNull() ?: salesCost}", 480f, y, textPaint)
        y += 20f

        canvas.drawText("3. Gross Profit (বিক্রয় লাভ):", 50f, y, boldPaint)
        canvas.drawText("$currency${grossProfit.toIntOrNull() ?: grossProfit}", 240f, y, boldPaint)

        canvas.drawText("4. Expenses (দোকানের খরচ):", 320f, y, textPaint)
        canvas.drawText("- $currency${expenses.toIntOrNull() ?: expenses}", 480f, y, textPaint)
        y += 24f

        val netProfitPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (netProfit >= 0) Color.rgb(4, 120, 87) else Color.rgb(220, 38, 38)
        }
        canvas.drawText("NET PROFIT (নিট লাভ/মুনাফা):", 50f, y, netProfitPaint)
        canvas.drawText("$currency${netProfit.toIntOrNull() ?: netProfit}", 240f, y, netProfitPaint)

        canvas.drawText("Customer Due (মোট বাকি):", 320f, y, textPaint)
        canvas.drawText("$currency${dueAmount.toIntOrNull() ?: dueAmount}", 480f, y, boldPaint)

        y += 45f

        // Recent Transactions Table Header
        canvas.drawText("Transaction Log in Period (সাম্প্রতিক লেনদেন):", 35f, y, boldPaint)
        y += 14f

        val headerPaint = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(35f, y - 10f, 560f, y + 10f), 4f, 4f, headerPaint)
        canvas.drawText("Type", 45f, y, boldPaint)
        canvas.drawText("Item / Description", 120f, y, boldPaint)
        canvas.drawText("Qty", 320f, y, boldPaint)
        canvas.drawText("Amount", 400f, y, boldPaint)
        canvas.drawText("Time", 480f, y, boldPaint)
        y += 18f

        val rowBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        transactions.take(22).forEachIndexed { i, tx ->
            if (i % 2 == 1) {
                canvas.drawRect(RectF(35f, y - 10f, 560f, y + 6f), rowBg)
            }
            val typeStr = when(tx.type) {
                "SALE" -> "SALE"
                "STOCK_IN" -> "STOCK IN"
                else -> "ADJUST"
            }
            canvas.drawText(typeStr, 45f, y, textPaint)
            val title = if (tx.productName.length > 25) tx.productName.take(23) + ".." else tx.productName
            canvas.drawText(title, 120f, y, textPaint)
            canvas.drawText("${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}", 320f, y, textPaint)
            canvas.drawText("$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}", 400f, y, boldPaint)
            val timeShort = SimpleDateFormat("dd/MM hh:mm", Locale.getDefault()).format(Date(tx.timestamp))
            canvas.drawText(timeShort, 480f, y, textPaint)

            y += 16f
        }

        // Footer
        canvas.drawText("Generated by ShopKhata App", 297.5f, 810f, subPaint)

        pdfDocument.finishPage(page)
        return savePdfToFile(context, pdfDocument, "Report_${periodTitle.replace(" ", "_")}.pdf")
    }

    /**
     * Generates a Customer Due Khata Statement PDF
     */
    fun generateCustomerDuePdf(
        context: Context,
        shopName: String,
        customer: Customer,
        history: List<DueLog>,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var y = 45f
        canvas.drawText(shopName, 297.5f, y, titlePaint)
        y += 16f
        canvas.drawText("Customer Due Statement / কাস্টমার বাকি খাতা", 297.5f, y, subPaint)
        y += 24f

        // Customer Profile Card in PDF
        val custBoxPaint = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(35f, y, 560f, y + 65f), 6f, 6f, custBoxPaint)

        y += 20f
        canvas.drawText("Customer Name: ${customer.name}", 50f, y, boldPaint)
        val dueColorPaint = Paint().apply {
            isAntiAlias = true
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(234, 88, 12)
        }
        canvas.drawText("Current Total Due: $currency${customer.totalDue.toIntOrNull() ?: customer.totalDue}", 340f, y, dueColorPaint)
        y += 20f
        canvas.drawText("Mobile Phone: ${customer.phone.ifBlank { "N/A" }}", 50f, y, textPaint)
        canvas.drawText("Address: ${customer.address.ifBlank { "N/A" }}", 340f, y, textPaint)
        y += 40f

        canvas.drawText("Ledger Activity / লেনদেন বিবরণী:", 35f, y, boldPaint)
        y += 14f

        val headerPaint = Paint().apply { color = Color.rgb(241, 245, 249); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(35f, y - 10f, 560f, y + 10f), 4f, 4f, headerPaint)
        canvas.drawText("Date & Time", 45f, y, boldPaint)
        canvas.drawText("Particulars / Note", 180f, y, boldPaint)
        canvas.drawText("Type", 380f, y, boldPaint)
        canvas.drawText("Amount ($currency)", 470f, y, boldPaint)
        y += 18f

        val greenPaint = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(5, 150, 105); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val redPaint = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(220, 38, 38); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        history.forEach { log ->
            val isCollected = log.type == "DUE_COLLECTED"
            val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(log.timestamp))

            canvas.drawText(dateStr, 45f, y, textPaint)
            val note = if (log.note.length > 28) log.note.take(26) + ".." else log.note
            canvas.drawText(note, 180f, y, textPaint)
            canvas.drawText(if (isCollected) "PAID/জমা" else "DUE/বাকি", 380f, y, if (isCollected) greenPaint else redPaint)
            canvas.drawText("${if (isCollected) "-" else "+"}${log.amount.toIntOrNull() ?: log.amount}", 470f, y, if (isCollected) greenPaint else redPaint)

            y += 18f
        }

        y += 20f
        canvas.drawLine(35f, y, 560f, y, linePaint)
        y += 30f

        canvas.drawText("For any queries, contact $shopName", 297.5f, 790f, subPaint)

        pdfDocument.finishPage(page)
        return savePdfToFile(context, pdfDocument, "Due_${customer.name.replace(" ", "_")}.pdf")
    }

    /**
     * Generates Complete Products / Stock Inventory PDF Report
     */
    fun generateAllProductsPdf(
        context: Context,
        shopName: String,
        products: List<Product>,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(16, 185, 129); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 595f, 12f, topBarPaint)

            var y = 42f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 16f
            canvas.drawText("দোকানের সকল পণ্য ও স্টক ইনভেন্টরি রিপোর্ট (Products & Stock List)", 297.5f, y, subPaint)
            y += 14f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 20f
                val totalStockVal = products.sumOf { it.stockQuantity * it.sellPrice }
                val totalCostVal = products.sumOf { it.stockQuantity * it.buyPrice }
                val boxPaint = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 42f), 6f, 6f, boxPaint)
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 42f), 6f, 6f, linePaint)

                y += 18f
                canvas.drawText("মোট পণ্য সংখ্যা: ${products.size} টি", 50f, y, boldPaint)
                canvas.drawText("মোট ক্রয়মূল্য সম্পদ: $currency${totalCostVal.toIntOrNull() ?: totalCostVal}", 220f, y, boldPaint)
                canvas.drawText("মোট বিক্রয় মূল্য মান: $currency${totalStockVal.toIntOrNull() ?: totalStockVal}", 400f, y, boldPaint)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            canvas.drawRoundRect(RectF(35f, startY - 12f, 560f, startY + 10f), 4f, 4f, headerPaint)
            canvas.drawText("নং", 42f, startY, boldPaint)
            canvas.drawText("পণ্যের নাম (Item Name)", 70f, startY, boldPaint)
            canvas.drawText("ক্যাটাগরি", 240f, startY, boldPaint)
            canvas.drawText("ক্রয়দর", 330f, startY, boldPaint)
            canvas.drawText("বিক্রয়দর", 390f, startY, boldPaint)
            canvas.drawText("স্টক পরিমাণ", 450f, startY, boldPaint)
            canvas.drawText("মোট মূল্য ($currency)", 510f, startY, boldPaint)
            return startY + 18f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 136f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }

        products.forEachIndexed { index, p ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 90f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(35f, y - 10f, 560f, y + 6f), rowBgAlt)
            }

            val itemTotal = p.stockQuantity * p.sellPrice
            canvas.drawText("${index + 1}", 42f, y, textPaint)
            val nameStr = if (p.name.length > 25) p.name.take(23) + ".." else p.name
            canvas.drawText(nameStr, 70f, y, textPaint)
            val catStr = if (p.category.length > 14) p.category.take(12) + ".." else p.category
            canvas.drawText(catStr, 240f, y, textPaint)
            canvas.drawText("${p.buyPrice.toIntOrNull() ?: p.buyPrice}", 330f, y, textPaint)
            canvas.drawText("${p.sellPrice.toIntOrNull() ?: p.sellPrice}", 390f, y, textPaint)
            canvas.drawText("${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}", 450f, y, textPaint)
            canvas.drawText("${itemTotal.toIntOrNull() ?: itemTotal}", 510f, y, boldPaint)

            y += 16f
        }

        canvas.drawText("Generated by NAFI SHOP 24", 297.5f, 815f, subPaint)
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "Products_Inventory_$timestamp.pdf")
    }

    /**
     * Generates All Customers Due Statement PDF
     */
    fun generateAllDuesPdf(
        context: Context,
        shopName: String,
        customers: List<Customer>,
        totalDue: Double,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val duePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(234, 88, 12)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(255, 237, 213)
            style = Paint.Style.FILL
        }

        val debtors = customers.filter { it.totalDue > 0 }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(234, 88, 12); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 595f, 12f, topBarPaint)

            var y = 42f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 16f
            canvas.drawText("সার্বিক কাস্টমার বাকি খাতা রিপোর্ট (All Customer Dues Statement)", 297.5f, y, subPaint)
            y += 14f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 20f
                val boxPaint = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 40f), 6f, 6f, boxPaint)
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 40f), 6f, 6f, linePaint)

                y += 17f
                canvas.drawText("মোট বাকিদার কাস্টমার: ${debtors.size} জন", 50f, y, boldPaint)
                val totalDueTitlePaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(194, 65, 12)
                }
                canvas.drawText("দোকানের মোট পাওনা বাকি: $currency${totalDue.toIntOrNull() ?: totalDue}", 300f, y, totalDueTitlePaint)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            canvas.drawRoundRect(RectF(35f, startY - 12f, 560f, startY + 10f), 4f, 4f, headerPaint)
            canvas.drawText("নং", 42f, startY, boldPaint)
            canvas.drawText("কাস্টমারের নাম (Customer)", 75f, startY, boldPaint)
            canvas.drawText("মোবাইল নাম্বার", 220f, startY, boldPaint)
            canvas.drawText("ঠিকানা", 340f, startY, boldPaint)
            canvas.drawText("বাকি পরিমাণ ($currency)", 465f, startY, boldPaint)
            return startY + 18f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 136f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL }

        debtors.forEachIndexed { index, c ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 90f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(35f, y - 10f, 560f, y + 6f), rowBgAlt)
            }

            canvas.drawText("${index + 1}", 42f, y, textPaint)
            val nameStr = if (c.name.length > 22) c.name.take(20) + ".." else c.name
            canvas.drawText(nameStr, 75f, y, boldPaint)
            canvas.drawText(c.phone.ifBlank { "N/A" }, 220f, y, textPaint)
            val addrStr = if (c.address.length > 20) c.address.take(18) + ".." else c.address.ifBlank { "N/A" }
            canvas.drawText(addrStr, 340f, y, textPaint)
            canvas.drawText("$currency${c.totalDue.toIntOrNull() ?: c.totalDue}", 465f, y, duePaint)

            y += 16f
        }

        canvas.drawText("Generated by NAFI SHOP 24 Due Ledger", 297.5f, 815f, subPaint)
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "All_Dues_Report_$timestamp.pdf")
    }

    /**
     * Generates Full Transactions / Sales Log PDF
     */
    fun generateTransactionsListPdf(
        context: Context,
        shopName: String,
        title: String,
        transactions: List<TransactionRecord>,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(37, 99, 235); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 595f, 12f, topBarPaint)

            var y = 42f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 16f
            canvas.drawText(title, 297.5f, y, subPaint)
            y += 14f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 20f
                val totalAmount = transactions.sumOf { it.totalAmount }
                val totalProfit = transactions.filter { it.type == "SALE" }.sumOf { it.profitAmount }
                val boxPaint = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 40f), 6f, 6f, boxPaint)
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 40f), 6f, 6f, linePaint)

                y += 17f
                canvas.drawText("মোট লেনদেন সংখ্যা: ${transactions.size} টি", 50f, y, boldPaint)
                canvas.drawText("মোট লেনদেন মূল্য: $currency${totalAmount.toIntOrNull() ?: totalAmount}", 240f, y, boldPaint)
                canvas.drawText("মোট বিক্রয় লাভ: $currency${totalProfit.toIntOrNull() ?: totalProfit}", 420f, y, boldPaint)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            canvas.drawRoundRect(RectF(35f, startY - 12f, 560f, startY + 10f), 4f, 4f, headerPaint)
            canvas.drawText("ধরন", 42f, startY, boldPaint)
            canvas.drawText("বিবরণ / পণ্য", 95f, startY, boldPaint)
            canvas.drawText("পরিমাণ", 270f, startY, boldPaint)
            canvas.drawText("মোট টাকা ($currency)", 340f, startY, boldPaint)
            canvas.drawText("পেমেন্ট", 430f, startY, boldPaint)
            canvas.drawText("সময়", 490f, startY, boldPaint)
            return startY + 18f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 136f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }

        transactions.forEachIndexed { index, tx ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 90f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(35f, y - 10f, 560f, y + 6f), rowBgAlt)
            }

            val typeStr = when (tx.type) {
                "SALE" -> "বিক্রি"
                "STOCK_IN" -> "স্টক ইন"
                "PURCHASE" -> "ক্রয়"
                else -> tx.type
            }
            canvas.drawText(typeStr, 42f, y, boldPaint)
            val descStr = if (tx.productName.length > 25) tx.productName.take(23) + ".." else tx.productName
            canvas.drawText(descStr, 95f, y, textPaint)
            canvas.drawText("${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}", 270f, y, textPaint)
            canvas.drawText("${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}", 340f, y, boldPaint)
            canvas.drawText(tx.paymentMethod, 430f, y, textPaint)
            val timeStr = SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
            canvas.drawText(timeStr, 490f, y, textPaint)

            y += 16f
        }

        canvas.drawText("Generated by NAFI SHOP 24", 297.5f, 815f, subPaint)
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "Transactions_$timestamp.pdf")
    }

    /**
     * Generates Expiring & Expired Products List PDF for supplier return/replacement
     */
    fun generateExpiringProductsPdf(
        context: Context,
        shopName: String,
        products: List<Product>,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(254, 243, 199) // amber header
            style = Paint.Style.FILL
        }

        val now = System.currentTimeMillis()
        val expiredCount = products.count { it.expiryDate in 1 until now }
        val expiringSoonCount = products.size - expiredCount
        val totalCost = products.sumOf { it.stockQuantity * it.buyPrice }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(234, 88, 12); style = Paint.Style.FILL } // warning orange
            canvas.drawRect(0f, 0f, 595f, 12f, topBarPaint)

            var y = 42f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 16f
            canvas.drawText("মেয়াদ শেষ পর্যায় ও মেয়াদোত্তীর্ণ পণ্যের তালিকা (Expiring Products Report)", 297.5f, y, subPaint)
            y += 14f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 20f
                val boxPaint = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 42f), 6f, 6f, boxPaint)
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 42f), 6f, 6f, linePaint)

                y += 18f
                canvas.drawText("মোট পণ্য: ${products.size} টি", 50f, y, boldPaint)
                canvas.drawText("মেয়াদোত্তীর্ণ: $expiredCount টি | শেষ পর্যায়: $expiringSoonCount টি", 180f, y, boldPaint)
                canvas.drawText("মোট ক্রয়মূল্য: $currency${totalCost.toIntOrNull() ?: totalCost}", 400f, y, boldPaint)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            canvas.drawRoundRect(RectF(35f, startY - 12f, 560f, startY + 10f), 4f, 4f, headerPaint)
            canvas.drawText("নং", 42f, startY, boldPaint)
            canvas.drawText("পণ্যের নাম (Item Name)", 70f, startY, boldPaint)
            canvas.drawText("স্টক পরিমাণ", 250f, startY, boldPaint)
            canvas.drawText("মেয়াদ শেষ তারিখ", 330f, startY, boldPaint)
            canvas.drawText("অবস্থা / দিন বাকি", 430f, startY, boldPaint)
            canvas.drawText("ক্রয়মূল্য ($currency)", 505f, startY, boldPaint)
            return startY + 18f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 136f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
        val redTextPaint = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(220, 38, 38); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val orangeTextPaint = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(217, 119, 6); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        products.forEachIndexed { index, p ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 90f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(35f, y - 10f, 560f, y + 6f), rowBgAlt)
            }

            canvas.drawText("${index + 1}", 42f, y, textPaint)
            val nameStr = if (p.name.length > 25) p.name.take(23) + ".." else p.name
            canvas.drawText(nameStr, 70f, y, textPaint)
            canvas.drawText("${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}", 250f, y, textPaint)

            val expDateStr = if (p.expiryDate > 0) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(p.expiryDate))
            } else "N/A"
            canvas.drawText(expDateStr, 330f, y, textPaint)

            val daysRemaining = if (p.expiryDate > 0) {
                ((p.expiryDate - now) / (1000 * 60 * 60 * 24)).toInt()
            } else 0

            val (statusStr, statusPaint) = when {
                daysRemaining < 0 -> Pair("মেয়াদ শেষ! (${-daysRemaining}d)", redTextPaint)
                daysRemaining == 0 -> Pair("আজ মেয়াদ শেষ", redTextPaint)
                else -> Pair("$daysRemaining দিন বাকি", orangeTextPaint)
            }
            canvas.drawText(statusStr, 430f, y, statusPaint)

            val costTotal = p.stockQuantity * p.buyPrice
            canvas.drawText("${costTotal.toIntOrNull() ?: costTotal}", 505f, y, boldPaint)

            y += 16f
        }

        canvas.drawText("Generated by NAFI SHOP 24 - মহাজন বা সরবরাহকারীকে ফেরত/বদল বাবদ রিপোর্ট", 297.5f, 815f, subPaint)
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "Expiring_Products_$timestamp.pdf")
    }

    private fun savePdfToFile(context: Context, pdfDocument: PdfDocument, filename: String): File? {
        return try {
            val pdfDir = File(context.cacheDir, "documents")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            val file = File(pdfDir, filename)
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Opens or shares the generated PDF with external apps / PDF viewers
     */
    fun openOrSharePdf(context: Context, file: File, chooserTitle: String = "Download / Share PDF") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "PDF Open Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

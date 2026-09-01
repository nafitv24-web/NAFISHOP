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
import com.example.data.model.Expense
import com.example.data.model.MasterCashEntry
import com.example.data.model.Product
import com.example.data.model.TransactionRecord
import com.example.ui.components.toIntOrNull
import com.example.ui.viewmodel.InvoiceDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    const val SPONSOR_TEXT = "সৌজন্যে নাফি এন্ড নাজমুল টেলিকম"
    const val SPONSOR_URL = "https://nfiptvserver.blogspot.com/"

    /**
     * Draws standard sponsor branding & clickable URL footer on PDF pages
     */
    fun drawSponsorFooter(canvas: Canvas, yStart: Float = 800f, extraNote: String? = null) {
        val sponsorPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.CENTER
        }
        val linkPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.rgb(37, 99, 235) // Hyperlink blue
            textAlign = Paint.Align.CENTER
            isUnderlineText = true
        }
        val notePaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }

        var currentY = yStart
        if (!extraNote.isNullOrBlank()) {
            canvas.drawText(extraNote, 297.5f, currentY, notePaint)
            currentY += 13f
        }
        canvas.drawText(SPONSOR_TEXT, 297.5f, currentY, sponsorPaint)
        canvas.drawText(SPONSOR_URL, 297.5f, currentY + 12f, linkPaint)
    }

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

        val rightBoldHeader = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("নং", 45f, y, boldPaint)
        canvas.drawText("পণ্যের বিবরণ (Item)", 75f, y, boldPaint)
        canvas.drawText("পরিমাণ (Qty)", 300f, y, boldPaint)
        canvas.drawText("দর (Rate)", 400f, y, boldPaint)
        canvas.drawText("মোট (Total)", 555f, y, rightBoldHeader)
        y += 22f

        // Items List
        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        invoice.items.forEachIndexed { index, item ->
            if (index % 2 == 1) {
                canvas.drawRect(RectF(35f, y - 12f, 560f, y + 8f), rowBgAlt)
            }
            canvas.drawText("${index + 1}", 45f, y, textPaint)
            val prodTitle = if (item.product.name.length > 32) item.product.name.take(30) + ".." else item.product.name
            canvas.drawText(prodTitle, 75f, y, textPaint)
            canvas.drawText("${item.quantity.toIntOrNull() ?: item.quantity} ${item.product.unit}", 300f, y, textPaint)
            canvas.drawText("$currency${item.customPrice.toIntOrNull() ?: item.customPrice}", 400f, y, textPaint)
            canvas.drawText("$currency${item.total.toIntOrNull() ?: item.total}", 555f, y, rightBoldPaint)

            y += 20f
        }

        y += 10f
        canvas.drawLine(35f, y, 560f, y, linePaint)
        y += 18f

        // Financial Summary on Right Side (Subtotal, Discount, Grand Total, Paid, Today Due, Prev Due, Total Due)
        val summaryX = 290f
        val valX = 555f

        val greenRightPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(4, 120, 87)
            textAlign = Paint.Align.RIGHT
        }
        val dueOrangeRightPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(234, 88, 12)
            textAlign = Paint.Align.RIGHT
        }
        val totalDueRedRightPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(220, 38, 38)
            textAlign = Paint.Align.RIGHT
        }

        // Sub Total
        canvas.drawText("মোট মূল্য (Subtotal):", summaryX, y, textPaint)
        canvas.drawText("$currency${invoice.subTotal.toIntOrNull() ?: invoice.subTotal}", valX, y, rightTextPaint)
        y += 16f

        if (invoice.discount > 0) {
            canvas.drawText("ডিসকাউন্ট / ছাড় (Discount):", summaryX, y, textPaint)
            canvas.drawText("- $currency${invoice.discount.toIntOrNull() ?: invoice.discount}", valX, y, greenRightPaint)
            y += 16f
        }

        canvas.drawText("সর্বমোট প্রদেয় (Grand Total):", summaryX, y, boldPaint)
        canvas.drawText("$currency${invoice.grandTotal.toIntOrNull() ?: invoice.grandTotal}", valX, y, rightBoldPaint)
        y += 16f

        canvas.drawText("জমা / পরিশোধিত (Paid):", summaryX, y, textPaint)
        canvas.drawText("$currency${invoice.paidAmount.toIntOrNull() ?: invoice.paidAmount}", valX, y, greenRightPaint)
        y += 16f

        if (invoice.dueAmount > 0 || invoice.totalCurrentDue > 0) {
            val dueOrangeLabel = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(234, 88, 12)
            }
            canvas.drawText("আজকের নতুন বাকি (Today's Due):", summaryX, y, dueOrangeLabel)
            canvas.drawText("$currency${invoice.dueAmount.toIntOrNull() ?: invoice.dueAmount}", valX, y, dueOrangeRightPaint)
            y += 16f

            if (invoice.previousDue > 0) {
                val prevDueLabel = Paint().apply {
                    isAntiAlias = true
                    textSize = 10.5f
                    color = Color.rgb(100, 116, 139)
                }
                canvas.drawText("পূর্বের বাকি ছিল (Previous Due):", summaryX, y, prevDueLabel)
                canvas.drawText("$currency${invoice.previousDue.toIntOrNull() ?: invoice.previousDue}", valX, y, rightTextPaint)
                y += 16f
            }

            // Total Due Box / Highlight
            val totalDueBg = Paint().apply {
                color = Color.rgb(254, 242, 242)
                style = Paint.Style.FILL
            }
            val totalDueBorder = Paint().apply {
                color = Color.rgb(254, 202, 202)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(RectF(summaryX - 6f, y - 13f, valX + 6f, y + 6f), 4f, 4f, totalDueBg)
            canvas.drawRoundRect(RectF(summaryX - 6f, y - 13f, valX + 6f, y + 6f), 4f, 4f, totalDueBorder)

            val totalDueRedLabel = Paint().apply {
                isAntiAlias = true
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(220, 38, 38)
            }
            canvas.drawText("সর্বমোট বর্তমান বকেয়া (Total Due):", summaryX, y, totalDueRedLabel)
            canvas.drawText("$currency${invoice.totalCurrentDue.toIntOrNull() ?: invoice.totalCurrentDue}", valX, y, totalDueRedRightPaint)
            y += 20f
        }

        val pmDisplay = when (invoice.paymentMethod) {
            "DUE" -> "বাকি (Credit)"
            "BKASH" -> "bKash (বিকাশ)"
            "NAGAD" -> "Nagad (নগদ)"
            "CASH" -> "নগদ ক্যাশ (Cash)"
            else -> invoice.paymentMethod
        }
        canvas.drawText("পেমেন্ট মাধ্যম (Payment Mode):", summaryX, y, textPaint)
        canvas.drawText(pmDisplay, valX, y, rightBoldPaint)
        y += 35f

        // Thank you footer & signature
        canvas.drawLine(35f, y + 40f, 150f, y + 40f, linePaint)
        canvas.drawText("Customer Signature", 35f, y + 54f, subPaint)

        canvas.drawLine(440f, y + 40f, 560f, y + 40f, linePaint)
        canvas.drawText("Authorized Signature", 440f, y + 54f, subPaint)

        val footerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            color = Color.rgb(5, 150, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for your business! ধন্যবাদ, আবার আসবেন।", 297.5f, 785f, footerTextPaint)
        drawSponsorFooter(canvas, 802f)

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
        drawSponsorFooter(canvas, 802f, "ShopKhata Report")

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
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        val greenPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(5, 150, 105)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(220, 38, 38)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val totalGiven = history.filter { it.type == "DUE_GIVEN" }.sumOf { it.amount }
        val totalCollected = history.filter { it.type == "DUE_COLLECTED" }.sumOf { it.amount }

        fun drawHeaderAndCustomerCard(isFirstPage: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(234, 88, 12); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 595f, 10f, topBarPaint)

            var y = 38f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 15f
            canvas.drawText("কাস্টমার বাকি খাতা বিবরণী (Customer Due Ledger)", 297.5f, y, subPaint)
            y += 13f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (isFirstPage) {
                y += 18f
                // Customer Profile Box
                val custBoxPaint = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(30f, y, 565f, y + 46f), 6f, 6f, custBoxPaint)
                canvas.drawRoundRect(RectF(30f, y, 565f, y + 46f), 6f, 6f, linePaint)

                canvas.drawText("কাস্টমার: ${customer.name}", 42f, y + 18f, boldPaint)
                canvas.drawText("মোবাইল: ${customer.phone.ifBlank { "N/A" }}", 42f, y + 34f, textPaint)
                canvas.drawText("ঠিকানা: ${customer.address.ifBlank { "N/A" }}", 320f, y + 18f, textPaint)
                canvas.drawText("মোট ক্রয়: $currency${customer.totalPurchased.toIntOrNull() ?: customer.totalPurchased}", 320f, y + 34f, textPaint)

                y += 54f
                // 3 Metric Summary Boxes
                val boxWidth = 172f
                val boxHeight = 36f

                // Total Given
                val b1 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(30f, y, 30f + boxWidth, y + boxHeight), 4f, 4f, b1)
                canvas.drawRoundRect(RectF(30f, y, 30f + boxWidth, y + boxHeight), 4f, 4f, linePaint)
                canvas.drawText("মোট বাকি প্রদান", 40f, y + 14f, subPaint)
                canvas.drawText("$currency${totalGiven.toIntOrNull() ?: totalGiven}", 40f, y + 28f, redPaint)

                // Total Collected
                val b2 = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(211f, y, 211f + boxWidth, y + boxHeight), 4f, 4f, b2)
                canvas.drawRoundRect(RectF(211f, y, 211f + boxWidth, y + boxHeight), 4f, 4f, linePaint)
                canvas.drawText("মোট নগদ জমা আদায়", 221f, y + 14f, subPaint)
                canvas.drawText("$currency${totalCollected.toIntOrNull() ?: totalCollected}", 221f, y + 28f, greenPaint)

                // Net Due
                val b3 = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(393f, y, 393f + boxWidth, y + boxHeight), 4f, 4f, b3)
                canvas.drawRoundRect(RectF(393f, y, 393f + boxWidth, y + boxHeight), 4f, 4f, linePaint)
                canvas.drawText("সর্বমোট বর্তমান বকেয়া", 403f, y + 14f, subPaint)
                val netDuePaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(234, 88, 12)
                }
                canvas.drawText("$currency${customer.totalDue.toIntOrNull() ?: customer.totalDue}", 403f, y + 29f, netDuePaint)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(241, 245, 249); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(30f, startY - 12f, 565f, startY + 10f), 4f, 4f, headerBg)
            canvas.drawText("#", 36f, startY, boldPaint)
            canvas.drawText("তারিখ ও সময়", 55f, startY, boldPaint)
            canvas.drawText("বিবরণ ও ক্রয়কৃত পণ্য", 155f, startY, boldPaint)
            canvas.drawText("বাকি প্রদান", 380f, startY, boldPaint)
            canvas.drawText("জমা আদায়", 445f, startY, boldPaint)
            canvas.drawText("চলতি বাকি", 510f, startY, boldPaint)
            return startY + 16f
        }

        drawHeaderAndCustomerCard(isFirstPage = true)
        var y = 180f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        var runningBal = 0.0

        history.sortedBy { it.timestamp }.forEachIndexed { index, log ->
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndCustomerCard(isFirstPage = false)
                y = 80f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(30f, y - 10f, 565f, y + 6f), rowBgAlt)
            }

            val isGiven = log.type == "DUE_GIVEN"
            if (isGiven) {
                runningBal += log.amount
            } else {
                runningBal = (runningBal - log.amount).coerceAtLeast(0.0)
            }

            val dateStr = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
            canvas.drawText("${index + 1}", 36f, y, textPaint)
            canvas.drawText(dateStr, 55f, y, textPaint)

            val cleanNote = log.note.replace("\n", " ")
            val noteStr = if (cleanNote.length > 34) cleanNote.take(32) + ".." else cleanNote
            canvas.drawText(noteStr, 155f, y, textPaint)

            if (isGiven) {
                canvas.drawText("+$currency${log.amount.toIntOrNull() ?: log.amount}", 380f, y, redPaint)
                canvas.drawText("-", 445f, y, textPaint)
            } else {
                canvas.drawText("-", 380f, y, textPaint)
                canvas.drawText("-$currency${log.amount.toIntOrNull() ?: log.amount}", 445f, y, greenPaint)
            }

            canvas.drawText("$currency${runningBal.toIntOrNull() ?: runningBal}", 510f, y, boldPaint)

            y += 16f
        }

        y += 10f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 16f

        // Table Bottom Summary Row
        canvas.drawText("মোট হিসাব:", 155f, y, boldPaint)
        canvas.drawText("+$currency${totalGiven.toIntOrNull() ?: totalGiven}", 380f, y, redPaint)
        canvas.drawText("-$currency${totalCollected.toIntOrNull() ?: totalCollected}", 445f, y, greenPaint)
        canvas.drawText("$currency${customer.totalDue.toIntOrNull() ?: customer.totalDue}", 510f, y, redPaint)

        drawSponsorFooter(canvas, 802f, "যেকোনো তথ্যের জন্য দোকানে যোগাযোগ করুন।")

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

        drawSponsorFooter(canvas, 802f, "Products Inventory Report")
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

        drawSponsorFooter(canvas, 802f, "Due Khata Statement")
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
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        val greenPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(5, 150, 105)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(220, 38, 38)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val saleTxs = transactions.filter { it.type == "SALE" }
        val totalSales = saleTxs.sumOf { it.totalAmount }
        val totalPaid = saleTxs.sumOf { it.paidAmount }
        val totalDue = saleTxs.sumOf { it.dueAmount }
        val totalProfit = saleTxs.sumOf { it.profitAmount }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(37, 99, 235); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 595f, 10f, topBarPaint)

            var y = 36f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 15f
            canvas.drawText(title, 297.5f, y, subPaint)
            y += 13f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber | মোট লেনদেন: ${transactions.size} টি", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 18f
                val boxWidth = 127f
                val boxHeight = 36f

                // Total Sales Box
                val b1 = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(30f, y, 30f + boxWidth, y + boxHeight), 4f, 4f, b1)
                canvas.drawRoundRect(RectF(30f, y, 30f + boxWidth, y + boxHeight), 4f, 4f, linePaint)
                canvas.drawText("মোট বিক্রি", 38f, y + 14f, subPaint)
                val blueBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(37, 99, 235) }
                canvas.drawText("$currency${totalSales.toIntOrNull() ?: totalSales}", 38f, y + 28f, blueBold)

                // Cash Collected Box
                val b2 = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(165f, y, 165f + boxWidth, y + boxHeight), 4f, 4f, b2)
                canvas.drawRoundRect(RectF(165f, y, 165f + boxWidth, y + boxHeight), 4f, 4f, linePaint)
                canvas.drawText("নগদ আদায়", 173f, y + 14f, subPaint)
                canvas.drawText("$currency${totalPaid.toIntOrNull() ?: totalPaid}", 173f, y + 28f, greenPaint)

                // Credit Due Box
                val b3 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(300f, y, 300f + boxWidth, y + boxHeight), 4f, 4f, b3)
                canvas.drawRoundRect(RectF(300f, y, 300f + boxWidth, y + boxHeight), 4f, 4f, linePaint)
                canvas.drawText("বাকি বিক্রি", 308f, y + 14f, subPaint)
                canvas.drawText("$currency${totalDue.toIntOrNull() ?: totalDue}", 308f, y + 28f, redPaint)

                // Net Profit Box
                val b4 = Paint().apply { color = Color.rgb(250, 245, 255); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(435f, y, 435f + boxWidth, y + boxHeight), 4f, 4f, b4)
                canvas.drawRoundRect(RectF(435f, y, 435f + boxWidth, y + boxHeight), 4f, 4f, linePaint)
                canvas.drawText("মোট বিক্রয় লাভ", 443f, y + 14f, subPaint)
                val purpleBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(147, 51, 234) }
                canvas.drawText("$currency${totalProfit.toIntOrNull() ?: totalProfit}", 443f, y + 28f, purpleBold)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(241, 245, 249); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(30f, startY - 12f, 565f, startY + 10f), 4f, 4f, headerBg)
            canvas.drawText("#", 35f, startY, boldPaint)
            canvas.drawText("সময়", 52f, startY, boldPaint)
            canvas.drawText("ক্রেতা ও মেমো", 110f, startY, boldPaint)
            canvas.drawText("পণ্য / বিবরণ", 225f, startY, boldPaint)
            canvas.drawText("পরিমাণ", 370f, startY, boldPaint)
            canvas.drawText("মোট মূল্য", 420f, startY, boldPaint)
            canvas.drawText("জমা", 475f, startY, boldPaint)
            canvas.drawText("বাকি", 525f, startY, boldPaint)
            return startY + 16f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 135f
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
                y = 80f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(30f, y - 10f, 565f, y + 6f), rowBgAlt)
            }

            val timeStr = SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
            canvas.drawText("${index + 1}", 35f, y, textPaint)
            canvas.drawText(timeStr, 52f, y, textPaint)

            // Customer and Invoice
            val custText = when {
                tx.customerName.isNotBlank() && !tx.invoiceNumber.isNullOrBlank() -> "${tx.customerName} (#${tx.invoiceNumber.takeLast(6)})"
                tx.customerName.isNotBlank() -> tx.customerName
                !tx.invoiceNumber.isNullOrBlank() -> "#${tx.invoiceNumber}"
                else -> "সাধারণ ক্রেতা"
            }
            val cleanCust = if (custText.length > 20) custText.take(18) + ".." else custText
            canvas.drawText(cleanCust, 110f, y, textPaint)

            // Product & Note
            val prodStr = if (tx.productName.length > 24) tx.productName.take(22) + ".." else tx.productName
            canvas.drawText(prodStr, 225f, y, textPaint)

            // Quantity
            val qtyStr = "${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}"
            canvas.drawText(qtyStr, 370f, y, textPaint)

            // Total Amount
            canvas.drawText("$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}", 420f, y, boldPaint)

            // Paid & Due
            if (tx.type == "SALE") {
                canvas.drawText("$currency${tx.paidAmount.toIntOrNull() ?: tx.paidAmount}", 475f, y, greenPaint)
                if (tx.dueAmount > 0) {
                    canvas.drawText("$currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount}", 525f, y, redPaint)
                } else {
                    canvas.drawText("৳0", 525f, y, textPaint)
                }
            } else {
                canvas.drawText(tx.type, 475f, y, textPaint)
                canvas.drawText("-", 525f, y, textPaint)
            }

            y += 16f
        }

        y += 10f
        canvas.drawLine(30f, y, 565f, y, linePaint)
        y += 16f

        // Table Bottom Summary Row
        canvas.drawText("সর্বমোট বিক্রয় হিসাব:", 225f, y, boldPaint)
        canvas.drawText("$currency${totalSales.toIntOrNull() ?: totalSales}", 420f, y, boldPaint)
        canvas.drawText("$currency${totalPaid.toIntOrNull() ?: totalPaid}", 475f, y, greenPaint)
        canvas.drawText("$currency${totalDue.toIntOrNull() ?: totalDue}", 525f, y, redPaint)

        drawSponsorFooter(canvas, 802f, "লেনদেন বিবরণী রিপোর্ট")
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

        drawSponsorFooter(canvas, 795f, "মহাজন বা সরবরাহকারীকে ফেরত/বদল বাবদ রিপোর্ট")
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "Expiring_Products_$timestamp.pdf")
    }

    /**
     * Generates Store Expenses PDF Report
     */
    fun generateExpensesPdf(
        context: Context,
        shopName: String,
        expenses: List<Expense>,
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
            color = Color.rgb(254, 226, 226) // Soft red header
            style = Paint.Style.FILL
        }

        val totalExpenseAmount = expenses.sumOf { it.amount }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(220, 38, 38); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, 595f, 12f, topBarPaint)

            var y = 42f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 16f
            canvas.drawText("দোকানের খরচ হিসাব রিপোর্ট (Store Expenses Statement)", 297.5f, y, subPaint)
            y += 14f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 20f
                val boxPaint = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 36f), 6f, 6f, boxPaint)
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 36f), 6f, 6f, linePaint)

                y += 18f
                canvas.drawText("মোট খরচের এন্ট্রি: ${expenses.size} টি", 50f, y, boldPaint)
                val redPaint = Paint().apply { isAntiAlias = true; textSize = 11f; color = Color.rgb(220, 38, 38); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                canvas.drawText("সর্বমোট খরচ: $currency${totalExpenseAmount.toIntOrNull() ?: totalExpenseAmount}", 380f, y, redPaint)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            canvas.drawRoundRect(RectF(35f, startY - 12f, 560f, startY + 10f), 4f, 4f, headerPaint)
            canvas.drawText("নং", 42f, startY, boldPaint)
            canvas.drawText("তারিখ ও সময়", 70f, startY, boldPaint)
            canvas.drawText("খরচের খাত / শিরোনাম", 180f, startY, boldPaint)
            canvas.drawText("ক্যাটাগরি", 340f, startY, boldPaint)
            canvas.drawText("পরিমাণ ($currency)", 470f, startY, boldPaint)
            return startY + 18f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 130f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
        val redTextPaint = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(220, 38, 38); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        expenses.forEachIndexed { index, exp ->
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
            val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(exp.timestamp))
            canvas.drawText(dateStr, 70f, y, textPaint)
            val titleStr = if (exp.title.length > 22) exp.title.take(20) + ".." else exp.title
            canvas.drawText(titleStr, 180f, y, boldPaint)
            val catStr = if (exp.category.length > 18) exp.category.take(16) + ".." else exp.category
            canvas.drawText(catStr, 340f, y, textPaint)
            canvas.drawText("${exp.amount.toIntOrNull() ?: exp.amount}", 470f, y, redTextPaint)

            y += 16f
        }

        drawSponsorFooter(canvas, 802f, "Store Expenses Report")
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "Expenses_Report_$timestamp.pdf")
    }

    /**
     * Generates Official Master Cash Book (দোকানের মূল ক্যাশ খাতা) PDF Statement
     */
    fun generateMasterCashBookPdf(
        context: Context,
        shopName: String,
        periodTitle: String,
        entries: List<MasterCashEntry>,
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
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val greenTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(22, 163, 74)
            textAlign = Paint.Align.RIGHT
        }
        val redTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(220, 38, 38)
            textAlign = Paint.Align.RIGHT
        }
        val balanceTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.RIGHT
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(30, 41, 59)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(51, 65, 85) // Slate header
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }

        val totalIncome = entries.filter { it.isAddition }.sumOf { it.amount }
        val totalExpense = entries.filter { !it.isAddition }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            val topBarPaint = Paint().apply { color = Color.rgb(15, 118, 110); style = Paint.Style.FILL } // Teal top bar
            canvas.drawRect(0f, 0f, 595f, 12f, topBarPaint)

            var y = 42f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 16f
            canvas.drawText("দোকানের মূল ক্যাশ খাতা স্টেটমেন্ট ($periodTitle)", 297.5f, y, subPaint)
            y += 14f
            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 20f
                val boxPaint = Paint().apply { color = Color.rgb(240, 253, 250); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 42f), 6f, 6f, boxPaint)
                canvas.drawRoundRect(RectF(35f, y, 560f, y + 42f), 6f, 6f, linePaint)

                y += 17f
                canvas.drawText("মোট লেনদেন: ${entries.size} টি", 50f, y, boldPaint)
                val sumGreen = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(22, 163, 74); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                val sumRed = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(220, 38, 38); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
                val sumBal = Paint().apply { isAntiAlias = true; textSize = 10f; color = Color.rgb(15, 23, 42); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

                canvas.drawText("মোট জমা: $currency${totalIncome.toIntOrNull() ?: totalIncome}", 180f, y, sumGreen)
                canvas.drawText("মোট প্রদত্ত: $currency${totalExpense.toIntOrNull() ?: totalExpense}", 315f, y, sumRed)
                canvas.drawText("নিট ব্যালেন্স: $currency${netBalance.toIntOrNull() ?: netBalance}", 440f, y, sumBal)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            canvas.drawRoundRect(RectF(35f, startY - 12f, 560f, startY + 10f), 4f, 4f, headerPaint)
            canvas.drawText("তারিখ ও সময়", 42f, startY, headerTextPaint)
            canvas.drawText("বিবরণ ও খাত / কাস্টমার", 160f, startY, headerTextPaint)
            val alignRightHeader = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.WHITE
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("জমা ($currency)", 375f, startY, alignRightHeader)
            canvas.drawText("প্রদত্ত ($currency)", 465f, startY, alignRightHeader)
            canvas.drawText("ব্যালেন্স ($currency)", 550f, startY, alignRightHeader)
            return startY + 18f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 138f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }

        entries.forEachIndexed { index, item ->
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

            val dateStr = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
            canvas.drawText(dateStr, 42f, y, textPaint)

            val displayTitle = if (item.title.length > 26) item.title.take(24) + ".." else item.title
            canvas.drawText(displayTitle, 160f, y, boldPaint)

            if (item.isAddition) {
                canvas.drawText("${item.amount.toIntOrNull() ?: item.amount}", 375f, y, greenTextPaint)
            } else {
                canvas.drawText("${item.amount.toIntOrNull() ?: item.amount}", 465f, y, redTextPaint)
            }

            val balStr = "${if (item.runningBalance < 0) "-" else ""}${Math.abs(item.runningBalance).toIntOrNull() ?: Math.abs(item.runningBalance)}"
            canvas.drawText(balStr, 550f, y, balanceTextPaint)

            y += 16f
        }

        drawSponsorFooter(canvas, 802f, "Master Cash Book Statement - নির্ভুল হিসাব গ্যারান্টিযুক্ত")
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "CashBook_Statement_$timestamp.pdf")
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

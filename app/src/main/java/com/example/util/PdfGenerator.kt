package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CashAdditionItem
import com.example.data.model.CashLog
import com.example.data.model.Customer
import com.example.data.model.DueLog
import com.example.data.model.Expense
import com.example.data.model.MasterCashEntry
import com.example.data.model.Product
import com.example.data.model.ReorderItem
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

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(30, 41, 59)
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.RIGHT
        }

        // Outer Document Frame
        canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

        // Header Background bar
        val headerBgPaint = Paint().apply {
            color = Color.rgb(16, 185, 129) // Emerald header
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(24f, 16f, 571f, 24f), 0f, 0f, headerBgPaint)

        // Store Header
        var y = 48f
        canvas.drawText(invoice.shopName, 297.5f, y, titlePaint)
        y += 16f
        if (invoice.shopAddress.isNotBlank()) {
            canvas.drawText(invoice.shopAddress, 297.5f, y, subPaint)
            y += 14f
        }
        canvas.drawText("মোবাইল: ${invoice.shopPhone}", 297.5f, y, subPaint)
        y += 18f

        // Document Title Badge
        val memoBadgePaint = Paint().apply {
            color = Color.rgb(236, 253, 245)
            style = Paint.Style.FILL
        }
        val memoBorder = Paint().apply {
            color = Color.rgb(167, 243, 208)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(185f, y - 13f, 410f, y + 9f), 11f, 11f, memoBadgePaint)
        canvas.drawRoundRect(RectF(185f, y - 13f, 410f, y + 9f), 11f, 11f, memoBorder)

        val memoTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(4, 120, 87)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CASH MEMO / ডিজিটাল ক্যাশ মেমো", 297.5f, y + 2f, memoTextPaint)
        y += 24f

        // Customer & Invoice Details Box
        val custBoxBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(34f, y - 6f, 561f, y + 36f), 5f, 5f, custBoxBg)
        canvas.drawRoundRect(RectF(34f, y - 6f, 561f, y + 36f), 5f, 5f, linePaint)

        val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))
        canvas.drawText("মেমো নং: #${invoice.invoiceNumber}", 44f, y + 12f, boldPaint)
        canvas.drawText("কাস্টমার: ${invoice.customerName}", 320f, y + 12f, boldPaint)
        canvas.drawText("তারিখ: $dateStr", 44f, y + 26f, textPaint)
        if (invoice.customerPhone.isNotBlank()) {
            canvas.drawText("মোবাইল: ${invoice.customerPhone}", 320f, y + 26f, textPaint)
        }
        y += 48f

        // Table Header
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(34f, y - 13f, 561f, y + 11f), 4f, 4f, tableHeaderPaint)

        canvas.drawText("নং", 46f, y, centerHeaderPaint)
        canvas.drawText("পণ্যের বিবরণ (Item Name)", 70f, y, leftHeaderPaint)
        canvas.drawText("পরিমাণ", 330f, y, rightHeaderPaint)
        canvas.drawText("দর (Rate)", 425f, y, rightHeaderPaint)
        canvas.drawText("মোট ($currency)", 553f, y, rightHeaderPaint)
        y += 18f

        // Items List
        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        invoice.items.forEachIndexed { index, item ->
            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 11f, 561f, y + 7f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 7f, 561f, y + 7f, linePaint)

            canvas.drawText("${index + 1}", 46f, y, centerTextPaint)
            val prodTitle = if (item.product.name.length > 32) item.product.name.take(30) + ".." else item.product.name
            canvas.drawText(prodTitle, 70f, y, boldPaint)
            canvas.drawText("${item.quantity.toIntOrNull() ?: item.quantity} ${item.product.unit}", 330f, y, rightTextPaint)
            canvas.drawText("$currency${item.customPrice.toIntOrNull() ?: item.customPrice}", 425f, y, rightTextPaint)
            canvas.drawText("$currency${item.total.toIntOrNull() ?: item.total}", 553f, y, rightBoldPaint)

            y += 18f
        }

        y += 12f

        // Financial Summary on Right Side
        val summaryX = 310f
        val valX = 553f

        val greenRightPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(4, 120, 87)
            textAlign = Paint.Align.RIGHT
        }
        val dueOrangeRightPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
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
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(234, 88, 12)
            }
            canvas.drawText("আজকের নতুন বাকি (Today Due):", summaryX, y, dueOrangeLabel)
            canvas.drawText("$currency${invoice.dueAmount.toIntOrNull() ?: invoice.dueAmount}", valX, y, dueOrangeRightPaint)
            y += 16f

            if (invoice.previousDue > 0) {
                val prevDueLabel = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
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
            canvas.drawRoundRect(RectF(summaryX - 6f, y - 12f, valX + 6f, y + 6f), 4f, 4f, totalDueBg)
            canvas.drawRoundRect(RectF(summaryX - 6f, y - 12f, valX + 6f, y + 6f), 4f, 4f, totalDueBorder)

            val totalDueRedLabel = Paint().apply {
                isAntiAlias = true
                textSize = 11f
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

        // Signatures Block
        val sigY = 755f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("কাস্টমারের স্বাক্ষর", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("বিক্রেতার স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        val footerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            color = Color.rgb(5, 150, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for your business! ধন্যবাদ, আবার আসবেন।", 297.5f, 788f, footerTextPaint)
        drawSponsorFooter(canvas, 812f, "NAFI KHATA ডিজিটাল ক্যাশ মেমো")

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
        expensesList: List<Expense> = emptyList(),
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(100, 116, 139)
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
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }

        // Outer Document Frame
        canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

        // Top banner
        val topBarPaint = Paint().apply { color = Color.rgb(16, 185, 129); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(24f, 16f, 571f, 24f), 0f, 0f, topBarPaint)

        var y = 48f
        canvas.drawText(shopName, 297.5f, y, titlePaint)
        y += 18f

        // Document Title Badge
        val badgeBg = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
        val badgeBorder = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBg)
        canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBorder)

        val badgeText = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(21, 128, 61)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("লাভ-ক্ষতি ও সার্বিক রিপোর্ট / PROFIT & LOSS STATEMENT", 297.5f, y + 2f, badgeText)
        y += 20f

        val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("রিপোর্ট সময়কাল: $periodTitle", 34f, y, boldPaint)
        val dateRightPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("তারিখ: $genDate", 561f, y, dateRightPaint)
        y += 10f
        canvas.drawLine(34f, y, 561f, y, linePaint)
        y += 12f

        // Financial Overview Box
        val boxPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(34f, y, 561f, y + 92f), 6f, 6f, boxPaint)
        canvas.drawRoundRect(RectF(34f, y, 561f, y + 92f), 6f, 6f, borderPaint)

        val rightNetProfitPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (netProfit >= 0) Color.rgb(21, 128, 61) else Color.rgb(220, 38, 38)
            textAlign = Paint.Align.RIGHT
        }

        y += 18f
        canvas.drawText("১. সর্বমোট বিক্রয় (Total Sales):", 46f, y, textPaint)
        canvas.drawText("$currency${totalSales.toIntOrNull() ?: totalSales}", 270f, y, rightBoldPaint)

        canvas.drawText("২. পণ্যের ক্রয়মূল্য (Cost of Goods):", 310f, y, textPaint)
        canvas.drawText("- $currency${salesCost.toIntOrNull() ?: salesCost}", 545f, y, rightTextPaint)
        y += 20f

        canvas.drawText("৩. মোট বিক্রয় লাভ (Gross Profit):", 46f, y, boldPaint)
        canvas.drawText("$currency${grossProfit.toIntOrNull() ?: grossProfit}", 270f, y, rightBoldPaint)

        canvas.drawText("৪. দোকানের মোট খরচ (Expenses):", 310f, y, textPaint)
        canvas.drawText("- $currency${expenses.toIntOrNull() ?: expenses}", 545f, y, rightTextPaint)
        y += 24f

        val netProfitPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (netProfit >= 0) Color.rgb(21, 128, 61) else Color.rgb(220, 38, 38)
        }
        canvas.drawText("★ নিট লাভ/মুনাফা (NET PROFIT):", 46f, y, netProfitPaint)
        canvas.drawText("$currency${netProfit.toIntOrNull() ?: netProfit}", 270f, y, rightNetProfitPaint)

        canvas.drawText("কাস্টমার বাকি পাওনা (Dues):", 310f, y, textPaint)
        canvas.drawText("$currency${dueAmount.toIntOrNull() ?: dueAmount}", 545f, y, rightBoldPaint)

        y += 24f

        // Expense Breakdown by Sector (দোকান খরচ খাতওয়ারী হিসাব)
        if (expensesList.isNotEmpty()) {
            val expByCategory = expensesList.groupBy { it.category.ifBlank { "অন্যান্য" } }
                .mapValues { it.value.sumOf { exp -> exp.amount } }
                .toList()
                .sortedByDescending { it.second }

            canvas.drawText("দোকান খরচের খাতওয়ারী সারসংক্ষেপ (Store Expenses Breakdown):", 34f, y, boldPaint)
            y += 12f

            val expHeaderPaint = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 11f, 561f, y + 9f), 4f, 4f, expHeaderPaint)

            canvas.drawText("খরচের খাত / শিরোনাম", 44f, y, leftHeaderPaint)
            canvas.drawText("মোট এন্ট্রি", 260f, y, leftHeaderPaint)
            canvas.drawText("পরিমাণ ($currency)", 410f, y, rightHeaderPaint)
            canvas.drawText("অংশ (%)", 553f, y, rightHeaderPaint)
            y += 16f

            val totalExp = if (expenses > 0) expenses else expensesList.sumOf { it.amount }
            val expRowBg = Paint().apply { color = Color.rgb(255, 250, 250); style = Paint.Style.FILL }

            expByCategory.take(4).forEachIndexed { idx, (cat, catAmt) ->
                if (idx % 2 == 1) {
                    canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), expRowBg)
                }
                canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

                val entriesCount = expensesList.count { (it.category.ifBlank { "অন্যান্য" }) == cat }
                val pct = if (totalExp > 0) (catAmt / totalExp * 100).toInt() else 0

                canvas.drawText(cat, 44f, y, textPaint)
                canvas.drawText("$entriesCount টি", 260f, y, textPaint)
                val redRight = Paint().apply { isAntiAlias = true; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(220, 38, 38); textAlign = Paint.Align.RIGHT }
                canvas.drawText("- $currency${catAmt.toIntOrNull() ?: catAmt}", 410f, y, redRight)
                canvas.drawText("$pct%", 553f, y, rightTextPaint)
                y += 16f
            }
            y += 10f
        }

        // Recent Transactions Table Header
        canvas.drawText("রিপোর্টকালীন লেনদেন বিবরণী (Transaction Log):", 34f, y, boldPaint)
        y += 12f

        val tableHeaderPaint = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(34f, y - 11f, 561f, y + 9f), 4f, 4f, tableHeaderPaint)
        canvas.drawText("ধরন", 44f, y, leftHeaderPaint)
        canvas.drawText("পণ্যের নাম ও বিবরণ", 120f, y, leftHeaderPaint)
        canvas.drawText("পরিমাণ", 330f, y, rightHeaderPaint)
        canvas.drawText("মোট টাকা ($currency)", 430f, y, rightHeaderPaint)
        canvas.drawText("সময়", 553f, y, rightHeaderPaint)
        y += 16f

        val rowBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        val maxTxToDraw = if (expensesList.isNotEmpty()) 10 else 18
        transactions.take(maxTxToDraw).forEachIndexed { i, tx ->
            if (y > 730f) return@forEachIndexed
            if (i % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBg)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            val typeStr = when(tx.type) {
                "SALE" -> "বিক্রয়"
                "STOCK_IN" -> "স্টক ইন"
                else -> "সমন্বয়"
            }
            canvas.drawText(typeStr, 44f, y, boldPaint)
            val title = if (tx.productName.length > 25) tx.productName.take(23) + ".." else tx.productName
            canvas.drawText(title, 120f, y, textPaint)
            canvas.drawText("${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}", 330f, y, rightTextPaint)
            canvas.drawText("$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}", 430f, y, rightBoldPaint)
            val timeShort = SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
            canvas.drawText(timeShort, 553f, y, rightTextPaint)

            y += 16f
        }

        // Signatures Block
        val sigY = 760f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("হিসাব প্রস্তুতকারী", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("স্বত্বাধিকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        // Footer
        drawSponsorFooter(canvas, 812f, "NAFI KHATA লাভ-ক্ষতি স্টেটমেন্ট প্রিন্ট")

        pdfDocument.finishPage(page)
        return savePdfToFile(context, pdfDocument, "Report_${periodTitle.replace(" ", "_")}.pdf")
    }

    /**
     * Generates a Stock In & Stock Out Valuation & Movement Statement PDF
     * Styled like an authentic shop memo / voucher (দোকানের মেমোর মতো আকর্ষণীয় ও নির্ভুল ডিজাইন)
     */
    fun generateStockInOutPdf(
        context: Context,
        shopName: String,
        periodTitle: String,
        totalStockInAmount: Double,
        totalStockInQty: Double,
        totalStockOutSales: Double,
        totalStockOutCost: Double,
        totalStockOutQty: Double,
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(100, 116, 139)
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
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }

        fun drawMemoDecorations(isFirstPage: Boolean): Float {
            // Outer neat document border
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            // Top primary accent bar
            val topBarPaint = Paint().apply { color = Color.rgb(16, 185, 129); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 24f), 0f, 0f, topBarPaint)

            var y = 48f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            // Memo Title Badge
            val badgeBg = Paint().apply { color = Color.rgb(236, 253, 245); style = Paint.Style.FILL }
            val badgeStroke = Paint().apply { color = Color.rgb(16, 185, 129); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(160f, y - 13f, 435f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(160f, y - 13f, 435f, y + 9f), 11f, 11f, badgeStroke)

            val badgeTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(4, 120, 87)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("স্টক ইন-আউট মেমো ভাউচার / STOCK MOVEMENT MEMO", 297.5f, y + 2f, badgeTextPaint)
            y += 20f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("মেমো সময়কাল: $periodTitle", 34f, y, boldPaint)
            val dateRightPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.rgb(71, 85, 105)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 561f, y, dateRightPaint)
            y += 10f

            canvas.drawLine(34f, y, 561f, y, linePaint)
            y += 12f

            if (isFirstPage) {
                // Two Distinct Summary Voucher Cards (Left: Stock In, Right: Stock Out)
                val cardWidth = 259f
                val cardHeight = 60f

                // Left Card (Stock In)
                val inBg = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                val inBorder = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 6f, 6f, inBg)
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 6f, 6f, inBorder)

                val inLabelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(21, 128, 61)
                }
                canvas.drawText("📥 মোট পণ্য স্টক ইন (ক্রয় চালান)", 44f, y + 16f, inLabelPaint)
                val inValPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(21, 128, 61)
                }
                canvas.drawText("$currency${totalStockInAmount.toIntOrNull() ?: totalStockInAmount}", 44f, y + 36f, inValPaint)
                canvas.drawText("মোট পরিমাণ: ${totalStockInQty.toIntOrNull() ?: totalStockInQty} টি পণ্য", 44f, y + 51f, subPaint)

                // Right Card (Stock Out)
                val outBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                val outBorder = Paint().apply { color = Color.rgb(254, 202, 202); style = Paint.Style.STROKE; strokeWidth = 1f }
                val rightCardX = 302f
                canvas.drawRoundRect(RectF(rightCardX, y, rightCardX + cardWidth, y + cardHeight), 6f, 6f, outBg)
                canvas.drawRoundRect(RectF(rightCardX, y, rightCardX + cardWidth, y + cardHeight), 6f, 6f, outBorder)

                val outLabelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(190, 18, 60)
                }
                canvas.drawText("📤 মোট পণ্য স্টক আউট (বিক্রি)", rightCardX + 10f, y + 16f, outLabelPaint)
                val outValPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(190, 18, 60)
                }
                canvas.drawText("$currency${totalStockOutSales.toIntOrNull() ?: totalStockOutSales}", rightCardX + 10f, y + 36f, outValPaint)
                canvas.drawText("কেনা খরচ: $currency${totalStockOutCost.toIntOrNull() ?: totalStockOutCost} • ${totalStockOutQty.toIntOrNull() ?: totalStockOutQty} টি পণ্য", rightCardX + 10f, y + 51f, subPaint)

                y += cardHeight + 8f

                // Net Capital Flow Strip
                val netFlow = totalStockInAmount - totalStockOutCost
                val flowBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                val flowBorder = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRoundRect(RectF(34f, y, 561f, y + 26f), 4f, 4f, flowBg)
                canvas.drawRoundRect(RectF(34f, y, 561f, y + 26f), 4f, 4f, flowBorder)

                canvas.drawText("স্টক মূলধনের নিট প্রবাহ (ইন মূল্য - আউট খরচ):", 44f, y + 17f, boldPaint)
                val netFlowPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = if (netFlow >= 0) Color.rgb(2, 132, 199) else Color.rgb(225, 29, 72)
                    textAlign = Paint.Align.RIGHT
                }
                val sign = if (netFlow >= 0) "+" else ""
                canvas.drawText("$sign$currency${netFlow.toIntOrNull() ?: netFlow}", 551f, y + 17f, netFlowPaint)

                y += 34f
            }

            return y
        }

        fun drawTableHeader(startY: Float): Float {
            val thBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 13f, 561f, startY + 11f), 4f, 4f, thBg)

            canvas.drawText("নং", 46f, startY, centerHeaderPaint)
            canvas.drawText("ধরন", 76f, startY, centerHeaderPaint)
            canvas.drawText("তারিখ ও সময়", 102f, startY, leftHeaderPaint)
            canvas.drawText("পণ্যের নাম ও বিবরণ", 196f, startY, leftHeaderPaint)
            canvas.drawText("পরিমাণ", 420f, startY, rightHeaderPaint)
            canvas.drawText("দর", 480f, startY, rightHeaderPaint)
            canvas.drawText("মোট টাকা", 553f, startY, rightHeaderPaint)

            return startY + 18f
        }

        var y = drawMemoDecorations(isFirstPage = true)
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        val inBadgePaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(21, 128, 61)
            textAlign = Paint.Align.CENTER
        }
        val outBadgePaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(190, 18, 60)
            textAlign = Paint.Align.CENTER
        }
        val inPillBg = Paint().apply { color = Color.rgb(220, 252, 231); style = Paint.Style.FILL }
        val outPillBg = Paint().apply { color = Color.rgb(254, 226, 226); style = Paint.Style.FILL }

        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }

        transactions.forEachIndexed { index, tx ->
            if (y > 745f) {
                // Draw footer before finishing page
                drawSponsorFooter(canvas, 812f, "NAFI KHATA মেমো ভাউচার • পৃষ্ঠা: $pageNumber")
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                y = drawMemoDecorations(isFirstPage = false)
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 11f, 561f, y + 7f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 7f, 561f, y + 7f, linePaint)

            val isStockIn = tx.type == "STOCK_IN" || tx.type == "PURCHASE"

            // SL No
            canvas.drawText("${index + 1}", 46f, y, centerTextPaint)

            // IN / OUT Badge
            val badgeRect = RectF(62f, y - 10f, 90f, y + 5f)
            canvas.drawRoundRect(badgeRect, 4f, 4f, if (isStockIn) inPillBg else outPillBg)
            canvas.drawText(if (isStockIn) "ইন" else "আউট", 76f, y + 1f, if (isStockIn) inBadgePaint else outBadgePaint)

            // Date & Time
            val timeShort = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
            canvas.drawText(timeShort, 102f, y, textPaint)

            // Product Name (truncated if long)
            val title = if (tx.productName.length > 25) tx.productName.take(23) + ".." else tx.productName
            canvas.drawText(title, 196f, y, textPaint)

            // Quantity (Right-aligned)
            canvas.drawText("${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}", 420f, y, rightTextPaint)

            // Rate (Right-aligned)
            val unitP = if (isStockIn) tx.costPrice.takeIf { it > 0 } ?: tx.unitPrice else tx.unitPrice
            canvas.drawText("$currency${unitP.toIntOrNull() ?: unitP}", 480f, y, rightTextPaint)

            // Total Amount (Right-aligned, bold)
            canvas.drawText("$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}", 553f, y, rightBoldPaint)

            y += 18f
        }

        // Totals Footer Row at bottom of table
        if (y <= 745f) {
            val totalRowBg = Paint().apply { color = Color.rgb(241, 245, 249); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("মোট হিসাব: ${transactions.size} টি লেনদেন", 44f, y + 5f, boldPaint)
            val sumText = "ইন: $currency${totalStockInAmount.toIntOrNull() ?: totalStockInAmount}  |  আউট: $currency${totalStockOutSales.toIntOrNull() ?: totalStockOutSales}"
            canvas.drawText(sumText, 553f, y + 5f, rightBoldPaint)
            y += 24f
        }

        // Signatures Block (মেমোর মতো স্বাক্ষর)
        val sigY = 780f
        val sigLinePaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 1f
        }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("হিসাবরক্ষক / প্রস্তুতকারী", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("স্বত্বাধিকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        // Footer note
        drawSponsorFooter(canvas, 812f, "NAFI KHATA ডিজিটাল খাতা ও মেমো প্রিন্ট • নির্ভুল হিসাবের বিশ্বস্ত সঙ্গী")

        pdfDocument.finishPage(page)
        return savePdfToFile(context, pdfDocument, "Stock_InOut_${periodTitle.replace(" ", "_")}.pdf")
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(100, 116, 139)
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val rightGreenPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(21, 128, 61)
            textAlign = Paint.Align.RIGHT
        }
        val rightRedPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(220, 38, 38)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }

        val totalGiven = history.filter { it.type == "DUE_GIVEN" }.sumOf { it.amount }
        val totalCollected = history.filter { it.type == "DUE_COLLECTED" }.sumOf { it.amount }

        fun drawHeaderAndCustomerCard(isFirstPage: Boolean): Float {
            // Document border
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            // Top accent bar
            val topBarPaint = Paint().apply { color = Color.rgb(234, 88, 12); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 24f), 0f, 0f, topBarPaint)

            var y = 48f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            // Document Title Badge
            val badgeBg = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(254, 215, 170); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(150f, y - 13f, 445f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(150f, y - 13f, 445f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(194, 65, 12)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("কাস্টমার বাকি খাতা ভাউচার / CUSTOMER DUE MEMO", 297.5f, y + 2f, badgeText)
            y += 20f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("হিসাব নং: CUST-${customer.id}", 34f, y, boldPaint)
            val dateRightPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.rgb(71, 85, 105)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 561f, y, dateRightPaint)
            y += 10f
            canvas.drawLine(34f, y, 561f, y, linePaint)
            y += 12f

            if (isFirstPage) {
                // Customer Profile Box
                val custBoxPaint = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                val custBorder = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRoundRect(RectF(34f, y, 561f, y + 42f), 6f, 6f, custBoxPaint)
                canvas.drawRoundRect(RectF(34f, y, 561f, y + 42f), 6f, 6f, custBorder)

                canvas.drawText("কাস্টমারের নাম: ${customer.name}", 44f, y + 16f, boldPaint)
                canvas.drawText("মোবাইল: ${customer.phone.ifBlank { "N/A" }}", 44f, y + 32f, textPaint)
                val addrStr = if (customer.address.length > 25) customer.address.take(23) + ".." else customer.address.ifBlank { "দোকানের নিয়মিত খদ্দের" }
                canvas.drawText("ঠিকানা: $addrStr", 300f, y + 16f, textPaint)
                canvas.drawText("মোট মোট বেচাকেনা: $currency${customer.totalPurchased.toIntOrNull() ?: customer.totalPurchased}", 300f, y + 32f, textPaint)

                y += 50f

                // 3 Summary Metric Cards Side by Side
                val cardWidth = 171f
                val cardHeight = 44f

                // Card 1: Total Given
                val b1 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                val b1Border = Paint().apply { color = Color.rgb(254, 202, 202); style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 5f, 5f, b1Border)
                val redLabel = Paint().apply { isAntiAlias = true; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(185, 28, 28) }
                val redVal = Paint().apply { isAntiAlias = true; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(220, 38, 38) }
                canvas.drawText("মোট বাকি প্রদান", 42f, y + 14f, redLabel)
                canvas.drawText("+$currency${totalGiven.toIntOrNull() ?: totalGiven}", 42f, y + 32f, redVal)

                // Card 2: Total Collected
                val b2 = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                val b2Border = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 1f }
                val c2X = 34f + cardWidth + 7f
                canvas.drawRoundRect(RectF(c2X, y, c2X + cardWidth, y + cardHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(c2X, y, c2X + cardWidth, y + cardHeight), 5f, 5f, b2Border)
                val greenLabel = Paint().apply { isAntiAlias = true; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(21, 128, 61) }
                val greenVal = Paint().apply { isAntiAlias = true; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(21, 128, 61) }
                canvas.drawText("মোট জমা আদায়", c2X + 8f, y + 14f, greenLabel)
                canvas.drawText("-$currency${totalCollected.toIntOrNull() ?: totalCollected}", c2X + 8f, y + 32f, greenVal)

                // Card 3: Net Current Due
                val b3 = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
                val b3Border = Paint().apply { color = Color.rgb(254, 215, 170); style = Paint.Style.STROKE; strokeWidth = 1f }
                val c3X = c2X + cardWidth + 7f
                canvas.drawRoundRect(RectF(c3X, y, c3X + cardWidth, y + cardHeight), 5f, 5f, b3)
                canvas.drawRoundRect(RectF(c3X, y, c3X + cardWidth, y + cardHeight), 5f, 5f, b3Border)
                val dueLabel = Paint().apply { isAntiAlias = true; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(194, 65, 12) }
                val dueVal = Paint().apply { isAntiAlias = true; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(234, 88, 12) }
                canvas.drawText("সর্বমোট বর্তমান বকেয়া", c3X + 8f, y + 14f, dueLabel)
                canvas.drawText("$currency${customer.totalDue.toIntOrNull() ?: customer.totalDue}", c3X + 8f, y + 32f, dueVal)

                y += cardHeight + 12f
            }

            return y
        }

        fun drawTableHeader(startY: Float): Float {
            val thBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 13f, 561f, startY + 11f), 4f, 4f, thBg)

            canvas.drawText("নং", 46f, startY, centerHeaderPaint)
            canvas.drawText("তারিখ ও সময়", 68f, startY, leftHeaderPaint)
            canvas.drawText("বিবরণ ও ক্রয়কৃত পণ্য", 175f, startY, leftHeaderPaint)
            canvas.drawText("বাকি প্রদান (+)", 415f, startY, rightHeaderPaint)
            canvas.drawText("জমা আদায় (-)", 485f, startY, rightHeaderPaint)
            canvas.drawText("চলতি বাকি", 553f, startY, rightHeaderPaint)

            return startY + 18f
        }

        var y = drawHeaderAndCustomerCard(isFirstPage = true)
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        var runningBal = 0.0

        val sortedHistory = history.sortedBy { it.timestamp }
        sortedHistory.forEachIndexed { index, log ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "NAFI KHATA বাকি খাতা ভাউচার • পৃষ্ঠা: $pageNumber")
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                y = drawHeaderAndCustomerCard(isFirstPage = false)
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 11f, 561f, y + 7f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 7f, 561f, y + 7f, linePaint)

            val isGiven = log.type == "DUE_GIVEN"
            if (isGiven) {
                runningBal += log.amount
            } else {
                runningBal = (runningBal - log.amount).coerceAtLeast(0.0)
            }

            // SL No
            canvas.drawText("${index + 1}", 46f, y, centerTextPaint)

            // Date & Time
            val dateStr = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
            canvas.drawText(dateStr, 68f, y, textPaint)

            // Description
            val cleanNote = log.note.replace("\n", " ")
            val noteStr = if (cleanNote.length > 32) cleanNote.take(30) + ".." else cleanNote
            canvas.drawText(noteStr, 175f, y, textPaint)

            // Due Given / Collected / Running Balance (all right-aligned)
            if (isGiven) {
                canvas.drawText("+$currency${log.amount.toIntOrNull() ?: log.amount}", 415f, y, rightRedPaint)
                canvas.drawText("-", 485f, y, centerTextPaint)
            } else {
                canvas.drawText("-", 415f, y, centerTextPaint)
                canvas.drawText("-$currency${log.amount.toIntOrNull() ?: log.amount}", 485f, y, rightGreenPaint)
            }

            canvas.drawText("$currency${runningBal.toIntOrNull() ?: runningBal}", 553f, y, rightBoldPaint)

            y += 18f
        }

        // Table Bottom Summary Row
        if (y <= 745f) {
            val totalRowBg = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(254, 215, 170); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("মোট হিসাব:", 68f, y + 5f, boldPaint)
            canvas.drawText("+$currency${totalGiven.toIntOrNull() ?: totalGiven}", 415f, y + 5f, rightRedPaint)
            canvas.drawText("-$currency${totalCollected.toIntOrNull() ?: totalCollected}", 485f, y + 5f, rightGreenPaint)
            canvas.drawText("$currency${customer.totalDue.toIntOrNull() ?: customer.totalDue}", 553f, y + 5f, rightRedPaint)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("কাস্টমারের স্বাক্ষর", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("দোকানদারের স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA ডিজিটাল বাকি খাতা ও মেমো প্রিন্ট • নির্ভুল হিসাবের বিশ্বস্ত সঙ্গী")

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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(100, 116, 139)
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }

        val totalStockVal = products.sumOf { it.stockQuantity * it.sellPrice }
        val totalCostVal = products.sumOf { it.stockQuantity * it.buyPrice }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean): Float {
            // Document border
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            // Top accent bar
            val topBarPaint = Paint().apply { color = Color.rgb(16, 185, 129); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 24f), 0f, 0f, topBarPaint)

            var y = 48f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            // Title Badge
            val badgeBg = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(145f, y - 13f, 450f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(145f, y - 13f, 450f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(21, 128, 61)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("পণ্য স্টক ইনভেন্টরি স্টেটমেন্ট / INVENTORY STOCK MEMO", 297.5f, y + 2f, badgeText)
            y += 20f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("দোকানের মোট মজুদ ইনভেন্টরি", 34f, y, boldPaint)
            val dateRightPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.rgb(71, 85, 105)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 561f, y, dateRightPaint)
            y += 10f
            canvas.drawLine(34f, y, 561f, y, linePaint)
            y += 12f

            if (drawSummaryBox) {
                // 3 Metric Cards
                val cardWidth = 171f
                val cardHeight = 44f

                // Card 1: Total Products
                val b1 = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                val b1Border = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 5f, 5f, b1Border)
                canvas.drawText("📦 মোট পণ্য আইটেম", 44f, y + 16f, subPaint)
                val c1Val = Paint().apply { isAntiAlias = true; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(15, 23, 42) }
                canvas.drawText("${products.size} টি", 44f, y + 33f, c1Val)

                // Card 2: Cost Asset Value
                val b2 = Paint().apply { color = Color.rgb(240, 253, 250); style = Paint.Style.FILL }
                val b2Border = Paint().apply { color = Color.rgb(153, 246, 228); style = Paint.Style.STROKE; strokeWidth = 1f }
                val c2X = 34f + cardWidth + 7f
                canvas.drawRoundRect(RectF(c2X, y, c2X + cardWidth, y + cardHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(c2X, y, c2X + cardWidth, y + cardHeight), 5f, 5f, b2Border)
                val c2Label = Paint().apply { isAntiAlias = true; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(15, 118, 110) }
                val c2Val = Paint().apply { isAntiAlias = true; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(13, 148, 136) }
                canvas.drawText("🏷️ ক্রয়মূল্য সম্পদ মান", c2X + 8f, y + 16f, c2Label)
                canvas.drawText("$currency${totalCostVal.toIntOrNull() ?: totalCostVal}", c2X + 8f, y + 33f, c2Val)

                // Card 3: Sell Asset Value
                val b3 = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                val b3Border = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 1f }
                val c3X = c2X + cardWidth + 7f
                canvas.drawRoundRect(RectF(c3X, y, c3X + cardWidth, y + cardHeight), 5f, 5f, b3)
                canvas.drawRoundRect(RectF(c3X, y, c3X + cardWidth, y + cardHeight), 5f, 5f, b3Border)
                val c3Label = Paint().apply { isAntiAlias = true; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(21, 128, 61) }
                val c3Val = Paint().apply { isAntiAlias = true; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(22, 163, 74) }
                canvas.drawText("💰 বিক্রয়মূল্য সম্পদ মান", c3X + 8f, y + 16f, c3Label)
                canvas.drawText("$currency${totalStockVal.toIntOrNull() ?: totalStockVal}", c3X + 8f, y + 33f, c3Val)

                y += cardHeight + 12f
            }

            return y
        }

        fun drawTableHeader(startY: Float): Float {
            val thBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 13f, 561f, startY + 11f), 4f, 4f, thBg)

            canvas.drawText("নং", 46f, startY, centerHeaderPaint)
            canvas.drawText("পণ্যের নাম (Product Name)", 68f, startY, leftHeaderPaint)
            canvas.drawText("ক্যাটাগরি", 230f, startY, leftHeaderPaint)
            canvas.drawText("ক্রয়দর", 335f, startY, rightHeaderPaint)
            canvas.drawText("বিক্রয়দর", 400f, startY, rightHeaderPaint)
            canvas.drawText("স্টক পরিমাণ", 475f, startY, rightHeaderPaint)
            canvas.drawText("মোট মূল্য ($currency)", 553f, startY, rightHeaderPaint)

            return startY + 18f
        }

        var y = drawHeaderAndSummary(drawSummaryBox = true)
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }

        products.forEachIndexed { index, p ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "NAFI KHATA পণ্য ইনভেন্টরি স্টেটমেন্ট • পৃষ্ঠা: $pageNumber")
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                y = drawHeaderAndSummary(drawSummaryBox = false)
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 11f, 561f, y + 7f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 7f, 561f, y + 7f, linePaint)

            val itemTotal = p.stockQuantity * p.sellPrice

            // SL No
            canvas.drawText("${index + 1}", 46f, y, centerTextPaint)

            // Name
            val nameStr = if (p.name.length > 24) p.name.take(22) + ".." else p.name
            canvas.drawText(nameStr, 68f, y, boldPaint)

            // Category
            val catStr = if (p.category.length > 15) p.category.take(13) + ".." else p.category
            canvas.drawText(catStr, 230f, y, textPaint)

            // Buy Price
            canvas.drawText("${p.buyPrice.toIntOrNull() ?: p.buyPrice}", 335f, y, rightTextPaint)

            // Sell Price
            canvas.drawText("${p.sellPrice.toIntOrNull() ?: p.sellPrice}", 400f, y, rightTextPaint)

            // Stock Quantity
            canvas.drawText("${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}", 475f, y, rightTextPaint)

            // Total Value (sellPrice * qty)
            canvas.drawText("${itemTotal.toIntOrNull() ?: itemTotal}", 553f, y, rightBoldPaint)

            y += 18f
        }

        // Table Bottom Summary Row
        if (y <= 745f) {
            val totalRowBg = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(187, 247, 208); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("মোট ইনভেন্টরি সারসংক্ষেপ:", 68f, y + 5f, boldPaint)
            val rightCostBold = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(15, 118, 110)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("ক্রয়মূল্য: $currency${totalCostVal.toIntOrNull() ?: totalCostVal}", 400f, y + 5f, rightCostBold)

            val rightTotalBold = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(21, 128, 61)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("$currency${totalStockVal.toIntOrNull() ?: totalStockVal}", 553f, y + 5f, rightTotalBold)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("স্টক ইনচার্জের স্বাক্ষর", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("স্বত্বাধিকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA ডিজিটাল ইনভেন্টরি ও মেমো প্রিন্ট • নির্ভুল হিসাবের বিশ্বস্ত সঙ্গী")
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(100, 116, 139)
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val rightDuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(220, 38, 38)
            textAlign = Paint.Align.RIGHT
        }

        val debtors = customers.filter { it.totalDue > 0 }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean): Float {
            // Document border
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            // Top accent bar
            val topBarPaint = Paint().apply { color = Color.rgb(234, 88, 12); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 24f), 0f, 0f, topBarPaint)

            var y = 48f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            // Title Badge
            val badgeBg = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(254, 215, 170); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(150f, y - 13f, 445f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(150f, y - 13f, 445f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(194, 65, 12)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("সার্বিক কাস্টমার বাকি খাতা / ALL CUSTOMER DUES", 297.5f, y + 2f, badgeText)
            y += 20f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("মোট বাকি হিসাব তালিকা", 34f, y, boldPaint)
            val dateRightPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.rgb(71, 85, 105)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("তারিখ: $genDate | পৃষ্ঠা: $pageNumber", 561f, y, dateRightPaint)
            y += 10f
            canvas.drawLine(34f, y, 561f, y, linePaint)
            y += 12f

            if (drawSummaryBox) {
                // Two Summary Cards Side by Side
                val cardWidth = 259f
                val cardHeight = 44f

                // Card 1: Total Debtors
                val b1 = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                val b1Border = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + cardWidth, y + cardHeight), 5f, 5f, b1Border)

                canvas.drawText("👥 মোট বাকিদার কাস্টমার সংখ্যা", 44f, y + 16f, subPaint)
                val countValPaint = Paint().apply { isAntiAlias = true; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(15, 23, 42) }
                canvas.drawText("${debtors.size} জন", 44f, y + 34f, countValPaint)

                // Card 2: Total Dues
                val b2 = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
                val b2Border = Paint().apply { color = Color.rgb(254, 215, 170); style = Paint.Style.STROKE; strokeWidth = 1f }
                val rightCardX = 302f
                canvas.drawRoundRect(RectF(rightCardX, y, rightCardX + cardWidth, y + cardHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(rightCardX, y, rightCardX + cardWidth, y + cardHeight), 5f, 5f, b2Border)

                val dueLabelPaint = Paint().apply { isAntiAlias = true; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(194, 65, 12) }
                val dueValPaint = Paint().apply { isAntiAlias = true; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(234, 88, 12) }
                canvas.drawText("💰 দোকানের মোট পাওনা বাকি", rightCardX + 10f, y + 16f, dueLabelPaint)
                canvas.drawText("$currency${totalDue.toIntOrNull() ?: totalDue}", rightCardX + 10f, y + 34f, dueValPaint)

                y += cardHeight + 12f
            }

            return y
        }

        fun drawTableHeader(startY: Float): Float {
            val thBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 13f, 561f, startY + 11f), 4f, 4f, thBg)

            canvas.drawText("নং", 46f, startY, centerHeaderPaint)
            canvas.drawText("কাস্টমারের নাম", 70f, startY, leftHeaderPaint)
            canvas.drawText("মোবাইল নাম্বার", 240f, startY, leftHeaderPaint)
            canvas.drawText("ঠিকানা", 355f, startY, leftHeaderPaint)
            canvas.drawText("বাকি পরিমাণ ($currency)", 553f, startY, rightHeaderPaint)

            return startY + 18f
        }

        var y = drawHeaderAndSummary(drawSummaryBox = true)
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL }

        debtors.forEachIndexed { index, c ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "NAFI KHATA সার্বিক বাকি খাতা • পৃষ্ঠা: $pageNumber")
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                y = drawHeaderAndSummary(drawSummaryBox = false)
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 11f, 561f, y + 7f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 7f, 561f, y + 7f, linePaint)

            // SL No
            canvas.drawText("${index + 1}", 46f, y, centerTextPaint)

            // Customer Name
            val nameStr = if (c.name.length > 24) c.name.take(22) + ".." else c.name
            canvas.drawText(nameStr, 70f, y, boldPaint)

            // Phone
            canvas.drawText(c.phone.ifBlank { "N/A" }, 240f, y, textPaint)

            // Address
            val addrStr = if (c.address.length > 22) c.address.take(20) + ".." else c.address.ifBlank { "দোকানের নিয়মিত খদ্দের" }
            canvas.drawText(addrStr, 355f, y, textPaint)

            // Due Amount (Right-aligned)
            canvas.drawText("$currency${c.totalDue.toIntOrNull() ?: c.totalDue}", 553f, y, rightDuePaint)

            y += 18f
        }

        // Table Bottom Summary Row
        if (y <= 745f) {
            val totalRowBg = Paint().apply { color = Color.rgb(255, 247, 237); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(254, 215, 170); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("সর্বমোট বাকিদার: ${debtors.size} জন", 70f, y + 5f, boldPaint)
            val rightTotalBold = Paint().apply {
                isAntiAlias = true
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(220, 38, 38)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("মোট বকেয়া: $currency${totalDue.toIntOrNull() ?: totalDue}", 553f, y + 5f, rightTotalBold)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("হিসাবরক্ষক / প্রস্তুতকারী", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("স্বত্বাধিকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA ডিজিটাল বাকি খাতা ও মেমো প্রিন্ট • নির্ভুল হিসাবের বিশ্বস্ত সঙ্গী")
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val greenPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(21, 128, 61)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val redPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(220, 38, 38)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.RIGHT
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }

        val saleTxs = transactions.filter { it.type == "SALE" }
        val totalSales = saleTxs.sumOf { it.totalAmount }
        val totalPaid = saleTxs.sumOf { it.paidAmount }
        val totalDue = saleTxs.sumOf { it.dueAmount }
        val totalProfit = saleTxs.sumOf { it.profitAmount }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            // Outer Frame
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            val topBarPaint = Paint().apply { color = Color.rgb(37, 99, 235); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 23f), 0f, 0f, topBarPaint)

            var y = 46f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            val badgeBg = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(191, 219, 254); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(150f, y - 13f, 445f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(150f, y - 13f, 445f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(29, 78, 216)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(title, 297.5f, y + 2f, badgeText)
            y += 18f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | মোট এন্ট্রি: ${transactions.size} টি | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 14f
                val boxWidth = 125f
                val boxHeight = 36f

                // Total Sales Box
                val b1 = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                val cardLbl = Paint().apply { isAntiAlias = true; textSize = 8.5f; color = Color.rgb(100, 116, 139) }
                canvas.drawText("মোট বিক্রি", 42f, y + 14f, cardLbl)
                val blueBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(37, 99, 235) }
                canvas.drawText("$currency${totalSales.toIntOrNull() ?: totalSales}", 42f, y + 28f, blueBold)

                // Cash Collected Box
                val b2 = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(166f, y, 166f + boxWidth, y + boxHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(166f, y, 166f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("নগদ আদায়", 174f, y + 14f, cardLbl)
                val greenBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(21, 128, 61) }
                canvas.drawText("$currency${totalPaid.toIntOrNull() ?: totalPaid}", 174f, y + 28f, greenBold)

                // Credit Due Box
                val b3 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(298f, y, 298f + boxWidth, y + boxHeight), 5f, 5f, b3)
                canvas.drawRoundRect(RectF(298f, y, 298f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("বাকি বিক্রি", 306f, y + 14f, cardLbl)
                val redBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(220, 38, 38) }
                canvas.drawText("$currency${totalDue.toIntOrNull() ?: totalDue}", 306f, y + 28f, redBold)

                // Net Profit Box
                val b4 = Paint().apply { color = Color.rgb(250, 245, 255); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(430f, y, 430f + boxWidth, y + boxHeight), 5f, 5f, b4)
                canvas.drawRoundRect(RectF(430f, y, 430f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("মোট বিক্রয় লাভ", 438f, y + 14f, cardLbl)
                val purpleBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(147, 51, 234) }
                canvas.drawText("$currency${totalProfit.toIntOrNull() ?: totalProfit}", 438f, y + 28f, purpleBold)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 11f, 561f, startY + 9f), 4f, 4f, headerBg)
            canvas.drawText("#", 45f, startY, centerHeaderPaint)
            canvas.drawText("সময়", 58f, startY, leftHeaderPaint)
            canvas.drawText("ক্রেতা ও মেমো", 112f, startY, leftHeaderPaint)
            canvas.drawText("পণ্য / বিবরণ", 225f, startY, leftHeaderPaint)
            canvas.drawText("পরিমাণ", 365f, startY, rightHeaderPaint)
            canvas.drawText("মূল্য ($currency)", 435f, startY, rightHeaderPaint)
            canvas.drawText("জমা", 495f, startY, rightHeaderPaint)
            canvas.drawText("বাকি", 553f, startY, rightHeaderPaint)
            return startY + 16f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 146f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }

        transactions.forEachIndexed { index, tx ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "লেনদেন বিবরণী খতিয়ান")
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 86f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            val timeStr = SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
            canvas.drawText("${index + 1}", 45f, y, centerTextPaint)
            canvas.drawText(timeStr, 58f, y, textPaint)

            // Customer and Invoice
            val custText = when {
                tx.customerName.isNotBlank() && !tx.invoiceNumber.isNullOrBlank() -> "${tx.customerName} (#${tx.invoiceNumber.takeLast(5)})"
                tx.customerName.isNotBlank() -> tx.customerName
                !tx.invoiceNumber.isNullOrBlank() -> "#${tx.invoiceNumber}"
                else -> "সাধারণ ক্রেতা"
            }
            val cleanCust = if (custText.length > 18) custText.take(16) + ".." else custText
            canvas.drawText(cleanCust, 112f, y, textPaint)

            // Product & Note
            val prodStr = if (tx.productName.length > 22) tx.productName.take(20) + ".." else tx.productName
            canvas.drawText(prodStr, 225f, y, textPaint)

            // Quantity
            val qtyStr = "${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}"
            canvas.drawText(qtyStr, 365f, y, rightTextPaint)

            // Total Amount
            canvas.drawText("$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}", 435f, y, rightBoldPaint)

            // Paid & Due
            if (tx.type == "SALE") {
                canvas.drawText("$currency${tx.paidAmount.toIntOrNull() ?: tx.paidAmount}", 495f, y, greenPaint)
                if (tx.dueAmount > 0) {
                    canvas.drawText("$currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount}", 553f, y, redPaint)
                } else {
                    canvas.drawText("৳0", 553f, y, rightTextPaint)
                }
            } else {
                val typeLabel = if (tx.type == "STOCK_IN") "স্টক ইন" else tx.type
                canvas.drawText(typeLabel, 495f, y, rightTextPaint)
                canvas.drawText("-", 553f, y, rightTextPaint)
            }

            y += 16f
        }

        // Table Bottom Summary Row
        if (y <= 730f) {
            y += 6f
            val totalRowBg = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(191, 219, 254); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("সর্বমোট বিক্রয় হিসাব:", 112f, y + 5f, boldPaint)
            canvas.drawText("$currency${totalSales.toIntOrNull() ?: totalSales}", 435f, y + 5f, rightBoldPaint)
            canvas.drawText("$currency${totalPaid.toIntOrNull() ?: totalPaid}", 495f, y + 5f, greenPaint)
            canvas.drawText("$currency${totalDue.toIntOrNull() ?: totalDue}", 553f, y + 5f, redPaint)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("হিসাবরক্ষক / প্রস্তুতকারী", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("স্বত্বাধিকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA লেনদেন বিবরণী খতিয়ান • ক্যাশ মেমো প্রিন্ট")
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }

        val now = System.currentTimeMillis()
        val expiredCount = products.count { it.expiryDate in 1 until now }
        val expiringSoonCount = products.size - expiredCount
        val totalCost = products.sumOf { it.stockQuantity * it.buyPrice }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            // Outer Document Frame
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            val topBarPaint = Paint().apply { color = Color.rgb(220, 38, 38); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 23f), 0f, 0f, topBarPaint)

            var y = 46f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            val badgeBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(254, 202, 202); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(130f, y - 13f, 465f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(130f, y - 13f, 465f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(185, 28, 28)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("মেয়াদোত্তীর্ণ ও মেয়াদ শেষ পণ্যের খতিয়ান / EXPIRING PRODUCTS", 297.5f, y + 2f, badgeText)
            y += 18f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | মোট তালিকাভুক্ত পণ্য: ${products.size} টি | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 14f
                val boxWidth = 168f
                val boxHeight = 36f

                // Total Products Card
                val b1 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                val cardLbl = Paint().apply { isAntiAlias = true; textSize = 8.5f; color = Color.rgb(100, 116, 139) }
                canvas.drawText("মোট মেয়াদোত্তীর্ণ পণ্য", 42f, y + 14f, cardLbl)
                val redBold = Paint().apply { isAntiAlias = true; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(220, 38, 38) }
                canvas.drawText("$expiredCount টি (শেষ পর্যায়: $expiringSoonCount)", 42f, y + 28f, redBold)

                // Cost Card
                val b2 = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(214f, y, 214f + boxWidth, y + boxHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(214f, y, 214f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("আটকা ক্রয়মূল্য / মূলধন", 222f, y + 14f, cardLbl)
                val orangeBold = Paint().apply { isAntiAlias = true; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(217, 119, 6) }
                canvas.drawText("$currency${totalCost.toIntOrNull() ?: totalCost}", 222f, y + 28f, orangeBold)

                // Return Purpose Card
                val b3 = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(394f, y, 561f, y + boxHeight), 5f, 5f, b3)
                canvas.drawRoundRect(RectF(394f, y, 561f, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("উদ্দেশ্য ও ব্যবস্থা", 402f, y + 14f, cardLbl)
                val darkBold = Paint().apply { isAntiAlias = true; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(15, 23, 42) }
                canvas.drawText("মহাজনকে ফেরত / বদল বাবদ", 402f, y + 28f, darkBold)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 11f, 561f, startY + 9f), 4f, 4f, headerBg)
            canvas.drawText("#", 45f, startY, centerHeaderPaint)
            canvas.drawText("পণ্যের বিবরণ (Item Name)", 68f, startY, leftHeaderPaint)
            canvas.drawText("স্টক পরিমাণ", 260f, startY, rightHeaderPaint)
            canvas.drawText("মেয়াদ শেষ তারিখ", 365f, startY, centerHeaderPaint)
            canvas.drawText("বর্তমান অবস্থা", 455f, startY, leftHeaderPaint)
            canvas.drawText("ক্রয়মূল্য ($currency)", 553f, startY, rightHeaderPaint)
            return startY + 16f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 146f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(255, 250, 250); style = Paint.Style.FILL }
        val redTextPaint = Paint().apply { isAntiAlias = true; textSize = 9f; color = Color.rgb(220, 38, 38); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val orangeTextPaint = Paint().apply { isAntiAlias = true; textSize = 9f; color = Color.rgb(217, 119, 6); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        products.forEachIndexed { index, p ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "মহাজন বা সরবরাহকারীকে ফেরত/বদল বাবদ মেমো")
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 86f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            canvas.drawText("${index + 1}", 45f, y, centerTextPaint)
            val nameStr = if (p.name.length > 26) p.name.take(24) + ".." else p.name
            canvas.drawText(nameStr, 68f, y, boldPaint)
            canvas.drawText("${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}", 260f, y, rightTextPaint)

            val expDateStr = if (p.expiryDate > 0) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(p.expiryDate))
            } else "N/A"
            canvas.drawText(expDateStr, 365f, y, centerTextPaint)

            val daysRemaining = if (p.expiryDate > 0) {
                ((p.expiryDate - now) / (1000 * 60 * 60 * 24)).toInt()
            } else 0

            val (statusStr, statusPaint) = when {
                daysRemaining < 0 -> Pair("মেয়াদ শেষ! (${-daysRemaining} দিন)", redTextPaint)
                daysRemaining == 0 -> Pair("আজই মেয়াদ শেষ", redTextPaint)
                else -> Pair("$daysRemaining দিন বাকি", orangeTextPaint)
            }
            canvas.drawText(statusStr, 455f, y, statusPaint)

            val costTotal = p.stockQuantity * p.buyPrice
            canvas.drawText("$currency${costTotal.toIntOrNull() ?: costTotal}", 553f, y, rightBoldPaint)

            y += 16f
        }

        // Table Bottom Summary Row
        if (y <= 730f) {
            y += 6f
            val totalRowBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(254, 202, 202); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("সর্বমোট মেয়াদোত্তীর্ণ পণ্যের আর্থিক মূল্য:", 68f, y + 5f, boldPaint)
            canvas.drawText("$currency${totalCost.toIntOrNull() ?: totalCost}", 553f, y + 5f, redTextPaint)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("দোকান প্রতিনিধি", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("সরবরাহকারী / মহাজন স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA মেয়াদোত্তীর্ণ পণ্য বিবরণী • সরবরাহকারী ফেরত মেমো")
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val redRightPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(220, 38, 38)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val totalExpenseAmount = expenses.sumOf { it.amount }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            // Outer Frame
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            val topBarPaint = Paint().apply { color = Color.rgb(220, 38, 38); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 23f), 0f, 0f, topBarPaint)

            var y = 46f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            val badgeBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(254, 202, 202); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(185, 28, 28)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("দোকানের খরচ হিসাব ও ভাউচার খতিয়ান / STORE EXPENSES", 297.5f, y + 2f, badgeText)
            y += 18f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | মোট এন্ট্রি: ${expenses.size} টি | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 14f
                val boxWidth = 255f
                val boxHeight = 36f

                // Entries Count Card
                val b1 = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                val cardLbl = Paint().apply { isAntiAlias = true; textSize = 8.5f; color = Color.rgb(100, 116, 139) }
                canvas.drawText("মোট খরচের ভাউচার সংখ্যা", 44f, y + 14f, cardLbl)
                val blueBold = Paint().apply { isAntiAlias = true; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(30, 41, 59) }
                canvas.drawText("${expenses.size} টি ভাউচার এন্ট্রি", 44f, y + 28f, blueBold)

                // Total Amount Card
                val b2 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(306f, y, 561f, y + boxHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(306f, y, 561f, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("সর্বমোট খরচ বাবদ ব্যয়", 316f, y + 14f, cardLbl)
                val redBold = Paint().apply { isAntiAlias = true; textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(220, 38, 38) }
                canvas.drawText("$currency${totalExpenseAmount.toIntOrNull() ?: totalExpenseAmount}", 316f, y + 28f, redBold)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 11f, 561f, startY + 9f), 4f, 4f, headerBg)
            canvas.drawText("#", 45f, startY, centerHeaderPaint)
            canvas.drawText("তারিখ ও সময়", 68f, startY, leftHeaderPaint)
            canvas.drawText("খরচের খাত / বিবরণ", 185f, startY, leftHeaderPaint)
            canvas.drawText("ক্যাটাগরি", 365f, startY, leftHeaderPaint)
            canvas.drawText("পরিমাণ ($currency)", 553f, startY, rightHeaderPaint)
            return startY + 16f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 146f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(255, 250, 250); style = Paint.Style.FILL }

        expenses.forEachIndexed { index, exp ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "দোকানের খরচ হিসাব ও ভাউচার খতিয়ান")
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 86f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            canvas.drawText("${index + 1}", 45f, y, centerTextPaint)
            val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(exp.timestamp))
            canvas.drawText(dateStr, 68f, y, textPaint)
            val titleStr = if (exp.title.length > 25) exp.title.take(23) + ".." else exp.title
            canvas.drawText(titleStr, 185f, y, boldPaint)
            val catStr = if (exp.category.length > 20) exp.category.take(18) + ".." else exp.category
            canvas.drawText(catStr, 365f, y, textPaint)
            canvas.drawText("- $currency${exp.amount.toIntOrNull() ?: exp.amount}", 553f, y, redRightPaint)

            y += 16f
        }

        // Table Bottom Summary Row
        if (y <= 730f) {
            y += 6f
            val totalRowBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(254, 202, 202); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("সর্বমোট খরচ হিসাব:", 68f, y + 5f, boldPaint)
            canvas.drawText("- $currency${totalExpenseAmount.toIntOrNull() ?: totalExpenseAmount}", 553f, y + 5f, redRightPaint)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("হিসাবরক্ষক / প্রস্তুতকারী", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("স্বত্বাধিকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA দোকান খরচ খতিয়ান • ডিজিটাল ক্যাশ মেমো")
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
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
            color = Color.rgb(21, 128, 61)
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
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(30, 41, 59)
        }
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }

        val totalIncome = entries.filter { it.isAddition }.sumOf { it.amount }
        val totalExpense = entries.filter { !it.isAddition }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            // Outer Frame
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            val topBarPaint = Paint().apply { color = Color.rgb(13, 148, 136); style = Paint.Style.FILL } // Teal top bar
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 23f), 0f, 0f, topBarPaint)

            var y = 46f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            val badgeBg = Paint().apply { color = Color.rgb(240, 253, 250); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(153, 246, 228); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(15, 118, 110)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("দোকানের মূল ক্যাশ খাতা স্টেটমেন্ট ($periodTitle)", 297.5f, y + 2f, badgeText)
            y += 18f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            canvas.drawText("তারিখ: $genDate | মোট এন্ট্রি: ${entries.size} টি | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 14f
                val boxWidth = 168f
                val boxHeight = 36f

                // Cash In Card
                val b1 = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                val cardLbl = Paint().apply { isAntiAlias = true; textSize = 8.5f; color = Color.rgb(100, 116, 139) }
                canvas.drawText("মোট জমা (নগদ ইন)", 42f, y + 14f, cardLbl)
                val greenBold = Paint().apply { isAntiAlias = true; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(21, 128, 61) }
                canvas.drawText("+$currency${totalIncome.toIntOrNull() ?: totalIncome}", 42f, y + 28f, greenBold)

                // Cash Out Card
                val b2 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(214f, y, 214f + boxWidth, y + boxHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(214f, y, 214f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("মোট খরচ / প্রদত্ত", 222f, y + 14f, cardLbl)
                val redBold = Paint().apply { isAntiAlias = true; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(220, 38, 38) }
                canvas.drawText("-$currency${totalExpense.toIntOrNull() ?: totalExpense}", 222f, y + 28f, redBold)

                // Net Cash Balance Card
                val b3 = Paint().apply { color = Color.rgb(240, 253, 250); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(394f, y, 561f, y + boxHeight), 5f, 5f, b3)
                canvas.drawRoundRect(RectF(394f, y, 561f, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("বর্তমান ক্যাশ ব্যালেন্স", 402f, y + 14f, cardLbl)
                val tealBold = Paint().apply { isAntiAlias = true; textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(15, 118, 110) }
                canvas.drawText("$currency${netBalance.toIntOrNull() ?: netBalance}", 402f, y + 28f, tealBold)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 11f, 561f, startY + 9f), 4f, 4f, headerBg)
            canvas.drawText("#", 45f, startY, centerHeaderPaint)
            canvas.drawText("তারিখ ও সময়", 65f, startY, leftHeaderPaint)
            canvas.drawText("বিবরণ ও খাত / ব্যক্তি", 175f, startY, leftHeaderPaint)
            canvas.drawText("জমা ($currency)", 375f, startY, rightHeaderPaint)
            canvas.drawText("প্রদত্ত ($currency)", 465f, startY, rightHeaderPaint)
            canvas.drawText("ব্যালেন্স ($currency)", 553f, startY, rightHeaderPaint)
            return startY + 16f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 146f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }

        entries.forEachIndexed { index, item ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "দোকানের মূল ক্যাশ খাতা স্টেটমেন্ট")
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 86f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            canvas.drawText("${index + 1}", 45f, y, centerTextPaint)
            val dateStr = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
            canvas.drawText(dateStr, 65f, y, textPaint)

            val displayTitle = if (item.title.length > 24) item.title.take(22) + ".." else item.title
            canvas.drawText(displayTitle, 175f, y, boldPaint)

            if (item.isAddition) {
                canvas.drawText("+${item.amount.toIntOrNull() ?: item.amount}", 375f, y, greenTextPaint)
                canvas.drawText("-", 465f, y, centerTextPaint)
            } else {
                canvas.drawText("-", 375f, y, centerTextPaint)
                canvas.drawText("-${item.amount.toIntOrNull() ?: item.amount}", 465f, y, redTextPaint)
            }

            val balStr = "${if (item.runningBalance < 0) "-" else ""}${Math.abs(item.runningBalance).toIntOrNull() ?: Math.abs(item.runningBalance)}"
            canvas.drawText(balStr, 553f, y, balanceTextPaint)

            y += 16f
        }

        // Table Bottom Summary Row
        if (y <= 730f) {
            y += 6f
            val totalRowBg = Paint().apply { color = Color.rgb(240, 253, 250); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(153, 246, 228); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            canvas.drawText("সর্বমোট ক্যাশ বই হিসাব:", 65f, y + 5f, boldPaint)
            canvas.drawText("+$currency${totalIncome.toIntOrNull() ?: totalIncome}", 375f, y + 5f, greenTextPaint)
            canvas.drawText("-$currency${totalExpense.toIntOrNull() ?: totalExpense}", 465f, y + 5f, redTextPaint)
            canvas.drawText("$currency${netBalance.toIntOrNull() ?: netBalance}", 553f, y + 5f, balanceTextPaint)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("হিসাবরক্ষক / ক্যাশিয়ার", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("স্বত্বাধিকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA মাস্টার ক্যাশ খাতা • নির্ভুল হিসাবের বিশ্বস্ত সঙ্গী")
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "CashBook_Statement_$timestamp.pdf")
    }

    /**
     * Generates Low Stock Purchase Order / Reorder Sheet PDF
     */
    fun generateLowStockOrderPdf(
        context: Context,
        shopName: String,
        shopPhone: String = "",
        supplierName: String = "",
        note: String = "",
        items: List<ReorderItem>,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }

        val totalPcs = items.sumOf { it.orderQuantity }
        val totalCost = items.sumOf { it.orderQuantity * it.unitPrice }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            // Outer Document Frame
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            val topBarPaint = Paint().apply { color = Color.rgb(217, 119, 6); style = Paint.Style.FILL } // Amber
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 23f), 0f, 0f, topBarPaint)

            var y = 46f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            val badgeBg = Paint().apply { color = Color.rgb(254, 243, 199); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(140f, y - 13f, 455f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(180, 83, 9)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("পণ্য ক্রয় ও রি-অর্ডার মেমো / PURCHASE REORDER SHEET", 297.5f, y + 2f, badgeText)
            y += 18f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val supText = if (supplierName.isNotBlank()) " | সরবরাহকারী: $supplierName" else ""
            val phoneText = if (shopPhone.isNotBlank()) " | মোবা: $shopPhone" else ""
            canvas.drawText("তারিখ: $genDate$phoneText$supText | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 14f
                val boxWidth = 255f
                val boxHeight = 36f

                // Order Count Card
                val b1 = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                val cardLbl = Paint().apply { isAntiAlias = true; textSize = 8.5f; color = Color.rgb(100, 116, 139) }
                canvas.drawText("মোট অর্ডার তালিকা", 44f, y + 14f, cardLbl)
                val darkBold = Paint().apply { isAntiAlias = true; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(15, 23, 42) }
                canvas.drawText("${items.size} টি পণ্য (${totalPcs.toIntOrNull() ?: totalPcs} পিছ)", 44f, y + 28f, darkBold)

                // Total Cost Card
                val b2 = Paint().apply { color = Color.rgb(254, 243, 199); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(306f, y, 561f, y + boxHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(306f, y, 561f, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("আনুমানিক মোট প্রদেয় বিল", 316f, y + 14f, cardLbl)
                val amberBold = Paint().apply { isAntiAlias = true; textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(180, 83, 9) }
                val totalCostStr = if (totalCost % 1.0 == 0.0) totalCost.toInt().toString() else "%.2f".format(totalCost)
                canvas.drawText("$currency$totalCostStr", 316f, y + 28f, amberBold)

                if (note.isNotBlank()) {
                    y += 42f
                    val notePaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 8.5f
                        color = Color.rgb(120, 53, 15)
                    }
                    val nText = if (note.length > 90) note.take(88) + ".." else note
                    canvas.drawText("বিশেষ নোট: $nText", 44f, y, notePaint)
                }
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 11f, 561f, startY + 9f), 4f, 4f, headerBg)
            canvas.drawText("#", 45f, startY, centerHeaderPaint)
            canvas.drawText("পণ্যের নাম ও বিবরণ", 65f, startY, leftHeaderPaint)
            canvas.drawText("ক্যাটাগরি", 240f, startY, leftHeaderPaint)
            canvas.drawText("বর্তমান স্টক", 345f, startY, rightHeaderPaint)
            canvas.drawText("অর্ডার পরিমাণ", 430f, startY, rightHeaderPaint)
            canvas.drawText("দর ($currency)", 495f, startY, rightHeaderPaint)
            canvas.drawText("মোট ($currency)", 553f, startY, rightHeaderPaint)
            return startY + 16f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = if (note.isNotBlank()) 156f else 146f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(255, 251, 235); style = Paint.Style.FILL }

        for ((index, item) in items.withIndex()) {
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "পণ্য ক্রয় ও রি-অর্ডার মেমো")
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 86f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            val p = item.product
            val itemTotal = item.orderQuantity * item.unitPrice
            canvas.drawText("${index + 1}", 45f, y, centerTextPaint)
            val nameStr = if (p.name.length > 24) p.name.take(22) + ".." else p.name
            canvas.drawText(nameStr, 65f, y, boldPaint)
            val catStr = if (p.category.length > 15) p.category.take(13) + ".." else p.category
            canvas.drawText(catStr, 240f, y, textPaint)
            canvas.drawText("${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}", 345f, y, rightTextPaint)

            val orderQtyStr = "${item.orderQuantity.toIntOrNull() ?: item.orderQuantity} ${p.unit}"
            canvas.drawText(orderQtyStr, 430f, y, rightBoldPaint)

            canvas.drawText("${item.unitPrice.toIntOrNull() ?: item.unitPrice}", 495f, y, rightTextPaint)
            val itemTotalStr = if (itemTotal % 1.0 == 0.0) itemTotal.toInt().toString() else "%.1f".format(itemTotal)
            canvas.drawText(itemTotalStr, 553f, y, rightBoldPaint)

            y += 16f
        }

        // Table Bottom Summary Row
        if (y <= 730f) {
            y += 6f
            val totalRowBg = Paint().apply { color = Color.rgb(254, 243, 199); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(253, 230, 138); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            val totalCostStr = if (totalCost % 1.0 == 0.0) totalCost.toInt().toString() else "%.2f".format(totalCost)
            canvas.drawText("সর্বমোট অর্ডার: ${totalPcs.toIntOrNull() ?: totalPcs} পিছ (${items.size} পণ্য)", 65f, y + 5f, boldPaint)
            canvas.drawText("মোট প্রদেয় বিল: $currency$totalCostStr", 553f, y + 5f, rightBoldPaint)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("দোকানদারের স্বাক্ষর", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("সরবরাহকারী / ডিলারের স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA রি-অর্ডার মেমো • সরবরাহকারী অর্ডার ভাউচার")
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "Stock_Reorder_$timestamp.pdf")
    }

    /**
     * Generates a final Stock-In / Received Order Memo PDF:
     * - Shows received items with received quantities and buy rates
     * - Clearly marks items that were not found / missing as "পাওয়া যায়নি" (NOT FOUND)
     */
    fun generateReceivedOrderPdf(
        context: Context,
        order: com.example.data.model.PendingOrder,
        shopName: String,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
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
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        val notFoundPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(220, 38, 38)
            textAlign = Paint.Align.CENTER
        }
        val notFoundBgPaint = Paint().apply {
            color = Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        val rightTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.RIGHT
        }
        val rightBoldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }

        val receivedItems = order.items.filter { !it.isNotFound && it.receivedQuantity > 0 }
        val notFoundItems = order.items.filter { it.isNotFound || it.receivedQuantity <= 0 }
        val totalReceivedPcs = receivedItems.sumOf { it.receivedQuantity }
        val totalReceivedCost = receivedItems.sumOf { it.receivedQuantity * it.buyPrice }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            // Outer Frame
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            val topBarPaint = Paint().apply { color = Color.rgb(37, 99, 235); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 23f), 0f, 0f, topBarPaint)

            var y = 46f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            val badgeBg = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(191, 219, 254); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(130f, y - 13f, 465f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(130f, y - 13f, 465f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(29, 78, 216)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("পণ্য স্টক-ইন ও প্রাপ্তি চালান / STOCK-IN & RECEIPT VOUCHER", 297.5f, y + 2f, badgeText)
            y += 18f

            val genDate = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.receivedTimestamp ?: System.currentTimeMillis()))
            val supText = if (order.supplierName.isNotBlank()) " | সরবরাহকারী: ${order.supplierName}" else ""
            canvas.drawText("অর্ডার #${order.orderNumber} | তারিখ: $genDate$supText | পৃষ্ঠা: $pageNumber", 297.5f, y, subPaint)

            if (drawSummaryBox) {
                y += 14f
                val boxWidth = 168f
                val boxHeight = 36f

                // Received Card
                val b1 = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, b1)
                canvas.drawRoundRect(RectF(34f, y, 34f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                val cardLbl = Paint().apply { isAntiAlias = true; textSize = 8.5f; color = Color.rgb(100, 116, 139) }
                canvas.drawText("প্রাপ্ত পণ্য ও পরিমাণ", 42f, y + 14f, cardLbl)
                val greenBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(21, 128, 61) }
                canvas.drawText("${receivedItems.size} পণ্য (${totalReceivedPcs.toIntOrNull() ?: totalReceivedPcs} পিছ)", 42f, y + 28f, greenBold)

                // Not Found Card
                val b2 = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(214f, y, 214f + boxWidth, y + boxHeight), 5f, 5f, b2)
                canvas.drawRoundRect(RectF(214f, y, 214f + boxWidth, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("ঘাটতি / পাওয়া যায়নি", 222f, y + 14f, cardLbl)
                val redBold = Paint().apply { isAntiAlias = true; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(220, 38, 38) }
                val nfText = if (notFoundItems.isNotEmpty()) "${notFoundItems.size} টি পণ্য পাওয়া যায়নি" else "কোন ঘাটতি নেই"
                canvas.drawText(nfText, 222f, y + 28f, redBold)

                // Total Cost Card
                val b3 = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
                canvas.drawRoundRect(RectF(394f, y, 561f, y + boxHeight), 5f, 5f, b3)
                canvas.drawRoundRect(RectF(394f, y, 561f, y + boxHeight), 5f, 5f, linePaint)
                canvas.drawText("মোট ক্রয় বিল", 402f, y + 14f, cardLbl)
                val blueBold = Paint().apply { isAntiAlias = true; textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(37, 99, 235) }
                val totalCostStr = if (totalReceivedCost % 1.0 == 0.0) totalReceivedCost.toInt().toString() else "%.2f".format(totalReceivedCost)
                canvas.drawText("$currency$totalCostStr", 402f, y + 28f, blueBold)

                if (order.orderNote.isNotBlank()) {
                    y += 42f
                    val notePaint = Paint().apply {
                        isAntiAlias = true
                        textSize = 8.5f
                        color = Color.rgb(30, 58, 138)
                    }
                    val nText = if (order.orderNote.length > 90) order.orderNote.take(88) + ".." else order.orderNote
                    canvas.drawText("অর্ডার নোট: $nText", 42f, y, notePaint)
                }
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val headerBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 11f, 561f, startY + 9f), 4f, 4f, headerBg)
            canvas.drawText("#", 45f, startY, centerHeaderPaint)
            canvas.drawText("পণ্যের বিবরণ", 65f, startY, leftHeaderPaint)
            canvas.drawText("অর্ডার", 260f, startY, rightHeaderPaint)
            canvas.drawText("প্রাপ্ত (পিছ)", 335f, startY, rightHeaderPaint)
            canvas.drawText("ক্রয় দর ($currency)", 415f, startY, rightHeaderPaint)
            canvas.drawText("মোট মূল্য ($currency)", 485f, startY, rightHeaderPaint)
            canvas.drawText("অবস্থা", 535f, startY, centerHeaderPaint)
            return startY + 16f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = if (order.orderNote.isNotBlank()) 156f else 146f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        val greenTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(21, 128, 61)
            textAlign = Paint.Align.CENTER
        }

        for ((index, item) in order.items.withIndex()) {
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "পণ্য স্টক-ইন ও প্রাপ্তি চালান মেমো")
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 86f
                y = drawTableHeader(y)
            }

            val isMissing = item.isNotFound || item.receivedQuantity <= 0
            if (isMissing) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), notFoundBgPaint)
            } else if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            canvas.drawText("${index + 1}", 45f, y, centerTextPaint)
            val nameStr = if (item.productName.length > 24) item.productName.take(22) + ".." else item.productName
            canvas.drawText(nameStr, 65f, y, if (isMissing) notFoundPaint else boldPaint)

            // Ordered quantity
            val orderedQtyStr = "${item.orderedQuantity.toIntOrNull() ?: item.orderedQuantity} ${item.unit}"
            canvas.drawText(orderedQtyStr, 260f, y, rightTextPaint)

            // Received quantity
            if (isMissing) {
                canvas.drawText("0 ${item.unit}", 335f, y, rightBoldPaint)
                val buyPriceStr = if (item.buyPrice > 0) "${item.buyPrice.toIntOrNull() ?: item.buyPrice}" else "00"
                canvas.drawText(buyPriceStr, 415f, y, rightTextPaint)
                canvas.drawText("0", 485f, y, rightTextPaint)
                canvas.drawText("পাওয়া যায়নি", 535f, y, notFoundPaint)
            } else {
                val rcvQtyStr = "${item.receivedQuantity.toIntOrNull() ?: item.receivedQuantity} ${item.unit}"
                canvas.drawText(rcvQtyStr, 335f, y, rightBoldPaint)
                val buyPriceStr = if (item.buyPrice > 0) "${item.buyPrice.toIntOrNull() ?: item.buyPrice}" else "00"
                canvas.drawText(buyPriceStr, 415f, y, rightTextPaint)
                val itemTotal = item.receivedQuantity * item.buyPrice
                val itemTotalStr = if (itemTotal % 1.0 == 0.0) itemTotal.toInt().toString() else "%.1f".format(itemTotal)
                canvas.drawText(itemTotalStr, 485f, y, rightBoldPaint)
                canvas.drawText("প্রাপ্ত", 535f, y, greenTextPaint)
            }

            y += 16f
        }

        // Table Bottom Summary Row
        if (y <= 730f) {
            y += 6f
            val grandTotalPaint = Paint().apply { color = Color.rgb(239, 246, 255); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, grandTotalPaint)
            val doubleLine = Paint().apply { color = Color.rgb(191, 219, 254); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            val totalCostStr = if (totalReceivedCost % 1.0 == 0.0) totalReceivedCost.toInt().toString() else "%.2f".format(totalReceivedCost)
            canvas.drawText("স্টক-ইন মোট: ${totalReceivedPcs.toIntOrNull() ?: totalReceivedPcs} পিছ (${receivedItems.size} পণ্য)", 65f, y + 5f, boldPaint)
            canvas.drawText("মোট ক্রয় বিল: $currency$totalCostStr", 485f, y + 5f, rightBoldPaint)
            y += 24f
        }

        // Signatures Block
        val sigY = 780f
        val sigLinePaint = Paint().apply { color = Color.rgb(148, 163, 184); strokeWidth = 1f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.rgb(71, 85, 105)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawLine(44f, sigY, 180f, sigY, sigLinePaint)
        canvas.drawText("গ্রহীতা / দোকানদারের স্বাক্ষর", 112f, sigY + 13f, sigTextPaint)

        canvas.drawLine(425f, sigY, 551f, sigY, sigLinePaint)
        canvas.drawText("ডেলিভারি প্রদানকারীর স্বাক্ষর", 488f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "NAFI KHATA স্টক-ইন রিসিট চালান • মেমো প্রিন্ট")
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "StockIn_Receipt_${order.orderNumber}_$timestamp.pdf")
    }

    /**
     * Generates an authentic shop memo PDF for cash additions made to the shop's master drawer
     * (নিজের ক্যাশ থেকে জমা অথবা কারো কাছ থেকে এনে যুক্ত করা টাকার খতিয়ান)
     */
    fun generateAddedCashHistoryPdf(
        context: Context,
        shopName: String,
        shopPhone: String = "",
        periodTitle: String = "সকল সময়",
        entries: List<CashAdditionItem>,
        currency: String = "৳"
    ): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(30, 41, 59)
        }
        val centerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(30, 41, 59)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val greenTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(16, 185, 129)
            textAlign = Paint.Align.RIGHT
        }
        val balanceTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
            textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        val framePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val leftHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        val centerHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val rightHeaderPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }

        val totalAdded = entries.sumOf { it.amount }
        val ownAdded = entries.filter { it.sourceCategory == "OWN" }.sumOf { it.amount }
        val personAdded = entries.filter { it.sourceCategory == "PERSON" }.sumOf { it.amount }
        val otherAdded = entries.filter { it.sourceCategory == "OTHER" }.sumOf { it.amount }

        fun drawHeaderAndSummary(drawSummaryBox: Boolean) {
            // Outer Frame
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 826f), 6f, 6f, framePaint)

            // Top accent bar (Emerald Green for Master Cash Deposits)
            val topBarPaint = Paint().apply { color = Color.rgb(5, 150, 105); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(24f, 16f, 571f, 23f), 0f, 0f, topBarPaint)

            var y = 46f
            canvas.drawText(shopName, 297.5f, y, titlePaint)
            y += 18f

            val badgeBg = Paint().apply { color = Color.rgb(236, 253, 245); style = Paint.Style.FILL }
            val badgeBorder = Paint().apply { color = Color.rgb(167, 243, 208); style = Paint.Style.STROKE; strokeWidth = 1f }
            canvas.drawRoundRect(RectF(125f, y - 13f, 470f, y + 9f), 11f, 11f, badgeBg)
            canvas.drawRoundRect(RectF(125f, y - 13f, 470f, y + 9f), 11f, 11f, badgeBorder)

            val badgeText = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(4, 120, 87)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("দোকানের মূল ক্যাশে টাকা যুক্ত করার খতিয়ান মেমো", 297.5f, y + 2f, badgeText)
            y += 19f

            val metaPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                color = Color.rgb(100, 116, 139)
                textAlign = Paint.Align.CENTER
            }
            val metaStr = if (shopPhone.isNotBlank()) "মোবাইল: $shopPhone • সময়কাল: $periodTitle" else "সময়কাল: $periodTitle"
            canvas.drawText(metaStr, 297.5f, y, metaPaint)

            if (drawSummaryBox) {
                // 3 or 4 Summary Metric Cards
                val boxBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                val boxStroke = Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE; strokeWidth = 0.8f }
                val summaryRect = RectF(34f, 88f, 561f, 138f)
                canvas.drawRoundRect(summaryRect, 6f, 6f, boxBg)
                canvas.drawRoundRect(summaryRect, 6f, 6f, boxStroke)

                // Dividers
                canvas.drawLine(165f, 92f, 165f, 134f, linePaint)
                canvas.drawLine(300f, 92f, 300f, 134f, linePaint)
                canvas.drawLine(435f, 92f, 435f, 134f, linePaint)

                val labelPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 7.5f
                    color = Color.rgb(100, 116, 139)
                    textAlign = Paint.Align.CENTER
                }
                val valPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(5, 150, 105)
                    textAlign = Paint.Align.CENTER
                }
                val blueValPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(2, 132, 199)
                    textAlign = Paint.Align.CENTER
                }
                val amberValPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(217, 119, 6)
                    textAlign = Paint.Align.CENTER
                }
                val countValPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = Color.rgb(71, 85, 105)
                    textAlign = Paint.Align.CENTER
                }

                // Col 1: সর্বমোট যুক্ত টাকা
                canvas.drawText("মোট যুক্ত ক্যাশ", 99f, 105f, labelPaint)
                canvas.drawText("+$currency ${totalAdded.toIntOrNull() ?: totalAdded}", 99f, 125f, valPaint)

                // Col 2: নিজের ক্যাশ থেকে
                canvas.drawText("নিজের ক্যাশ থেকে", 232f, 105f, labelPaint)
                canvas.drawText("+$currency ${ownAdded.toIntOrNull() ?: ownAdded}", 232f, 125f, blueValPaint)

                // Col 3: কারো কাছ থেকে ধার / আনা
                canvas.drawText("কারো কাছ থেকে আনা / ধার", 367f, 105f, labelPaint)
                canvas.drawText("+$currency ${personAdded.toIntOrNull() ?: personAdded}", 367f, 125f, amberValPaint)

                // Col 4: মোট লেনদেন সংখ্যা
                canvas.drawText("মোট লেনদেন সংখ্যা", 498f, 105f, labelPaint)
                canvas.drawText("${entries.size} বার", 498f, 125f, countValPaint)
            }
        }

        fun drawTableHeader(startY: Float): Float {
            val thBg = Paint().apply { color = Color.rgb(30, 41, 59); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, startY - 10f, 561f, startY + 12f), 4f, 4f, thBg)
            canvas.drawText("#", 45f, startY + 4f, centerHeaderPaint)
            canvas.drawText("তারিখ ও সময়", 65f, startY + 4f, leftHeaderPaint)
            canvas.drawText("জমার উৎস", 155f, startY + 4f, leftHeaderPaint)
            canvas.drawText("যার কাছ থেকে আনা", 240f, startY + 4f, leftHeaderPaint)
            canvas.drawText("বিবরণ / নোট", 340f, startY + 4f, leftHeaderPaint)
            canvas.drawText("যুক্ত টাকা ($currency)", 485f, startY + 4f, rightHeaderPaint)
            canvas.drawText("ব্যালেন্স ($currency)", 555f, startY + 4f, rightHeaderPaint)
            return startY + 20f
        }

        drawHeaderAndSummary(drawSummaryBox = true)
        var y = 146f
        y = drawTableHeader(y)

        val rowBgAlt = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }

        entries.forEachIndexed { index, item ->
            if (y > 745f) {
                drawSponsorFooter(canvas, 812f, "মূল ক্যাশে টাকা জমার খতিয়ান মেমো")
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeaderAndSummary(drawSummaryBox = false)
                y = 86f
                y = drawTableHeader(y)
            }

            if (index % 2 == 1) {
                canvas.drawRect(RectF(34f, y - 10f, 561f, y + 6f), rowBgAlt)
            }
            canvas.drawLine(34f, y + 6f, 561f, y + 6f, linePaint)

            canvas.drawText("${index + 1}", 45f, y, centerTextPaint)
            val dateStr = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
            canvas.drawText(dateStr, 65f, y, textPaint)

            val sourceText = when (item.sourceCategory) {
                "OWN" -> "নিজের ক্যাশ"
                "PERSON" -> "ধার / আনা"
                else -> "অন্যান্য"
            }
            val sourcePaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = when (item.sourceCategory) {
                    "OWN" -> Color.rgb(2, 132, 199)
                    "PERSON" -> Color.rgb(217, 119, 6)
                    else -> Color.rgb(100, 116, 139)
                }
            }
            canvas.drawText(sourceText, 155f, y, sourcePaint)

            val personDisplay = if (item.personName.isNotBlank()) {
                if (item.personName.length > 15) item.personName.take(13) + ".." else item.personName
            } else if (item.sourceCategory == "OWN") {
                "ব্যক্তিগত"
            } else {
                "-"
            }
            canvas.drawText(personDisplay, 240f, y, boldPaint)

            val noteDisplay = if (item.note.isNotBlank()) {
                if (item.note.length > 20) item.note.take(18) + ".." else item.note
            } else {
                "-"
            }
            canvas.drawText(noteDisplay, 340f, y, textPaint)

            canvas.drawText("+${item.amount.toIntOrNull() ?: item.amount}", 485f, y, greenTextPaint)

            val balStr = "${item.balanceAfter.toIntOrNull() ?: item.balanceAfter}"
            canvas.drawText(balStr, 555f, y, balanceTextPaint)

            y += 16f
        }

        // Table Bottom Summary Row
        if (y <= 730f) {
            y += 6f
            val totalRowBg = Paint().apply { color = Color.rgb(236, 253, 245); style = Paint.Style.FILL }
            canvas.drawRoundRect(RectF(34f, y - 8f, 561f, y + 14f), 4f, 4f, totalRowBg)
            val doubleLine = Paint().apply { color = Color.rgb(167, 243, 208); strokeWidth = 1f }
            canvas.drawLine(34f, y - 8f, 561f, y - 8f, doubleLine)
            canvas.drawLine(34f, y + 14f, 561f, y + 14f, doubleLine)

            val summaryTotalText = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(4, 120, 87)
            }
            canvas.drawText("সর্বমোট ক্যাশে যুক্ত করা টাকার পরিমাণ:", 65f, y + 6f, summaryTotalText)

            val bigGreenTotal = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(4, 120, 87)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("+$currency ${totalAdded.toIntOrNull() ?: totalAdded}", 485f, y + 6f, bigGreenTotal)
        }

        // Signature Blocks
        val sigY = 780f
        val sigLine = Paint().apply { color = Color.rgb(203, 213, 225); strokeWidth = 0.9f }
        val sigTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.rgb(100, 116, 139)
            textAlign = Paint.Align.CENTER
        }

        // Left Signature: হিসাবরক্ষক / ক্যাশিয়ার
        canvas.drawLine(55f, sigY, 175f, sigY, sigLine)
        canvas.drawText("হিসাবরক্ষক / ক্যাশিয়ারের স্বাক্ষর", 115f, sigY + 13f, sigTextPaint)

        // Right Signature: দোকানদার / স্বত্বাধিকারী
        canvas.drawLine(415f, sigY, 545f, sigY, sigLine)
        canvas.drawText("দোকানদার / স্বত্বাধিকারীর স্বাক্ষর", 480f, sigY + 13f, sigTextPaint)

        drawSponsorFooter(canvas, 812f, "দোকানের মূল ক্যাশে টাকা জমার খতিয়ান মেমো")
        pdfDocument.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return savePdfToFile(context, pdfDocument, "Added_Cash_History_$timestamp.pdf")
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

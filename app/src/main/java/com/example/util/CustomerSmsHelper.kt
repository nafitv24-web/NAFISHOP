package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.components.toIntOrNull

object CustomerSmsHelper {

    const val SPONSOR_HEADER = "সৌজন্যে নাফি এন্ড নাজমুল টেলিকম"
    const val SPONSOR_URL = "https://nfiptvserver.blogspot.com/"
    const val SPONSOR_FOOTER = "সৌজন্যে নাফি এন্ড নাজমুল টেলিকম\nhttps://nfiptvserver.blogspot.com/"

    /**
     * Builds the complete customer message containing:
     * - Shop Name & Header
     * - Customer Name
     * - Purchased Products breakdown with quantities & prices
     * - Sale Total & Cash Paid
     * - Today's New Due amount
     * - Previous Due (পূর্বের বাকি)
     * - Total Outstanding Due (সর্বমোট বর্তমান বকেয়া = পূর্বের বাকি + আজকের বাকি)
     * - Sponsor Footer with link
     */
    fun buildDueSaleMessage(
        shopName: String,
        shopPhone: String,
        customerName: String,
        invoiceNo: String?,
        purchasedItemsSummary: String,
        saleTotal: Double,
        paidAmount: Double,
        todayNewDue: Double,
        previousDue: Double,
        totalCurrentDue: Double,
        currency: String = "৳"
    ): String {
        return buildString {
            appendLine("🏪 $shopName")
            appendLine("প্রিয় $customerName,")
            if (purchasedItemsSummary.isNotBlank()) {
                appendLine("🛍️ আজকের ক্রয়কৃত পণ্য:")
                appendLine(purchasedItemsSummary)
            }
            if (!invoiceNo.isNullOrBlank()) {
                appendLine("মেমো নং: $invoiceNo")
            }
            appendLine("মোট বিল: $currency${saleTotal.toIntOrNull() ?: saleTotal}")
            if (paidAmount > 0) {
                appendLine("নগদ পরিশোধ: $currency${paidAmount.toIntOrNull() ?: paidAmount}")
            }
            if (todayNewDue > 0) {
                appendLine("আজকের নতুন বাকি: $currency${todayNewDue.toIntOrNull() ?: todayNewDue}")
            }
            appendLine("-------------------------")
            appendLine("পূর্বের বাকি ছিল: $currency${previousDue.toIntOrNull() ?: previousDue}")
            appendLine("সর্বমোট বর্তমান বকেয়া: $currency${totalCurrentDue.toIntOrNull() ?: totalCurrentDue}")
            appendLine("-------------------------")
            appendLine("অনুগ্রহ করে সময়মতো পরিশোধ করুন। ধন্যবাদ!")
            if (shopPhone.isNotBlank()) {
                appendLine("দোকান যোগাযোগ: $shopPhone")
            }
            appendLine("-------------------------")
            appendLine(SPONSOR_FOOTER)
        }
    }

    /**
     * Builds SMS for direct ledger adjustments (Credit given or Payment received)
     */
    fun buildLedgerTransactionMessage(
        shopName: String,
        shopPhone: String,
        customerName: String,
        type: String, // "GIVEN" (বাকি) or "COLLECTED" (জমা)
        amount: Double,
        note: String,
        previousDue: Double,
        totalCurrentDue: Double,
        currency: String = "৳"
    ): String {
        val isGiven = type == "GIVEN"
        return buildString {
            appendLine("🏪 $shopName")
            appendLine("প্রিয় $customerName,")
            if (isGiven) {
                appendLine("আজকের নতুন বাকি: $currency${amount.toIntOrNull() ?: amount}")
            } else {
                appendLine("আজকের জমা আদায়: $currency${amount.toIntOrNull() ?: amount}")
            }
            if (note.isNotBlank()) {
                appendLine("বিবরণ: $note")
            }
            appendLine("-------------------------")
            appendLine("পূর্বের বাকি ছিল: $currency${previousDue.toIntOrNull() ?: previousDue}")
            appendLine("সর্বমোট বর্তমান বকেয়া: $currency${totalCurrentDue.toIntOrNull() ?: totalCurrentDue}")
            appendLine("-------------------------")
            if (isGiven) {
                appendLine("সময়মতো পরিশোধের অনুরোধ করা হলো। ধন্যবাদ!")
            } else {
                appendLine("টাকা পরিশোধের জন্য আন্তরিক ধন্যবাদ!")
            }
            if (shopPhone.isNotBlank()) {
                appendLine("যোগাযোগ: $shopPhone")
            }
            appendLine("-------------------------")
            appendLine(SPONSOR_FOOTER)
        }
    }

    /**
     * Send direct SMS prefilled with customer phone and text message
     */
    fun sendDirectSms(context: Context, phone: String, message: String) {
        try {
            val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
            val intent = if (cleanPhone.isNotBlank()) {
                Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$cleanPhone")
                    putExtra("sms_body", message)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareFallback(context, message)
        }
    }

    /**
     * Send WhatsApp message with prefilled customer phone and text message
     */
    fun sendWhatsAppMessage(context: Context, phone: String, message: String) {
        try {
            val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
            val formattedPhone = when {
                cleanPhone.startsWith("880") -> cleanPhone
                cleanPhone.startsWith("0") -> "88$cleanPhone"
                cleanPhone.length == 10 && cleanPhone.startsWith("1") -> "880$cleanPhone"
                cleanPhone.isNotBlank() -> "880$cleanPhone"
                else -> ""
            }
            val uri = if (formattedPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareFallback(context, message)
        }
    }

    /**
     * Fallback generic share dialog
     */
    fun shareFallback(context: Context, message: String, title: String = "কাস্টমারকে মেসেজ পাঠান") {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "মেসেজ ওপেন করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }
}

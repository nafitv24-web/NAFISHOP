package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Standard garment types requested:
 * কামিজ, সেলোয়ার, বাচ্চাদের ফ্রক, পেটিকোট, ব্লাউজ, পাঞ্জাবি, শার্ট, প্যান্ট ইত্যাদি।
 */
enum class GarmentType(
    val id: String,
    val bnName: String,
    val enName: String,
    val category: String = "LADIES" // LADIES, GENTS, KIDS, CUSTOM
) {
    KAMIZ("KAMIZ", "কামিজ", "Kamiz / Kurti", "LADIES"),
    SALWAR("SALWAR", "সেলোয়ার", "Salwar / Pajama", "LADIES"),
    FROCK("FROCK", "বাচ্চাদের ফ্রক", "Children's Frock", "KIDS"),
    PETTICOAT("PETTICOAT", "পেটিকোট", "Petticoat", "LADIES"),
    BLOUSE("BLOUSE", "ব্লাউজ", "Blouse", "LADIES"),
    PANJABI("PANJABI", "পাঞ্জাবি", "Panjabi / Kurta", "GENTS"),
    SHIRT("SHIRT", "শার্ট", "Shirt", "GENTS"),
    PANT("PANT", "প্যান্ট", "Pant / Trouser", "GENTS"),
    MAXI("MAXI", "ম্যাক্সি / নাইটি", "Maxi / Nighty", "LADIES"),
    BURQA("BURQA", "বোরকা / আবায়া", "Burqa / Abaya", "LADIES"),
    OTHER("OTHER", "অন্যান্য পোশাক", "Other / Custom", "CUSTOM");

    companion object {
        fun fromId(id: String): GarmentType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}

/**
 * Definition of a measurement field (Label in Bengali & English, placeholder hint)
 */
data class MeasurementFieldDef(
    val key: String,
    val bnLabel: String,
    val enLabel: String,
    val hint: String = "ইঞ্চি",
    val defaultValue: String = ""
)

object TailorMeasurementTemplates {
    // 1. কামিজ (Kamiz)
    val kamizFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ৩৮\""),
        MeasurementFieldDef("chest", "বডি / ছাতি", "Body / Chest", "উদাঃ ৩৬\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ৩২\""),
        MeasurementFieldDef("hip", "হিপ", "Hip", "উদাঃ ৩৮\""),
        MeasurementFieldDef("shoulder", "পুট / কাঁধ", "Shoulder", "উদাঃ ১৪\""),
        MeasurementFieldDef("sleeve_length", "হাতার লম্বা", "Sleeve Length", "উদাঃ ১৮\""),
        MeasurementFieldDef("sleeve_opening", "হাতার মোহরী", "Sleeve Opening", "উদাঃ ৯\""),
        MeasurementFieldDef("front_neck", "সামনের গলা", "Front Neck", "উদাঃ ৬\""),
        MeasurementFieldDef("back_neck", "পেছনের গলা", "Back Neck", "উদাঃ ৪.৫\""),
        MeasurementFieldDef("side_slit", "সাইড ফাড়া", "Side Slit", "উদাঃ ২১\""),
        MeasurementFieldDef("gher", "ঘের", "Bottom Flare", "উদাঃ ২৪\"")
    )

    // 2. সেলোয়ার (Salwar / Pajama)
    val salwarFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ৩৮\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ৩৬\""),
        MeasurementFieldDef("crotch", "হাই / আসন", "Crotch / High", "উদাঃ ১৫\""),
        MeasurementFieldDef("thigh", "রান / থাই", "Thigh", "উদাঃ ২৫\""),
        MeasurementFieldDef("ankle", "মুহুরি / পায়ের মোহরী", "Ankle Opening", "উদাঃ ১২\""),
        MeasurementFieldDef("belt", "বেল্ট / ইলাস্টিক", "Belt / Elastic", "উদাঃ ৮\""),
        MeasurementFieldDef("kuchi", "কুচি / পকেট", "Pleats / Pocket", "উদাঃ নরমাল কুচি")
    )

    // 3. বাচ্চাদের ফ্রক (Children's Frock)
    val frockFields = listOf(
        MeasurementFieldDef("child_age", "বাচ্চার বয়স / সাইজ", "Child's Age", "উদাঃ ৪ বছর"),
        MeasurementFieldDef("length", "পুরো লম্বা", "Full Length", "উদাঃ ২৪\""),
        MeasurementFieldDef("body", "বডি / ছাতি", "Body / Chest", "উদাঃ ২২\""),
        MeasurementFieldDef("yoke_length", "বডির লম্বা (ইয়ক)", "Yoke Length", "উদাঃ ৮\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ২০\""),
        MeasurementFieldDef("shoulder", "পুট / কাঁধ", "Shoulder", "উদাঃ ৯.৫\""),
        MeasurementFieldDef("sleeve", "হাতা", "Sleeve", "উদাঃ ৫\""),
        MeasurementFieldDef("gher", "ঘের", "Flare / Gher", "উদাঃ ৩৬\""),
        MeasurementFieldDef("ribbon", "পেছনের ফিতা / বেল্ট", "Ribbon / Tie", "উদাঃ ফিতা থাকবে")
    )

    // 4. পেটিকোট (Petticoat)
    val petticoatFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ৩৬\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ৩৪\""),
        MeasurementFieldDef("hip", "হিপ", "Hip", "উদাঃ ৩৮\""),
        MeasurementFieldDef("gher", "ঘের", "Flare", "উদাঃ ৭৫\""),
        MeasurementFieldDef("koli", "কলি সংখ্যা", "Panels / Koli", "উদাঃ ৬ কলি")
    )

    // 5. ব্লাউজ (Blouse)
    val blouseFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ১৪\""),
        MeasurementFieldDef("chest", "বডি / ছাতি", "Chest", "উদাঃ ৩৬\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ৩০\""),
        MeasurementFieldDef("shoulder", "পুট / কাঁধ", "Shoulder", "উদাঃ ১৩\""),
        MeasurementFieldDef("sleeve_length", "হাতার লম্বা", "Sleeve Length", "উদাঃ ৮\""),
        MeasurementFieldDef("sleeve_opening", "হাতার মোহরী", "Sleeve Opening", "উদাঃ ১১\""),
        MeasurementFieldDef("front_neck", "সামনের গলা", "Front Neck", "উদাঃ ৬.৫\""),
        MeasurementFieldDef("back_neck", "পেছনের গলা", "Back Neck", "উদাঃ ৭\""),
        MeasurementFieldDef("dart_apex", "টিকেন / বাস্ট পয়েন্ট", "Dart / Apex", "উদাঃ ৯.৫\""),
        MeasurementFieldDef("hook", "হুক / বোতাম", "Hook Placement", "উদাঃ সামনে হুক")
    )

    // 6. পাঞ্জাবি (Panjabi)
    val panjabiFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ৪২\""),
        MeasurementFieldDef("chest", "ছাতি", "Chest", "উদাঃ ৪০\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ৩৮\""),
        MeasurementFieldDef("shoulder", "পুট / কাঁধ", "Shoulder", "উদাঃ ১৮\""),
        MeasurementFieldDef("sleeve_length", "হাতার লম্বা", "Sleeve Length", "উদাঃ ২৪\""),
        MeasurementFieldDef("collar", "কলার", "Collar", "উদাঃ ১৬\""),
        MeasurementFieldDef("gher", "ঘের", "Bottom Flare", "উদাঃ ২৮\"")
    )

    // 7. শার্ট (Shirt)
    val shirtFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ২৯\""),
        MeasurementFieldDef("chest", "ছাতি", "Chest", "উদাঃ ৩৯\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ৩৬\""),
        MeasurementFieldDef("shoulder", "শোল্ডার", "Shoulder", "উদাঃ ১৭.৫\""),
        MeasurementFieldDef("sleeve_length", "হাতার লম্বা", "Sleeve Length", "উদাঃ ২৩.৫\""),
        MeasurementFieldDef("collar", "কলার", "Collar", "উদাঃ ১৫.৫\""),
        MeasurementFieldDef("cuff", "কাফ মোহরী", "Cuff Opening", "উদাঃ ৯\"")
    )

    // 8. প্যান্ট (Pant)
    val pantFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ৩৯\""),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ ৩২\""),
        MeasurementFieldDef("hip", "হিপ", "Hip", "উদাঃ ৩৮\""),
        MeasurementFieldDef("crotch", "হাই", "Crotch", "উদাঃ ২৩\""),
        MeasurementFieldDef("thigh", "থাই / রান", "Thigh", "উদাঃ ২২\""),
        MeasurementFieldDef("bottom", "পায়ের মোহরী", "Bottom Opening", "উদাঃ ১৪\"")
    )

    // 9. ম্যাক্সি / নাইটি (Maxi / Nighty)
    val maxiFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ৫২\""),
        MeasurementFieldDef("chest", "বডি / ছাতি", "Body / Chest", "উদাঃ ৪২\""),
        MeasurementFieldDef("shoulder", "পুট", "Shoulder", "উদাঃ ১৫\""),
        MeasurementFieldDef("sleeve_length", "হাতার লম্বা", "Sleeve Length", "উদাঃ ৯\""),
        MeasurementFieldDef("sleeve_opening", "হাতার মোহরী", "Sleeve Opening", "উদাঃ ১২\""),
        MeasurementFieldDef("gher", "ঘের", "Flare", "উদাঃ ৮০\"")
    )

    // 10. বোরকা / আবায়া (Burqa / Abaya)
    val burqaFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ ৫৪\""),
        MeasurementFieldDef("chest", "বডি", "Body", "উদাঃ ৪৪\""),
        MeasurementFieldDef("shoulder", "পুট", "Shoulder", "উদাঃ ১৬\""),
        MeasurementFieldDef("sleeve_length", "হাতার লম্বা", "Sleeve Length", "উদাঃ ২৩\""),
        MeasurementFieldDef("sleeve_opening", "কাফ / মোহরী", "Cuff / Opening", "উদাঃ ১০\""),
        MeasurementFieldDef("gher", "ঘের", "Flare", "উদাঃ ১০০\"")
    )

    // 11. অন্যান্য / কাস্টম (Other / Custom)
    val otherFields = listOf(
        MeasurementFieldDef("length", "লম্বা", "Length", "উদাঃ মাপ লিখুন"),
        MeasurementFieldDef("body", "বডি / ছাতি", "Body / Chest", "উদাঃ মাপ লিখুন"),
        MeasurementFieldDef("waist", "কোমর", "Waist", "উদাঃ মাপ লিখুন"),
        MeasurementFieldDef("shoulder", "পুট / কাঁধ", "Shoulder", "উদাঃ মাপ লিখুন"),
        MeasurementFieldDef("sleeve", "হাতা", "Sleeve", "উদাঃ মাপ লিখুন"),
        MeasurementFieldDef("extra", "অন্যান্য মাপ", "Extra Details", "উদাঃ বিশেষ মাপ")
    )

    fun getFieldsForGarmentType(type: GarmentType): List<MeasurementFieldDef> {
        return when (type) {
            GarmentType.KAMIZ -> kamizFields
            GarmentType.SALWAR -> salwarFields
            GarmentType.FROCK -> frockFields
            GarmentType.PETTICOAT -> petticoatFields
            GarmentType.BLOUSE -> blouseFields
            GarmentType.PANJABI -> panjabiFields
            GarmentType.SHIRT -> shirtFields
            GarmentType.PANT -> pantFields
            GarmentType.MAXI -> maxiFields
            GarmentType.BURQA -> burqaFields
            GarmentType.OTHER -> otherFields
        }
    }
}

/**
 * An individual garment inside an order, e.g. 1x কামিজ with specific measurements
 */
data class GarmentMeasurementItem(
    val garmentType: String = GarmentType.KAMIZ.name,
    val garmentName: String = "কামিজ",
    val quantity: Int = 1,
    val stitchingRate: Double = 0.0,
    val notes: String = "",
    val measurements: Map<String, String> = emptyMap()
) {
    val totalRate: Double
        get() = quantity * stitchingRate

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("garmentType", garmentType)
        obj.put("garmentName", garmentName)
        obj.put("quantity", quantity)
        obj.put("stitchingRate", stitchingRate)
        obj.put("notes", notes)
        val mObj = JSONObject()
        measurements.forEach { (k, v) ->
            if (v.isNotBlank()) {
                mObj.put(k, v)
            }
        }
        obj.put("measurements", mObj)
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): GarmentMeasurementItem {
            val gType = obj.optString("garmentType", GarmentType.KAMIZ.name)
            val gName = obj.optString("garmentName", "কামিজ")
            val qty = obj.optInt("quantity", 1).coerceAtLeast(1)
            val rate = obj.optDouble("stitchingRate", 0.0)
            val notes = obj.optString("notes", "")

            val measurementsMap = mutableMapOf<String, String>()
            val mObj = obj.optJSONObject("measurements")
            if (mObj != null) {
                val keys = mObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = mObj.optString(key, "")
                    if (value.isNotBlank()) {
                        measurementsMap[key] = value
                    }
                }
            }

            return GarmentMeasurementItem(
                garmentType = gType,
                garmentName = gName,
                quantity = qty,
                stitchingRate = rate,
                notes = notes,
                measurements = measurementsMap
            )
        }
    }
}

/**
 * Tailor order entity stored in Room database
 */
@Entity(tableName = "tailor_orders")
data class TailorOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String = "",
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val orderDate: Long = System.currentTimeMillis(),
    val deliveryDate: Long = System.currentTimeMillis() + 86400000L * 5,
    val fabricType: String = "কাস্টমারের কাপড়",
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val dueAmount: Double = 0.0,
    val status: String = "RECEIVED", // RECEIVED, CUTTING, STITCHING, READY, DELIVERED, CANCELLED
    val paymentMethod: String = "CASH",
    val designNotes: String = "",
    val garmentsJson: String = "[]",
    val sampleImageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun parseGarments(): List<GarmentMeasurementItem> {
        if (garmentsJson.isBlank() || garmentsJson == "[]") return emptyList()
        return try {
            val list = mutableListOf<GarmentMeasurementItem>()
            val array = JSONArray(garmentsJson)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj != null) {
                    list.add(GarmentMeasurementItem.fromJson(obj))
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getStatusBengali(): String {
        return when (status.uppercase()) {
            "RECEIVED" -> "অর্ডার নেওয়া হয়েছে"
            "CUTTING" -> "কাটিং হচ্ছে"
            "STITCHING" -> "সেলাই চলছে"
            "READY" -> "ডেলিভারির জন্য তৈরি"
            "DELIVERED" -> "ডেলিভারি সম্পন্ন"
            "CANCELLED" -> "বাতিল"
            else -> status
        }
    }

    fun getGarmentsSummary(): String {
        val garments = parseGarments()
        if (garments.isEmpty()) return "পোশাকের মাপ"
        return garments.joinToString(", ") { "${it.garmentName} (${it.quantity}টি)" }
    }
}

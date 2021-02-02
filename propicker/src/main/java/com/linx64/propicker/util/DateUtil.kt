package com.linx64.propicker.util


import java.text.SimpleDateFormat
import java.util.*

fun Date.formattedDate(toFormat: String = "yyyy-mm-dd"): String {
    return try {
        SimpleDateFormat(toFormat, Locale.ENGLISH).format(this)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}
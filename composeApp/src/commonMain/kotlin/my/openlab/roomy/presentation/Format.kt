package my.openlab.roomy.presentation

import kotlin.math.abs
import kotlin.math.round

/** Fixed-decimal formatting. Kotlin common has no printf, so this is the shared one (DRY). */
internal fun Double.format(decimals: Int): String {
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    val scaled = round(abs(this) * factor).toLong()
    val sign = if (this < 0) "-" else ""
    if (decimals == 0) return sign + scaled.toString()
    val whole = scaled / factor.toLong()
    val fraction = scaled % factor.toLong()
    return sign + whole.toString() + "." + fraction.toString().padStart(decimals, '0')
}

internal fun Float.format(decimals: Int): String = toDouble().format(decimals)

internal fun Int.pad2(): String = toString().padStart(2, '0')

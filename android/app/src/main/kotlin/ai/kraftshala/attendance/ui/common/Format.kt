package ai.kraftshala.attendance.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)
private val shortDateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

private fun parseAny(iso: String): ZonedDateTime? {
    if (iso.isBlank()) return null
    // Try strict Instant first (e.g. "2026-05-22T02:39:38Z")
    runCatching { return Instant.parse(iso).atZone(ZoneId.systemDefault()) }
    // Try OffsetDateTime (e.g. "2026-05-22T02:39:38+00:00")
    runCatching { return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()) }
    // Try PG's space-separated format ("2026-05-22 02:39:38+00")
    val normalised = iso.replaceFirst(" ", "T").let {
        when {
            it.endsWith("+00") -> it.dropLast(3) + "+00:00"
            it.endsWith("Z")    -> it
            it.matches(Regex(".*[+-]\\d{2}$")) -> it + ":00"
            else -> it
        }
    }
    runCatching { return OffsetDateTime.parse(normalised).atZoneSameInstant(ZoneId.systemDefault()) }
    return null
}

fun formatTime(iso: String?): String = iso?.let { parseAny(it) }?.let { timeFmt.format(it) } ?: "—"
fun formatDateLong(iso: String?): String = iso?.let { parseAny(it) }?.let { dateFmt.format(it) } ?: ""
fun formatDateShort(iso: String?): String = iso?.let { parseAny(it) }?.let { shortDateFmt.format(it) } ?: ""

fun todayLong(): String = dateFmt.format(LocalDate.now(ZoneId.systemDefault()))

fun timeRange(startIso: String?, endIso: String?): String =
    "${formatTime(startIso)} — ${formatTime(endIso)}"

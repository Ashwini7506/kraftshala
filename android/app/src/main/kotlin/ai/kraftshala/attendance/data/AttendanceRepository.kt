package ai.kraftshala.attendance.data

import ai.kraftshala.attendance.data.local.AppDatabase
import ai.kraftshala.attendance.data.local.QueuedMark
import ai.kraftshala.attendance.domain.models.AttendanceRecord
import ai.kraftshala.attendance.domain.models.Session
import android.content.Context
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import java.time.Instant

class AttendanceRepository(private val context: Context) {

    private val client = SupabaseClientProvider.client
    private val db = AppDatabase.get(context)
    private val keyMgr = DeviceKeyManager()

    data class MarkResult(val ok: Boolean, val errorMessage: String? = null)

    suspend fun markPresent(
        sessionId: String,
        userId: String,
        nonce: String,
        signalStrengths: List<Int>
    ): MarkResult {
        try {
            ensureDeviceEnrolled(userId)
        } catch (e: Exception) {
            return MarkResult(false, "enroll: ${e.message}")
        }
        val timestamp = Instant.now().toString()
        val message = buildJsonObject {
            put("session_id", sessionId)
            put("nonce", nonce)
            put("timestamp", timestamp)
            putJsonArray("ble_signal_strengths") { signalStrengths.forEach { add(it) } }
        }.toString()
        val signature = keyMgr.sign(message.toByteArray(Charsets.UTF_8))

        return try {
            if (ai.kraftshala.attendance.BuildConfig.DEMO_MODE) {
                // Test env: skip edge function, write directly via PostgREST so the
                // full UI flow demos end-to-end without verify-signature being deployed.
                client.from("attendance_records").upsert(
                    buildJsonObject {
                        put("session_id", sessionId)
                        put("user_id", userId)
                        put("self_mark_at", timestamp)
                        put("signed_token", signature)
                        put("signature_verified", true)
                        put("ble_detected_throughout", true)
                    },
                    onConflict = "session_id,user_id"
                )
                Timber.d("DEMO_MODE mark recorded directly")
                return MarkResult(true)
            }
            val body = buildJsonObject {
                put("session_id", sessionId)
                put("user_id", userId)
                put("nonce", nonce)
                put("timestamp", timestamp)
                putJsonArray("ble_signal_strengths") { signalStrengths.forEach { add(it) } }
                put("signature_b64", signature)
            }
            val response = client.functions.invoke("verify-signature", body)
            Timber.d("Mark verified: ${response.bodyAsText()}")
            MarkResult(true)
        } catch (e: Exception) {
            Timber.w(e, "Offline or verify failed, queueing locally")
            db.queueDao().insertMark(
                QueuedMark(
                    sessionId = sessionId,
                    userId = userId,
                    timestamp = timestamp,
                    nonce = nonce,
                    signatureB64 = signature,
                    signalStrengthsCsv = signalStrengths.joinToString(",")
                )
            )
            MarkResult(false, "mark: ${e::class.java.simpleName}: ${e.message}")
        }
    }

    suspend fun syncQueue() {
        val pendingMarks = db.queueDao().pendingMarks()
        for (m in pendingMarks) {
            try {
                val body = buildJsonObject {
                    put("session_id", m.sessionId)
                    put("user_id", m.userId)
                    put("nonce", m.nonce)
                    put("timestamp", m.timestamp)
                    putJsonArray("ble_signal_strengths") {
                        m.signalStrengthsCsv.split(",").mapNotNull { it.toIntOrNull() }.forEach { add(it) }
                    }
                    put("signature_b64", m.signatureB64)
                }
                client.functions.invoke("verify-signature", body)
                db.queueDao().markMarkSynced(m.id, Instant.now().toString())
            } catch (e: Exception) {
                Timber.w(e, "Sync failed for mark ${m.id}, will retry")
            }
        }

        val pendingBle = db.queueDao().pendingBleEvents()
        for (e in pendingBle) {
            try {
                client.from("ble_proximity_log").insert(
                    buildJsonObject {
                        put("session_id", e.sessionId)
                        put("user_id", e.userId)
                        put("signal_strength", e.signalStrength)
                        put("detected_at", e.detectedAt)
                        put("event_type", e.eventType)
                    }
                )
                db.queueDao().markBleSynced(e.id, Instant.now().toString())
            } catch (ex: Exception) {
                Timber.w(ex, "Sync failed for BLE event ${e.id}")
            }
        }
    }

    suspend fun activeSessionForCohort(cohortId: String): Session? = runCatching {
        client.from("sessions").select {
            filter { eq("cohort_id", cohortId); eq("status", "active") }
            limit(1)
        }.decodeList<Session>().firstOrNull()
    }.getOrNull()

    suspend fun todaySessions(cohortId: String? = null, instructorId: String? = null): List<Session> = runCatching {
        val now = Instant.now()
        val start = now.minusSeconds(12 * 60 * 60).toString()
        val end = now.plusSeconds(24 * 60 * 60).toString()
        client.from("sessions").select {
            filter {
                cohortId?.let { eq("cohort_id", it) }
                instructorId?.let { eq("instructor_id", it) }
                gte("start_at", start)
                lte("start_at", end)
            }
            order("start_at", Order.ASCENDING)
        }.decodeList<Session>()
    }.getOrElse { Timber.w(it, "todaySessions failed"); emptyList() }

    suspend fun pastSessions(cohortId: String? = null, instructorId: String? = null, limit: Long = 100): List<Session> = runCatching {
        client.from("sessions").select {
            filter {
                cohortId?.let { eq("cohort_id", it) }
                instructorId?.let { eq("instructor_id", it) }
            }
            order("start_at", Order.DESCENDING)
            limit(limit)
        }.decodeList<Session>()
    }.getOrElse { emptyList() }

    suspend fun sessionById(id: String): Session? = runCatching {
        client.from("sessions").select { filter { eq("id", id) }; limit(1) }
            .decodeList<Session>().firstOrNull()
    }.getOrNull()

    suspend fun rosterForSession(sessionId: String): List<AttendanceRecord> = runCatching {
        client.from("attendance_records")
            .select { filter { eq("session_id", sessionId) } }
            .decodeList<AttendanceRecord>()
    }.getOrElse { emptyList() }

    fun rosterStream(sessionId: String): Flow<List<AttendanceRecord>> = flow {
        emit(rosterForSession(sessionId))
        try {
            val channel = client.realtime.channel("attendance:$sessionId")
            val changes = channel.postgresChangeFlow<PostgresAction>(schema = "kraftshala") {
                table = "attendance_records"
                filter = "session_id=eq.$sessionId"
            }
            channel.subscribe()
            changes.collect { emit(rosterForSession(sessionId)) }
        } catch (e: Exception) {
            Timber.w(e, "realtime roster stream failed")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun myRecordFor(sessionId: String, userId: String): AttendanceRecord? = runCatching {
        client.from("attendance_records").select {
            filter { eq("session_id", sessionId); eq("user_id", userId) }
            limit(1)
        }.decodeList<AttendanceRecord>().firstOrNull()
    }.getOrNull()

    suspend fun overrideRecord(sessionId: String, userId: String, override: String, reason: String? = null) {
        runCatching {
            client.from("attendance_records").update(
                buildJsonObject {
                    put("instructor_called_absent", override == "force_absent")
                    put("instructor_override", override)
                    reason?.let { put("override_reason", it) }
                }
            ) { filter { eq("session_id", sessionId); eq("user_id", userId) } }
        }.onFailure { Timber.w(it, "override failed") }
    }

    suspend fun startSessionNonce(sessionId: String, instructorId: String): String? = runCatching {
        val res = client.functions.invoke("generate-nonce", buildJsonObject {
            put("session_id", sessionId)
            put("instructor_id", instructorId)
        })
        val body = res.bodyAsText()
        (Json.parseToJsonElement(body) as? JsonObject)
            ?.get("session_nonce")?.toString()?.trim('"')
    }.getOrNull()

    suspend fun finaliseSession(sessionId: String, instructorId: String) {
        runCatching {
            client.functions.invoke("finalise-session", buildJsonObject {
                put("session_id", sessionId)
                put("instructor_id", instructorId)
            })
        }.onFailure { Timber.w(it, "finalise failed") }
    }

    /** Lazily creates the device key pair + enrols it server-side on first attendance attempt. */
    private suspend fun ensureDeviceEnrolled(userId: String) {
        if (keyMgr.hasKeyPair()) {
            // Ensure DB row exists too (in case key persists but row was wiped).
            val existing = runCatching {
                client.from("devices").select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "active")
                    }
                    limit(1)
                }.decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()
            }.getOrNull()
            if (existing != null) return
        } else {
            keyMgr.generateKeyPair()
        }
        val pem = keyMgr.getPublicKeyPem()
        val fingerprint = android.provider.Settings.Secure.getString(
            context.contentResolver, android.provider.Settings.Secure.ANDROID_ID
        ) ?: java.util.UUID.randomUUID().toString()
        runCatching {
            client.from("devices").insert(
                buildJsonObject {
                    put("user_id", userId)
                    put("public_key", pem)
                    put("device_fingerprint", fingerprint)
                    put("status", "active")
                }
            )
        }.onFailure { Timber.w(it, "device enroll failed") }
    }
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(
    key: String,
    builder: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit
) {
    put(key, buildJsonArray(builder))
}

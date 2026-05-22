package ai.kraftshala.attendance.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val role: String,                              // 'student' | 'instructor' | 'admin' | 'coordinator'
    @SerialName("cohort_id") val cohortId: String?
)

@Serializable
data class Session(
    val id: String,
    @SerialName("cohort_id") val cohortId: String,
    @SerialName("instructor_id") val instructorId: String,
    val classroom: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("session_nonce") val sessionNonce: String,
    val status: String
)

@Serializable
data class AttendanceRecord(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("self_mark_at") val selfMarkAt: String?,
    @SerialName("ble_first_detected_at") val bleFirstDetectedAt: String?,
    @SerialName("ble_last_detected_at") val bleLastDetectedAt: String?,
    @SerialName("ble_detected_throughout") val bleDetectedThroughout: Boolean,
    @SerialName("instructor_called_absent") val instructorCalledAbsent: Boolean,
    @SerialName("instructor_override") val instructorOverride: String?,
    @SerialName("final_flag") val finalFlag: String?
)

@Serializable
data class SignedMarkPayload(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String,
    val nonce: String,
    val timestamp: String,
    @SerialName("ble_signal_strengths") val bleSignalStrengths: List<Int>,
    @SerialName("signature_b64") val signatureB64: String
)

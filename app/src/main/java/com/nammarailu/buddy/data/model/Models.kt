package com.nammarailu.buddy.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Station(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val zone: String = "",
    val state: String = ""
)

@IgnoreExtraProperties
data class Train(
    val id: String = "",
    val name: String = "",
    val number: String = "",
    val type: String = "",
    val stations: List<String> = emptyList()
)

@IgnoreExtraProperties
data class PlatformUpdate(
    val train_id: String = "",
    val station_id: String = "",
    val platform_number: Int = 0,
    val confirmations_count: Int = 0,
    val timestamp: Long = 0L,
    val status: String = "UNKNOWN",
    val userVotes: Map<String, Int> = emptyMap()
) {
    val trainStatus: TrainStatus get() = when (status) {
        "ON_TIME" -> TrainStatus.ON_TIME
        "DELAYED" -> TrainStatus.DELAYED
        "WARNING" -> TrainStatus.WARNING
        else -> TrainStatus.UNKNOWN
    }
}

enum class TrainStatus { ON_TIME, DELAYED, WARNING, UNKNOWN }

@IgnoreExtraProperties
data class Coach(
    val type: String = "",
    val label: String = "",
    val position: Int = 0
)

@IgnoreExtraProperties
data class CoachPosition(
    val train_id: String = "",
    val coaches: List<Coach> = emptyList(),
    val direction: String = "FRONT_TO_ENGINE"
)

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val points: Int = 0,
    val contributions: Int = 0
)

data class LiveTrainInfo(
    val train: Train,
    val platformUpdate: PlatformUpdate?,
    val arrivalTime: String = "",
    val departureTime: String = ""
)

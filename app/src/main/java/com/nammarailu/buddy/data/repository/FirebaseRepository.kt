package com.nammarailu.buddy.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.nammarailu.buddy.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class FirebaseRepository @Inject constructor(
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    val currentUid get() = auth.currentUser?.uid

    fun getStations(): Flow<Result<List<Station>>> = callbackFlow {
        trySend(Result.Loading)
        val ref = db.getReference("stations")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val list = snap.children.mapNotNull { it.getValue(Station::class.java) }
                trySend(Result.Success(list))
            }
            override fun onCancelled(err: DatabaseError) { trySend(Result.Error(err.message)) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getTrainsAtStation(stationId: String): Flow<Result<List<LiveTrainInfo>>> = callbackFlow {
        trySend(Result.Loading)
        val trainsRef    = db.getReference("trains")
        val updatesRef   = db.getReference("platformUpdates")
        val schedulesRef = db.getReference("trainSchedules").child(stationId)

        var currentTrains: List<Train>? = null
        var currentUpdates: DataSnapshot? = null
        var currentSchedules: DataSnapshot? = null

        fun emitIfReady() {
            val trains = currentTrains ?: return
            val updSnap = currentUpdates ?: return
            val schedSnap = currentSchedules ?: return
            
            val infos = trains.map { t ->
                val arrivalTime   = schedSnap.child(t.id).child("arrival").getValue(String::class.java) ?: ""
                val departureTime = schedSnap.child(t.id).child("departure").getValue(String::class.java) ?: ""
                val platformUpdate = updSnap.child("${t.id}_${stationId}").getValue(PlatformUpdate::class.java)
                LiveTrainInfo(t, platformUpdate, arrivalTime, departureTime)
            }
            trySend(Result.Success(infos))
        }

        val trainsListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                currentTrains = snap.children.mapNotNull { child ->
                    var train = child.getValue(Train::class.java)
                    if (train != null && train.stations.isEmpty()) {
                        val stationsSnap = child.child("stations")
                        val stationsFromMap = stationsSnap.children.mapNotNull { it.getValue(String::class.java) }
                        if (stationsFromMap.isNotEmpty()) {
                            train = train.copy(stations = stationsFromMap)
                        }
                    }
                    train
                }.filter { it.stations.contains(stationId) }
                emitIfReady()
            }
            override fun onCancelled(err: DatabaseError) { trySend(Result.Error(err.message)) }
        }

        val updatesListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                currentUpdates = snap
                emitIfReady()
            }
            override fun onCancelled(err: DatabaseError) { trySend(Result.Error(err.message)) }
        }

        val schedulesListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                currentSchedules = snap
                emitIfReady()
            }
            override fun onCancelled(err: DatabaseError) { trySend(Result.Error(err.message)) }
        }

        trainsRef.addValueEventListener(trainsListener)
        updatesRef.addValueEventListener(updatesListener)
        schedulesRef.addValueEventListener(schedulesListener)

        awaitClose {
            trainsRef.removeEventListener(trainsListener)
            updatesRef.removeEventListener(updatesListener)
            schedulesRef.removeEventListener(schedulesListener)
        }
    }

    fun getPlatformUpdate(trainId: String, stationId: String): Flow<Result<PlatformUpdate?>> = callbackFlow {
        trySend(Result.Loading)
        // FIX 1: Key is trainId_stationId — each station has its own platform record for this train
        val ref = db.getReference("platformUpdates").child("${trainId}_${stationId}")
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { trySend(Result.Success(snap.getValue(PlatformUpdate::class.java))) }
            override fun onCancelled(err: DatabaseError)  { trySend(Result.Error(err.message)) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * FIX 1 + 4: Submit or update a platform ping.
     *  - Key is trainId_stationId, so the same train at different stations stores independently.
     *  - If the majority platform changes, reset confirmations to 1 and status to ON_TIME.
     *  - If confirming the same platform, increment count.
     *  - status is always set to ON_TIME when a new/updated ping arrives (visible in Live Station).
     */
    suspend fun submitPlatformPing(trainId: String, stationId: String, platform: Int, existingUpdate: PlatformUpdate?) {
        ensureAuth()
        val uid = currentUid ?: return
        val key = "${trainId}_${stationId}"
        val ref = db.getReference("platformUpdates").child(key)
        try {
            val existing = existingUpdate ?: PlatformUpdate(
                train_id = trainId,
                station_id = stationId,
                status = "ON_TIME"
            )

            // Update user's vote
            val updatedUserVotes = existing.userVotes.toMutableMap()
            updatedUserVotes[uid] = platform

            // Find platform with highest votes
            val voteCounts = mutableMapOf<Int, Int>()
            for (vote in updatedUserVotes.values) {
                voteCounts[vote] = (voteCounts[vote] ?: 0) + 1
            }

            var maxPlatform = 0
            var maxVotes = 0
            for ((p, count) in voteCounts) {
                if (count > maxVotes) {
                    maxVotes = count
                    maxPlatform = p
                }
            }

            val updated = existing.copy(
                platform_number = maxPlatform,
                confirmations_count = maxVotes,
                timestamp = System.currentTimeMillis(),
                userVotes = updatedUserVotes
            )

            ref.setValue(updated) // No await() allows instant local cache update

        } catch (_: Exception) {}
        awardPoints(1)
    }

    fun getCoachPosition(trainId: String): Flow<Result<CoachPosition?>> = callbackFlow {
        trySend(Result.Loading)
        val ref = db.getReference("coachPosition").child(trainId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (!snap.exists()) {
                    trySend(Result.Success(null))
                    return
                }
                var coachPos = snap.getValue(CoachPosition::class.java)
                if (coachPos == null || coachPos.coaches.isEmpty()) {
                    try {
                        val trainIdVal  = snap.child("train_id").getValue(String::class.java) ?: trainId
                        val direction   = snap.child("direction").getValue(String::class.java) ?: "FRONT_TO_ENGINE"
                        val coachesSnap = snap.child("coaches")
                        val coaches     = coachesSnap.children.mapNotNull { child -> child.getValue(Coach::class.java) }
                        if (coaches.isNotEmpty()) {
                            coachPos = CoachPosition(train_id = trainIdVal, coaches = coaches, direction = direction)
                        }
                    } catch (_: Exception) {}
                }
                trySend(Result.Success(coachPos))
            }
            override fun onCancelled(err: DatabaseError) { trySend(Result.Error(err.message)) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun awardPoints(pts: Int) {
        val uid = currentUid ?: return
        val ref = db.getReference("users").child(uid)
        ref.get().addOnSuccessListener { snap ->
            val user = snap.getValue(UserProfile::class.java) ?: UserProfile(id = uid)
            ref.setValue(user.copy(points = user.points + pts, contributions = user.contributions + 1))
        }
    }

    fun getUserProfile(): Flow<Result<UserProfile?>> = callbackFlow {
        val uid = currentUid
        if (uid == null) { trySend(Result.Success(null)); close(); return@callbackFlow }
        val ref = db.getReference("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { trySend(Result.Success(snap.getValue(UserProfile::class.java))) }
            override fun onCancelled(err: DatabaseError)  { trySend(Result.Error(err.message)) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private suspend fun ensureAuth() {
        if (auth.currentUser == null) {
            try { auth.signInAnonymously().await() } catch (_: Exception) {}
        }
    }
}
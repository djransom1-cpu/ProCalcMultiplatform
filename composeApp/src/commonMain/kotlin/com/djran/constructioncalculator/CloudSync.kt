package com.djran.constructioncalculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Signs in with Firebase email/password and keeps the whole [AppState] in one Firestore document
 * per user, so the same projects show up on the phone and on the web.
 *
 * Uses the same Firebase project as Crewsync (one login for both apps) and talks to Firebase over
 * its REST APIs, which work identically on Android and in the Wasm web build.
 */
object FirebaseConfig {
    const val API_KEY = "AIzaSyD7VnuipzkUGy3aQ6Pg0jhIfw24IjjsayI"
    const val PROJECT_ID = "gen-lang-client-0438127279"
    const val SYNC_COLLECTION = "procalc_sync"
}

class CloudSyncException(message: String) : Exception(message)

class CloudSync(private val settings: Settings?) {
    private class RemoteDoc(val stateJson: String, val version: String)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    var email by mutableStateOf(settings?.getStringOrNull(KEY_EMAIL))
        private set
    var status by mutableStateOf("")
        private set
    var isBusy by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    val isSignedIn: Boolean get() = email != null && uid != null && refreshToken != null

    private var uid: String? = settings?.getStringOrNull(KEY_UID)
    private var refreshToken: String? = settings?.getStringOrNull(KEY_REFRESH)
    private var idToken: String? = null
    private var idTokenExpiresAt: TimeMark? = null

    // Firestore updateTime of the document the last time this device was in step with it,
    // and a hash of the local state at that moment (to tell whether local edits are pending).
    private var syncedVersion: String?
        get() = settings?.getStringOrNull(KEY_VERSION)
        set(v) { if (v == null) settings?.remove(KEY_VERSION) else settings?.putString(KEY_VERSION, v) }
    private var syncedHash: Int?
        get() = settings?.getIntOrNull(KEY_HASH)
        set(v) { if (v == null) settings?.remove(KEY_HASH) else settings?.putInt(KEY_HASH, v) }

    // ---------------------------------------------------------------- auth

    suspend fun signIn(email: String, password: String) = authenticate("signInWithPassword", email, password)

    suspend fun createAccount(email: String, password: String) = authenticate("signUp", email, password)

    suspend fun sendPasswordReset(email: String) {
        val body = buildJsonObject {
            put("requestType", "PASSWORD_RESET")
            put("email", email.trim())
        }.toString()
        val res = httpRequest("POST", "$AUTH_URL/accounts:sendOobCode?key=${FirebaseConfig.API_KEY}", JSON_HEADERS, body)
        if (res.status != 200) throw CloudSyncException(authError(res))
    }

    fun signOut() {
        listOf(KEY_EMAIL, KEY_UID, KEY_REFRESH, KEY_VERSION, KEY_HASH).forEach { settings?.remove(it) }
        email = null
        uid = null
        refreshToken = null
        idToken = null
        status = ""
        lastError = null
    }

    private suspend fun authenticate(endpoint: String, email: String, password: String) {
        val body = buildJsonObject {
            put("email", email.trim())
            put("password", password)
            put("returnSecureToken", true)
        }.toString()
        val res = httpRequest("POST", "$AUTH_URL/accounts:$endpoint?key=${FirebaseConfig.API_KEY}", JSON_HEADERS, body)
        if (res.status != 200) throw CloudSyncException(authError(res))
        val obj = json.parseToJsonElement(res.body).jsonObject
        // A new login may belong to a different account, so forget what this device last synced.
        syncedVersion = null
        syncedHash = null
        storeSession(
            uid = obj.string("localId"),
            email = obj.string("email"),
            refresh = obj.string("refreshToken"),
            token = obj.string("idToken"),
            expiresIn = obj.string("expiresIn")
        )
        lastError = null
    }

    private fun storeSession(uid: String, email: String, refresh: String, token: String, expiresIn: String) {
        this.uid = uid
        this.refreshToken = refresh
        this.idToken = token
        this.idTokenExpiresAt = TimeSource.Monotonic.markNow() + ((expiresIn.toLongOrNull() ?: 3600L) - 60L).seconds
        settings?.putString(KEY_UID, uid)
        settings?.putString(KEY_REFRESH, refresh)
        settings?.putString(KEY_EMAIL, email)
        this.email = email
    }

    private suspend fun validIdToken(): String {
        val token = idToken
        if (token != null && idTokenExpiresAt?.hasPassedNow() == false) return token
        val refresh = refreshToken ?: throw CloudSyncException("Not signed in")
        val res = httpRequest(
            "POST",
            "https://securetoken.googleapis.com/v1/token?key=${FirebaseConfig.API_KEY}",
            mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            "grant_type=refresh_token&refresh_token=$refresh"
        )
        if (res.status == 400 || res.status == 401) {
            val message = authError(res)
            signOut()
            throw CloudSyncException("Signed out: $message")
        }
        if (res.status != 200) throw CloudSyncException(authError(res))
        val obj = json.parseToJsonElement(res.body).jsonObject
        storeSession(
            uid = obj.string("user_id"),
            email = email ?: "",
            refresh = obj.string("refresh_token"),
            token = obj.string("id_token"),
            expiresIn = obj.string("expires_in")
        )
        return idToken!!
    }

    // ---------------------------------------------------------------- sync

    /**
     * Brings local and cloud state in step. [localState] is the current on-device state; if the
     * cloud copy should replace it, the merged/remote state is handed to [applyRemote].
     */
    suspend fun sync(localState: AppState, applyRemote: (AppState) -> Unit) {
        if (!isSignedIn) return
        mutex.withLock {
            isBusy = true
            try {
                syncLocked(localState, applyRemote)
                lastError = null
                status = "Synced"
            } catch (e: CloudSyncException) {
                lastError = e.message
                status = "Sync failed"
            } catch (e: Exception) {
                lastError = e.message ?: "Sync failed"
                status = "Sync failed"
            } finally {
                isBusy = false
            }
        }
    }

    private suspend fun syncLocked(initialLocal: AppState, applyRemote: (AppState) -> Unit) {
        var local = initialLocal
        // A write only fails its precondition if another device saved in between; retry a few times.
        repeat(3) {
            val localJson = encode(local)
            val localDirty = localJson.hashCode() != syncedHash
            val remote = fetchRemote()

            if (remote == null) {
                // Nothing in the cloud yet: upload what this device has.
                if (pushState(localJson, expectedVersion = null)) return
                return@repeat
            }

            val remoteState = decode(remote.stateJson)
            if (remote.version == syncedVersion) {
                // Cloud unchanged since our last sync.
                if (!localDirty || pushState(localJson, remote.version)) return
                return@repeat
            }

            // Cloud changed (another device saved, or this is this device's first sync).
            val firstSync = syncedVersion == null
            if (!localDirty && !firstSync) {
                adopt(remoteState, remote.version, applyRemote)
                return
            }
            if (firstSync && isEmpty(local)) {
                adopt(remoteState, remote.version, applyRemote)
                return
            }
            // Both sides have changes: merge them and upload the result.
            val merged = decode(mergeStates(remote.stateJson, localJson, combineLists = firstSync))
            applyRemote(merged)
            local = merged
            if (pushState(encode(merged), remote.version)) return
        }
        throw CloudSyncException("Cloud data kept changing, try again")
    }

    private fun adopt(state: AppState, version: String, applyRemote: (AppState) -> Unit) {
        applyRemote(state)
        syncedVersion = version
        syncedHash = encode(state).hashCode()
    }

    private suspend fun fetchRemote(): RemoteDoc? {
        val res = httpRequest("GET", docUrl(), authHeaders())
        if (res.status == 404) return null
        if (res.status != 200) throw CloudSyncException(firestoreError(res))
        val obj = json.parseToJsonElement(res.body).jsonObject
        val stateJson = (obj["fields"] as? JsonObject)
            ?.get("state")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content
            ?: return null
        return RemoteDoc(stateJson, obj.string("updateTime"))
    }

    /** Returns false if the cloud copy changed since [expectedVersion] (caller should re-sync). */
    private suspend fun pushState(stateJson: String, expectedVersion: String?): Boolean {
        val body = buildJsonObject {
            putJsonArray("writes") {
                add(buildJsonObject {
                    putJsonObject("update") {
                        put("name", docName())
                        putJsonObject("fields") {
                            putJsonObject("state") { put("stringValue", stateJson) }
                        }
                    }
                    putJsonObject("currentDocument") {
                        if (expectedVersion == null) put("exists", false) else put("updateTime", expectedVersion)
                    }
                })
            }
        }.toString()
        val res = httpRequest("POST", "$FIRESTORE_URL/documents:commit", authHeaders() + JSON_HEADERS, body)
        if (res.status == 200) {
            val version = json.parseToJsonElement(res.body).jsonObject["writeResults"]
                ?.let { it as? JsonArray }?.firstOrNull()?.jsonObject?.get("updateTime")?.jsonPrimitive?.content
            syncedVersion = version
            syncedHash = stateJson.hashCode()
            return true
        }
        if (res.body.contains("FAILED_PRECONDITION") || res.body.contains("ALREADY_EXISTS") || res.status == 409) return false
        throw CloudSyncException(firestoreError(res))
    }

    private suspend fun authHeaders(): Map<String, String> = mapOf("Authorization" to "Bearer ${validIdToken()}")

    private fun docName() = "projects/${FirebaseConfig.PROJECT_ID}/databases/(default)/documents/${FirebaseConfig.SYNC_COLLECTION}/$uid"
    private fun docUrl() = "https://firestore.googleapis.com/v1/${docName()}"

    fun encode(state: AppState): String = json.encodeToString(AppState.serializer(), state)
    private fun decode(stateJson: String): AppState = json.decodeFromString(AppState.serializer(), stateJson)

    private fun isEmpty(state: AppState): Boolean {
        val defaults = AppState(listOf("Default"), "Default", emptyMap(), emptyMap(), emptyMap())
        val stripped = json.parseToJsonElement(encode(state)).jsonObject
        val base = json.parseToJsonElement(encode(defaults)).jsonObject
        return stripped.all { (key, value) ->
            key == "currentProjectName" || value == base[key] ||
                (value is JsonObject && value.values.all { it is JsonArray && it.isEmpty() })
        }
    }

    /**
     * Merges two serialized [AppState]s. Project lists are unioned; for per-project entry maps the
     * local copy of a project wins, unless [combineLists] is set (first sync on a device), in which
     * case both sides' entries are kept so nothing already saved anywhere is lost.
     */
    private fun mergeStates(remoteJson: String, localJson: String, combineLists: Boolean): String {
        val remote = json.parseToJsonElement(remoteJson).jsonObject
        val local = json.parseToJsonElement(localJson).jsonObject
        val merged = buildJsonObject {
            for (key in remote.keys + local.keys) {
                val r = remote[key]
                val l = local[key]
                val value: JsonElement = when {
                    r == null -> l!!
                    l == null -> r
                    r is JsonArray && l is JsonArray -> JsonArray((r + l).distinct())
                    r is JsonObject && l is JsonObject -> buildJsonObject {
                        for (project in r.keys + l.keys) {
                            val rp = r[project]
                            val lp = l[project]
                            put(project, when {
                                rp == null -> lp!!
                                lp == null -> rp
                                combineLists && rp is JsonArray && lp is JsonArray -> JsonArray((rp + lp).distinct())
                                else -> lp!!
                            })
                        }
                    }
                    else -> l!!
                }
                put(key, value)
            }
        }
        return merged.toString()
    }

    private fun authError(res: HttpResult): String {
        if (res.status == 0) return "Can't reach the server. Check your connection."
        val code = errorMessage(res) ?: return "Sign-in failed (${res.status})"
        return when {
            code.startsWith("EMAIL_NOT_FOUND") || code.startsWith("INVALID_PASSWORD") ||
                code.startsWith("INVALID_LOGIN_CREDENTIALS") -> "Wrong email or password"
            code.startsWith("EMAIL_EXISTS") -> "An account with that email already exists. Sign in instead."
            code.startsWith("INVALID_EMAIL") -> "That email address isn't valid"
            code.startsWith("WEAK_PASSWORD") -> "Password must be at least 6 characters"
            code.startsWith("USER_DISABLED") -> "This account has been disabled"
            code.startsWith("TOO_MANY_ATTEMPTS") -> "Too many attempts. Try again later."
            code.startsWith("TOKEN_EXPIRED") || code.startsWith("INVALID_REFRESH_TOKEN") ||
                code.startsWith("USER_NOT_FOUND") -> "Please sign in again"
            else -> code
        }
    }

    private fun firestoreError(res: HttpResult): String {
        if (res.status == 0) return "Can't reach the server. Check your connection."
        if (res.status == 403) return "The cloud refused access (check the Firestore rules for ${FirebaseConfig.SYNC_COLLECTION})"
        return errorMessage(res) ?: "Cloud error (${res.status})"
    }

    private fun errorMessage(res: HttpResult): String? = try {
        json.parseToJsonElement(res.body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }

    private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.content ?: ""

    private companion object {
        const val AUTH_URL = "https://identitytoolkit.googleapis.com/v1"
        const val FIRESTORE_URL = "https://firestore.googleapis.com/v1/projects/${FirebaseConfig.PROJECT_ID}/databases/(default)"
        val JSON_HEADERS = mapOf("Content-Type" to "application/json")
        const val KEY_EMAIL = "cloud_email"
        const val KEY_UID = "cloud_uid"
        const val KEY_REFRESH = "cloud_refresh_token"
        const val KEY_VERSION = "cloud_synced_version"
        const val KEY_HASH = "cloud_synced_hash"
    }
}

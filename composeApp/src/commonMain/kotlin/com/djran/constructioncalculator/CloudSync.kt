package com.djran.constructioncalculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

/** A local project that is linked to a cloud project (the Android app's `cloud_id_<project>`). */
@Serializable
data class CloudLink(
    val cloudId: String,
    /** Cloud `updatedAt` the last time this project was pulled (the app's `cloud_synced_at`). */
    val syncedAtMillis: Long = 0,
    /** The cloud dataJson last pulled - keeps phone-only data intact when this device pushes. */
    val dataJson: String = ""
)

@Serializable
data class CloudUser(val uid: String, val email: String)

@Serializable
data class CloudProjectSummary(
    val cloudId: String,
    val displayName: String,
    val ownerEmail: String = "",
    val memberCount: Int = 1,
    val updatedAtMillis: Long = 0
)

@Serializable
data class TeamInfo(
    val inviteCode: String,
    val ownerUid: String,
    val ownerEmail: String,
    val memberEmails: List<String>,
    val isOwner: Boolean
)

@Serializable
private data class CloudProject(val cloudId: String, val displayName: String, val dataJson: String, val updatedAtMillis: Long)

@Serializable
private data class CreatedProject(val cloudId: String, val syncedAtMillis: Long)

@Serializable
private data class CloudIdResult(val cloudId: String)

@Serializable
private data class InviteCodeResult(val inviteCode: String)

class CloudSyncException(message: String) : Exception(message)

/**
 * Cloud sync that works exactly like the Android app's: Google sign-in, projects synced one at a
 * time into `projects/{cloudId}` of the pro-construction-calculator Firebase project, and teams
 * joined by invite code. All Firebase calls go through the web bridge (procalc-cloud.js).
 */
class CloudSync {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    var user by mutableStateOf<CloudUser?>(null)
        private set
    var isBusy by mutableStateOf(false)
        private set
    /** Last outcome to show the user, e.g. "'Smith Job' synced" or an error. */
    var message by mutableStateOf<String?>(null)
        private set
    var messageIsError by mutableStateOf(false)
        private set

    val isSignedIn: Boolean get() = user != null

    fun clearMessage() { message = null }

    // ---------------------------------------------------------------- auth

    suspend fun restoreSession() {
        if (!cloudSyncSupported) return
        user = runCatching { decodeOrNull<CloudUser>(call("currentUser")) }.getOrNull()
    }

    suspend fun signIn() = work {
        user = decodeOrNull<CloudUser>(call("signIn"))
        user?.let { report("Signed in as ${it.email}") }
    }

    suspend fun signOut() = work {
        call("signOut")
        user = null
        report("Signed out. Your projects stay on this device.")
    }

    // ---------------------------------------------------------------- projects

    /**
     * Same as the app's ProjectListActivity.mergeCloudProjects: brings down cloud projects this
     * device doesn't have yet and pulls linked projects that changed in the cloud since.
     */
    suspend fun mergeCloudProjects(current: () -> AppState, apply: (AppState) -> Unit) = work(quiet = true) {
        if (!isSignedIn) return@work
        val summaries = json.decodeFromJsonElement<List<CloudProjectSummary>>(call("listProjects"))
        for (summary in summaries) {
            val state = current()
            val localName = state.cloudLinks.entries.firstOrNull { it.value.cloudId == summary.cloudId }?.key
            if (localName == null) {
                apply(importProject(state, summary.cloudId))
            } else if ((state.cloudLinks[localName]?.syncedAtMillis ?: 0) < summary.updatedAtMillis) {
                apply(pullProject(state, localName))
            }
        }
    }

    /**
     * The CLOUD / SYNCED button: uploads a project the first time, afterwards pushes this device's
     * copy and pulls the result back (the app's enableCloudSync / pushProject + pullProject).
     */
    suspend fun syncProject(name: String, current: () -> AppState, apply: (AppState) -> Unit) = work {
        requireSignedIn()
        val state = current()
        val link = state.cloudLinks[name]
        if (link == null) {
            val dataJson = PhoneFormat.toDataJson(state, name, null)
            val created = json.decodeFromJsonElement<CreatedProject>(call("createProject", buildJsonObject {
                put("displayName", name)
                put("dataJson", dataJson)
            }.toString()))
            apply(state.copy(cloudLinks = state.cloudLinks + (name to CloudLink(created.cloudId, created.syncedAtMillis, dataJson))))
            report("'$name' is now synced to the cloud")
        } else {
            call("pushProject", buildJsonObject {
                put("cloudId", link.cloudId)
                put("displayName", name)
                put("dataJson", PhoneFormat.toDataJson(state, name, link.dataJson))
            }.toString())
            apply(pullProject(current(), name))
            report("'$name' synced")
        }
    }

    suspend fun joinByCode(code: String, current: () -> AppState, apply: (AppState) -> Unit) = work {
        requireSignedIn()
        val joined = json.decodeFromJsonElement<CloudIdResult>(call("joinByCode", code))
        val state = importProject(current(), joined.cloudId)
        apply(state)
        val name = state.cloudLinks.entries.first { it.value.cloudId == joined.cloudId }.key
        report("Joined '$name'")
    }

    suspend fun teamInfo(cloudId: String): TeamInfo =
        json.decodeFromJsonElement(call("teamInfo", cloudId))

    suspend fun regenerateInviteCode(cloudId: String): String =
        json.decodeFromJsonElement<InviteCodeResult>(call("regenerateCode", cloudId)).inviteCode

    /** Owner: deletes the cloud copy for everyone. Member: leaves the team. Call before deleting locally. */
    suspend fun removeFromCloud(cloudId: String) {
        call("removeProject", cloudId)
    }

    private suspend fun importProject(state: AppState, cloudId: String): AppState {
        val project = json.decodeFromJsonElement<CloudProject>(call("getProject", cloudId))
        var localName = project.displayName
        if (state.projectList.contains(localName) && state.cloudLinks[localName]?.cloudId != cloudId) {
            var suffix = 2
            while (state.projectList.contains("${project.displayName} ($suffix)")) suffix++
            localName = "${project.displayName} ($suffix)"
        }
        val withProject = state.copy(
            projectList = if (state.projectList.contains(localName)) state.projectList else state.projectList + localName
        )
        return PhoneFormat.fromDataJson(withProject, localName, project.dataJson).let {
            it.copy(cloudLinks = it.cloudLinks + (localName to CloudLink(cloudId, project.updatedAtMillis, project.dataJson)))
        }
    }

    private suspend fun pullProject(state: AppState, name: String): AppState {
        val link = state.cloudLinks[name] ?: return state
        val project = json.decodeFromJsonElement<CloudProject>(call("getProject", link.cloudId))
        return PhoneFormat.fromDataJson(state, name, project.dataJson).let {
            it.copy(cloudLinks = it.cloudLinks + (name to link.copy(syncedAtMillis = project.updatedAtMillis, dataJson = project.dataJson)))
        }
    }

    // ---------------------------------------------------------------- plumbing

    private fun requireSignedIn() {
        if (!isSignedIn) throw CloudSyncException("Sign in with Google first")
    }

    private fun report(text: String, isError: Boolean = false) {
        message = text
        messageIsError = isError
    }

    private suspend fun work(quiet: Boolean = false, block: suspend () -> Unit) {
        mutex.withLock {
            isBusy = true
            try {
                block()
            } catch (e: Exception) {
                if (!quiet) report(e.message ?: "Cloud sync failed", isError = true)
            } finally {
                isBusy = false
            }
        }
    }

    private suspend fun call(function: String, arg: String = ""): JsonElement {
        if (!cloudSyncSupported) throw CloudSyncException("Cloud sync is available in the Android app and on the web")
        val reply = json.parseToJsonElement(callCloudBridge(function, arg)) as? JsonObject
            ?: throw CloudSyncException("Unexpected reply from the cloud")
        (reply["error"] as? JsonPrimitive)?.let { throw CloudSyncException(it.content) }
        return reply["ok"] ?: JsonNull
    }

    private inline fun <reified T> decodeOrNull(element: JsonElement): T? =
        if (element is JsonNull) null else json.decodeFromJsonElement<T>(element)
}

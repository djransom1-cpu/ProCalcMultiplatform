package com.djran.constructioncalculator

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Converts one project between this app's [AppState] lists and the Android app's cloud format:
 * `dataJson` is a flat object of the phone's SharedPreferences keys for that project, e.g.
 * `"stairs_<project>": "[{...}]"` (each list stored as a JSON *string*, exactly as
 * ProjectDataManager saves it) plus `"META_PROJECT_NAME"`.
 *
 * The two apps' entries don't have identical fields, so:
 *  - When writing, each web entry is laid over the phone entry it came from (same position), so
 *    phone-only fields (e.g. stair floor thickness) survive a round trip through the web.
 *  - Keys the web doesn't understand (hip/valley, squaring, blueprints, text notes...) are kept.
 *  - The web also stores its own full-fidelity copy under `web_state_<project>`, together with a
 *    signature of each phone list it wrote. Reading back, a list the phone hasn't touched since
 *    comes from that copy; a list the phone changed is translated from the phone's version.
 */
object PhoneFormat {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private const val META_NAME = "META_PROJECT_NAME"
    private const val WEB_STATE = "web_state"
    private const val JOB_NOTES = "job_notes"

    /** Builds the dataJson to upload for [name]. [baseDataJson] is the copy last pulled from the cloud. */
    fun toDataJson(state: AppState, name: String, baseDataJson: String?): String {
        val base = baseDataJson?.let { parseObject(it) }?.let { data ->
            val oldName = (data[META_NAME] as? JsonPrimitive)?.content
            if (oldName != null && oldName != name) rekey(data, oldName, name) else data
        } ?: JsonObject(emptyMap())

        val out = base.toMutableMap()
        out[META_NAME] = JsonPrimitive(name)
        val webState = mutableMapOf<String, JsonElement>()
        for (tool in TOOLS) {
            tool.write(state, name, base, out, webState)
        }
        writeCoordinates(state, name, base, out, webState)
        webState[JOB_NOTES] = buildJsonObject {
            put("items", json.encodeToJsonElement(ListSerializer(SerializablePath.serializer()), state.allJobNotes[name] ?: emptyList()))
        }
        out["${WEB_STATE}_$name"] = JsonPrimitive(JsonObject(webState).toString())
        return JsonObject(out).toString()
    }

    /** Replaces project [name]'s entries in [state] with what [dataJson] holds. */
    fun fromDataJson(state: AppState, name: String, dataJson: String): AppState {
        var data = parseObject(dataJson) ?: return state
        val keyName = (data[META_NAME] as? JsonPrimitive)?.content ?: name
        if (keyName != name) data = rekey(data, keyName, name)
        val webState = (data["${WEB_STATE}_$name"] as? JsonPrimitive)?.content?.let { parseObject(it) }

        var result = state
        for (tool in TOOLS) {
            result = tool.read(result, name, data, webState)
        }
        result = readCoordinates(result, name, data, webState)
        val notes = (webState?.get(JOB_NOTES) as? JsonObject)?.get("items")?.let {
            runCatching { json.decodeFromJsonElement(ListSerializer(SerializablePath.serializer()), it) }.getOrNull()
        }
        if (notes != null) {
            result = result.copy(allJobNotes = result.allJobNotes + (name to notes))
        }
        return result
    }

    // Site coordinates: the phone keeps one flat point list ("coordinates_<project>", a point with
    // isPathStart begins a new path; arcHeight bulges the segment to the next point) plus a
    // "closed_<project>" flag; the web keeps a list of paths.
    private const val COORDS = "coordinates"

    private fun writeCoordinates(state: AppState, name: String, base: JsonObject, out: MutableMap<String, JsonElement>, webState: MutableMap<String, JsonElement>) {
        val key = "${COORDS}_$name"
        val paths = state.allCoordinatePaths[name] ?: emptyList()
        if (paths.isEmpty() && base[key] == null) return
        val basePoints = parseArray((base[key] as? JsonPrimitive)?.content)
        val points = mutableListOf<JsonObject>()
        paths.forEach { path ->
            path.points.forEachIndexed { i, p ->
                val f = Fields(basePoints.getOrNull(points.size)?.toMutableMap() ?: mutableMapOf())
                f.default("num", (points.size + 1).toString())
                f.put("north", p.n.toString()); f.put("east", p.e.toString())
                f.put("elevation", p.elev.toString()); f.put("desc", p.description)
                f.put("isPathStart", i == 0)
                f.put("arcHeight", (path.arcHeights.getOrNull(i) ?: 0.0).toString())
                f.default("isExclusion", false)
                points.add(JsonObject(f.map))
            }
        }
        val phoneText = JsonArray(points).toString()
        out[key] = JsonPrimitive(phoneText)
        out["closed_$name"] = JsonPrimitive(paths.any { it.isClosed })
        webState[COORDS] = buildJsonObject {
            put("sig", phoneText.hashCode())
            put("items", json.encodeToJsonElement(ListSerializer(PlottedPath.serializer()), paths))
        }
    }

    private fun readCoordinates(state: AppState, name: String, data: JsonObject, webState: JsonObject?): AppState {
        val phoneText = (data["${COORDS}_$name"] as? JsonPrimitive)?.content
        val saved = webState?.get(COORDS) as? JsonObject
        val savedSig = (saved?.get("sig") as? JsonPrimitive)?.intOrNull
        val fromStash = if (saved != null && phoneText != null && savedSig == phoneText.hashCode()) {
            runCatching { json.decodeFromJsonElement(ListSerializer(PlottedPath.serializer()), saved["items"] ?: JsonArray(emptyList())) }.getOrNull()
        } else null
        val paths = fromStash ?: run {
            val closed = Fields(data.toMutableMap()).b("closed_$name")
            val groups = mutableListOf<MutableList<Fields>>()
            parseArray(phoneText).map { Fields(it.toMutableMap()) }.forEach { f ->
                if (groups.isEmpty() || f.b("isPathStart")) groups.add(mutableListOf())
                groups.last().add(f)
            }
            groups.mapIndexed { i, group ->
                PlottedPath(
                    id = entryId(i),
                    points = group.map { f ->
                        CoordinatePoint(
                            n = f.s("north").toDoubleOrNull() ?: 0.0,
                            e = f.s("east").toDoubleOrNull() ?: 0.0,
                            elev = f.s("elevation").toDoubleOrNull() ?: 0.0,
                            description = f.s("desc")
                        )
                    },
                    isTakeoff = !group.first().b("isExclusion"),
                    isClosed = closed,
                    arcHeights = group.map { it.s("arcHeight").toDoubleOrNull() ?: 0.0 }
                )
            }
        }
        val current = state.allCoordinatePaths
        if (paths.isEmpty() && current[name].isNullOrEmpty()) return state
        return state.copy(allCoordinatePaths = current + (name to paths))
    }

    /** Port of CloudSyncManager.rekeyProjectData so a renamed project keeps its phone-only keys. */
    private fun rekey(data: JsonObject, oldName: String, newName: String): JsonObject {
        val result = mutableMapOf<String, JsonElement>()
        for ((key, value) in data) {
            val newKey = when {
                key == META_NAME -> key
                key.endsWith("_$oldName") -> key.substringBeforeLast("_$oldName") + "_$newName"
                key.contains("project_$oldName") -> key.replace("project_$oldName", "project_$newName")
                key.endsWith(oldName) -> key.substringBeforeLast(oldName) + newName
                else -> key
            }
            result[newKey] = value
        }
        result[META_NAME] = JsonPrimitive(newName)
        return JsonObject(result)
    }

    private fun parseObject(text: String): JsonObject? =
        runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()

    private fun parseArray(text: String?): List<JsonObject> {
        if (text.isNullOrBlank()) return emptyList()
        return runCatching { (json.parseToJsonElement(text) as? JsonArray)?.mapNotNull { it as? JsonObject } }
            .getOrNull() ?: emptyList()
    }

    // ------------------------------------------------------------------ per-tool mapping

    private class Tool<T>(
        /** Phone list key prefix: the phone stores the list at "<prefix>_<project>". */
        val prefix: String,
        val serializer: KSerializer<T>,
        val get: (AppState) -> Map<String, List<T>>,
        val set: (AppState, Map<String, List<T>>) -> AppState,
        /** Web entry -> phone object, laid over the phone object it came from (if any). */
        val toPhone: (T, Fields) -> Unit,
        /** Phone object -> web entry. */
        val fromPhone: (Fields, Int) -> T
    ) {
        fun write(state: AppState, name: String, base: JsonObject, out: MutableMap<String, JsonElement>, webState: MutableMap<String, JsonElement>) {
            val key = "${prefix}_$name"
            val items = get(state)[name] ?: emptyList()
            val baseObjects = parseArray((base[key] as? JsonPrimitive)?.content)
            if (items.isEmpty() && base[key] == null) return
            val phoneObjects = items.mapIndexed { i, item ->
                val fields = Fields(baseObjects.getOrNull(i)?.toMutableMap() ?: mutableMapOf())
                toPhone(item, fields)
                JsonObject(fields.map)
            }
            val phoneText = JsonArray(phoneObjects).toString()
            out[key] = JsonPrimitive(phoneText)
            webState[prefix] = buildJsonObject {
                put("sig", phoneText.hashCode())
                put("items", json.encodeToJsonElement(ListSerializer(serializer), items))
            }
        }

        fun read(state: AppState, name: String, data: JsonObject, webState: JsonObject?): AppState {
            val phoneText = (data["${prefix}_$name"] as? JsonPrimitive)?.content
            val saved = webState?.get(prefix) as? JsonObject
            val savedSig = (saved?.get("sig") as? JsonPrimitive)?.intOrNull
            val items: List<T> = if (saved != null && phoneText != null && savedSig == phoneText.hashCode()) {
                runCatching { json.decodeFromJsonElement(ListSerializer(serializer), saved["items"] ?: JsonArray(emptyList())) }
                    .getOrElse { parseArray(phoneText).mapIndexed { i, o -> fromPhone(Fields(o.toMutableMap()), i) } }
            } else {
                parseArray(phoneText).mapIndexed { i, o -> fromPhone(Fields(o.toMutableMap()), i) }
            }
            val current = get(state)
            if (items.isEmpty() && current[name].isNullOrEmpty()) return state
            return set(state, current + (name to items))
        }
    }

    /** Mutable view of one phone JSON object with lenient getters (the phone's org.json coerces types). */
    private class Fields(val map: MutableMap<String, JsonElement>) {
        fun s(key: String, default: String = ""): String = when (val v = map[key]) {
            null, JsonNull -> default
            is JsonPrimitive -> v.content
            else -> v.toString()
        }
        fun b(key: String, default: Boolean = false): Boolean {
            val v = map[key] as? JsonPrimitive ?: return default
            return v.booleanOrNull ?: v.content.equals("true", ignoreCase = true)
        }
        fun i(key: String, default: Int = 0): Int {
            val v = map[key] as? JsonPrimitive ?: return default
            return v.intOrNull ?: v.content.toDoubleOrNull()?.toInt() ?: default
        }
        fun put(key: String, value: String) { map[key] = JsonPrimitive(value) }
        fun put(key: String, value: Boolean) { map[key] = JsonPrimitive(value) }
        fun put(key: String, value: Int) { map[key] = JsonPrimitive(value) }
        /** Sets [key] only if the phone object doesn't have it yet (fields the web can't supply). */
        fun default(key: String, value: String) { if (map[key] == null) put(key, value) }
        fun default(key: String, value: Boolean) { if (map[key] == null) put(key, value) }
        fun default(key: String, value: Int) { if (map[key] == null) put(key, value) }
    }

    private fun entryId(i: Int) = (i + 1).toString()

    private val LUMBER_DEPTHS = listOf(
        "2x4" to "3 1/2", "2x6" to "5 1/2", "2x8" to "7 1/4", "2x10" to "9 1/4", "2x12" to "11 1/4"
    )
    private fun lumberForDepth(depth: String) = LUMBER_DEPTHS.firstOrNull { it.second == depth.trim() }?.first
    private fun depthForLumber(lumber: String) = LUMBER_DEPTHS.firstOrNull { it.first == lumber.trim() }?.second

    /** "12 - 2x4 x 6' 8\"" -> ("12", "6' 8\"") as the phone writes framing members. */
    private fun splitMember(text: String): Pair<String, String> {
        val count = text.substringBefore(" - ", "").trim()
        val length = text.substringAfterLast(" x ", text).trim()
        return count to length
    }

    private val TOOLS: List<Tool<*>> = listOf(
        Tool(
            "stairs", StairEntry.serializer(), { it.allStairEntries }, { s, m -> s.copy(allStairEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("rise", e.rise); f.put("riserCount", e.riserCount)
                f.put("actualRise", e.actualRise); f.put("stringerLen", e.stringerLen); f.put("headroom", e.headroom)
                f.put("treadCount", e.treadCount); f.put("stringerRun", e.stringerRun); f.put("totalRun", e.totalRun)
                f.put("treadsOut", e.treadsOut); f.put("distOut", e.distOut); f.put("angle", e.angleDeg)
                f.default("run", ""); f.default("floorThick", "10 3/4"); f.default("nosing", "1")
                f.default("showOpening", true); f.default("stringerSize", "2x12")
            },
            fromPhone = { f, i ->
                StairEntry(
                    id = entryId(i), name = f.s("name"), rise = f.s("rise"), riserCount = f.s("riserCount"),
                    actualRise = f.s("actualRise"), treadCount = f.s("treadCount"), stringerRun = f.s("stringerRun"),
                    totalRun = f.s("totalRun"), stringerLen = f.s("stringerLen"), angleDeg = f.s("angle"),
                    treadsOut = f.s("treadsOut"), distOut = f.s("distOut"), headroom = f.s("headroom", "Clear")
                )
            }
        ),
        Tool(
            "rafters", RafterEntry.serializer(), { it.allRafterEntries }, { s, m -> s.copy(allRafterEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("span", e.span); f.put("pitch", e.pitch)
                f.put("run", e.commonRun); f.put("rise", e.commonRise); f.put("length", e.commonLen)
                f.put("overall", e.commonOverall); f.put("ridge", e.ridge); f.put("heel", e.heel)
                f.put("overhang", e.overhang); f.put("totRise", e.commonOARise)
                f.put("hipRun", e.hipRun); f.put("hipLen", e.hipLen); f.put("hipPitch", e.hipPitch)
                lumberForDepth(e.depth)?.let { f.put("lumberSize", it) }
                f.default("tailLen", ""); f.default("leftEnd", 0); f.default("rightEnd", 0)
                f.default("lumberSize", "2x8")
            },
            fromPhone = { f, i ->
                RafterEntry(
                    id = entryId(i), name = f.s("name"), span = f.s("span"), pitch = f.s("pitch"),
                    ridge = f.s("ridge", "1 1/2"), depth = depthForLumber(f.s("lumberSize", "2x8")) ?: "7 1/4",
                    heel = f.s("heel", "4"), overhang = f.s("overhang", "12"),
                    commonLen = f.s("length"), commonOverall = f.s("overall"), commonRun = f.s("run"),
                    commonRise = f.s("rise"), commonOARise = f.s("totRise"), commonPlumbSeat = "",
                    hipLen = f.s("hipLen"), hipOverall = "", hipRun = f.s("hipRun"), hipRise = "",
                    hipOARise = "", hipPitch = f.s("hipPitch"), hipPlumbSeat = "", jack16 = "", jack24 = ""
                )
            }
        ),
        Tool(
            "roof_master", RoofEntry.serializer(), { it.allRoofEntries }, { s, m -> s.copy(allRoofEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("span", e.span); f.put("pitch", e.pitch); f.put("bldgLen", e.bldgLen)
                f.put("leftEnd", e.leftEnd); f.put("rightEnd", e.rightEnd); f.put("rafterInfo", e.commonInfo)
                f.put("hipInfo", e.hipInfo); f.put("valleyInfo", e.valleyInfo); f.put("ridgeInfo", e.ridgeInfo)
                f.put("subFascia", e.subFascia)
                f.default("run", ""); f.default("rise", ""); f.default("length", ""); f.default("overall", "")
            },
            fromPhone = { f, i ->
                RoofEntry(
                    id = entryId(i), name = f.s("name"), span = f.s("span"), bldgLen = f.s("bldgLen"),
                    pitch = f.s("pitch"), leftEnd = f.i("leftEnd"), rightEnd = f.i("rightEnd"),
                    commonInfo = f.s("rafterInfo"), hipInfo = f.s("hipInfo"), valleyInfo = f.s("valleyInfo"),
                    ridgeInfo = f.s("ridgeInfo"), subFascia = f.s("subFascia")
                )
            }
        ),
        Tool(
            "handrails", HandrailEntry.serializer(), { it.allHandrailEntries }, { s, m -> s.copy(allHandrailEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("railLen", e.length); f.put("spindleCount", e.spindles)
                f.put("onCenter", e.spacing); f.put("gap", e.gap)
            },
            fromPhone = { f, i ->
                HandrailEntry(
                    id = entryId(i), name = f.s("name"), length = f.s("railLen"), spindleWidth = "1 1/2",
                    maxOpening = "4", spindles = f.s("spindleCount"), spacing = f.s("onCenter"), gap = f.s("gap")
                )
            }
        ),
        Tool(
            "curves", ArcEntry.serializer(), { it.allArcEntries }, { s, m -> s.copy(allArcEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("chord", e.chord); f.put("height", e.height)
                f.put("radius", e.radius); f.put("arcLen", e.arcLength); f.put("angle", e.arcAngle)
            },
            fromPhone = { f, i ->
                ArcEntry(
                    id = entryId(i), name = f.s("name"), chord = f.s("chord"), height = f.s("height"),
                    radius = f.s("radius"), arcLength = f.s("arcLen"), arcAngle = f.s("angle")
                )
            }
        ),
        Tool(
            "gazebo", GazeboEntry.serializer(), { it.allGazeboEntries }, { s, m -> s.copy(allGazeboEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("sides", e.sides); f.put("span", e.diameter); f.put("pitch", e.pitch)
                f.put("mainLen", e.commonRafterLen); f.put("hipLen", e.hipLen); f.put("ridgeSpan", e.ridgeSpan)
                f.put("overhang", e.overhang); f.put("heel", e.heel)
                f.default("footprintInfo", ""); f.default("rafterInfo", ""); f.default("hipInfo", "")
                f.default("joistOC", "16")
            },
            fromPhone = { f, i ->
                GazeboEntry(
                    id = entryId(i), name = f.s("name"), sides = f.s("sides"), diameter = f.s("span"),
                    ridgeSpan = f.s("ridgeSpan", "0"), pitch = f.s("pitch"), overhang = f.s("overhang", "12"),
                    heel = f.s("heel", "6"), sideLength = "", apothem = "", perimeter = "", interiorAngle = "",
                    miter = "", area = "", verticalRise = "", commonRafterLen = f.s("mainLen"), commonOverall = "",
                    hipLen = f.s("hipLen"), hipOverall = "", commonPlumbSeat = "", hipPlumbSeat = "", ridgeOffset = ""
                )
            }
        ),
        Tool(
            "circ_stairs", CircularStairEntry.serializer(), { it.allCircularStairEntries }, { s, m -> s.copy(allCircularStairEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("rise", e.totalRise); f.put("radius", e.radius)
                f.put("treadWidth", e.walkTread); f.put("numTreads", e.numTreads)
                f.put("straightTreads", e.straightTreads); f.put("straightRun", e.straightRun)
                f.put("nosing", e.nosing); f.put("circleSize", e.circleSize)
                f.put("outerArc", e.outerArcTread); f.put("innerArc", e.innerArcTread)
                f.put("outsideRadius", e.outsideRadius); f.put("anglePerTread", e.anglePerTread)
                f.default("totalOuterCirc", ""); f.default("totalInnerCirc", "")
            },
            fromPhone = { f, i ->
                CircularStairEntry(
                    id = entryId(i), name = f.s("name"), totalRise = f.s("rise"), radius = f.s("radius"),
                    walkTread = f.s("treadWidth"), numTreads = f.s("numTreads"),
                    straightTreads = f.s("straightTreads", "0"), straightRun = f.s("straightRun", "10"),
                    nosing = f.s("nosing", "1"), circleSize = f.s("circleSize", "2/4"),
                    outerArcTread = f.s("outerArc"), innerArcTread = f.s("innerArc"),
                    outsideRadius = f.s("outsideRadius"), anglePerTread = f.s("anglePerTread")
                )
            }
        ),
        Tool(
            "trig", TrigEntry.serializer(), { it.allTrigEntries }, { s, m -> s.copy(allTrigEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("angle", e.angleA)
                f.put("results", "Side A: ${e.sideA}\nSide B: ${e.sideB}\nHypotenuse: ${e.hypotenuse}\nAngle A: ${e.angleA}\nAngle B: ${e.angleB}")
            },
            fromPhone = { f, i ->
                val lines = f.s("results").lines().associate { it.substringBefore(":").trim() to it.substringAfter(":", "").trim() }
                TrigEntry(
                    id = entryId(i), name = f.s("name"), sideA = lines["Side A"] ?: "", sideB = lines["Side B"] ?: "",
                    hypotenuse = lines["Hypotenuse"] ?: "", angleA = f.s("angle"), angleB = lines["Angle B"] ?: ""
                )
            }
        ),
        Tool(
            "crown", CrownEntry.serializer(), { it.allCrownEntries }, { s, m -> s.copy(allCrownEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("spring", e.spring); f.put("corner", e.wallAngle)
                f.put("miter", e.miter); f.put("bevel", e.bevel)
            },
            fromPhone = { f, i ->
                CrownEntry(
                    id = entryId(i), name = f.s("name"), spring = f.s("spring"), wallAngle = f.s("corner"),
                    miter = f.s("miter"), bevel = f.s("bevel")
                )
            }
        ),
        Tool(
            "concrete", ConcreteEntry.serializer(), { it.allConcreteEntries }, { s, m -> s.copy(allConcreteEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("qty", e.qty); f.put("dims", e.dimensions); f.put("cuYd", e.volume)
                f.put("rebarFt", e.rebar); f.put("rebarOC", e.spacing)
                f.default("cuFt", ""); f.default("rebarMode", 0); f.default("barsTop", "0"); f.default("barsBottom", "2")
            },
            fromPhone = { f, i ->
                ConcreteEntry(
                    id = entryId(i), name = f.s("name"), qty = f.s("qty", "1"), volume = f.s("cuYd"),
                    rebar = f.s("rebarFt", "0"), spacing = f.s("rebarOC", "12"), dimensions = f.s("dims")
                )
            }
        ),
        Tool(
            "masonry", MasonryEntry.serializer(), { it.allMasonryEntries }, { s, m -> s.copy(allMasonryEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("qty", e.qty); f.put("sqFt", e.sqFt); f.put("type", e.type)
                f.put("units", e.units); f.put("mortar", e.mortar); f.put("sand", e.sand)
                f.put("length", e.length); f.put("height", e.height)
            },
            fromPhone = { f, i ->
                MasonryEntry(
                    id = entryId(i), name = f.s("name"), qty = f.s("qty", "1"), sqFt = f.s("sqFt"), type = f.s("type", "Block"),
                    units = f.s("units", "0"), mortar = f.s("mortar", "0"), sand = f.s("sand", "0"),
                    length = f.s("length"), height = f.s("height")
                )
            }
        ),
        Tool(
            "flooring", FloorEntry.serializer(), { it.allFloorEntries }, { s, m -> s.copy(allFloorEntries = m) },
            toPhone = { e, f ->
                f.put("length", e.length); f.put("width", e.width); f.put("joistOC", e.spacing)
                f.put("joistCount", e.joists); f.put("plySheets", e.sheets); f.put("rimJoist", e.rimJoist)
                f.default("joistSize", "2x10"); f.default("numBeams", "0"); f.default("direction", "Span Length")
                f.default("plyThick", "3/4"); f.default("joistLen", "")
            },
            fromPhone = { f, i ->
                FloorEntry(
                    id = entryId(i), name = "Floor ${i + 1}", length = f.s("length"), width = f.s("width"),
                    spacing = f.s("joistOC"), joists = f.s("joistCount"), sheets = f.s("plySheets"), rimJoist = f.s("rimJoist")
                )
            }
        ),
        Tool(
            "walls", WallEntry.serializer(), { it.allWallEntries }, { s, m -> s.copy(allWallEntries = m) },
            toPhone = { e, f ->
                f.put("length", e.length); f.put("height", e.height); f.put("oc", e.spacing)
                f.put("totalStuds", e.studs); f.put("platesFt", e.plates); f.put("plySheets", e.sheets)
            },
            fromPhone = { f, i ->
                WallEntry(
                    id = entryId(i), name = "Wall ${i + 1}", length = f.s("length"), height = f.s("height"),
                    spacing = f.s("oc"), studs = f.s("totalStuds"), plates = f.s("platesFt"), sheets = f.s("plySheets")
                )
            }
        ),
        Tool(
            "drywall", DrywallEntry.serializer(), { it.allDrywallEntries }, { s, m -> s.copy(allDrywallEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("area", e.area); f.put("sheetSize", e.sheetSize)
                f.put("sheets", e.sheets); f.put("mud", e.mud); f.put("tape", e.tape)
            },
            fromPhone = { f, i ->
                DrywallEntry(
                    id = entryId(i), name = f.s("name"), area = f.s("area"), sheets = f.s("sheets"),
                    sheetSize = f.s("sheetSize", "4x8"), mud = f.s("mud"), tape = f.s("tape")
                )
            }
        ),
        Tool(
            "deck", DeckEntry.serializer(), { it.allDeckEntries }, { s, m -> s.copy(allDeckEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("length", e.length); f.put("width", e.width)
                f.put("boardWidth", e.boardWidth); f.put("screwCount", e.fasteners)
                f.default("totalLF", e.boards)
            },
            fromPhone = { f, i ->
                DeckEntry(
                    id = entryId(i), name = f.s("name"), length = f.s("length"), width = f.s("width"),
                    boardWidth = f.s("boardWidth"), gap = "1/8", boards = f.s("totalLF"), fasteners = f.s("screwCount")
                )
            }
        ),
        Tool(
            "columns", ColumnEntry.serializer(), { it.allColumnEntries }, { s, m -> s.copy(allColumnEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("diameter", e.diameter); f.put("height", e.height)
                f.put("volume", e.volume); f.put("area", e.surfaceArea); f.put("circumference", e.circumference)
                f.default("qty", "1"); f.default("rebarFt", "0")
            },
            fromPhone = { f, i ->
                ColumnEntry(
                    id = entryId(i), name = f.s("name"), diameter = f.s("diameter"), height = f.s("height"),
                    volume = f.s("volume"), surfaceArea = f.s("area"), circumference = f.s("circumference")
                )
            }
        ),
        Tool(
            "pineline", PineLineEntry.serializer(), { it.allPineLineEntries }, { s, m -> s.copy(allPineLineEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("pitch", e.pitch); f.put("heel", e.heel); f.put("wall", e.wall)
                f.put("hasBrick", (e.brick.trim().toDoubleOrNull() ?: 0.0) > 0.0)
                f.put("fascia", e.fascia); f.put("frieze", e.frieze); f.put("overhang", e.overhang)
                f.put("reveal", e.reveal); f.put("pineLineV", e.drop)
                f.default("lumber", "2x6"); f.default("fasciaThick", "0.75"); f.default("friezeThick", "0.75")
                f.default("pineLineH", "")
            },
            fromPhone = { f, i ->
                PineLineEntry(
                    id = entryId(i), name = f.s("name"), pitch = f.s("pitch"), heel = f.s("heel"), wall = f.s("wall"),
                    brick = if (f.b("hasBrick")) "4" else "0", frieze = f.s("frieze"), overhang = f.s("overhang"),
                    fascia = f.s("fascia"), reveal = f.s("reveal", "0.5"), drop = f.s("pineLineV")
                )
            }
        ),
        Tool(
            "openings", FramingEntry.serializer(), { it.allFramingEntries }, { s, m -> s.copy(allFramingEntries = m) },
            toPhone = { e, f ->
                f.put("name", e.name); f.put("qty", e.qty); f.put("notes", e.notes)
                f.put("header", "${e.headerNominal} x ${e.headerLength}")
                f.put("jack", "${e.jackCount} - ${e.studSize} x ${e.jackLength}")
                f.put("sill", "${e.sillCount} - ${e.studSize} x ${e.sillLength}")
                f.put("lower", "${e.lowerCrippleCount} - ${e.studSize} x ${e.lowerCrippleLength}")
                f.put("upper", "${e.upperCrippleCount} - ${e.studSize} x ${e.upperCrippleLength}")
            },
            fromPhone = { f, i ->
                val (jackCount, jackLength) = splitMember(f.s("jack"))
                val (sillCount, sillLength) = splitMember(f.s("sill"))
                val (lowerCount, lowerLength) = splitMember(f.s("lower"))
                val (upperCount, upperLength) = splitMember(f.s("upper"))
                val header = f.s("header")
                FramingEntry(
                    id = entryId(i), name = f.s("name"), wallHeight = "", headerHeightBottom = "", headerDepth = "",
                    roWidth = "", roHeight = "", qty = f.s("qty", "1"), notes = f.s("notes"),
                    headerNominal = header.substringBefore(" x ", "").trim(),
                    headerLength = header.substringAfter(" x ", header).trim(),
                    jackCount = jackCount, jackLength = jackLength, sillCount = sillCount, sillLength = sillLength,
                    lowerCrippleCount = lowerCount, lowerCrippleLength = lowerLength,
                    upperCrippleCount = upperCount, upperCrippleLength = upperLength
                )
            }
        )
    )
}

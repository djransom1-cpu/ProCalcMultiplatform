package com.djran.constructioncalculator

import androidx.compose.runtime.Composable

/*
 * Project backup files (.ccp), the same as the Android app's EXPORT / IMPORT PROJECT (.ccp):
 * a zip holding data.json (the project's keys, the same JSON the cloud sync uses) and, from the
 * phone, a blueprints/ folder. This app has no blueprints, so it writes data.json only and skips
 * blueprints when reading.
 */
object ProjectPackage {
    fun export(state: AppState, name: String): ByteArray =
        Zip.write(listOf("data.json" to PhoneFormat.toDataJson(state, name, null).encodeToByteArray()))

    /** Adds the project in [bytes] to [state]; a name already in use gets " (2)", " (3)"... Returns null if it isn't a project file. */
    fun import(state: AppState, bytes: ByteArray): Pair<AppState, String>? {
        val data = runCatching { Zip.read(bytes)["data.json"] }.getOrNull()?.decodeToString() ?: return null
        val base = Regex("\"META_PROJECT_NAME\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(data)?.groupValues?.get(1)
            ?.replace("\\\"", "\"")?.replace("\\\\", "\\")?.ifBlank { null } ?: "Imported Project"
        var name = base
        var n = 2
        while (state.projectList.contains(name)) name = "$base (${n++})"
        val withProject = state.copy(projectList = state.projectList + name)
        return PhoneFormat.fromDataJson(withProject, name, data) to name
    }
}

/** Opens the system file chooser; [onPicked] gets the file's bytes (not called if cancelled). */
@Composable
expect fun rememberFilePicker(onPicked: (ByteArray) -> Unit): () -> Unit

/** Minimal zip: writes stored entries, reads stored and deflated entries. */
object Zip {
    private val crcTable = IntArray(256) { n ->
        var c = n
        repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1 }
        c
    }

    fun crc32(b: ByteArray): Int {
        var c = -1
        for (x in b) c = crcTable[(c xor x.toInt()) and 0xFF] xor (c ushr 8)
        return c.inv()
    }

    private class Out {
        val buf = ArrayList<Byte>()
        fun u16(v: Int) { buf.add(v.toByte()); buf.add((v shr 8).toByte()) }
        fun u32(v: Int) { u16(v and 0xFFFF); u16(v ushr 16) }
        fun bytes(b: ByteArray) { b.forEach { buf.add(it) } }
    }

    fun write(entries: List<Pair<String, ByteArray>>): ByteArray {
        val o = Out()
        val central = Out()
        entries.forEach { (name, data) ->
            val nb = name.encodeToByteArray()
            val crc = crc32(data)
            val offset = o.buf.size
            o.u32(0x04034b50); o.u16(20); o.u16(0x0800); o.u16(0); o.u16(0); o.u16(0x21)
            o.u32(crc); o.u32(data.size); o.u32(data.size); o.u16(nb.size); o.u16(0); o.bytes(nb); o.bytes(data)
            central.u32(0x02014b50); central.u16(20); central.u16(20); central.u16(0x0800); central.u16(0); central.u16(0); central.u16(0x21)
            central.u32(crc); central.u32(data.size); central.u32(data.size); central.u16(nb.size); central.u16(0); central.u16(0)
            central.u16(0); central.u16(0); central.u32(0); central.u32(offset); central.bytes(nb)
        }
        val cdStart = o.buf.size
        o.bytes(central.buf.toByteArray())
        o.u32(0x06054b50); o.u16(0); o.u16(0); o.u16(entries.size); o.u16(entries.size)
        o.u32(central.buf.size); o.u32(cdStart); o.u16(0)
        return o.buf.toByteArray()
    }

    private fun ByteArray.u16(i: Int) = (this[i].toInt() and 0xFF) or ((this[i + 1].toInt() and 0xFF) shl 8)
    private fun ByteArray.u32(i: Int) = u16(i) or (u16(i + 2) shl 16)

    /** Reads entries through the central directory (works with data descriptors, as Java's ZipOutputStream writes). */
    fun read(b: ByteArray): Map<String, ByteArray> {
        var eocd = b.size - 22
        while (eocd >= 0 && b.u32(eocd) != 0x06054b50) eocd--
        require(eocd >= 0) { "not a zip" }
        val count = b.u16(eocd + 10)
        var p = b.u32(eocd + 16)
        val out = LinkedHashMap<String, ByteArray>()
        repeat(count) {
            require(b.u32(p) == 0x02014b50)
            val method = b.u16(p + 10)
            val compSize = b.u32(p + 20)
            val nameLen = b.u16(p + 28); val extraLen = b.u16(p + 30); val commentLen = b.u16(p + 32)
            val local = b.u32(p + 42)
            val name = b.copyOfRange(p + 46, p + 46 + nameLen).decodeToString()
            val dataStart = local + 30 + b.u16(local + 26) + b.u16(local + 28)
            val raw = b.copyOfRange(dataStart, dataStart + compSize)
            out[name] = when (method) { 0 -> raw; 8 -> Inflate.inflate(raw); else -> error("zip method $method") }
            p += 46 + nameLen + extraLen + commentLen
        }
        return out
    }
}

/** Raw DEFLATE decoder (RFC 1951), small and slow but plenty for a project file. */
object Inflate {
    private class Bits(val src: ByteArray) {
        var pos = 0; var bit = 0
        fun get(n: Int): Int {
            var v = 0
            for (i in 0 until n) {
                val b = (src[pos].toInt() shr bit) and 1
                v = v or (b shl i)
                if (++bit == 8) { bit = 0; pos++ }
            }
            return v
        }
        fun align() { if (bit != 0) { bit = 0; pos++ } }
    }

    private class Huff(lengths: IntArray) {
        val counts = IntArray(16); val symbols = IntArray(lengths.size)
        init {
            lengths.forEach { counts[it]++ }
            counts[0] = 0
            val offs = IntArray(16)
            for (i in 1 until 16) offs[i] = offs[i - 1] + counts[i - 1]
            lengths.forEachIndexed { s, l -> if (l != 0) symbols[offs[l]++] = s }
        }
        fun decode(b: Bits): Int {
            var code = 0; var first = 0; var index = 0
            for (len in 1 until 16) {
                code = code or b.get(1)
                val count = counts[len]
                if (code - count < first) return symbols[index + (code - first)]
                index += count; first += count; first = first shl 1; code = code shl 1
            }
            error("bad code")
        }
    }

    private val LBASE = intArrayOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258)
    private val LEXT = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0)
    private val DBASE = intArrayOf(1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577)
    private val DEXT = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13)
    private val ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

    fun inflate(src: ByteArray): ByteArray {
        val b = Bits(src)
        val out = ArrayList<Byte>(src.size * 4)
        do {
            val last = b.get(1)
            when (b.get(2)) {
                0 -> {
                    b.align()
                    val len = (src[b.pos].toInt() and 0xFF) or ((src[b.pos + 1].toInt() and 0xFF) shl 8)
                    b.pos += 4
                    for (i in 0 until len) out.add(src[b.pos + i])
                    b.pos += len
                }
                1 -> {
                    val l = IntArray(288) { when { it < 144 -> 8; it < 256 -> 9; it < 280 -> 7; else -> 8 } }
                    block(b, out, Huff(l), Huff(IntArray(30) { 5 }))
                }
                2 -> {
                    val hlit = b.get(5) + 257; val hdist = b.get(5) + 1; val hclen = b.get(4) + 4
                    val cl = IntArray(19)
                    for (i in 0 until hclen) cl[ORDER[i]] = b.get(3)
                    val ch = Huff(cl)
                    val lens = IntArray(hlit + hdist)
                    var i = 0
                    while (i < hlit + hdist) {
                        when (val sym = ch.decode(b)) {
                            in 0..15 -> lens[i++] = sym
                            16 -> { val prev = lens[i - 1]; repeat(3 + b.get(2)) { lens[i++] = prev } }
                            17 -> repeat(3 + b.get(3)) { lens[i++] = 0 }
                            else -> repeat(11 + b.get(7)) { lens[i++] = 0 }
                        }
                    }
                    block(b, out, Huff(lens.copyOfRange(0, hlit)), Huff(lens.copyOfRange(hlit, hlit + hdist)))
                }
                else -> error("bad block")
            }
        } while (last == 0)
        return out.toByteArray()
    }

    private fun block(b: Bits, out: ArrayList<Byte>, lit: Huff, dist: Huff) {
        while (true) {
            val sym = lit.decode(b)
            when {
                sym < 256 -> out.add(sym.toByte())
                sym == 256 -> return
                else -> {
                    val s = sym - 257
                    val len = LBASE[s] + b.get(LEXT[s])
                    val ds = dist.decode(b)
                    val d = DBASE[ds] + b.get(DEXT[ds])
                    val start = out.size - d
                    for (i in 0 until len) out.add(out[start + i])
                }
            }
        }
    }
}

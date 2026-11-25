package app.assistantkeeper

import android.content.Context

enum class Result { NO_TARGET, NO_ROOT, OK, RESTORED }

// Current system state: settings + running process names
class Snapshot(
    val assistant: String,
    val vis: String,
    val vrs: String,
    private val procs: Set<String>
) {
    fun alive(pkg: String) = procs.any { it == pkg || it.startsWith("$pkg:") }
}

object Keeper {
    private const val GET = "settings get secure"

    private fun value(s: String) = s.trim().let { if (it == "null") "" else it }

    fun snapshot(): Snapshot? {
        val cmd = "$GET assistant; $GET voice_interaction_service; " +
            "$GET voice_recognition_service; ps -A -o NAME; true"
        val out = Root.run(cmd) ?: return null
        if (out.size < 3) return null
        return Snapshot(
            value(out[0]), value(out[1]), value(out[2]),
            out.drop(3).map { it.trim() }.toSet()
        )
    }

    private fun put(key: String, v: String) =
        if (v.isEmpty()) "settings delete secure $key" else "settings put secure $key '$v'"

    // Restore target if it was replaced or its service process died
    fun restore(c: Context): Result {
        val t = Store.load(c) ?: return Result.NO_TARGET
        val s = snapshot() ?: return Result.NO_ROOT

        val same = s.assistant == t.assistant && s.vis == t.vis
        val dead = t.vis.isNotEmpty() && !s.alive(t.vis.substringBefore('/'))
        if (same && !dead) return Result.OK

        val cmds = ArrayList<String>()
        if (same) {
            // Same value would not trigger a rebind
            cmds += put("voice_interaction_service", "")
            cmds += "sleep 0.5"
        }
        cmds += put("voice_recognition_service", t.vrs)
        cmds += put("assistant", t.assistant)
        cmds += put("voice_interaction_service", t.vis)
        Root.run(cmds.joinToString("; ") + "; true") ?: return Result.NO_ROOT
        return Result.RESTORED
    }
}

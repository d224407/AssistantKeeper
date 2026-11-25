package app.assistantkeeper

import android.content.Context

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("target", Context.MODE_PRIVATE)

    fun save(c: Context, a: Assistant) {
        prefs(c).edit()
            .putString("label", a.label)
            .putString("assistant", a.assistant)
            .putString("vis", a.vis)
            .putString("vrs", a.vrs)
            .putBoolean("set", true)
            .apply()
    }

    fun load(c: Context): Assistant? {
        val p = prefs(c)
        if (!p.getBoolean("set", false)) return null
        return Assistant(
            p.getString("label", "") ?: "",
            p.getString("assistant", "") ?: "",
            p.getString("vis", "") ?: "",
            p.getString("vrs", "") ?: ""
        )
    }
}

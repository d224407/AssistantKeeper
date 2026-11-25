package app.assistantkeeper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import org.xmlpull.v1.XmlPullParser

// Values of the three secure settings that define an assistant
data class Assistant(
    val label: String,
    val assistant: String,
    val vis: String,
    val vrs: String
)

object Assistants {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    fun list(c: Context): List<Assistant> {
        val pm = c.packageManager
        val out = ArrayList<Assistant>()
        val pkgs = HashSet<String>()

        // Voice interaction services
        val svc = pm.queryIntentServices(
            Intent("android.service.voice.VoiceInteractionService"),
            PackageManager.GET_META_DATA
        )
        for (r in svc) {
            val si = r.serviceInfo
            if (si.permission != "android.permission.BIND_VOICE_INTERACTION") continue
            val cn = ComponentName(si.packageName, si.name).flattenToString()
            out += Assistant(label(pm, r.loadLabel(pm), si.packageName), cn, cn, recognizer(pm, si))
            pkgs += si.packageName
        }

        // Plain assist activities
        for (r in pm.queryIntentActivities(Intent(Intent.ACTION_ASSIST), 0)) {
            val ai = r.activityInfo
            if (ai.packageName in pkgs) continue
            val cn = ComponentName(ai.packageName, ai.name).flattenToString()
            out += Assistant(label(pm, r.loadLabel(pm), ai.packageName), cn, "", "")
            pkgs += ai.packageName
        }
        return out.sortedBy { it.label.lowercase() }
    }

    private fun label(pm: PackageManager, name: CharSequence, pkg: String): String {
        val app = try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
        return if (name.toString() == app) "$app ($pkg)" else "$app - $name ($pkg)"
    }

    // Read recognitionService from the voice-interaction-service metadata
    private fun recognizer(pm: PackageManager, si: ServiceInfo): String {
        try {
            si.loadXmlMetaData(pm, "android.voice_interaction")?.use { p ->
                while (p.next() != XmlPullParser.END_DOCUMENT) {
                    if (p.eventType == XmlPullParser.START_TAG && p.name == "voice-interaction-service") {
                        val rs = p.getAttributeValue(ANDROID_NS, "recognitionService")
                        if (!rs.isNullOrEmpty()) {
                            return ComponentName(si.packageName, rs).flattenToString()
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return ""
    }
}

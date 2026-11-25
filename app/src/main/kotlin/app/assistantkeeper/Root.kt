package app.assistantkeeper

object Root {
    // Run a command as root; returns stdout lines, or null if su failed
    fun run(cmd: String): List<String>? = try {
        val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readLines()
        if (p.waitFor() == 0) out else null
    } catch (_: Exception) {
        null
    }
}

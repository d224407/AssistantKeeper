package app.assistantkeeper

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var list: ListView
    private var items: List<Assistant> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, pad)
        }
        val detect = Button(this).apply {
            setText(R.string.detect)
            setOnClickListener { detectCurrent() }
        }
        list = ListView(this).apply { choiceMode = ListView.CHOICE_MODE_SINGLE }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(detect, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(list, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            setOnApplyWindowInsetsListener { v, w ->
                val b = bars(w)
                v.setPadding(pad + b[0], pad + b[1], pad + b[2], pad + b[3])
                w
            }
        }
        setContentView(root)

        items = Assistants.list(this)
        list.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_single_choice,
            items.map { it.label }
        )
        list.setOnItemClickListener { _, _, i, _ ->
            Store.save(this, items[i])
            refresh()
        }
        refresh()
    }

    @Suppress("DEPRECATION")
    private fun bars(w: WindowInsets): IntArray =
        if (Build.VERSION.SDK_INT >= 30) {
            val i = w.getInsets(WindowInsets.Type.systemBars())
            intArrayOf(i.left, i.top, i.right, i.bottom)
        } else {
            intArrayOf(
                w.systemWindowInsetLeft, w.systemWindowInsetTop,
                w.systemWindowInsetRight, w.systemWindowInsetBottom
            )
        }

    private fun refresh() {
        val t = Store.load(this)
        status.text = getString(R.string.target, t?.label ?: getString(R.string.none))
        val i = if (t == null) -1 else items.indexOfFirst { it.assistant == t.assistant }
        list.clearChoices()
        if (i >= 0) list.setItemChecked(i, true)
        list.invalidateViews()
    }

    // Save the assistant currently set in the system as the target
    private fun detectCurrent() {
        Thread {
            val s = Keeper.snapshot()
            runOnUiThread {
                if (s == null || (s.assistant.isEmpty() && s.vis.isEmpty())) return@runOnUiThread
                val comp = s.assistant.ifEmpty { s.vis }
                val label = items.firstOrNull { it.assistant == comp }?.label ?: comp
                Store.save(this, Assistant(label, s.assistant, s.vis, s.vrs))
                refresh()
            }
        }.start()
    }
}

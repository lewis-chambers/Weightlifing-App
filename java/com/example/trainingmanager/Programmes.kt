package com.example.trainingmanager

import RenamingPopup
import Utils
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.Gravity.CENTER
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.floatingactionbutton.FloatingActionButton


class Programmes : AppCompatActivity() {
    private lateinit var lifecycleListener: ProgrammesLifecycleListener
    private lateinit var baseLayout: LinearLayout
    private fun makeLifecycleListener() {
        lifecycleListener = ProgrammesLifecycleListener(this,findViewById(R.id.frame),lifecycle)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programmes)
        makeLifecycleListener()
        baseLayout = findViewById(R.id.mainLayout)
        registerForContextMenu(findViewById(R.id.program_list_view))
        val db = DatabaseHelper(this)
    }
    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        if (v!!.id == R.id.program_list_view ) {
            menu!!.setHeaderTitle("Options")
            menu.add("Rename")
            menu.add("Edit")
            menu.add("Duplicate")
            menu.add("Delete")
            menu.add("Set as Current Program")
        }
    }

    override fun onResume() {
        super.onResume()
        Utils().deleteSharedPrefs(this,"your_prefs")
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.toString()) {
            "Rename" -> {
                val popup = RenamingPopup(this)
                popup.show(baseLayout)
                lifecycleListener.renamePopup(item,popup)
            }
            "Edit" -> {
                lifecycleListener.editProgram(item)
            }
            "Duplicate" -> {
                lifecycleListener.duplicateProgram(item)
            }
            "Delete" -> {
                lifecycleListener.deleteAlert(item)
            }
            "Set as Current Program" -> {
                lifecycleListener.setAsCurrentProgram(item)
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        this.finish()
        return true
    }
    override fun onBackPressed() {
        this.finish()
    }

}
private class ProgrammesLifecycleListener(
    private var context : Context,
    private var frame : androidx.constraintlayout.widget.ConstraintLayout,
    lifecycle : Lifecycle
) : LifecycleObserver {
    private lateinit var listView : ListView
    private lateinit var newProgramBtn: FloatingActionButton
    private val baseLayout = frame.findViewWithTag<LinearLayout>("mainLayout")
    init {
        lifecycle.addObserver(this)
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun create() {
        setViews()
        setListeners()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun start() {
        loadPrograms()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        loadPrograms()
    }
    fun setViews() {
        listView = baseLayout.findViewWithTag("listView")
        newProgramBtn = frame.findViewWithTag("addButton")
    }
    fun setListeners() {
        newProgramBtn.setOnClickListener{
            val intent = Intent(context,ProgramDesigner::class.java)
            context.startActivity(intent)
        }
        listView.setOnItemClickListener{parent,view,position,id ->
            lateinit var intent: Intent
            val db = DatabaseHelper(context)
            val sessions = db.getSessions(listView.adapter.getItem(position).toString())!!
            if (sessions.count() == 1) {
                intent = Intent(context,SessionEntryScreen::class.java)
                intent.putExtra("session",sessions[0])
            } else {
                intent = Intent(context, WorkoutLaunchScreen::class.java)
            }
            intent.putExtra("program",listView.adapter.getItem(position).toString())
            context.startActivity(intent)
        }
    }
    fun loadPrograms() {
        val db = DatabaseHelper(context)
        val list = db.getPrograms()

        if (list.count() == 0) {//testing for empty programs
            baseLayout.removeViewAt(0)
            baseLayout.gravity = CENTER
            val tv = TextView(context)
            tv.text = "Oh no! it looks like you have no programs.\n\nPress the button to get started."
            tv.textSize = 20F

            val lp = Utils().widthHeight(WRAP_CONTENT, WRAP_CONTENT)
            lp.gravity = CENTER

            tv.layoutParams = lp
            baseLayout.addView(tv)
        } else {
            val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, list)
            listView.adapter = adapter
        }


    }
    fun deleteAlert(item: MenuItem){
        var output: Boolean? = null

        val builder = AlertDialog.Builder(context)

        builder.setTitle("Warning")
        builder.setMessage("Deletion is permanent, are you sure?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setIconAttribute(android.R.attr.alertDialogIcon)

        builder.setPositiveButton("Yes"){dialogInterface, i ->
            output=true
            deleteAlertBackend(output!!,item)
        }
        builder.setNegativeButton("No") { dialogInterface, i ->
            output = false
            deleteAlertBackend(output!!,item)
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
    fun deleteAlertBackend(output:Boolean,item:MenuItem) {
        if (output) {
            val lv = baseLayout.findViewWithTag<ListView>("listView")
            val info = item.menuInfo as AdapterContextMenuInfo
            var adapter = lv.adapter
            val itemSelected = adapter.getItem(info.position).toString()
            //SQLite removal

            val db = DatabaseHelper(context)
            db.deleteProgram(itemSelected)


            val adapterList = mutableListOf<String>()
            for (i in 0 until adapter.count) {
                adapterList.add(adapter.getItem(i).toString())
            }
            adapterList.removeAt(info.position)
            adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, adapterList)
            lv.adapter=adapter
        }
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun renamePopup(item:MenuItem,popupWindow: RenamingPopup) {
        val lv = baseLayout.findViewWithTag<ListView>("listView")
        val info = item.menuInfo as AdapterContextMenuInfo
        val adapter = lv.adapter
        val itemSelected = adapter.getItem(info.position).toString()

        val popup = popupWindow.getLayout()
        val btn = popup.findViewWithTag<Button>("change_button")
        val et = popup.findViewWithTag<EditText>("title_et")

        et.setText(itemSelected)
        et.requestFocus()

        btn.setOnClickListener {
            val db = DatabaseHelper(context)
            if (db.programExists(et.text.toString())) {
                et.error = "Title already taken"
            } else {
                db.renameProgram(itemSelected,et.text.toString())
                loadPrograms()
                popupWindow.dismiss()
            }
        }
    }
    fun duplicateProgram(item: MenuItem) {
        val lv = baseLayout.findViewWithTag<ListView>("listView")
        val info = item.menuInfo as AdapterContextMenuInfo
        val adapter = lv.adapter
        var itemSelected = adapter.getItem(info.position).toString()

        val db = DatabaseHelper(context)

        val count = countFileNameRepeats(itemSelected)
        val newProgram: String
        if (count == 0) {
            newProgram = itemSelected + " - Copy 1"
        } else {
            newProgram = itemSelected + " - Copy "+(count+1).toString()
        }

        db.makeProgramCopy(itemSelected,newProgram)
        loadPrograms()
    }
    fun countFileNameRepeats(fileName: String): Int {
        val db = DatabaseHelper(context)
        val programs = db.getPrograms()
        var copyCount = 0
        val regex = "$fileName - Copy [0-9]+".toRegex()

        for (program in programs) {
            if (program.matches(regex)) {
                copyCount++
            }
        }
        return copyCount
    }
    fun editProgram(item: MenuItem) {
        val lv = baseLayout.findViewWithTag<ListView>("listView")
        val info = item.menuInfo as AdapterContextMenuInfo
        val adapter = lv.adapter
        val itemSelected = adapter.getItem(info.position).toString()

        val intent = Intent(context, ProgramDesigner::class.java)
        intent.putExtra("program",itemSelected)
        context.startActivity(intent)
    }
    fun setAsCurrentProgram(item: MenuItem) {
        val lv = baseLayout.findViewWithTag<ListView>("listView")
        val info = item.menuInfo as AdapterContextMenuInfo
        val adapter = lv.adapter
        val itemSelected = adapter.getItem(info.position).toString()

        val db = DatabaseHelper(context)
        db.updateCurrentProgram(itemSelected)
    }
}
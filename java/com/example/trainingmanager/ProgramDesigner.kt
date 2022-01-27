package com.example.trainingmanager

import Utils
import WarningPopup
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity.CENTER
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.core.view.iterator
import androidx.core.view.size
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*


class ProgramDesigner : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_program_designer)

        val program = intent.getStringExtra("program")
        val recyclerView = findViewById<RecyclerView>(R.id.session_recycler_view)
        val programTitleET = findViewById<TextInputLayout>(R.id.program_title_layout)

        val viewAdapter: MyAdapter
        val itemTouchHelper by lazy {
            // 1. Note that I am specifying all 4 directions.
            //    Specifying START and END also allows
            //    more organic dragging than just specifying UP and DOWN.
            val simpleItemTouchCallback =
                object : ItemTouchHelper.SimpleCallback(UP or
                        DOWN or
                        START or
                        END, 0) {

                    override fun onMove(recyclerView: RecyclerView,
                                        viewHolder: RecyclerView.ViewHolder,
                                        target: RecyclerView.ViewHolder): Boolean {

                        val adapter = recyclerView.adapter as MyAdapter
                        val from = viewHolder.adapterPosition
                        val to = target.adapterPosition
                        // 2. Update the backing model. Custom implementation in
                        //    MainRecyclerViewAdapter. You need to implement
                        //    reordering of the backing model inside the method.
                        adapter.moveItem(from,to)
                        // 3. Tell adapter to render the model update.
                        adapter.notifyItemMoved(from, to)

                        return true
                    }
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                          direction: Int) {
                        // 4. Code block for horizontal swipe.
                        //    ItemTouchHelper handles horizontal swipe as well, but
                        //    it is not relevant with reordering. Ignoring here.
                    }
                }
            ItemTouchHelper(simpleItemTouchCallback)
        }
        itemTouchHelper.attachToRecyclerView(recyclerView)
        addSubmitClick()

        if (program == null) {
            viewAdapter = MyAdapter(this, mutableListOf())
        } else {
            viewAdapter = MyAdapter(this,getProgramData(program))
            programTitleET.editText?.setText(program)
        }

        val viewManager = LinearLayoutManager(this)
        recyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(SessionEntryScreen.VerticalSpaceItemDecoration(10))
        }

        val addSessionBtn = findViewById<Button>(R.id.add_session_button)
        addSessionBtn.setOnClickListener {
            viewAdapter.addView(null)
        }
    }

    override fun onBackPressed() {
        backCode()
    }
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        backCode()
        return true
    }
    fun addSubmitClick() {
        fun isSubmissionErrors() : Boolean {
            val db = DatabaseHelper(this)
            var toast = Toast.makeText(this.applicationContext,"",Toast.LENGTH_SHORT)
            toast.setGravity(CENTER,0,0)
            val sessionArray: MutableList<String> = mutableListOf()
            val title = findViewById<TextInputEditText>(R.id.program_title)
            val titleLayout = findViewById<TextInputLayout>(R.id.program_title_layout)
            val sessions = findViewById<RecyclerView>(R.id.session_recycler_view)
            //check for empty title
            if (title.text.toString().trim().isEmpty()) {
                titleLayout.error = "Field is empty"
                title.addTextChangedListener(object : TextWatcher {

                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(s: CharSequence, start: Int,
                                                   count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int,
                                               before: Int, count: Int) {
                        if (s.isNotEmpty()) {
                            titleLayout.isErrorEnabled = false
                        }
                    }
                })
                return true
            }
            //check if program exists
            if (db.programExists(title.text.toString())) {
                val program = (this as Activity).intent.getStringExtra("program")
                if (program == null) {
                    title.error = "Program already exists"
                    title.requestFocus()
                    return true
                } else {
                    if (title.text.toString() != program) {
                        title.error = "Program already exists"
                        title.requestFocus()
                        return true
                    }
                }
            }
            //check if any sessions
            if (sessions.childCount == 0) {
                toast = Toast.makeText(this.applicationContext,"Program is empty",Toast.LENGTH_SHORT)
                toast.setGravity(CENTER,0,0)
                toast.show()
                return true
            }
            for (view in sessions) {
                val sessionTitle = view.findViewById<EditText>(R.id.session_title)
                val exerciseRecyclerView = view.findViewById<RecyclerView>(R.id.exercise_recycler_view)

                var titleMatch = false
                val error = "Sessions cannot have the same name"
                //check if session if empty
                if (exerciseRecyclerView.childCount == 0) {
                    toast.setText("Session: \"${sessionTitle.text}\" is empty")
                    toast.show()
                    return true
                }
                //check for repeated titles
                for (i in 0 until sessionArray.count()) {
                    if (sessionTitle.text.toString() == sessionArray[i]) {
                        titleMatch = true
                        sessions[i].findViewById<EditText>(R.id.session_title).error = error
                    }
                    if (titleMatch) {
                        toast.setText(error)
                        toast.show()

                        sessionTitle.error = error
                        sessionTitle.requestFocus()
                        return true
                    }
                }
                sessionArray.add(sessionTitle.text.toString())
                //check for empty fields
                for (row in exerciseRecyclerView) {
                    val exercise = row.findViewById<AutoCompleteTextView>(R.id.exercise)
                    val sets = row.findViewById<EditText>(R.id.sets)
                    val reps = row.findViewById<EditText>(R.id.reps)

                    if (exercise.text.isEmpty()) {
                        exercise.error = "Enter an exercise"
                        return true
                    } else if (sets.text.isEmpty()) {
                        sets.error = "Enter number of sets"
                        return true
                    } else if (reps.text.isEmpty()) {
                        reps.error = "Enter a rep range"
                        return true
                    }
                }
            }
            return false
        }
        val submitButton = findViewById<Button>(R.id.submit_program_button)
        submitButton.setOnClickListener{
             if (!isSubmissionErrors()) {
                WarningPopup(this,"Are you sure you want to submit?",::newSubmission)
             }
         }
    }
    fun newSubmission() {
        val db = DatabaseHelper(this)
        val savedProgram = intent.getStringExtra("program")
        val programName = findViewById<TextInputEditText>(R.id.program_title)
        val sessionRecyclerView = findViewById<RecyclerView>(R.id.session_recycler_view)

        val sqlOutputInsert = mutableListOf<Utils.SQLProgramExercise>()
        val sqlOutputUpdate = mutableListOf<Utils.SQLOutputExercise>()

        for (i in 0 until sessionRecyclerView.size) {
            val v = sessionRecyclerView[i]
            val sessionIDView = v.findViewById<View>(R.id.id)
            val sessionTitle = v.findViewById<EditText>(R.id.session_title)
            val innerRecyclerView = v.findViewById<RecyclerView>(R.id.exercise_recycler_view)

            var sessionID: String? = null
            if (sessionIDView.tag != null) {
                sessionID = sessionIDView.tag.toString()
            }
            for (ii in 0 until innerRecyclerView.size) {
                val vv = innerRecyclerView[ii]
                val exerciseBox = vv.findViewById<AutoCompleteTextView>(R.id.exercise)
                val setsBox = vv.findViewById<EditText>(R.id.sets)
                val repsBox = vv.findViewById<EditText>(R.id.reps)

                val exercise = Utils.SQLProgramExercise(
                    sessionTitle.text.toString(),
                    exerciseBox.text.toString(),
                    setsBox.text.toString(),
                    repsBox.text.toString()
                )
                sqlOutputInsert.add(
                    exercise
                )
                sqlOutputUpdate.add(
                    Utils.SQLOutputExercise(
                        sessionID,
                        exercise
                    )
                )
            }
        }
        if (savedProgram != null) {
            db.updateProgram(savedProgram,sqlOutputUpdate)
            if (programName.text.toString() != savedProgram) {
                db.renameProgram(savedProgram,programName.text.toString())
            }
        } else {
            db.insertProgram(programName.text.toString(),sqlOutputInsert)
        }
        this.finish()
    }
    fun backCode() {
        fun simpleIsSafe(): Boolean {
            val programTitle = findViewById<TextInputEditText>(R.id.program_title)
            val sessions = findViewById<RecyclerView>(R.id.session_recycler_view)
            //Check if title is empty
            if (programTitle.text.toString().isNotEmpty()) {
                return false
            }
            //check if any sessions exist
            if (sessions.childCount != 0) {
                return false
            }
            return true
        }
        fun issSafe(): Boolean {
            val programTitle = findViewById<TextInputEditText>(R.id.program_title)
            val sessions = findViewById<RecyclerView>(R.id.session_recycler_view)
            //check if title is empty
            if (programTitle.text.toString().isNotEmpty()) {
                return false
            }
            //check if session fields are empty
            for (session in sessions) {
                val exercises = session.findViewById<RecyclerView>(R.id.exercise_recycler_view)

                for (exercise in exercises) {
                    val exerciseName = exercise.findViewById<AutoCompleteTextView>(R.id.exercise)
                    val sets = exercise.findViewById<EditText>(R.id.sets)
                    val reps = exercise.findViewById<EditText>(R.id.reps)
                    println(sets)
                    if (exerciseName.text.isNotEmpty()) {
                        return false
                    } else if (sets.text.isNotEmpty()) {
                        return false
                    } else if (reps.text.isNotEmpty()) {
                        return false
                    }
                }
            }
            return true
        }
        if (simpleIsSafe()) {
            this.finish()
        } else {
            WarningPopup(this, resources.getString(R.string.progress_will_be_lost)) { this.finish() }
        }
    }
    fun getProgramData(program: String): MutableList<Session> {
        val programData: MutableList<Session> = arrayListOf()
        val db = DatabaseHelper(this)
        val sessionNames = db.getSessions(program)!!

        for (i in 0 until sessionNames.count()) {
            val sessionData = db.getSessionExercises(program,sessionNames[i])
            programData.add(Session(
                db.getSessionID(program,sessionNames[i]),
                sessionNames[i],
                sessionData
            ))
        }
        return programData
    }
    data class Session (
        var sessionID: Int?,
        var sessionName: String?,
        var data: MutableList<Utils.ExerciseProgram>
    )
    inner class MyAdapter(
        private val context: Context,
        private val sessions: MutableList<Session>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        private val exerciseNames = DatabaseHelper(context).getAllExerciseNames()
        private var sessionsMade: Int = sessions.size

        inner class MyViewHolder(val ll: ConstraintLayout) : RecyclerView.ViewHolder(ll)
        fun addView(sessionName: String?) {
            sessions.add(
                Session(
                null,
                sessionName,
                mutableListOf()))
            notifyItemInserted(sessions.size)
            sessionsMade++
        }
        fun removeView(position:Int,holder:MyViewHolder) {
            sessions.removeAt(position)
            notifyItemRemoved(position)
        }
        fun moveItem(from:Int,to:Int) {
            Collections.swap(sessions, from, to)
        }

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyViewHolder {

            val session = LayoutInflater.from(parent.context)
                .inflate(R.layout.program_designer_workout, parent, false) as ConstraintLayout
            val holder = MyViewHolder(session)
            val deleteSession = session.findViewById<Button>(R.id.delete_session_button)
            val handleView = session.findViewById<Button>(R.id.move_session_button)
            deleteSession.setOnClickListener{removeView(holder.adapterPosition,holder)}

            return holder
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val recyclerView = holder.ll.findViewById<RecyclerView>(R.id.exercise_recycler_view)
            val viewManager = LinearLayoutManager(this@ProgramDesigner)
            val session = sessions[holder.adapterPosition]
            val viewAdapter = InnerAdapter(exerciseNames,session)

            recyclerView.apply {
                layoutManager = viewManager
                this.adapter = viewAdapter
                addItemDecoration(SessionEntryScreen.VerticalSpaceItemDecoration(10))
            }
            holder.ll.findViewById<View>(R.id.id).apply {
                if (session.sessionID != null) {
                    this.tag = session.sessionID
                }
            }
            holder.ll.findViewById<Button>(R.id.add_exercise_button)
                .setOnClickListener {
                    viewAdapter.addView()
                }
            holder.ll.findViewById<EditText>(R.id.session_title).apply {
                if (session.sessionName == null) {
                    setText("Session $sessionsMade")
                } else {
                    setText(session.sessionName)
                }
            }
        }

        override fun getItemCount(): Int {
            return sessions.count()
        }
    }
    inner class InnerAdapter(
        private val exerciseNames: ArrayList<String>?,
        private val session: Session) :
        RecyclerView.Adapter<InnerAdapter.MyViewHolder>() {
        private var exercisesMade: Int = 0

        inner class MyViewHolder(val ll: LinearLayout) : RecyclerView.ViewHolder(ll)
        fun addView() {
            session.data.add(
                Utils.ExerciseProgram(
                    "",
                    "",
                    ""
                )
            )
            notifyItemInserted(session.data.size)
            exercisesMade++
        }
        fun removeView(view:Int) {
            session.data.removeAt(view)
            notifyItemRemoved(view)
        }
        fun setExerciseSuggestions(context: Context,exercise:AutoCompleteTextView) {
            if (exerciseNames != null) {
                val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, exerciseNames)
                exercise.setAdapter(adapter)
                exercise.threshold = 2
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyViewHolder {
            val row = LayoutInflater.from(parent.context)
                .inflate(R.layout.program_designer_input_row, parent,false) as LinearLayout
            val exercise = row.findViewById<AutoCompleteTextView>(R.id.exercise)
            val deleteBtn = row.findViewById<Button>(R.id.delete)
            //setting exercise suggestions
            setExerciseSuggestions(parent.context,exercise)
            val holder = MyViewHolder(row)
            deleteBtn.setOnClickListener{removeView(holder.adapterPosition)}

            return holder
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val exercise = holder.ll.findViewById<AutoCompleteTextView>(R.id.exercise)
            val sets = holder.ll.findViewById<EditText>(R.id.sets)
            val reps = holder.ll.findViewById<EditText>(R.id.reps)

            val exerciseData = session.data[holder.adapterPosition]
            exercise.setText(exerciseData.title)
            sets.setText(exerciseData.sets)
            reps.setText(exerciseData.reps)

        }

        override fun getItemCount(): Int {
            return session.data.count()
        }
    }
}
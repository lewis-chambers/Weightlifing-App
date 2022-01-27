package com.example.trainingmanager

import ExerciseSetupPopup
import Stopwatch
import Utils
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.view.View.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class RecyclerFragment : Fragment() {
    private lateinit var exercise: Utils.SQLExercise
    private lateinit var inputWrapper : RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var timer: Stopwatch
    private lateinit var optionsView: ConstraintLayout
    private lateinit var setsAdapter: MyAdapter
    private var bMenu: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState:
        Bundle?
    ): View? {
        val args = arguments!!
        // Creates the view controlled by the fragment
        val view =
            inflater.inflate(R.layout.fragment_exercise_separated_sets_recycler, container, false)
        titleTextView = view.findViewById(R.id.exercise_title)
        inputWrapper = view.findViewById(R.id.sets_layout)
        val lastCommentHeader = view.findViewById<TextView>(R.id.last_comments_header)
        val lastComment = view.findViewById<TextView>(R.id.last_comments)
        val setsReps = view.findViewById<TextView>(R.id.rep_range)
        val setupInstructions = view.findViewById<Button>(R.id.setup_instructions)
        val addSetButton = view.findViewById<Button>(R.id.add_set_button)

        // Retrieving parcelised data from initialisation
        exercise = args.getParcelable("exercise_data")!!
        timer = args.getParcelable("timer")!!

        // Extracting correct set and rep range from storage exercise
        setsReps.text = "${exercise.sets} sets x ${exercise.reps} reps"
        titleTextView.text = exercise.title
        setupInstructions.setOnClickListener {
            val setupInstructionsPopup = ExerciseSetupPopup(activity!!)
            if (exercise.setup != null) {
                setupInstructionsPopup.setText(exercise.setup!!)
            }
            setupInstructionsPopup.setTheWidth(view.findViewById<ConstraintLayout>(R.id.constraintLayout).width)
            setupInstructionsPopup.show(activity!!.findViewById(R.id.base_layout))

            val doneButton = setupInstructionsPopup.getButton()
            doneButton.setOnClickListener {
                if (exercise.setup != setupInstructionsPopup.getText()) {
                    exercise.setup = setupInstructionsPopup.getText()
                }
                //write to database
                val db = DatabaseHelper(context)
                db.updateExerciseSetup(setupInstructionsPopup.getText(), exercise.title)
                setupInstructionsPopup.dismiss()
            }
        }

        if (exercise.comment != null) { //adds last sessions comment or hides the view
            lastComment.text = exercise.comment
        } else {
            lastComment.visibility = GONE
            lastCommentHeader.visibility = GONE
        }
        if (exercise.data != null){
            setsAdapter = MyAdapter(exercise.data!!)
        } else {
            val list = mutableListOf<Utils.SQLExerciseData>()
            for (i in 0 until exercise.sets.toInt()) {
                list.add(Utils.SQLExerciseData(null,null,null))
            }
            setsAdapter = MyAdapter(list)
        }
        val viewManager = LinearLayoutManager(context)
        inputWrapper.apply {
            layoutManager = viewManager
            adapter = setsAdapter
            addItemDecoration(SessionEntryScreen.VerticalSpaceItemDecoration(this.marginTop))
        }
        addSetButton.setOnClickListener{
            setsAdapter.addView()
        }
        return view

    }
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        if (v.tag == "options" ) {
            optionsView = v.parent as ConstraintLayout
            menu.setHeaderTitle("Options")

            if (optionsView.findViewWithTag<TextView>("weight_title").visibility == GONE) {
                menu.add("Edit")
            }
            menu.add("Delete")
            bMenu = true
        }
    }
    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (bMenu) {
            bMenu = false
            if (item.toString() == "Edit") {
                expand()
                return true
            } else if (item.toString() == "Delete"){
                deleteSet()
            } else {
                return super.onContextItemSelected(item)
            }
        }
        return super.onContextItemSelected(item)
    }
    private fun expand() {
        optionsView.findViewWithTag<TextView>("weight_title").visibility = VISIBLE
        optionsView.findViewWithTag<TextView>("reps_title").visibility = VISIBLE
        optionsView.findViewWithTag<TextView>("RPE_title").visibility = VISIBLE
        optionsView.findViewWithTag<EditText>("weight_input").visibility = VISIBLE
        optionsView.findViewWithTag<EditText>("reps_input").visibility = VISIBLE
        optionsView.findViewWithTag<EditText>("RPE_input").visibility = VISIBLE
        optionsView.findViewWithTag<Button>("set_finished_button").visibility = VISIBLE
    }
    private fun deleteSet() {
        val viewInt = inputWrapper.indexOfChild(optionsView)
        setsAdapter.removeView(viewInt)
    }
    private fun initialiseLine(data: Utils.SQLExerciseData?,lastSet: Boolean) {
        val db = DatabaseHelper(context)
        // Initalising the views
        val line = inflate(context,R.layout.fragment_exercise_separated_sets_set_layout,null)
        val setNumberTextView = line.findViewWithTag<TextView>("set_number")
        val lastTextView = line.findViewWithTag<TextView>("performance_last_header")
        val lastData = line.findViewWithTag<TextView>("last_data")
        val thisTextView = line.findViewWithTag<TextView>("performance_this_header")
        val thisData = line.findViewWithTag<TextView>("this_data")

        val weightTitle = line.findViewWithTag<TextView>("weight_title")
        val weight = line.findViewWithTag<EditText>("weight_input")

        val repsTitle = line.findViewWithTag<TextView>("reps_title")
        val reps = line.findViewWithTag<EditText>("reps_input")

        val RPETitle = line.findViewWithTag<TextView>("RPE_title")
        val RPE = line.findViewWithTag<EditText>("RPE_input")


        val finishedButton = line.findViewWithTag<Button>("set_finished_button")
        val options = line.findViewWithTag<Button>("options")

        // Setting data from last time
        setNumberTextView.text = "Set ${(inputWrapper.childCount+1)}"
        if (data != null) {
            lastData.text =
                "${Utils().decimalCheck(data.weight!!.toDouble())}kg x ${data.reps} reps @ RPE: ${data.RPE}"
            weight.setText("${Utils().decimalCheck(data.weight.toDouble())}")
            reps.setText("${data.reps}")
            //RPE.setText("${data.RPE}")
        } else {
            lastData.visibility = GONE
            lastTextView.visibility = GONE
            if (lastSet) {
                val last = inputWrapper.getChildAt(inputWrapper.childCount-1)
                weight.setText("${last.findViewWithTag<EditText>("weight_input").text}")
                reps.setText("${last.findViewWithTag<EditText>("reps_input").text}")
            }
        }
        // Setting listeners
        finishedButton.setOnClickListener{
            fun error(weight: EditText, reps: EditText, RPE: EditText) : Boolean {
                var error = false
                if (weight.text.isEmpty()) {
                    weight.error = "Weight is required"
                    error = true
                }
                if (reps.text.isEmpty()) {
                    reps.error = "reps are required"
                    error = true
                }
                if (RPE.text.isEmpty()) {
                    RPE.error = "RPE is required"
                    error = true
                }
                if (error) {
                    return true
                }
                return false
            }
            if (!error(weight,reps,RPE)) {
                if (thisData.visibility == GONE) {
                    timer.start()
                    activity!!.findViewById<LinearLayout>(R.id.timer_layout).visibility = VISIBLE
                    thisTextView.visibility = VISIBLE
                    thisData.visibility = VISIBLE
                }
                weightTitle.visibility = GONE
                weight.visibility = GONE
                repsTitle.visibility = GONE
                reps.visibility = GONE
                RPETitle.visibility = GONE
                RPE.visibility = GONE
                finishedButton.visibility = GONE
                finishedButton.text = "Done"
                thisData.text = "${weight.text}kg x ${reps.text} reps @ RPE: ${RPE.text}"
            }
        }
        registerForContextMenu(options)
        inputWrapper.addView(line)
        Utils().setMargins(line, 0, 20, 0, 0)
    }
    inner class MyAdapter(
        private val data: MutableList<Utils.SQLExerciseData>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        inner class MyViewHolder(val ll: ConstraintLayout) : RecyclerView.ViewHolder(ll)
        fun setSetNumber(textView: TextView,number: Int) {
            textView.text = "Set $number"
        }
        fun addView() {
            data.add(
                Utils.SQLExerciseData(
                    null,
                    null,
                    null
                )
            )
            notifyItemInserted(data.size)
            updateSetNumbers()
        }
        fun removeView(position:Int) {
            println("Before deletion ${inputWrapper.childCount}")
            data.removeAt(position)
            notifyItemRemoved(position)
            println("After deletion ${inputWrapper.childCount}")
            updateSetNumbers()
        }
        private fun updateSetNumbers() {
            println(inputWrapper.childCount)
            for (i in 0 until inputWrapper.childCount) {
                inputWrapper[i].findViewById<TextView>(R.id.set_number).text = "Set ${i+1}"
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyViewHolder {

            val set = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_exercise_separated_sets_set_layout, parent, false) as ConstraintLayout
            val options = set.findViewById<Button>(R.id.options)
            val finishedButton = set.findViewById<Button>(R.id.set_finished_button)
            val thisTextView = set.findViewById<TextView>(R.id.performance_this_header)
            val thisData = set.findViewById<TextView>(R.id.this_data)
            val weight = set.findViewById<EditText>(R.id.weight_input)
            val reps = set.findViewById<EditText>(R.id.reps_input)
            val RPE = set.findViewById<EditText>(R.id.RPE_input)
            val weightTitle = set.findViewById<TextView>(R.id.weight_title)
            val repsTitle = set.findViewById<TextView>(R.id.reps_title)
            val RPETitle = set.findViewById<TextView>(R.id.RPE_title)

            val holder = MyViewHolder(set)
            finishedButton.setOnClickListener{
                fun error(weight: EditText, reps: EditText, RPE: EditText) : Boolean {
                    var error = false
                    if (weight.text.isEmpty()) {
                        weight.error = "Weight is required"
                        error = true
                    }
                    if (reps.text.isEmpty()) {
                        reps.error = "reps are required"
                        error = true
                    }
                    if (RPE.text.isEmpty()) {
                        RPE.error = "RPE is required"
                        error = true
                    }
                    if (error) {
                        return true
                    }
                    return false
                }
                if (!error(weight,reps,RPE)) {
                    if (thisData.visibility == GONE) {
                        timer.start()
                        activity!!.findViewById<LinearLayout>(R.id.timer_layout).visibility = VISIBLE
                        thisTextView.visibility = VISIBLE
                        thisData.visibility = VISIBLE
                    }
                    weightTitle.visibility = GONE
                    weight.visibility = GONE
                    repsTitle.visibility = GONE
                    reps.visibility = GONE
                    RPETitle.visibility = GONE
                    RPE.visibility = GONE
                    finishedButton.visibility = GONE
                    finishedButton.text = "Done"
                    thisData.text = "${weight.text}kg x ${reps.text} reps @ RPE: ${RPE.text}"
                }
            }
            registerForContextMenu(options)
            return holder
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            println("bound: ${holder.adapterPosition}")
            val title = holder.ll.findViewById<TextView>(R.id.set_number)
            val lastData = holder.ll.findViewById<TextView>(R.id.last_data)
            val weightET = holder.ll.findViewById<EditText>(R.id.weight_input)
            val repsET = holder.ll.findViewById<EditText>(R.id.reps_input)
            val lastDataTitle = holder.ll.findViewById<TextView>(R.id.performance_last_header)

            val last = data[holder.adapterPosition]
            title.text = "Set ${holder.adapterPosition + 1}"
            if (last.weight != null) {
                lastData.text = "${last.weight}kg x ${last.reps} reps @ RPE: ${last.RPE}"
                weightET.setText(last.weight)
                repsET.setText(last.reps)
            } else {
                lastDataTitle.visibility = GONE
                lastData.visibility = GONE
            }

        }

        override fun getItemCount(): Int {
            return data.count()
        }
    }
    companion object {

        // Method for creating new instances of the fragment
        fun newInstance(context: Context,
                        exercise: Utils.ExerciseProgram,
                        exerciseTimer: Stopwatch,
                        db: DatabaseHelper,
                        lastSessionData: Utils.SessionActivity?,
                        program: String,
                        session: String,
                        position: Int): RecyclerFragment {
            val args = Bundle()
            val exerciseInstructions = db.getExerciseInstructions(exercise.title)

            val exerciseFull = Utils.SQLExercise(
                exercise.title,
                exercise.sets,
                exercise.reps,
                exerciseInstructions?.setup,
                exerciseInstructions?.instructions,
                null,
                null
            )
            if (lastSessionData != null) {
                val comment = db.getExerciseComment(
                    exercise.title,
                    lastSessionData.time
                )
                exerciseFull.data = db.getLastExerciseData(program,session,exercise.title, lastSessionData.time)
                exerciseFull.comment = comment
            } else {
                exerciseFull.data = db.getLastExerciseData(program,session,exercise.title, null)
            }
            args.putParcelable("exercise_data",exerciseFull as Parcelable)
            args.putParcelable("timer",exerciseTimer as Parcelable)

            // to be retrieved and displayed when the view is created
            val fragment = RecyclerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
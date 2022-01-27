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
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import java.io.File
import kotlin.properties.Delegates


class ExerciseFragmentSeparatedSets : Fragment() {
    private lateinit var exerciseStorage: Utils.ExerciseStorage
    private var variantInt by Delegates.notNull<Int>()
    private var lastInstance by Delegates.notNull<Utils.ExerciseInstance>()
    private lateinit var variant: Utils.Exercise
    private lateinit var inputWrapper : LinearLayout
    private lateinit var titleTextView: TextView
    private lateinit var timer: Stopwatch
    private lateinit var optionsView: ConstraintLayout
    private var test: global = global()
    private var bMenu: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState:
        Bundle?
    ): View? {
        val args = arguments!!
        // Creates the view controlled by the fragment
        val view = inflater.inflate(R.layout.fragment_exercise_separated_sets, container, false)
        titleTextView = view.findViewById(R.id.exercise_title)
        inputWrapper = view.findViewById(R.id.sets_layout)
        val lastCommentHeader = view.findViewById<TextView>(R.id.last_comments_header)
        val lastComment = view.findViewById<TextView>(R.id.last_comments)
        val setsReps = view.findViewById<TextView>(R.id.rep_range)
        val setupInstructions = view.findViewById<Button>(R.id.setup_instructions)
        val addSetButton = view.findViewById<Button>(R.id.add_set_button)

        // Retrieving parcelised data from initialisation
        exerciseStorage = args.getParcelable("exercise_storage")!!
        variantInt = args.getInt("variant_index")
        timer = args.getParcelable("timer")!!

        // Extracting correct set and rep range from storage exercise
        variant = exerciseStorage.variants[variantInt]
        setsReps.text = "${variant.sets} sets x ${variant.reps} reps"
        titleTextView.text = exerciseStorage.exerciseTitle
        setupInstructions.setOnClickListener{
            val setupInstructionsPopup = ExerciseSetupPopup(activity!!)
            setupInstructionsPopup.setText(exerciseStorage.setupInstructions)
            setupInstructionsPopup.setTheWidth(view.findViewById<ConstraintLayout>(R.id.constraintLayout).width)
            setupInstructionsPopup.show(activity!!.findViewById(R.id.base_layout))

            val doneButton = setupInstructionsPopup.getButton()
            doneButton.setOnClickListener{
                if (exerciseStorage.setupInstructions != setupInstructionsPopup.getText()) {
                    exerciseStorage.setupInstructions = setupInstructionsPopup.getText()
                    val file = File(activity!!.filesDir,"/exercises/"+
                            Utils().removePunctuationCheck(exerciseStorage.exerciseTitle)+
                            "/exercise.json")
                    file.writeText(Gson().toJson(exerciseStorage))
                }
                setupInstructionsPopup.dismiss()
            }
        }

        // Adds set boxes based on whether there is any variant history
        if (variant.instances.isNotEmpty()) {
            lastInstance = variant.instances.last()

            if (lastInstance.comment.isNotEmpty()) { //adds last sessions comment or hides the view
                lastComment.text = lastInstance.comment
            } else {
                lastComment.visibility = GONE
                lastCommentHeader.visibility = GONE
            }

            for (i in 0 until variant.sets.toInt()) {
                if (i < lastInstance.sets.count()) {
                    initialiseLine(i, putData = true, lastSet = false)
                } else {
                    initialiseLine(i, putData = false, lastSet = false)
                }
            }
        } else {
            for (i in 0 until variant.sets.toInt()) {
                initialiseLine(i, putData = false, lastSet = false)
            }
        }
        // Add set button listener
        addSetButton.setOnClickListener{
            initialiseLine(inputWrapper.childCount, putData = false, lastSet = true)
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
        inputWrapper.removeView(optionsView)

        for (i in viewInt until inputWrapper.childCount) {
            inputWrapper.getChildAt(i).findViewById<TextView>(R.id.set_number).text = "Set ${i+1}"
        }
    }
    private fun initialiseLine(setNumber: Int,putData: Boolean,lastSet: Boolean) {
        val i = setNumber
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
        setNumberTextView.text = "Set ${(i+1)}"
        if (putData) {
            lastData.text =
                "${Utils().decimalCheck(lastInstance.sets[i].weight)}kg x ${lastInstance.sets[i].repsCompleted} reps @ RPE: ${lastInstance.sets[i].difficult}"
            weight.setText("${Utils().decimalCheck(lastInstance.sets[i].weight)}")
            reps.setText("${lastInstance.sets[i].repsCompleted}")
            RPE.setText("${lastInstance.sets[i].difficult}")
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
    companion object {

        // Method for creating new instances of the fragment
        fun newInstance(
            context: Context,
            progExercise: Utils.ExerciseProgram,
            exerciseTimer: Stopwatch
        ): ExerciseFragmentSeparatedSets {
        val args = Bundle()
        val file = File(context.filesDir,"/exercises/"+
                    Utils().removePunctuationCheck(progExercise.title)+
                    "/exercise.json")
        val exercise = Utils().parseExerciseStorage(file.readText())
            for (i in 0 until exercise.variants.count()) {
                if (exercise.variants[i].sets == progExercise.sets &&
                    exercise.variants[i].reps == progExercise.reps) {

                    args.putParcelable("exercise_storage",exercise as Parcelable)
                    args.putInt("variant_index",i)
                }
            }
            args.putParcelable("timer",exerciseTimer as Parcelable)

            // to be retrieved and displayed when the view is created
            val fragment = ExerciseFragmentSeparatedSets()
            fragment.arguments = args
            return fragment
        }
    }
}
class global() {
    var layout: ConstraintLayout? = null
    fun init(layoutIn: ConstraintLayout) {
        layout = layoutIn
    }
    fun printWeight() {
        println((layout?.parent?.parent as ConstraintLayout).findViewById<TextView>(R.id.exercise_title).text)
    }
}
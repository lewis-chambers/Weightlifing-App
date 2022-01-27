package com.example.trainingmanager

import Utils
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import kotlinx.android.synthetic.main.activity_session_entry_screen.*

class SessionEntryScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_entry_screen)

        val session = intent.getStringExtra("session")!!
        val program = intent.getStringExtra("program")!!
        val db = DatabaseHelper(this)

        val sessionTitle = findViewById<TextView>(R.id.session_title)
        val commentHeader = findViewById<TextView>(R.id.last_comments_header)
        val commentTextView = findViewById<TextView>(R.id.last_comments)
        val lastSession = db.getLastSessionEntry(program,session)
        var lastComment: String?
        if (lastSession != null) {
            lastComment = db.getSessionComment(program,session,lastSession.time)
            println("last: $lastComment")
            if (lastComment != null) {
                println(lastComment.length)
                commentTextView.text = lastComment
            } else {
                commentTextView.visibility = GONE
                commentHeader.visibility = GONE
            }
        } else {
            commentTextView.visibility = GONE
            commentHeader.visibility = GONE
        }
        sessionTitle.text  = session

        val viewManager = LinearLayoutManager(this)
        val viewAdapter = MyAdapter(db.getSessionExercises(program,session))
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(10))

        start_workout_button.setOnClickListener{
            val intent = Intent(this,SessionScreen::class.java)
            intent.putExtra("session",session)
            intent.putExtra("program",program)
            startActivity(intent)
        }
    }
    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) :
        ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = verticalSpaceHeight
        }

    }
    class MyAdapter(private val myDataset: MutableList<Utils.ExerciseProgram>) :
        RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        class MyViewHolder(val ll: ConstraintLayout) : RecyclerView.ViewHolder(ll)


        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): MyViewHolder {
            // create a new view
            val ll = LayoutInflater.from(parent.context)
                .inflate(R.layout.session_entry_exercise, parent, false) as ConstraintLayout
            // set the view's size, margins, paddings and layout parameters
            return MyViewHolder(ll)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.ll.findViewWithTag<TextView>("exercise_name").text = myDataset[position].title
            holder.ll.findViewWithTag<TextView>("exercise_sets_reps").text =
                myDataset[position].sets+" sets of "+myDataset[position].reps+" reps"
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = myDataset.size
    }
}
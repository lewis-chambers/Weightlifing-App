import android.R.attr
import android.app.Activity
import android.content.Context
import android.os.Parcelable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.parcel.Parcelize
import java.io.File


class BackButtonPress(
    private var context: Context
) {
    val a = backButtonAlert()
    private fun backButtonAlert(){
        var output: Boolean? = null

        val builder = AlertDialog.Builder(context)

        builder.setTitle("Warning")
        builder.setMessage("Progress will be lost.\n\nAre you sure you want to go back?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setIconAttribute(attr.alertDialogIcon)

        builder.setPositiveButton("Yes"){dialogInterface, i ->
            output=true
            backButtonAlertOutput(output!!)
        }
        builder.setNegativeButton("No") { dialogInterface, i ->
            output = false
            backButtonAlertOutput(output!!)
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
    private fun backButtonAlertOutput(output:Boolean) {
        if (output) {
            (context as Activity).finish()
        }
    }

}
class Maintenance {
    fun addInstructionsToExercise(context: Context) {
        File(context.filesDir, "/exercises/").walk().maxDepth(1).forEach {
            if (it.toString().contains("/exercises/")) {
                val file = File(it,"exercise.json")
                val exercise = Utils().parseExerciseStorage(file.readText())

                //file.writeText(Gson().toJson(tempExerciseStorage))
            }
        }
    }
    fun addCommentsToSessions(context: Context) {
        File(context.filesDir, "/programs/").walk().maxDepth(1).forEach {
            if (it.toString().contains("/programs/")) {
                val file = File(it,"workout.json")
                val program = Utils().parseProgram(file.readText())
                for (i in 0 until program.program.count()) {
                    val session = program.program[i]
                    program.program[i].comment = mutableListOf()
                }
                file.writeText(Gson().toJson(program))
            }
        }
    }
}
class Utils {
    @Parcelize
    data class ExerciseStorage(
        var exerciseTitle: String,
        var variants: MutableList<Exercise>,
        var setupInstructions: String
    ) : Parcelable
    @Parcelize
    data class Exercise(
        var sets: String,
        var reps: String,
        var instances: MutableList<ExerciseInstance>
    ) : Parcelable
    @Parcelize
    data class ExerciseInstance(
        var program: String,
        var sets: MutableList<SetData>,
        var comment: String
    ) : Parcelable
    @Parcelize
    data class SetData(
        var weight: Double,
        var repsCompleted: Int,
        var difficult: Int
    ) : Parcelable
    @Parcelize
    data class Session(
        var title: String,
        var exercises: MutableList<ExerciseProgram>,
        var comment: MutableList<String>
    ) : Parcelable
    @Parcelize
    data class Program(
        var title: String,
        var program: MutableList<Session>
    ) : Parcelable

    data class ExerciseList(
        var exercises: MutableList<ExerciseFull>
    )
    data class ExerciseFull(
        var title: String,
        var sets: String,
        var reps: String,
        var instance: ExerciseInstance,
        var setupInstructions: String
    )
    @Parcelize
    data class ExerciseProgram(
        var title: String,
        var sets: String,
        var reps: String
    ) : Parcelable
    data class SQLProgramExercise(
        var session: String,
        var title: String,
        var sets: String,
        var reps: String
    )
    data class SessionData(
        var programTitle: String,
        var sessionTitle: String,
        var startTime: String,
        var endTime: String
    )
    data class SessionActivity(
        val program: String,
        val session: String,
        val time:String)
    @Parcelize
    data class SQLExercise(
        val title: String,
        val sets: String,
        val reps: String,
        var setup: String?,
        val instructions: String?,
        var data: MutableList<SQLExerciseData>?,
        var comment: String?
    ) : Parcelable
    @Parcelize
    data class SQLExerciseData(
        val weight: String?,
        val reps: String?,
        val RPE: String?
    ) : Parcelable
    @Parcelize
    data class SQLExerciseInstructions(
        val setup: String?,
        val instructions: String?
    ) : Parcelable
    data class SQLOutputExercise(
        val sessionID:String?,
        val exercise:Utils.SQLProgramExercise
    )
    data class ProgramTracking(
        var entries: MutableList<SessionData>
    )
    fun getFilePath(context: Context): File {
        val rootPath: File? = context.filesDir
        return File(rootPath, "/programs/")

    }
    fun saveStringSharedPref(context: Context,prefs: String,key:String,value: String) {
        val sp = context.getSharedPreferences(prefs, Activity.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(key, value)
        editor.apply()
    }
    fun getStringSharedPref(context: Context,prefs:String,key:String) : String {
        val sp = context.getSharedPreferences(prefs, Activity.MODE_PRIVATE)
        return sp.getString(key, "").toString()
    }
    fun deleteSharedPrefs(context: Context,prefs: String) {
        val sp = context.getSharedPreferences(prefs, Activity.MODE_PRIVATE)
        sp.edit().clear().apply()
    }
    fun deleteSharedPrefStr(context: Context,sharedPref:String,string:String) {
        val sp = context.getSharedPreferences(sharedPref, Activity.MODE_PRIVATE)
        sp.edit().remove(string).apply()
    }
    fun sharedPrefStrExists(context:Context,prefs:String,key:String) : Boolean {
        val sp = context.getSharedPreferences(prefs, Activity.MODE_PRIVATE)
        return sp.contains(key)
    }
    fun getProgramStr(context: Context,str: String): String {

        val rootPath: File? = context.filesDir
        val programFolder = File(rootPath, "/programs/")

        return File(programFolder, "/$str/workout.json").readText()
    }

    fun getProgram(context: Context) : Program {
        return parseProgram(getStringSharedPref(context,"your_prefs","program"))
    }
    fun getExercise(context: Context) : Exercise {
        return parseExercise(Utils().getStringSharedPref(context,"your_prefs","exercise"))
    }
    fun getExerciseFull(context: Context) : ExerciseFull {
        return parseExerciseFull(Utils().getStringSharedPref(context,"your_prefs","exercise"))
    }
    fun getExerciseList(context: Context) : ExerciseList {
        return parseExerciseList(Utils().getStringSharedPref(context,"your_prefs","outputSession"))
    }
    fun getSession(context: Context) : Session {
        return parseSession(getStringSharedPref(context,"your_prefs","session"))
    }

    private fun parseExercise(str:String) : Exercise {
        val type = object : TypeToken<Exercise>() {}.type
        return Gson().fromJson<Exercise>(str,type)
    }
    fun parseSessionData(str:String) : SessionData {
        val type = object : TypeToken<SessionData>() {}.type
        return Gson().fromJson<SessionData>(str,type)
    }
    fun parseProgramTracking(str:String) : ProgramTracking {
        return Gson().fromJson<ProgramTracking>(str,
            object:TypeToken<ProgramTracking>() {}.type)
    }
    fun parseExerciseStorage(str:String) : ExerciseStorage {
        val type = object : TypeToken<ExerciseStorage>() {}.type
        return Gson().fromJson<ExerciseStorage>(str,type)
    }
    fun parseExerciseProgram(str:String) : ExerciseProgram {
        val type = object : TypeToken<ExerciseProgram>() {}.type
        return Gson().fromJson<ExerciseProgram>(str,type)
    }
    private fun parseExerciseFull(str:String) : ExerciseFull {
        val type = object : TypeToken<ExerciseFull>() {}.type
        return Gson().fromJson<ExerciseFull>(str,type)
    }
    fun parseExerciseList(str:String) : ExerciseList {
        val type = object : TypeToken<ExerciseList>() {}.type
        return Gson().fromJson<ExerciseList>(str,type)
    }
    fun parseProgram(str:String) : Program {
        val type = object : TypeToken<Program>() {}.type
        return Gson().fromJson<Program>(str,type)
    }
    fun parseSession(str:String) : Session {
        val type = object : TypeToken<Session>() {}.type
        return Gson().fromJson<Session>(str,type)
    }

    fun widthHeight(w:Int,h:Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(w,h)
    }
    fun decimalCheck(number: Double): Number {
        return if (kotlin.math.ceil(number)-number == 0.0) {
            number.toInt()
        } else {
            number
        }
    }
    fun setMargins(view: View,left: Int, top: Int, right: Int,bottom: Int) {
        if (view.layoutParams is MarginLayoutParams) {
            val p = view.layoutParams as MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }
    fun forceKeyboard(context: Context,editText: EditText) {
        val imm: InputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
    fun writeProgram(context: Context,program: Program) {
        val rootPath: File? = context.filesDir
        val programFolder = File(rootPath, "/programs/")
        val thisProgramFolder = File(programFolder,"/"+program.title+"/")

        val file = File(thisProgramFolder,"workout.json")
        file.writeText(Gson().toJson(program))
    }
    fun mkProgramDir(context: Context,programName:String): File {
        val programFolder = File(context.filesDir, "programs/$programName/")
        programFolder.mkdirs()
        return programFolder
    }
    fun exerciseStrFromFile(context:Context, title: String): String {
        val rootPath: File? = context.filesDir
        val exerciseFolder = File(rootPath, "/exercises/")

        return File(exerciseFolder, "/$title/exercise.json").readText()
    }
    fun removePunctuationCheck(string: String): String {
        var newString = string
        if (newString.contains("<")) {
            newString = newString.replace("<","--less_than--")
        }
        if (newString.contains(">")) {
            newString = newString.replace(">","--more_than--")
        }
        if (newString.contains(":")) {
            newString = newString.replace(":","--colon--")
        }
        if (newString.contains("\"")) {
            newString = newString.replace("\"","--quotation--")
        }
        if (newString.contains("/")) {
            newString = newString.replace("/","--forward_slash--")
        }
        if (newString.contains("\\")) {
            newString = newString.replace("\\","--back_slash--")
        }
        if (newString.contains("|")) {
            newString = newString.replace("|","--pipe--")
        }
        if (newString.contains("?")) {
            newString = newString.replace("?","--question_mark--")
        }
        if (newString.contains("*")) {
            newString = newString.replace("*","--asterisk--")
        }
        return newString
    }
    fun replacePunctuationCheck(string: String): String {
        var newString = string
        if (newString.contains("--less_than--")) {
            newString = newString.replace("--less_than--",">")
        }
        if (newString.contains("--more_than--")) {
            newString = newString.replace("--more_than--",">")
        }
        if (newString.contains("--colon--")) {
            newString = newString.replace("--colon--",":")
        }
        if (newString.contains("--quotation--")) {
            newString = newString.replace("--quotation--","\"")
        }
        if (newString.contains("--forward_slash--")) {
            newString = newString.replace("--forward_slash--","/")
        }
        if (newString.contains("--back_slash--")) {
            newString = newString.replace("--back_slash--","\\")
        }
        if (newString.contains("--pipe--")) {
            newString = newString.replace("--pipe--","|")
        }
        if (newString.contains("--question_mark--")) {
            newString = newString.replace("--question_mark--","?")
        }
        if (newString.contains("--asterisk--")) {
            newString = newString.replace("--asterisk--","*")
        }
        return newString
    }
    fun saveViewWidth(context: Context,viewArr:List<View>) {
        var i = 0
        val runArr = mutableListOf<Int>()

        viewArr[viewArr.count()-1].post({
            fun run(viewArr:List<View>) {
                for (box in viewArr) {
                    box.post({
                        fun subRun(): Int {
                            return box.width
                            //saveStringSharedPref(context,"size",box.tag.toString(),width.toString())
                        }
                        val a = subRun()
                        runArr.add(a)
                    })
                }
            }
            run(viewArr)
        })
    }
    fun isOriginalTitle(programName:String,context: Context): Boolean {
        getFilePath(context).walk().maxDepth(1).forEach {
            val str = it.toString().removePrefix(getFilePath(context).toString()+"/")
            if (str == programName) {
                return false
            }
        }
        return true
    }
}

class CustomsArrayAdapter(
    contex: Context,
    textViewResourceId: Int,
    private val myImageArray: MutableList<String>,
    private val v: AutoCompleteTextView,
    private val baseLayout: View
) :
    ArrayAdapter<Any?>(contex, textViewResourceId, myImageArray as List<Any?>) {
    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val tv = TextView(context)
        tv.text = myImageArray[position]
        tv.isLongClickable = true
        tv.setOnLongClickListener{
            deleteSuggestion(myImageArray,position)
            return@setOnLongClickListener true
        }
        tv.setOnClickListener{
            v.setText(tv.text.toString())
        }
        return tv
    }
    private fun deleteSuggestion(
        myImageArray: MutableList<String>,
        position: Int
    ) {
        val popup = View.inflate(context,
            com.example.trainingmanager.R.layout.delete_suggestion_popup,null)
        val yesBtn = popup.findViewWithTag<Button>("yes_button")
        val noBtn = popup.findViewWithTag<Button>("no_button")


        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it

        val popupWindow = PopupWindow(popup, width, height, focusable)

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(baseLayout, Gravity.CENTER, 0, 0)

        yesBtn.setOnClickListener{
            myImageArray.removeAt(position)
            popupWindow.dismiss()
        }
        noBtn.setOnClickListener{
            popupWindow.dismiss()
        }
    }
}
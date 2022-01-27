
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.example.trainingmanager.R

class ExerciseSetupPopup (
    context: Context
    ): PopupWindow(context){
    private var popupWindow: PopupWindow
    private var popupView: View = View.inflate(context, R.layout.exercise_setup_popup,null)
    private var setupText: EditText = popupView.findViewById(R.id.exercise_setup_tv)
    private var doneButton: Button = popupView.findViewById(R.id.done_button)

    init {
        // create the popup window
        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        popupWindow = PopupWindow(popupView, width, height, focusable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 30F
        }
    }
    fun setText(string: String) {
        setupText.setText(string)
    }
    fun getText(): String {
        return setupText.text.toString()
    }
    fun getButton(): Button {
        return doneButton
    }
    fun show(anchor: View) {
        // Animation to make it appear and disappear like a Dialog
        animationStyle = android.R.style.Animation_Dialog

        // Show it
        popupWindow.showAtLocation(anchor, Gravity.CENTER, 0, 0)
    }
    fun setTheWidth(width: Int) {
        popupWindow.width = width
    }
    override fun dismiss() {
        popupWindow.dismiss()
    }
}
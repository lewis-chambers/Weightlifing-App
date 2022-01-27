import android.content.Context
import android.os.Build
import android.view.Gravity.CENTER
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.trainingmanager.R

class RenamingPopup(
    context: Context
): PopupWindow(context){
    private lateinit var popupWindow: PopupWindow
    init {
        val popup = View.inflate(context,R.layout.rename_program_popup,null)

        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it

        popupWindow = PopupWindow(popup, width, height, focusable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 30F
        }
    }
    fun show(anchor: View) {
        // Grab the pixel count for how far down you want to put it.
        // toolbar_height is 56dp for me

        // Animation to make it appear and disappear like a Dialog
        animationStyle = android.R.style.Animation_Dialog

        // Show it
        popupWindow.showAtLocation(anchor, CENTER, 0, 0)
    }
    fun getLayout(): ConstraintLayout {
        return (popupWindow.contentView as ConstraintLayout)
    }
    override fun dismiss() {
        popupWindow.dismiss()
    }
}
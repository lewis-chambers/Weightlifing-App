import android.content.Context
import androidx.appcompat.app.AlertDialog

class WarningPopup(
    val context: Context,
    msg: String,
    val cmd: () -> Unit
) {
    init {
        var output: Boolean? = null

        val builder = AlertDialog.Builder(context)

        builder.setTitle("Warning")
        builder.setMessage(msg)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setIconAttribute(android.R.attr.alertDialogIcon)

        builder.setPositiveButton("Yes"){dialogInterface, i ->
            cmd()
        }
        builder.setNegativeButton("No") { dialogInterface, i ->
            //something
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}
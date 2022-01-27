
import android.os.Handler
import android.os.Parcelable
import android.widget.TextView
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Parcelize
class Stopwatch(val format: String) : Parcelable {
    @IgnoredOnParcel
    var seconds: Int = 0
    @IgnoredOnParcel
    var running: Boolean =  false
    @IgnoredOnParcel
    lateinit var startTime: String

    fun initialise(view: TextView) {
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                val hours = seconds / 60 / 60 % 60
                val minutes = seconds / 60 % 60
                val secs = seconds % 60
                var time = ""
                when (":".toRegex().findAll(format).count()) {
                    0 -> {
                        error("Invalid Format")
                    }
                    1 -> {
                        time = java.lang.String.format(
                            Locale.getDefault(),
                            format,minutes, secs
                        )
                    }
                    2 -> {
                        time = java.lang.String.format(
                            Locale.getDefault(),
                            format,hours,minutes, secs
                        )
                    }
                }


                //Set the text view text.
                view.text = time
                //If running is true, increment the
                // seconds variable.
                if (running) {
                    seconds++
                }

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000)
            }
        })
    }
    fun calibrate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentTime = sdf.format(Date())

        val diffInMillies = abs(sdf.parse(currentTime)!!.time - sdf.parse(startTime)!!.time)

        seconds = java.util.concurrent.TimeUnit.SECONDS.convert(diffInMillies,
            java.util.concurrent.TimeUnit.MILLISECONDS
        ).toInt()
    }
    fun attachStartTime(time: String) {
        startTime = time
    }
    fun start() {
        seconds = 0
        running = true
    }
    fun stop() {
        running = false
        seconds = 0
    }
}
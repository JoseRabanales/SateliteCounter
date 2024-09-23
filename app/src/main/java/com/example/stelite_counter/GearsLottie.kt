import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.stelite_counter.R

class AnimationDialog(private val activity: AppCompatActivity) : Dialog(activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.gears_animation)

        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
        }, 5000)
    }
}

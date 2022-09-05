package com.faptastic.webcamx

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.faptastic.webcamx.camerax.CameraX
import com.faptastic.webcamx.composable.CameraCompose
import com.faptastic.webcamx.ui.theme.CameraXComposeTheme
import com.faptastic.webcamx.utils.Commons.allPermissionsGranted
import com.faptastic.webcamx.utils.Commons.showLog
import java.util.*

class MainActivity : ComponentActivity() {
    private var cameraX: CameraX = CameraX(this, this)

    var timerActive = false
    var myTimer:Timer = Timer() // Old java language stuff.
    val timerMinuteFreq:Long = 45

    fun onCaptureClick(doUpload:Boolean = false):Unit
    {
        if (allPermissionsGranted(this)) { cameraX.capturePhoto(doUpload) }
    }

    var onTimerClickVar: () -> Unit = {

        if (timerActive == false)
        {
            myTimer = Timer()

            //if (allPermissionsGranted(this)) { cameraX.capturePhoto() }
            // Not 100% happy about this unused variable either
            myTimer.apply {
                val task = object : TimerTask() {
                    override fun run() {
                        //ticks++
                        showLog("Running timer upload task...")
                        onCaptureClick(true)

                    }
                }
                scheduleAtFixedRate(task, 2000, 1000*60*timerMinuteFreq) // every 45 minutes

                //scheduleAtFixedRate(task, 2000, 2000) // every 45 minutes
            }

            Toast.makeText(this, "Timer started. ", Toast.LENGTH_SHORT).show()
            timerActive = true
        }
        else
        {
            myTimer.cancel()

            Toast.makeText(this, "Timer stopped.", Toast.LENGTH_SHORT).show()
            timerActive = false
        }


     //   onCaptureClick(true)

    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // So the screen stays ON
        setContent {
            CameraXComposeTheme {
                CameraCompose(this, cameraX = cameraX, onTimerClickVar) {
                    onCaptureClick()
                }
            }
        }


    }

}

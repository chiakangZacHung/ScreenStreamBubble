package com.torrydo.testfloatingbubble

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class MainActivityKt : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            startBubbleService()
        }
    }

    private fun startBubbleService() {
        val bubbleIntent = Intent(this, MyServiceKt::class.java)
        startService(bubbleIntent)
    }
}
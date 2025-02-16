package com.torrydo.testfloatingbubble

import android.content.Intent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import com.torrydo.floatingbubbleview.CloseBubbleBehavior
import com.torrydo.floatingbubbleview.FloatingBubbleListener
import com.torrydo.floatingbubbleview.helper.NotificationHelper
import com.torrydo.floatingbubbleview.helper.ViewHelper
import com.torrydo.floatingbubbleview.service.expandable.BubbleBuilder
import com.torrydo.floatingbubbleview.service.expandable.ExpandableBubbleService
import com.torrydo.floatingbubbleview.service.expandable.ExpandedBubbleBuilder


class MyServiceKt : ExpandableBubbleService() {

    override fun startNotificationForeground() {

        val noti = NotificationHelper(this)
        noti.createNotificationChannel()
        startForeground(noti.notificationId, noti.defaultNotification())
    }

    override fun onCreate() {
        super.onCreate()
        minimize()
    }

    override fun configBubble(): BubbleBuilder? {
        val bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)

        bubbleView.setOnClickListener {
            val intent = Intent().apply {
                setClassName(
                    "info.dvkr.screenstream.dev",
                    "info.dvkr.screenstream.SingleActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }

        return BubbleBuilder(this)

            // set bubble view
            .bubbleView(bubbleView)

            .triggerClickablePerimeterPx(5f)

            // or our sweetie, Jetpack Compose
//            .bubbleCompose {
//                BubbleCompose(expand = { expand() })
//            }
//            .forceDragging(false)

            // set style for the bubble, fade animation by default
            .bubbleStyle(null)

            // set start location for the bubble, (x=0, y=0) is the top-left
            .startLocation(100, 100)    // in dp
            .startLocationPx(100, 100)  // in px

            // enable auto animate bubble to the left/right side when release, true by default
            .enableAnimateToEdge(true)

            // set close-bubble view
            .closeBubbleView(ViewHelper.fromDrawable(this, R.drawable.ic_close_bubble, 60, 60))

            // set style for close-bubble, null by default
            .closeBubbleStyle(null)

            // DYNAMIC_CLOSE_BUBBLE: close-bubble moving based on the bubble's location
            // FIXED_CLOSE_BUBBLE (default): bubble will automatically move to the close-bubble when it reaches the closable-area
            .closeBehavior(CloseBubbleBehavior.FIXED_CLOSE_BUBBLE)

            // the more value (dp), the larger closeable-area
            .distanceToClose(100)

            // enable bottom background, false by default
            .bottomBackground(false)

            .addFloatingBubbleListener(object : FloatingBubbleListener {
                override fun onFingerMove(
                    x: Float,
                    y: Float
                ) {
                } // The location of the finger on the screen which triggers the movement of the bubble.

                override fun onFingerUp(
                    x: Float,
                    y: Float
                ) {
                }   // ..., when finger release from bubble

                override fun onFingerDown(x: Float, y: Float) {} // ..., when finger tap the bubble
            })

    }

    override fun configExpandedBubble(): ExpandedBubbleBuilder? {

        val expandedView = LayoutInflater.from(this).inflate(R.layout.layout_view_test, null)
        expandedView.findViewById<View>(R.id.btn).setOnClickListener {
            minimize()
        }

        return ExpandedBubbleBuilder(this)
//            .expandedView(expandedView)
            .expandedCompose {
                TestComposeView(popBack = {minimize()})
            }
            .onDispatchKeyEvent {
                if(it.keyCode == KeyEvent.KEYCODE_BACK){
                    minimize()
                }
                null
            }
            .startLocation(0, 0)
            .draggable(true)
            .style(null)
            .fillMaxWidth(false)
            .enableAnimateToEdge(true)
            .dimAmount(0.5f)
    }
}
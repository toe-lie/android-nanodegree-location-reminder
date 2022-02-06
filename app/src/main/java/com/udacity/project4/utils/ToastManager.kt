package com.udacity.project4.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource

// Reference: https://knowledge.udacity.com/questions/770651#770668
object ToastManager {
    private val idlingResource: CountingIdlingResource = CountingIdlingResource("toast")

    // For testing
    fun getIdlingResource(): IdlingResource {
        return idlingResource
    }

    fun increment() {
        idlingResource.increment()
    }

    private val listener: View.OnAttachStateChangeListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View?) {
                if (!idlingResource.isIdleNow) {
                    idlingResource.decrement()
                }
            }

            override fun onViewDetachedFromWindow(v: View?) {}
        }

    fun showToast(context: Context?, text: CharSequence?, duration: Int): Toast {
        val t: Toast = Toast.makeText(context, text, duration)
        t.view?.addOnAttachStateChangeListener(listener)
        return t
    }

}
package vaida.dryzaite.currencyconverter.util

import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import vaida.dryzaite.currencyconverter.R

fun Window.setStatusBar(context: Context) {
    this.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    this.statusBarColor = ContextCompat.getColor(context, android.R.color.transparent)
    this.setBackgroundDrawableResource(R.drawable.bg_gradient)
}

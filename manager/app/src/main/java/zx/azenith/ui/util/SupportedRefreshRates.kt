package zx.azenith.ui.util

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

fun getSupportedRefreshRates(context: Context): List<String> {
    val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
    }


    val supportedRR = display?.supportedModes?.map { it.refreshRate.toInt() }?.distinct()?.sorted() ?: listOf(60)
    
    val finalModes = mutableListOf("Default")

    val standardModes = listOf(60, 90, 120, 144)
    
    standardModes.forEach { mode ->
        if (supportedRR.any { it >= mode - 1 && it <= mode + 1 }) { // Toleransi +/- 1Hz
            finalModes.add(mode.toString())
        }
    }
    
    return finalModes
}

fun getSupportedRefreshRatesPicker(context: Context): List<String> {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }

    val supportedRR = display?.supportedModes
        ?.map { it.refreshRate.toInt() }
        ?.distinct()
        ?.sortedDescending() ?: listOf(60)
    
    // Inisialisasi list kosong tanpa "Default"
    val finalModes = mutableListOf<String>()

    val standardModes = listOf(144, 120, 90, 60)
    
    standardModes.forEach { mode ->
        if (supportedRR.any { it in (mode - 1)..(mode + 1) }) {
            finalModes.add(mode.toString())
        }
    }
    
    return finalModes 
}

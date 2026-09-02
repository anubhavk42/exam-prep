package com.anubhav.diprep.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * No periodic work here. onUpdate fires only when the widget is added/resized
 * (updatePeriodMillis is 0). All other refreshes are pushed from the foreground
 * via [CountdownWidget.requestRefresh].
 */
class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CountdownWidget.updateAll(context)
    }

    override fun onEnabled(context: Context) {
        CountdownWidget.updateAll(context)
    }
}

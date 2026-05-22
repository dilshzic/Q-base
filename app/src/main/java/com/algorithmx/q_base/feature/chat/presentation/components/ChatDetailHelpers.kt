package com.algorithmx.q_base.feature.chat.presentation.components
import com.algorithmx.q_base.core.state.AppAccessState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun accessStateLabel(state: AppAccessState): String = when (state) {
    AppAccessState.RestoringSession -> "Restoring"
    AppAccessState.OnlineReady -> "Online"
    AppAccessState.SignedInOffline -> "Offline"
    AppAccessState.GuestOnline -> "Guest Online"
    AppAccessState.OfflineGuest -> "Guest Offline"
}

fun formatDateRelatively(timestamp: Long): String {
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        isSameDay(now, messageDate) -> "Today"
        isYesterday(now, messageDate) -> "Yesterday"
        else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, date: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply { 
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, date)
}
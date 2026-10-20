package dev.johan.tvlauncher.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.johan.tvlauncher.R
import dev.johan.tvlauncher.ui.theme.Palette
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Greeting and date on the left; quick actions and a large clock on the right. Ticks once per minute. */
@Composable
fun Header(modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
            delay(java.time.Duration.between(LocalDateTime.now(), next).toMillis().coerceAtLeast(250))
            now = LocalDateTime.now()
        }
    }
    val locale = Locale.getDefault()
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val time = remember(now.hour, now.minute, timePattern) { now.format(DateTimeFormatter.ofPattern(timePattern, locale)) }
    val date = remember(now.toLocalDate(), locale) {
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)).replaceFirstChar { it.titlecase(locale) }
    }
    val greeting = stringResource(
        when (now.hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
            Text(date, style = MaterialTheme.typography.bodyLarge, color = Palette.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
        Spacer(Modifier.width(28.dp))
        Text(time, style = MaterialTheme.typography.displayLarge, color = Palette.TextPrimary)
    }
}

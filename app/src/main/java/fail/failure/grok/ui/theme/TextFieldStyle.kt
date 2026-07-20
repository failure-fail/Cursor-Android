package fail.failure.grok.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A filled rounded field with no visible outline, closer to what Grok's own app uses than a
 * stock Material outlined box - a subtle background fill instead of a drawn border. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cursorFilledTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = GrokSurfaceRaised,
    unfocusedContainerColor = GrokSurfaceRaised,
    disabledContainerColor = GrokSurfaceRaised,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
)

val GrokTextFieldShape = RoundedCornerShape(16.dp)

/** A fully-rounded pill shape for [androidx.compose.material3.FilterChip], matching the pill
 * language used everywhere else (buttons, the bottom bar, the segmented toggle) - the stock
 * FilterChip shape is only lightly rounded, which read as a mismatched, generic Material chip
 * next to those. */
val GrokFilterChipShape = RoundedCornerShape(50)

/** Selected FilterChips otherwise fall back to Material's default `secondaryContainer`, an
 * indigo/purple that was never part of this theme's palette - happened to work out unnoticed
 * until a selected chip actually rendered and showed a jarring off-brand blue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cursorFilterChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = GrokSurfaceRaised,
    labelColor = GrokTextSecondary,
    iconColor = GrokTextSecondary,
    selectedContainerColor = GrokAccent,
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White,
)

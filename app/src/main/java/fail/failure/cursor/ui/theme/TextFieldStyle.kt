package fail.failure.cursor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A filled rounded field with no visible outline, closer to what Cursor's own app uses than a
 * stock Material outlined box - a subtle background fill instead of a drawn border. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cursorFilledTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CursorSurfaceRaised,
    unfocusedContainerColor = CursorSurfaceRaised,
    disabledContainerColor = CursorSurfaceRaised,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
)

val CursorTextFieldShape = RoundedCornerShape(16.dp)

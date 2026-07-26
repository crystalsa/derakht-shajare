import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
fun test(modifier: Modifier) {
    modifier.combinedClickable(
        onDoubleClick = {},
        onClick = {}
    )
}

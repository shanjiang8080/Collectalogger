package com.example.collectalogger2

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.collectalogger2.util.LocalNavEventBus
import kotlinx.coroutines.launch

private class Destination(
    val onNavigate: () -> Unit,
    val name: String,
    val icon: ImageVector,
)

@Composable
fun BottomAppBar(
    onNavigateToWishlist: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    var selectedDestination by rememberSaveable { mutableIntStateOf(1) /* whatever the gallery is */ }
    val destinations = listOf(
        Destination(onNavigateToWishlist, "Wishlist", Icons.Rounded.ShoppingCart),
        Destination(onNavigateToGallery, "Gallery", Icons.Rounded.Home),
        Destination(onNavigateToSettings, "Settings", Icons.Rounded.Settings),
    )
    val scope = rememberCoroutineScope()
    val navBus = LocalNavEventBus.current
    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets
    )
    {
        // icons, add more as necessary.
        destinations.forEachIndexed { index, destination ->
            NavigationBarItem(
                selected = selectedDestination == index,
                onClick = {
                    // If you press it again, do it
                    if (selectedDestination == index) {
                        // Send the scroll event through the nav event bus
                        scope.launch { navBus.emitScrollEvent(destination.name) }
                    } else {
                        destination.onNavigate()
                        selectedDestination = index
                    }
                },
                icon = {
                    Icon(
                        destination.icon,
                        contentDescription = "${destination.name} icon"
                    )
                },
                label = {
                    Text(destination.name)
                }
            )
        }

    }
}

@Preview(
    showBackground = true
)
@Composable
fun BarPreview() {
    BottomAppBar({}, {}, {})
}

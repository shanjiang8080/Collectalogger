package com.example.collectalogger2

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.collectalogger2.navigation.Gallery
import com.example.collectalogger2.navigation.WishList

@Composable
fun BottomAppBar(navController: NavController?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    )
    {
        // icons, add more as necessary.
        BottomNavItem("Wishlist", Icons.Rounded.ShoppingCart)
        { navController?.navigate(route = WishList) }
        BottomNavItem("Gallery", Icons.Rounded.Home)
        { navController?.navigate(route = Gallery) }
    }
}

@Composable
fun BottomNavItem(text: String, icon: ImageVector, onClicked: () -> Unit) {
    FilledTonalButton(
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
        onClick = { onClicked() }
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text)
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
fun ShoppingNavItem() {
    BottomNavItem("Wishlist", Icons.Rounded.ShoppingCart) {}
}

@Preview(
    showBackground = true
)
@Composable
fun BarPreview() {
    BottomAppBar(null)
}

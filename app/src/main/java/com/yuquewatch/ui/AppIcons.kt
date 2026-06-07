package com.yuquewatch.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Single place to change every app icon. Swap an assignment here and it updates everywhere.
 * (material-icons-core only; to use richer glyphs later, add material-icons-extended and
 * point these at the new vectors — no call-site changes needed.)
 */
object AppIcons {
    // Home tabs
    val tabQuick: ImageVector = Icons.Filled.Add
    val tabMini: ImageVector = Icons.Filled.Edit
    val tabMine: ImageVector = Icons.Filled.Person

    // 我的 actions
    val favorites: ImageVector = Icons.Filled.Star
    val recent: ImageVector = Icons.Filled.Refresh
    val search: ImageVector = Icons.Filled.Search
    val settings: ImageVector = Icons.Filled.Settings

    // Settings menu
    val personal: ImageVector = Icons.Filled.Person
    val reading: ImageVector = Icons.Filled.Menu
    val account: ImageVector = Icons.Filled.Lock
    val cache: ImageVector = Icons.Filled.Delete
    val debug: ImageVector = Icons.Filled.Search
    val about: ImageVector = Icons.Filled.Info
    val knowledge: ImageVector = Icons.Filled.List
}

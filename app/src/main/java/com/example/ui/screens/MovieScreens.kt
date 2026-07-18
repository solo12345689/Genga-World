package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.model.Episode
import com.example.data.model.MovieSubject
import com.example.data.model.StreamItem
import com.example.data.model.SubtitleItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovieViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.net.Uri
import androidx.compose.ui.text.input.PasswordVisualTransformation
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape

@Composable
fun MovieBoxApp(viewModel: MovieViewModel) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        MainContent(viewModel)
    }
}

@Composable
fun LockoutScreen(viewModel: MovieViewModel) {
    var passwordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140204),
                        ObsidianDark
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Earth/Globe Icon that represents geofencing
            Icon(
                imageVector = Icons.Filled.PublicOff,
                contentDescription = "Geofenced",
                tint = CinematicRed,
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Service Restriction",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Clickable text to unlock developer mode
            Text(
                text = "This application is not available in your current region.",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clickable(onClick = {
                        viewModel.passwordAttempts++
                        if (viewModel.passwordAttempts >= 10) {
                            viewModel.showPasswordDialog = true
                        }
                    })
                    .testTag("lockout_message")
            )

            Spacer(modifier = Modifier.height(40.dp))

            // User friendly help block for development environments
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STUDIO EMULATOR MODE ACTIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To bypass geographic carrier restrictions, tap the message above 10 times and enter security access key or use the Auto-Bypass below.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.submitDeveloperPassword("or666")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auto_bypass_button")
                    ) {
                        Icon(Icons.Filled.Terminal, contentDescription = "Bypass", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Bypass Lockout", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Lab password popup dialog
        if (viewModel.showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showPasswordDialog = false },
                title = { Text("Laboratory Security Gateway", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter decryption or bypass developer credentials:", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Gateway Key") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CinematicRed,
                                unfocusedBorderColor = TextSecondary
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("password_input")
                        )
                        if (loginError) {
                            Text("Invalid security key. Hint: 'or666'", color = CinematicRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = viewModel.submitDeveloperPassword(passwordInput)
                            if (success) {
                                loginError = false
                            } else {
                                loginError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CinematicRed)
                    ) {
                        Text("Verify Key")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showPasswordDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = ObsidianCard
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainContent(viewModel: MovieViewModel) {
    var activeTab by remember { mutableStateOf("home") } // home, search, watchlist, history, account

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (viewModel.playInfo == null) {
                    NavigationBar(
                        containerColor = ObsidianDark,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "home",
                            onClick = { activeTab = "home" },
                            icon = { Icon(if (activeTab == "home") Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CinematicRed,
                                selectedTextColor = CinematicRed,
                                indicatorColor = ObsidianCard,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = activeTab == "search",
                            onClick = { activeTab = "search" },
                            icon = { Icon(if (activeTab == "search") Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "Search") },
                            label = { Text("Search", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CinematicRed,
                                selectedTextColor = CinematicRed,
                                indicatorColor = ObsidianCard,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_search")
                        )
                        NavigationBarItem(
                            selected = activeTab == "watchlist",
                            onClick = { activeTab = "watchlist" },
                            icon = { Icon(if (activeTab == "watchlist") Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = "Watchlist") },
                            label = { Text("Watchlist", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CinematicRed,
                                selectedTextColor = CinematicRed,
                                indicatorColor = ObsidianCard,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_watchlist")
                        )
                        NavigationBarItem(
                            selected = activeTab == "history",
                            onClick = { activeTab = "history" },
                            icon = { Icon(if (activeTab == "history") Icons.Filled.History else Icons.Outlined.History, contentDescription = "History") },
                            label = { Text("History", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CinematicRed,
                                selectedTextColor = CinematicRed,
                                indicatorColor = ObsidianCard,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_history")
                        )
                        NavigationBarItem(
                            selected = activeTab == "account",
                            onClick = { activeTab = "account" },
                            icon = { Icon(if (activeTab == "account") Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Account") },
                            label = { Text("Account", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CinematicRed,
                                selectedTextColor = CinematicRed,
                                indicatorColor = ObsidianCard,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_account")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    "home" -> HomeScreen(viewModel)
                    "search" -> SearchScreen(viewModel)
                    "watchlist" -> WatchlistScreen(viewModel)
                    "history" -> HistoryScreen(viewModel)
                    "account" -> AccountScreen(viewModel)
                }
            }
        }



        // Detail screen slide-up overlay
        AnimatedVisibility(
            visible = viewModel.selectedSubject != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
        ) {
            DetailScreen(viewModel)
        }

        // Simulated Video Player overlay
        AnimatedVisibility(
            visible = viewModel.playInfo != null,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            PlayerScreen(viewModel)
        }

        // Playback loading overlay
        if (viewModel.isPlaybackLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, // block all clicks while loading
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CinematicRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching stream links...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1E22),
                Color(0xFF2E2E32),
                Color(0xFF1E1E22),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun DashboardShimmerScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Shimmer Hero Banner Placeholder
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .shimmerEffect()
            ) {
                // Bottom shading gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianDark)
                            )
                        )
                )
                
                // Text lines placeholders
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }

        // Horizontal Row Placeholder 1
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 12.dp)
                        .width(140.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) {
                        Column(modifier = Modifier.width(110.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(165.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips placeholder
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(4) {
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmerEffect()
                    )
                }
            }
        }

        // Grid items placeholder
        items(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MovieViewModel) {
    val trending by viewModel.trendingMovies.collectAsState()
    val categoryMovies by viewModel.categoryMovies.collectAsState()

    if (viewModel.isDashboardLoading) {
        DashboardShimmerScreen()
    } else {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // Hero Slider/Banner Section
        if (trending.isNotEmpty()) {
            item {
                val heroMovie = trending.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clickable { viewModel.selectSubject(heroMovie) }
                ) {
                    AsyncImage(
                        model = heroMovie.poster,
                        contentDescription = heroMovie.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient shading
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ObsidianDark
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CinematicRed),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "TRENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = heroMovie.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = heroMovie.genres.joinToString(" • "),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Button(
                                onClick = { viewModel.selectSubject(heroMovie) },
                                colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Details & Play", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Horizontal list of Trending items (without "Editor's Choice" label)
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trending) { movie ->
                        MovieGridItem(movie) {
                            viewModel.selectSubject(movie)
                        }
                    }
                }
            }
        }

        // Horizontal Category filter chips (without "Explore Categories" label)
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                val categories = listOf(
                    2 to "Movies",
                    5 to "TV/Series",
                    8 to "Anime",
                    18 to "Asian/Regional"
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = viewModel.selectedCategoryTab == cat.first,
                            onClick = { viewModel.loadCategoryList(cat.first) },
                            label = { Text(cat.second) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CinematicRed,
                                selectedLabelColor = Color.White,
                                containerColor = ObsidianCard,
                                labelColor = TextSecondary
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        // Category Movies Grid list
        items(categoryMovies.chunked(3)) { rowMovies ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMovies.forEach { movie ->
                    Box(modifier = Modifier.weight(1f)) {
                        MovieGridItem(movie) {
                            viewModel.selectSubject(movie)
                        }
                    }
                }
                // Fill up remaining grid slots
                if (rowMovies.size < 3) {
                    repeat(3 - rowMovies.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Bottom space padding
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
    }
}

@Composable
fun MovieGridItem(movie: MovieSubject, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = movie.poster,
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Rating Tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = "Rating", tint = AccentAmber, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(movie.rating, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = movie.year,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun SearchScreen(viewModel: MovieViewModel) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val results by viewModel.searchResults.collectAsState()
    var searchInput by remember { mutableStateOf(viewModel.searchQuery) }
    var isSearchSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSearchSuggestions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchInput,
            onValueChange = {
                searchInput = it
                isSearchSubmitted = false
                viewModel.searchMovies(it)
            },
            placeholder = { Text("Search", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextSecondary) },
            trailingIcon = {
                if (searchInput.isNotEmpty()) {
                    IconButton(onClick = {
                        searchInput = ""
                        isSearchSubmitted = false
                        viewModel.searchMovies("")
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    isSearchSubmitted = true
                    focusManager.clearFocus()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = ObsidianCard,
                unfocusedContainerColor = ObsidianCard,
                focusedBorderColor = CinematicRed,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        val suggestions by viewModel.searchSuggestions.collectAsState()

        if (viewModel.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CinematicRed)
            }
        } else if (!isSearchSubmitted || searchInput.isEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = if (searchInput.isEmpty()) "Trending Searches" else "Suggestions",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                itemsIndexed(suggestions) { index, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchInput = suggestion
                                isSearchSubmitted = true
                                viewModel.searchMovies(suggestion)
                                focusManager.clearFocus()
                            }
                            .background(ObsidianCard, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchInput.isEmpty()) {
                            Text(
                                text = "${index + 1}",
                                color = if (index < 3) CinematicRed else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.width(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Suggestion",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = suggestion,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (searchInput.isEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = "Trending",
                                tint = if (index < 3) CinematicRed else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SearchOff, contentDescription = "No Results", tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No results found", color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                viewModel.selectSubject(movie)
                            }
                            .background(ObsidianCard, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = movie.poster,
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 60.dp, height = 90.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movie.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(movie.genres.joinToString(", "), fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = "Rating", tint = AccentAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(movie.rating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(movie.year, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistScreen(viewModel: MovieViewModel) {
    val favorites by viewModel.favoritesList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshWatchlist()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
    ) {
        Text("My Watchlist", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = "Watchlist empty", tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your watchlist is empty", color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Maps entities back to subjects
                                val subject = MovieSubject(
                                    id = movie.id,
                                    name = movie.name,
                                    poster = movie.poster,
                                    isTvShow = movie.isTvShow,
                                    rating = movie.rating,
                                    year = movie.year
                                )
                                viewModel.selectSubject(subject)
                            }
                            .background(ObsidianCard, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = movie.poster,
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 50.dp, height = 75.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movie.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(movie.year + " • " + if (movie.isTvShow) "TV Show" else "Movie", fontSize = 13.sp, color = TextSecondary)
                        }
                        IconButton(onClick = {
                            val subject = MovieSubject(id = movie.id, name = movie.name, poster = movie.poster)
                            viewModel.toggleFavorite(subject)
                        }) {
                            Icon(Icons.Filled.BookmarkRemove, contentDescription = "Remove", tint = CinematicRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: MovieViewModel) {
    val history by viewModel.historyList.collectAsState()
    var expandedSubjectId by remember { mutableStateOf<String?>(null) }

    val groupedHistory = remember(history) {
        history.groupBy { it.subjectId }.map { entry ->
            entry.value.first() to entry.value
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
    ) {
        Text("Continue Watching", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 16.dp))

        if (groupedHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PlayCircleOutline, contentDescription = "History empty", tint = TextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No playback history available", color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedHistory) { (item, episodesList) ->
                    val isExpanded = expandedSubjectId == item.subjectId

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianCard, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.isTvShow) {
                                        expandedSubjectId = if (isExpanded) null else item.subjectId
                                    } else {
                                        // Movie: play directly
                                        val subject = MovieSubject(
                                            id = item.subjectId,
                                            name = item.name,
                                            poster = item.poster,
                                            isTvShow = item.isTvShow
                                        )
                                        viewModel.selectSubject(subject)
                                        viewModel.selectMovieDirect()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.poster,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 60.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (item.isTvShow) {
                                    Text(
                                        text = "${episodesList.size} episodes watched • Tap to view",
                                        fontSize = 13.sp,
                                        color = AccentAmber,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    val percentage = if (item.totalTime > 0) (item.seeTime.toFloat() / item.totalTime.toFloat()) else 0f
                                    val formattedProgress = formatTime(item.seeTime) + " / " + formatTime(item.totalTime)
                                    Text(formattedProgress, fontSize = 12.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = percentage.coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = CinematicRed,
                                        trackColor = Color.Gray.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                episodesList.forEach { viewModel.deleteHistoryItem(it.id) }
                            }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete All", tint = TextSecondary)
                            }
                        }

                        if (item.isTvShow && isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, start = 16.dp, end = 8.dp)
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                episodesList.forEach { epItem ->
                                    val percentage = if (epItem.totalTime > 0) (epItem.seeTime.toFloat() / epItem.totalTime.toFloat()) else 0f
                                    val formattedProgress = formatTime(epItem.seeTime) + " / " + formatTime(epItem.totalTime)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val subject = MovieSubject(
                                                    id = epItem.subjectId,
                                                    name = epItem.name,
                                                    poster = epItem.poster,
                                                    isTvShow = epItem.isTvShow
                                                )
                                                viewModel.selectSubject(subject)
                                                val episode = Episode(
                                                    episodeId = epItem.id,
                                                    title = "Season ${epItem.season} Episode ${epItem.episode}",
                                                    episodeNumber = epItem.episode,
                                                    seasonNumber = epItem.season
                                                )
                                                viewModel.selectEpisode(episode)
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Season ${epItem.season} Ep ${epItem.episode}", fontSize = 13.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(formattedProgress, fontSize = 11.sp, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = percentage.coerceIn(0f, 1f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(1.dp)),
                                                color = CinematicRed,
                                                trackColor = Color.Gray.copy(alpha = 0.3f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteHistoryItem(epItem.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabScreen(viewModel: MovieViewModel) {
    var hostText by remember { mutableStateOf(viewModel.mockHost) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Science, contentDescription = "Laboratory", tint = AccentAmber, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("BFF Laboratory Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Sandbox & geofencing simulator config", fontSize = 13.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Simulate Region Carrier Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Overrides SIM parameters in API handshake headers", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(16.dp))

                // Region choices matching protocol Section 9
                val regions = listOf(
                    Triple("90101", "us", "United States (Global)"),
                    Triple("40401", "in", "India (Hindi Dubs)"),
                    Triple("62101", "ng", "Nigeria (Nollywood)")
                )

                regions.forEach { reg ->
                    val isSelected = viewModel.customMcc == reg.first && viewModel.customCountryIso == reg.second
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.applyCustomSandbox(reg.first, reg.second) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.applyCustomSandbox(reg.first, reg.second) },
                            colors = RadioButtonDefaults.colors(selectedColor = CinematicRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(reg.third, color = TextPrimary, fontSize = 14.sp)
                            Text("MCC: ${reg.first} | ISO: ${reg.second}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BFF Proxy Host Configuration", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Route API traffic through your custom cloud instance", fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = hostText,
                    onValueChange = { hostText = it },
                    label = { Text("Server Host URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CinematicRed,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = CinematicRed,
                        cursorColor = CinematicRed
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("mock_host_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultHost = "http://127.0.0.1:3000"
                            hostText = defaultHost
                            viewModel.updateMockHost(defaultHost)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Local Server", color = AccentAmber, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            hostText = "http://127.0.0.1:3000"
                            viewModel.updateMockHost("http://127.0.0.1:3000")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("Default Local", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateMockHost(hostText.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save & Apply Host", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.resetSandbox() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Re-Lock Sandbox & Exit Lab", color = CinematicRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailScreen(viewModel: MovieViewModel) {
    val subject = viewModel.selectedSubject ?: return
    val isFav by viewModel.isFavoriteFlow(subject.id).collectAsState(false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Backdrop and poster cover
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    AsyncImage(
                        model = subject.poster,
                        contentDescription = subject.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Backdrop gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        ObsidianDark
                                    )
                                )
                            )
                    )

                    // Close Button
                    IconButton(
                        onClick = { viewModel.selectedSubject = null },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 16.dp, start = 16.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .testTag("detail_close_button")
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }

            // Title block
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject.name,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.toggleFavorite(subject) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) CinematicRed else TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = "Rating", tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(subject.rating, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(subject.year, color = TextSecondary, fontSize = 14.sp)
                        Text(subject.duration, color = TextSecondary, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Genres row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        subject.genres.forEach { gen ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = gen,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description text
                    Text(
                        text = subject.description.ifEmpty { "No description available for this item." },
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    // Live Cast Section
                    if (subject.cast.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(subject.cast) { actor ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(ObsidianCard)
                                    ) {
                                        if (actor.avatar.isNotEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = actor.avatar,
                                                contentDescription = actor.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = "Avatar placeholder",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = actor.name,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = actor.role.ifEmpty { "Cast" },
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    if (!subject.isTvShow) {
                        Button(
                            onClick = { viewModel.selectMovieDirect() },
                            colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("play_movie_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stream Movie in High Definition", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Text(
                            text = "Seasons",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Season Selector Horizontal chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            items(viewModel.seasonsList) { s ->
                                FilterChip(
                                    selected = viewModel.selectedSeason == s,
                                    onClick = { viewModel.loadEpisodesForSeason(s) },
                                    label = { Text("Season $s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CinematicRed,
                                        selectedLabelColor = Color.White,
                                        containerColor = ObsidianCard,
                                        labelColor = TextSecondary
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }

            // TV Show Episodes List Section
            if (subject.isTvShow) {
                items(viewModel.episodesList) { ep ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable { viewModel.selectEpisode(ep) }
                            .background(ObsidianCard, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ep.stillPath.ifEmpty { subject.poster },
                            contentDescription = ep.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 80.dp, height = 50.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Episode ${ep.episodeNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                            Text(
                                text = ep.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Play", tint = CinematicRed, modifier = Modifier.size(32.dp))
                    }
                }
            }

            // Bottom space
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PlayerScreen(viewModel: MovieViewModel) {
    val playInfo = viewModel.playInfo ?: return
    val subject = viewModel.selectedSubject ?: return
    val context = LocalContext.current

    val selectedStream = viewModel.selectedStream
    val streamUrl = selectedStream?.url ?: ""
    val selectedCookie = selectedStream?.cookie ?: ""
    val subtitleUrl = viewModel.selectedSubtitle?.url

    var activeSheet by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val playerHeight = minOf(screenWidth * (9f / 16f), screenHeight * 0.32f)
    LaunchedEffect(configuration.orientation) {
        isFullscreen = (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
    }
    var isControllerVisible by remember { mutableStateOf(true) }

    // Initialize Media3 ExoPlayer
    val exoPlayer = remember {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
            
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setMediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
                val decoders = androidx.media3.exoplayer.mediacodec.MediaCodecUtil.getDecoderInfos(mimeType, requiresSecure, requiresTunneling)
                decoders.sortedWith { d1, d2 ->
                    val n1 = d1.name.lowercase()
                    val n2 = d2.name.lowercase()
                    val sw1 = n1.contains("google") || n1.contains("c2.android") || n1.contains("sw")
                    val sw2 = n2.contains("google") || n2.contains("c2.android") || n2.contains("sw")
                    when {
                        sw1 && !sw2 -> -1
                        !sw1 && sw2 -> 1
                        else -> 0
                    }
                }
            }
        }
        
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .build().apply {
                playWhenReady = viewModel.isPlaying
            }
    }

    // Sync native playlist next/prev buttons transitions with ViewModel selections
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                viewModel.isPlaying = playWhenReady
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null && mediaItem.localConfiguration?.uri?.toString() == "http://dummy.mp4") {
                    val targetEpisodeId = mediaItem.mediaId
                    val targetEpisode = viewModel.episodesList.find { it.episodeId == targetEpisodeId }
                    if (targetEpisode != null) {
                        viewModel.selectEpisode(targetEpisode)
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    var currentProgressMs by remember { mutableStateOf(viewModel.playbackProgressMs) }
    var durationMs by remember { mutableStateOf(viewModel.mediaDurationMs) }

    // Update stream and subtitle configuration
    LaunchedEffect(streamUrl, subtitleUrl) {
        if (streamUrl.isNotEmpty()) {
            val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("MovieBoxPro/16.2.1 (Android 14; com.community.mbox.in)")
                .setAllowCrossProtocolRedirects(true)
            
            if (selectedCookie.isNotEmpty()) {
                dataSourceFactory.setDefaultRequestProperties(mapOf("Cookie" to selectedCookie))
            }

            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(streamUrl))
            if (!subtitleUrl.isNullOrEmpty()) {
                val mimeType = if (subtitleUrl.endsWith(".vtt", ignoreCase = true)) {
                    MimeTypes.TEXT_VTT
                } else {
                    MimeTypes.APPLICATION_SUBRIP
                }
                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(mimeType)
                    .setLanguage(viewModel.selectedSubtitle?.language ?: "en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
            }

            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                dataSourceFactory,
                androidx.media3.extractor.DefaultExtractorsFactory()
            )
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItemBuilder.build())

            val currentEpisode = viewModel.currentEpisode
            val episodes = viewModel.episodesList
            if (subject.isTvShow && episodes.isNotEmpty() && currentEpisode != null) {
                val currentIndex = episodes.indexOfFirst { it.episodeId == currentEpisode.episodeId }
                if (currentIndex != -1) {
                    val mediaSources = episodes.map { ep ->
                        if (ep.episodeId == currentEpisode.episodeId) {
                            mediaSource
                        } else {
                            mediaSourceFactory.createMediaSource(
                                MediaItem.Builder()
                                    .setMediaId(ep.episodeId)
                                    .setUri(Uri.parse("http://dummy.mp4"))
                                    .build()
                            )
                        }
                    }
                    exoPlayer.setMediaSources(mediaSources, currentIndex, viewModel.playbackProgressMs)
                } else {
                    exoPlayer.setMediaSource(mediaSource)
                }
            } else {
                exoPlayer.setMediaSource(mediaSource)
            }
            exoPlayer.prepare()
            if (viewModel.playbackProgressMs > 0) {
                exoPlayer.seekTo(viewModel.playbackProgressMs)
            }
            if (viewModel.isPlaying) {
                exoPlayer.play()
            }
        }
    }

    // Play/Pause sync
    LaunchedEffect(viewModel.isPlaying) {
        if (viewModel.isPlaying != exoPlayer.playWhenReady) {
            exoPlayer.playWhenReady = viewModel.isPlaying
        }
    }

    // Buffering listener
    var isBuffering by remember { mutableStateOf(false) }
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == androidx.media3.common.Player.STATE_BUFFERING)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Auto-hide controller when playing
    LaunchedEffect(isControllerVisible, viewModel.isPlaying) {
        if (isControllerVisible && viewModel.isPlaying) {
            delay(4000)
            isControllerVisible = false
        }
    }

    // Speed sync
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    LaunchedEffect(selectedSpeed) {
        exoPlayer.setPlaybackSpeed(selectedSpeed)
    }

    // Dynamic resolution constraint selector
    LaunchedEffect(selectedStream) {
        val q = selectedStream?.quality ?: "Auto"
        val (maxWidth, maxHeight) = when {
            q.contains("1080") -> Pair(1920, 1080)
            q.contains("720") -> Pair(1280, 720)
            q.contains("480") -> Pair(854, 480)
            q.contains("360") -> Pair(640, 360)
            else -> Pair(Integer.MAX_VALUE, Integer.MAX_VALUE)
        }
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxVideoSize(maxWidth, maxHeight)
                .build()
        } catch (e: Exception) {
            // Log fallback or safety
        }
    }

    // Listen to player progress ticking
    LaunchedEffect(viewModel.isPlaying) {
        if (viewModel.isPlaying) {
            while (true) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                if (dur > 0) {
                    currentProgressMs = pos
                    durationMs = dur
                    viewModel.updatePlaybackProgress(pos, dur)
                }
                delay(1000)
            }
        }
    }

    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            exoPlayer.release()
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val activity = context as? android.app.Activity
    val isInPip = activity?.isInPictureInPictureMode ?: false

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
        ) {
            if (!isFullscreen && !isInPip) {
                Text(
                    text = subject.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            // Player Container (Always present in tree at index 0)
        Box(
            modifier = if (isFullscreen || isInPip) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(playerHeight)
            }
        ) {
            if (streamUrl.isNotEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        val view = android.view.LayoutInflater.from(ctx).inflate(com.example.R.layout.texture_player_view, null) as androidx.media3.ui.PlayerView
                        view.isHapticFeedbackEnabled = false
                        view.isSoundEffectsEnabled = false
                        view.layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        view.apply {
                            subtitleView?.setViewType(androidx.media3.ui.SubtitleView.VIEW_TYPE_CANVAS)
                            val cleanStyle = androidx.media3.ui.CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                null
                            )
                            subtitleView?.setStyle(cleanStyle)
                        }
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Click gesture overlay catcher when controllers are hidden
                if (!isControllerVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { isControllerVisible = true }
                    )
                }

                // Custom Compose Playback Controller Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isControllerVisible && !isInPip,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { isControllerVisible = false }
                    ) {
                        // Top Bar: Back button on the left, Settings gear on the right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                                    )
                                )
                                .statusBarsPadding()
                                .clickable(enabled = false) {} // block click-through
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                viewModel.playInfo = null
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    activity.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                                }
                            }) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Close Player",
                                    tint = Color.White
                                )
                            }
                            
                            // Top Right Icons: Settings Gear
                            IconButton(onClick = {
                                activeSheet = "player_settings"
                            }) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Player Settings",
                                    tint = Color.White
                                )
                            }
                        }

                        // Center Buffering Spinner (if loading)
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = CinematicRed,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp)
                            )
                        }

                        // Bottom Controls Row: Play/Pause, Seekbar, Duration, PIP, Fullscreen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    )
                                )
                                .navigationBarsPadding()
                                .clickable(enabled = false) {} // block click-through
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Pause Button
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                        viewModel.isPlaying = false
                                    } else {
                                        exoPlayer.play()
                                        viewModel.isPlaying = true
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Progress Slider
                            Slider(
                                value = currentProgressMs.toFloat(),
                                onValueChange = { currentProgressMs = it.toLong() },
                                onValueChangeFinished = { exoPlayer.seekTo(currentProgressMs) },
                                valueRange = 0f..maxOf(1f, durationMs.toFloat()),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            // Position / Duration text (e.g. 00:03/02:05)
                            Text(
                                text = "${formatTime(currentProgressMs)}/${formatTime(durationMs)}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // PIP Button
                            IconButton(
                                onClick = {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            activity.enterPictureInPictureMode(
                                                android.app.PictureInPictureParams.Builder().build()
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.PictureInPicture,
                                    contentDescription = "PiP Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Fullscreen Toggle Button
                            IconButton(
                                onClick = {
                                    val activity = context as? android.app.Activity
                                    if (activity != null) {
                                        if (!isFullscreen) {
                                            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            activity.window.decorView.systemUiVisibility = (
                                                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                            )
                                        } else {
                                            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            activity.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                    }
                }
            }
        }

                // Transparent click catcher overlay on player view when bottom sheet is active
                if (activeSheet != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { activeSheet = null }
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Filled.VideoSettings,
                        contentDescription = "No Stream",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Live Stream Available",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "This content has no streams for the selected language dub. Please try another language/audio.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Portrait Details panel scroll container at bottom (only in portrait mode)
        if (!isFullscreen && !isInPip) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                    // Player Controller Panels (Subtitles, Dubs & Resolutions)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianDark)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (activeSheet != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Header row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (activeSheet) {
                                        "player_settings" -> "Playback Settings"
                                        "language" -> "Select language"
                                        "season" -> "${viewModel.seasonsList.size} seasons"
                                        "subtitle" -> "Select subtitle"
                                        "quality" -> "Select quality"
                                        "speed" -> "Playback speed"
                                        "episodes" -> "All episodes"
                                        else -> ""
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE2E8F0)
                                )
                                IconButton(onClick = { activeSheet = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                                }
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.Gray.copy(alpha = 0.2f)
                            )
                            
                            if (activeSheet == "player_settings") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // 1. Audio Dub Language
                                    val currentDub = viewModel.selectedResource?.name ?: "Original"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "language" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Audio Dub Language", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(currentDub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 2. Subtitles
                                    val currentSub = viewModel.selectedSubtitle?.language ?: "Off"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "subtitle" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Subtitles", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(currentSub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 3. Playback Speed
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "speed" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Playback Speed", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text("${selectedSpeed}x", color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 4. Video Quality
                                    val currentQuality = viewModel.selectedStream?.quality ?: "Auto"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "quality" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Video Quality", color = Color.White, fontSize = 14.sp)
                                        }
                                        Text(currentQuality, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // 5. Episodes List
                                    if (subject.isTvShow) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { activeSheet = "episodes" }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text("Episodes List", color = Color.White, fontSize = 14.sp)
                                            }
                                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            } else {
                                val itemsList = when (activeSheet) {
                                    "language" -> subject.resourceDetectors
                                    "season" -> viewModel.seasonsList
                                    "subtitle" -> listOf("None") + playInfo.subTitleList.map { it.language }
                                    "quality" -> playInfo.streamList
                                    "speed" -> listOf("0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")
                                    "episodes" -> viewModel.episodesList
                                    else -> emptyList()
                                }
                                
                                val cols = if (activeSheet == "episodes") 4 else 2
                                val chunked = itemsList.chunked(cols)
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    chunked.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowItems.forEach { item ->
                                                val isSelected = when (activeSheet) {
                                                    "language" -> viewModel.selectedResource == item
                                                    "season" -> viewModel.selectedSeason == item
                                                    "subtitle" -> if (item == "None") viewModel.selectedSubtitle == null else viewModel.selectedSubtitle?.language == item
                                                    "quality" -> viewModel.selectedStream == item
                                                    "speed" -> {
                                                        val currentSpeed = exoPlayer.playbackParameters.speed
                                                        val itemSpeed = when (item as String) {
                                                            "0.5x" -> 0.5f
                                                            "0.75x" -> 0.75f
                                                            "1.0x (Normal)" -> 1.0f
                                                            "1.25x" -> 1.25f
                                                            "1.5x" -> 1.5f
                                                            "2.0x" -> 2.0f
                                                            else -> 1.0f
                                                        }
                                                        Math.abs(currentSpeed - itemSpeed) < 0.05f
                                                    }
                                                    "episodes" -> viewModel.currentEpisode?.episodeId == (item as com.example.data.model.Episode).episodeId
                                                    else -> false
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        when (activeSheet) {
                                                            "language" -> viewModel.selectLanguageResource(item as com.example.data.model.ResourceDetector)
                                                            "season" -> viewModel.loadEpisodesForSeason(item as Int)
                                                            "subtitle" -> {
                                                                if (item == "None") {
                                                                    viewModel.selectedSubtitle = null
                                                                } else {
                                                                    val found = playInfo.subTitleList.find { it.language == item }
                                                                    if (found != null) viewModel.selectedSubtitle = found
                                                                }
                                                            }
                                                            "quality" -> {
                                                                viewModel.selectedStream = item as com.example.data.model.StreamItem
                                                            }
                                                            "speed" -> {
                                                                val speedVal = when (item as String) {
                                                                    "0.5x" -> 0.5f
                                                                    "0.75x" -> 0.75f
                                                                    "1.0x (Normal)" -> 1.0f
                                                                    "1.25x" -> 1.25f
                                                                    "1.5x" -> 1.5f
                                                                    "2.0x" -> 2.0f
                                                                    else -> 1.0f
                                                                }
                                                                exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(speedVal)
                                                            }
                                                            "episodes" -> {
                                                                viewModel.selectEpisode(item as com.example.data.model.Episode)
                                                            }
                                                        }
                                                        activeSheet = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) Color(0xFF115E59) else Color(0xFF2D2D30)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                ) {
                                                    Text(
                                                        text = when (activeSheet) {
                                                            "language" -> (item as com.example.data.model.ResourceDetector).name
                                                            "season" -> "Season ${(item as Int).toString().padStart(2, '0')}"
                                                            "subtitle" -> item as String
                                                            "quality" -> (item as com.example.data.model.StreamItem).quality
                                                            "speed" -> item as String
                                                            "episodes" -> (item as com.example.data.model.Episode).episodeNumber.toString().padStart(2, '0')
                                                            else -> ""
                                                        },
                                                        color = if (isSelected) Color(0xFF2DD4BF) else Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            if (rowItems.size < cols) {
                                                Spacer(modifier = Modifier.weight((cols - rowItems.size).toFloat()))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Resource and Season selectors section
                        Text(
                            text = "Resource / Season",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            // Resource trigger button
                            if (subject.resourceDetectors.isNotEmpty()) {
                                Button(
                                    onClick = { activeSheet = "language" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D30)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    val currentDub = viewModel.selectedResource?.name ?: "Original Audio"
                                    Text(
                                        text = "$currentDub ▾",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Season trigger button (only for TV Shows)
                            if (subject.isTvShow && viewModel.seasonsList.isNotEmpty()) {
                                Button(
                                    onClick = { activeSheet = "season" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D30)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Season ${viewModel.selectedSeason.toString().padStart(2, '0')} ▾",
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Episodes Grid list
                        if (subject.isTvShow) {
                            val chunkedEp = viewModel.episodesList.chunked(4)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                chunkedEp.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { ep ->
                                            val isCurrentEp = viewModel.currentEpisode?.episodeId == ep.episodeId
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .clickable { viewModel.selectEpisode(ep) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isCurrentEp) Color(0xFF115E59) else Color(0xFF2E2E30)
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ep.episodeNumber.toString().padStart(2, '0'),
                                                        color = if (isCurrentEp) Color(0xFF2DD4BF) else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                        // Handle odd items padding
                                        if (rowItems.size < 4) {
                                            Spacer(modifier = Modifier.weight((4 - rowItems.size).toFloat()))
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

        } // Close screen-wrapping Column

        // Custom Slide-Up 2-Column Grid Bottom Sheet Overlay (Root Level)
        if (activeSheet != null && isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { activeSheet = null }
            ) {
                val isPlayerSheet = isFullscreen && (activeSheet == "subtitle" || activeSheet == "quality" || activeSheet == "speed" || activeSheet == "language" || activeSheet == "episodes" || activeSheet == "season" || activeSheet == "player_settings")
                Card(
                    modifier = Modifier
                        .align(if (isPlayerSheet) Alignment.Center else Alignment.BottomCenter)
                        .padding(if (isPlayerSheet) 24.dp else 0.dp)
                        .fillMaxWidth(if (isPlayerSheet) 0.6f else 1f)
                        .clickable(enabled = false) {}, // prevent click-through
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E20)),
                    shape = if (isPlayerSheet) RoundedCornerShape(16.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeSheet) {
                                    "player_settings" -> "Playback Settings"
                                    "language" -> "Select language"
                                    "season" -> "${viewModel.seasonsList.size} seasons"
                                    "subtitle" -> "Select subtitle"
                                    "quality" -> "Select quality"
                                    "speed" -> "Playback speed"
                                    "episodes" -> "All episodes"
                                    else -> ""
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                            IconButton(onClick = { activeSheet = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                            }
                        }
                        
                        if (activeSheet == "player_settings") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. Audio Dub Language
                                val currentDub = viewModel.selectedResource?.name ?: "Original"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "language" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Audio Dub Language", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(currentDub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 2. Subtitles
                                val currentSub = viewModel.selectedSubtitle?.language ?: "Off"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "subtitle" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Subtitles", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(currentSub, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 3. Playback Speed
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "speed" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Playback Speed", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text("${selectedSpeed}x", color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 4. Quality Resolution
                                val currentQuality = viewModel.selectedStream?.quality ?: "Auto"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeSheet = "quality" }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Video Quality", color = Color.White, fontSize = 14.sp)
                                    }
                                    Text(currentQuality, color = CinematicRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                // 5. Episodes List (if TV Show)
                                if (subject.isTvShow) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeSheet = "episodes" }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text("Episodes List", color = Color.White, fontSize = 14.sp)
                                        }
                                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        } else {
                            // Scrollable Option Grid list (2-column or 4-column depending on sheet type)
                            val itemsList = when (activeSheet) {
                                        "language" -> subject.resourceDetectors
                                        "season" -> viewModel.seasonsList
                                        "subtitle" -> listOf("None") + playInfo.subTitleList.map { it.language }
                                        "quality" -> playInfo.streamList
                                        "speed" -> listOf("0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")
                                        "episodes" -> viewModel.episodesList
                                        else -> emptyList()
                                    }
                            
                            val cols = if (activeSheet == "episodes") 4 else 2
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val chunked = itemsList.chunked(cols)
                            items(chunked.size) { index ->
                                val rowItems = chunked[index]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        val isSelected = when (activeSheet) {
                                            "language" -> viewModel.selectedResource == item
                                            "season" -> viewModel.selectedSeason == item
                                            "subtitle" -> if (item == "None") viewModel.selectedSubtitle == null else viewModel.selectedSubtitle?.language == item
                                            "quality" -> viewModel.selectedStream == item
                                            "speed" -> {
                                                val currentSpeed = exoPlayer.playbackParameters.speed
                                                val itemSpeed = when (item as String) {
                                                    "0.5x" -> 0.5f
                                                    "0.75x" -> 0.75f
                                                    "1.0x (Normal)" -> 1.0f
                                                    "1.25x" -> 1.25f
                                                    "1.5x" -> 1.5f
                                                    "2.0x" -> 2.0f
                                                    else -> 1.0f
                                                }
                                                Math.abs(currentSpeed - itemSpeed) < 0.05f
                                            }
                                            "episodes" -> viewModel.currentEpisode?.episodeId == (item as com.example.data.model.Episode).episodeId
                                            else -> false
                                        }
                                        
                                        Button(
                                            onClick = {
                                                when (activeSheet) {
                                                    "language" -> viewModel.selectLanguageResource(item as com.example.data.model.ResourceDetector)
                                                    "season" -> viewModel.loadEpisodesForSeason(item as Int)
                                                    "subtitle" -> {
                                                        if (item == "None") {
                                                            viewModel.selectedSubtitle = null
                                                        } else {
                                                            val found = playInfo.subTitleList.find { it.language == item }
                                                            if (found != null) viewModel.selectedSubtitle = found
                                                        }
                                                    }
                                                    "quality" -> {
                                                        viewModel.selectedStream = item as com.example.data.model.StreamItem
                                                    }
                                                    "speed" -> {
                                                        val speedVal = when (item as String) {
                                                            "0.5x" -> 0.5f
                                                            "0.75x" -> 0.75f
                                                            "1.0x (Normal)" -> 1.0f
                                                            "1.25x" -> 1.25f
                                                            "1.5x" -> 1.5f
                                                            "2.0x" -> 2.0f
                                                            else -> 1.0f
                                                        }
                                                        exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(speedVal)
                                                    }
                                                    "episodes" -> {
                                                        viewModel.selectEpisode(item as com.example.data.model.Episode)
                                                    }
                                                }
                                                activeSheet = null
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) Color(0xFF115E59) else Color(0xFF2D2D30)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(
                                                text = when (activeSheet) {
                                                    "language" -> (item as com.example.data.model.ResourceDetector).name
                                                    "season" -> "Season ${(item as Int).toString().padStart(2, '0')}"
                                                    "subtitle" -> item as String
                                                    "quality" -> (item as com.example.data.model.StreamItem).quality
                                                    "speed" -> item as String
                                                    "episodes" -> (item as com.example.data.model.Episode).episodeNumber.toString().padStart(2, '0')
                                                    else -> ""
                                                },
                                                color = if (isSelected) Color(0xFF2DD4BF) else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    // Handle odd elements padding
                                    if (rowItems.size < cols) {
                                        Spacer(modifier = Modifier.weight((cols - rowItems.size).toFloat()))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// Human readable time formatter helper (mm:ss)
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(viewModel: MovieViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(viewModel.isLoggedIn) {
        if (viewModel.isLoggedIn) {
            viewModel.checkUserSession()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
    ) {
        if (viewModel.isLoggedIn) {
            val user = viewModel.currentUserProfile
            var showEditNicknameDialog by remember { mutableStateOf(false) }
            var editNickname by remember { mutableStateOf(user?.nickname ?: "") }

            val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: android.net.Uri? ->
                if (uri != null) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        if (bytes != null) {
                            viewModel.uploadAvatarAndModifyProfile(
                                nickname = user?.nickname ?: "",
                                imageBytes = bytes,
                                onSuccess = {
                                    Toast.makeText(context, "Avatar updated", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (showEditNicknameDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showEditNicknameDialog = false },
                    title = { Text("Edit Nickname", color = TextPrimary) },
                    text = {
                        androidx.compose.material3.OutlinedTextField(
                            value = editNickname,
                            onValueChange = { editNickname = it },
                            label = { Text("Nickname") },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    nickname = editNickname,
                                    avatar = user?.avatar ?: "",
                                    onSuccess = {
                                        Toast.makeText(context, "Nickname updated", Toast.LENGTH_SHORT).show()
                                        showEditNicknameDialog = false
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CinematicRed)
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditNicknameDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = ObsidianCard
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(ObsidianCard, shape = CircleShape)
                        .clickable {
                            photoPickerLauncher.launch("image/*")
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.avatar?.isNotEmpty() == true) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "User Avatar",
                            tint = CinematicRed,
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Profile card details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (user?.nickname?.isNotEmpty() == true) user.nickname else "Official User",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    editNickname = user?.nickname ?: ""
                                    showEditNicknameDialog = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = CinematicRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = if (user?.username?.isNotEmpty() == true) user.username else if (user?.email?.isNotEmpty() == true) user.email else "authenticated_session@moviebox.api",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Account Status", color = TextSecondary, fontSize = 14.sp)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CinematicRed.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (user?.vip == 1) "VIP Premium" else "Active Member",
                                    color = CinematicRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (user?.userId?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("User ID", color = TextSecondary, fontSize = 14.sp)
                                Text(user.userId, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (user?.vip == 1 && user.vipExpire.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("VIP Expiry", color = TextSecondary, fontSize = 14.sp)
                                Text(user.vipExpire, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stats = listOf(
                        Triple("Watched", user?.haveSeenCount ?: 0, Icons.Filled.History),
                        Triple("Watchlist", user?.wantToSeeCount ?: 0, Icons.Filled.Bookmark)
                    )

                    stats.forEach { (label, count, icon) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = CinematicRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = count.toString(),
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = label,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = {
                        viewModel.logoutUser {
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .testTag("logout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Log Out")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        } else {
            // Sign In / Register Card Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Brand Header
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "MovieBox Auth",
                    tint = CinematicRed,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRegisterMode) "Create Account" else "Welcome Back",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = if (isRegisterMode) "Sign up to sync your history & favorites" else "Sign in to access premium media content",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = CinematicRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        if (statusMessage.isNotEmpty()) {
                            Text(
                                text = statusMessage,
                                color = AccentAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Email Address Input
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinematicRed,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedLabelColor = CinematicRed,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Password Input
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinematicRed,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedLabelColor = CinematicRed,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // OTP Code Input (only in Register Mode)
                        if (isRegisterMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = otp,
                                    onValueChange = { otp = it },
                                    label = { Text("OTP Code") },
                                    leadingIcon = { Icon(Icons.Filled.Check, contentDescription = "OTP Code") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("otp_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CinematicRed,
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                        focusedLabelColor = CinematicRed,
                                        unfocusedLabelColor = TextSecondary,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        if (email.isEmpty()) {
                                            errorMessage = "Please enter your email to request OTP"
                                            return@Button
                                        }
                                        viewModel.requestOtp(email, isRegister = true, onSuccess = {
                                            errorMessage = ""
                                            statusMessage = "OTP sent to your email successfully!"
                                        }, onError = { err ->
                                            statusMessage = ""
                                            errorMessage = err
                                        })
                                    },
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("request_otp_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianDark),
                                    border = BorderStroke(1.dp, CinematicRed),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !viewModel.isAuthLoading
                                ) {
                                    Text("Get OTP", color = CinematicRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (email.isEmpty() || password.isEmpty()) {
                                    errorMessage = "Email and Password cannot be empty"
                                    return@Button
                                }
                                if (isRegisterMode) {
                                    if (otp.isEmpty()) {
                                        errorMessage = "Please enter the OTP verification code"
                                        return@Button
                                    }
                                    viewModel.registerAccount(email, password, otp, onSuccess = {
                                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    }, onError = { err ->
                                        errorMessage = err
                                    })
                                } else {
                                    viewModel.loginWithPassword(email, password, onSuccess = {
                                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                    }, onError = { err ->
                                        errorMessage = err
                                    })
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CinematicRed),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !viewModel.isAuthLoading
                        ) {
                            if (viewModel.isAuthLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (isRegisterMode) "Register & Sign In" else "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Toggle Sign In vs Register
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRegisterMode) "Already have an account?" else "Don't have an account?",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            errorMessage = ""
                            statusMessage = ""
                        },
                        modifier = Modifier.testTag("toggle_auth_mode")
                    ) {
                        Text(
                            text = if (isRegisterMode) "Sign In" else "Sign Up",
                            color = CinematicRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

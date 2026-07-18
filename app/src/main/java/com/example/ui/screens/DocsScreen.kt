package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ui.theme.*
import com.example.ui.viewmodel.MovieViewModel

data class DocSection(
    val subtitle: String,
    val explanation: String,
    val codeBlock: String? = null,
    val tag: String? = null
)

data class DocChapter(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val sections: List<DocSection>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(viewModel: MovieViewModel, onClose: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val chapters = remember { getMovieBoxDocs() }
    val filteredChapters = remember(searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            chapters
        } else {
            chapters.mapNotNull { chapter ->
                val matchingSections = chapter.sections.filter { section ->
                    section.subtitle.contains(searchQuery, ignoreCase = true) ||
                    section.explanation.contains(searchQuery, ignoreCase = true) ||
                    (section.codeBlock?.contains(searchQuery, ignoreCase = true) ?: false)
                }
                if (matchingSections.isNotEmpty() || chapter.title.contains(searchQuery, ignoreCase = true)) {
                    chapter.copy(
                        sections = matchingSections.ifEmpty { chapter.sections }
                    )
                } else {
                    null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MovieBox API Docs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Decompiled BFF Protocol & Handshake v16.2.1",
                            fontSize = 11.sp,
                            color = AccentAmber
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("docs_close_button")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close Docs", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = ObsidianDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianDark)
        ) {
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search parameters, endpoints, secrets...", color = TextSecondary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("docs_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CinematicRed,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianCard,
                    unfocusedContainerColor = ObsidianCard
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredChapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SearchOff, contentDescription = "No results", tint = TextSecondary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching documentation found", color = TextSecondary, fontSize = 15.sp)
                        Text("Try searching for 'signature', 'or666', or 'BFF'", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredChapters) { chapter ->
                        ChapterCard(
                            chapter = chapter,
                            onCopy = { text, label ->
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterCard(chapter: DocChapter, onCopy: (String, String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isExpanded) CinematicRed.copy(alpha = 0.5f) else Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CinematicRed.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = chapter.icon, contentDescription = chapter.title, tint = CinematicRed)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = chapter.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = chapter.description, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.rotate(rotationState)
                ) {
                    Icon(imageVector = Icons.Filled.ExpandMore, contentDescription = "Expand", tint = TextSecondary)
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    chapter.sections.forEach { section ->
                        SectionView(section = section, onCopy = onCopy)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionView(section: DocSection, onCopy: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = section.subtitle,
            color = AccentAmber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = section.explanation,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        if (section.codeBlock != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = section.tag ?: "CODEPAYLOAD",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CinematicRed
                        )
                        IconButton(
                            onClick = { onCopy(section.codeBlock, section.subtitle) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectionContainer {
                        Text(
                            text = section.codeBlock,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFA8FFB2), // Terminal green
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.Gray.copy(alpha = 0.1f))
    }
}

fun getMovieBoxDocs(): List<DocChapter> {
    return listOf(
        DocChapter(
            title = "1. Client Identity & Headers",
            icon = Icons.Filled.VpnKey,
            description = "BFF Cluster HTTP headers and user identification",
            sections = listOf(
                DocSection(
                    subtitle = "Required HTTP Headers",
                    explanation = "The API servers enforce header-based checking to reject unauthorized requests. Parity with official mobile application signatures is mandatory for 4K and native resolution playback.",
                    codeBlock = """User-Agent: MovieBoxPro/16.2.1 (Android 12; Pixel 6)
X-M-Version: 16.2.1
Accept: application/json
Content-Type: application/json;charset=UTF-8
Referer: https://api6.aoneroom.com/
X-Play-Mode: 2""",
                    tag = "HTTP_HEADERS"
                ),
                DocSection(
                    subtitle = "Cryptographic guest verification (X-Client-Token)",
                    explanation = "To interact with the APIs without logging in, the app generates a dynamic token inside Smali interceptor (com.transsion.baselib.net.f):\n1. Grab current Unix epoch in ms.\n2. Reverse the string.\n3. Compute MD5 digest of reversed ms.\n4. Combine: timestamp + \",\" + md5",
                    codeBlock = """# Python generation equivalent
import time, hashlib
ts = str(int(time.time() * 1000))
rev = ts[::-1]
h = hashlib.md5(rev.encode()).hexdigest()
token = ts + "," + h""",
                    tag = "SIGNATURE_GENERATOR"
                ),
                DocSection(
                    subtitle = "Device Metadata (X-Client-Info)",
                    explanation = "Serialized JSON containing regional, carrier, and hardware specifications used to geofence streams.",
                    codeBlock = """{
  "package_name": "com.community.oneroom",
  "version_name": "3.0.03.0529.03",
  "version_code": 50020042,
  "os": "android",
  "os_version": "12",
  "device_id": "4b689a7f30018b3d68e1a69a2d8a4f8c",
  "brand": "Redmi",
  "model": "2201117TG",
  "system_language": "en",
  "net": "NETWORK_WIFI",
  "region": "US",
  "sp_code": "40401",
  "X-Play-Mode": "2"
}""",
                    tag = "X-CLIENT-INFO-JSON"
                )
            )
        ),
        DocChapter(
            title = "2. Cryptographic App Signatures",
            icon = Icons.Filled.Security,
            description = "How payload and header HMAC signatures are resolved",
            sections = listOf(
                DocSection(
                    subtitle = "App Signature (x-tr-signature)",
                    explanation = "Validates the HTTP payload and URI query parameters against interception or replay attacks. Compiles request method, headers, timestamps, request body hashes, and target URI, then hashes via HMAC-MD5 with the Gateway Secret.",
                    codeBlock = """[METHOD]\n
[ACCEPT_HEADER]\n
[CONTENT_TYPE_HEADER]\n
[BODY_LENGTH]\n
[TIMESTAMP_MS]\n
[BODY_MD5_HASH]\n
[CANONICAL_PATH_AND_QUERY]""",
                    tag = "CANONICAL_STRING_FORMAT"
                ),
                DocSection(
                    subtitle = "Static Security Gateways",
                    explanation = "Extracted security secret keys compiled directly into Smali class mappings:",
                    codeBlock = """Gateway Secret Key (Sign Calculation):
76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O

Transsion WeFeed Secret:
df70dbad6215444ca9e87ee1078cc681

ByteDance Gecko/Pangle Access Key:
f36c832c8dbb162c49b46a7a6dd47fbd""",
                    tag = "STATIC_SECRETS"
                )
            )
        ),
        DocChapter(
            title = "3. Core BFF Endpoints Map",
            icon = Icons.Filled.ListAlt,
            description = "Key Backend-For-Frontend endpoints and parameters",
            sections = listOf(
                DocSection(
                    subtitle = "Content & Playback Resolver",
                    explanation = "Primary routes used to fetch details, search, and resolve stream URLs with CloudFront cookies.",
                    codeBlock = """// Retrieve Detailed Metadata
GET /wefeed-mobile-bff/subject-api/get?subjectId={id}

// Search Video Library
GET /wefeed-mobile-bff/subject-api/search?q={query}&page={page}&pageSize=20

// Play Stream Resolver
GET /wefeed-mobile-bff/subject-api/play-info?subjectId={id}&se={season}&ep={episode}""",
                    tag = "API_ROUTES"
                ),
                DocSection(
                    subtitle = "User History & Syncing Progress",
                    explanation = "Endpoints backing 'Continue Watching' watchlist, and synchronization across active sessions.",
                    codeBlock = """// Fetch History & Watchlist (seeType 2: Watched, 1: Watchlist)
GET /wefeed-mobile-bff/subject-api/see-list-v2?seeType=2&page=1&pageSize=20

// Report Playback progress to cloud (Save Progress)
POST /wefeed-mobile-bff/subject-api/have-seen
Payload:
{"list": [{"subjectId": 12345, "seeTime": 140000, "totalTime": 7200000, "status": 1}]}""",
                    tag = "API_USER_SYNC"
                )
            )
        ),
        DocChapter(
            title = "4. Bypass & Lab Easter Egg",
            icon = Icons.Filled.Science,
            description = "Unlocking developer laboratory and sandbox settings",
            sections = listOf(
                DocSection(
                    subtitle = "The 'or666' Easter Egg Gateway",
                    explanation = "The official application restricts usage outside regional boundaries and inside virtualized environments. To bypass this, the Easter Egg can be triggered:\n1. Tap the 'Not Available' text 10 times.\n2. In the Gateway Dialog, type bypass key 'or666'.\n3. The app cryptographically verifies input using salt '-321' and target MD5 hash '031A68C3912D796E235A72EE0BF89C16'.",
                    codeBlock = """Input: "or666"
Salt: "-321"
Concatenated: "or666-321"
MD5 Hash: 031a68c3912d796e235a72ee0bf89c16 (Match Success)""",
                    tag = "CRYPTO_VERIFICATION"
                ),
                DocSection(
                    subtitle = "Overriding Local Sandbox Variables (MMKV)",
                    explanation = "When bypassed successfully, the app overrides carrier details to spoof a US physical carrier via key-value stores:",
                    codeBlock = """// Spoofs a United States AT&T / T-Mobile carrier
sp_code = "90101"
custom_local_iso = "us"
custom_local_country = "United States"
custom_country_code = "1"
is_developer = true""",
                    tag = "MMKV_OVERRIDE_SCHEME"
                )
            )
        ),
        DocChapter(
            title = "5. Secondary Hybrid Systems",
            icon = Icons.Filled.Extension,
            description = "UGC Videos, Live watch, WebView Bridges and Games",
            sections = listOf(
                DocSection(
                    subtitle = "Live Watch Together & Chat Rooms",
                    explanation = "The Dynamic Room recommendation feed and AliCloud OSS live stream structures synchronizing video playback through WebSocket triggers.",
                    codeBlock = """WebSocket Server Endpoint:
wss://chat-api.aoneroom.com/chat-api/v1/room/sync

Join Room Call:
POST /wefeed-mobile-bff/room-api/join
Payload: {"roomId": "ROOM_ID", "chatToken": "JWT"}""",
                    tag = "WEBSOCKET_PLAYBACK"
                ),
                DocSection(
                    subtitle = "WebView Sports Bridge",
                    explanation = "Live sports (cricket, soccer) aggregated through third-party webviews require injection of a custom JS bridge 'moviebox_bridge.js' allowing browser threads to trigger native android ExoPlayer segments.",
                    codeBlock = """// Deep-Link WebView launch
oneroom://webview?url=https://sportslivetoday.com/live/detail?id=9902

// JavaScript Native Interface
window.MovieBoxBridge.openNativePlayer(streamUrl);""",
                    tag = "JAVASCRIPT_BRIDGE"
                ),
                DocSection(
                    subtitle = "UGC Short Videos upload (AliCloud STS Token)",
                    explanation = "Resolving upload capabilities allows short content upload to Alibaba Cloud. The app fetches temporary STS credentials to upload mp4 media fragments directly to the target buckets.",
                    codeBlock = """POST /wefeed-mobile-bff/upload/sts-token/v2
Returns: AccessKeyId, SecretAccessKey, SessionToken, Bucket, Endpoint""",
                    tag = "UGC_OSS_UPLOAD"
                )
            )
        )
    )
}

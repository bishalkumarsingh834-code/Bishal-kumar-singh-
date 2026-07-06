package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MessageEntity
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekBubbleAI
import com.example.ui.theme.SleekBubbleUser
import com.example.ui.theme.SleekGreen
import com.example.ui.theme.SleekOnPrimary
import com.example.ui.theme.SleekOnSurface
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekTextSecondary
import com.example.ui.theme.SleekTimestampBg
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SleekBackground
                ) { innerPadding ->
                    ChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val apiKeyWarning by viewModel.apiKeyWarning.collectAsState()

    var inputText by remember { mutableStateFlowOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Auto-scroll to the bottom when messages change
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Default suggested prompts (Hinglish/Hindi)
    val prefilledSuggestions = listOf(
        "भारत का सबसे ऊँचा पर्वत कौन सा है?",
        "AI कैसे काम करता है, संक्षेप में बताएं?",
        "एक मज़ेदार चुटकुला सुनाओ 😃",
        "सिक्किम की राजधानी क्या है?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBackground)
    ) {
        // --- Material 3 Top App Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(SleekBackground)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    Toast.makeText(context, "AI सहायक हमेशा आपके साथ है!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "पीछे जाएं",
                    tint = SleekOnSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "AI सहायक",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = SleekOnSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SleekGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ऑनलाइन",
                        fontSize = 12.sp,
                        color = SleekTextSecondary
                    )
                }
            }

            // Quick clear chat button
            IconButton(
                onClick = {
                    viewModel.clearChat()
                    Toast.makeText(context, "चैट इतिहास हटा दिया गया", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "चैट साफ करें",
                    tint = SleekOnSurface
                )
            }

            // Info / Help button
            IconButton(
                onClick = {
                    Toast.makeText(context, "यह ऐप Gemini 3.5 Flash API से संचालित है।", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "जानकारी",
                    tint = SleekOnSurface
                )
            }
        }

        // --- API Key Warning Banner ---
        AnimatedVisibility(
            visible = apiKeyWarning,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3CD),
                    contentColor = Color(0xFF856404)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(1.dp, Color(0xFFFFEEBA), RoundedCornerShape(12.dp))
                    .testTag("api_key_banner")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ API Key Configuration Needed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI से प्रतिक्रिया प्राप्त करने के लिए कृपया AI Studio के Secrets पैनल में 'GEMINI_API_KEY' कुंजी जोड़ें।",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // --- Chat Viewport ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // Empty state view / Welcome screen with suggestions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SleekPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "नमस्ते! मैं आपका AI सहायक हूँ।",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekOnSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "मैं विभिन्न विषयों पर आपके सवालों के जवाब दे सकता हूँ। बातचीत शुरू करने के लिए नीचे दिए गए सुझावों पर क्लिक करें या अपना खुद का सवाल लिखें।",
                        fontSize = 14.sp,
                        color = SleekTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "कुछ सुझाव प्रयास करें:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SleekTextSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        prefilledSuggestions.forEach { suggestion ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        inputText = ""
                                        viewModel.sendMessage(suggestion)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SleekBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 14.sp,
                                        color = SleekPrimary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "⚡",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Constant Date Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "आज",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SleekTextSecondary,
                                modifier = Modifier
                                    .background(SleekTimestampBg, RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        MessageRow(
                            message = message,
                            onSuggestionClick = { suggestion ->
                                viewModel.sendMessage(suggestion)
                            }
                        )
                    }

                    // Loading / generating indicator
                    if (isGenerating) {
                        item {
                            GeneratingIndicatorRow()
                        }
                    }
                }
            }
        }

        // --- Bottom Bar with Floating Input Design ---
        Surface(
            color = SleekBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SleekBorder, RoundedCornerShape(28.dp))
                        .background(SleekSurface, RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Plus Action Button
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "अधिक विकल्प जल्द ही आ रहे हैं!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "विकल्प",
                            tint = SleekTextSecondary
                        )
                    }

                    // Flexible Input Field using Custom Styled BasicTextField
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp, vertical = 10.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "अपना सवाल यहाँ लिखें...",
                                fontSize = 16.sp,
                                color = SleekTextSecondary
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = SleekOnSurface
                            ),
                            cursorBrush = SolidColor(SleekPrimary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() && !isGenerating) {
                                        viewModel.sendMessage(inputText.trim())
                                        inputText = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("msg_input")
                        )
                    }

                    // Send Button
                    val isSendEnabled = inputText.isNotBlank() && !isGenerating
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSendEnabled) SleekPrimary else SleekBorder)
                            .clickable(enabled = isSendEnabled) {
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                                focusManager.clearFocus()
                            }
                            .testTag("send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "भेजें",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Decorative home line matching the design spec
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SleekTextSecondary.copy(alpha = 0.2f))
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    message: MessageEntity,
    onSuggestionClick: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isUser = message.sender == "USER"

    val timeString = remember(message.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // AI Profile Picture Left
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SleekPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(weight = 1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Text Bubble
            val bubbleShape = if (isUser) {
                RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
            }

            val bubbleColor = if (isUser) {
                SleekBubbleUser
            } else if (message.isError) {
                Color(0xFFFDE8E8)
            } else {
                SleekBubbleAI
            }

            Surface(
                modifier = Modifier
                    .shadow(1.dp, bubbleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            clipboardManager.setText(AnnotatedString(message.text))
                            Toast.makeText(context, "संदेश कॉपी किया गया", Toast.LENGTH_SHORT).show()
                        }
                    ),
                color = bubbleColor,
                shape = bubbleShape
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (!isUser && message.text.contains("कंचनजंगा") || message.text.contains("Kangchenjunga")) {
                        Text(
                            text = "कंचनजंगा (Kangchenjunga)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = SleekOnSurface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = if (message.isError) Color(0xFFC81E1E) else SleekOnSurface,
                        lineHeight = 22.sp
                    )

                    // Add responsive suggestion chips inside the bubble for certain replies to match design HTML
                    if (!isUser && !message.isError && (message.text.contains("पर्वत") || message.text.contains("कंचनजंगा"))) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(text = "और जानें") {
                                onSuggestionClick("कंचनजंगा पर्वत के बारे में और रोचक तथ्य बताएं")
                            }
                            SuggestionChip(text = "सिक्किम यात्रा") {
                                onSuggestionClick("सिक्किम और कंचनजंगा कैसे जाएं?")
                            }
                        }
                    }

                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = SleekTextSecondary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(if (isUser) Alignment.End else Alignment.Start)
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Profile Picture Right
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SleekTextSecondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "यू",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(1.dp, SleekBorder, RoundedCornerShape(50))
            .background(Color.White, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = SleekOnSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GeneratingIndicatorRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SleekPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier.shadow(1.dp, RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)),
            color = SleekBubbleAI,
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI सहायक लिख रहा है",
                    fontSize = 14.sp,
                    color = SleekTextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                CircularProgressIndicator(
                    color = SleekPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// Helper function to allow state flow creation cleanly
fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)

package com.example.karhebti_android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OtpCodeInput(
    length: Int = 6,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusRequesters = remember { List(length) { FocusRequester() } }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(value) {
        if (value.length < length) {
            // Focus on the next empty box
            val nextIndex = value.length.coerceIn(0, length - 1)
            focusRequesters[nextIndex].requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Enter $length digit code" },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(length) { index ->
            val char = value.getOrNull(index)?.toString() ?: ""
            val isFocused = value.length == index

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .border(
                        width = 2.dp,
                        color = when {
                            isError -> MaterialTheme.colorScheme.error
                            isFocused -> MaterialTheme.colorScheme.primary
                            char.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = char,
                    onValueChange = { newChar ->
                        if (newChar.length <= 1 && newChar.all { it.isDigit() }) {
                            val newValue = buildString {
                                append(value.substring(0, index))
                                append(newChar)
                                if (index < value.length - 1) {
                                    append(value.substring(index + 1))
                                }
                            }
                            onValueChange(newValue.take(length))

                            // Auto-advance to next box
                            if (newChar.isNotEmpty() && index < length - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        } else if (newChar.isEmpty() && char.isNotEmpty()) {
                            // Handle deletion
                            val newValue = buildString {
                                append(value.substring(0, index))
                                if (index < value.length - 1) {
                                    append(value.substring(index + 1))
                                }
                            }
                            onValueChange(newValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequesters[index])
                        .onPreviewKeyEvent { keyEvent ->
                            // Handle backspace to go to previous box
                            if (keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Backspace &&
                                char.isEmpty() &&
                                index > 0
                            ) {
                                focusRequesters[index - 1].requestFocus()
                                // Delete the previous character
                                // Only delete if there's actually a character to delete
                                if (value.isNotEmpty() && index - 1 < value.length) {
                                    val newValue = value.substring(0, index - 1) +
                                        if (index < value.length) value.substring(index) else ""
                                    onValueChange(newValue)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            // Handle paste
                            if (keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.V &&
                                keyEvent.isCtrlPressed
                            ) {
                                val clipText = clipboardManager.getText()?.text
                                if (clipText != null && clipText.all { it.isDigit() }) {
                                    onValueChange(clipText.take(length))
                                }
                                true
                            } else {
                                false
                            }
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp
                    )
                )

                if (char.isEmpty() && !isFocused) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }

    // Request focus on first box when component is first displayed
    LaunchedEffect(Unit) {
        if (value.isEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }
}

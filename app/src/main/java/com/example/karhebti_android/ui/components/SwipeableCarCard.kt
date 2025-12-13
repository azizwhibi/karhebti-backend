package com.example.karhebti_android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.karhebti_android.data.api.MarketplaceCarResponse
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Helper function to build full image URL
private fun getFullImageUrl(imageUrl: String?): String? {
    if (imageUrl == null) return null
    val fullUrl = if (imageUrl.startsWith("http")) {
        imageUrl
    } else {
        "http://10.0.2.2:3000${if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"}"
    }
    android.util.Log.d("SwipeableCarCard", "Image URL: $imageUrl -> Full URL: $fullUrl")
    return fullUrl
}

@Composable
fun SwipeableCarCard(
    car: MarketplaceCarResponse,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }

    val swipeThreshold = 300f

    LaunchedEffect(Unit) {
        animatedOffsetX.snapTo(offsetX)
        animatedOffsetY.snapTo(offsetY)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .offset { IntOffset(animatedOffsetX.value.roundToInt(), animatedOffsetY.value.roundToInt()) }
            .rotate(animatedOffsetX.value / 20f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offsetX) > swipeThreshold) {
                                // Swipe completed
                                val targetX = if (offsetX > 0) 1000f else -1000f
                                animatedOffsetX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = tween(300)
                                )
                                if (offsetX > 0) {
                                    onSwipeRight()
                                } else {
                                    onSwipeLeft()
                                }
                            } else {
                                // Return to center
                                animatedOffsetX.animateTo(0f, animationSpec = tween(300))
                                animatedOffsetY.animateTo(0f, animationSpec = tween(300))
                            }
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        scope.launch {
                            animatedOffsetX.snapTo(offsetX)
                            animatedOffsetY.snapTo(offsetY)
                        }
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Car Image
                val imageUrl = remember(car.imageUrl) {
                    car.imageUrl?.let { url ->
                        "http://10.0.2.2:3000${if (url.startsWith("/")) url else "/$url"}"
                    }
                }
                if (imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = "${car.marque} ${car.modele}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Image load failed",
                                    modifier = Modifier.size(120.dp),
                                    tint = Color.Red.copy(alpha = 0.3f)
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.7f
                        }
                )

                // Car Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "${car.marque} ${car.modele}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Year: ${car.annee}",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    car.kilometrage?.let {
                        Text(
                            text = "Mileage: ${it} km",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = "Fuel: ${car.typeCarburant}",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    car.price?.let {
                        Text(
                            text = "Price: $${it}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    car.description?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2
                        )
                    }
                }

                // Swipe indicators
                if (offsetX > 50) {
                    // Right swipe indicator (Like)
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(32.dp)
                            .rotate(-20f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Green.copy(alpha = (offsetX / swipeThreshold).coerceIn(0f, 0.8f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Like",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("INTERESTED", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (offsetX < -50) {
                    // Left swipe indicator (Pass)
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(32.dp)
                            .rotate(20f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = (abs(offsetX) / swipeThreshold).coerceIn(0f, 0.8f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Pass",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PASS", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

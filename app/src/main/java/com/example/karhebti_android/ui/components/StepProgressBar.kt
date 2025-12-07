package com.example.karhebti_android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StepProgressBar(
    steps: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, label ->
            Step(
                modifier = Modifier.weight(1f),
                label = label,
                number = index + 1,
                isCompleted = index < currentStep,
                isActive = index == currentStep,
                isLastStep = index == steps.lastIndex
            )
        }
    }
}

@Composable
private fun Step(
    modifier: Modifier = Modifier,
    label: String,
    number: Int,
    isCompleted: Boolean,
    isActive: Boolean,
    isLastStep: Boolean
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    val circleColor by animateColorAsState(
        targetValue = if (isCompleted || isActive) activeColor else inactiveColor,
        animationSpec = tween(500)
    )
    val lineColor by animateColorAsState(
        targetValue = if (isCompleted) activeColor else inactiveColor,
        animationSpec = tween(500)
    )
    val textColor by animateColorAsState(
        targetValue = if (isCompleted || isActive) activeColor else inactiveColor,
        animationSpec = tween(500)
    )
    val circleSize by animateDpAsState(
        targetValue = if (isActive) 40.dp else 32.dp,
        animationSpec = tween(500)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ligne de connexion (sauf pour le premier élément)
            if (number > 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(lineColor)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Cercle
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = number.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Ligne de connexion (sauf pour le dernier élément)
            if (!isLastStep) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(lineColor)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            text = label,
            color = textColor,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}


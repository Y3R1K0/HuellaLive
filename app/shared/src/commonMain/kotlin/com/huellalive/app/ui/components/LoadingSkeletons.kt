package com.huellalive.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.huellalive.app.ui.theme.Background
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.Surface
import com.huellalive.app.ui.theme.SurfaceRaised

@Composable
fun Modifier.huellaSkeleton(shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val transition = rememberInfiniteTransition(label = "Huella skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Skeleton pulse"
    )
    return clip(shape).background(lerp(Surface, SurfaceRaised, progress))
}

@Composable
fun SearchShelterSkeletons() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .width(174.dp)
                    .height(116.dp)
                    .huellaSkeleton(RoundedCornerShape(18.dp))
            ) {
                Box(Modifier.fillMaxWidth().height(64.dp).huellaSkeleton(RoundedCornerShape(18.dp)))
                Spacer(Modifier.height(9.dp))
                Box(Modifier.padding(horizontal = 10.dp).width(104.dp).height(13.dp).huellaSkeleton())
                Spacer(Modifier.height(6.dp))
                Box(Modifier.padding(horizontal = 10.dp).width(70.dp).height(10.dp).huellaSkeleton())
            }
        }
    }
}

@Composable
fun AnimalGridSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(214.dp)
            .huellaSkeleton(RoundedCornerShape(18.dp))
    ) {
        Box(Modifier.fillMaxWidth().height(148.dp).huellaSkeleton(RoundedCornerShape(18.dp)))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.padding(horizontal = 10.dp).fillMaxWidth(0.62f).height(14.dp).huellaSkeleton())
        Spacer(Modifier.height(7.dp))
        Box(Modifier.padding(horizontal = 10.dp).fillMaxWidth(0.42f).height(10.dp).huellaSkeleton())
    }
}

@Composable
fun ExploreMapSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.huellaSkeleton(RoundedCornerShape(24.dp))
    ) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .align(
                        when (index) {
                            0 -> Alignment.TopStart
                            1 -> Alignment.TopEnd
                            2 -> Alignment.Center
                            3 -> Alignment.BottomStart
                            else -> Alignment.BottomEnd
                        }
                    )
                    .padding(30.dp + (index % 2 * 22).dp)
                    .size(if (index == 2) 52.dp else 38.dp)
                    .huellaSkeleton(CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(18.dp)
                .clip(CircleShape)
                .background(DustyRose.copy(alpha = 0.55f))
        )
    }
}

@Composable
fun ProfileLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(Background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .huellaSkeleton(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
        )
        Box(
            modifier = Modifier
                .padding(top = 18.dp)
                .width(180.dp)
                .height(26.dp)
                .huellaSkeleton()
        )
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(128.dp)
                .height(14.dp)
                .huellaSkeleton()
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f).height(54.dp).huellaSkeleton(RoundedCornerShape(16.dp)))
            Box(Modifier.weight(1f).height(54.dp).huellaSkeleton(RoundedCornerShape(16.dp)))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimalGridSkeleton(Modifier.weight(1f))
            AnimalGridSkeleton(Modifier.weight(1f))
        }
    }
}

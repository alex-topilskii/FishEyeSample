package com.ato.fisheyesample

import android.graphics.RenderEffect.createRuntimeShaderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ato.fisheyesample.ui.theme.FishEyeSampleTheme

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FishEyeSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FishEyeLayer(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ComplexScreen()
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FishEyeLayer(
    modifier: Modifier = Modifier,
    circleRadius: Float = 120.dp.value,
    content: @Composable () -> Unit
) {
    val contentKey = "content"
    val circleCenterKey = "circleCenter"
    val circleRadiusKey = "circleRadius"

    val shaderCode = remember {
        """
            uniform shader $contentKey;
            uniform float2 $circleCenterKey;
            uniform float  $circleRadiusKey;

            half4 main(float2 fragCoord) {
                float zoom = 1.7;
                float distortion = 0.8;
                float powVal = 20.0;

                float2 toCenter = fragCoord - $circleCenterKey;
                float dist = length(toCenter);

                float2 sampleCoord = fragCoord;

                if (dist <= circleRadius) {
                    float r = dist / circleRadius;
                    float factor = 1.0 + distortion * pow(r, powVal);
                    sampleCoord = $circleCenterKey + toCenter / (zoom * factor);
                }

                half4 color = $contentKey.eval(sampleCoord);
                return color;
            }
        """.trimIndent()
    }
    val runtimeShader = remember(shaderCode) { RuntimeShader(shaderCode) }
    var fishEyeEffect by remember { mutableStateOf<RenderEffect?>(null) }

    val density = LocalDensity.current
    var circleCenter by remember { mutableStateOf(android.graphics.PointF(400f, 400f)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapAndDrag(
                    onPress = { offset ->
                        circleCenter = android.graphics.PointF(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        circleCenter = android.graphics.PointF(change.position.x, change.position.y)
                    }
                )
            }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                renderEffect = fishEyeEffect
            }
    ) {
        LaunchedEffect(circleCenter, circleRadius) {
            runtimeShader.setFloatUniform(circleCenterKey, circleCenter.x, circleCenter.y)
            runtimeShader.setFloatUniform(
                circleRadiusKey,
                with(density) { circleRadius.dp.toPx() }
            )
            fishEyeEffect =
                createRuntimeShaderEffect(runtimeShader, contentKey).asComposeRenderEffect()
        }
        content()
    }
}

suspend fun PointerInputScope.detectTapAndDrag(
    onPress: (Offset) -> Unit,
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown()
            onPress(down.position)
            drag(down.id) { change ->
                onDrag(change, change.position)
                change.consume()
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun ComplexScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header Text
        Text(
            text = "Welcome to the Complex Screen!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 16.dp)
        )

        // Horizontal Scrollable Row of Images (e.g., Avatars)
        Text(text = "Featured Users", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(10) { index ->
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background), // Replace with your actual drawable
                    contentDescription = "User Avatar ${index + 1}",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Section with Cards
        Text(text = "Informational Cards", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        (1..3).forEach { cardNumber ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace
                        contentDescription = "Card Image $cardNumber",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Card Title $cardNumber",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This is some descriptive text for card number $cardNumber. It can be a bit longer to see how it wraps within the card.",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Long Text Section
        Text(text = "Detailed Information", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = """
                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. 
                Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. 
                Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. 
                Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                
                Phasellus egestas tellus rutrum tellus pellentesque eu. Mattis molestie a iaculis at erat pellentesque adipiscing commodo. 
                Nunc sed id semper risus in hendrerit gravida rutrum. Amet justo donec enim diam vulputate ut pharetra sit. 
                Velit euismod in pellentesque massa placerat duis ultricies lacus.
            """.trimIndent(),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Another Image
        Text(text = "Another Image Section", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background), // Replace
            contentDescription = "Bottom Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ComplexScreenPreview() {
    FishEyeSampleTheme {
        ComplexScreen()
    }
}
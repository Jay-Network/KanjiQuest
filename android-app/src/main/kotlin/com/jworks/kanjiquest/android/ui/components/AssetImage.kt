package com.jworks.kanjiquest.android.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

/**
 * Loads and displays a PNG image from the assets/images/ directory.
 *
 * @param filename The image filename (e.g. "mode-recognition.png")
 * @param contentDescription Accessibility description
 * @param modifier Modifier for the Image composable
 * @param contentScale How the image should be scaled (default: Fit)
 */
@Composable
fun AssetImage(
    filename: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val bitmap = remember(filename) {
        context.assets.open("images/$filename").use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
fun RadicalImage(
    radicalId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val bitmap: Bitmap? = remember(radicalId) {
        try {
            context.assets.open("radicals/radical_$radicalId.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = contentDescription ?: "?", fontSize = 40.sp)
        }
    }
}

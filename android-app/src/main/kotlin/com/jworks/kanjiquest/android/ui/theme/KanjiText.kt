package com.jworks.kanjiquest.android.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Text composable that forces Japanese locale for correct CJK glyph rendering.
 * Without this, Android may render kanji using Chinese font variants.
 */
@Composable
fun KanjiText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        style = style.copy(localeList = LocaleList("ja"))
    )
}

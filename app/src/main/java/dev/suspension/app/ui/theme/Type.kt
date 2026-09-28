package dev.suspension.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import dev.suspension.app.R

val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** Tabular (lining) numeral feature, used wherever numbers must align in a column. */
private val TabularNums = TextGeometricTransform()

/** Custom text-style set matching spec §4 "Typography" exactly — not Material3's default slots. */
data class AppTypography(
    val screenTitle: TextStyle = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
    ),
    val groupHeading: TextStyle = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    val rowLabel: TextStyle = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    val rowHint: TextStyle = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    val valueNumeral: TextStyle = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        fontFeatureSettings = "tnum",
    ),
    val valueUnit: TextStyle = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    val navLabel: TextStyle = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    val tableValue: TextStyle = TextStyle(
        fontFamily = Barlow,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        fontFeatureSettings = "tnum",
    ),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

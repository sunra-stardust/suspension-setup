package dev.suspension.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic design tokens (spec §4). These mirror the anodised colour of the physical adjuster
 * on the bike, so a row on screen maps to a dial in hand — never substitute with theme-derived
 * colours.
 */
data class AppColors(
    val bg: Color,
    val surface: Color,
    val line: Color,
    val ink: Color,
    val dim: Color,
    val hit: Color,
    val comp: Color,
    val reb: Color,
    val spring: Color,
    val neutral: Color,
)

val LightAppColors = AppColors(
    bg = Color(0xFFF2F2EF),
    surface = Color(0xFFFFFFFF),
    line = Color(0xFFD8D7D1),
    ink = Color(0xFF22252A),
    dim = Color(0xFF6E7278),
    hit = Color(0xFFE9E8E3),
    comp = Color(0xFF2F6C93),
    reb = Color(0xFFA8392C),
    spring = Color(0xFF9A7434),
    neutral = Color(0xFF7C8188),
)

val DarkAppColors = AppColors(
    bg = Color(0xFF16181C),
    surface = Color(0xFF1E2126),
    line = Color(0xFF31353B),
    ink = Color(0xFFE8E8E5),
    dim = Color(0xFF969BA1),
    hit = Color(0xFF262A30),
    comp = Color(0xFF5E9FC7),
    reb = Color(0xFFD2705F),
    spring = Color(0xFFC6A05A),
    neutral = Color(0xFF7C8188),
)

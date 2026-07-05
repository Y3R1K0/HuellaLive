package com.huellalive.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Legacy palette kept for reference:
 * Background #0D0D0D, Surface #161616, SurfaceRaised #1F1F1F,
 * SurfaceHigh #272727, BottomBar #111111.
 * DustyRose #E8A0A0, DustyRoseLight #EFB8B8, DustyRoseDark #D4889A.
 * LavenderSoft #C9A8E0, MintCream #A8D5B5, PeachWarm #F2C4A0,
 * SkyPowder #A8C8E8, LilacSoft #D4A8D5, ButterSoft #E8D8A0.
 * TextPrimary #F0EBF8, TextSecondary #B8B0CC, TextTertiary #7A7390.
 */

// HuellaLive core palette
val HuellaInk        = Color(0xFF101112)
val HuellaSurface    = Color(0xFF181A1B)
val HuellaRaised     = Color(0xFF222526)
val HuellaHigh       = Color(0xFF2B3031)
val HuellaBottom     = Color(0xFF121415)

val HuellaPink       = Color(0xFFF29CA3)
val HuellaPinkSoft   = Color(0xFFFFC2C7)
val HuellaPinkDark   = Color(0xFFD96F7B)
val HuellaPinkGlow   = Color(0x24F29CA3)

val AdoptionGreen    = Color(0xFF7CC9A7)
val AdoptionGreenSoft = Color(0xFFBFE8D4)
val CookieOrange     = Color(0xFFF4B37A)
val InfoBlue         = Color(0xFF82B7E8)
val CareLilac        = Color(0xFFD99AE8)
val BadgeCream       = Color(0xFFF2D89B)

val HuellaText       = Color(0xFFF7F2F0)
val HuellaTextSoft   = Color(0xFFC9C0BE)
val HuellaTextMuted  = Color(0xFF8F8583)
val HuellaOnAccent   = Color(0xFF151112)

// Existing app tokens mapped to the new palette.
val Background     = HuellaInk
val Surface        = HuellaSurface
val SurfaceRaised  = HuellaRaised
val SurfaceHigh    = HuellaHigh
val BottomBar      = HuellaBottom

val DustyRose      = HuellaPink
val DustyRoseLight = HuellaPinkSoft
val DustyRoseDark  = HuellaPinkDark
val DustyRoseGlow  = HuellaPinkGlow

val LavenderSoft   = CareLilac
val MintCream      = AdoptionGreen
val PeachWarm      = CookieOrange
val SkyPowder      = InfoBlue
val LilacSoft      = CareLilac
val ButterSoft     = BadgeCream

// Animal states
val StatusAvailable    = Color(0xFF63C990)
val StatusRecovering   = CookieOrange
val StatusPregnant     = CareLilac
val StatusAdopted      = InfoBlue
val StatusOther        = HuellaTextMuted

val StatusAvailableBg  = Color(0x2463C990)
val StatusRecoveringBg = Color(0x24F4B37A)
val StatusPregnantBg   = Color(0x24D99AE8)
val StatusAdoptedBg    = Color(0x2482B7E8)
val StatusOtherBg      = Color(0x248F8583)

// Badges and highlights
val BadgeBronze    = Color(0xFFD19A66)
val BadgeSilver    = Color(0xFFC8CFD6)
val BadgeGold      = BadgeCream
val BadgePlatinum  = CareLilac
val BadgeDiamond   = InfoBlue

// Text
val TextPrimary    = HuellaText
val TextSecondary  = HuellaTextSoft
val TextTertiary   = HuellaTextMuted
val TextOnAccent   = HuellaOnAccent

// Semantic colors
val Success        = StatusAvailable
val Error          = Color(0xFFFF7A8A)
val Warning        = CookieOrange

val White          = HuellaText
val Black          = HuellaInk
val Outline        = Color(0xFF353A3B)
val OutlineVariant = Color(0xFF25292A)

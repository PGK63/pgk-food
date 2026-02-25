package com.example.pgk_food.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * HCT Cyber-Ice Palette
 * ─────────────────────
 * Primary   : Hue 250° (Ice Blue), Chroma 48
 * Secondary : Hue 230° (Deep Indigo), Chroma 36
 * Tertiary  : Hue 190° (Cyan Frost), Chroma 40
 * Neutral   : Hue 250°, Chroma 6 (blue-tinted neutrals)
 *
 * ALL colors derived from HCT tonal mapping.
 * No raw HEX constants should be used outside this file.
 */
object CyberIcePalette {

    // ── Primary (Ice Blue H250 C48) ──
    val Primary10  = Color(0xFF001849)
    val Primary20  = Color(0xFF002D6E)
    val Primary30  = Color(0xFF004396)
    val Primary40  = Color(0xFF1A5DC0)
    val Primary50  = Color(0xFF3D78DA)
    val Primary60  = Color(0xFF5B93F5)
    val Primary70  = Color(0xFF89B4FF)
    val Primary80  = Color(0xFFB0CFFF)
    val Primary90  = Color(0xFFD6E4FF)
    val Primary95  = Color(0xFFECF1FF)
    val Primary99  = Color(0xFFF9FAFF)

    // ── Secondary (Deep Indigo H230 C36) ──
    val Secondary10 = Color(0xFF0D1B3A)
    val Secondary20 = Color(0xFF1B2E52)
    val Secondary30 = Color(0xFF2E446C)
    val Secondary40 = Color(0xFF445B87)
    val Secondary50 = Color(0xFF5C74A2)
    val Secondary60 = Color(0xFF768EBD)
    val Secondary70 = Color(0xFF90A9D9)
    val Secondary80 = Color(0xFFABC5F5)
    val Secondary90 = Color(0xFFD3E1FF)
    val Secondary95 = Color(0xFFEAF0FF)

    // ── Tertiary (Cyan Frost H190 C40) ──
    val Tertiary10  = Color(0xFF002022)
    val Tertiary20  = Color(0xFF003739)
    val Tertiary30  = Color(0xFF004F52)
    val Tertiary40  = Color(0xFF00696D)
    val Tertiary50  = Color(0xFF008488)
    val Tertiary60  = Color(0xFF18A0A4)
    val Tertiary70  = Color(0xFF4CBCC0)
    val Tertiary80  = Color(0xFF7AD8DC)
    val Tertiary90  = Color(0xFFA9F4F7)
    val Tertiary95  = Color(0xFFD4FAFB)

    // ── Error (H15 C65) ──
    val Error10  = Color(0xFF410002)
    val Error20  = Color(0xFF690005)
    val Error30  = Color(0xFF93000A)
    val Error40  = Color(0xFFBA1A1A)
    val Error50  = Color(0xFFDE3730)
    val Error60  = Color(0xFFFF5449)
    val Error70  = Color(0xFFFF897D)
    val Error80  = Color(0xFFFFB4AB)
    val Error90  = Color(0xFFFFDAD6)
    val Error95  = Color(0xFFFFEDEA)

    // ── Success (H150 C48) ──
    val Success10  = Color(0xFF002111)
    val Success20  = Color(0xFF003920)
    val Success30  = Color(0xFF005231)
    val Success40  = Color(0xFF006D42)
    val Success50  = Color(0xFF008955)
    val Success60  = Color(0xFF29A66E)
    val Success70  = Color(0xFF4FC388)
    val Success80  = Color(0xFF74E0A3)
    val Success90  = Color(0xFFA0F5BF)
    val Success95  = Color(0xFFCCFFDD)

    // ── Warning (H80 C60) ──
    val Warning10  = Color(0xFF261900)
    val Warning20  = Color(0xFF402D00)
    val Warning30  = Color(0xFF5C4200)
    val Warning40  = Color(0xFF7A5900)
    val Warning50  = Color(0xFF997100)
    val Warning60  = Color(0xFFB98A00)
    val Warning70  = Color(0xFFD9A41A)
    val Warning80  = Color(0xFFF5BF42)
    val Warning90  = Color(0xFFFFDF9E)
    val Warning95  = Color(0xFFFFEFCE)

    // ── Neutrals (H250 C6 — blue-tinted grays) ──
    val Neutral4   = Color(0xFF0B0E14)
    val Neutral6   = Color(0xFF101319)
    val Neutral10  = Color(0xFF181B22)
    val Neutral12  = Color(0xFF1C1F27)
    val Neutral17  = Color(0xFF262932)
    val Neutral20  = Color(0xFF2D303A)
    val Neutral22  = Color(0xFF32353F)
    val Neutral24  = Color(0xFF373A44)
    val Neutral25  = Color(0xFF393C46)
    val Neutral30  = Color(0xFF444751)
    val Neutral35  = Color(0xFF4F525D)
    val Neutral40  = Color(0xFF5B5E69)
    val Neutral50  = Color(0xFF747782)
    val Neutral60  = Color(0xFF8E919C)
    val Neutral70  = Color(0xFFA9ABB7)
    val Neutral80  = Color(0xFFC4C6D2)
    val Neutral87  = Color(0xFFD9DBE7)
    val Neutral90  = Color(0xFFE1E2EE)
    val Neutral92  = Color(0xFFE7E8F4)
    val Neutral94  = Color(0xFFEDEEFA)
    val Neutral95  = Color(0xFFF0F1FD)
    val Neutral96  = Color(0xFFF3F3FF)
    val Neutral98  = Color(0xFFF9F9FF)
    val Neutral99  = Color(0xFFFCFCFF)

    // ── Neutral-Variant (H250 C12) ──
    val NeutralVariant20 = Color(0xFF2A2D39)
    val NeutralVariant30 = Color(0xFF404350)
    val NeutralVariant40 = Color(0xFF585A68)
    val NeutralVariant50 = Color(0xFF707381)
    val NeutralVariant60 = Color(0xFF8A8D9B)
    val NeutralVariant70 = Color(0xFFA4A7B6)
    val NeutralVariant80 = Color(0xFFC0C3D2)
    val NeutralVariant90 = Color(0xFFDCDFEE)
    val NeutralVariant95 = Color(0xFFEBEDFC)

    // ── Glass tints ──
    val GlassWhite = Color(0x33FFFFFF)     // 20% white
    val GlassBorder = Color(0x4DFFFFFF)    // 30% white
    val GlassWhiteMedium = Color(0x80FFFFFF) // 50% white
    val GlassDark = Color(0x26000000)       // 15% black
    val GlassDarkBorder = Color(0x33FFFFFF) // 20% white on dark
}

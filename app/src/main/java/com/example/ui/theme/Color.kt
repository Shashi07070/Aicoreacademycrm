package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// AI CORE ACADEMY Brand Palette
// Bold Red Accent: #E11D48
val BrandRed = Color(0xFFE11D48)
val BrandRedHover = Color(0xFFBE123C)
val BrandRedLight = Color(0xFFFFF1F2)
val BrandRedBorder = Color(0xFFFECDD3)

// Light Minimalist Theme (#F9FAFB Background, Pure White Cards)
val RedPrimaryLight = Color(0xFFE11D48) // Bold Red Accent
val RedOnPrimaryLight = Color(0xFFFFFFFF)
val RedPrimaryContainerLight = Color(0xFFFFF1F2)
val RedOnPrimaryContainerLight = Color(0xFF9F1239)

val SlateSecondaryLight = Color(0xFF1F2937) // Deep Charcoal Slate
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateSecondaryContainerLight = Color(0xFFF3F4F6)
val SlateOnSecondaryContainerLight = Color(0xFF111827)

val SkyTertiaryLight = Color(0xFF0284C7)
val SkyOnTertiaryLight = Color(0xFFFFFFFF)
val SkyTertiaryContainerLight = Color(0xFFE0F2FE)
val SkyOnTertiaryContainerLight = Color(0xFF0369A1)

val BackgroundLight = Color(0xFFF9FAFB) // Light off-white / very pale gray
val SurfaceLight = Color(0xFFFFFFFF) // Pure white card
val SurfaceVariantLight = Color(0xFFF3F4F6) // Subtle soft gray
val OnSurfaceLight = Color(0xFF111827) // Crisp modern dark text
val OnSurfaceVariantLight = Color(0xFF4B5563) // Slate neutral
val OutlineLight = Color(0xFFE5E7EB) // Hairline border
val OutlineVariantLight = Color(0xFFF3F4F6)

// Dark Minimalist Theme
val RedPrimaryDark = Color(0xFFFB7185)
val RedOnPrimaryDark = Color(0xFF4C0519)
val RedPrimaryContainerDark = Color(0xFF881337)
val RedOnPrimaryContainerDark = Color(0xFFFFE4E6)

val SlateSecondaryDark = Color(0xFFE5E7EB)
val SlateOnSecondaryDark = Color(0xFF111827)
val SlateSecondaryContainerDark = Color(0xFF374151)
val SlateOnSecondaryContainerDark = Color(0xFFF9FAFB)

val SkyTertiaryDark = Color(0xFF38BDF8)
val SkyOnTertiaryDark = Color(0xFF082F49)
val SkyTertiaryContainerDark = Color(0xFF075985)
val SkyOnTertiaryContainerDark = Color(0xFFE0F2FE)

val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceVariantDark = Color(0xFF334155)
val OnSurfaceDark = Color(0xFFF9FAFB)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF475569)
val OutlineVariantDark = Color(0xFF334155)

// Semantic Fee & Attendance Status Colors
val StatusPaidGreen = Color(0xFF16A34A) // Crisp Emerald Green
val StatusPaidContainer = Color(0xFFDCFCE7)
val StatusPaidText = Color(0xFF15803D)

val StatusPendingRed = Color(0xFFE11D48) // Bold Red Accent
val StatusPendingContainer = Color(0xFFFFF1F2)
val StatusPendingText = Color(0xFFBE123C)

val StatusLateAmber = Color(0xFFD97706) // Late Attendance Amber
val StatusLateContainer = Color(0xFFFEF3C7)
val StatusLateText = Color(0xFFB45309)

val StatusExcusedBlue = Color(0xFF2563EB) // Excused Attendance Blue
val StatusExcusedContainer = Color(0xFFDBEAFE)
val StatusExcusedText = Color(0xFF1D4ED8)

val StatusPartialAmber = Color(0xFFF59E0B)
val StatusPartialContainer = Color(0xFFFFFBEB)
val StatusPartialText = Color(0xFF92400E)

// Soft Minimalist Gradients
val BrandRedGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFE11D48),
        Color(0xFFBE123C)
    )
)

val LuxuryHeroGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF111827),
        Color(0xFF1F2937),
        Color(0xFFE11D48)
    )
)

val LuxuryCardGradients = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFF9FAFB)
    )
)



package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// LIGHT THEME — Muted Teal / Emerald concept
// ============================================================

// Core brand colors
val KosherTeal = Color(0xFF0F766E)          // primary — Teal 700
val KosherLightBlue = Color(0xFF14B8A6)     // secondary — Teal 500
val KosherGreen = Color(0xFF059669)         // tertiary — Emerald 600

// Surfaces — background is intentionally a touch cooler/darker than
// surface so cards visually separate from the scaffold instead of
// blending into a single flat white.
val KosherBgLight = Color(0xFFF1F6F5)
val KosherSurfaceLight = Color(0xFFFFFFFF)
val KosherSurfaceVariantLight = Color(0xFFDCEAE8)
val KosherOnSurfaceVariantLight = Color(0xFF3F4F4C)

// Containers (used heavily throughout the app for card borders/chips —
// previously undefined, so Compose silently fell back to Material's
// default lavender/purple baseline palette)
val KosherPrimaryContainerLight = Color(0xFFCCFBF1)
val KosherOnPrimaryContainerLight = Color(0xFF042F2E)
val KosherSecondaryContainerLight = Color(0xFFD9F4F0)
val KosherOnSecondaryContainerLight = Color(0xFF0B3B38)
val KosherTertiaryContainerLight = Color(0xFFBBF7D0)
val KosherOnTertiaryContainerLight = Color(0xFF04331E)

// Outlines / borders
val KosherOutlineLight = Color(0xFF6F827D)
val KosherOutlineVariantLight = Color(0xFFC2D3D0)

// Error (used by the root-shell failure dialog, etc.)
val KosherErrorLight = Color(0xFFDC2626)
val KosherOnErrorLight = Color(0xFFFFFFFF)
val KosherErrorContainerLight = Color(0xFFFECACA)
val KosherOnErrorContainerLight = Color(0xFF7F1D1D)

// Surface container tonal steps (M3 elevation-by-color)
val KosherSurfaceDimLight = Color(0xFFD7E3E0)
val KosherSurfaceBrightLight = Color(0xFFF8FAFC)
val KosherSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val KosherSurfaceContainerLowLight = Color(0xFFF3F8F7)
val KosherSurfaceContainerLight = Color(0xFFEDF5F3)
val KosherSurfaceContainerHighLight = Color(0xFFE6EFEC)
val KosherSurfaceContainerHighestLight = Color(0xFFDFEAE7)

// Inverse
val KosherInverseSurfaceLight = Color(0xFF1B2C29)
val KosherInverseOnSurfaceLight = Color(0xFFECF3F1)
val KosherInversePrimaryLight = Color(0xFF6FE0D2)

// ============================================================
// DARK THEME
// ============================================================

val KosherTealDark = Color(0xFF38B2AC)      // primary — calm turquoise
val KosherLightBlueDark = Color(0xFF0F766E) // secondary — deep teal
val KosherGreenDark = Color(0xFF4ADE80)     // tertiary — soft green

val KosherBgDark = Color(0xFF0F172A)        // dark slate background
val KosherSurfaceDark = Color(0xFF1E293B)   // dark slate containers
val KosherSurfaceVariantDark = Color(0xFF2A3B45)
val KosherOnSurfaceVariantDark = Color(0xFFC4D3D6)

val KosherPrimaryContainerDark = Color(0xFF115E56)
val KosherOnPrimaryContainerDark = Color(0xFFA6F1E6)
val KosherSecondaryContainerDark = Color(0xFF1B4F49)
val KosherOnSecondaryContainerDark = Color(0xFFAEEAE2)
val KosherTertiaryContainerDark = Color(0xFF1E4030)
val KosherOnTertiaryContainerDark = Color(0xFFBFF6D4)

val KosherOutlineDark = Color(0xFF7C9392)
val KosherOutlineVariantDark = Color(0xFF3B4F4D)

val KosherErrorDark = Color(0xFFF87171)
val KosherOnErrorDark = Color(0xFF450A0A)
val KosherErrorContainerDark = Color(0xFF7F1D1D)
val KosherOnErrorContainerDark = Color(0xFFFFE4E4)

val KosherSurfaceDimDark = Color(0xFF0F172A)
val KosherSurfaceBrightDark = Color(0xFF37485A)
val KosherSurfaceContainerLowestDark = Color(0xFF0A101C)
val KosherSurfaceContainerLowDark = Color(0xFF182231)
val KosherSurfaceContainerDark = Color(0xFF212D40)
val KosherSurfaceContainerHighDark = Color(0xFF29374D)
val KosherSurfaceContainerHighestDark = Color(0xFF34445C)

val KosherInverseSurfaceDark = Color(0xFFE7F0EE)
val KosherInverseOnSurfaceDark = Color(0xFF1B2C29)
val KosherInversePrimaryDark = Color(0xFF0F766E)

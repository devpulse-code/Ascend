package com.abyssinia.dev.ascend.ui.onboarding

// Data class for each onboarding screen
data class OnboardingItem(
    val imageRes: Int,      // Drawable resource (like R.drawable.img_habit)
    val title: String,      // Main headline text
    val description: String // Subtext / explanation
)

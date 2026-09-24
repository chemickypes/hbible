package com.hooloovoochimico.kmp.hbible.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Material 3 Expressive motion, implemented with standard Compose animation
 * APIs tuned to Material 3 durations and easings (see skill §3B).
 */
val ExpressiveEase = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

object ExpressiveMotion {
  const val DURATION_SHORT = 150
  const val DURATION_MEDIUM = 300
  const val DURATION_LONG = 500

  /** Default expressive spatial motion: critically damped, medium-low stiffness. */
  fun <T> spatialSpring(): SpringSpec<T> =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 450f)

  /** Fast expressive spatial motion for quick enter/exit transitions. */
  fun <T> fastSpatialSpring(): SpringSpec<T> =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1350f)

  /** Expressive effects motion for color/alpha animations. */
  fun effectsTween(durationMillis: Int = DURATION_SHORT): FiniteAnimationSpec<Float> =
    tween(durationMillis = durationMillis, easing = ExpressiveEase)
}

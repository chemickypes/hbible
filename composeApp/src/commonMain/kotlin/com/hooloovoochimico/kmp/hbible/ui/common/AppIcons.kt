/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Le 17 icone del set Filled usate dall'app (NEXT_STEPS punto 3c): copiate
 * verbatim dai sorgenti di androidx material-icons-core 1.7.3 (Apache 2.0)
 * per sostituire la dipendenza da material-icons-extended, che non riceve
 * piu' aggiornamenti. Visivamente identiche a Icons.Default.X /
 * Icons.AutoMirrored.Filled.X (stesso builder, stessi path).
 */
package com.hooloovoochimico.kmp.hbible.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Icone Material "Filled" usate dall'app, senza material-icons-extended. */
object AppIcons {

  private fun builder(name: String, autoMirror: Boolean = false): ImageVector.Builder =
    ImageVector.Builder(
      name = name,
      defaultWidth = 24.dp,
      defaultHeight = 24.dp,
      viewportWidth = 24f,
      viewportHeight = 24f,
      autoMirror = autoMirror,
    )

  // Stessi default di materialPath (AndroidX material-icons-core).
  private fun ImageVector.Builder.materialPathCompat(
    fillAlpha: Float = 1.0f,
    strokeAlpha: Float = 1.0f,
    pathFillType: PathFillType = PathFillType.NonZero,
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
  ) {
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = fillAlpha,
      stroke = null,
      strokeAlpha = strokeAlpha,
      strokeLineWidth = 1f,
      strokeLineCap = StrokeCap.Butt,
      strokeLineJoin = StrokeJoin.Miter,
      strokeLineMiter = 4f,
      pathFillType = pathFillType,
      pathBuilder = pathBuilder,
    )
  }


  val Close: ImageVector by lazy {
    builder("AppIcons.Close", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(19.0f, 6.41f)
                        lineTo(17.59f, 5.0f)
                        lineTo(12.0f, 10.59f)
                        lineTo(6.41f, 5.0f)
                        lineTo(5.0f, 6.41f)
                        lineTo(10.59f, 12.0f)
                        lineTo(5.0f, 17.59f)
                        lineTo(6.41f, 19.0f)
                        lineTo(12.0f, 13.41f)
                        lineTo(17.59f, 19.0f)
                        lineTo(19.0f, 17.59f)
                        lineTo(13.41f, 12.0f)
                        close()
            
      }
    }.build()
  }

  val Search: ImageVector by lazy {
    builder("AppIcons.Search", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(15.5f, 14.0f)
                        horizontalLineToRelative(-0.79f)
                        lineToRelative(-0.28f, -0.27f)
                        curveTo(15.41f, 12.59f, 16.0f, 11.11f, 16.0f, 9.5f)
                        curveTo(16.0f, 5.91f, 13.09f, 3.0f, 9.5f, 3.0f)
                        reflectiveCurveTo(3.0f, 5.91f, 3.0f, 9.5f)
                        reflectiveCurveTo(5.91f, 16.0f, 9.5f, 16.0f)
                        curveToRelative(1.61f, 0.0f, 3.09f, -0.59f, 4.23f, -1.57f)
                        lineToRelative(0.27f, 0.28f)
                        verticalLineToRelative(0.79f)
                        lineToRelative(5.0f, 4.99f)
                        lineTo(20.49f, 19.0f)
                        lineToRelative(-4.99f, -5.0f)
                        close()
                        moveTo(9.5f, 14.0f)
                        curveTo(7.01f, 14.0f, 5.0f, 11.99f, 5.0f, 9.5f)
                        reflectiveCurveTo(7.01f, 5.0f, 9.5f, 5.0f)
                        reflectiveCurveTo(14.0f, 7.01f, 14.0f, 9.5f)
                        reflectiveCurveTo(11.99f, 14.0f, 9.5f, 14.0f)
                        close()
            
      }
    }.build()
  }

  val Share: ImageVector by lazy {
    builder("AppIcons.Share", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(18.0f, 16.08f)
                        curveToRelative(-0.76f, 0.0f, -1.44f, 0.3f, -1.96f, 0.77f)
                        lineTo(8.91f, 12.7f)
                        curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
                        reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f)
                        lineToRelative(7.05f, -4.11f)
                        curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
                        curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
                        reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                        reflectiveCurveToRelative(-3.0f, 1.34f, -3.0f, 3.0f)
                        curveToRelative(0.0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
                        lineTo(8.04f, 9.81f)
                        curveTo(7.5f, 9.31f, 6.79f, 9.0f, 6.0f, 9.0f)
                        curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                        reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                        curveToRelative(0.79f, 0.0f, 1.5f, -0.31f, 2.04f, -0.81f)
                        lineToRelative(7.12f, 4.16f)
                        curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                        curveToRelative(0.0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
                        curveToRelative(1.61f, 0.0f, 2.92f, -1.31f, 2.92f, -2.92f)
                        reflectiveCurveToRelative(-1.31f, -2.92f, -2.92f, -2.92f)
                        close()
            
      }
    }.build()
  }

  val Refresh: ImageVector by lazy {
    builder("AppIcons.Refresh", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(17.65f, 6.35f)
                        curveTo(16.2f, 4.9f, 14.21f, 4.0f, 12.0f, 4.0f)
                        curveToRelative(-4.42f, 0.0f, -7.99f, 3.58f, -7.99f, 8.0f)
                        reflectiveCurveToRelative(3.57f, 8.0f, 7.99f, 8.0f)
                        curveToRelative(3.73f, 0.0f, 6.84f, -2.55f, 7.73f, -6.0f)
                        horizontalLineToRelative(-2.08f)
                        curveToRelative(-0.82f, 2.33f, -3.04f, 4.0f, -5.65f, 4.0f)
                        curveToRelative(-3.31f, 0.0f, -6.0f, -2.69f, -6.0f, -6.0f)
                        reflectiveCurveToRelative(2.69f, -6.0f, 6.0f, -6.0f)
                        curveToRelative(1.66f, 0.0f, 3.14f, 0.69f, 4.22f, 1.78f)
                        lineTo(13.0f, 11.0f)
                        horizontalLineToRelative(7.0f)
                        verticalLineTo(4.0f)
                        lineToRelative(-2.35f, 2.35f)
                        close()
            
      }
    }.build()
  }

  val Menu: ImageVector by lazy {
    builder("AppIcons.Menu", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(3.0f, 18.0f)
                        horizontalLineToRelative(18.0f)
                        verticalLineToRelative(-2.0f)
                        lineTo(3.0f, 16.0f)
                        verticalLineToRelative(2.0f)
                        close()
                        moveTo(3.0f, 13.0f)
                        horizontalLineToRelative(18.0f)
                        verticalLineToRelative(-2.0f)
                        lineTo(3.0f, 11.0f)
                        verticalLineToRelative(2.0f)
                        close()
                        moveTo(3.0f, 6.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(18.0f)
                        lineTo(21.0f, 6.0f)
                        lineTo(3.0f, 6.0f)
                        close()
            
      }
    }.build()
  }

  val Delete: ImageVector by lazy {
    builder("AppIcons.Delete", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(6.0f, 19.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(8.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(7.0f)
                        horizontalLineTo(6.0f)
                        verticalLineToRelative(12.0f)
                        close()
                        moveTo(19.0f, 4.0f)
                        horizontalLineToRelative(-3.5f)
                        lineToRelative(-1.0f, -1.0f)
                        horizontalLineToRelative(-5.0f)
                        lineToRelative(-1.0f, 1.0f)
                        horizontalLineTo(5.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(14.0f)
                        verticalLineTo(4.0f)
                        close()
            
      }
    }.build()
  }

  val Check: ImageVector by lazy {
    builder("AppIcons.Check", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(9.0f, 16.17f)
                        lineTo(4.83f, 12.0f)
                        lineToRelative(-1.42f, 1.41f)
                        lineTo(9.0f, 19.0f)
                        lineTo(21.0f, 7.0f)
                        lineToRelative(-1.41f, -1.41f)
                        close()
            
      }
    }.build()
  }

  val Add: ImageVector by lazy {
    builder("AppIcons.Add", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(19.0f, 13.0f)
                        horizontalLineToRelative(-6.0f)
                        verticalLineToRelative(6.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(-6.0f)
                        horizontalLineTo(5.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineToRelative(6.0f)
                        verticalLineTo(5.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(6.0f)
                        horizontalLineToRelative(6.0f)
                        verticalLineToRelative(2.0f)
                        close()
            
      }
    }.build()
  }

  val Settings: ImageVector by lazy {
    builder("AppIcons.Settings", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(19.14f, 12.94f)
                        curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
                        curveToRelative(0.0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
                        lineToRelative(2.03f, -1.58f)
                        curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
                        lineToRelative(-1.92f, -3.32f)
                        curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                        lineToRelative(-2.39f, 0.96f)
                        curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                        lineTo(14.4f, 2.81f)
                        curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                        horizontalLineToRelative(-3.84f)
                        curveToRelative(-0.24f, 0.0f, -0.43f, 0.17f, -0.47f, 0.41f)
                        lineTo(9.25f, 5.35f)
                        curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
                        lineTo(5.24f, 5.33f)
                        curveToRelative(-0.22f, -0.08f, -0.47f, 0.0f, -0.59f, 0.22f)
                        lineTo(2.74f, 8.87f)
                        curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
                        lineToRelative(2.03f, 1.58f)
                        curveTo(4.84f, 11.36f, 4.8f, 11.69f, 4.8f, 12.0f)
                        reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
                        lineToRelative(-2.03f, 1.58f)
                        curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
                        lineToRelative(1.92f, 3.32f)
                        curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                        lineToRelative(2.39f, -0.96f)
                        curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                        lineToRelative(0.36f, 2.54f)
                        curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                        horizontalLineToRelative(3.84f)
                        curveToRelative(0.24f, 0.0f, 0.44f, -0.17f, 0.47f, -0.41f)
                        lineToRelative(0.36f, -2.54f)
                        curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                        lineToRelative(2.39f, 0.96f)
                        curveToRelative(0.22f, 0.08f, 0.47f, 0.0f, 0.59f, -0.22f)
                        lineToRelative(1.92f, -3.32f)
                        curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
                        lineTo(19.14f, 12.94f)
                        close()
                        moveTo(12.0f, 15.6f)
                        curveToRelative(-1.98f, 0.0f, -3.6f, -1.62f, -3.6f, -3.6f)
                        reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
                        reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
                        reflectiveCurveTo(13.98f, 15.6f, 12.0f, 15.6f)
                        close()
            
      }
    }.build()
  }

  val KeyboardArrowUp: ImageVector by lazy {
    builder("AppIcons.KeyboardArrowUp", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(7.41f, 15.41f)
                        lineTo(12.0f, 10.83f)
                        lineToRelative(4.59f, 4.58f)
                        lineTo(18.0f, 14.0f)
                        lineToRelative(-6.0f, -6.0f)
                        lineToRelative(-6.0f, 6.0f)
                        close()
            
      }
    }.build()
  }

  val KeyboardArrowDown: ImageVector by lazy {
    builder("AppIcons.KeyboardArrowDown", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(7.41f, 8.59f)
                        lineTo(12.0f, 13.17f)
                        lineToRelative(4.59f, -4.58f)
                        lineTo(18.0f, 10.0f)
                        lineToRelative(-6.0f, 6.0f)
                        lineToRelative(-6.0f, -6.0f)
                        lineToRelative(1.41f, -1.41f)
                        close()
            
      }
    }.build()
  }

  val Info: ImageVector by lazy {
    builder("AppIcons.Info", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(12.0f, 2.0f)
                        curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                        reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                        reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                        reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                        close()
                        moveTo(13.0f, 17.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(-6.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(6.0f)
                        close()
                        moveTo(13.0f, 9.0f)
                        horizontalLineToRelative(-2.0f)
                        lineTo(11.0f, 7.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(2.0f)
                        close()
            
      }
    }.build()
  }

  val Favorite: ImageVector by lazy {
    builder("AppIcons.Favorite", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(12.0f, 21.35f)
                        lineToRelative(-1.45f, -1.32f)
                        curveTo(5.4f, 15.36f, 2.0f, 12.28f, 2.0f, 8.5f)
                        curveTo(2.0f, 5.42f, 4.42f, 3.0f, 7.5f, 3.0f)
                        curveToRelative(1.74f, 0.0f, 3.41f, 0.81f, 4.5f, 2.09f)
                        curveTo(13.09f, 3.81f, 14.76f, 3.0f, 16.5f, 3.0f)
                        curveTo(19.58f, 3.0f, 22.0f, 5.42f, 22.0f, 8.5f)
                        curveToRelative(0.0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                        lineTo(12.0f, 21.35f)
                        close()
            
      }
    }.build()
  }

  val Edit: ImageVector by lazy {
    builder("AppIcons.Edit", autoMirror = false).apply {
      materialPathCompat {
                        moveTo(3.0f, 17.25f)
                        verticalLineTo(21.0f)
                        horizontalLineToRelative(3.75f)
                        lineTo(17.81f, 9.94f)
                        lineToRelative(-3.75f, -3.75f)
                        lineTo(3.0f, 17.25f)
                        close()
                        moveTo(20.71f, 7.04f)
                        curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                        lineToRelative(-2.34f, -2.34f)
                        curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                        lineToRelative(-1.83f, 1.83f)
                        lineToRelative(3.75f, 3.75f)
                        lineToRelative(1.83f, -1.83f)
                        close()
            
      }
    }.build()
  }

  val Send: ImageVector by lazy {
    builder("AppIcons.Send", autoMirror = true).apply {
      materialPathCompat {
                        moveTo(2.01f, 21.0f)
                        lineTo(23.0f, 12.0f)
                        lineTo(2.01f, 3.0f)
                        lineTo(2.0f, 10.0f)
                        lineToRelative(15.0f, 2.0f)
                        lineToRelative(-15.0f, 2.0f)
                        close()
            
      }
    }.build()
  }

  val ArrowBack: ImageVector by lazy {
    builder("AppIcons.ArrowBack", autoMirror = true).apply {
      materialPathCompat {
                        moveTo(20.0f, 11.0f)
                        horizontalLineTo(7.83f)
                        lineToRelative(5.59f, -5.59f)
                        lineTo(12.0f, 4.0f)
                        lineToRelative(-8.0f, 8.0f)
                        lineToRelative(8.0f, 8.0f)
                        lineToRelative(1.41f, -1.41f)
                        lineTo(7.83f, 13.0f)
                        horizontalLineTo(20.0f)
                        verticalLineToRelative(-2.0f)
                        close()
            
      }
    }.build()
  }

  val ArrowForward: ImageVector by lazy {
    builder("AppIcons.ArrowForward", autoMirror = true).apply {
      materialPathCompat {
                        moveTo(12.0f, 4.0f)
                        lineToRelative(-1.41f, 1.41f)
                        lineTo(16.17f, 11.0f)
                        horizontalLineTo(4.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(12.17f)
                        lineToRelative(-5.58f, 5.59f)
                        lineTo(12.0f, 20.0f)
                        lineToRelative(8.0f, -8.0f)
                        close()
            
      }
    }.build()
  }
}

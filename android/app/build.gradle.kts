import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tamapoke.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tamapoke.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
            }
        }
    }

    sourceSets {
        getByName("main").assets.srcDir(rootProject.file("../tools/sdcard/mons"))
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val generatedLauncherIcons = layout.buildDirectory.dir("generated/res/launcherIcons")

val generateLauncherIcons by tasks.registering {
    val spriteSheet = rootProject.file("../tools/pmd_test/Idle.png")
    outputs.dir(generatedLauncherIcons)

    doLast {
        require(spriteSheet.exists()) { "TamaPoke Idle.png sprite sheet is missing: $spriteSheet" }
        val source = ImageIO.read(spriteSheet)
        require(source.width >= 32 && source.height >= 32) { "Unexpected Idle.png sprite sheet dimensions" }

        // Use the first idle animation frame directly from the in-game sprite sheet.
        val frame = source.getSubimage(0, 0, 32, 32)
        val outputRoot = generatedLauncherIcons.get().asFile
        outputRoot.deleteRecursively()

        val densities = listOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )

        densities.forEach { (density, size) ->
            val canvas = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val graphics = canvas.createGraphics()
            graphics.color = Color(225, 245, 255)
            graphics.fillRect(0, 0, size, size)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

            val spriteSize = (size * 0.78).toInt()
            val offset = (size - spriteSize) / 2
            graphics.drawImage(frame, offset, offset, spriteSize, spriteSize, null)
            graphics.dispose()

            val directory = outputRoot.resolve("mipmap-$density")
            directory.mkdirs()
            ImageIO.write(canvas, "png", directory.resolve("ic_launcher.png"))
        }
    }
}

android.sourceSets.getByName("main").res.srcDir(generatedLauncherIcons)
tasks.named("preBuild").configure {
    dependsOn(generateLauncherIcons)
}

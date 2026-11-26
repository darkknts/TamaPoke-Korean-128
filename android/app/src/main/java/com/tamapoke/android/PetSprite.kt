package com.tamapoke.android

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.DataInputStream
import java.io.InputStream

internal data class SpriteFrame(val bitmap: Bitmap, val durationMs: Int)
internal data class PokemonSprite(val actions: Map<Int, List<SpriteFrame>>)

private fun readU8(input: DataInputStream): Int = input.readUnsignedByte()

private fun readU16LE(input: DataInputStream): Int {
    val low = readU8(input)
    val high = readU8(input)
    return low or (high shl 8)
}

internal fun loadPokemonSprite(context: Context, speciesId: Int, shiny: Boolean): PokemonSprite? {
    if (speciesId !in 1..251) return null
    fun loadAsset(isShiny: Boolean): PokemonSprite? {
        val prefix = if (isShiny) "ps" else "p"
        val fileName = "%s%03d.bin".format(prefix, speciesId)
        return runCatching {
            context.assets.open(fileName).use { raw ->
                parseTpk2(raw)
            }
        }.getOrNull()
    }

    // If a shiny sprite is missing or damaged, keep the pet visible with its
    // regular sprite instead of showing an empty image.
    return if (shiny) loadAsset(true) ?: loadAsset(false) else loadAsset(false)
}

internal fun parseTpk2(raw: InputStream): PokemonSprite? {
    val input = DataInputStream(raw)
    val magic = ByteArray(4)
    input.readFully(magic)
    if (String(magic, Charsets.US_ASCII) != "TPK2") return null

    val actionCount = readU8(input)
    val paletteCount = readU16LE(input)
    require(actionCount in 1..16 && paletteCount in 1..256)

    val palette = IntArray(paletteCount) {
        val color = readU16LE(input)
        val r5 = (color shr 11) and 0x1f
        val g6 = (color shr 5) and 0x3f
        val b5 = color and 0x1f
        val r = (r5 * 255 + 15) / 31
        val g = (g6 * 255 + 31) / 63
        val b = (b5 * 255 + 15) / 31
        (0xff shl 24) or (r shl 16) or (g shl 8) or b
    }

    val actions = linkedMapOf<Int, List<SpriteFrame>>()
    var fallbackFrames: List<SpriteFrame>? = null

    repeat(actionCount) {
        val actionId = readU8(input)
        val width = readU8(input)
        val height = readU8(input)
        val frameCount = readU8(input)
        require(width in 1..128 && height in 1..128 && frameCount in 1..24)

        val durations = IntArray(frameCount) { readU16LE(input).coerceAtLeast(80) }
        val frames = ArrayList<SpriteFrame>(frameCount)
        repeat(frameCount) { frameIndex ->
            val pixels = IntArray(width * height)
            repeat(pixels.size) { pixelIndex ->
                val paletteIndex = readU8(input)
                pixels[pixelIndex] = if (paletteIndex == 0xff) {
                    0x00000000
                } else {
                    require(paletteIndex < palette.size)
                    palette[paletteIndex]
                }
            }
            val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
            frames += SpriteFrame(bitmap, durations[frameIndex])
        }
        if (frames.isNotEmpty()) {
            if (fallbackFrames == null) fallbackFrames = frames
            actions[actionId] = frames
        }
    }

    if (actions[0] == null && fallbackFrames != null) actions[0] = fallbackFrames!!
    if (actions.isEmpty()) return null
    return PokemonSprite(actions)
}

@Composable
fun PetSprite(
    speciesId: Int,
    shiny: Boolean,
    actionId: Int = 0,
    actionToken: Int = 0,
    loopAction: Boolean = false,
    modifier: Modifier = Modifier,
    imageSize: androidx.compose.ui.unit.Dp = 160.dp
) {
    val context = LocalContext.current
    val sprite = remember(context, speciesId, shiny) {
        loadPokemonSprite(context, speciesId, shiny)
    }
    var frameIndex by remember(sprite) { mutableIntStateOf(0) }
    var visibleAction by remember(sprite) { mutableIntStateOf(0) }

    LaunchedEffect(sprite, actionId, actionToken, loopAction) {
        frameIndex = 0
        visibleAction = actionId
        val idle = sprite?.actions?.get(0) ?: return@LaunchedEffect
        val requested = sprite.actions[actionId] ?: idle

        if (actionId != 0 && requested !== idle && requested.size > 1) {
            if (loopAction) {
                while (true) {
                    requested.forEachIndexed { index, frame ->
                        frameIndex = index
                        delay(frame.durationMs.toLong())
                    }
                }
            } else {
                requested.forEachIndexed { index, frame ->
                    frameIndex = index
                    delay(frame.durationMs.toLong())
                }
                visibleAction = 0
                frameIndex = 0
            }
        }

        while (idle.size > 1) {
            val frame = idle[frameIndex.coerceIn(idle.indices)]
            delay(frame.durationMs.toLong())
            frameIndex = (frameIndex + 1) % idle.size
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val frames = sprite?.actions?.get(visibleAction) ?: sprite?.actions?.get(0)
        if (!frames.isNullOrEmpty()) {
            Image(
                bitmap = frames[frameIndex.coerceIn(frames.indices)].bitmap.asImageBitmap(),
                contentDescription = "도감 번호 $speciesId 포켓몬",
                modifier = Modifier.size(imageSize),
                filterQuality = FilterQuality.None
            )
        } else {
            Text("포켓몬 이미지를 불러오지 못했습니다")
        }
    }
}


internal fun loadPetSpriteBitmap(context: Context, speciesId: Int, shiny: Boolean): Bitmap? {
    val sprite = loadPokemonSprite(context, speciesId, shiny) ?: return null
    return (sprite.actions[0] ?: sprite.actions.values.firstOrNull())?.firstOrNull()?.bitmap
}

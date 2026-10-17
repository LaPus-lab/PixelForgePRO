package com.example.pixelforgepro

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

private val Bg = ComposeColor(0xFF0B0D12)
private val Panel = ComposeColor(0xFF171B24)
private val Accent = ComposeColor(0xFF8B7CFF)
private val Muted = ComposeColor(0xFF9AA3B2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11, 13, 18)
        window.navigationBarColor = android.graphics.Color.rgb(11, 13, 18)
        setContent { PixelForgeApp() }
    }
}

@Composable
private fun PixelForgeApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var grayscale by remember { mutableStateOf(false) }
    var rotation by remember { mutableIntStateOf(0) }
    var edited by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("Открой фотографию и начни редактировать") }

    fun render() {
        val src = original ?: return
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cm = android.graphics.ColorMatrix()
        val sat = if (grayscale) 0f else saturation
        val satMatrix = android.graphics.ColorMatrix().apply { setSaturation(sat) }
        val b = brightness * 255f
        val c = contrast
        val offset = 128f * (1f - c) + b
        val adjust = android.graphics.ColorMatrix(floatArrayOf(
            c,0f,0f,0f,offset,
            0f,c,0f,0f,offset,
            0f,0f,c,0f,offset,
            0f,0f,0f,1f,0f
        ))
        satMatrix.postConcat(adjust)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(satMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        var result = out
        if (rotation % 360 != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            result = Bitmap.createBitmap(out, 0, 0, out.width, out.height, matrix, true)
            if (result !== out) out.recycle()
        }
        edited?.takeIf { it !== original && !it.isRecycled }?.recycle()
        edited = result
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    original?.takeIf { !it.isRecycled }?.recycle()
                    edited?.takeIf { it !== original && !it.isRecycled }?.recycle()
                    original = BitmapFactory.decodeStream(stream)
                    brightness = 0f; contrast = 1f; saturation = 1f
                    grayscale = false; rotation = 0
                    render()
                    status = "Фото загружено"
                }
            } catch (_: Exception) {
                status = "Не удалось открыть изображение"
            }
        }
    }

    fun saveImage() {
        val bmp = edited ?: original
        if (bmp == null) { status = "Сначала открой фотографию"; return }
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "PixelForge_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PixelForgePRO")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                status = "Сохранено в Pictures/PixelForgePRO"
                Toast.makeText(context, "Фото сохранено в галерею", Toast.LENGTH_SHORT).show()
            } else status = "Не удалось сохранить фото"
        } catch (_: Exception) { status = "Ошибка сохранения" }
    }

    LaunchedEffect(original, brightness, contrast, saturation, grayscale, rotation) {
        val src = original
        if (src != null) {
            // Keep large images manageable on phones while editing.
            render()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, background = Bg, surface = Panel,
            onBackground = ComposeColor.White, onSurface = ComposeColor.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("PIXELFORGE", color = ComposeColor.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("PRO  •  PHOTO STUDIO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Editor", tint = Accent, modifier = Modifier.padding(12.dp).size(26.dp))
                    }
                }

                Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 440.dp).padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val image = edited ?: original
                        if (image != null) {
                            Image(bitmap = image.asImageBitmap(), contentDescription = "Редактируемая фотография", modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp))
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Accent, modifier = Modifier.size(58.dp))
                                Text("Твоя фотография — здесь", color = ComposeColor.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text("Открой изображение, настрой цвет и сохрани результат.", color = Muted)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("Открыть фото")
                    }
                    OutlinedButton(onClick = { saveImage() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp)); Text("Сохранить")
                    }
                }

                Text("НАСТРОЙКИ ИЗОБРАЖЕНИЯ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SliderControl("Яркость", brightness, -0.5f, 0.5f, { brightness = it })
                        SliderControl("Контраст", contrast, 0.5f, 1.8f, { contrast = it })
                        SliderControl("Насыщенность", saturation, 0f, 2f, { saturation = it })
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Чёрно-белый фильтр", modifier = Modifier.weight(1f), color = ComposeColor.White)
                            Switch(checked = grayscale, onCheckedChange = { grayscale = it })
                        }
                    }
                }

                Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ИНСТРУМЕНТЫ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolButton("Повернуть", Icons.Default.RotateRight, Modifier.weight(1f)) {
                                rotation = (rotation + 90) % 360
                            }
                            ToolButton("Сбросить", Icons.Default.RestartAlt, Modifier.weight(1f)) {
                                brightness = 0f; contrast = 1f; saturation = 1f; grayscale = false; rotation = 0
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text("AI Studio", color = ComposeColor.White, fontWeight = FontWeight.SemiBold)
                                Text("Удаление фона, объектов и генеративная заливка — следующий этап интеграции модели.", color = Muted, fontSize = 12.sp)
                            }
                            Surface(color = ComposeColor(0xFF332C56), shape = RoundedCornerShape(8.dp)) {
                                Text("SOON", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                            }
                        }
                    }
                }
                Text(status, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                Text("Первая сборка • обработка фото выполняется локально на устройстве", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SliderControl(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = ComposeColor.White)
            Text(if (label == "Яркость") "${(value * 100).toInt()}%" else "${(value * 100).toInt()}%", color = Accent, fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

@Composable
private fun ToolButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(6.dp)); Text(label)
    }
}

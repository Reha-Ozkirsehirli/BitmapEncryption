package com.rehaozkirsehirli.bitmappass

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.math.BigInteger

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var editText: EditText
    private lateinit var textView: TextView
    private lateinit var encodeButton: Button
    private lateinit var decodeButton: Button
    private var selectedBitmap: Bitmap? = null


    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerLauncher()
        setUp()
    }

    private fun setUp() {
        imageView = findViewById(R.id.imageView)
        editText = findViewById(R.id.editText)
        encodeButton = findViewById(R.id.encodeButton)
        decodeButton = findViewById(R.id.decodeButton)
        textView = findViewById(R.id.textView)

        imageView.setOnClickListener {
            selectImage(it)
        }

        encodeButton.setOnClickListener {
            val bitmap = selectedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            bitmap?.let { bit ->
                val rgbaList = bitmapToRgba(bit)
                val hex = asciiToHex(editText.text.toString())
                val binary = hexToBinaryLong(hex).toCharArray()
                binary.forEachIndexed { index, c ->
                    if (c.toString().toByte() == 0.toByte()) {
                        if (rgbaList[index * 4 + 3] % 2 == 1) {
                            rgbaList[index * 4 + 3] -= 1
                        }
                    } else {
                        if (rgbaList[index * 4 + 3] % 2 == 0) {
                            rgbaList[index * 4 + 3] += 1
                        }
                    }
                }
                imageView.setImageBitmap(bitmapFromRgba(bit.width, bit.height, rgbaList))
                selectedBitmap = bitmapFromRgba(bit.width, bit.height, rgbaList)
            }

        }

        decodeButton.setOnClickListener {
            val bitmap = selectedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            bitmap?.let { bit ->
                val rgbaList = bitmapToRgba(bit)
                var countStr = ""
                for (i in 0 until 16) {
                    countStr += if (rgbaList[i * 4 + 3] % 2 == 0) {
                        "0"
                    } else {
                        "1"
                    }
                }
                val count = getCount(countStr)
                var string = ""
                for (i in 16 until 16 + count * 8) {
                    string += if (rgbaList[i * 4 + 3] % 2 == 0) {
                        "0"
                    } else {
                        "1"
                    }
                }
                textView.text = hexToAscii(binaryToHex(string))
            }
        }
    }

    private fun getCount(binary: String): Int {
        val hex = binaryToHex(binary)
        val output = java.lang.StringBuilder("")
        var i = 0
        while (i < hex.length) {
            val str = hex.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString().toInt()
    }

    private fun asciiToHex(asciiStr: String): String {
        val chars = asciiStr.toCharArray()
        val hex = StringBuilder()
        for (ch in chars) {
            hex.append(Integer.toHexString(ch.code))
        }
        return hex.toString()
    }

    private fun hexToBinary(hex: String): String {
        val i = hex.toInt(16)
        var result = Integer.toBinaryString(i)
        if (8 - (result.length % 8) != 0) {
            for (str in 0 until 8 - result.length % 8) {
                result = "0$result"
            }
        }
        val count = asciiToHex((result.length / 8).toString()).toInt(16)
        result = Integer.toBinaryString(count) + result
        if (8 - (result.length % 8) != 0) {
            for (str in 0 until 8 - result.length % 8) {
                result = "0$result"
            }
        }

        if (result.length / 8 < 10) {
            for (zero in 0 until 8) {
                result = "0$result"
            }
        }

        return result
    }

    private fun hexToBinaryLong(hex: String): String {
        var result = ""
        for (i in 0 until hex.length / 2) {
            val char = hex[i * 2].toString() + hex[i * 2 + 1]
            val chartInt = char.toInt(16)
            result = Integer.toBinaryString(chartInt) + result
            if (8 - (result.length % 8) != 0) {
                for (str in 0 until 8 - result.length % 8) {
                    result = "0$result"
                }
            }
        }

        val count = asciiToHex((result.length / 8).toString()).toInt(16)
        result = Integer.toBinaryString(count) + result
        if (8 - (result.length % 8) != 0) {
            for (str in 0 until 8 - result.length % 8) {
                result = "0$result"
            }
        }

        if (result.length / 8 <= 10) {
            for (zero in 0 until 8) {
                result = "0$result"
            }
        }

        return result
    }

    private fun binaryToHex(binary: String): String {
        return BigInteger(binary, 2).toString(16)
    }

    private fun hexToAscii(hexStr: String): String {
        var output = ""
        var i = 0
        while (i < hexStr.length) {
            val str = hexStr.substring(i, i + 2)
            output = str.toInt(16).toChar() + output
            i += 2
        }
        return output
    }

    private fun bitmapToRgba(bitmap: Bitmap): IntArray {
        require(bitmap.config == Bitmap.Config.ARGB_8888) { "Bitmap must be in ARGB_8888 format" }
        val pixels = IntArray(bitmap.width * bitmap.height)
        val bytes = IntArray(pixels.size * 4)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var i = 0
        for (pixel in pixels) {
            // Get components assuming is ARGB
            val a = pixel shr 24 and 0xff
            val r = pixel shr 16 and 0xff
            val g = pixel shr 8 and 0xff
            val b = pixel and 0xff
            bytes[i++] = r
            bytes[i++] = g
            bytes[i++] = b
            bytes[i++] = a
        }
        return bytes
    }

    private fun bitmapFromRgba(width: Int, height: Int, bytes: IntArray): Bitmap? {
        val pixels = IntArray(bytes.size / 4)
        var j = 0
        for (i in pixels.indices) {
            val r: Int = bytes[j++] and 0xff
            val g: Int = bytes[j++] and 0xff
            val b: Int = bytes[j++] and 0xff
            val a: Int = bytes[j++] and 0xff
            val pixel = a shl 24 or (r shl 16) or (g shl 8) or b
            pixels[i] = pixel
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }


    private fun selectImage(view: View) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction(
                        "Give Permission"
                    ) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            val intentToGallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }


    private fun registerLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        val imageData = intentFromResult.data
                        try {
                            val source = ImageDecoder.createSource(
                                this@MainActivity.contentResolver,
                                imageData!!
                            )
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
//                            selectedBitmap = makeSmallerBitmap(selectedBitmap!!, 300)
                            imageView.setImageBitmap(selectedBitmap)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //permission granted
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                } else {
                    //permission denied
                    Toast.makeText(this@MainActivity, "Permisson needed!", Toast.LENGTH_LONG).show()
                }
            }
    }
}
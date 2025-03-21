package com.example.audiotranscriber

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.audiotranscriber.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null
    private var selectedAudioUri: Uri? = null

    private val RECORD_AUDIO_PERMISSION = 123
    private val PICK_AUDIO_REQUEST = 456

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        binding.selectFileButton.setOnClickListener {
            selectAudioFile()
        }

        binding.recordButton.setOnClickListener {
            if (checkPermission()) {
                if (!isRecording) {
                    startRecording()
                } else {
                    stopRecording()
                }
            } else {
                requestPermission()
            }
        }

        binding.transcribeButton.setOnClickListener {
            transcribeAudio()
        }

        binding.saveButton.setOnClickListener {
            saveTranscription()
        }
    }

    private fun selectAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        startActivityForResult(intent, PICK_AUDIO_REQUEST)
    }

    private fun startRecording() {
        try {
            audioFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.mp3")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            binding.recordButton.text = "Stop Recording"
            binding.transcribeButton.isEnabled = false
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            binding.recordButton.text = "Record Audio"
            binding.transcribeButton.isEnabled = true
            selectedAudioUri = null
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun transcribeAudio() {
        binding.progressBar.visibility = View.VISIBLE
        binding.transcribeButton.isEnabled = false
        binding.saveButton.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = when {
                    audioFile != null -> audioFile!!
                    selectedAudioUri != null -> {
                        val tempFile = File(cacheDir, "selected_audio.mp3")
                        contentResolver.openInputStream(selectedAudioUri!!)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }
                    else -> throw IOException("No audio file available")
                }

                val apiKey = BuildConfig.OPENAI_API_KEY ?: throw IOException("API key not found")

                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("audio/mpeg".toMediaTypeOrNull())
                    )
                    .addFormDataPart("model", "whisper-1")
                    .build()

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val transcription = jsonResponse.getString("text")
                        binding.transcriptionText.text = transcription
                        binding.saveButton.isEnabled = true
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Transcription failed: ${response.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.transcribeButton.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    binding.transcribeButton.isEnabled = true
                }
            }
        }
    }

    private fun saveTranscription() {
        val transcription = binding.transcriptionText.text.toString()
        if (transcription.isBlank()) {
            Toast.makeText(this, "No transcription to save", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "transcription_$timestamp.txt"

                // Create a directory for transcriptions if it doesn't exist
                val transcriptionsDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "transcriptions")
                if (!transcriptionsDir.exists()) {
                    transcriptionsDir.mkdirs()
                }

                val file = File(transcriptionsDir, fileName)
                file.writeText(transcription)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Transcription saved to: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to save transcription: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio Permission Granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedAudioUri = uri
                audioFile = null
                binding.transcribeButton.isEnabled = true
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
    }
}
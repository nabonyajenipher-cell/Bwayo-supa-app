package com.bwayo.musicstudio

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.exp
import kotlin.math.tanh

class MainActivity : Activity() {
    private val sampleRate = 44100
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var projectPlayer: MediaPlayer? = null
    private lateinit var projectsList: LinearLayout
    private var importedBeat: ShortArray? = null
    private lateinit var importedBeatLabel: TextView
    @Volatile private var recording = false
    private var output: File? = null
    private lateinit var status: TextView
    private lateinit var beatChoice: Spinner
    private lateinit var tempoLabel: TextView
    private lateinit var tempo: SeekBar
    private lateinit var beatVolume: SeekBar
    private lateinit var beatVolumeLabel: TextView
    private val beats = listOf("Hip-hop", "Afrobeat", "Pop", "Slow R&B")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 34, 28, 28)
            setBackgroundColor(0xFF17132B.toInt())
        }
        fun label(text: String, size: Float, color: Int = 0xFFFFFFFF.toInt()) =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                setPadding(0, 12, 0, 12)
            }

        root.addView(label("BWAYO MUSIC STUDIO", 25f))
        root.addView(label("Record your voice with a softer offline backing beat.", 15f, 0xFFD5D0E8.toInt()))
        root.addView(label("Choose a beat", 17f))
        val importBeat = Button(this).apply { text = "＋  IMPORT MY BEAT (WAV)" }
        importedBeatLabel = label("No custom beat selected", 13f, 0xFFD5D0E8.toInt())
        root.addView(importBeat)
        root.addView(importedBeatLabel)
        importBeat.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/wav"
            }
            startActivityForResult(intent, 81)
        }
        beatChoice = Spinner(this)
        beatChoice.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, beats)
        root.addView(beatChoice)
        tempoLabel = label("Tempo: 100 BPM", 15f)
        root.addView(tempoLabel)
        tempo = SeekBar(this).apply { max = 100; progress = 40 }
        tempo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                tempoLabel.text = "Tempo: ${60 + p} BPM"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(tempo)
        beatVolumeLabel = label("Beat volume: 15% (keep your voice clear)", 15f)
        root.addView(beatVolumeLabel)
        beatVolume = SeekBar(this).apply { max = 100; progress = 15 }
        beatVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                beatVolumeLabel.text = "Beat volume: ${p}% (your voice stays in front)"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(beatVolume)
        val start = Button(this).apply { text = "●  START RECORDING" }
        val stop = Button(this).apply { text = "■  STOP & SAVE"; isEnabled = false }
        root.addView(start)
        root.addView(stop)
        status = label("Ready. Headphones are recommended to reduce beat echo.", 14f, 0xFFBEB6D8.toInt())
        root.addView(status)
        root.addView(label("RECORDED PROJECTS", 19f))
        val refresh = Button(this).apply { text = "↻  REFRESH PROJECTS" }
        root.addView(refresh)
        projectsList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(projectsList)
        refresh.setOnClickListener { refreshProjects() }
        setContentView(root)
        refreshProjects()

        start.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7)
                status.text = "Allow microphone access, then tap Start again."
            } else if (!recording) {
                startRecording()
                start.isEnabled = false
                stop.isEnabled = true
            }
        }
        stop.setOnClickListener {
            stopRecording()
            start.isEnabled = true
            stop.isEnabled = false
        }
    }

    private fun startRecording() {
        val minIn = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val minOut = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = maxOf(minIn, minOut, 4096)
        try {
            val inputRecorder = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
            )
            val outputPlayer = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM
            )
            recorder = inputRecorder
            player = outputPlayer
            val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
            output = File(musicDir, "Bwayo_Recording_${System.currentTimeMillis()}.wav")
            inputRecorder.startRecording()
            outputPlayer.play()
            recording = true
            status.text = "Recording with ${beats[beatChoice.selectedItemPosition]} beat…"
            val bpm = 60 + tempo.progress
            val style = beatChoice.selectedItemPosition
            thread(name = "bwayo-audio-mix") {
                var sampleIndex = 0L
                val input = ShortArray(1024)
                val mixed = ShortArray(1024)
                val file = output!!
                FileOutputStream(file).use { stream ->
                    stream.write(ByteArray(44))
                    while (recording) {
                        val count = inputRecorder.read(input, 0, input.size)
                        if (count <= 0) continue
                        for (i in 0 until count) {
                            val beat = beatSample(sampleIndex, bpm, style)
                            // Gentle compression and soft limiting reduce harsh vocal peaks.
                            val rawMic = input[i].toDouble() * 0.65
                            val compressedMic = if (kotlin.math.abs(rawMic) > 6500.0) {
                                (if (rawMic < 0) -1.0 else 1.0) * (6500.0 + (kotlin.math.abs(rawMic) - 6500.0) * 0.28)
                            } else rawMic
                            val mic = tanh(compressedMic / 25000.0) * 25000.0
                            val custom = importedBeat
                            val backing = if (custom != null && custom.isNotEmpty()) custom[(sampleIndex % custom.size).toInt()].toDouble() else beat
                            val beatGain = (beatVolume.progress / 100.0) * 0.25
                            mixed[i] = (mic + backing * beatGain).toInt().coerceIn(-32768, 32767).toShort()
                            sampleIndex++
                        }
                        outputPlayer.write(mixed, 0, count)
                        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until count) bytes.putShort(mixed[i])
                        stream.write(bytes.array())
                    }
                    writeWavHeader(file, sampleIndex.toInt() * 2)
                }
                runOnUiThread {
                    status.text = "Saved recording: ${file.name}"
                    refreshProjects()
                }
            }
        } catch (e: Exception) {
            recording = false
            status.text = "Audio error: ${e.message}"
            releaseAudio()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 81 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val samples = loadWavBeat(uri)
                importedBeat = samples
                importedBeatLabel.text = "Custom beat loaded (" + (samples.size / sampleRate) + " sec). It will loop while recording."
                status.text = "Your WAV beat is ready."
            } catch (e: Exception) {
                importedBeat = null
                importedBeatLabel.text = "Could not load beat. Choose a PCM WAV file."
                status.text = "Beat import error: " + e.message
            }
        }
    }

    // Imports standard 16-bit PCM WAV, mono or stereo, and resamples it to 44.1 kHz mono.
    private fun loadWavBeat(uri: Uri): ShortArray {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot open selected file")
        if (bytes.size < 44 || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE")
            throw IllegalArgumentException("Not a WAV file")
        fun u16(pos: Int) = (bytes[pos].toInt() and 255) or ((bytes[pos + 1].toInt() and 255) shl 8)
        fun i32(pos: Int) = (bytes[pos].toInt() and 255) or ((bytes[pos + 1].toInt() and 255) shl 8) or
            ((bytes[pos + 2].toInt() and 255) shl 16) or (bytes[pos + 3].toInt() shl 24)
        val format = u16(20)
        val channels = u16(22)
        val rate = i32(24)
        val bits = u16(34)
        if (format != 1 || bits != 16 || channels !in 1..2 || rate <= 0)
            throw IllegalArgumentException("Use 16-bit PCM WAV, mono or stereo")
        var pos = 12
        var dataStart = -1
        var dataSize = 0
        while (pos + 8 <= bytes.size) {
            val chunk = String(bytes, pos, 4)
            val size = i32(pos + 4).coerceAtLeast(0)
            if (chunk == "data") { dataStart = pos + 8; dataSize = minOf(size, bytes.size - dataStart); break }
            pos += 8 + size + (size and 1)
        }
        if (dataStart < 0 || dataSize < channels * 2) throw IllegalArgumentException("WAV has no audio data")
        val frames = dataSize / (channels * 2)
        val mono = ShortArray(frames)
        var p = dataStart
        for (i in 0 until frames) {
            var sum = 0
            repeat(channels) {
                val lo = bytes[p++].toInt() and 255
                val hi = bytes[p++].toInt()
                sum += (hi shl 8) or lo
            }
            mono[i] = (sum / channels).toShort()
        }
        if (rate == sampleRate) return mono
        val outCount = (frames.toLong() * sampleRate / rate).toInt().coerceAtLeast(1)
        return ShortArray(outCount) { i ->
            val source = i.toDouble() * rate / sampleRate
            val left = source.toInt().coerceIn(0, frames - 1)
            val right = (left + 1).coerceAtMost(frames - 1)
            val fraction = source - left
            (mono[left] * (1.0 - fraction) + mono[right] * fraction).toInt().toShort()
        }
    }

    private fun beatSample(index: Long, bpm: Int, style: Int): Double {
        val beatLen = sampleRate * 60.0 / bpm
        val step = (index % (beatLen * 4).toLong()).toDouble()
        val beatNo = (step / beatLen).toInt()
        val phase = step % beatLen
        val kick = beatNo == 0 || ((style == 1 || style == 0) && beatNo == 2)
        val snare = (beatNo == 1 || beatNo == 3) && phase < 5000
        val hatEvery = if (style == 3) 2 else 1
        var v = 0.0
        if (kick && phase < 7000) {
            val t = phase / sampleRate
            v += sin(2 * PI * (65.0 - 30.0 * t) * t) * exp(-phase / 1800.0) * 17000
        }
        if (snare && phase < 5000) {
            v += sin(index * 12.9898) * 9000 * exp(-phase / 900.0)
        }
        val hatPeriod = maxOf(1L, (beatLen / hatEvery).toLong())
        val hatPhase = index % hatPeriod
        if (hatPhase < 900) v += sin(index * 0.73) * 2200 * exp(-hatPhase / 250.0)
        return v
    }

    private fun refreshProjects() {
        if (!::projectsList.isInitialized) return
        projectsList.removeAllViews()
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val files = dir.listFiles()?.filter { it.isFile && it.extension.equals("wav", true) }?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) {
            projectsList.addView(TextView(this).apply {
                text = "No recordings yet. Your saved songs will appear here."
                textSize = 14f
                setTextColor(0xFFD5D0E8.toInt())
                setPadding(0, 12, 0, 18)
            })
            return
        }
        files.forEach { file ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(0xFF28223F.toInt())
            }
            row.addView(TextView(this).apply {
                text = file.name
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
            })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val playButton = Button(this).apply { text = "▶ PLAY" }
            val stopButton = Button(this).apply { text = "■ STOP" }
            actions.addView(playButton, LinearLayout.LayoutParams(0, -2, 1f))
            actions.addView(stopButton, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(actions)
            playButton.setOnClickListener { playProject(file) }
            stopButton.setOnClickListener { stopProjectPlayback() }
            projectsList.addView(row)
            projectsList.addView(TextView(this).apply { text = " " })
        }
    }

    private fun playProject(file: File) {
        stopProjectPlayback()
        try {
            val mp = MediaPlayer()
            projectPlayer = mp
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                it.release()
                if (projectPlayer === it) projectPlayer = null
                status.text = "Finished playing: ${file.name}"
            }
            mp.setOnErrorListener { it, _, _ ->
                it.release()
                if (projectPlayer === it) projectPlayer = null
                status.text = "Could not play this recording."
                true
            }
            mp.prepare()
            mp.start()
            status.text = "Playing: ${file.name}"
        } catch (e: Exception) {
            status.text = "Playback error: ${e.message}"
            stopProjectPlayback()
        }
    }

    private fun stopProjectPlayback() {
        try { projectPlayer?.stop() } catch (_: Exception) {}
        try { projectPlayer?.release() } catch (_: Exception) {}
        projectPlayer = null
    }

    private fun stopRecording() {
        recording = false
        try { recorder?.stop() } catch (_: Exception) {}
        try { player?.stop() } catch (_: Exception) {}
        releaseAudio()
        status.text = "Finishing your recording…"
    }

    private fun releaseAudio() {
        try { recorder?.release() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        recorder = null
        player = null
    }

    private fun writeWavHeader(file: File, dataBytes: Int) {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()); header.putInt(dataBytes + 36); header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray()); header.putInt(16); header.putShort(1); header.putShort(1)
        header.putInt(sampleRate); header.putInt(sampleRate * 2); header.putShort(2); header.putShort(16)
        header.put("data".toByteArray()); header.putInt(dataBytes)
        java.io.RandomAccessFile(file, "rw").use { it.seek(0); it.write(header.array()) }
    }

    override fun onDestroy() {
        if (recording) stopRecording()
        stopProjectPlayback()
        super.onDestroy()
    }
}

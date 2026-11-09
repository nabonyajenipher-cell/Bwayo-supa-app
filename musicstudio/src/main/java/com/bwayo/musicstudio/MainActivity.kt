package com.bwayo.musicstudio

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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

class MainActivity : Activity() {
    private val sampleRate = 44100
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var projectPlayer: MediaPlayer? = null
    private lateinit var projectsList: LinearLayout
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
                            val mic = input[i].toInt() * 0.95
                            val beatGain = (beatVolume.progress / 100.0) * 0.25
                            mixed[i] = (mic + beat * beatGain).toInt().coerceIn(-32768, 32767).toShort()
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

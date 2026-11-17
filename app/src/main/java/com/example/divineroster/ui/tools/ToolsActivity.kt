package com.example.divineroster.ui.tools

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.divineroster.R
import com.example.divineroster.audio.Metronome
import com.example.divineroster.audio.PitchPipe
import com.example.divineroster.databinding.ActivityToolsBinding
import com.example.divineroster.ui.common.BottomNav
import java.util.Locale

class ToolsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityToolsBinding

    private val pitchPipe = PitchPipe()
    private var octave = 4

    private val noteNames = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val accidentals = setOf(1, 3, 6, 8, 10)

    private lateinit var metronome: Metronome
    private val beatChoices = listOf(2, 3, 4, 6)
    private val beatDots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BottomNav.setup(this, binding.bottomNavigation, R.id.nav_tools)
        setupPitchPipe()
        setupMetronome()
    }

    // ---- Pitch pipe -------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPitchPipe() {
        val margin = dp(4)
        noteNames.forEachIndexed { semitone, name ->
            val isAccidental = semitone in accidentals
            val key = Button(this).apply {
                text = name
                textSize = 18f
                isAllCaps = false
                setBackgroundResource(if (isAccidental) R.drawable.bg_note_accidental else R.drawable.bg_note_natural)
                val restingColor = if (isAccidental) {
                    ContextCompat.getColor(context, R.color.primary_dark)
                } else {
                    currentTextColor
                }
                setTextColor(restingColor)
                stateListAnimator = null
                contentDescription = "Play ${name.replace("♯", " sharp")}"

                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            view.isPressed = true
                            setTextColor(Color.WHITE)
                            playNote(semitone)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            view.isPressed = false
                            setTextColor(restingColor)
                            releaseNote()
                            if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
                        }
                    }
                    true
                }
            }
            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                height = dp(64)
                setMargins(margin, margin, margin, margin)
            }
            binding.pitchGrid.addView(key, params)
        }

        binding.btnOctaveDown.setOnClickListener { changeOctave(-1) }
        binding.btnOctaveUp.setOnClickListener { changeOctave(1) }
    }

    private fun changeOctave(delta: Int) {
        octave = (octave + delta).coerceIn(2, 6)
        binding.tvOctave.text = octave.toString()
    }

    private fun playNote(semitone: Int) {
        val frequency = PitchPipe.frequencyOf(semitone, octave)
        pitchPipe.start(frequency)
        binding.tvPlayingNote.text = String.format(Locale.US, "%s%d  -  %.1f Hz", noteNames[semitone], octave, frequency)
    }

    private fun releaseNote() {
        pitchPipe.stop()
        binding.tvPlayingNote.text = " "
    }

    // ---- Metronome --------------------------------------------------------------------------

    private fun setupMetronome() {
        metronome = Metronome { beatIndex -> highlightBeat(beatIndex) }

        binding.seekBpm.max = Metronome.MAX_BPM - Metronome.MIN_BPM
        binding.seekBpm.progress = metronome.bpm - Metronome.MIN_BPM
        showBpm()

        binding.seekBpm.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) setBpm(progress + Metronome.MIN_BPM)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
        binding.btnBpmDown.setOnClickListener { setBpm(metronome.bpm - 1) }
        binding.btnBpmUp.setOnClickListener { setBpm(metronome.bpm + 1) }

        buildBeatChoices()
        buildBeatIndicators(metronome.beatsPerBar)

        binding.btnMetronomeToggle.setOnClickListener {
            if (metronome.isRunning) stopMetronome() else startMetronome()
        }
    }

    private fun setBpm(value: Int) {
        metronome.bpm = value
        binding.seekBpm.progress = metronome.bpm - Metronome.MIN_BPM
        showBpm()
    }

    private fun showBpm() {
        binding.tvBpm.text = metronome.bpm.toString()
    }

    private fun buildBeatChoices() {
        beatChoices.forEach { beats ->
            val choice = RadioButton(this).apply {
                id = View.generateViewId()
                text = beats.toString()
                tag = beats
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_urgency_normal_selector)
                setButtonDrawable(null)
                textSize = 16f
                isChecked = beats == metronome.beatsPerBar
            }
            val params = RadioGroup.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
            binding.rgBeats.addView(choice, params)
        }
        binding.rgBeats.setOnCheckedChangeListener { group, checkedId ->
            val beats = group.findViewById<RadioButton>(checkedId)?.tag as? Int ?: return@setOnCheckedChangeListener
            metronome.beatsPerBar = beats
            buildBeatIndicators(beats)
        }
    }

    private fun buildBeatIndicators(beats: Int) {
        binding.beatIndicators.removeAllViews()
        beatDots.clear()
        repeat(beats) {
            val dot = View(this).apply { setBackgroundResource(R.drawable.dot_beat_off) }
            val params = android.widget.LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                setMargins(dp(6), 0, dp(6), 0)
            }
            binding.beatIndicators.addView(dot, params)
            beatDots.add(dot)
        }
    }

    private fun highlightBeat(beatIndex: Int) {
        if (!metronome.isRunning) return
        beatDots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                when {
                    index != beatIndex -> R.drawable.dot_beat_off
                    index == 0 -> R.drawable.dot_beat_first
                    else -> R.drawable.dot_beat_on
                }
            )
        }
    }

    private fun startMetronome() {
        metronome.start()
        binding.btnMetronomeToggle.text = "Stop"
    }

    private fun stopMetronome() {
        metronome.stop()
        binding.btnMetronomeToggle.text = "Start"
        beatDots.forEach { it.setBackgroundResource(R.drawable.dot_beat_off) }
    }

    // ---- Lifecycle --------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        BottomNav.markSelected(binding.bottomNavigation, R.id.nav_tools)
    }

    override fun onPause() {
        // Sound should never keep playing once the screen is gone.
        pitchPipe.stop()
        stopMetronome()
        super.onPause()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

/**
 * NEMO OPENING — CIRCULAR IRIS EXPANSION ENGINE
 * Full Mascot Animation -> Iris Wipe Circular Expansion -> Dashboard Reveal
 */

(function () {
  'use strict';

  // --- Studio State ---
  const state = {
    preset: 'smooth-iris',       // 'smooth-iris' | 'anticipation-burst' | 'mascot-fly'
    theme: 'night',              // 'night' | 'day'
    viewportMode: 'phone',       // 'phone' | 'full'
    duration: 750,               // Circle expansion duration in ms
    mascotDuration: 3000,        // Exactly 3 seconds initial mascot animation
    audioEnabled: true,
    themeAdapt: true,
    isPlaying: false,
    animFrameId: null,
  };

  // --- DOM Elements ---
  const elPhoneWrapper = document.getElementById('phoneWrapper');
  const elPhoneScreen = document.getElementById('phoneScreen');
  const elExpandingCircle = document.getElementById('expandingCircle');
  const elRevealedDashboard = document.getElementById('revealedDashboard');
  const elOpeningFgStage = document.getElementById('openingFgStage');
  const elMascotHousing = document.getElementById('mascotHousing');
  const elMascotImg = document.getElementById('mascotImg');
  const elMascotFaceSwap = document.getElementById('mascotFaceSwap');
  const elSplashTypography = document.getElementById('splashTypography');
  const elLetters = document.querySelectorAll('.name-row .letter');
  const elSplashTagline = document.getElementById('splashTagline');
  const elDashMiniAvatar = document.getElementById('dashMiniAvatar');
  const elStatusPillText = document.getElementById('statusPillText');

  // Sliders & Controls
  const elTimelineScrubber = document.getElementById('timelineScrubber');
  const elValTimeline = document.getElementById('valTimeline');
  const elSliderDuration = document.getElementById('sliderDuration');
  const elValDuration = document.getElementById('valDuration');
  const elSliderMascotDuration = document.getElementById('sliderMascotDuration');
  const elValMascotDuration = document.getElementById('valMascotDuration');
  const elToggleThemeAdapt = document.getElementById('toggleThemeAdapt');
  const elCodeBlock = document.getElementById('codeBlockCompose');
  const elCopyBtn = document.getElementById('btnCopyCompose');
  const elCopyLabel = document.getElementById('copyLabel');

  // Timeline Step Ticks
  const elTick1 = document.getElementById('tick1');
  const elTick2 = document.getElementById('tick2');
  const elTick3 = document.getElementById('tick3');

  // Audio Context (Synthesized 432Hz Zen Chime)
  let audioCtx = null;

  function initAudio() {
    if (!audioCtx) {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (AudioContextClass) audioCtx = new AudioContextClass();
    }
    if (audioCtx && audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
  }

  function playZenChime() {
    if (!state.audioEnabled) return;
    initAudio();
    if (!audioCtx) return;

    const now = audioCtx.currentTime;
    const baseFreq = 432; // 432Hz meditative fundamental

    const harmonics = [
      { freq: baseFreq, gain: 0.26, decay: 2.4 },
      { freq: baseFreq * 1.5, gain: 0.12, decay: 1.8 },
      { freq: baseFreq * 2.0, gain: 0.08, decay: 1.4 },
      { freq: baseFreq * 2.66, gain: 0.03, decay: 0.9 },
    ];

    harmonics.forEach(({ freq, gain: peakGain, decay }) => {
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, now);

      gain.gain.setValueAtTime(0.0001, now);
      gain.gain.exponentialRampToValueAtTime(peakGain, now + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + decay);

      osc.connect(gain);
      gain.connect(audioCtx.destination);

      osc.start(now);
      osc.stop(now + decay + 0.1);
    });
  }

  // --- Easing Math Functions ---
  function easeInOutCubic(x) {
    return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
  }

  function easeOutCubic(x) {
    return 1 - Math.pow(1 - x, 3);
  }

  function easeOutQuart(x) {
    return 1 - Math.pow(1 - x, 4);
  }

  function easeOutExpo(x) {
    return x === 1 ? 1 : 1 - Math.pow(2, -10 * x);
  }

  // --- Core Scrubber Renderer (0.0 to 1.0) ---
  // Total timeline:
  // 0.00 to 0.70 (70%): Phase 1 — Mascot dynamic animation (breathing + wink at 0.50..0.65)
  // 0.70 to 0.95 (25%): Phase 2 — White circle expansion until it covers whole screen
  // 0.95 to 1.00 (5%):  Phase 3 — Dashboard fully settled & interactive
  function renderTimelineProgress(p) {
    p = Math.max(0, Math.min(1, p));

    // Update scrubber UI
    elTimelineScrubber.value = Math.round(p * 100);
    elValTimeline.textContent = `${Math.round(p * 100)}%`;

    // Highlight active tick step
    elTick1.classList.toggle('active', p < 0.70);
    elTick2.classList.toggle('active', p >= 0.70 && p < 0.95);
    elTick3.classList.toggle('active', p >= 0.95);

    if (p < 0.70) {
      // =========================================================================
      // FASE 1: MASKOT BERANIMASI
      // =========================================================================
      const mascotNorm = p / 0.70; // 0.0 to 1.0
      elStatusPillText.textContent = 'Fase 1: Maskot beranimasi, bernapas & berkedip...';

      // Circle sits dormant around mascot (scale 1.0)
      elExpandingCircle.style.transform = 'translate(-50%, -50%) scale(1)';
      elExpandingCircle.style.borderRadius = '50%';
      elExpandingCircle.style.borderColor = 'var(--app-rim)';
      elRevealedDashboard.style.opacity = '0';
      elOpeningFgStage.style.opacity = '1';

      // Typography visible
      elSplashTypography.style.opacity = '1';
      elSplashTypography.style.transform = 'translateY(0)';

      // Mascot idle breathing motion
      const breathPhase = Math.sin(mascotNorm * Math.PI * 2);
      const ty = -breathPhase * 6; // up to -6px
      const sc = 1 + breathPhase * 0.02;

      // Face swap (wink) between 65% and 85% of mascotNorm
      const isWinking = mascotNorm >= 0.65 && mascotNorm <= 0.85;
      elMascotFaceSwap.style.opacity = isWinking ? '1' : '0';

      // Bounce on wink
      const winkBounce = isWinking ? -4 : 0;
      elMascotHousing.style.transform = `translate3d(0, ${ty + winkBounce}px, 0) scale(${sc})`;

      if (state.preset === 'mascot-fly') {
        elDashMiniAvatar.style.opacity = '0';
      }

    } else if (p < 0.95) {
      // =========================================================================
      // FASE 2: LINGKARAN MEMBESAR MEMENUHI LAYAR (THE HERO TRANSITION)
      // =========================================================================
      const expandNorm = (p - 0.70) / 0.25; // 0.0 to 1.0
      elStatusPillText.textContent = 'Fase 2: Lingkaran mengembang memenuhi layar...';

      // Keep mascot face neutral
      elMascotFaceSwap.style.opacity = '0';

      // Calculate circle scale (from scale 1.0 -> ~8.5 to cover the entire phone)
      let scale = 1;
      let borderRadius = 50;

      if (state.preset === 'anticipation-burst') {
        // Small 10% inhale dip first 15% of expandNorm
        if (expandNorm < 0.15) {
          const dip = Math.sin((expandNorm / 0.15) * Math.PI);
          scale = 1 - dip * 0.06; // slight inhale shrink
        } else {
          const burstProgress = (expandNorm - 0.15) / 0.85;
          const eased = easeOutQuart(burstProgress);
          scale = 0.94 + eased * 7.56;
          borderRadius = 50 * (1 - eased * 0.7);
        }
      } else {
        // Buttery Smooth Iris
        const eased = easeOutCubic(expandNorm);
        scale = 1 + eased * 7.5;
        borderRadius = 50 * (1 - eased * 0.7);
      }

      elExpandingCircle.style.transform = `translate(-50%, -50%) scale(${scale})`;
      elExpandingCircle.style.borderRadius = `${borderRadius}%`;
      elExpandingCircle.style.borderColor = expandNorm < 0.25 ? 'var(--app-rim)' : 'transparent';

      // Fade out splash typography rapidly
      const textFade = Math.max(0, 1 - expandNorm * 2.5);
      elSplashTypography.style.opacity = textFade;
      elSplashTypography.style.transform = `translateY(${expandNorm * 18}px)`;

      // Handle mascot behavior during expansion
      if (state.preset === 'mascot-fly') {
        // Mascot flies to the top-left avatar target
        const flyEased = easeOutQuart(expandNorm);
        const targetDeltaX = -138; // relative to center
        const targetDeltaY = -272; // relative to center
        const targetScale = 0.32;

        const currentX = targetDeltaX * flyEased;
        const currentY = targetDeltaY * flyEased;
        const currentScale = 1 + (targetScale - 1) * flyEased;

        elMascotHousing.style.transform = `translate3d(${currentX}px, ${currentY}px, 0) scale(${currentScale})`;
        elMascotHousing.style.opacity = '1';
        elDashMiniAvatar.style.opacity = expandNorm > 0.9 ? '1' : '0';
      } else {
        // Mascot gently dissolves into the clean white background
        const mascotFade = Math.max(0, 1 - expandNorm * 1.8);
        elMascotHousing.style.opacity = mascotFade;
        elMascotHousing.style.transform = `scale(${1 + expandNorm * 0.2})`;
      }

      // Inner dashboard emerges smoothly as circle finishes expansion
      const dashFade = Math.min(1, Math.max(0, (expandNorm - 0.35) / 0.65));
      elRevealedDashboard.style.opacity = dashFade;

    } else {
      // =========================================================================
      // FASE 3: LAYAR PENUH (DASHBOARD READY)
      // =========================================================================
      elStatusPillText.textContent = 'Fase 3: Transisi Berhasil! Antarmuka aktif.';

      elExpandingCircle.style.transform = 'translate(-50%, -50%) scale(8.5)';
      elExpandingCircle.style.borderRadius = '0%';
      elExpandingCircle.style.borderColor = 'transparent';
      elRevealedDashboard.style.opacity = '1';
      elSplashTypography.style.opacity = '0';

      if (state.preset === 'mascot-fly') {
        elMascotHousing.style.opacity = '0';
        elDashMiniAvatar.style.opacity = '1';
      } else {
        elMascotHousing.style.opacity = '0';
      }
    }
  }

  // --- Automated Sequence Playback ---
  function playFullSequence() {
    stopSequence();
    state.isPlaying = true;

    // Reset to 0
    renderTimelineProgress(0);

    const totalDuration = state.mascotDuration + state.duration;
    const mascotPhaseRatio = state.mascotDuration / totalDuration; // e.g. ~0.76
    const startTime = performance.now();

    let chimePlayed = false;

    function step(now) {
      if (!state.isPlaying) return;

      const elapsed = now - startTime;
      const rawProgress = elapsed / totalDuration;

      // Play chime right when mascot finishes and circle starts expanding
      if (rawProgress >= 0.70 && !chimePlayed) {
        playZenChime();
        chimePlayed = true;
      }

      if (rawProgress >= 1.0) {
        renderTimelineProgress(1.0);
        state.isPlaying = false;
        return;
      }

      renderTimelineProgress(rawProgress);
      state.animFrameId = requestAnimationFrame(step);
    }

    state.animFrameId = requestAnimationFrame(step);
  }

  function playCircleBurstOnly() {
    stopSequence();
    state.isPlaying = true;

    // Jump straight to start of burst (p = 0.70)
    renderTimelineProgress(0.70);
    playZenChime();

    const burstDuration = state.duration;
    const startTime = performance.now();

    function step(now) {
      if (!state.isPlaying) return;

      const elapsed = now - startTime;
      const burstProgress = elapsed / burstDuration;

      if (burstProgress >= 1.0) {
        renderTimelineProgress(1.0);
        state.isPlaying = false;
        return;
      }

      const p = 0.70 + burstProgress * 0.30;
      renderTimelineProgress(p);
      state.animFrameId = requestAnimationFrame(step);
    }

    state.animFrameId = requestAnimationFrame(step);
  }

  function stopSequence() {
    state.isPlaying = false;
    if (state.animFrameId) {
      cancelAnimationFrame(state.animFrameId);
      state.animFrameId = null;
    }
  }

  function resetToBeginning() {
    stopSequence();
    renderTimelineProgress(0);
    elStatusPillText.textContent = 'Siap memutar animasi opening...';
  }

  // --- Jetpack Compose Code Generator ---
  const composeSnippets = {
    'smooth-iris': `// =========================================================================
// TRANSISI IRIS EXPAND: BUTTERY SMOOTH (NEMO SPLASH)
// =========================================================================
// 1. Jalankan animasi maskot (breathe + wink)
// 2. Lingkaran putih mengembang (scale 1f -> 10f) mengisi seluruh layar
// 3. Masuk ke Main Tasks Screen

@Composable
fun NemoSplashWithIrisExpand(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transitionState = remember { Animatable(0f) }
    var isWinking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Fase 1: Animasi Maskot (2400ms)
        // Di detik 1.6s, maskot berkedip (wink)
        delay(1600L)
        isWinking = true
        delay(400L)
        isWinking = false
        delay(400L)

        // Fase 2: Lingkaran mengembang memenuhi layar (750ms)
        transitionState.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 750,
                easing = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f) // Smooth deceleration
            )
        )
        onFinished()
    }

    val expandProgress = transitionState.value
    val circleColor = Nemo.colors.background // Putih di Day, Hitam di Night

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Nemo.colors.fog) // Latar awal sebelum burst
    ) {
        // LINGKARAN PUTIH YANG MEMBESAR MEMENUHI LAYAR
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val maxScale = 10f // Memenuhi seluruh layar ponsel
                    val s = 1f + (maxScale - 1f) * expandProgress
                    scaleX = s
                    scaleY = s
                    clip = true
                    shape = CircleShape
                }
                .size(140.dp)
                .background(circleColor, CircleShape)
        )

        // MASKOT & TYPOGRAPHY DI DEPAN LINGKARAN
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = (1f - expandProgress * 2f).coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NemoMascot(
                expression = if (isWinking) MascotExpression.WINK else MascotExpression.NEUTRAL,
                size = 120.dp
            )
            Text(
                text = "Nemo",
                style = NemoTypography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}`,

    'anticipation-burst': `// =========================================================================
// TRANSISI IRIS EXPAND: INHALE & BURST
// =========================================================================
// Lingkaran sedikit mengecil (inhale) sebelum meledak memenuhi layar

val irisScale = remember { Animatable(1f) }

LaunchedEffect(Unit) {
    // 1. Tunggu animasi maskot selesai
    delay(2400L)

    // 2. Inhale (Anticipation dip ke 0.94f)
    irisScale.animateTo(0.94f, tween(120, easing = LinearEasing))

    // 3. Burst Expand (Meledak ke 10f)
    irisScale.animateTo(10f, tween(650, easing = FastOutSlowInEasing))
    onFinished()
}`,

    'mascot-fly': `// =========================================================================
// TRANSISI IRIS EXPAND + MASCOT HANDOFF (FLY TO HEADER)
// =========================================================================
// Lingkaran membesar, maskot mengecil dan melayang mulus ke avatar dashboard

val handoff = remember { Animatable(0f) }

// Maskot terbang dari center (x=0, y=0) ke top-left target
Modifier.graphicsLayer {
    val h = handoff.value
    translationX = deltaX * h
    translationY = deltaY * h
    val s = 1f + (targetScale - 1f) * h
    scaleX = s
    scaleY = s
}`
  };

  function updateCodeBlock() {
    elCodeBlock.textContent = composeSnippets[state.preset] || composeSnippets['smooth-iris'];
  }

  // --- Theme Toggle ---
  function setTheme(newTheme) {
    state.theme = newTheme;
    document.documentElement.setAttribute('data-app-theme', newTheme);

    const themeLabel = document.querySelector('.theme-label');
    if (themeLabel) {
      themeLabel.textContent = newTheme === 'night' ? 'Night Mode' : 'Day Mode';
    }

    const sunIcon = document.querySelector('.theme-icon.sun');
    const moonIcon = document.querySelector('.theme-icon.moon');
    if (sunIcon && moonIcon) {
      sunIcon.style.display = newTheme === 'day' ? 'inline' : 'none';
      moonIcon.style.display = newTheme === 'night' ? 'inline' : 'none';
    }
  }

  // --- Setup Event Listeners ---
  function initListeners() {
    // Play Buttons
    document.getElementById('btnPlaySequence').addEventListener('click', playFullSequence);
    document.getElementById('btnInstantBurst').addEventListener('click', playCircleBurstOnly);
    document.getElementById('btnResetSequence').addEventListener('click', resetToBeginning);

    // Scrubber
    elTimelineScrubber.addEventListener('input', (e) => {
      stopSequence();
      const p = parseInt(e.target.value, 10) / 100;
      renderTimelineProgress(p);
    });

    // Preset Cards
    document.querySelectorAll('.preset-card').forEach((card) => {
      card.addEventListener('click', () => {
        document.querySelectorAll('.preset-card').forEach((c) => c.classList.remove('active'));
        card.classList.add('active');
        state.preset = card.dataset.preset;
        updateCodeBlock();
        playFullSequence();
      });
    });

    // Duration Sliders
    elSliderDuration.addEventListener('input', (e) => {
      state.duration = parseInt(e.target.value, 10);
      elValDuration.textContent = `${state.duration}ms`;
    });

    elSliderMascotDuration.addEventListener('input', (e) => {
      state.mascotDuration = parseInt(e.target.value, 10);
      elValMascotDuration.textContent = `${(state.mascotDuration / 1000).toFixed(1)}s`;
    });

    // Theme Toggle
    document.getElementById('btnThemeToggle').addEventListener('click', () => {
      setTheme(state.theme === 'night' ? 'day' : 'night');
    });

    // Audio Toggle
    document.getElementById('btnAudioToggle').addEventListener('click', () => {
      state.audioEnabled = !state.audioEnabled;
      const btn = document.getElementById('btnAudioToggle');
      const text = btn.querySelector('.btn-text');
      btn.classList.toggle('active', state.audioEnabled);
      text.textContent = state.audioEnabled ? 'Chime On' : 'Chime Off';
      if (state.audioEnabled) {
        initAudio();
        playZenChime();
      }
    });

    // Viewport Toggle
    document.getElementById('btnToggleViewport').addEventListener('click', () => {
      state.viewportMode = state.viewportMode === 'phone' ? 'full' : 'phone';
      elPhoneWrapper.classList.toggle('full-viewport', state.viewportMode === 'full');
      const btnText = document.querySelector('#btnToggleViewport .btn-text');
      if (btnText) {
        btnText.textContent = state.viewportMode === 'phone' ? 'Phone Frame' : 'Full Stage';
      }
    });

    // Copy Code Button
    elCopyBtn.addEventListener('click', () => {
      navigator.clipboard.writeText(elCodeBlock.textContent).then(() => {
        elCopyLabel.textContent = 'Tersalin!';
        elCopyBtn.style.borderColor = '#4ade80';
        elCopyBtn.style.color = '#4ade80';

        setTimeout(() => {
          elCopyLabel.textContent = 'Salin Kode';
          elCopyBtn.style.borderColor = '';
          elCopyBtn.style.color = '';
        }, 2000);
      });
    });

    // Digital Status Clock
    setInterval(() => {
      const d = new Date();
      const h = String(d.getHours()).padStart(2, '0');
      const m = String(d.getMinutes()).padStart(2, '0');
      const clockEl = document.getElementById('statusClock');
      if (clockEl) clockEl.textContent = `${h}:${m}`;
    }, 1000);
  }

  // --- Bootstrap Studio ---
  function init() {
    setTheme('night');
    initListeners();
    updateCodeBlock();
    renderTimelineProgress(0);

    // Auto-play preview after 600ms on first load so user immediately sees it in action
    setTimeout(() => {
      playFullSequence();
    }, 600);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

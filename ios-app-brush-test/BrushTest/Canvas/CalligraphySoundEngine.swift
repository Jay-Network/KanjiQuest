import AVFoundation

/// Procedural brush-on-paper sound engine using AVAudioEngine.
///
/// Generates filtered noise modulated by brush velocity and pressure to simulate
/// the sound of bristles dragging across 半紙 (hanshi) paper.
///
/// Three states:
/// - **Contact**: initial impact (brief transient)
/// - **Drag**: continuous paper-grain noise (velocity → volume, pressure → filter)
/// - **Lift**: fade-out on brush release
final class CalligraphySoundEngine {

    /// When true, no sound is produced
    var isMuted = false

    // MARK: - Audio Engine

    private var engine: AVAudioEngine?
    private var sourceNode: AVAudioSourceNode?

    // MARK: - Audio State (accessed from real-time audio thread)

    // Using simple properties without locks — on ARM64, aligned load/store of
    // Float/Bool is naturally atomic. Acceptable for a brush test app.
    private var isActive = false
    private var targetVolume: Float = 0
    private var currentVolume: Float = 0

    /// Low-pass filter coefficient (0 = no filtering, 1 = full filtering).
    /// Higher pressure → higher coefficient → muffled sound (brush pressed into paper).
    private var filterCoefficient: Float = 0.3

    /// Previous filtered sample for one-pole IIR filter
    private var lastFilteredSample: Float = 0

    /// Contact transient: brief volume spike on touch-down
    private var contactTransientSamples: Int = 0

    /// PRNG state for allocation-free noise generation on audio thread
    private var rngState: UInt32 = 48271

    // MARK: - Constants

    private let sampleRate: Double = 44100
    private let masterVolume: Float = 0.12
    private let fadeRate: Float = 1.0 / 4410  // ~100ms volume ramp

    // MARK: - Init

    init() {
        setupEngine()
    }

    deinit {
        engine?.stop()
    }

    // MARK: - Setup

    private func setupEngine() {
        let engine = AVAudioEngine()
        guard let format = AVAudioFormat(
            standardFormatWithSampleRate: sampleRate,
            channels: 1
        ) else { return }

        let sourceNode = AVAudioSourceNode(format: format) { [weak self] _, _, frameCount, bufferList -> OSStatus in
            guard let self = self else { return noErr }

            let ablPointer = UnsafeMutableAudioBufferListPointer(bufferList)
            guard let samples = ablPointer[0].mData?.assumingMemoryBound(to: Float.self) else {
                return noErr
            }

            for i in 0..<Int(frameCount) {
                // Volume ramping
                if self.isActive && !self.isMuted {
                    let target = self.targetVolume + (self.contactTransientSamples > 0 ? 0.3 : 0)
                    self.currentVolume = min(target, self.currentVolume + self.fadeRate)
                    if self.contactTransientSamples > 0 {
                        self.contactTransientSamples -= 1
                    }
                } else {
                    self.currentVolume = max(0, self.currentVolume - self.fadeRate)
                }

                // Allocation-free noise (xorshift32)
                self.rngState ^= self.rngState << 13
                self.rngState ^= self.rngState >> 17
                self.rngState ^= self.rngState << 5
                let noise = Float(Int32(bitPattern: self.rngState)) / Float(Int32.max)

                // One-pole low-pass filter: y[n] = (1-a)*x[n] + a*y[n-1]
                let coeff = self.filterCoefficient
                let filtered = (1.0 - coeff) * noise + coeff * self.lastFilteredSample
                self.lastFilteredSample = filtered

                samples[i] = filtered * self.currentVolume * self.masterVolume
            }

            return noErr
        }

        engine.attach(sourceNode)
        engine.connect(sourceNode, to: engine.mainMixerNode, format: format)

        self.engine = engine
        self.sourceNode = sourceNode
    }

    // MARK: - Control

    /// Called when the brush makes contact with the paper.
    func startContact(pressure: CGFloat) {
        guard !isMuted else { return }

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, options: .mixWithOthers)
            try AVAudioSession.sharedInstance().setActive(true)
            try engine?.start()
        } catch {
            return
        }

        targetVolume = Float(0.2 + pressure * 0.4)
        filterCoefficient = Float(0.4 + pressure * 0.3)
        contactTransientSamples = Int(sampleRate * 0.03) // 30ms transient
        isActive = true
    }

    /// Called continuously during brush drag.
    /// - Parameters:
    ///   - velocity: Current stroke velocity in points/sec
    ///   - pressure: Current normalized pressure (0–1)
    func updateDrag(velocity: CGFloat, pressure: CGFloat) {
        guard isActive else { return }

        let velocityNorm = Float(min(1.0, velocity / 2000.0))
        targetVolume = velocityNorm * 0.5 + Float(pressure) * 0.3
        // Higher pressure → higher filter coefficient → more muffled (brush pressed flat)
        filterCoefficient = 0.3 + Float(pressure) * 0.4
    }

    /// Called when the brush lifts off the paper.
    func endContact() {
        isActive = false
        // Engine keeps running briefly for the fade-out ramp, then stops
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            guard let self = self else { return }
            if !self.isActive {
                self.engine?.stop()
            }
        }
    }
}

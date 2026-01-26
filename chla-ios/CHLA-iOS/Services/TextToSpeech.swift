//
//  TextToSpeech.swift
//  CHLA-iOS
//
//  Text-to-speech service using AVSpeechSynthesizer
//

import Foundation
import AVFoundation

@MainActor
class TextToSpeech: ObservableObject {
    @Published var isSpeaking: Bool = false
    @Published var isPaused: Bool = false
    
    private let synthesizer = AVSpeechSynthesizer()
    private var delegate: SpeechDelegate?
    
    init() {
        delegate = SpeechDelegate(parent: self)
        synthesizer.delegate = delegate
        configureAudioSession()
    }
    
    private func configureAudioSession() {
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers, .allowBluetooth])
        } catch {
            print("Failed to configure audio session for TTS: \(error)")
        }
    }
    
    func speak(_ text: String, language: String? = nil) {
        // Stop any current speech
        stop()
        
        // Clean the text - remove markdown symbols
        let cleanText = cleanMarkdown(text)
        
        guard !cleanText.isEmpty else { return }
        
        // Create utterance
        let utterance = AVSpeechUtterance(string: cleanText)
        
        // Configure voice based on language or device locale
        let languageCode = language ?? Locale.current.language.languageCode?.identifier ?? "en"
        if let voice = AVSpeechSynthesisVoice(language: languageCode) {
            utterance.voice = voice
        } else {
            // Fallback to default voice
            utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        }
        
        // Configure speech parameters
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9  // Slightly slower for clarity
        utterance.pitchMultiplier = 1.0
        utterance.volume = 1.0
        
        // Activate audio session and speak
        do {
            try AVAudioSession.sharedInstance().setActive(true)
            synthesizer.speak(utterance)
            isSpeaking = true
            isPaused = false
        } catch {
            print("Failed to activate audio session: \(error)")
        }
    }
    
    func pause() {
        if isSpeaking && !isPaused {
            synthesizer.pauseSpeaking(at: .word)
            isPaused = true
        }
    }
    
    func resume() {
        if isPaused {
            synthesizer.continueSpeaking()
            isPaused = false
        }
    }
    
    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
        isSpeaking = false
        isPaused = false
        
        // Deactivate audio session
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("Failed to deactivate audio session: \(error)")
        }
    }
    
    func togglePause() {
        if isPaused {
            resume()
        } else {
            pause()
        }
    }
    
    // Clean markdown formatting from text
    private func cleanMarkdown(_ text: String) -> String {
        var cleaned = text
        
        // Remove bold/italic markers
        cleaned = cleaned.replacingOccurrences(of: "**", with: "")
        cleaned = cleaned.replacingOccurrences(of: "__", with: "")
        cleaned = cleaned.replacingOccurrences(of: "*", with: "")
        cleaned = cleaned.replacingOccurrences(of: "_", with: "")
        
        // Remove bullet points and list markers
        cleaned = cleaned.replacingOccurrences(of: "• ", with: "")
        cleaned = cleaned.replacingOccurrences(of: "- ", with: "")
        
        // Remove header markers
        let headerPattern = "^#{1,6}\\s+"
        if let regex = try? NSRegularExpression(pattern: headerPattern, options: .anchorsMatchLines) {
            cleaned = regex.stringByReplacingMatches(in: cleaned, range: NSRange(cleaned.startIndex..., in: cleaned), withTemplate: "")
        }
        
        // Remove code blocks
        cleaned = cleaned.replacingOccurrences(of: "```", with: "")
        cleaned = cleaned.replacingOccurrences(of: "`", with: "")
        
        // Clean up extra whitespace
        cleaned = cleaned.replacingOccurrences(of: "\n\n+", with: ". ", options: .regularExpression)
        cleaned = cleaned.replacingOccurrences(of: "\n", with: " ")
        cleaned = cleaned.replacingOccurrences(of: "  +", with: " ", options: .regularExpression)
        
        return cleaned.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    
    // Delegate to track speech state
    private class SpeechDelegate: NSObject, AVSpeechSynthesizerDelegate {
        weak var parent: TextToSpeech?
        
        init(parent: TextToSpeech) {
            self.parent = parent
        }
        
        func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
            Task { @MainActor in
                self.parent?.isSpeaking = false
                self.parent?.isPaused = false
            }
        }
        
        func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
            Task { @MainActor in
                self.parent?.isSpeaking = false
                self.parent?.isPaused = false
            }
        }
    }
}

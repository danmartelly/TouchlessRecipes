package tts;

import com.sun.speech.freetts.*;

public class TTS {
	protected VoiceManager voiceManager;
	protected Voice voice = null;
	
	public TTS() {
		voiceManager = VoiceManager.getInstance();
		voice = voiceManager.getVoice("kevin16");
		voice.allocate();
	}
	
	/**
	 * Blocking function
	 * @param str thing to say in English
	 */
	public void say(String str) {
		voice.speak(str);
	}
	
	// not thread safe. Seems like a dumb way to do it but the
	// only way I could find at the moment.
	// could be a cause of problems
	public void stop() {
		voice.deallocate();
		voice = voiceManager.getVoice("kevin16");
		voice.allocate();
	}
	
	public boolean close() {
		if (voice != null) {
			voice.deallocate();
		}
		return true;
	}
}

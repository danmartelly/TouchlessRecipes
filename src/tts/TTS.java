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
	
	public void say(String str) {
		voice.speak(str);
	}
	
	public boolean close() {
		if (voice != null) {
			voice.deallocate();
		}
		return true;
	}
}

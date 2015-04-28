package tts;

import com.sun.speech.freetts.*;

public class TTS {
	
	public static class Sayer implements Runnable {
		protected Voice voice;
		protected String toSay;
		
		public Sayer(Voice voice, String toSay) {
			this.voice = voice;
			this.toSay = toSay;
		}
		
		@Override
		public void run() {
			voice.speak(toSay);
		}
		
	}
	
	protected VoiceManager voiceManager;
	protected Voice voice = null;
	protected Thread speakingThread;
	
	public TTS() {
		voiceManager = VoiceManager.getInstance();
		voice = voiceManager.getVoice("kevin16");
		voice.allocate();
		speakingThread = null;
	}
	
	/**
	 * Blocking function
	 * @param str thing to say in English
	 */
	public void say(String str) {
		if (speakingThread != null && speakingThread.isAlive()) {
			stop();
		}
		speakingThread = new Thread(new Sayer(voice, str));
		speakingThread.start();
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

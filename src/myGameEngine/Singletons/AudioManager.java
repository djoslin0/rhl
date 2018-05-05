package myGameEngine.Singletons;

import myGameEngine.Helpers.SoundGroup;
import ray.audio.AudioManagerFactory;
import ray.audio.AudioResource;
import ray.audio.AudioResourceType;
import ray.audio.IAudioManager;
import ray.rage.scene.SceneNode;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class AudioManager /*implements Updatable*/ {
    private static final AudioManager instance = new AudioManager();
    private IAudioManager audio;
    private SceneNode ear;
    private HashMap<String, AudioResource> resources = new HashMap<>();
    private HashMap<String, Float> resourceLengths = new HashMap<>();

    public SoundGroup impact;
    public SoundGroup miss;
    public SoundGroup punch;

    public AudioManager() {
        audio = AudioManagerFactory.createAudioManager("ray.audio.joal.JOALAudioManager");
        if (!audio.initialize()) {
            System.out.println("Audio Manager failed to initialize!");
        }
        //UpdateManager.add(this);
    }

    public static void initialize() {
        instance.impact = new SoundGroup(instance.audio,
                new String[] { "impact1.wav", "impact2.wav", "impact3.wav", "impact4.wav" },
                100, false, 300, 5f
        );

        instance.miss = new SoundGroup(instance.audio,
                new String[] { "miss1.wav", "miss2.wav", "miss3.wav", "miss4.wav" },
                100, false, 300, 5f
        );

        instance.punch = new SoundGroup(instance.audio,
                new String[] { "punch1.wav", "punch2.wav", "punch3.wav", "punch4.wav" },
                100, false, 300, 5f
        );
    }

    public static AudioManager get() { return instance; }
    public static void setEar(SceneNode ear) { instance.ear = ear; }
    public static SceneNode getEar() { return instance.ear; }

    public static AudioResource getResource(String fileName) {
        if (!instance.resources.containsKey(fileName)) {
            AudioResource resource = instance.audio.createAudioResource("assets/sound/" + fileName, AudioResourceType.AUDIO_SAMPLE);
            instance.resources.put(fileName, resource);
            instance.resourceLengths.put(fileName, getWavLength(fileName));
        }
        return instance.resources.get(fileName);
    }

    public static Float getResourceLength(String fileName) {
        return instance.resourceLengths.get(fileName);
    }

    private static float getWavLength(String fileName) {
        File file = new File("./assets/sound/" + fileName);
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioInputStream.getFormat();
            long audioFileLength = file.length();
            int frameSize = format.getFrameSize();
            float frameRate = format.getFrameRate();
            audioInputStream.close();
            return (audioFileLength / (frameSize * frameRate));
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /*
    @Override
    public void update(float delta) {
        System.out.println(audio.getEar().getLocation());
        impact.setLocation(Vector3f.createFrom(100, 0, 0));
        audio.getEar().setLocation(ear.getWorldPosition());
        audio.getEar().setOrientation(ear.getWorldForwardAxis(), Vector3f.createUnitVectorY());
        audio.getEar().setVelocity(Vector3f.createZeroVector());
    }
    @Override
    public boolean blockUpdates() { return false; }
    */

}

package myGameEngine.Singletons;

import myGameEngine.Helpers.SoundGroup;
import myGameEngine.Helpers.Updatable;
import ray.audio.*;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3f;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class AudioManager implements Updatable {
    private static final AudioManager instance = new AudioManager();
    private IAudioManager audio;
    private SceneNode ear;
    private HashMap<String, AudioResource> resources = new HashMap<>();
    private HashMap<String, Float> resourceLengths = new HashMap<>();

    public SoundGroup timer;
    public SoundGroup start;
    public SoundGroup impact;
    public SoundGroup miss;
    public SoundGroup punch;
    public SoundGroup step;
    public SoundGroup nearDeath;
    public SoundGroup death;
    public SoundGroup respawn;
    public SoundGroup glitch;
    public SoundGroup explosion;
    public SoundGroup goalWon;
    public SoundGroup goalLost;
    public SoundGroup cheer;
    public SoundGroup jump;
    public SoundGroup land;
    public SoundGroup slide;
    public SoundGroup spin;
    public SoundGroup ice;
    public SoundGroup rink;
    public SoundGroup matchOver;

    public AudioManager() {
        audio = AudioManagerFactory.createAudioManager("ray.audio.joal.JOALAudioManager");
        if (!audio.initialize()) {
            System.out.println("Audio Manager failed to initialize!");
        }
    }

    public static void initialize() {
        instance.start = new SoundGroup(instance.audio,
                new String[] {"start.wav"},50,false,0,0f
        );

        instance.timer = new SoundGroup(instance.audio,
                new String[] {"timer.wav"},50,false,0,0f
        );

        instance.impact = new SoundGroup(instance.audio,
                new String[] { "impact1.wav", "impact2.wav", "impact3.wav", "impact4.wav" },
                100, false, 300, 0.1f
        );

        instance.miss = new SoundGroup(instance.audio,
                new String[] { "miss1.wav", "miss2.wav", "miss3.wav", "miss4.wav" },
                100, false, 300, 0.1f
        );

        instance.punch = new SoundGroup(instance.audio,
                new String[] { "punch1.wav", "punch2.wav", "punch3.wav", "punch4.wav" },
                100, false, 300, 0.1f
        );

        instance.step = new SoundGroup(instance.audio,
                new String[] { "step1.wav", "step2.wav", "step3.wav", "step4.wav" },
                60, false, 200, 0.1f
        );

        instance.death = new SoundGroup(instance.audio,
                new String[] { "death.wav" },
                80, false, 300, 0.1f
        );

        instance.nearDeath = new SoundGroup(instance.audio,
                new String[] { "neardeath.wav" },
                100, false, 200, 0.1f
        );

        instance.respawn = new SoundGroup(instance.audio,
                new String[] { "respawn.wav" },
                100, false, 100, 0.1f
        );

        instance.glitch = new SoundGroup(instance.audio,
                new String[] { "glitch1.wav", "glitch2.wav", "glitch3.wav", "glitch4.wav" },
                100, false, 200, 0.1f
        );

        instance.explosion = new SoundGroup(instance.audio,
                new String[] { "explosion1.wav", "explosion2.wav" },
                100, false, 500, 0.1f
        );

        instance.goalWon = new SoundGroup(instance.audio,
                new String[] { "goalwon.wav" },
                75, false, 200, 0.1f
        );

        instance.goalLost = new SoundGroup(instance.audio,
                new String[] { "goallost.wav" },
                75, false, 200, 0.1f
        );

        instance.cheer = new SoundGroup(instance.audio,
                new String[] { "cheer1.wav", "cheer2.wav" },
                75, false, 200, 0.1f
        );

        instance.jump = new SoundGroup(instance.audio,
                new String[] { "jump1.wav", "jump2.wav" },
                30, false, 200, 0.1f
        );

        instance.land = new SoundGroup(instance.audio,
                new String[] { "land1.wav", "land2.wav" },
                50, false, 200, 0.1f
        );

        instance.slide = new SoundGroup(instance.audio,
                new String[] { "slide.wav" },
                0, true, 200, 0.15f
        );

        instance.spin = new SoundGroup(instance.audio,
                new String[] { "spin.wav" },
                0, true, 200, 0.1f
        );

        instance.ice = new SoundGroup(instance.audio,
                new String[] { "ice1.wav", "ice2.wav", "ice3.wav" },
                50, false, 200, 0.1f
        );

        instance.rink = new SoundGroup(instance.audio,
                new String[] { "rink1.wav", "rink2.wav", "rink3.wav" },
                50, false, 200, 0.1f
        );

        instance.matchOver = new SoundGroup(instance.audio,
                new String[] { "matchover.wav" },
                75, false, 200, 0.1f
        );
        UpdateManager.add(instance);
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

    @Override
    public void update(float delta) {
        audio.getEar().setLocation(ear.getWorldPosition());
        audio.getEar().setOrientation(ear.getWorldForwardAxis(), Vector3f.createUnitVectorY());
    }

    @Override
    public boolean blockUpdates() { return false; }

}

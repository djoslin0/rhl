package myGameEngine.Helpers;
import myGameEngine.Singletons.AudioManager;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.UpdateManager;
import ray.audio.*;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

public class SoundGroup implements Updatable {
    private IAudioManager audioMgr;
    private String[] soundFiles;
    private AudioResource[] resources;
    private Float[] lengths;
    private Sound[] sounds;
    private int index = 0;
    private int volume;
    private boolean looping;
    private float maxDistance;
    private float rollOff;
    private SceneNode node;
    private float pitch = 1;

    private Sound sound;

    public SoundGroup(IAudioManager audioMgr, String[] soundFiles, int volume, boolean looping, float maxDistance, float rollOff) {
        this.audioMgr = audioMgr;
        this.soundFiles = soundFiles;
        this.volume = volume;
        this.looping = looping;
        this.maxDistance = maxDistance;
        this.rollOff = rollOff;

        this.sounds = new Sound[soundFiles.length];
        this.resources = new AudioResource[soundFiles.length];
        this.lengths = new Float[soundFiles.length];
        for (int i = 0; i < soundFiles.length; i++) {
            resources[i] = AudioManager.getResource(soundFiles[i]);
            lengths[i] = AudioManager.getResourceLength(soundFiles[i]);
            sounds[i] = new Sound(resources[i], SoundType.SOUND_EFFECT, volume, looping);
            sounds[i].initialize(audioMgr);
            try {
                sound.setMaxDistance(maxDistance);
                sound.setRollOff(rollOff);
            } catch (Exception ex) {}
        }
    }

    public SoundGroup(IAudioManager audioMgr, String[] soundFiles, int volume, boolean looping, float maxDistance, float rollOff, SceneNode node) {
        this(audioMgr, soundFiles, volume, looping, maxDistance, rollOff);
        this.node = node;
    }

    public void setVolume(int volume) {
        this.volume = volume;
        if (sound != null) { sound.setVolume(volume); }
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (sound != null) {
            try {
                sound.setPitch(pitch);
            } catch (Exception ex) {}
        }
    }

    public void play(int volume) {
        UpdateManager.remove(this);

        if (sound != null) { sound.stop(); }
        if (node == null) { node = EntityManager.getLocalPlayer().getCameraNode(); }

        int increment = (int)(Math.random() * (sounds.length - 1) * 0.6f) + 1;
        index = (index + increment) % sounds.length;

        sound = sounds[index];

        pitch = 1 + (float)Math.random() * 0.2f - 0.1f;

        setVolume(volume);

        // check initialization
        if (sound.getVolume() != volume) {
            if (sound == null) {
                sounds[index] = new Sound(resources[index], SoundType.SOUND_EFFECT, volume, looping);
                sound = sounds[index];
            }
            sound.initialize(audioMgr);
            try {
                sound.setMaxDistance(maxDistance);
                sound.setRollOff(rollOff);
            } catch (Exception ex) {}
        }

        setPitch(pitch);
        sound.setLocation(node.getWorldPosition());
        sound.play();

        UpdateManager.add(this);
    }

    public void play() {
        play(this.volume);
    }

    public void destroy() {
        for (Sound sound : sounds) {
            try { sound.release(audioMgr); } catch (Exception ex) {}
        }
    }

    public SoundGroup clone(SceneNode node) {
        return new SoundGroup(audioMgr, soundFiles, volume, looping, maxDistance, rollOff, node);
    }

    public void stop() {
        if (sound != null) { sound.stop(); }
        UpdateManager.remove(this);
    }

    @Override
    public void update(float delta) {
        if (sound == null) { return; }
        sound.setLocation(node.getWorldPosition());

        if (sound.getProgress() >= lengths[index] * 0.8) {
            stop();
            if (looping) {
                play();
            }
        }
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}

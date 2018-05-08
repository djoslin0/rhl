package myGameEngine.Helpers;
import myGameEngine.Singletons.AudioManager;
import myGameEngine.Singletons.UpdateManager;
import ray.audio.AudioResource;
import ray.audio.IAudioManager;
import ray.audio.Sound;
import ray.audio.SoundType;
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
    private float pitch;

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
            /*sounds[i].setMaxDistance(maxDistance);
            sounds[i].setRollOff(rollOff);
            sounds[i].setLocation(Vector3f.createFrom(100f, 0, 0));
            sounds[i].setVelocity(Vector3f.createZeroVector());
            sounds[index].setEmitDirection(Vector3f.createUnitVectorZ(), 1f);*/
        }
    }

    public SoundGroup(IAudioManager audioMgr, String[] soundFiles, int volume, boolean looping, float maxDistance, float rollOff, SceneNode node) {
        this(audioMgr, soundFiles, volume, looping, maxDistance, rollOff);
        this.node = node;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        sounds[index].setPitch(pitch);
    }

    public void play(int volume) {
        UpdateManager.remove(this);
        sounds[index].stop();
        int increment = (int)(Math.random() * (sounds.length - 1) * 0.6f) + 1;
        index = (index + increment) % sounds.length;

        this.volume = volume;
        updateVolume();
        pitch = 1 + (float)Math.random() * 0.2f - 0.1f;
        try { sounds[index].setPitch(pitch); } catch (NullPointerException ex) {}
        sounds[index].play();

        UpdateManager.add(this);
    }

    private void updateVolume() {
        float dist = 1;
        if (node != null && AudioManager.getEar() != null) {
            dist = (float)Math.pow(1 - node.getWorldPosition().sub(AudioManager.getEar().getWorldPosition()).length() / maxDistance, rollOff);
        }
        if (dist < 0) { dist = 0; }
        if (dist > 1) { dist = 1; }
        int vol = (int)(volume * dist);
        sounds[index].setVolume(vol);
    }

    public void play() {
        play(this.volume);
    }

    public void setLocation(Vector3 location) {
        for (Sound sound : sounds) {
            sound.setLocation(location);
        }
    }

    public void destroy() {
        for (Sound sound : sounds) {
            sound.release(audioMgr);
        }
    }

    public SoundGroup clone(SceneNode node) {
        return new SoundGroup(audioMgr, soundFiles, volume, looping, maxDistance, rollOff, node);
    }

    public float getProgress() {
        return sounds[index].getProgress();
    }

    public boolean getIsPlaying() {
        return sounds[index].getIsPlaying();
    }

    public void stop() {
        sounds[index].stop();
        UpdateManager.remove(this);
    }

    @Override
    public void update(float delta) {
        updateVolume();
        if (sounds[index].getProgress() >= lengths[index] * 0.8) {
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

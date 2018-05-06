package myGameEngine.Helpers;

import a2.Actions.ActionMove;
import com.jogamp.openal.AL;
import myGameEngine.Singletons.AudioManager;
import myGameEngine.Singletons.EntityManager;
import myGameEngine.Singletons.UpdateManager;
import ray.audio.*;
import ray.audio.joal.JOALAudioManager;
import ray.audio.joal.JOALAudioPlayer;
import ray.audio.joal.JOALAudioResource;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

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
        sounds[index].setPitch(pitch);
    }

    public void play(int volume) {
        UpdateManager.remove(this);
        sounds[index].stop();
        int increment = (int)(Math.random() * (sounds.length - 1) * 0.6f) + 1;
        index = (index + increment) % sounds.length;

        float dist = 1;
        if (node != null) {
            dist = (float)Math.pow(1 - node.getWorldPosition().sub(AudioManager.getEar().getWorldPosition()).length() / maxDistance, rollOff);
        }
        if (dist < 0) { dist = 0; }
        if (dist > 1) { dist = 1; }
        int vol = (int)(volume * dist);
        sounds[index].setVolume(vol);
        sounds[index].setPitch(1 + (float)Math.random() * 0.2f - 0.1f);
        sounds[index].play();

        if (!looping) {
            UpdateManager.add(this);
        }
        System.out.println(soundFiles[index]);
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

    public void stop() {
        sounds[index].stop();
        UpdateManager.remove(this);
    }

    @Override
    public void update(float delta) {
        if (sounds[index].getProgress() >= lengths[index] * 0.9) {
            stop();
        }
    }

    @Override
    public boolean blockUpdates() {
        return false;
    }
}

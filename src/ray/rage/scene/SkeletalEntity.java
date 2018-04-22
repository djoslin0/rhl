//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ray.rage.scene;

import java.io.IOException;

public interface SkeletalEntity extends Entity {
    void update(float delta); /* MyChange: added delta for framerate independence */

    void loadAnimation(String animName, String animationPath) throws IOException;

    void playAnimation(String animName, float animSpeed, EndType endType, int endTypeCount);

    void pauseAnimation();

    void stopAnimation();

    public static enum EndType {
        NONE,
        STOP,
        PAUSE,
        LOOP,
        PINGPONG;

        private EndType() {
        }
    }
}

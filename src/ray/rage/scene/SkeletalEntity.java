//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ray.rage.scene;

import ray.rml.Quaternion;
import ray.rml.Vector3;

import java.io.IOException;

public interface SkeletalEntity extends Entity {
    void update(float delta); /* MyChange: added delta for framerate independence */

    void loadAnimation(String animName, String animationPath) throws IOException;

    void playAnimation(String animName, float animSpeed, EndType endType, int endTypeCount, boolean interruptable); /* MyChange: added interruptable */

    void pauseAnimation();

    void stopAnimation();

    void addScaleOverride(String boneName, Vector3 scale); /* MyChange: added override */
    void removeScaleOverride(String boneName); /* MyChange: added override */

    void addRotationOverride(String boneName, Quaternion rotation); /* MyChange: added override */
    void removeRotationOverride(String boneName); /* MyChange: added override */

    void addRotationAdditive(String boneName, Quaternion rotation); /* MyChange: added override */
    void removeRotationAdditive(String boneName); /* MyChange: added override */

    void addLocationOverride(String boneName, Vector3 location); /* MyChange: added override */
    void removeLocationOverride(String boneName); /* MyChange: added override */

    void addLocationAdditive(String boneName, Vector3 location); /* MyChange: added override */
    void removeLocationAdditive(String boneName); /* MyChange: added override */

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

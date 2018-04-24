//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ray.rage.scene.generic;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import myGameEngine.Helpers.MathHelper;
import ray.rage.asset.animation.Animation;
import ray.rage.asset.material.Material;
import ray.rage.asset.mesh.Mesh;
import ray.rage.asset.mesh.SubMesh;
import ray.rage.asset.skeleton.Skeleton;
import ray.rage.rendersystem.Renderable.Primitive;
import ray.rage.rendersystem.shader.GpuShaderProgram;
import ray.rage.rendersystem.states.RenderState;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SkeletalEntity;
import ray.rage.scene.SubEntity;
import ray.rage.scene.SkeletalEntity.EndType;
import ray.rage.scene.SubEntity.Visitor;
import ray.rml.*;

final class GenericSkeletalEntity extends AbstractGenericSceneObject implements SkeletalEntity {

    private class AnimationState {  /* MyChange: added anim states */
        private Animation animation = null;
        private int frame = -1;
        private int nextFrame = -1; /* MyChange: added for lerping */
        private float lerpScale = 0f; /* MyChange: added for lerping */
        private float lerpedFrame = -1.0F;
        private float speed = 1.0F;
        private EndType endtype = EndType.NONE;
        private int endTypeTotal = -1;
        private int endTypeCount = 0;
        private boolean paused = false;
        public boolean stopped = false;
        public float animationLerp = 0;
        public float animationLerpSpeed = 0.0065f;
        public boolean interruptable = true;

        public Vector3 getBoneScale(int i) {
            return animation.getFrameBoneScl(frame, i).lerp(animation.getFrameBoneScl(nextFrame, i), this.lerpScale); /* MyChange: lerping */
        }

        public Quaternion getBoneRotation(int i) {
            return MathHelper.slerp(animation.getFrameBoneRot(frame, i), animation.getFrameBoneRot(nextFrame, i), this.lerpScale); /* MyChange: lerping */
        }

        public Vector3 getBoneLocation(int i) {
            return animation.getFrameBoneLoc(frame, i).lerp(animation.getFrameBoneLoc(nextFrame, i), this.lerpScale); /* MyChange: lerping */
        }

        public void update(float delta, boolean hasWaiting) {
            animationLerp += animationLerpSpeed * delta * (hasWaiting ? 1.2f : 1f);
            if (animationLerp < 0) { animationLerp = 0; }
            if (animationLerp > 1) { animationLerp = 1; }
            if (animation != null && !paused && speed != 0.0F) {
                lerpedFrame += speed * delta;
                lerpScale = lerpedFrame - (int)lerpedFrame;  /* MyChange: added for lerping */
                if (speed < 0) { lerpScale = 1f - lerpScale; }
                if (lerpScale < 0) { lerpScale = 0; }
                if (lerpScale > 1) { lerpScale = 1; }
                frame = (int)(lerpedFrame); /* MyChange: changed round function */
                if (frame >= animation.getFrameCount() || frame < 0) {
                    handleAnimationEnd();
                }
                nextFrame = calculateNextFrame(); /* MyChange: added for lerping */
            }
        }

        private int calculateNextFrame() { /* MyChange: added for lerping */
            if (speed == 0) { return frame; }
            int nextFrame = frame + (speed > 0 ? 1 : -1);
            switch(endtype) {
                case NONE:
                case STOP:
                case PAUSE:
                    if (nextFrame >= animation.getFrameCount()) { nextFrame = frame; }
                    if (nextFrame < 0) { nextFrame = 0; }
                    break;
                case LOOP:
                    if (nextFrame >= animation.getFrameCount()) { nextFrame -= animation.getFrameCount(); }
                    if (nextFrame < 0) { nextFrame += animation.getFrameCount(); }
                    break;
                case PINGPONG:
                    if (nextFrame >= animation.getFrameCount()) { nextFrame -= 2; }
                    if (nextFrame < 0) { nextFrame += 2; }
                    break;
            }

            return nextFrame;
        }

        private void handleAnimationEnd() {
            ++endTypeCount;
            if (endTypeTotal != 0 && endTypeCount > endTypeTotal) {
                stopped = true;
            } else {
                switch(endtype) {
                    case NONE:
                    case STOP:
                        if (speed > 0.0D) {
                            frame = animation.getFrameCount() - 1;
                            lerpedFrame = (float)(animation.getFrameCount() - 1);
                        } else if (speed < 0.0D) {
                            frame = 0;
                            lerpedFrame = 0.0F;
                        }
                        stopped = true;
                        break;
                    case PAUSE:
                        if (speed > 0.0D) {
                            frame = animation.getFrameCount() - 1;
                            lerpedFrame = (float)(animation.getFrameCount() - 1);
                        } else if (speed < 0.0D) {
                            frame = 0;
                            lerpedFrame = 0.0F;
                        }

                        speed = 0.0F;
                        break;
                    case LOOP:
                        if (speed > 0.0D) {
                            lerpedFrame  -= (float)(animation.getFrameCount()); /* MyChange: adjusted loop */
                            frame = (int)(lerpedFrame); /* MyChange: adjusted loop */
                        } else if (speed < 0.0D) {
                            lerpedFrame  += (float)(animation.getFrameCount()); /* MyChange: adjusted loop */
                            frame = (int)(lerpedFrame); /* MyChange: adjusted loop */
                        }
                        break;
                    case PINGPONG:
                        if (speed > 0.0D) {
                            frame = animation.getFrameCount() - 2;
                            lerpedFrame = (float)(animation.getFrameCount() - 1);
                            speed *= -1.0F;
                        } else if (speed < 0.0D) {
                            frame = 1;
                            lerpedFrame = 1.0F;
                            speed *= -1.0F;
                        }
                }

            }
        }
    }

    private Mesh mesh;
    private Skeleton skeleton;
    private List<SubEntity> subEntityList;
    private HashMap<String, Animation> animationsList = new HashMap();

    private AnimationState curAnimState; /* MyChange: added anim states */
    private AnimationState nextAnimState; /* MyChange: added anim states */
    private AnimationState waitingAnimState; /* MyChange: added anim states */

    private Matrix4[] curSkinMatrices;
    private Matrix3[] curSkinMatricesIT;

    private HashMap<Integer, Vector3> boneScaleOverride = new HashMap();
    private HashMap<Integer, Quaternion> boneRotationOverride = new HashMap();
    private HashMap<Integer, Quaternion> boneRotationAdditive = new HashMap();
    private HashMap<Integer, Vector3> boneLocationOverride = new HashMap();
    private HashMap<Integer, Vector3> boneLocationAdditive = new HashMap();

    GenericSkeletalEntity(SceneManager manager, String name, Mesh m, Skeleton s) {
        super(manager, name);

        curAnimState = null; /* MyChange: added anim states */

        if (m == null) {
            throw new NullPointerException("Null " + Mesh.class.getSimpleName());
        } else if (m.getSubMeshCount() == 0) {
            throw new RuntimeException(Mesh.class.getSimpleName() + " has 0 " + SubMesh.class.getSimpleName());
        } else if (m.getSubMesh(0).getBoneCount() != s.getBoneCount()) {
            throw new RuntimeException(SubMesh.class.getSimpleName() + " and " + Skeleton.class.getSimpleName() + " have different bone counts");
        } else {
            this.mesh = m;
            this.skeleton = s;
            this.subEntityList = new ArrayList(this.mesh.getSubMeshCount());
            int boneCount = this.skeleton.getBoneCount();
            this.curSkinMatrices = new Matrix4[boneCount];
            this.curSkinMatricesIT = new Matrix3[boneCount];

            int i;
            for(i = 0; i < this.curSkinMatrices.length; ++i) {
                this.curSkinMatrices[i] = Matrix4f.createIdentityMatrix();
            }

            for(i = 0; i < this.curSkinMatricesIT.length; ++i) {
                this.curSkinMatricesIT[i] = Matrix3f.createIdentityMatrix();
            }

            Iterator var7 = this.mesh.getSubMeshes().iterator();

            while(var7.hasNext()) {
                SubMesh subMesh = (SubMesh)var7.next();
                this.subEntityList.add(new GenericSubEntity(this, subMesh));
            }

            var7 = this.getSubEntities().iterator();

            while(var7.hasNext()) {
                SubEntity se = (SubEntity)var7.next();
                se.setPoseSkinMatrices(this.curSkinMatrices);
                se.setPoseSkinMatricesIT(this.curSkinMatricesIT);
            }

        }
    }

    public void update(float delta) {  /* MyChange: added delta for framerate independence */
        this.updateAnimation(delta);  /* MyChange: added delta for framerate independence */
        this.updateCurrentPoseMatrices();
    }

    private void updateCurrentPoseMatrices() {
        int i;
        for(i = 0; i < this.curSkinMatrices.length; ++i) {
            Matrix4 mat = this.getBoneLocal2ModelSpaceTransform(i).inverse();

            for(int curBone = i; curBone != -1; curBone = this.skeleton.getBoneParentIndex(curBone)) {
                mat = this.getBoneCurLocalTransform(curBone).mult(mat);
                mat = this.getBoneRestTransformRel2Parent(curBone).mult(mat);
            }

            this.curSkinMatrices[i] = mat;
        }

        for(i = 0; i < this.curSkinMatrices.length; ++i) {
            this.curSkinMatricesIT[i] = this.curSkinMatrices[i].inverse().transpose().toMatrix3();
        }

    }

    public Matrix4 getBoneLocal2ModelSpaceTransform(int i) {
        Matrix4 mat = Matrix4f.createIdentityMatrix();

        for(int curBone = i; curBone != -1; curBone = this.skeleton.getBoneParentIndex(curBone)) {
            mat = this.getBoneRestTransformRel2Parent(curBone).mult(mat);
        }

        return mat;
    }

    public Matrix4 getBoneCurLocalTransform(int i) {
        if (curAnimState == null) { /* MyChange: added anim states */
            return Matrix4f.createIdentityMatrix();
        } else {
            Vector3 scale = curAnimState.getBoneScale(i);  /* MyChange: added anim states */
            Quaternion rot = curAnimState.getBoneRotation(i);  /* MyChange: added anim states */
            Vector3 loc = curAnimState.getBoneLocation(i);  /* MyChange: added anim states */

            if (nextAnimState != null) {  /* MyChange: added anim states */
                scale = scale.lerp(nextAnimState.getBoneScale(i), nextAnimState.animationLerp);
                rot = MathHelper.slerp(rot, nextAnimState.getBoneRotation(i), nextAnimState.animationLerp);
                loc = loc.lerp(nextAnimState.getBoneLocation(i), nextAnimState.animationLerp);
            }

            /* MyChange: added overrides */
            if (boneScaleOverride.containsKey(i)) { scale = boneScaleOverride.get(i); }
            if (boneRotationOverride.containsKey(i)) { rot = boneRotationOverride.get(i); }
            if (boneRotationAdditive.containsKey(i)) { rot = rot.mult(boneRotationAdditive.get(i)); }
            if (boneLocationOverride.containsKey(i)) { loc = boneLocationOverride.get(i); }
            if (boneLocationAdditive.containsKey(i)) { loc = loc.add(boneLocationAdditive.get(i)); }

            Matrix4 mat = Matrix4f.createScalingFrom(scale);
            mat = Matrix4f.createRotationFrom(rot.angle(), this.getQuatAxis(rot)).mult(mat);
            mat = Matrix4f.createTranslationFrom(loc).mult(mat);
            return mat;
        }
    }

    public Matrix4 getBoneModelTransform(String boneName) { /* MyChange: added bone transform getter */
        int index = getBoneIndex(boneName);
        if (index == -1) { return null; }

        Matrix4 mat = Matrix4f.createIdentityMatrix();

        for(int curBone = index; curBone != -1; curBone = this.skeleton.getBoneParentIndex(curBone)) {
            mat = this.getBoneCurLocalTransform(curBone).mult(mat);
            mat = this.getBoneRestTransformRel2Parent(curBone).mult(mat);
        }

        return mat;
    }

    private Vector3 getQuatAxis(Quaternion q) {
        Vector3 axis;
        try {
            axis = q.axis();
        } catch (ArithmeticException var4) {
            axis = Vector3f.createFrom(1.0F, 0.0F, 0.0F);
        }

        return axis;
    }

    private Matrix4 getBoneRestTransformRel2Parent(int i) {
        Matrix4 mat = Matrix4f.createIdentityMatrix();
        Quaternion restRot = this.skeleton.getBoneRestRot(i);
        mat = Matrix4f.createRotationFrom(restRot.angle(), this.getQuatAxis(restRot)).mult(mat);
        Vector3 restLoc = this.skeleton.getBoneRestLoc(i);
        mat = Matrix4f.createTranslationFrom(restLoc).mult(mat);
        int parentIndex = this.skeleton.getBoneParentIndex(i);
        if (parentIndex == -1) {
            return mat;
        } else {
            float parentBoneLength = this.skeleton.getBoneLength(parentIndex);
            mat = Matrix4f.createTranslationFrom(0.0F, parentBoneLength, 0.0F).mult(mat);
            return mat;
        }
    }

    private void updateAnimation(float delta) {  /* MyChange: added delta for framerate independence */
        if (curAnimState != null) {
            curAnimState.update(delta, waitingAnimState != null);
            if (!curAnimState.interruptable && !curAnimState.stopped) {
                return;
            }
        }
        if (nextAnimState != null) {
            nextAnimState.update(delta, waitingAnimState != null);
            if (curAnimState == null || nextAnimState.animationLerp >= 1) {
                curAnimState = nextAnimState;
                nextAnimState = waitingAnimState;
                waitingAnimState = null;
            }
        }
    }

    public void playAnimation(String animName, float animSpeed, EndType endType, int endTypeCount, boolean interruptable) { /* MyChange: added interruptable */
        Animation anim = (Animation)this.animationsList.get(animName);
        if (anim != null) { /* MyChange: added anim states */
            AnimationState state = new AnimationState();
            state.animation = anim;
            state.lerpedFrame = 0;
            state.frame = 0;
            state.nextFrame = 1;
            state.endTypeTotal = endTypeCount;
            state.endTypeCount = 0;
            state.lerpedFrame = 0.0F;
            state.speed = animSpeed;
            state.endtype = endType;
            if (state.speed < 0.0F) {
                state.frame = anim.getFrameCount() - 1;
                state.nextFrame = anim.getFrameCount() - 2;
                state.lerpedFrame = (float)(anim.getFrameCount() - 1);
            }
            state.paused = false;
            state.stopped = false;
            state.interruptable = interruptable;

            if (curAnimState == null) {
                curAnimState = state;
            } else if (nextAnimState == null) {
                nextAnimState = state;
            } else {
                waitingAnimState = state;
            }
        }
    }

    public void pauseAnimation() {
        if (curAnimState != null) { curAnimState.paused = true; }
        if (nextAnimState != null) { nextAnimState.paused = true; }
    }

    public void stopAnimation() {
        curAnimState = null;
        nextAnimState = null;
    }

    private int getBoneIndex(String boneName) { /* MyChange: added bone index lookup */
        String[] boneNames = skeleton.getBoneNames();
        for (int i = 0; i < boneNames.length; i++) {
            if (boneNames[i].compareTo(boneName) == 0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void addScaleOverride(String boneName, Vector3 scale) { /* MyChange: added override */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneScaleOverride.put(index, scale);
    }

    @Override
    public void removeScaleOverride(String boneName) { /* MyChange: added override */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneScaleOverride.remove(index);
    }

    @Override
    public void addRotationOverride(String boneName, Quaternion rotation) { /* MyChange: added override */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneRotationOverride.put(index, rotation);
    }

    @Override
    public void removeRotationOverride(String boneName) { /* MyChange: added override */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneRotationOverride.remove(index);
    }

    @Override
    public void addRotationAdditive(String boneName, Quaternion rotation) { /* MyChange: added additive */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneRotationAdditive.put(index, rotation);

    }

    @Override
    public void removeRotationAdditive(String boneName) { /* MyChange: added additive */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneRotationAdditive.remove(index);
    }

    @Override
    public void addLocationOverride(String boneName, Vector3 location) { /* MyChange: added override */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneLocationOverride.put(index, location);
    }

    @Override
    public void removeLocationOverride(String boneName) { /* MyChange: added override */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneLocationOverride.remove(index);
    }

    @Override
    public void addLocationAdditive(String boneName, Vector3 location) { /* MyChange: added additive */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneLocationAdditive.put(index, location);
    }

    @Override
    public void removeLocationAdditive(String boneName) { /* MyChange: added additive */
        int index = getBoneIndex(boneName);
        if (index == -1) { return; }
        boneLocationAdditive.remove(index);
    }

    public void loadAnimation(String animName, String animationPath) throws IOException {
        if (animationPath.isEmpty()) {
            throw new IllegalArgumentException("animationPath is empty");
        } else if (animName.isEmpty()) {
            throw new IllegalArgumentException("animName is empty");
        } else {
            Animation anim = (Animation)this.getManager().getAnimationManager().getAsset(Paths.get(animationPath));
            if (anim != null) {
                if (anim.getBoneCount() != this.skeleton.getBoneCount()) {
                    throw new RuntimeException(Animation.class.getSimpleName() + " and " + Skeleton.class.getSimpleName() + " have different bone counts");
                } else {
                    this.animationsList.put(animName, anim);
                }
            }
        }
    }

    public void unloadAnimation(String animName) {
        if (animName.isEmpty()) {
            throw new IllegalArgumentException("animName is empty");
        } else if (this.animationsList.containsKey(animName)) {
            this.animationsList.remove(animName);
        }
    }

    public Mesh getMesh() {
        return this.mesh;
    }

    public Iterable<SubEntity> getSubEntities() {
        return this.subEntityList;
    }

    public SubEntity getSubEntity(int idx) {
        return (SubEntity)this.subEntityList.get(idx);
    }

    public int getSubEntityCount() {
        return this.subEntityList.size();
    }

    public void setMaterial(Material mat) {
        Iterator var3 = this.subEntityList.iterator();

        while(var3.hasNext()) {
            SubEntity se = (SubEntity)var3.next();
            se.setMaterial(mat);
        }

    }

    public void setGpuShaderProgram(GpuShaderProgram prog) {
        Iterator var3 = this.subEntityList.iterator();

        while(var3.hasNext()) {
            SubEntity se = (SubEntity)var3.next();
            se.setGpuShaderProgram(prog);
        }

    }

    public void setPrimitive(Primitive prim) {
        Iterator var3 = this.subEntityList.iterator();

        while(var3.hasNext()) {
            SubEntity se = (SubEntity)var3.next();
            se.setPrimitive(prim);
        }

    }

    public void visitSubEntities(Visitor visitor) {
        Iterator var3 = this.subEntityList.iterator();

        while(var3.hasNext()) {
            SubEntity se = (SubEntity)var3.next();
            visitor.visit(se);
        }

    }

    public void setRenderState(RenderState rs) {
        Iterator var3 = this.subEntityList.iterator();

        while(var3.hasNext()) {
            SubEntity se = (SubEntity)var3.next();
            se.setRenderState(rs);
        }

    }

    public void notifyDispose() {
        Iterator var2 = this.animationsList.keySet().iterator();

        while(var2.hasNext()) {
            String animName = (String)var2.next();
            this.animationsList.replace(animName, null);
        }

        this.animationsList = null;
        this.mesh = null;
        this.skeleton = null;
        this.animationsList.clear();
        this.animationsList = null;
        var2 = this.subEntityList.iterator();

        while(var2.hasNext()) {
            SubEntity se = (SubEntity)var2.next();
            se.notifyDispose();
        }

        this.subEntityList.clear();
        this.subEntityList = null;
        super.notifyDispose();
    }
}

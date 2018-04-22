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
import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Matrix4;
import ray.rml.Matrix4f;
import ray.rml.Quaternion;
import ray.rml.Vector3;
import ray.rml.Vector3f;

final class GenericSkeletalEntity extends AbstractGenericSceneObject implements SkeletalEntity {
    private Mesh mesh;
    private Skeleton skeleton;
    private List<SubEntity> subEntityList;
    private HashMap<String, Animation> animationsList = new HashMap();
    private Animation curAnimation = null;
    private int curAnimFrame = -1;
    private int nextAnimFrame = -1; /* MyChange: added for lerping */
    private float lerpScale = 0f; /* MyChange: added for lerping */
    private float curLerpedAnimFrame = -1.0F;
    private float curAnimSpeed = 1.0F;
    private EndType curAnimEndtype;
    private int curAnimEndTypeTotal;
    private int curAnimEndTypeCount;
    private boolean curAnimPaused;
    private Matrix4[] curSkinMatrices;
    private Matrix3[] curSkinMatricesIT;

    GenericSkeletalEntity(SceneManager manager, String name, Mesh m, Skeleton s) {
        super(manager, name);
        this.curAnimEndtype = EndType.NONE;
        this.curAnimEndTypeTotal = -1;
        this.curAnimEndTypeCount = 0;
        this.curAnimPaused = false;
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
        if (this.curAnimation == null) {
            return Matrix4f.createIdentityMatrix();
        } else {
            Vector3 scale = this.curAnimation.getFrameBoneScl(this.curAnimFrame, i)
                    .lerp(this.curAnimation.getFrameBoneScl(this.nextAnimFrame, i), this.lerpScale); /* MyChange: added lerping */

            Quaternion rot = this.curAnimation.getFrameBoneRot(this.curAnimFrame, i)
                    .lerp(this.curAnimation.getFrameBoneRot(this.nextAnimFrame, i), this.lerpScale); /* MyChange: added lerping */

            Vector3 loc = this.curAnimation.getFrameBoneLoc(this.curAnimFrame, i)
                    .lerp(this.curAnimation.getFrameBoneLoc(this.nextAnimFrame, i), this.lerpScale); /* MyChange: added lerping */

            Matrix4 mat = Matrix4f.createScalingFrom(scale);
            mat = Matrix4f.createRotationFrom(rot.angle(), this.getQuatAxis(rot)).mult(mat);
            mat = Matrix4f.createTranslationFrom(loc).mult(mat);
            return mat;
        }
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
        if (this.curAnimation != null && !this.curAnimPaused && this.curAnimSpeed != 0.0F) {
            this.curLerpedAnimFrame += this.curAnimSpeed * delta;
            this.lerpScale = this.curLerpedAnimFrame - (int)this.curLerpedAnimFrame;  /* MyChange: added for lerping */
            this.curAnimFrame = (int)(this.curLerpedAnimFrame); /* MyChange: changed round function */
            if (this.curAnimFrame >= this.curAnimation.getFrameCount() || this.curAnimFrame < 0) {
                this.handleAnimationEnd();
            }
            this.nextAnimFrame = calculateNextFrame(); /* MyChange: added for lerping */
        }

    }

    private int calculateNextFrame() { /* MyChange: added for lerping */
        if (curAnimSpeed == 0) { return curAnimFrame; }
        int nextFrame = curAnimFrame + (curAnimSpeed > 0 ? 1 : -1);
        switch(this.curAnimEndtype) {
            case NONE:
            case STOP:
                if (nextFrame >= curAnimation.getFrameCount()) { nextFrame = curAnimFrame; }
                break;
            case PAUSE:
                if (nextFrame >= curAnimation.getFrameCount()) { nextFrame = curAnimFrame; }
                if (nextFrame < 0) { nextFrame = 0; }
                break;
            case LOOP:
                if (nextFrame >= curAnimation.getFrameCount()) { nextFrame -= curAnimation.getFrameCount(); }
                if (nextFrame < 0) { nextFrame += curAnimation.getFrameCount(); }
                break;
            case PINGPONG:
                if (nextFrame >= curAnimation.getFrameCount()) { nextFrame -= 2; }
                if (nextFrame < 0) { nextFrame += 2; }
                break;
        }

        return nextFrame;
    }

    private void handleAnimationEnd() {
        ++this.curAnimEndTypeCount;
        if (this.curAnimEndTypeTotal != 0 && this.curAnimEndTypeCount > this.curAnimEndTypeTotal) {
            this.stopAnimation();
        } else {
            switch(this.curAnimEndtype) {
                case NONE:
                case STOP:
                    this.stopAnimation();
                    break;
                case PAUSE:
                    if ((double)this.curAnimSpeed > 0.0D) {
                        this.curAnimFrame = this.curAnimation.getFrameCount() - 1;
                        this.curLerpedAnimFrame = (float)(this.curAnimation.getFrameCount() - 1);
                    } else if ((double)this.curAnimSpeed < 0.0D) {
                        this.curAnimFrame = 0;
                        this.curLerpedAnimFrame = 0.0F;
                    }

                    this.curAnimSpeed = 0.0F;
                    break;
                case LOOP:
                    if ((double)this.curAnimSpeed > 0.0D) {
                        this.curLerpedAnimFrame  -= (float)(curAnimation.getFrameCount()); /* MyChange: adjusted loop */
                        this.curAnimFrame = (int)(this.curLerpedAnimFrame); /* MyChange: adjusted loop */
                    } else if ((double)this.curAnimSpeed < 0.0D) {
                        this.curLerpedAnimFrame  += (float)(curAnimation.getFrameCount()); /* MyChange: adjusted loop */
                        this.curAnimFrame = (int)(this.curLerpedAnimFrame); /* MyChange: adjusted loop */
                    }
                    break;
                case PINGPONG:
                    if ((double)this.curAnimSpeed > 0.0D) {
                        this.curAnimFrame = this.curAnimation.getFrameCount() - 2;
                        this.curLerpedAnimFrame = (float)(this.curAnimation.getFrameCount() - 1);
                        this.curAnimSpeed *= -1.0F;
                    } else if ((double)this.curAnimSpeed < 0.0D) {
                        this.curAnimFrame = 1;
                        this.curLerpedAnimFrame = 1.0F;
                        this.curAnimSpeed *= -1.0F;
                    }
            }

        }
    }

    public void playAnimation(String animName, float animSpeed, EndType endType, int endTypeCount) {
        Animation anim = (Animation)this.animationsList.get(animName);
        if (anim != null) {
            this.curAnimation = anim;
            this.curLerpedAnimFrame = 0.0F;
            this.curAnimFrame = 0;
            this.nextAnimFrame = 0;
            this.curAnimEndTypeTotal = endTypeCount;
            this.curAnimEndTypeCount = 0;
            this.curLerpedAnimFrame = 0.0F;
            this.curAnimSpeed = animSpeed;
            this.curAnimEndtype = endType;
            if (this.curAnimSpeed < 0.0F) {
                this.curAnimFrame = anim.getFrameCount() - 1;
                this.curLerpedAnimFrame = (float)(anim.getFrameCount() - 1);
            }

            this.curAnimPaused = false;
        }
    }

    public void pauseAnimation() {
        this.curAnimPaused = true;
    }

    public void stopAnimation() {
        this.curAnimation = null;
        this.curAnimEndtype = EndType.NONE;
        this.curAnimFrame = -1;
        this.nextAnimFrame = -1;
        this.curLerpedAnimFrame = -1.0F;
        this.curAnimPaused = false;
        this.curAnimSpeed = 1.0F;
        this.curAnimEndTypeCount = 0;
        this.curAnimEndTypeTotal = 0;
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

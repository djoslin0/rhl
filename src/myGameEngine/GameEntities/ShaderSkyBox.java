package myGameEngine.GameEntities;

import a2.MyGame;
import ray.rage.Engine;
import ray.rage.asset.texture.Texture;
import ray.rage.asset.texture.TextureManager;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SkyBox;

import javax.security.auth.login.Configuration;
import java.awt.geom.AffineTransform;
import java.io.IOException;

public class ShaderSkyBox {
    public ShaderSkyBox(Engine eng, SceneManager sm,MyGame game) throws IOException {
        initiatSkyBox(eng , sm, game);

    }
    private void initiatSkyBox(Engine eng, SceneManager sm, MyGame game)throws IOException{
        // set up sky box
        ray.rage.util.Configuration conf = eng.getConfiguration();
        TextureManager tm = game.getEngine().getTextureManager();
        tm.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
        Texture front = tm.getAssetByPath("Front.png");
        Texture back = tm.getAssetByPath("Back.png");
        Texture left = tm.getAssetByPath("Left.png");
        Texture right = tm.getAssetByPath("Right.png");
        Texture top = tm.getAssetByPath("Top.png");
        Texture bottom = tm.getAssetByPath("Bottom.png");
        tm.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));

        AffineTransform xform = new AffineTransform();
        xform.translate(0, front.getImage().getHeight());
        xform.scale(1d, -1d);
        front.transform(xform);
        back.transform(xform);
        left.transform(xform);
        right.transform(xform);
        top.transform(xform);
        bottom.transform(xform);
        SkyBox sb = sm.createSkyBox("SkyBox");
        sb.setTexture(front, SkyBox.Face.FRONT);
        sb.setTexture(back, SkyBox.Face.BACK);
        sb.setTexture(left, SkyBox.Face.LEFT);
        sb.setTexture(right, SkyBox.Face.RIGHT);
        sb.setTexture(top, SkyBox.Face.TOP);
        sb.setTexture(bottom, SkyBox.Face.BOTTOM);
        sm.setActiveSkyBox(sb);
    }
}

package a2.Hud;

import myGameEngine.GameEntities.HudElement;
import myGameEngine.Singletons.UniqueCounter;
import ray.rage.scene.SceneNode;
import ray.rml.Vector2;
import ray.rml.Vector2f;

import java.awt.*;
import java.io.IOException;

public class HudNumber {
    private SceneNode[] digitNodes;
    private HudElement[] digits;
    private float spacing;
    private boolean showLeadingZeros = false;

    public HudNumber(SceneNode parentNode, int digits, float scale, Vector2 location, Color color, boolean showLeadingZeros) throws IOException {
        this.digits = new HudElement[digits];
        this.digitNodes = new SceneNode[digits];
        this.spacing = scale * 1.3f;
        this.showLeadingZeros = showLeadingZeros;

        for (int i = 0; i < digits; i++) {
            this.digitNodes[i] = parentNode.createChildSceneNode("digit" + UniqueCounter.next());
            this.digits[i] = new HudElement(digitNodes[i], scale, location, Vector2f.createZeroVector(), (i == 0) ? "0.png" : "empty.png", color);
        }
        update(0);
    }

    public void update(int number) {
        int numberLength = 0;
        for (int i = 0; i < digits.length; i++) {
            String textureName = (number % 10) + ".png";
            if (i > 0 && number == 0 && !showLeadingZeros) {
                textureName = "empty.png";
            } else {
                numberLength++;
            }
            digits[i].updateTexture(textureName);
            number /= 10;
        }

        for (int i = 0; i < numberLength; i++) {
            digitNodes[i].setLocalPosition((i - (numberLength - 1) / 2f) * spacing, 0, 0);
        }
    }

    public void setScale(float scale) {
        for (HudElement digit : digits) {
            digit.getNode().setLocalScale(scale, scale, scale);
        }
    }

    public void setColor(Color color) {
        for (HudElement digit : digits) {
            digit.getMaterial().setAmbient(color);
        }
    }

    public void show() {
        for (HudElement digit : digits) { digit.show(); }
    }

    public void hide() {
        for (HudElement digit : digits) { digit.hide(); }
    }
}

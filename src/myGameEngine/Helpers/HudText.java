package myGameEngine.Helpers;

import java.awt.*;

// The HudText class represents pieces of text that are displayed on the HUD
// negative x and y values are drawn from the opposite sides of the screen
//      e.g. an x of -30 will be drawn at (screen width - 30)
public class HudText {
    public String text;
    public int x;
    public int y;
    public int font;
    public Color color;

    public HudText(int x, int y, Color color, int font) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.font = font;
    }

    public int renderX(Canvas canvas) {
        return x >= 0 ? x : x + canvas.getWidth();
    }

    public int renderY(Canvas canvas) {
        return y >= 0 ? y : y + canvas.getHeight();
    }
}

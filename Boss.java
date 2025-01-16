/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-16
 * Description: This class defines properties for the boss character such as health, name 
 * and visual representation. It provides helper methods for interactions with the boss such
 * as taking damage or being defeated.
 */
import java.awt.*;
import java.awt.image.BufferedImage;

public class Boss{
    private int hp;
    private int maxHP;
    private String name;
    private Rectangle rect;
    private BufferedImage image;

    // constructor to initialize the Boss with specified attributes
    public Boss(String name, int maxHP, BufferedImage image, int x, int y, int width, int height) {
        this.name = name;
        this.hp = maxHP;
        this.maxHP = maxHP;
        this.rect = new Rectangle(x, y, width, height);
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public int getHP() {
        return hp;
    }

    public int getMaxHP() {
        return maxHP;
    }

    // reduces the boss's HP by the specified amount
    public void damage(int amount) {
        hp -= amount;
        if (hp < 0) {
            hp = 0;
        }
    }

    // checks if the boss is defeated (0 or less HP)
    public boolean isDefeated() {
        if(hp <=0 ){
            return true;
        } else {
            return false;
        }
    }

    public Rectangle getRect() {
        return rect;
    }

    // draws the boss's UI, including name and HP bar
    public void drawUI(Graphics g, Font uiFont, int width) {
        String nameText;
        FontMetrics fm;
        int nameWidth;
        int nameX;
        int nameY;
        int barWidth;
        int barHeight;
        int barX;
        int barY;
        double hpRatio;
        int fillWidth;

        g.setFont(uiFont);
        nameText = name;
        fm = g.getFontMetrics();

        nameWidth = fm.stringWidth(nameText);
        nameX = width / 2 - nameWidth / 2;
        nameY = 30;

        g.setColor(Color.WHITE);
        g.drawString(nameText, nameX, nameY);

        barWidth = 200;
        barHeight = 15;

        barX = width / 2 - barWidth / 2;
        barY = nameY + 10;

        g.drawRect(barX, barY, barWidth, barHeight);
        hpRatio = (double) hp / maxHP;

        fillWidth = (int) (barWidth*hpRatio);

        g.setColor(Color.RED);
        g.fillRect(barX + 1, barY + 1, fillWidth - 1, barHeight - 1);
    }

    // draws the boss's image
    public void drawImage(Graphics g, int width, int height) {
        g.drawImage(image, rect.x, rect.y, rect.width, rect.height, null);
    }
}

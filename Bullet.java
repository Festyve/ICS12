/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class defines the properties of a bullet in the game. It contains methods for 
 * handling its position, movement, rendering, and collision detection.
 */
import java.awt.*;

public class Bullet {
    public int x, y;
    private int size;
    private int imageWidth, imageHeight;
    public int speedX, speedY;
    private int damage;
    private Color color;
    private Image bulletImage;

    // constructor for a Bullet with equal width and height based on the size parameter.
    public Bullet(int x, int y, int size, int speedX, int speedY, int damage, Image bulletImage, Color color) {
        this(x, y, size, size, size, speedX, speedY, damage, bulletImage, color);
    }

    // constructor for a Bullet with specified dimensions
    public Bullet(int x, int y, int size, int imageWidth, int imageHeight, int speedX, int speedY, int damage, Image bulletImage, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.speedX = speedX;
        this.speedY = speedY;
        this.damage = damage;
        this.bulletImage = bulletImage;
        this.color = color;
    }

    // updates the bullet's position based on its speed
    public void update() {
        x += speedX;
        y += speedY;
    }

    // draws the bullet's image on the screen
    public void draw(Graphics g, Component observer) {
        if(bulletImage != null){
            g.drawImage(bulletImage, x - imageWidth / 2, y - imageHeight / 2, imageWidth, imageHeight, observer);
        } else {
            g.setColor(color);
            g.fillRect(x, y, 20,20);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, 20, 20);
        }
    }

    // checks if the bullet is out of the specified bounds , used for collision detection
    public boolean isOutOfBounds(int width, int height, Rectangle battleBox) {
        return (x < -size || x > width + size || y < -size || y > height + size);
    }

    // returns the bounding rectangle of the bullet for collision detection.
    public Rectangle getBounds() {
        return new Rectangle(x - size / 2, y - size / 2, size, size);
    }

    // returns the damage value of the bullet
    public int getDamage() {
        return damage;
    }
}

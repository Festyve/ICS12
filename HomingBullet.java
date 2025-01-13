/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class defines the properties of a homing bullet that tracks and moves 
 * towards a target player. It inherits methods from the Bullet class for handling its position,
 * movement, rendering, and collision detection.
 */
import java.awt.*;

public class HomingBullet extends Bullet {
    private Player target; // the target that the bullet with home onto

    // constructor  for a HomingBullet with the specified attributes and target
    public HomingBullet(int x, int y, int size, int damage, Player target, Image bulletImage) {
        super(x, y, size, 0, 0, damage, bulletImage, Color.GREEN);
        this.target = target;
    }

    // adjusts the bullet's speed to move towards the player
    public void home() {
        Rectangle targetHitbox;
        int tx;
        int ty;
        int dx;
        int dy;
        double dist;
        double speed;

        targetHitbox = target.getHitbox();

        // calculate the center of the target hitbox
        tx = targetHitbox.x + targetHitbox.width / 2;
        ty = targetHitbox.y + targetHitbox.height / 2;

        // compute the difference in coordinates
        dx = tx - x;
        dy = ty - y;

        // calculate the distance to the target
        dist = Math.sqrt(dx * dx + dy * dy);

        speed = 5.0;

        // normalize the direction and set the speed
        if (dist > 0) {
            speedX = (int) (speed * (dx / dist));
            speedY = (int) (speed * (dy / dist));
        }
    }
}

/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class implements a homing projectile attack pattern for the boss character.
 * It manages the spawning and behavior of homing bullets that track the player. The attack has
 * configurable parameters such as duration, damage, spawn intervals, and adapts based on the
 * game's cycle count.
 */
import java.awt.*;
import java.util.Iterator;
import java.util.Random;

public class HomingProjectileAttack extends AttackPattern {
    private long startTime;
    private boolean started = false;
    private int duration = 7000; 
    private int damage = 15; 
    private Random rand = new Random();
    private int delay = 2000; 

    private boolean showExclamation = false;
    private long lastToggleTime = 0; // last time the exclamation mark visiblity is toggled
    private int toggleInterval = 500; // interval between toggles of exclamation mark
    private int exclamationX;
    private int exclamationY;

    private Image bulletImage;

    private int spawnedCount = 0;
    private int maxSpawns = 10;   
    private long lastSpawnTime;
    private long spawnInterval = 2000; // interval between spawning new bullets

    // constructor for a HomingProjectileAttack with the specified GamePanel and bullet image
    public HomingProjectileAttack(GamePanel panel, Image bulletImage) {
        super(panel);
        this.bulletImage = bulletImage;
    }

    @Override
    // method to initailize the attack pattern, setting up all the variables and states
    public void initialize() {
        Rectangle battleBox;
        
        // reset all variables of the attack
        panel.getBullets().clear();
        finished = false;
        started = false;
        startTime = System.currentTimeMillis();

        battleBox = panel.getBattleBox();

        // configure the position of the exclamation mark image
        exclamationX = battleBox.x + battleBox.width / 2;
        exclamationY = battleBox.y + 180;

        lastToggleTime = startTime;
        // set exclamation mark to not visible
        panel.setExclamationState(this, false, exclamationX, exclamationY);

        spawnedCount = 0;
        maxSpawns = 10;
        lastSpawnTime = System.currentTimeMillis();
        spawnInterval = 2000; 

        // if this is the second attack cycle or later, increase attack difficulty
        if (panel.getCycleCount() >= 1) {
            duration = 10000; 
            spawnInterval = 1500;
            maxSpawns = 15; 
            damage = 25;
        }
    }

    
    @Override
    // method to perform the attack pattern
    public void execute() {
        long now;
        Rectangle box;
        Point spawnPoint;
        HomingBullet hb;
        Iterator<Bullet> it;

        now = System.currentTimeMillis();
        box = panel.getBattleBox();

        if (!started) {
            // toggle exclamation mark visibility to indicate impending attack
            if (now - lastToggleTime >= toggleInterval) {
                showExclamation = !showExclamation;
                lastToggleTime = now;
                panel.setExclamationState(this, showExclamation, exclamationX, exclamationY);
            }
            // start spawning bullets after the initial delay
            if ((now - startTime) >= delay) {
                for (int i = 0; i < 5; i++) {
                    spawnPoint = getRandomEdgePoint(box);
                    hb = new HomingBullet(spawnPoint.x,spawnPoint.y,45,damage,panel.getPlayer(), bulletImage);
                    panel.spawnBullet(hb);
                    spawnedCount++;
                }
                started = true;
                showExclamation = false;
                panel.setExclamationState(this, false, exclamationX, exclamationY);
            }
        } else {
            // spawn additional bullets periodically if maximum spawns not reached
            if ((now - lastSpawnTime >= spawnInterval) && spawnedCount < maxSpawns) {
                spawnPoint = getRandomEdgePoint(box);
                hb = new HomingBullet(spawnPoint.x,spawnPoint.y,45,damage,panel.getPlayer(),bulletImage);
                panel.spawnBullet(hb);
                spawnedCount++;
                lastSpawnTime = now;
            }
        }

        it = panel.getBullets().iterator();
        while (it.hasNext()) {
            Bullet b = it.next();

            // if the bullet is a HomingBullet, adjust trajectory of homing bullet towards the player
            if (b instanceof HomingBullet) {
                hb = (HomingBullet) b;
                hb.home();
            }

            // update the bullet's position and speed
            b.update();

            // check if the bullet has moved out of the battle area bounds
            if (b.isOutOfBounds(panel.getWidth(), panel.getHeight(), box)) {
                it.remove();
            } 
            // check for collision between the bullet and the player
            else if (b.getBounds().intersects(panel.getPlayer().getHitbox())) {
                panel.decreasePlayerHP(b.getDamage());
                it.remove();
                // if the player's HP reaches zero or below, mark the attack as finished
                if (panel.getPlayer().isDead()) {
                    finished = true;
                }
            }
        }

        // check for end conditions: attack duration elapsed or all bullets spawned and cleared
        if ((now - startTime) >= duration || (started && panel.getBullets().isEmpty() && spawnedCount >= maxSpawns)) {
            finished = true;
        }
    }

    // generates a random point along the edge of the battle arena
    private Point getRandomEdgePoint(Rectangle box) {
        // randomly select one of the four sides (top, bottom, left, right)
        int side = rand.nextInt(4);
        int x = 0;
        int y = 0;

        // randomize the position based on which side its being generated
        if (side == 0) { // top edge
            x = box.x + rand.nextInt(box.width);
            y = box.y;
        } else if (side == 1) { // right edge
            x = box.x + box.width;
            y = box.y + rand.nextInt(box.height);
        } else if (side == 2) { // bottom edge
            x = box.x + rand.nextInt(box.width);
            y = box.y + box.height;
        } else if (side == 3) { // left edge
            x = box.x;
            y = box.y + rand.nextInt(box.height);
        }

        return new Point(x, y);
    }
}

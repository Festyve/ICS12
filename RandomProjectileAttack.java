/*
* Author: Michael Zhang and Bryan Yao
* Date: 2025-01-08
* Description: This class implements a random projectile attack pattern from the boss.
* It manages the spawning and behavior of the randomly generated bullets move at a random direction/speed.
* The attack pattern also changes depending on the amount of cycles the boss has done through his attack pattersns.
*/

import java.util.Iterator;
import java.util.Random;
import java.awt.*;

public class RandomProjectileAttack extends AttackPattern { //child of AttackPattern class
    private int projectilesSpawned = 0;
    private int maxProjectiles = 100; 
    private long lastSpawnTime;
    private int spawnInterval = 100; // interval between spawning attacks
    private Random rand = new Random();
    private Image projectileImage;
    private int damage = 10; 
    private int speedMin = 3; 
    private int speedMax = 4; 

    // constructor for the HomingProjectileAttack with the specified image & game panel
    public RandomProjectileAttack(GamePanel panel, Image projectileImage) {
        super(panel);
        this.projectileImage = projectileImage;
    }

    // method that initializes the attack pattern
    public void initialize() {
        projectilesSpawned = 0;
        lastSpawnTime = System.currentTimeMillis();
        finished = false;

        // If cycle >= 1 => make it even harder
        if (panel.getCycleCount() >= 1) {
            maxProjectiles += 100; // spawn more
        }
    }

    // method that executes/runs the attack pattern
    public void execute() {
        long now;
        Rectangle battleBox;
        Iterator<Bullet> it;
        Bullet bullet;

        now = System.currentTimeMillis();
        battleBox = panel.getBattleBox();
        
        // spawns the projectile based on the time in milliseconds
        if (now - lastSpawnTime > spawnInterval && projectilesSpawned < maxProjectiles) {
            spawnRandomProjectile(battleBox);
            projectilesSpawned++;
            lastSpawnTime = now;
        }
        
        // clears up the projectiles if they're out of pounds
        it = panel.getBullets().iterator();
        while (it.hasNext()) {
            bullet = it.next();
            bullet.update();
            if (bullet.isOutOfBounds(panel.getWidth(), panel.getHeight(), battleBox)) {
                it.remove(); // delete bullet
            } else if (bullet.getBounds().intersects(panel.getPlayer().getHitbox())) { // if projectile hits player
                panel.decreasePlayerHP(bullet.getDamage()); // depletes player HP based off the damage the bullet does
                it.remove();
                if (panel.getPlayer().isDead()) {
                    finished = true; // if player dies stop the attack
                }
            }
        }
        
        // caps the # of projectiles
        if (projectilesSpawned >= maxProjectiles && panel.getBullets().isEmpty()) {
            finished = true;
        }
    }
    
    // method to spawn the random projectiles
    private void spawnRandomProjectile(Rectangle battleBox) {
        // variables for speed and position of random projectiles
        int spawnSide;
        int size;
        int x;
        int y;
        int speedX;
        int speedY;
        int sx;
        int sy;

        Bullet newBullet; // variable for new bullets that are spawned
        
        spawnSide = rand.nextInt(4);

        size = 35;

        x = 0;
        y = 0;
        
        // speed and position of the randomized projectiles
        speedX = 0;
        speedY = 0;
        
        sx = speedMin + rand.nextInt(speedMax - speedMin + 1);
        sy = speedMin + rand.nextInt(speedMax - speedMin + 1);
        
        // where the projectiles spawn from (which side of the rectangle) and randomize which direction the bullets go
        if (spawnSide == 0) {
            x = battleBox.x + rand.nextInt(battleBox.width);
            y = battleBox.y - size;
            speedY = sy;
            speedX = rand.nextBoolean() ? sx : -sx;
        } else if (spawnSide == 1) {
            x = battleBox.x + rand.nextInt(battleBox.width);
            y = battleBox.y + battleBox.height + size;
            speedY = -sy;
            speedX = rand.nextBoolean() ? sx : -sx;
        } else if (spawnSide == 2) {
            x = battleBox.x - size;
            y = battleBox.y + rand.nextInt(battleBox.height);
            speedX = sx;
            speedY = rand.nextBoolean() ? sy : -sy;
        } else if (spawnSide == 3) {
            x = battleBox.x + battleBox.width + size;
            y = battleBox.y + rand.nextInt(battleBox.height);
            speedX = -sx;
            speedY = rand.nextBoolean() ? sy : -sy;
        }
        
        // spawn the bullet after randomization
        newBullet = new Bullet(x, y, size, speedX, speedY, damage, projectileImage, Color.ORANGE);
        panel.spawnBullet(newBullet);
    }
}
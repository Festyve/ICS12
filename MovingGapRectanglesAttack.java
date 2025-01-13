/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class handles a boss attack pattern where moving rectangular pillars
 * travel horizontally and leave gaps that the player must navigate through. It initializes 
 * pillars, updates their positions, checks collisions with the player, and renders them in the game.
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.IOException;

public class MovingGapRectanglesAttack extends AttackPattern {
    private long startTime;
    private int duration = 8000; 
    private int damage = 20; 
    private int rectWidth = 60;
    private int rectSpeed = 5;
    private ArrayList<int[]> pairs = new ArrayList<>();
    private Random rand = new Random();

    private Image attackImage;
    private boolean imageLoaded = false;

    // constructor that initializes the attack by loading the attack image.
    public MovingGapRectanglesAttack(GamePanel panel) {
        super(panel);
        loadAttackImage("/Images/pipe.png");
    }

    // loads the attack image from a given path.
    private void loadAttackImage(String imagePath) {
        Image tempImg;
        try {
            tempImg = ImageIO.read(getClass().getResource(imagePath));
            attackImage = tempImg;
            imageLoaded = true;
        } catch (IOException | IllegalArgumentException ex) {
            attackImage = null;
            imageLoaded = false;
            System.err.println("Failed to load attack image: " + ex.getMessage());
        }
    }

    // initializes the attack by clearing bullets and placing pillars off-screen
    public void initialize() {
        int numberOfPillars;
        int x;
        int gapY;
        int gapHeight;
        int[] p;

        panel.getBullets().clear();
        finished = false;
        startTime = System.currentTimeMillis();

        Rectangle box = panel.getBattleBox();
        pairs.clear();

        numberOfPillars = 8; // default for the first cycle

        // if second phase, increase difficulty
        if (panel.getCycleCount() >= 1) {
            duration = 14000; 
            rectSpeed = 10;
            damage += 5;
            numberOfPillars = 16;
        }

        // create pillars at intervals, each with a random gap position and a fixed gap height
        for (int i = 0; i < numberOfPillars; i++) {
            x = box.x - (i * 500) - 200;
            gapY = box.y + rand.nextInt(box.height - 100);
            gapHeight = 80; 

            p = new int[] { x, gapY, gapHeight };
            pairs.add(p);
        }
    }

    // updates the logic each frame: moves pillars, checks collisions and other game logic
    @Override
    public void execute() {
        long now;
        Rectangle box;

        now = System.currentTimeMillis();
        box = panel.getBattleBox();

        Iterator<int[]> it;
        int[] p;

        // define all the hitboxes for collision detection
        Rectangle playerRect;
        Rectangle topRect;
        Rectangle bottomRect;

        // move each pillar, detect collisions, and remove if out of bounds.
        it = pairs.iterator();
        while (it.hasNext()) {
            p = it.next();
            p[0] += (int) (rectSpeed * panel.getBossSpeedModifier());

            playerRect = panel.getPlayer().getHitbox();

            topRect = new Rectangle(p[0], box.y, rectWidth, p[1] - box.y);
            bottomRect = new Rectangle(p[0],p[1] + p[2],rectWidth,box.y + box.height - (p[1] + p[2]));

            // check collision with the player
            if (topRect.intersects(playerRect) || bottomRect.intersects(playerRect)) {
                panel.decreasePlayerHP(damage);
                if (panel.getPlayer().isDead()) {
                    finished = true;
                }
            }

            // remove pillars that have moved out of bounds
            if (p[0] > box.x + box.width + 200) {
                it.remove();
            }
        }

        // finish the attack phase if duration has passed and all pillars are cleared
        if (now - startTime > duration && pairs.isEmpty()) {
            finished = true;
            panel.getBullets().clear();
        }
    }

    // draw the pillars for the attack phase
    public void drawAttack(Graphics g) {
        Rectangle box = panel.getBattleBox();
        int topHeight;
        int bottomHeight;

        for (int[] p : pairs) {
            if (imageLoaded && attackImage != null) {
                topHeight = p[1] - box.y;
                bottomHeight = box.y + box.height - (p[1] + p[2]);

                // draw top rectangle using attackImage
                g.drawImage(attackImage,p[0], box.y,p[0] + rectWidth, box.y + topHeight,0, 0,attackImage.getWidth(panel), attackImage.getHeight(panel) / 2,panel);

                // draw bottom rectangle using attackImage
                g.drawImage(attackImage,p[0], p[1] + p[2],p[0] + rectWidth, p[1] + p[2] + bottomHeight,0, attackImage.getHeight(panel) / 2,attackImage.getWidth(panel), attackImage.getHeight(panel),panel);
            }
        }
    }
}

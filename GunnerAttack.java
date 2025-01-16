/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class manages a gunner shooting style attack with gaps in columns and 
 * boxes that the player must shoot or dodge. It initializes pillars, updates their positions, 
 * checks collisions with the player, and renders them in the game.
 */
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GunnerAttack extends AttackPattern {
    private long startTime;
    private int duration = 8000;
    private int damage = 20;

    // pillar and box sizes
    private int rectWidth = 50;
    private int rectSpeed = 4;
    private int gapBaseHeight = 130;
    private int boxSpacing = 5; 
    private int boxSize = 40;

    private ArrayList<Column> columns = new ArrayList<>();
    private Random rand = new Random();

    // constructor of attack pattern
    public GunnerAttack(GamePanel panel) {
        super(panel);
    }

    @Override
    // initializes the attack pattern by setting up all variables and columns
    public void initialize() {
        Rectangle box;
        int count;
        Column c;
        int boxY;
        int numBoxes;
        Rectangle smallBox;

        // clear any leftover bullets
        panel.getBullets().clear();
        panel.getPlayerBullets().clear();

        // reset the attack pattern variables
        finished = false;
        startTime = System.currentTimeMillis();
        columns.clear();
        box = panel.getBattleBox();
        count = 5; // base number of columns

        // if second phase, increase difficulty
        if (panel.getCycleCount() >= 1) {
            duration = 10000; 
            count = 10; 
            damage = 25;
            rectSpeed = 6;
        }

        // switch player image to jet image
        panel.getPlayer().setToShipImage();
    
        // create columns
        for (int i = 0; i < count; i++) {
            c = new Column();

            // set up column position to the right, so they come in sequentially
            c.x = box.x + box.width + i * 300;

            // randomized gap so the opening for the user moves around
            c.gapY = box.y + rand.nextInt(box.height - gapBaseHeight);

            // gap is larger than the player, to give the player room
            c.gapHeight = gapBaseHeight;

            // create collision hitboxes
            c.topRect = new Rectangle(c.x,box.y,rectWidth,c.gapY - box.y);
            c.bottomRect = new Rectangle(c.x,c.gapY + c.gapHeight,rectWidth,box.y + box.height - (c.gapY + c.gapHeight));

            c.boxes.clear();
            
            // create boxes in the gap
            numBoxes = 3;
            for (int b = 0; b < numBoxes; b++) {
                boxY = c.gapY + (b * (boxSize + boxSpacing));
                smallBox = new Rectangle(c.x + (rectWidth - boxSize)/2, boxY, boxSize, boxSize);
                c.boxes.add(smallBox);
            }

            columns.add(c);
        }
    }

    @Override
    // updates the logic each frame: moves pillars, checks collisions and other game logic
    public void execute() {
        long now;
        Rectangle box;
        Iterator<Column> it = columns.iterator();

        now = System.currentTimeMillis();
        box = panel.getBattleBox();

        it = columns.iterator();
        Column c;

        // define hitbox for collision detection
        Rectangle playerRect;

        Iterator<Bullet> pbit;
        Iterator<Rectangle> boxIt;
        Bullet pb;
        boolean consumed;
        Rectangle bRect;

        while (it.hasNext()) {
            c = it.next();

            // move the column left based on rectangle speed and boss modifier
            c.x -= (int) (rectSpeed * panel.getBossSpeedModifier());

            // update topRect and bottomRect in-place 
            c.topRect.x = c.x;
            c.topRect.height = c.gapY - box.y;
            c.bottomRect.x = c.x;
            c.bottomRect.y = c.gapY + c.gapHeight;
            c.bottomRect.height = box.y + box.height - (c.gapY + c.gapHeight);

            // update each small box's x-position
            for (Rectangle smallBox : c.boxes) {
                smallBox.x = c.x + (rectWidth - boxSize) / 2;
            }

            // define player hitbox for collision detection
            playerRect = panel.getPlayer().getHitbox();
            
            // if player hits topRect or bottomRect, damage them
            if (c.topRect.intersects(playerRect) || c.bottomRect.intersects(playerRect)) {
                panel.decreasePlayerHP(damage);

                // if player dies to attack, end game
                if (panel.getPlayer().isDead()) {
                    finished = true;
                    panel.getPlayerBullets().clear();
                    return;
                }
            }

            // check collision with the boxes in the gap
            for (Rectangle bR : c.boxes) {
                if (bR.intersects(playerRect)) {
                    panel.decreasePlayerHP(damage);

                    // if player dies to attack, end game
                    if (panel.getPlayer().isDead()) {
                        finished = true;
                        panel.getPlayerBullets().clear();
                        return;
                    }
                }
            }

            // remove the column if it has moved past the left boundary
            if (c.x + rectWidth < box.x - 200) {
                it.remove();
            }
        }

        // handle collisions between player's bullets and the attack's columns and boxes
        pbit = panel.getPlayerBullets().iterator();
        while (pbit.hasNext()) {
            pb = pbit.next();
            consumed = false;

            for (Column col : columns) {
                // check collisions with the small boxes in the gap
                boxIt = col.boxes.iterator();
                while (boxIt.hasNext()) {
                    bRect = boxIt.next();
                    if (bRect.intersects(pb.getBounds())) {
                        // remove just that small box and the bullet
                        boxIt.remove();
                        pbit.remove();
                        consumed = true;
                        break;
                    }
                }
                if (consumed) {
                    // bullet is gone, stop checking further
                    break;
                }

                // check collision with topRect
                if (col.topRect.intersects(pb.getBounds())) {
                    pbit.remove();
                    consumed = true;
                    break;
                }

                // check collision with bottomRect
                if (col.bottomRect.intersects(pb.getBounds())) {
                    pbit.remove();
                    consumed = true;
                    break;
                }
            }
        }

        // if the attack duration has passed and there are no remaining columns, end the attack
        if (now - startTime > duration && columns.isEmpty()) {
            finished = true;
            panel.getPlayerBullets().clear();
        }

        // if finished, revert player image back to the heart image
        if(finished){
            panel.getPlayer().resetToHeartImage();
        }
    }

    // draws the attack phase, including the columns and boxes within the columns
    public void drawAttack(Graphics g) {
        Rectangle box = panel.getBattleBox();

        g.setColor(Color.WHITE);

        for (Column c : columns) {
            // draw the top and bottom rectangles of the column
            g.fillRect(c.x, box.y, rectWidth, c.gapY - box.y);
            g.fillRect(c.x, c.gapY + c.gapHeight, rectWidth, box.y + box.height - (c.gapY + c.gapHeight));

            g.setColor(Color.BLUE);

            // draw the individual boxes within the column
            for (Rectangle br : c.boxes) {
                g.fillRect(br.x, br.y, br.width, br.height);
            }

            g.setColor(Color.WHITE);
        }
    }
}

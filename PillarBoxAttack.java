/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class manages a pillar-box style attack with columns and gaps
 * that the player must dodge. It initializes pillars, updates their positions, 
 * checks collisions with the player, and renders them in the game.
 */
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class PillarBoxAttack extends AttackPattern {
    private long startTime;
    private int duration = 8000;
    private int damage = 20;
    private int rectWidth = 60;
    private int rectSpeed = 4;

    private ArrayList<Column> columns = new ArrayList<>();
    private Random rand = new Random();

    // constructor of attack pattern
    public PillarBoxAttack(GamePanel panel) {
        super(panel);
    }

    @Override
    // initializes the attack pattern by setting up all variables and columns
    public void initialize() {
        Rectangle box;
        int count;
        Column c;
        int boxSize;
        int numBoxes;
        Rectangle br;

        // reset all the variables
        panel.getBullets().clear();
        panel.getPlayerBullets().clear();
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
        }

        panel.getPlayer().setToShipImage();

        for (int i = 0; i < count; i++) {
            c = new Column();

            // set up column position
            c.x = box.x + box.width + i * 300;
            c.gapY = box.y + rand.nextInt(box.height - 100);
            c.gapHeight = 100;

            c.boxes.clear();

            boxSize = 20;
            numBoxes = 4;

            // create hitboxes for rendering and collision detection
            for (int b = 0; b < numBoxes; b++) {
                br = new Rectangle(c.x, c.gapY + (b * (boxSize + 5)), boxSize, boxSize);
                c.boxes.add(br);
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

        // define all the hitboxes for collision detection
        Rectangle playerRect;
        Rectangle topRect;
        Rectangle bottomRect;

        Iterator<Bullet> pbit;
        Bullet pb;
        boolean consumed;
        Iterator<Rectangle> boxIt;
        Rectangle bRect;

        while (it.hasNext()) {
            c = it.next();

            // move the column left based on rectangle speed and boss modifier
            c.x -= (int) (rectSpeed * panel.getBossSpeedModifier());

            // update boxes's x to match column x
            for (Rectangle br : c.boxes) {
                br.x = c.x;
            }

            playerRect = panel.getPlayer().getHitbox();
            
            // define the top and bottom rectangles with gaps for the player to pass through
            topRect = new Rectangle(c.x, box.y, rectWidth, c.gapY - box.y);
            bottomRect = new Rectangle(c.x, c.gapY + c.gapHeight, rectWidth, box.y + box.height - (c.gapY + c.gapHeight));

            // check collision between player and columns
            if (topRect.intersects(playerRect) || bottomRect.intersects(playerRect)) {
                panel.decreasePlayerHP(damage);
                if (panel.getPlayer().isDead()) {
                    finished = true;
                    panel.getPlayerBullets().clear();
                    return;
                }
            }

            // check collision between player and individual boxes between the column
            for (Rectangle bR : c.boxes) {
                if (bR.intersects(playerRect)) {
                    panel.decreasePlayerHP(damage);
                    if (panel.getPlayer().isDead()) {
                        finished = true;
                        panel.getPlayerBullets().clear();
                        return;
                    }
                }
            }

            // remove the column if it has moved out of the battle arena
            if (c.x + rectWidth < box.x - 200) {
                it.remove();
            }
        }

        // handle collisions between player's bullets and the attack's columns and boxes
        pbit = panel.getPlayerBullets().iterator();
        while (pbit.hasNext()) {
            pb = pbit.next();
            consumed = false;

            for (int i = 0; i < columns.size() && !consumed; i++) {
                c = columns.get(i);

                topRect = new Rectangle(c.x, box.y, rectWidth, c.gapY - box.y);
                bottomRect = new Rectangle(c.x, c.gapY + c.gapHeight, rectWidth, box.y + box.height - (c.gapY + c.gapHeight));

                // check collision with top and bottom rectangle
                if (topRect.intersects(pb.getBounds()) || bottomRect.intersects(pb.getBounds())) {
                    pbit.remove();
                    consumed = true;
                    break;
                }

                boxIt = c.boxes.iterator();

                // check collision with individual boxes within the column
                while (boxIt.hasNext()) {
                    bRect = boxIt.next();
                    if (bRect.intersects(pb.getBounds())) {
                        boxIt.remove();
                        pbit.remove();
                        consumed = true;
                        break;
                    }
                }
            }
        }

        // if the attack duration has passed and there are no remaining columns, end the attack
        if (now - startTime > duration && columns.isEmpty()) {
            finished = true;
            panel.getPlayerBullets().clear();
        }

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
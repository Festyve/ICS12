/*
* Author: Michael Zhang and Bryan Yao
* Date: 2025-01-08
* Description: This class implements a  laser attack pattern for the boss character.
* It manages the spawning, behaviour, and timing of the lasers, that spawn randomly or track the player.
* The attack also changes based on the game's cycle count.
*/
import java.awt.*;

public class VerticalLaserAttack extends AttackPattern {
    private long startTime;
    private int step = 0;
    private int damage = 30;
    private boolean warning = true;
    private long phaseStartTime;
    private int[][] patterns;
    private boolean lasersActive = false;

    // a phase during the attack where the boss shoots singular lasers instead of groups of them
    private boolean singlePhaseActivated = false;

    // durations of each phase of the attack for organization
    private static final long FINAL_PHASE_DURATION = 10000; 
    private static final long FINAL_LASER_WARNING_TIME = 1000;
    private static final long NORMAL_WARNING_TIME = 1500;  
    private static final long NORMAL_ACTIVE_TIME  = 1000;  
    private static final long FAST_WARNING_TIME   = 500;  
    private static final long FAST_ACTIVE_TIME    = 500;  

    // variables for the final single phase of the attack (singlePhaseActivated)
    private boolean finalPhaseActive = false;
    private long finalPhaseStartTime = 0;
    private boolean finalLaserWarning = false; 
    private long finalLaserWarnStart = 0;
    private int finalLaserLane = 0;
    private boolean finalLaserActive = false;

    private int totalSteps = 4;
    private int lanesPerStep = 3;  // e.g. 3 columns lit each step
    private int laserCount = 6;  
    
    // constructor method
    public VerticalLaserAttack(GamePanel panel) {
        super(panel);
    }

    // method that initializes the attack pattern
    public void initialize() {
        panel.getBullets().clear(); // clear screen
        finished = false;
        startTime = System.currentTimeMillis();
        phaseStartTime = startTime;
        step = 0;
        warning = true;
        lasersActive = false;
        singlePhaseActivated = false;

        // if cycle >= 1, everything is faster (game gets harder as game goes on)
        if (panel.getCycleCount() >= 1) {
            damage += 10;
        }

        patterns = new int[totalSteps][lanesPerStep]; // invisible steps on the bottom of the board to figure out where the lasers attack
        for (int s = 0; s < totalSteps; s++) {
            for (int l = 0; l < lanesPerStep; l++) {
                patterns[s][l] = 1 + (int)(Math.random() * laserCount); // randomly selects where to spawn laser
            }
        }
    }

    public void execute() {
        // variables for tracking interval of time
        long now;
        long elapsed;
        long elapsedFinal;
        long wTime;
        long aTime;

        // variables for hitboxes
        Rectangle box;
        Rectangle laserRect;

        // variables for drawing the lasers
        int laserCount;
        int segmentWidth;

        // variables for drawing the current pattern
        int[] currentPattern;
        int lx;

        now = System.currentTimeMillis();

        // if all patterns have been used, transition to final single-laser phase
        if (step >= patterns.length) {
            // single phase for a short period if not already done
            if (!singlePhaseActivated) {
                singlePhaseActivated = true;
                finalPhaseActive = true;
                finalPhaseStartTime = now;
                // immediately spawn a laser warning
                spawnFinalLaserWarning(now);
            } 
            else if (finalPhaseActive) {
                // final single-laser phase
                elapsedFinal = now - finalPhaseStartTime;
                if (elapsedFinal > FINAL_PHASE_DURATION) {
                    // done after the FINAL_PHASE_DURATION
                    finished = true;
                } else {
                    handleFinalLaserLogic(now);
                }
            }
            return;
        }

        elapsed = now - phaseStartTime;

        // change how fast the game is for the main patterns depending on how long into the attack pattern the player is
        wTime = (panel.getCycleCount() >= 1) ? FAST_WARNING_TIME : NORMAL_WARNING_TIME;
        aTime = (panel.getCycleCount() >= 1) ? FAST_ACTIVE_TIME  : NORMAL_ACTIVE_TIME;

        if (warning) { // warning of laser
            if (elapsed > wTime) {
                warning = false;
                lasersActive = true;
                phaseStartTime = now;
            }
        } else if (lasersActive) {
            if (elapsed > aTime) {
                step++;
                if (step < patterns.length) {
                    warning = true;
                    lasersActive = false;
                    phaseStartTime = now;
                }
            } else {
                // actively draw lasers for the current pattern
                box = panel.getBattleBox();
                laserCount = 6;
                segmentWidth = box.width / laserCount;
                currentPattern = patterns[step];

                for (int i : currentPattern) {
                    lx = box.x + (i - 1) * segmentWidth;
                    laserRect = new Rectangle(lx, box.y, segmentWidth, box.height);

                    if (laserRect.intersects(panel.getPlayer().getHitbox())) {
                        panel.decreasePlayerHP(damage);
                        if (panel.getPlayer().isDead()) {
                            finished = true;
                            return;
                        }
                    }
                }
            }
        }
    }

    // handles the final single-laser logic over 10 seconds where code spawns a warning where the player is, waits, then spawns the laser and continue/repeat
    private void handleFinalLaserLogic(long now) {
        long laserActiveTime;

        if (finalLaserWarning) {
            // if in warning mode, check if enough time to shoot laser has passed
            if (now - finalLaserWarnStart >= FINAL_LASER_WARNING_TIME) {
                // convert to active laser
                finalLaserWarning = false;
                finalLaserActive = true;
            }
        }
        else if (finalLaserActive) {
            // consider the laser 'active' for half a second orr keep it active until a new warning is spawned
            laserActiveTime = 500; // half a second
            if (now - finalLaserWarnStart >= FINAL_LASER_WARNING_TIME + laserActiveTime) {
                // laser done, spawn next warning
                spawnFinalLaserWarning(now);
            }
            else {
                // check if user intersects
                doFinalLaserDamage();
            }
        }
    }

    // spawns a final laser warning where the user currently is.
    private void spawnFinalLaserWarning(long now) {
        Rectangle box;
        int laserCount;
        int segmentWidth;

        // variables for player's center
        Rectangle pHit;
        int pxCenter;

        // variables to clamp the player to the nearest lane
        int relativeX;
        int laneIndex;

        finalLaserWarning = true;
        finalLaserActive = false;
        finalLaserWarnStart = now;

        // figure out the lane where user is
        box = panel.getBattleBox();
        laserCount = 6;
        segmentWidth = box.width / laserCount;

        // find player's center X
        pHit = panel.getPlayer().getHitbox();
        pxCenter = pHit.x + pHit.width / 2;

        // clamp and spawn to the nearest lane
        relativeX = pxCenter - box.x;
        laneIndex = relativeX / segmentWidth; // 0..5
        if (laneIndex < 0) {
            laneIndex = 0;
        } else if (laneIndex >= laserCount) {
            laneIndex = laserCount - 1;
        }
        finalLaserLane = laneIndex + 1; // lane from 1..6
    }

    // damages the user if they are in the final laser
    private void doFinalLaserDamage() {
        Rectangle box;
        int laserCount;
        int segmentWidth;

        // variables for hitbox
        int lx;
        Rectangle laserRect;

        box = panel.getBattleBox();
        laserCount = 6;
        segmentWidth = box.width / laserCount;

        lx = box.x + (finalLaserLane - 1) * segmentWidth;
        laserRect = new Rectangle(lx, box.y, segmentWidth, box.height);

        if (laserRect.intersects(panel.getPlayer().getHitbox())) {
            panel.decreasePlayerHP(damage);
            if (panel.getPlayer().isDead()) {
                finished = true;
            }
        }
    }

    // draw the laser attacks
    public void drawAttack(Graphics g) {
        Rectangle box;
        int laserCount;
        int segmentWidth;

        // variables for drawing the attack pattern
        int[] currentPattern;
        int lx;

        box = panel.getBattleBox();
        laserCount = 6;
        segmentWidth = box.width / laserCount;

        if (step < patterns.length) {
            // normal phase
            currentPattern = patterns[step];

            if (warning) {
                g.setColor(Color.YELLOW);
                for (int lane : currentPattern) {
                    lx = box.x + (lane - 1) * segmentWidth;
                    g.fillRect(lx, box.y + box.height - 20, segmentWidth, 20);
                }
            } else if (lasersActive) {
                g.setColor(Color.WHITE);
                for (int lane : currentPattern) {
                    lx = box.x + (lane - 1) * segmentWidth;
                    g.fillRect(lx, box.y, segmentWidth, box.height);
                }
            }
        } else {
            // the single-laser approach
            if (finalPhaseActive) {
                if (finalLaserWarning) {
                    // draw a small rectangle at the bottom to show where the laser will appear as a warning
                    g.setColor(Color.YELLOW);
                    lx = box.x + (finalLaserLane - 1) * segmentWidth;
                    g.fillRect(lx, box.y + box.height - 20, segmentWidth, 20);
                }
                else if (finalLaserActive) {
                    // draw the actual laser
                    g.setColor(Color.WHITE);
                    lx = box.x + (finalLaserLane - 1) * segmentWidth;
                    g.fillRect(lx, box.y, segmentWidth, box.height);
                }
            }
        }
    }
}
/*
* Author: Michael Zhang and Bryan Yao
* Date: 2025-01-16
* Description: This class manages rendering, user input, and core game logic across 
* various states such as menus, instructions, settings, and battles. It handles difficulty 
* selection, menu navigation, combat interactions, etc. It is constantly updating game entities and 
* playing sounds. The class ensures smooth gameplay by processing keyboard events, updating 
* animations, and managing resources like images and audio.
*/
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;

public class GamePanel extends JPanel implements KeyListener, ActionListener {
    //short enum that defines all possible states/screens in the game
    public enum State {
    MAIN_MENU,
    INTRO,
    PLAYER_MENU,
    PLAYER_FIGHT_TIMING,
    PLAYER_ACT,
    PLAYER_ITEM_SELECT,
    DIALOG,
    BOSS_ATTACK,
    WIN,
    LOSE,
    INSTRUCTIONS,
    SETTINGS
    }

    // variables for the text of different difficulties the player can use
    private String[] difficulties = {"EASY", "HARD", "CHEAT"};
    private String[] difficultyDescription = {
        "Easy Mode (300 HP) - Great for beginners!",
        "Hard Mode (100 HP) - A Challenge!",
        "Mr. Anthony Mode - A MILLION HP."
    };

    // track which difficulty is being used
    private int difficultyIdx = 0;
    // the HP values for each mode
    private int[] difficultyHP = {300, 100, 1000000};

    // keeps track of the current state of the game
    private State currentState = State.MAIN_MENU;

    // image fields used for drawing various UI elements, background, boss, player, etc
    private BufferedImage exclamationImage;
    private BufferedImage bossImage;
    private BufferedImage playerImage; 
    private BufferedImage flashImage;  
    private BufferedImage backgroundImage;
    private BufferedImage jetImage;
    private BufferedImage jetFlashImage;
    private Image fightButtonImage;
    private Image actButtonImage;
    private Image itemButtonImage;
    private Image mercyButtonImage;
    private Image fightHoverImage;
    private Image actHoverImage;
    private Image itemHoverImage;
    private Image mercyHoverImage;
    private Image fightBarImage;
    private Image cursorImage;
    private Image bulletImage;
    private Image homingImage;
    private Image laserImage;
    private Image laserWarningImage;
    private Image winImage;
    private Image loseImage;

    // variables for scaled images to avoid rescaling every frame, reduce lag
    private Image fightButtonScaled;
    private Image fightHoverScaled;
    private Image actButtonScaled;
    private Image actHoverScaled;
    private Image itemButtonScaled;
    private Image itemHoverScaled;
    private Image mercyButtonScaled;
    private Image mercyHoverScaled;
    private Image backgroundScaled;

    // Strings to store the label of menu options for the main "battle" menu
    private final String[] MENUOPTIONS = {"FIGHT", "ACT", "ITEM", "QUIT"};
    private int selectedOption = 0; // track the currently selected menu option

    // sub-menu options for ACT actions
    private final String[] ACTOPTIONS = {"CHECK CODE", "JOKE", "TECH SUPPORT", "ORZ"};
    private int selectedActOption = 0; // track which ACT option is selected

    // variables for game entities
    private Player player;
    private Boss boss;

    // fonts used throughout the game for dialogs, menus, etc.
    private Font customFont;
    private Font dialogFont = new Font("Monospaced", Font.PLAIN, 32);
    private Font menuFont = new Font("Monospaced", Font.BOLD, 36);
    private Font uiFont = new Font("Monospaced", Font.PLAIN, 28);

    // variables for kirby animation
    private Image kirbyImage;     

    // variable that holds the text that appears in the dialog box
    private String dialogText = "";

    // variables for the fight bar data (used for the "timing" minigame during FIGHT)
    private int fightBarWidth = 400;
    private int fightMarkerX;
    private boolean fightMarkerMovingRight = true;
    private boolean fightKeyPressed = false;
    private int targetStartX;
    private int targetWidth = 100; // the "perfect" hit zone's width

    // variables for the damage popup data (the floating damage text above the boss after being hit)
    private boolean showDamagePopup = false;
    private String damageText = "";
    private int damagePopupTimer = 0;
    private int damagePopupX;
    private int damagePopupY;

    // variables for tracking movement keys for the player (arrow keys)
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;

    // a list of AttackPatterns (boss phases/patterns). The boss cycles through them.
    private ArrayList<AttackPattern> attackPatterns = new ArrayList<>();
    private int currentAttackPattern = 0; // tracks which pattern is active

    // variables for rectangles representing the hitboxes for the battle area and bottom UI box
    private Rectangle battleBox;
    private Rectangle bottomBox;

    // lists of bullets (boss bullets and player bullets)
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Bullet> playerBullets = new ArrayList<>();

    // the main timer for our game loop
    private Timer gameTimer;

    // concrete attack patterns (instances for each pattern type)
    private MovingGapAttack mga;
    private RandomProjectileAttack rpa;
    private HomingProjectileAttack hpa;
    private VerticalLaserAttack vla;
    private GunnerAttack ga;

    // variables for exclamation mark usage (for homing warnings, etc.)
    private boolean showExclamationMark = false;
    private int exclamationX = 0;
    private int exclamationY = 0;

    // variables for adjustments to boss or player difficulty
    public float bossSpeedModifier = 1.0f;
    public float bossDamageModifier = 1.0f;
    private float playerDamageModifier = 1.0f;

    // track whether or not the player's turn ended (used to trigger the boss phase next)
    private boolean playerTurnEnded = false;

    // track how many cycles of attack patterns have been completed.
    private int cycleCount = 0;

    // variables for instruction screen page tracking
    private int instructionPage = 1;
    private final int TOTAL_INSTRUCTION_PAGES = 7;

    // temporary flag to indicate if the player has a boosted damage effect
    private boolean tempDamageBoostActive = false;

    // music player variable
    public static Clip clip;
    public static Clip effectClip;

    // constructor for the gamepnael, sets up various listeners, initializes timer, fonts, images, etc
    public GamePanel() {
        // set up the panel's appearance
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        setDoubleBuffered(true);

        // timer for updates
        gameTimer = new Timer(15, this);
        gameTimer.start();

        // resizes or sets up the game layout when the panel is shown or resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                initializeLayout();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                // re-scale the background one time here (rather than per frame)
                if (backgroundImage != null && getWidth() > 0 && getHeight() > 0) {
                    backgroundScaled = backgroundImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                }
                initializeLayout();
            }
        });

        // load the custom font and set the fonts
        loadCustomFont();
        setFonts();

        // load images, resources, sound, etc.
        loadResourcesInConstructor();
        initializeLayout();
        createEntities();
        initializeAttackPatterns();
        playMusic("Sounds/menumusic.wav");

        // the initial text in the dialog
        dialogText = "YOU ENCOUNTERED GREBORY ANTONY.";

        setCurrentState(State.MAIN_MENU);
    }

    // utility method to resize a BufferedImage (e.g., for scaling down/up images).
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image tmp;
        BufferedImage resized;
        Graphics g2d;

        // create a temporary scaled instance
        tmp = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    // plays music in a loop, at a certain volume, sourced from https://www.geeksforgeeks.org/play-audio-file-using-java/
    public static void playMusic(String track) {
        File audioFile;
        AudioInputStream audioStream;

        // ensure music is stopped
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop(); // stop the currently playing clip
            }
            clip.close(); // close the clip to free resources
            clip = null; // set clip to null to indicate it's not in use
        }

        try {
            // specify the path to the audio file
            audioFile = new File(track);
            // create an AudioInputStream object from the audio file
            audioStream = AudioSystem.getAudioInputStream(audioFile);
            // get a clip resource
            clip = AudioSystem.getClip(); // assign the new clip to the class-level variable
            // open the audio stream for playback
            clip.open(audioStream);
            // loop the clip indefinitely
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            // start playing the audio
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing the audio file: " + e.getMessage());
        }
    }

    // plays the sound effect
    static void playSoundEffect(String file){
        File soundFile;
        AudioInputStream audioInput;
        Clip clip;
        try {
            soundFile = new File(file);  // specify the path to the audio file
            audioInput = AudioSystem.getAudioInputStream(soundFile);
            clip = AudioSystem.getClip();
            clip.open(audioInput); // open the audio stream for playback
            clip.start(); // start playing the audio
            effectClip = clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Error playing sound file:" + e.getMessage());
        }
    }

    // loads images and other resources used in the game (buttons, background, etc.).
    private void loadResourcesInConstructor() {
        // variables for temporary loading of the images
        Image tempImage;
        Image tempCursor;
        ImageIcon kirbyIcon;
        BufferedImage originalPlayerImage;
        BufferedImage originalFlashImage;
        BufferedImage originalBulletImage;
        BufferedImage originalJet;
        BufferedImage originalJetFlash;

        // fight bar
        try {
            tempImage = ImageIO.read(getClass().getResource("/Images/attackbar.png"));
            fightBarImage = tempImage;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // cursor (used in the fight bar timing minigame)
        try {
            tempCursor = ImageIO.read(getClass().getResource("/Images/attackcursor.gif"));
            cursorImage = tempCursor;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // exclamation mark (used for warnings like homing attacks)
        try {
            exclamationImage = ImageIO.read(getClass().getResource("/Images/exclamation.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // kirby animation for the main menu
        try {
            kirbyIcon = new ImageIcon(getClass().getResource("/Images/kirby.gif"));
            kirbyImage = kirbyIcon.getImage();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // boss image
        try {
            bossImage = ImageIO.read(getClass().getResource("/Images/mranthony.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // win screen image
        try {
            winImage = ImageIO.read(getClass().getResource("/Images/win.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // lose screen image
        try {
            loseImage = ImageIO.read(getClass().getResource("/Images/lose.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // player's normal heart image
        try {
            originalPlayerImage = ImageIO.read(getClass().getResource("/Images/heart.png"));
            playerImage = resizeImage(originalPlayerImage, 25, 25);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // player's flash heart (used for a "hit" or "flash" animation)
        try {
            originalFlashImage = ImageIO.read(getClass().getResource("/Images/whiteheart.png"));
            flashImage = resizeImage(originalFlashImage, 25, 25);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // background image
        try {
            backgroundImage = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // basic bullet image (fireball)
        try {
            originalBulletImage = ImageIO.read(getClass().getResource("/Images/fireball.png"));
            bulletImage = resizeImage(originalBulletImage, 15, 15);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // homing bullet image
        try {
            homingImage = ImageIO.read(getClass().getResource("/Images/homingbullet.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // laser image
        try {
            laserImage = ImageIO.read(getClass().getResource("/Images/laser.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // laser warning image
        try {
            laserWarningImage = ImageIO.read(getClass().getResource("/Images/laserstart.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // button & hover images for FIGHT
        try {
            fightButtonImage = ImageIO.read(getClass().getResource("/Images/fight.png"));
            fightHoverImage = ImageIO.read(getClass().getResource("/Images/fight_hover.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // button & hover images for ACT
        try {
            actButtonImage = ImageIO.read(getClass().getResource("/Images/act.png"));
            actHoverImage = ImageIO.read(getClass().getResource("/Images/act_hover.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // button & hover images for ITEM
        try {
            itemButtonImage = ImageIO.read(getClass().getResource("/Images/item.png"));
            itemHoverImage = ImageIO.read(getClass().getResource("/Images/item_hover.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // button & hover images for MERCY
        try {
            mercyButtonImage = ImageIO.read(getClass().getResource("/Images/mercy.png"));
            mercyHoverImage = ImageIO.read(getClass().getResource("/Images/mercy_hover.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // jet & jet flash images
        try {
            originalJet = ImageIO.read(getClass().getResource("/Images/jet.png"));
            jetImage = resizeImage(originalJet, 25, 25);
            originalJetFlash = ImageIO.read(getClass().getResource("/Images/whitejet.png"));
            jetFlashImage = resizeImage(originalJetFlash, 25, 25);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fightButtonScaled = fightButtonImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
        fightHoverScaled = fightHoverImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
        actButtonScaled = actButtonImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
        actHoverScaled = actHoverImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
        itemButtonScaled = itemButtonImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
        itemHoverScaled = itemHoverImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
        mercyButtonScaled = mercyButtonImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
        mercyHoverScaled = mercyHoverImage.getScaledInstance(200, 80, Image.SCALE_SMOOTH);
    }

    // loads a custom font (PixelOperator8-Bold.ttf) from the resources folder, if present.
    private void loadCustomFont() {
        InputStream fontStream;

        try {
            fontStream = getClass().getResourceAsStream("/PixelOperator8-Bold.ttf");
            if (fontStream != null) {
                customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            } else {
                System.err.println("Font file not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // applies the custom font (if loaded). otherwise, leaves the default fonts.
    private void setFonts() {
        if (customFont != null) {
            dialogFont = customFont.deriveFont(Font.PLAIN, 32f);
            menuFont = customFont.deriveFont(Font.BOLD, 36f);
            uiFont = customFont.deriveFont(Font.PLAIN, 28f);
        }
    }

    // initializes the attack pattern list, each pattern is a distinct type of obstacle arrangement
    private void initializeAttackPatterns() {
        attackPatterns.clear(); // reset all the attack patterns

        // create instances of each pattern
        rpa = new RandomProjectileAttack(this, bulletImage);
        mga = new MovingGapAttack(this);
        hpa = new HomingProjectileAttack(this, homingImage);
        vla = new VerticalLaserAttack(this, laserImage, laserWarningImage);
        ga = new GunnerAttack(this);

        attackPatterns.add(mga);
        attackPatterns.add(rpa);
        attackPatterns.add(hpa);
        attackPatterns.add(vla);
        attackPatterns.add(ga);
    }

    // set up the layout sizes for the boss sprite, the battle box, and the bottom UI box depending on current state
    private void initializeLayout() {
        int panelWidth = getWidth();
        int bossWidth = 150;
        int bossHeight = 150;
        int bossX = (panelWidth - bossWidth) / 2;
        int bossY = 50;
        int boxWidth;
        int boxHeight;
        int bottomBoxWidth;
        int bottomBoxHeight;
        int boxX;
        int boxY;
        int bottomBoxY;

        // place the boss in the upper center
        if (boss != null) {
            boss.getRect().x = bossX;
            boss.getRect().y = bossY;
            boss.getRect().width = bossWidth;
            boss.getRect().height = bossHeight;
        }

        // decide the size of the battle box and bottom box based on state
        switch (currentState) {
            case DIALOG:
            case INTRO:
            case PLAYER_MENU:
            case PLAYER_ITEM_SELECT:
            case PLAYER_ACT:
            default:
                boxWidth = 400;
                boxHeight = 220;
                bottomBoxWidth = 1100;
                bottomBoxHeight = 210;
                break;

            case BOSS_ATTACK:
                boxWidth = 600;
                boxHeight = 400;
                bottomBoxWidth = panelWidth - 100;
                bottomBoxHeight = 140;
                break;

            case PLAYER_FIGHT_TIMING:
                boxWidth = 300;
                boxHeight = 200;
                bottomBoxWidth = getWidth() - 100;
                bottomBoxHeight = 220;
                break;
        }

        boxX = (panelWidth - boxWidth) / 2;
        boxY = bossY + bossHeight + 10;
        battleBox = new Rectangle(boxX, boxY, boxWidth, boxHeight);

        bottomBoxY = boxY + boxHeight + 110; 
        bottomBox = new Rectangle((panelWidth - bottomBoxWidth) / 2, bottomBoxY, bottomBoxWidth, bottomBoxHeight);

        // position the fight minigame marker
        fightMarkerX = panelWidth / 2 - fightBarWidth / 2;
        fightMarkerMovingRight = true;
        targetStartX = (panelWidth / 2 - fightBarWidth / 2) + (fightBarWidth / 2 - 50);
    }

    // create the player and boss entities, setting their initial positions and properties
    private void createEntities() {
        int playerX;
        int playerY;

        // if battleBox is null, we cannot place the player yet
        if (battleBox == null) {
            return;
        }
        
        // configure the position of the player
        playerX = battleBox.x + battleBox.width / 2;
        playerY = battleBox.y + battleBox.height / 2;
        if(playerImage != null) {
            playerX -= (playerImage.getWidth())/2;
        } else {
            playerX -= 25/2;
        }
        if(playerImage != null) {
            playerX -= (playerImage.getHeight())/2;
        } else {
            playerX -= 25/2;
        }

        // create the Player object and configure properties
        player = new Player(playerX, playerY, 25, 25, difficultyHP[difficultyIdx], playerImage, flashImage, playerImage, flashImage, jetImage, jetFlashImage);

        // create the Boss object and configure properties
        boss = new Boss("GREBBORY ANTONY", 100, bossImage, 0, 0, 150, 150);

        // initialize player's inventory (this might add items, potions, etc.)
        player.initializeInventory();
    }

    // resets the game to a fresh state, used after losing/winning or returning to main menu
    private void resetGame() {
        int playerStartX;
        int playerStartY;

        playerStartX = battleBox.x + battleBox.width / 2 - 12;
        playerStartY = battleBox.y + battleBox.height / 2 - 12;

        // reset all the variables, creating new instances of all objects and clearing all lists
        player = new Player(playerStartX, playerStartY, 25, 25, difficultyHP[difficultyIdx], playerImage, flashImage,  playerImage, flashImage, jetImage, jetFlashImage);
        player.initializeInventory();
        boss = new Boss("GREBBORY ANTONY", 100, bossImage, 0, 0, 150, 150);
        currentAttackPattern = 0;
        dialogText = "YOU ENCOUNTERED GREBBORY ANTONY.".toUpperCase();
        bullets.clear();
        playerBullets.clear();
        bossSpeedModifier = 1.0f;
        bossDamageModifier = 1.0f;
        playerDamageModifier = 1.0f;
        cycleCount = 0;
        initializeAttackPatterns();
    }

    // allows AttackPatterns to control the exclamation mark for warnings
    public void setExclamationState(AttackPattern attack, boolean show, int x, int y) {
        if (attack instanceof HomingProjectileAttack) {
            showExclamationMark = show;
            exclamationX = x;
            exclamationY = y;
        }
    }

    // paints the entire game scene depending on the current state of the game
    @Override
    protected void paintComponent(Graphics g) {
        State localState;

        super.paintComponent(g);

        // a local copy of currentState to avoid concurrency issues
        localState = currentState;

        // draw main menu
        if (localState == State.MAIN_MENU) {
            drawMainMenu(g);
            return;
        }

        // draw instructions
        if (localState == State.INSTRUCTIONS) {
            drawInstructions(g);
            return;
        }

        // draw settings screen
        if (localState == State.SETTINGS) {
            drawSettings(g);
            return;
        }

        // draw background
        g.drawImage(backgroundScaled, 0, 0, this);

        // draw common UI (boss HP, etc.) for game states (except the main menu)
        drawBossUI(g);
        drawBattleBox(g);
        drawPlayerHPBar(g);

        // handle states where player actions or attacks are relevant
        if (localState == State.PLAYER_MENU ||
            localState == State.PLAYER_ITEM_SELECT ||
            localState == State.BOSS_ATTACK ||
            localState == State.PLAYER_FIGHT_TIMING ||
            localState == State.PLAYER_ACT) {

            // for BOSS_ATTACK: show bullets, player, etc.
            if (localState == State.BOSS_ATTACK) {
                player.draw(g);     // draw the player's heart
                drawBossAttack(g);  // draw bullets, special patterns, etc.
            }

            // draw the main player options (FIGHT, ACT, ITEM, QUIT)
            if (localState == State.PLAYER_MENU) {
                drawMenu(g);
            }

            // draw the FIGHT bar minigame
            if (localState == State.PLAYER_FIGHT_TIMING) {
                drawFightBar(g);
            }

            // draw the ACT sub-menu
            if (localState == State.PLAYER_ACT) {
                drawActSubMenu(g);
            }

            // draw the ITEM selection sub-menu
            if (localState == State.PLAYER_ITEM_SELECT) {
                drawItemSelect(g);
            }
        }

        // for Intro or Dialog states: show the bottom text box with dialog
        if (localState == State.INTRO || localState == State.DIALOG) {
            drawDialog(g);
        }

        // draw respective win or lose overlays
        if (localState == State.WIN) {
            drawWinScreen(g);
        }
        if (localState == State.LOSE) {
            drawLoseScreen(g);
        }

        // show floating damage popup if needed
        drawDamagePopup(g);

        // draw player bullets
        for (Bullet pb : playerBullets) {
            pb.draw(g, this);
        }

        // draw exclamation mark warnings (e.g., homing bullet warning)
        if (showExclamationMark) {
            drawExclamationMarks(g);
        }
    }

    // draw exclamation images for warnings
    private void drawExclamationMarks(Graphics g) {
        Graphics g2d;

        if (exclamationImage == null || battleBox == null) {
            return;
        }

        g2d = (Graphics2D) g.create();
        g2d.drawImage(exclamationImage,exclamationX - exclamationImage.getWidth(this) / 2,exclamationY - exclamationImage.getHeight(this) / 2,this);
        g2d.dispose();
    }


    // draw the HP bar for the player near the bottom of the screen
    private void drawPlayerHPBar(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // variables for drawing the bar
        int centerX;
        int baseY;
        FontMetrics fm;
        String hpLabel;
        String hpValues;
        int labelW;
        int valuesW;
        int barWidth;
        int barHeight;
        int gap;
        int totalWidth;
        int leftX;

        // variables for drawing black box behind the bar
        int barX;
        int barY;

        // variables for drawing the portion of HP bar filled
        double ratio;
        int fillWidth;
        int valuesX;

        g2d.setFont(uiFont);
        centerX = getWidth() / 2;
        baseY = bottomBox.y - 30;
        fm = g2d.getFontMetrics();
        hpLabel = "HP";
        hpValues = player.getHP() + "/" + player.getMaxHP();
        labelW = fm.stringWidth(hpLabel);
        valuesW = fm.stringWidth(hpValues);
        barWidth = 50;
        barHeight = fm.getAscent();
        gap = 8;
        totalWidth = labelW + gap + barWidth + gap + valuesW;
        leftX = centerX - (totalWidth / 2);

        // drawing the HP text
        g2d.setColor(Color.WHITE);
        g2d.drawString(hpLabel, leftX, baseY);

        // drawing the black box behind the bar
        barX = leftX + labelW + gap;
        barY = baseY - barHeight;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(barX, barY, barWidth, barHeight);

        // filling the portion of the bar based on ratio (HP / MaxHP)
        ratio = (double) player.getHP() / player.getMaxHP();
        fillWidth = (int) (barWidth * ratio);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(barX, barY, fillWidth, barHeight);

        // draw "NN/NN" text
        valuesX = barX + barWidth + gap;
        g2d.setColor(Color.WHITE);
        g2d.drawString(hpValues, valuesX, baseY);

        g2d.dispose();
    }

    // draws the main menu with the title, credits, options to start, etc
    private void drawMainMenu(Graphics g) {
        Graphics2D g2d;

        // variables for title lines;
        String line1;
        String line2;
        FontMetrics fm;
        int line1X;
        int line1Y;
        int line2X;
        int line2Y;
        
        // variables for the credits
        String credits;
        int creditsX;
        int creditsY;

        int kirbySize; // holds the size of the kirby animation

        // variables for drawing the main menu options
        String[] options;
        int startY;
        String option;
        int x;
        int y;

        g2d = (Graphics2D) g;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setFont(menuFont.deriveFont(Font.BOLD, 42f));
        g2d.setColor(Color.WHITE);

        // title lines
        line1 = "GREB'S (GREBBORY)";
        line2 = "GREGARIOUS GAME!";
        fm = g2d.getFontMetrics();
        line1X = getWidth() / 2 - fm.stringWidth(line1) / 2;
        line1Y = 80;
        g2d.drawString(line1, line1X, line1Y);

        line2X = getWidth() / 2 - fm.stringWidth(line2) / 2;
        line2Y = line1Y + 50;
        g2d.drawString(line2, line2X, line2Y);

        // credits
        g2d.setFont(menuFont.deriveFont(Font.PLAIN, 24f));
        fm = g2d.getFontMetrics();
        credits = "By Michael Zhang and Bryan Yao for ICS4U";
        creditsX = getWidth() / 2 - fm.stringWidth(credits) / 2;
        creditsY = line2Y + 60;
        g2d.drawString(credits, creditsX, creditsY);

        // drawing the kirby animation on the right side
        if (kirbyImage != null) {
            kirbySize = 350;
            g2d.drawImage(kirbyImage,getWidth() - kirbySize - 70,getHeight() / 3 + 50,kirbySize, kirbySize,this);
        }

        // drawing the main menu options
        g2d.setFont(menuFont.deriveFont(Font.BOLD, 36f));
        fm = g2d.getFontMetrics();

        options = new String[]{"[Z] START", "[I] INSTRUCTIONS", "[S] SETTINGS", "[Q] QUIT"};
        startY = 350; 
        for (int i = 0; i < options.length; i++) {
            option = options[i];
            x = 90;
            y = startY + i * 90;
            g2d.setColor(Color.WHITE);
            g2d.drawString(option, x, y);
        }
    }

    // draws the instructions screen with multiple pages and instructions for navigation
    private void drawInstructions(Graphics g) {
        String[] instructionsPages;
        String navPrompt;
        int navW;

        g.setFont(uiFont);
        g.setColor(Color.WHITE);
    
        // define instruction text for each page
        instructionsPages = new String[TOTAL_INSTRUCTION_PAGES];
        instructionsPages[0] = 
            "Instructions - Page 1:\n\n" +
            "General:\n" +
            "- All text is wrapped. This is because on\n" +
            "different displays, the text formatting\n" + 
            "may vary.\n" +
            "- Use Arrow Keys to Move.\n" +
            "- Press [Z] to Confirm Actions.\n" +
            "- Press [X] to Cancel.\n" +
            "- Use Items Wisely!\n" +
            "- Defeat Grebbory Antony!\n" + 
            "- HP of the boss and player are displayed at the \n" + 
            "top and middle of the screen.\n" +
            "- If the player receives damage from an \n" + 
            "attack, they will get 0.5 seconds of \n" + 
            "invincibility where they take 0 damage \n" + 
            "(shown with a flashing sprite).\n";
        
            instructionsPages[1] = 
            "Instructions - Page 2:\n\n" + 
            "Player:\n" +
            "- When the player becomes a jet,\n" +
            "Press [X] to shoot bullets. \n"+
            "These bullets will destroy the upcoming \n" + 
            "blue boxes so you can go through the gaps \n" + 
            "in the rectangle without taking damage.\n" +
            "- When the game starts, you are prompted \n" +
            "with a menu with 4 options: \n" +
            "FIGHT, ACT, ITEM, MERCY.\n";
    
        // Page 3: FIGHT Option Details
        instructionsPages[2] = 
            "Instructions - Page 3:\n\n" +
            "FIGHT:\n" +
            "- If FIGHT is chosen, a minigame appears on the\n" + 
            "screen with a moving white bar.\n" +
            "- Stop the moving white bar (press [Z]) as \n" + 
            "close as you can to the middle to deal more\n" + 
            "damage to the boss.\n";
    
        // Page 4: ACT Options Details
        instructionsPages[3] = 
            "Instructions - Page 4:\n\n" +
            "ACT:\n" +
            "- If ACT is chosen, you're prompted with 4 \n" +
            "actions you can perform.\n" +
            "- The consequence of each option can be \n" + 
            "positive, negative, or neutral.\n" +
            "- Play the game to figure out what each \n" + 
            "action does!\n\n";
    
        // Page 5: ITEM Options Details
        instructionsPages[4] = 
            "Instructions - Page 5:\n\n" +
            "ITEM:\n" +
            "- If ITEM is chosen, you will see one of the \n" +
            "three possible items in your inventory.\n" +
            "- The number of items in your inventory and\n" + 
            "the types of items you see cannot be changed.\n" +
            "- Each item provides a different buff to the \n" + 
            "player, which is shown through the description.\n" +
            "- If you use an item when there's none of it \n" + 
            "left, it will start the next attack and waste\n" +
            "your turn. Don't be dumb!";

        // Page 6: MERCY Option and Boss Details
        instructionsPages[5] = 
            "Instructions - Page 6:\n\n" +
            "MERCY:\n" +
            "- If MERCY is chosen, the player forfeits the\n"+
            "fight and will return to the main menu.\n" +
            "- After any of these options (but MERCY),\n" + 
            "the player's turn ends and then it's the \n" + 
            "boss's turn.\n\n";
        
        // Page 7: Boss Details  
        instructionsPages[6] = 
            "Instructions - Page 7:\n\n" +
            "Boss:\n" +
            "- The boss has 5 attack patterns, and each \n" +
            "turn the boss chooses 1 to execute.\n" +
            "- The order in which the boss selects each \n" + 
            "attack pattern remains the same.\n" +
            "- After the boss has cycled through all 5 \n" + 
            "attack patterns, each attack pattern \n" + 
            "becomes harder with projectiles dealing \n" +
            "more damage and/or becoming faster.\n" +
            "- The game ends when the player chooses \n" + 
            "mercy, or if the player/boss reaches 0 HP.\n" +
            "- In the boss's laser phase, try not to\n" + 
            "stay right above the blue sprites at the\n" + 
            "bottom of the screen!";
    
        // ensure instructionPage is within valid bounds
        instructionPage = Math.max(1, Math.min(instructionPage, TOTAL_INSTRUCTION_PAGES));
    
        // draw the current instruction page with text wrapping
        drawStringWrapped(g, instructionsPages[instructionPage - 1], 100, 100, getWidth() - 200);
    
        // navigation prompt at the bottom
        navPrompt = "[LEFT/RIGHT] to cycle | [Z] BACK TO MAIN MENU";
        navW = g.getFontMetrics().stringWidth(navPrompt);
        g.drawString(navPrompt, getWidth() / 2 - navW / 2, getHeight() - 100);
    }

    // draws the settings screen
    private void drawSettings(Graphics g) {
        // variables for instructions
        String adjustText;
        int adjW;

        // variables for text in settings
        String title;
        String displayText;
        int titleWidth;
        int baseY;
        int spacing;
        FontMetrics fm;

        g.setFont(uiFont);
        g.setColor(Color.WHITE);

        // draw the title
        title = "Select Difficulty:";
        g.setFont(g.getFont().deriveFont(40f));
        fm = g.getFontMetrics();
        titleWidth = fm.stringWidth(title);
        g.drawString(title, getWidth()/2 - titleWidth/2, 150);

        baseY = getHeight()/2 - 50;
        spacing = 80;
        g.setFont(g.getFont().deriveFont(26f));
        for(int i = 0; i < difficulties.length; i++){
            displayText = difficulties[i] + ": " + difficultyDescription[i];
            if(i == difficultyIdx){
                // highlight current difficulty in yellow
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            // draw the option and description
            g.drawString(displayText, 140, baseY + i * spacing);
        }     

        // show instructions
        g.setColor(Color.WHITE);
        adjustText = "[UP/DOWN] SWITCH, [Z] CONFIRM, [X] BACK";
        adjW = g.getFontMetrics().stringWidth(adjustText);
        g.drawString(adjustText, getWidth() / 2 - adjW / 2, getHeight() / 2 + 300);
    }

    // draws the boss UI (HP) and its image at the top
    private void drawBossUI(Graphics g) {
        boss.drawUI(g, uiFont, getWidth()); // draws the boss HP and name
        boss.drawImage(g, getWidth(), getHeight()); // draws the boss image at the assigned coordinates
    }

    // draws a flotaing damage popup when the boss takes damage
    private void drawDamagePopup(Graphics g) {
        if (showDamagePopup) {
            g.setFont(uiFont.deriveFont(Font.BOLD, 32f));
            g.setColor(Color.RED);
            g.drawString(damageText, damagePopupX, damagePopupY);
        }
    }

    // draws the battle box, where the player moves during attacks
    private void drawBattleBox(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawRect(battleBox.x, battleBox.y, battleBox.width, battleBox.height);
        g.drawRect(battleBox.x+1, battleBox.y+1, battleBox.width-2, battleBox.height-2);

        g.setColor(Color.BLACK);
        g.fillRect(battleBox.x+2, battleBox.y+2, battleBox.width-3, battleBox.height-3);
    }

    // drwas the dialog box
    private void drawDialog(Graphics g) {
        // variables for wrapping the dialog text
        int textX;
        int textY;

        // variables for drawing the prompt
        String prompt;
        FontMetrics fm;
        int promptWidth;

        g.setColor(Color.WHITE);
        g.drawRect(bottomBox.x, bottomBox.y, bottomBox.width, bottomBox.height);
        g.setColor(Color.BLACK);
        g.fillRect(bottomBox.x + 1, bottomBox.y + 1, bottomBox.width - 1, bottomBox.height - 1);

        g.setFont(dialogFont);
        g.setColor(Color.WHITE);

        // wrap the dialog text if it's too long
        textX = bottomBox.x + 20;
        textY = bottomBox.y + 60;
        drawStringWrapped(g, dialogText, textX, textY, bottomBox.width - 40);

        // prompt to press [Z] to continue
        prompt = "[Z]";
        fm = g.getFontMetrics();
        promptWidth = fm.stringWidth(prompt);
        g.drawString(prompt, bottomBox.x + bottomBox.width - promptWidth - 20, bottomBox.y + bottomBox.height - 20);
    }

    // draws the bottom menu with the four buttons (FIGHT, ACT, ITEM, QUIT)
    private void drawMenu(Graphics g) {
        Graphics2D g2d;

        // variables for storing the button images and hover images
        Image[] normalImages;
        Image[] hoverImages;

        // variables for formatting the buttons
        int buttonWidth;
        int buttonHeight;
        int spacing;
        int count;
        int totalWidth;
        int startX;
        int centerY;
        int x;
        int y;
        Image toDraw;

        // variables for prompt on navgiation
        String prompt;
        int promptWidth;

        // use pre-scaled images
        normalImages = new Image[]{fightButtonScaled, actButtonScaled, itemButtonScaled, mercyButtonScaled};
        hoverImages = new Image[]{fightHoverScaled, actHoverScaled, itemHoverScaled, mercyHoverScaled};

        // draws the outer box of the menu
        g2d = (Graphics2D) g.create();
        g2d.setStroke(new BasicStroke(3f));
        g2d.setColor(Color.WHITE);
        g2d.drawRect(bottomBox.x, bottomBox.y, bottomBox.width, bottomBox.height);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(bottomBox.x + 1, bottomBox.y + 1, bottomBox.width - 1, bottomBox.height - 1);
        g2d.dispose();

        // set up all the formatting variables
        buttonWidth  = 200;
        buttonHeight = 80;
        spacing = 80;
        count = normalImages.length;
        totalWidth = count * buttonWidth + (count - 1) * spacing;
        startX = bottomBox.x + (bottomBox.width - totalWidth) / 2;
        centerY = bottomBox.y + bottomBox.height / 2;
        x = startX;

        // draw each button in a row
        for (int i = 0; i < count; i++) {
            if(i==selectedOption){
                toDraw = hoverImages[i];
            } else {
                toDraw = normalImages[i];
            }

            y = centerY - buttonHeight / 2;
            g.drawImage(toDraw, x, y, null);
            x += buttonWidth + spacing;
        }

        // instructions on how to navigate the menu
        g.setFont(menuFont.deriveFont(Font.PLAIN, 24f));
        g.setColor(Color.WHITE);
        prompt = "[LEFT/RIGHT] to cycle, [Z] to confirm";
        promptWidth = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, bottomBox.x + bottomBox.width - promptWidth - 20, bottomBox.y + bottomBox.height - 20);
    }

    // draws the fight bar minigame for the FIGHT option
    private void drawFightBar(Graphics g) {
        // variables for formatting the bar and cursor, respectively
        int barY;
        int barX;
        int cursorY;

        // background box
        g.setColor(Color.WHITE);
        g.drawRect(bottomBox.x, bottomBox.y, bottomBox.width, bottomBox.height);
        g.setColor(Color.BLACK);
        g.fillRect(bottomBox.x + 1, bottomBox.y + 1, bottomBox.width - 1, bottomBox.height - 1);

        barY = bottomBox.y + (bottomBox.height - fightBarImage.getHeight(this)) / 2;
        barX = (getWidth() - fightBarImage.getWidth(this)) / 2;
        g.drawImage(fightBarImage, barX, barY, this);

        // the moving cursor for timing
        cursorY = barY + (fightBarImage.getHeight(this) / 2) - (cursorImage.getHeight(this) / 2);
        g.drawImage(cursorImage, fightMarkerX, cursorY, this);
    }

    // draws the boss's bullets and special attacks
    private void drawBossAttack(Graphics g) {
        AttackPattern pattern; // track the current attack pattern

        // draw each boss bullet
        for (Bullet bullet : bullets) {
            bullet.draw(g, this);
        }
        // draw extra shapes/effects from the active pattern, if any
        pattern = getCurrentAttackPattern();
        if (pattern instanceof MovingGapAttack) {
            ((MovingGapAttack) pattern).drawAttack(g);
        }
        if (pattern instanceof VerticalLaserAttack) {
            ((VerticalLaserAttack) pattern).drawAttack(g);
        }
        if (pattern instanceof GunnerAttack) {
            ((GunnerAttack) pattern).drawAttack(g);
        }
    }

    // draws the overlay for when the player wins
    private void drawWinScreen(Graphics g) {
        g.drawImage(winImage, 0, 0, getWidth(), getHeight(), null);
    }

    // draws the overlay for when the player loses
    private void drawLoseScreen(Graphics g) {
        g.drawImage(loseImage, 0, 0, getWidth(), getHeight(), null);
    }

    // draws the item selection screen
    private void drawItemSelect(Graphics g) {
        // variables to track information
        String title;
        Item selectedItem;
        String itemName;
        String itemQuantity;
        String controls;
        String idesc;
        String noItems;

        // variables for formatting text
        FontMetrics fm;
        FontMetrics fmControls;
        int nameWidth;
        int cw;

        // draw the item selection box
        g.setColor(Color.WHITE);
        g.drawRect(bottomBox.x, bottomBox.y, bottomBox.width, bottomBox.height);
        g.setColor(Color.BLACK);
        g.fillRect(bottomBox.x + 1, bottomBox.y + 1, bottomBox.width - 1, bottomBox.height - 1);
    
        // title for the item selection
        g.setFont(uiFont.deriveFont(Font.BOLD, 32f));
        g.setColor(Color.WHITE);
        title = "Select Item:";
        g.drawString(title, bottomBox.x + 40, bottomBox.y + 50);
    
        // text for item name/quantity
        g.setFont(uiFont.deriveFont(Font.PLAIN, 28f));
        selectedItem = player.getSelectedItem();
    
        if (selectedItem != null) {
            itemName = selectedItem.getName();
            itemQuantity = "x" + selectedItem.getQuantity();
    
            fm = g.getFontMetrics();
            nameWidth = fm.stringWidth(itemName + " ");
    
            // draw item name in white
            g.setColor(Color.WHITE);
            g.drawString(itemName + " ", bottomBox.x + 40, bottomBox.y + 100);
    
            // if quantity is zero, highlight in red
            if (selectedItem.getQuantity() == 0) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString(itemQuantity, bottomBox.x + 40 + nameWidth, bottomBox.y + 100);
    
            // reset color
            g.setColor(Color.WHITE);
    
            // draw the item's description
            idesc = selectedItem.getDescription();
            drawStringWrapped(g, idesc, bottomBox.x + 40, bottomBox.y + 140, bottomBox.width - 80);
        } else {
            // if no items are available, display that
            noItems = "No Items Available.";
            g.setColor(Color.WHITE);
            g.drawString(noItems, bottomBox.x + 40, bottomBox.y + 100);
        }
    
        // draw the controls at the bottom
        controls = "[LEFT/RIGHT] to cycle, [Z] Use, [X] Cancel";
        fmControls = g.getFontMetrics();
        cw = fmControls.stringWidth(controls);
        g.setColor(Color.WHITE);
        g.drawString(controls, bottomBox.x + bottomBox.width - cw - 40, bottomBox.y + bottomBox.height - 30);
    }

    // draw the act sub-menu
    private void drawActSubMenu(Graphics g) {
        // variables for formatting the text
        Font actFont;
        FontMetrics fm;
        int cols;
        int rows;
        int baseSpacingX;
        int baseSpacingY;
        int widestLabel;
        int w;
        int gridWidth;
        int startX;
        int startY;
        int index;
        int rowY;

        // variables for the options menu
        String option;
        boolean isSelected;
        int labelWidth;
        int cellX;
        int textX;

        // variables for drawing the navigation prompt
        Font instructionsFont;
        String instructions;
        int instW;

        g.setColor(Color.WHITE);
        g.drawRect(bottomBox.x, bottomBox.y, bottomBox.width, bottomBox.height);
        g.setColor(Color.BLACK);
        g.fillRect(bottomBox.x + 1, bottomBox.y + 1, bottomBox.width - 1, bottomBox.height - 1);

        actFont = menuFont.deriveFont(Font.BOLD, 36f);
        g.setFont(actFont);
        g.setColor(Color.WHITE);

        fm = g.getFontMetrics();
        cols = 2;
        rows = 2;
        baseSpacingX = 80;
        baseSpacingY = 40;
        widestLabel = 0;

        // find the widest option so we can align them
        for (String op : ACTOPTIONS) {
            w = fm.stringWidth(op);
            if (w > widestLabel) {
                widestLabel = w;
            }
        }

        gridWidth = cols * widestLabel + (cols - 1) * baseSpacingX;
        startX = bottomBox.x + (bottomBox.width - gridWidth) / 2;
        startY = bottomBox.y + 70;
        index = 0;

        // place ACT options in a grid formation
        for (int r = 0; r < rows; r++) {
            rowY = startY + r * (fm.getHeight() + baseSpacingY);
            for (int c = 0; c < cols; c++) {
                if (index >= ACTOPTIONS.length) break;
                option = ACTOPTIONS[index];
                isSelected = (index == selectedActOption);

                labelWidth = fm.stringWidth(option);
                cellX = startX + c * (widestLabel + baseSpacingX);
                textX = cellX + (widestLabel - labelWidth) / 2;

                // highlight selected option in yellow
                if (isSelected) {
                    g.setColor(Color.YELLOW);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.drawString(option, textX, rowY);
                index++;
            }
        }

        // instructions on the bottom
        instructionsFont = menuFont.deriveFont(Font.PLAIN, 24f);
        g.setFont(instructionsFont);
        instructions = "[[LEFT/RIGHT] CYCLE   [Z] CONFIRM   [X] CANCEL";
        instW = g.getFontMetrics().stringWidth(instructions);
        g.setColor(Color.WHITE);
        g.drawString(instructions, bottomBox.x + bottomBox.width - instW - 80, bottomBox.y + bottomBox.height - 20);
    }

    // the main game loop called by the timer, updates logic based on current state
    @Override
    public void actionPerformed(ActionEvent e) {
        // move cursor back and forth
        if (currentState == State.PLAYER_FIGHT_TIMING) {
            updateFightTiming();
        }
        // allow the player to move, update bullets, etc.
        else if (currentState == State.BOSS_ATTACK) {
            updatePlayerMovement();
            updatePlayerBullets();
            executeCurrentAttackPattern();
        }

        // update damage popup timer
        if (showDamagePopup) {
            damagePopupTimer--;
            if (damagePopupTimer <= 0) {
                showDamagePopup = false;
            }
        }

        // update player's flashing animation
        player.updateFlash();

        // repaint the screen
        repaint();
    }

    // moves the fight marker left and right for the fight minigame.
    private void updateFightTiming() {
        int speed = 5;

        if (fightMarkerMovingRight) {
            fightMarkerX += speed;
            // if marker goes too far right, switch direction
            if (fightMarkerX >= (getWidth() / 2 + fightBarWidth / 2 + 40)) {
                fightMarkerMovingRight = false;
            }
        } else {
            fightMarkerX -= speed;
            // if marker goes too far left, switch direction
            if (fightMarkerX <= (getWidth() / 2 - fightBarWidth / 2 - 40)) {
                fightMarkerMovingRight = true;
            }
        }
    }

    // updates the player's position based on arrow key presses
    private void updatePlayerMovement() {
        int speed = 5;
        int dx = 0;
        int dy = 0;

        // adjust the player's x/y velocity based on keys
        if (leftPressed) dx -= speed;
        if (rightPressed) dx += speed;
        if (upPressed) dy -= speed;
        if (downPressed) dy += speed;

        // move the player, clamping inside the battleBox
        player.move(dx, dy, battleBox);
    }

    // updates any bullets fired by the player. removes them if they leave the battleBox or screen
    private void updatePlayerBullets() {
        Bullet pb;

        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            pb = playerBullets.get(i);
            pb.update();
            // if the bullet is out of the battlefield, remove it
            if (pb.isOutOfBounds(getWidth(), getHeight(), battleBox)) {
                playerBullets.remove(i);
            }
        }
    }

    // executes the current attack pattern, advancing to the next pattern if done.
    private void executeCurrentAttackPattern() {
        AttackPattern pattern;
        
        pattern = getCurrentAttackPattern();
        if (pattern != null) {
            pattern.execute();

            // if the pattern signals it's finished, move on
            if (pattern.isFinished()) {
                currentAttackPattern++;
                if (currentAttackPattern >= attackPatterns.size()) {
                    currentAttackPattern = 0;
                    // increase cycle count if we've cycled through all patterns
                    cycleCount++;
                }
                // clear bullets between patterns
                bullets.clear();
                playerBullets.clear();

                // if the boss still lives, go back to DIALOG to show next text
                if (boss.getHP() > 0 && !player.isDead()) {
                    dialogText = ("Greb: Not bad... Next phase!").toUpperCase();
                    setCurrentState(State.DIALOG);
                }
            }
        }
        // if the player died, show LOSE screen
        if (player.isDead()) {
            bullets.clear();
            playerBullets.clear();

            // stop current music
            if (clip != null && clip.isRunning()) {
                clip.stop();
            }
            // play defeat sound effect
            playSoundEffect("Sounds/defeat.wav"); 

            setCurrentState(State.LOSE);
        }
    }

    // helper method for other classes to add bullets
    public void spawnBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    // helper method to spawn player bullets
    public void spawnPlayerBullet(Bullet bullet) {
        playerBullets.add(bullet);
    }

    // getter method for bullets
    public ArrayList<Bullet> getBullets() {
        return bullets;
    }

    // getter method for player bullets
    public ArrayList<Bullet> getPlayerBullets() {
        return playerBullets;
    }

    // getter method for battle box
    public Rectangle getBattleBox() {
        return battleBox;
    }

    // getter method for player object
    public Player getPlayer() {
        return player;
    }

    // getter method for boss object
    public Boss getBoss() {
        return boss;
    }

    // helper method to decrease player health
    public void decreasePlayerHP(int amount) {
        player.damage(amount);
    }

    // getter method for current boss speed modifier
    public float getBossSpeedModifier() {
        return bossSpeedModifier;
    }

    // getter method for current boss damage modifier
    public float getBossDamageModifier() {
        return bossDamageModifier;
    }

    // getter method for current player damage modifier
    public float getPlayerDamageModifier() {
        return playerDamageModifier;
    }

    // getter method for the current cycle of attack patterns
    public int getCycleCount() {
        return cycleCount;
    }

    // sets the current state of the game and re-initializes layou
    public void setCurrentState(State newState) {
        // user has lost, play main menu music
        if (currentState == State.LOSE && newState == State.MAIN_MENU) {
            playMusic("Sounds/menumusic.wav");
        }

        // update the current state, and re-initialize layout based on that state
        currentState = newState;
        initializeLayout();

        // reset instruction page when switching to or from instructions
        if (newState == State.INSTRUCTIONS) {
            instructionPage = 1;
        }
    }

    // retrieves the currently active AttackPattern object
    public AttackPattern getCurrentAttackPattern() {
        // check if current attack pattern is in the valid range
        if (currentAttackPattern < attackPatterns.size()) {
            return attackPatterns.get(currentAttackPattern);
        }
        // not possible, return null
        return null;
    }

    // return the players jet image
    public BufferedImage getJetImage() {
        return jetImage;
    }

    // return the players white jet image
    public BufferedImage getJetFlashImage() {
        return jetFlashImage;
    }

    // return the players heart image
    public BufferedImage getHeartImage() {
        return playerImage;
    }

    // return the players white heart image
    public BufferedImage getFlashHeartImage() {
        return flashImage;
    }

    // switches the game to BOSS_ATTACK state, clearing bullets and centering player, etc.
    private void startBossAttack() {
        AttackPattern current;

        setCurrentState(State.BOSS_ATTACK);
        bullets.clear();
        playerBullets.clear();
        player.centerInBox(battleBox);
        resetMovementBooleans();

        current = getCurrentAttackPattern();
        if (current != null) {
            current.initialize();
        }
    }

    // switches to the FIGHT timing minigame and resets the marker position.
    private void startFightTiming() {
        setCurrentState(State.PLAYER_FIGHT_TIMING);
        fightMarkerX = getWidth() / 2 - fightBarWidth / 2;
        fightMarkerMovingRight = true;
        fightKeyPressed = false;
    }

    // ends the FIGHT timing minigame, calculates damage, and transitions to dialog.
    private void endFightTiming() {
        // variables for calculating how close the cursor is to the targets
        int markerPos;
        int center;
        int distance;
        double bossHpRatio;
        double dmgMod;

        // variables to hold the final damage
        int baseDamage;
        String hitResult;
        int finalDamage;

        // calculate how close the marker is to the center or target zone, awarding damage
        markerPos = fightMarkerX - (getWidth() / 2 - fightBarWidth / 2);
        center = fightBarWidth / 2;
        distance = Math.abs(markerPos - center);
        bossHpRatio = (double) boss.getHP() / boss.getMaxHP();
        // if boss is below 40% HP, reduce player damage slightly
        dmgMod = (bossHpRatio < 0.4) ? 0.8 : 1.0;

        // award damage based off of the target zone the player hits
        if (fightMarkerX >= targetStartX && fightMarkerX <= targetStartX + targetWidth) {
            baseDamage = 10;
            finalDamage = 10;
            hitResult = "PERFECT HIT! YOU HIT CRAZY DAMAGE!";
        } else if (distance < fightBarWidth / 4) {
            baseDamage = 7;
            finalDamage = 7;
            hitResult = "GOOD HIT! YOU DEALT GREAT DAMAGE!";
        } else if (distance < fightBarWidth / 2) {
            baseDamage = 4;
            finalDamage = 4;
            hitResult = "OKAY HIT! YOU DEALT SOME DAMAGE.";
        } else {
            baseDamage = 1;
            finalDamage = 1;
            hitResult = "MISS... YOU DEALT LITTLE DAMAGE.";
        }

        // combine various modifiers with the base damage
        finalDamage = Math.max(finalDamage, (int) ((baseDamage + player.getNextAttackBoost()) * dmgMod * playerDamageModifier));
        boss.damage(finalDamage);
        player.setAttackBuff(0);
        playSoundEffect("Sounds/slash.wav");

        // show floating damage text over the boss
        damageText = "-" + finalDamage;
        damagePopupX = boss.getRect().x + (boss.getRect().width / 2) + 50;
        damagePopupY = boss.getRect().y + 60;
        showDamagePopup = true;
        damagePopupTimer = 60;

        // if that last hit defeated the boss, go to WIN
        if (boss.isDefeated()) {
            // stop current music
            if (clip != null && clip.isRunning()) {
                clip.stop();
            }
            // play a victory sound effect
            playSoundEffect("Sounds/victory.wav");
            setCurrentState(State.WIN);
        } else {
            dialogText = hitResult.toUpperCase();
            playerTurnEnded = true;
            setCurrentState(State.DIALOG);
        }

        // reset temporary damage boost if it was active
        if (tempDamageBoostActive) {
            playerDamageModifier /= 2.0f;
            tempDamageBoostActive = false;
        }
    }

    // switches to the ACT sub-menu
    private void act() {
        setCurrentState(State.PLAYER_ACT);
        selectedActOption = 0;
    }

    // forfeits the fight, returning the player to the main menu after some dialog.
    private void forfeitBattle() {
        dialogText = ("YOU FORFEIT THE FIGHT MID-BATTLE.\n\nGREB: YOU... FAIL!").toUpperCase();
        setCurrentState(State.DIALOG);
    }

    // switch to the ITEM selection sub-menu.
    private void useItem() {
        setCurrentState(State.PLAYER_ITEM_SELECT);
    }

    // confirms using the currently selected item (if available).
    private void confirmUseItem() {
        Item selectedItem;

        if (player.hasItems()) {
            selectedItem = player.getSelectedItem();

            // display different texts based on properties of the item the user selects
            if (selectedItem != null && selectedItem.getQuantity() > 0) {
                player.useSelectedItem();
                dialogText = ("YOU USED THE " + selectedItem.getName() + "!").toUpperCase();
            } else if (selectedItem != null && selectedItem.getQuantity() == 0) {
                dialogText = ("YOU HAVE NO " + selectedItem.getName().toUpperCase() + " LEFT!").toUpperCase();
            } else {
                dialogText = "INVALID ITEM SELECTION.".toUpperCase();
            }
        } else {
            // player has no items, display that
            dialogText = "YOU HAVE NO ITEMS LEFT!".toUpperCase();
        }
        playerTurnEnded = true;
        setCurrentState(State.DIALOG);
    }

    // interprets the selected ACT choice and applies effects to the boss or the player
    private void handleActChoice(String choice) {
        int missingHP;
        int heal;

        if (choice.equals("CHECK CODE")) {
            // slightly increases bossDamageModifier, making the boss a bit more powerful
            bossDamageModifier *= 1.10f;
            dialogText = "YOU CHECK THE CODE...\nGREB: SCROLL UP, SCROLL DOWN.\nGREB: THERE'S AN ERROR IN YOUR CODE!!\nGREB IS NOT PLEASED.";
        } else if (choice.equals("JOKE")) {
            // boss recovers some HP (like a negative damage)
            missingHP = boss.getMaxHP() - boss.getHP();
            heal = (int) (0.2 * missingHP);
            boss.damage(-heal);
            dialogText = "YOU TELL A JOKE...\nGREB: HAHAHAHAHAHAHAHAHA!!!\nGREB IS IN A GOOD MOOD.\nHE REGAINS SOME HEALTH!";
        } else if (choice.equals("TECH SUPPORT")) {
            // waste the players turn, display some text too
            dialogText = "YOU CALL TECH SUPPORT...\nBOSS: \"THANK YOU FOR CALLING TECH SUPPORT.\"\nGREB: I WILL NOT HELP YOU.";
        } else if (choice.equals("ORZ")) {
            if (!tempDamageBoostActive) { 
                // apply boost only if not already active
                playerDamageModifier *= 2.0f;
                tempDamageBoostActive = true;
                dialogText = "YOU GO 'ORZ' AND SHOW RESPECT.\nYOUR DETERMINATION SWELLS.\nYOUR NEXT ATTACK DEALS MORE DAMAGE!";
            } else {
                // do not apply if already active (will waste player turn)
                dialogText = "YOUR SPIRIT IS ALREADY DETERMINED!";
            }
        }
        playerTurnEnded = true;
        setCurrentState(State.DIALOG);
    }

    // ends the dialog (DIALOG or INTRO states) and decides what's next
    private void endDialog() {
        // if in the INTRO, move to the player's first menu
        if (currentState == State.INTRO) {
            setCurrentState(State.PLAYER_MENU);
            return;
        }
        // if the dialog involves a forfeit text, go back to main menu
        if (dialogText.contains("FORFEIT")) {
            setCurrentState(State.MAIN_MENU);
            resetGame();
            return;
        }
        // if the boss has vanished or was defeated, go to WIN
        if (dialogText.contains("VANISHES") || dialogText.contains("DEFEATED")) {
            setCurrentState(State.WIN);
            return;
        }
        // if the player's turn ended, start the boss attack next
        if (playerTurnEnded) {
            if (!boss.isDefeated() && !player.isDead()) {
                startBossAttack();
            }
            playerTurnEnded = false;
            return;
        }
        // if the text references certain keywords, move to next attack or back to menu
        if (dialogText.contains("DAMAGE") || dialogText.contains("FUNNY")
                || dialogText.contains("TECH SUPPORT") || dialogText.contains("RESPECT")) {
            if (!boss.isDefeated() && !player.isDead()) {
                startBossAttack();
                return;
            }
        }
        if (dialogText.contains("NEXT PHASE") || dialogText.contains("CONTINUE")) {
            if (!boss.isDefeated() && !player.isDead() && getCurrentAttackPattern() != null) {
                setCurrentState(State.PLAYER_MENU);
                return;
            }
        }
        // otherwise, if boss is alive, just return to the player's menu
        if (!boss.isDefeated() && !player.isDead()) {
            setCurrentState(State.PLAYER_MENU);
        }
    }

    // resets all movement booleans so the player doesn't keep moving after releasing the keys.
    private void resetMovementBooleans() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
    }

    // handles key presses for each state of the game.
    @Override
    public void keyPressed(KeyEvent e) {
        int code;
        String choice; // keep track of selection user option
        Rectangle pr;
        Bullet pb;
        AttackPattern pattern;
        
        code = e.getKeyCode();

        // MAIN MENU
        if (currentState == State.MAIN_MENU) {
            if (code == KeyEvent.VK_Z) {
                setCurrentState(State.INTRO);
                playSoundEffect("Sounds/buttonselect.wav");
                playMusic("Sounds/fightmusic.wav");
                dialogText = "YOU ENCOUNTERED GREBORY ANTONY.";
            } else if (code == KeyEvent.VK_I) {
                playSoundEffect("Sounds/buttonselect.wav");
                setCurrentState(State.INSTRUCTIONS);
            } else if (code == KeyEvent.VK_S) {
                playSoundEffect("Sounds/buttonselect.wav");
                setCurrentState(State.SETTINGS);
            } else if (code == KeyEvent.VK_Q) {
                System.exit(0);
            }
        }

        // INSTRUCTIONS
        else if (currentState == State.INSTRUCTIONS) {
            if (code == KeyEvent.VK_LEFT) {
                playSoundEffect("Sounds/buttonswitch.wav");
                if (instructionPage > 1) {
                    instructionPage--;
                }
            }
            if (code == KeyEvent.VK_RIGHT) {
                playSoundEffect("Sounds/buttonswitch.wav");
                if (instructionPage < TOTAL_INSTRUCTION_PAGES) {
                    instructionPage++;
                }
            }
            if (code == KeyEvent.VK_Z) {
                playSoundEffect("Sounds/buttonselect.wav");
                setCurrentState(State.MAIN_MENU);
            }
        }

        // SETTINGS
        else if (currentState == State.SETTINGS) {
            if (code == KeyEvent.VK_Z) {
                // set player's HP to the chosen difficulty
                playSoundEffect("Sounds/buttonselect.wav");
                player.setMaxHP(difficultyHP[difficultyIdx]);
                setCurrentState(State.MAIN_MENU);
            } else if (code == KeyEvent.VK_UP) {
                playSoundEffect("Sounds/buttonswitch.wav");
                // move selection up in the difficulty list
                difficultyIdx--;
                if (difficultyIdx < 0) {
                    difficultyIdx = difficulties.length - 1;
                }
            } else if (code == KeyEvent.VK_DOWN) {
                playSoundEffect("Sounds/buttonswitch.wav");
                // move selection down in the difficulty list
                difficultyIdx++;
                if (difficultyIdx >= difficulties.length) {
                    difficultyIdx = 0;
                }
            } else if (code == KeyEvent.VK_X) {
                // go back to main menu without applying changes
                playSoundEffect("Sounds/buttonselect.wav");
                setCurrentState(State.MAIN_MENU);
            }
        }

        // PLAYER MENU (FIGHT, ACT, ITEM, QUIT)
        else if (currentState == State.PLAYER_MENU) {
            if (code == KeyEvent.VK_LEFT) {
                selectedOption = (selectedOption - 1 + MENUOPTIONS.length) % MENUOPTIONS.length;
                playSoundEffect("Sounds/hover.wav");
            } else if (code == KeyEvent.VK_RIGHT) {
                selectedOption = (selectedOption + 1) % MENUOPTIONS.length;
                playSoundEffect("Sounds/hover.wav");
            } else if (code == KeyEvent.VK_Z) {
                choice = MENUOPTIONS[selectedOption];
                playSoundEffect("Sounds/click.wav");
                if (choice.equals("FIGHT")) {
                    startFightTiming();
                } else if (choice.equals("ACT")) {
                    act();
                } else if (choice.equals("ITEM")) {
                    useItem();
                } else if (choice.equals("QUIT")) {
                    forfeitBattle();
                }
            }
        }

        // ITEM SELECT
        else if (currentState == State.PLAYER_ITEM_SELECT) {
            if (code == KeyEvent.VK_LEFT) {
                player.cycleItem(false);
            }
            if (code == KeyEvent.VK_RIGHT) {
                player.cycleItem(true);
            }
            if (code == KeyEvent.VK_Z) {
                confirmUseItem();
            }
            if (code == KeyEvent.VK_X) {
                setCurrentState(State.PLAYER_MENU);
            }
        }

        // FIGHT TIMING
        else if (currentState == State.PLAYER_FIGHT_TIMING) {
            // If [Z] is pressed, finalize the timing and deal damage
            if (code == KeyEvent.VK_Z && !fightKeyPressed) {
                fightKeyPressed = true;
                endFightTiming();
            }
        }

        // DIALOG or INTRO states
        else if (currentState == State.DIALOG || currentState == State.INTRO) {
            if (code == KeyEvent.VK_Z) {
                if (dialogText.contains("FORFEIT")) {
                    playMusic("Sounds/menumusic.wav");
                }
                endDialog();
            }
        }

        // ACT submenu
        else if (currentState == State.PLAYER_ACT) {
            if (code == KeyEvent.VK_LEFT) {
                // move selection left if possible
                if (selectedActOption % 2 == 1) {
                    selectedActOption = Math.max(selectedActOption - 1, 0);
                }
            } else if (code == KeyEvent.VK_RIGHT) {
                // move selection right if possible
                if (selectedActOption % 2 == 0 && selectedActOption < ACTOPTIONS.length - 1) {
                    selectedActOption = Math.min(selectedActOption + 1, ACTOPTIONS.length - 1);
                }
            } else if (code == KeyEvent.VK_UP) {
                // move up one row
                if (selectedActOption >= 2) {
                    selectedActOption -= 2;
                }
            } else if (code == KeyEvent.VK_DOWN) {
                // move down one row
                if (selectedActOption < 2) {
                    selectedActOption += 2;
                }
            } else if (code == KeyEvent.VK_Z) {
                // confirm the ACT choice
                choice = ACTOPTIONS[selectedActOption];
                handleActChoice(choice);
            } else if (code == KeyEvent.VK_X) {
                setCurrentState(State.PLAYER_MENU);
            }
        }

        // BOSS ATTACK (player can move around in the battle box)
        else if (currentState == State.BOSS_ATTACK) {
            if (code == KeyEvent.VK_UP) {
                upPressed = true;
            }
            if (code == KeyEvent.VK_DOWN) {
                downPressed = true;
            }
            if (code == KeyEvent.VK_LEFT) {
                leftPressed = true;
            }
            if (code == KeyEvent.VK_RIGHT) {
                rightPressed = true;
            }
            // [X] can be used to shoot if the pattern allows
            if (code == KeyEvent.VK_X) {
                pattern = getCurrentAttackPattern();
                if (pattern instanceof GunnerAttack) {
                    pr = player.getHitbox();
                    pb = new Bullet(pr.x + pr.width, pr.y + pr.height/2 - 5,10, 10, 0, 0, null,Color.YELLOW);
                    GamePanel.playSoundEffect("Sounds/playerbullet.wav");
                    spawnPlayerBullet(pb);
                }
            }
        }

        // WIN or LOSE states
        else if (currentState == State.WIN || currentState == State.LOSE) {
            if (code == KeyEvent.VK_Z) {
                // stop any ongoing sound effect
                if (effectClip != null && effectClip.isRunning()) {
                    effectClip.stop();
                    effectClip.close();
                }

                resetGame();

                // resume background music when leaving win/lose screen
                playMusic("Sounds/menumusic.wav");
                setCurrentState(State.MAIN_MENU);
            }
        }
    }

    // when a key is released, stop moving in that direction.
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_UP)    { upPressed = false; }
        if (code == KeyEvent.VK_DOWN)  { downPressed = false; }
        if (code == KeyEvent.VK_LEFT)  { leftPressed = false; }
        if (code == KeyEvent.VK_RIGHT) { rightPressed = false; }
    }

    // must be overriden, but not used
    @Override
    public void keyTyped(KeyEvent e) {
    }

    // a helper method to draw text with basic word-wrapping based on maxWidth.
    public void drawStringWrapped(Graphics g, String text, int x, int y, int maxWidth) {
        // variables for formatting text
        FontMetrics fm;
        String[] lines;
        int breakIndex;

        fm = g.getFontMetrics();
        lines = text.split("\n");

        for (String line : lines) {
            // if a line is too long, break it
            while (fm.stringWidth(line) > maxWidth && line.length() > 0) {
                breakIndex = line.length();
                // decrement breakIndex until the substring fits
                while (breakIndex > 0 && fm.stringWidth(line.substring(0, breakIndex)) > maxWidth) {
                    breakIndex--;
                }
                if (breakIndex <= 0) {
                    break;
                }
                g.drawString(line.substring(0, breakIndex), x, y);
                y += fm.getHeight();
                line = line.substring(breakIndex).trim();
            }
            // draw what remains
            g.drawString(line, x, y);
            y += fm.getHeight();
        }
    }
}

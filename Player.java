/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class represents the Player in the game, handling its position, movement,
 * health, rendering, damage, invulnerability frames, and inventory/consumable item usage.
 */
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class Player {
    private Rectangle hitbox;

    // variables for players HP
    private int hp;
    private int maxHP;

    private long invincibleUntil = 0; // timestamp for the players i-frames
    private int nextAttackBoost = 0;  // boost for next attack
    private boolean flashing = false; // flashing state to indicate when player takes damage
    private int flashTimer = 0; // hold how long player image flashes for

    // the normal and flash images used to represent the player
    private BufferedImage image;
    private BufferedImage flashImage;

    // defualt images for heart and jet
    private BufferedImage heartImage;
    private BufferedImage flashHeartImage;
    private BufferedImage shipImage;
    private BufferedImage flashShipImage;

    // collection of consumable items the player holds
    private ArrayList<Item> items;
    private int selectedItemIndex = 0;

    // shield state and the number of turns it remains active
    private boolean shieldActive = false;
    private int shieldTurns = 0;

    // constructor for a Player with configured values
    public Player(int x, int y, int width, int height, int hp, BufferedImage normalImg, BufferedImage flashImg, BufferedImage heartImg, BufferedImage flashHeartImg, BufferedImage shipImg, BufferedImage flashShipImg) {
        this.hitbox = new Rectangle(x, y, width, height);
        this.hp = hp;
        this.maxHP = hp;
        this.image = normalImg;
        this.flashImage = flashImg;
        this.heartImage = heartImg;
        this.flashHeartImage = flashHeartImg;
        this.shipImage = shipImg;
        this.flashShipImage = flashShipImg;

        items = new ArrayList<>();
    }

    // method to reset to heart image
    public void resetToHeartImage() {
        this.image = heartImage;
        this.flashImage = flashHeartImage;
    }
    
    // method to set to ship image
    public void setToShipImage() {
        this.image = shipImage;
        this.flashImage = flashShipImage;
    }

    // initializes the player's inventory with a default set of items.
    public void initializeInventory() {
        items.clear();
        items.add(new Item("Health Potion","Heals 40 HP.",40,3));
        items.add(new Item("Shield Token","Nullifies all damage twice.",0,1));
        items.add(new Item("Power Flask","Your next attack deals +150% damage.",0,2));
        items.add(new Item("Grebbory's Assignment Resubmission", "Heals all missing HP.",100,1));

        selectedItemIndex = 0;
    }

    // returns the players hitbox
    public Rectangle getHitbox() {
        return hitbox;
    }

    // returns the players current HP
    public int getHP() {
        return hp;
    }

    // returns the players max HP
    public int getMaxHP() {
        return maxHP;
    }

    // sets the player's max HP to a different value
    public void setMaxHP(int newMaxHP) {
        this.maxHP = newMaxHP;
        this.hp = newMaxHP;
    }

    // sets the player image
    public void setImage(BufferedImage img) {
        this.image = img;
    }

    // sets the player flash image
    public void setFlashImage(BufferedImage img) {
        this.flashImage = img;
    }

    // damage the player
    public void damage(int amount) {
        long now;
        
        now = System.currentTimeMillis();

        // if shield active, deal no damage
        if (shieldActive) {
            amount = 0;
        }

        // check for iframes
        if (now > invincibleUntil) {
            hp -= amount;
            GamePanel.playSoundEffect("Sounds/damagetaken.wav");

            if (hp < 0) {
                hp = 0;
            }

            // reset iframes
            invincibleUntil = now + 500;
            flashing = true;
            flashTimer = 30;

            // if shield active, decrease uses
            if (shieldActive) {
                shieldTurns--;
                if (shieldTurns <= 0) {
                    shieldActive = false;
                }
            }
        }
    }

    // returns whether the player is dead
    public boolean isDead() {
        return hp <= 0;
    }

    // moves the player by the given deltas, and ensures player stays within box
    public void move(int dx, int dy, Rectangle battleBox) {
        hitbox.x += dx;
        hitbox.y += dy;

        if (hitbox.x < battleBox.x) {
            hitbox.x = battleBox.x;
        }
        if (hitbox.y < battleBox.y) {
            hitbox.y = battleBox.y;
        }
        if (hitbox.x + hitbox.width > battleBox.x + battleBox.width) {
            hitbox.x = battleBox.x + battleBox.width - hitbox.width;
        }
        if (hitbox.y + hitbox.height > battleBox.y + battleBox.height) {
            hitbox.y = battleBox.y + battleBox.height - hitbox.height;
        }
    }

    // center player within given battle box
    public void centerInBox(Rectangle battleBox) {
        hitbox.x = battleBox.x + battleBox.width / 2 - hitbox.width / 2;
        hitbox.y = battleBox.y + battleBox.height / 2 - hitbox.height / 2;
    }

    // draw the player, including the flashing effect if damage is taken
    public void draw(Graphics g) {
        if (image != null && flashImage != null) {
            if (flashing) {
                if ((flashTimer / 5) % 2 == 0) {
                    g.drawImage(image, hitbox.x, hitbox.y, hitbox.width, hitbox.height, null);
                } else {
                    g.drawImage(flashImage, hitbox.x, hitbox.y, hitbox.width, hitbox.height, null);
                }
            } else {
                g.drawImage(image, hitbox.x, hitbox.y, hitbox.width, hitbox.height, null);
            }
        } else {
            if (flashing && flashTimer % 4 < 2) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(Color.RED);
            }
            g.fillRect(hitbox.x, hitbox.y, hitbox.width, hitbox.height);
        }
    }

    // retrieve next attack boost
    public int getNextAttackBoost() {
        return nextAttackBoost;
    }

    // check if player has any items in inventory
    public boolean hasItems() {
        for (Item it : items) {
            if (it.getQuantity() > 0) {
                return true;
            }
        }
        return false;
    }

    // retrieve name and quantity of selected item
    public String getSelectedItemName() {
        Item i;

        if (items.isEmpty()) {
            return "No Items";
        }
        i = items.get(selectedItemIndex);
        return i.getName() + " x" + i.getQuantity();
    }

    // retrieve description of the currently selected item
    public String getSelectedItemDesc() {
        Item i;

        if (items.isEmpty()) {
            return "No items available.";
        }
        i = items.get(selectedItemIndex);
        return i.getDescription();
    }

    // cycle through the inventory items, moving forward or backward
    public void cycleItem(boolean forward) {
        int count;

        if (items.isEmpty()) {
            return;
        }
        count = items.size();

        if (forward) {
            selectedItemIndex = (selectedItemIndex + 1) % count;
        } else {
            selectedItemIndex = (selectedItemIndex - 1 + count) % count;
        }
    }

    // uses the currently selected item if there is any remaining quantity
    public void useSelectedItem() {
        Item i;

        if (items.isEmpty()) {
            return;
        }
        i = items.get(selectedItemIndex);

        if (i.getQuantity() > 0) {
            if (i.getName().equals("Health Potion")) {
                hp += i.getHealAmount();
                if (hp > maxHP) {
                    hp = maxHP;
                }
                i.useOne();
            }
            else if (i.getName().equals("Shield Token")) {
                shieldActive = true;
                shieldTurns = 2;
                i.useOne();
            }
            else if (i.getName().equals("Power Flask")) {
                nextAttackBoost = 15; 
                i.useOne();
            }
        }
    }

    // update the flashing state for the player image
    public void updateFlash() {
        if (flashing) {
            flashTimer--;
            if (flashTimer <= 0) {
                flashing = false;
            }
        }
    }

    // return the currently selected item
    public Item getSelectedItem() {
        if (items.isEmpty()) {
            return null;
        }
        return items.get(selectedItemIndex);
    }
}

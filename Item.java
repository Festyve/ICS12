/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class defines properties for a game item. It provides helper methods for 
 * gathering its name, description, quantity and other properties.
 */
public class Item {
    private String name;
    private String description;
    private int healAmount;
    private int quantity;

    public Item(String name, String description, int healAmount, int quantity) {
        this.name = name;
        this.description = description;
        this.healAmount = healAmount;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public int getQuantity() {
        return quantity;
    }

    public void useOne() {
        if (quantity > 0) {
            quantity--;
        }
    }
}
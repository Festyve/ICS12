/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2024-01-08
 * Description: This class creates a new GameFrame, which initializes the game.
 */
import javax.swing.SwingUtilities;

public class Main {
    // main method where the main application starts
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame game = new GameFrame();
            game.setVisible(true);
        });
    }
}
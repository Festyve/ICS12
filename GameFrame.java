/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2024-01-08
 * Description: This class establishes the main frame for the Pong game. It sets up the frame properties,
 * including adding the GamePanel, setting the title, size and default behaviours.
 */
import javax.swing.JFrame;

public class GameFrame extends JFrame {
    private GamePanel gamePanel;

    public GameFrame() {
        gamePanel = new GamePanel();
        this.add(gamePanel); // add the panel to the frame
        this.setTitle("Greb's Game"); // create a new GamePanel instance
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // exit application when X button is pressed
        this.setSize(1400, 800); // set size of the frame
        this.setResizable(false); // disable resizing of the frame
        this.setVisible(true); // makes window visible to user
        this.setLocationRelativeTo(null); // center the frame on the screen
    }
}

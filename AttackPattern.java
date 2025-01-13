/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This abstract class defines the structure for attack patterns in the game.
 */
public abstract class AttackPattern {
    protected GamePanel panel; // GamePanel where the AttackPattern is executed
    protected boolean finished = false;

    // constructor for an AttackPattern with the specified GamePanel
    public AttackPattern(GamePanel panel) {
        this.panel = panel;
    }

    // checks if the attack pattern has finished executing.
    public boolean isFinished() {
        return finished;
    }

    // setups the attack pattern and all of its respective variables
    public abstract void initialize();

    // executes the attack pattern, contains logic on how to perform the attack
    public abstract void execute();
}

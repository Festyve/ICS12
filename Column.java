/*
 * Author: Michael Zhang and Bryan Yao
 * Date: 2025-01-08
 * Description: This class defines properties for a column object. It is used solely for ease of
 * implementation of the PillarBoxAttack class.
 */
import java.awt.Rectangle;
import java.util.ArrayList;

public class Column {
    public int x;
    public int gapY;
    public int gapHeight;
    public ArrayList<Rectangle> boxes;

    public Column() {
        boxes = new ArrayList<>();
    }
}
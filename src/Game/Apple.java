package Game;

import java.awt.*;

public class Apple {
    int x;
    int y;
    public Apple(int x, int y){
        this.x = x;
        this.y = y;
    }
    public void draw(Graphics g){
        g.setColor(Color.RED);
        g.fillOval(x, y, SnakeGame.DOT_SIZE, SnakeGame.DOT_SIZE);
    }
}

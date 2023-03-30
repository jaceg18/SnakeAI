import Game.SnakeGame;

import javax.swing.*;

public class Main {
    // Main class
    public static void main(String[] args) {
        JFrame frame = new JFrame("SnakeAI");
        SnakeGame game = new SnakeGame();
        frame.add(game);
        frame.setResizable(false);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
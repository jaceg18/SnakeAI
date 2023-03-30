package Game;

import Engine.SnakeAI;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.swing.*;

public class SnakeGame extends JPanel implements ActionListener {
    private final int WIDTH = 600;
    private final int HEIGHT = 600;
    public static final int DOT_SIZE = 10;
    private final int ALL_DOTS = (WIDTH * HEIGHT) / (DOT_SIZE * DOT_SIZE);
    public static final int DELAY = 30;
    private final int[] x = new int[ALL_DOTS];
    private final int[] y = new int[ALL_DOTS];
    private int dots;
    private boolean leftDirection = false;
    private boolean rightDirection = true;
    private boolean upDirection = false;
    private boolean downDirection = false;
    private boolean inGame = true;
    private Timer timer;
    private SnakeAI snakeAI;
    long gameTime;
    Apple apple;
    int applesEaten;

    int recentWallAttempts = 0;

    public SnakeGame() {
        initGame();
    }
    private void initGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        // Create a new SnakeAI instance, this will be replaced with the information saved in the training data.
        snakeAI = new SnakeAI(12, 8, 4);

        this.apple = generateApple();
        this.applesEaten = 0;
        initGameParams();
        timer = new Timer(DELAY, this);
        gameTime = System.currentTimeMillis();
        timer.start();
    }
    public void restart(){
        inGame = true;
        initGame();
    }
    private void initGameParams() {
        dots = 3;

        for (int i = 0; i < dots; i++) {
            x[i] = 100 - i * DOT_SIZE;
            y[i] = 100;
        }
    }
    private Apple generateApple() {

        int x = new Random().nextInt(60) * DOT_SIZE;
        int y = new Random().nextInt(60) * DOT_SIZE;

        if (x >= 570){
            x -= DOT_SIZE*3;
        }
        if (10 >= x){
        x += DOT_SIZE*3;
        }
        if (y >= 570){
            y -= DOT_SIZE*3;
        }
        if (10 >= y){
            y+= DOT_SIZE*3;
        }

        return new Apple(x, y);
    }
    private void move() {
         // Get the current state of the game
         double[] currentState = getInput();
        int dx = apple.x - x[0];
        int dy = apple.y - y[0];
        double foodDistances = Math.sqrt(dx * dx + dy * dy);
         // Pass current game state to get an action for the NN
         int action = snakeAI.getAction(currentState);
         // Move the snake according to the selected action
         if (action == 0) {
             moveLeft();
         } else if (action == 1) {
             moveRight();
         } else if (action == 2) {
             moveUp();
         } else if (action == 3) {
             moveDown();
         }

         moveSnake();
         // Check if the snake has collided with a wall or its own body
         checkCollision();

         giveReward(currentState, action, foodDistances);
     }
    private void giveReward(double[] currentState, int action, double foodDistances){
        int[] head = new int[]{x[0], y[0]};
        int[] applePos = new int[]{apple.x, apple.y};

        // Check if the snake has eaten an apple
        if (head[0] == applePos[0] && head[1] == applePos[1]){
            // Give a positive reward to the snake for eating an apple
            dots++;
            applesEaten++;
            double reward = calculateReward(foodDistances, true, action);
            snakeAI.train(currentState, action, getInput(), reward);
            apple = generateApple();
        } else {
            // Negative reward for snake not eating an apple
            double reward = calculateReward(foodDistances, false, action);
            snakeAI.train(currentState, action, getInput(), reward);
        }

        if (!inGame){
            // Give a negative reward to the snake for dying
            double reward = -10.0;
            snakeAI.train(currentState, action, getInput(), reward);
        }
    }
    private double calculateReward(double foodDistances, boolean ateApple, int action){
        double reward = -0.1;


        // Negative reward for approaching a wall
        double distToWall = Math.min(y[0], Math.min(WIDTH - x[0], Math.min(HEIGHT - y[0], x[0])));
        if (distToWall <= DOT_SIZE) {
            // Give a negative reward to the snake for being too close to the wall
            recentWallAttempts++;
            if (recentWallAttempts > 5){
                inGame = false;
            }
            return -10.0;
        }

        recentWallAttempts = 0;

        if (ateApple){
            return 10;
        }

        // Gives reward or penalty for making or missing a perfect turn
        if (y[0] == apple.y){
            if (apple.x > x[0]) {
                if (action == 1) {
                    return 0.5;
                }else {
                    return -0.5;
                }
            } else if (x[0] > apple.x){
                if (action == 0){
                    return 0.5;
                }else {
                    return -0.5;
                }
            }
        }
        if (x[0] == apple.x){
            if (apple.y > y[0]){
                if (action == 3){
                    return 0.5;
                } else {
                    return -0.5;
                }
            } else if (y[0] > apple.y){
                if (action == 2){
                    return 0.5;
                }else {
                    return -0.5;
                }
            }
        }

        // Positive reward for getting closer to the food
        int dx = apple.x - x[0];
        int dy = apple.y - y[0];
        double newFoodDistance = Math.sqrt(dx * dx + dy * dy);
        if (newFoodDistance < foodDistances){
            System.out.println("Food is closer! +.01");
            return 0.01;
        }

        System.out.println(reward + " Not moving towards food");


        return reward;
    }
    private void followHead(){
        for (int i = dots; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }
    }
    private void moveLeft() {
        upDirection = false;
        downDirection = false;
        leftDirection = true;
        rightDirection = false;
    }
    private void moveRight() {
        upDirection = false;
        downDirection = false;
        leftDirection = false;
        rightDirection = true;
    }
    private void moveUp() {
        upDirection = true;
        downDirection = false;
        leftDirection = false;
        rightDirection = false;
    }
    private void moveDown() {
        upDirection = false;
        downDirection = true;
        leftDirection = false;
        rightDirection = false;
    }
    private void moveSnake(){
        followHead();
        if (rightDirection){
            x[0] += DOT_SIZE;
        } else if (leftDirection){
            x[0] -= DOT_SIZE;
        } else if (upDirection){
            y[0] -= DOT_SIZE;
        } else if (downDirection){
            y[0] += DOT_SIZE;
        }
    }
    private double[] getInput() {
        double[] input = new double[12];

        // Snake's head x and y
        input[0] = x[0];
        input[1] = y[0];

        // Apple x and y
        input[2] = apple.x;
        input[3] = apple.y;

        // Snake's direction
        if (leftDirection) {
            input[4] = 1;
        } else if (rightDirection) {
            input[4] = -1;
        }
        if (upDirection) {
            input[5] = 1;
        } else if (downDirection) {
            input[5] = -1;
        }

        // Distances from the snake to the wall
        input[6] = x[0]; // Distance to left wall
        input[7] = WIDTH - x[0]; // Distance to right wall
        input[8] = y[0]; // Distance to top wall
        input[9] = HEIGHT - y[0]; // Distance to bottom wall

        // Distance from the snake to the food
        int dx = apple.x - x[0];
        int dy = apple.y - y[0];
        input[10] = Math.sqrt(dx * dx + dy * dy);

        // Snakes dot amount
        input[11] = dots;

        return input;
    }
    private void checkCollision() {
        if (y[0] >= HEIGHT) {
            inGame = false;
        }

        if (y[0] < 0) {
            inGame = false;
        }

        if (x[0] >= WIDTH) {
            inGame = false;
        }

        if (x[0] < 0) {
            inGame = false;
        }
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (inGame) {
            // draw the apple
           apple.draw(g);
            // draw the snake
            for (int i = 0; i < dots; i++) {
                if (i == 0) {
                    g.setColor(Color.green);
                } else {
                    g.setColor(Color.white);
                }
                g.fillRect(x[i], y[i], DOT_SIZE, DOT_SIZE);
            }

            // sync graphics state
            Toolkit.getDefaultToolkit().sync();
        }
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        if (inGame) {
           move();
        } else {
            snakeAI.saveWeights();
            snakeAI.saveScore(gameTime, applesEaten);
            timer.stop();
            if (applesEaten > 9){
                System.out.println("Stopped training, Ai ate 10 apples in one turn.");
            } else {
                restart();
            }
        }
        repaint();
    }

}
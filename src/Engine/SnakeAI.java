package Engine;

import Game.SnakeGame;

import java.io.*;
import java.util.Random;

public class SnakeAI {

    private int inputSize;
    private int hiddenSize;
    private int outputSize;
    private double[][] inputHiddenWeights;
    private double[][] hiddenOutputWeights;
    private double LEARNING_RATE = 0.01;
    private final File weightsFile = new File("src/Engine/Data/weights.dat");
    private final File gameLogs = new File("src/Engine/Data/games.txt");
    public SnakeAI(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        inputHiddenWeights = new double[inputSize][hiddenSize];
        hiddenOutputWeights = new double[hiddenSize][outputSize];

        loadWeights();
    }
    public int getAction(double[] state) {
        double[] output = forward(state);
        int action = 0;
        double max = output[0];
        for (int i = 1; i < outputSize; i++) {
            if (output[i] > max) {
                max = output[i];
                action = i;
            }
        }
        return action;
    }
    public void train(double[] state, int action, double[] nextState, double reward) {
        score += reward;
        double[] output = forward(state);
        double[] nextOutput = forward(nextState);

        double[] targetOutput = output.clone();
        targetOutput[action] = reward + getMaxQ(nextOutput);

        double[] hiddenOutputs = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double sum = 0;
            for (int j = 0; j < inputSize; j++) {
                sum += state[j] * inputHiddenWeights[j][i];
            }
            hiddenOutputs[i] = sigmoid(sum);
        }

        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                hiddenOutputWeights[i][j] += LEARNING_RATE * hiddenOutputs[i] * (targetOutput[j] - output[j]);
            }
        }

        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                double sum = 0;
                for (int k = 0; k < outputSize; k++) {
                    sum += (targetOutput[k] - output[k]) * hiddenOutputWeights[j][k];
                }
                inputHiddenWeights[i][j] += LEARNING_RATE * state[i] * hiddenOutputs[j] * (1 - hiddenOutputs[j]) * sum;
            }
        }
    }
    public double[] forward(double[] input) {
        double[] hiddenOutputs = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double sum = 0;
            for (int j = 0; j < inputSize; j++) {
                sum += input[j] * inputHiddenWeights[j][i];
            }
            hiddenOutputs[i] = sigmoid(sum);
        }

        double[] output = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            double sum = 0;
            for (int j = 0; j < hiddenSize; j++) {
                sum += hiddenOutputs[j] * hiddenOutputWeights[j][i];
            }
            output[i] = sum;
        }
        return output;
    }
    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }
    private void initializeWeights(double[][] weights) {
        Random random = new Random();
        for (int i = 0; i < weights.length; i++) {
            for (int j = 0; j < weights[0].length; j++) {
                weights[i][j] = random.nextDouble() * 2 - 1; // Random value between -1 and 1
            }
        }
    }
    private double getMaxQ(double[] output) {
        double max = output[0];
        for (int i = 1; i < output.length; i++) {
            if (output[i] > max) {
                max = output[i];
            }
        }
        return max;
    }
    public double[][] getWeights() {
        double[][] weights = new double[inputSize + hiddenSize + 2][];
        weights[0] = new double[]{inputSize, hiddenSize, outputSize}; // Add network dimensions to the weights array
        for (int i = 0; i < inputSize; i++) {
            weights[i + 1] = inputHiddenWeights[i].clone();
        }
        for (int i = 0; i < hiddenSize; i++) {
            weights[inputSize + 1 + i] = hiddenOutputWeights[i].clone();
        }
        return weights;
    }
    public void saveWeights(){
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("src/Engine/Data/weights.dat"));
            out.writeObject(getWeights());
            out.close();
        } catch (IOException e) {
           saveWeights();
        }
    }
    private void loadWeights(){
        if (weightsFile.exists() && weightsFile.length() > 0){
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(weightsFile));
                double[][] weights = (double[][]) in.readObject();
                in.close();
                setWeights(weights);
            } catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        } else {
            System.out.println("No training data found, initializing new weights.");
            createTrainingFile(true);
            initializeWeights(inputHiddenWeights);
            initializeWeights(hiddenOutputWeights);
            saveWeights();
        }
    }
    private void setWeights(double[][] weights) {
        inputSize = (int) weights[0][0];
        hiddenSize = (int) weights[0][1];
        outputSize = (int) weights[0][2];
        inputHiddenWeights = new double[inputSize][hiddenSize];
        hiddenOutputWeights = new double[hiddenSize][outputSize];
        for (int i = 0; i < inputSize; i++) {
            inputHiddenWeights[i] = weights[i + 1].clone();
        }
        for (int i = 0; i < hiddenSize; i++) {
            hiddenOutputWeights[i] = weights[inputSize + 1 + i].clone();
        }
    }

    double score = 0;
    public void saveScore(long time, int applesEaten){
        if (gameLogs.exists()) {
            // Note, a low score may not be bad and may be a sign of improvement, if the snake is able to stay alive or avoid dying for longer.
            String print = "Time Played (Ticks): " + ((System.currentTimeMillis() - time) / SnakeGame.DELAY) + " | Apples Eaten: " + applesEaten + " | Score: " + score;
            try {
                FileWriter fileWriter = new FileWriter("src/Engine/Data/games.txt", true);
                fileWriter.append(print).append("\n");
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No game logs file found, generating one now.");
            createTrainingFile(false);
        }
    }
    @SuppressWarnings("all")
    private void createTrainingFile(boolean fileType){
        // True for weights file, false for game logs.
        File file = (fileType) ? weightsFile : gameLogs;
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
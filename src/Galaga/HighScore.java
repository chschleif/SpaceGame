package Galaga;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;

/**
 * Class to represent a high-score "claiming" instance, to set a user's new high score among existing scores
 * (and perhaps display them, too)
 *
 * @author Christian Schleif
 * Last modified: 2017 March 24
 */
class HighScore {

    private int selectedNameChar = 0;
    private static final String SCORES_FILE = "scores.dat";
    private static char[][] scoreNameTable;
    private static int[] scoreValueTable;
    private static final int MAX_SCORES = 5;
    private int scoreInsertionPoint = -1;

    public HighScore(int score){
        if (canPostScore(score)) {
            makeScoreInsertion(score);
            scoreValueTable[scoreInsertionPoint] = score;
            scoreNameTable[scoreInsertionPoint] = new char[]{'Y', 'O', 'U'};
        }
    }

    private static void loadScores() {

        scoreNameTable = new char[MAX_SCORES][];
        scoreValueTable = new int[MAX_SCORES];
        for(int i = 0; i <MAX_SCORES; i++) {
            scoreNameTable[i] = new char[]{'A', 'A', 'A'};
            scoreValueTable[i] = 0;
        }

        if (!(new File(SCORES_FILE)).isFile()){
            saveScores();
            return;
        }

        try {
            FileReader fr = new FileReader(SCORES_FILE);
            int entryCount = fr.read();

            for(int i = 0; i < MAX_SCORES; i++){
                if (i < entryCount){
                    char[] name = new char[3];
                    fr.read(name);
                    int score = fr.read();

                    scoreNameTable[i] = name;
                    scoreValueTable[i] = score;
                } else {
                    scoreNameTable[i] = new char[] {'A', 'A', 'A'};
                    scoreValueTable[i] = 0;
                }
            }
            fr.close();
        } catch (FileNotFoundException fnfe) {
            System.err.println("Error creating the read file");
        } catch (IOException iof){
            System.err.println("Error reading file.");
        }
    }

    private static void saveScores() {
        File fileRef = new File(SCORES_FILE);
        if (!fileRef.isFile()) {
            try {
                fileRef.createNewFile();
            } catch (Exception e) {
                System.err.println("Error creating scores file.");
            }
        }
        try {
            FileWriter fw = new FileWriter(SCORES_FILE);
            fw.write(MAX_SCORES);
            for(int i = 0; i < MAX_SCORES; i++){
                fw.write(scoreNameTable[i]);
                fw.write(scoreValueTable[i]);
            }
            fw.flush();
            fw.close();
        } catch (Exception err) {
            System.err.println("Error saving scores");
        }
    }

    private void shiftChar(int distance){
        final int ASCII_FRAME_MAX = 90;
        final int ASCII_FRAME_MIN = 65;
        int value = (int)scoreNameTable[scoreInsertionPoint][selectedNameChar];
        value += distance;
        if (value < ASCII_FRAME_MIN){
            value = (ASCII_FRAME_MAX) - (ASCII_FRAME_MIN - value);
        } else if (value > ASCII_FRAME_MAX){
            value = (ASCII_FRAME_MIN) + (value - ASCII_FRAME_MAX);
        }
        scoreNameTable[scoreInsertionPoint][selectedNameChar] = (char)value;
    }

    /**
     * Check if a score is a high score!
     * @return Whether or not the reported score is good
     */
    public static boolean canPostScore(int score){
        if (scoreValueTable == null){
            loadScores();
        }
        return scoreValueTable[MAX_SCORES - 1] < score;
    }

    /**
     * Make room for the given score at the properly ordered spot
     * @param score the Score to add into the table
     */
    private void makeScoreInsertion(int score){
        if (scoreValueTable == null){
            loadScores();
        }
        for(int i = 0; i < MAX_SCORES; i++){
            if (score > scoreValueTable[i]){
                scoreInsertionPoint = i;
                break;
            }
        }

        // bump everything else down
        for(int i = MAX_SCORES; i < scoreInsertionPoint; i--){
            scoreValueTable[i] = scoreValueTable[i-1];
            scoreNameTable[i] = scoreNameTable[i-1];
        }
        scoreValueTable[scoreInsertionPoint] = score;
    }

    /**
     * Draw the frame for the panel
     * @param g The graphics object to use
     */
    public void draw(Graphics g){
        Dimension frame = GalagaGame.getLevelSize();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, frame.width, frame.height);

        Font drawFont = new Font("Arial", Font.PLAIN, 20);
        g.setFont(drawFont);
        g.setColor(Color.GREEN);
        if (scoreInsertionPoint != -1) {
            g.drawString("Enter your initials for your high score!", 20, 40);
            g.drawString("Use the arrow keys to enter information!", 20, 60);
        }
        g.drawString("Enter to finish", 20, 80);

        Point listCorner = new Point(20, 120);
        for(int i = 0; i < MAX_SCORES; i++){
            int lineHeight = 20;
            if (i == scoreInsertionPoint){
                g.setColor(Color.GREEN);
            } else {
                g.setColor(Color.GRAY);
            }
            g.drawString(""+scoreNameTable[i][0] + scoreNameTable[i][1] + scoreNameTable[i][2] + "      " + scoreValueTable[i],
                    listCorner.x, (lineHeight*(i)) + listCorner.y);
        }
    }

    public void receiveKeyPress(KeyEvent evt) {
        if (scoreInsertionPoint == -1) {
            // handle no changing input from the user
            return;
        }
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                shiftChar(1);
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                shiftChar(-1);
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                selectedNameChar -= 1;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                selectedNameChar += 1;
                break;
            case KeyEvent.VK_ENTER:
                saveScores();
                break;
        }
        if (selectedNameChar > 2) {
            selectedNameChar = 2;
        } else if (selectedNameChar < 0) {
            selectedNameChar = 0;
        }
    }
}

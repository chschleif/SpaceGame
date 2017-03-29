package Galaga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Window element used to display the galaga 'console'
 *
 * @author chschleif
 * Last modified: 2017 March 24
 */
class GalagaWindow extends JFrame{
    public static void main(String[] args){
        GalagaWindow gw = new GalagaWindow();
    }
    private GalagaPanel panel;
    private GalagaGame game;
    private boolean leftDown = false;
    private boolean rightDown = false;
    private boolean spaceDown = false;
    private int gameTickCount = 0;
    private Timer gameDriver;
    private WindowStatus status = WindowStatus.ENTRY;
    private HighScore highScore;

    /**
     * Create/initialize the GalagaWindow object
     */
    private GalagaWindow(){

        // Bind a keyListener as necessary to track keyboard events.
        // allow for arrow AND WASD control!
        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }
            @Override
            public void keyPressed(KeyEvent e) {
                if (getStatus() == WindowStatus.HIGHSCORE){
                    highScore.receiveKeyPress(e);
                    panel.repaint();
                    if (e.getKeyCode() == KeyEvent.VK_ENTER){
                        setStatus(WindowStatus.ENTRY);

                        gameDriver.restart();
                        return; // let's not jump RIGHT back into the game
                    }
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        if(getStatus() == WindowStatus.ENTRY) {
                            game = new GalagaGame(getWidth(), getHeight());
                            panel.setGameRef(game);
                            setStatus(WindowStatus.GAME);
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        leftDown = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        rightDown = true;
                        break;
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_Z:
                    case KeyEvent.VK_SPACE:
                        spaceDown = true;
                        break;
                    case KeyEvent.VK_P:
                        // easy pause
                        if (gameDriver.isRunning()){
                            gameDriver.stop();
                        } else {
                            gameDriver.start();
                        }
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        leftDown = false;
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        rightDown = false;
                        break;
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_Z:
                        spaceDown = false;
                        break;
                }
            }
        });

        // set up the game-driving tick: 25 ms intervals per frame. push the game forward then draw it
        gameDriver = new Timer(23, e -> {
            if (game != null) {
                if (!game.isRunning()) {
                    setStatus(WindowStatus.HIGHSCORE);
                    highScore = new HighScore(game.getScore());
                    panel.setHighScoreReference(highScore);
                    gameDriver.stop(); // passive handling for now
                    game = null;

                } else {
                    game.handleTick(gameTickCount++, spaceDown, rightDown, leftDown);
                }
            }
            panel.repaint();
        });

        // further render the window. give a proper size, position panel
        this.setSize(400, 600);
        this.setLayout(new BorderLayout());

        // begin timer (game), finish building window
        gameDriver.start();
        panel = new GalagaPanel(game);
        this.add(panel);
        this.setVisible(true);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public WindowStatus getStatus(){
        return this.status;
    }

    public void setStatus(WindowStatus status) {
        this.status = status;
        if (panel != null) {
            panel.setStatus(status);
        }
    }

    public enum WindowStatus{
        GAME, HIGHSCORE, ENTRY
    }
}

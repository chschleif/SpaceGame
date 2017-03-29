package Galaga;

import javax.swing.*;
import java.awt.*;

/**
 * Draw the galaga game -- the middle-man between Game and Window.
 *
 * @author Christian Schleif
 *         Last modified: 2017 March 24
 */
class GalagaPanel extends JPanel {

    private GalagaGame game;
    private GalagaWindow.WindowStatus status = GalagaWindow.WindowStatus.ENTRY;
    private HighScore highScoreRef;

    /**
     * Create the panel, tying it to the galaga instance.
     *
     * @param game The galaga game instance.
     */
    public GalagaPanel(GalagaGame game) {
        this.game = game;
    }

    public void setHighScoreReference(HighScore hsref) {
        this.highScoreRef = hsref;
    }

    public void setGameRef(GalagaGame gmRef) {
        this.game = gmRef;
    }

    public void setStatus(GalagaWindow.WindowStatus inStat) {
        this.status = inStat;
    }

    /**
     * Draw the Galaga visuals onto the panel.
     *
     * @param g The graphics to draw with.
     */
    @Override
    public void paintComponent(Graphics g) {

        // Stop frame lag on linux environments
        Toolkit.getDefaultToolkit().sync();

        // Clear old graphics
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        switch (this.status) {

            case GAME:
                for (Actor x : game.collLibrary) {
                    if (x.isAlive()) {
                        x.drawActor(g);
                    }
                }
                break;

            case ENTRY:
                g.setColor(Color.GREEN);
                Font fontLarge = new Font("Arial", Font.BOLD, 32);
                Font fontSmall = new Font("Arial", Font.ITALIC, 12);
                g.setFont(fontLarge);
                drawCenteredString("GALAGA", 60, g);
                g.setFont(fontSmall);
                drawCenteredString("or something like it", 80, g);
                drawCenteredString("Created by Christian Schleif", 560, g);
                g.setFont(fontLarge);
                drawCenteredString("Press ENTER to start ", 530, g);
                break;

            case HIGHSCORE:
                highScoreRef.draw(g);
                break;
        }

    }

    private GalagaWindow getParentWindow(Container element) {
        if (!(element.getParent() instanceof GalagaWindow)) {
            return getParentWindow(element.getParent());
        } else {
            return (GalagaWindow) element.getParent();
        }
    }

    private void drawCenteredString(String msg, int y, Graphics g) {
        int width = getWidth();

        int msgwidth = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, width / 2 - msgwidth / 2, y);
    }
}

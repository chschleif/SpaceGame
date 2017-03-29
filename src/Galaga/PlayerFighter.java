package Galaga;

import java.awt.*;

/**
 * The Actor controlled by the user. Fights. Is green. Can fire weapon.
 *
 * @author chschleif
 * Last modified: 2017 March 24
 */
class PlayerFighter extends Actor {

    /**
     * Draw the playerFighter with some graphics
     * @param g The graphics to draw with
     */
    @Override
    void drawActor(Graphics g) {
        // generate the new points if necessary
        //generateRepresentation();
        g.setColor(Color.GREEN);
        g.fillPolygon(xVals,
                yVals,
                3);
    }

    /**
     * Generate new points for the fighter to exist at. That is, set the vertices
     */
    @Override
    void generateRepresentation() {

        // set the easy-to-read points of a triangle
        Dimension s = getSize();
        Point p = this.getPos();
        xVals = new int[]{s.width / 2, s.width, 0};
        yVals = new int[]{0, s.height, s.height};

        // now offset all those points by the actual position of the actor
        for (int i = 0; i < xVals.length; i++) {
            xVals[i] += p.x;
            yVals[i] += p.y;
        }
    }

    /**
     * Handle collisions by default with no response.
     * @param other The other item connected with
     */
    @Override
    void handleCollision(Actor other) {

    }

    /**
     * Create the (square) playerFighter with size size
     * @param size The width/height of the fighter.
     */
    public PlayerFighter(GalagaGame parent, int size) {
        super(parent);
        this.setSize(new Dimension(size, size));
    }

}

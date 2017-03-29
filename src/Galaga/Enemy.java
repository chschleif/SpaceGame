package Galaga;

import javafx.geometry.Pos;

import java.awt.*;

/**
 * The actor in a game of galaga that opposes the player. Can occasionally follow dive-bombing paths.
 *
 * @author chschleif
 * Last modified: 2017 March 24
 */
class Enemy extends Actor{

    /**
     * Draw the enemy
     * @param g The graphics to draw with
     */
    @Override
    void drawActor(Graphics g) {
        if (super.isAlive()) {
            g.setColor(Color.RED);
            Point loc = this.getPos();
            g.fillOval(loc.x, loc.y, getSize().width, getSize().height);
            //g.drawPolygon(xVals, yVals, xVals.length);
        }
    }

    /**
     * Create a new enemy
     * @param size The size of the enemy
     * @param location The point of location of the enemy (relative to the window/panel/level)
     */
    public Enemy(GalagaGame parent, int size, Point location){
        super(parent);
        this.setSize(new Dimension(size,size));

        this.setPos(location);
        generateRepresentation();
    }

    /**
     * Determine the new points of intersection for the enemy
     */
    void generateRepresentation(){
        /*
        explanation: the origin of this function ties in with the enemy. to apply the SAT (or my previous idea), sides were necessary
        as opposed to one constant curve. So, we approximate a regular polygon to represent the bounds of a circle. the more
        sides, the more circular it is.
         */
        final int CORRECTION_DEPTH = 16;
        xVals = new int[CORRECTION_DEPTH];
        yVals = new int[CORRECTION_DEPTH];
        int s = this.getSize().width / 2;
        Point loc = this.getPos();
        for(int i = 0; i < CORRECTION_DEPTH; i++){
            // generate CORRECTION_DEPTH sides by calculating each endpoint. This is done via unit circle calculations
            double radians = (360 / CORRECTION_DEPTH) * i * Math.PI / 180;
            xVals[i] = loc.x + (int)Math.floor(s * (Math.sin(radians)+1));
            yVals[i] = loc.y + (int)Math.floor(s * (Math.cos(radians)+1));
        }
    }

    /**
     * Handle colliding with another object. Generally, we die, they die.
     * @param other The other object we are colliding with
     */
    @Override
    void handleCollision(Actor other) {
        if (super.isAlive()) {
            if (other instanceof Projectile) {
                if (((Projectile) other).canCollide) {
                    super.setAlive(false);
                }
            } else {
                super.setAlive(false);
                other.setAlive(false);
            }
        }
    }

}

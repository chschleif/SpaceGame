package Galaga;

import java.awt.*;

/**
 * A moving actor 'fired' between other actors to collide with them.
 *
 * @author Christian Schleif
 * Last modified: 2017 March 24
 */
class Projectile extends Actor{
    boolean canCollide = true;

    private static final int width = 2;
    static final int height = 8;

    private int momentumX = 0;
    private int momentumY = 0;

    /**
     * Create a new projectile (shot)
     * @param x The x coordinate to begin at
     * @param y The y coordinate to begin at
     * @param modX The amount to move in X distance per move
     * @param modY The amount to move in Y distance per move
     */
    public Projectile(GalagaGame parent, int x, int y, int modX, int modY){
        // appease the super in case
        super(parent);
        this.setLocation(x, y);
        this.setSize(new Dimension(width, height));
        this.momentumX = modX;
        this.momentumY = modY;

        generateRepresentation();
    }

    /**
     * Move the projectile its given momentum/distance.
     */
    public void move(){
        Point previousLocation = this.getPos();
        setLocation(previousLocation.x + momentumX, previousLocation.y + momentumY);
    }

    /**
     * Draw the projectile as necessary
     * @param g The graphics object to draw with
     */
    @Override
    void drawActor(Graphics g) {
        g.setColor(Color.WHITE);
        Point currentLocation = this.getPos();
        g.fillRect(currentLocation.x, currentLocation.y, width, height);
    }

    /**
     * Create the individual X and Y arrays to represent the actor
     */
    @Override
    void generateRepresentation() {
        Point p = this.getPos();
        this.xVals = new int[] { 0, width, width , 0};
        this.yVals = new int[] { 0, 0, height, height };
        for (int i = 0; i < xVals.length; i++) {
            xVals[i] += p.x;
            yVals[i] += p.y;
        }
    }

    /**
     * The projectile hit something. The offending actor and itself are now no longer.
     * @param other The other actor collided with
     */
    @Override
    void handleCollision(Actor other) {
        other.setAlive(false);
        setAlive(false);
    }
}

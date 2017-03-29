package Galaga;

import java.awt.*;
import java.util.Random;

/**
 * A defined path for an actor (an enemy) to follow
 *
 * @author Christian Schleif
 * Last modified: 2017 March 24
 */
public abstract class TravelingPath {
    private Actor traveler;
    private double position = 0;
    private Point[] p;
    private static double travelDistance = 1.5;

    /**
     * Create a new path for an actor to follow to a point
     * @param pathFollower The actor to follow a path
     * @param destination The destination of the path, the end of the journey.
     */
    public TravelingPath(Actor pathFollower, Point destination){

        // tie the traveler to the window, pick a random bezier influence
        this.traveler = pathFollower;
        Random r = new Random();
        Dimension window = GalagaGame.getLevelSize();

        Point influence = new Point(r.nextInt(window.width), r.nextInt(window.height - 100));
        p = new Point[] { pathFollower.getPos(), influence, destination };
    }

    public static double getTravelDistance() {
        return travelDistance;
    }

    public static void setTravelDistance(double travelDistance) {
        TravelingPath.travelDistance = travelDistance;
    }

    /**
     * Set the new location of the traveler as defined by the curve
     * @see <a href='http://stackoverflow.com/questions/5634460/quadratic-bezier-curve-calculate-point'>StackOverflow formula</a>
     * @see <a href='https://en.wikipedia.org/wiki/B%C3%A9zier_curve'>"Primary" source</a>
     */
    public void move(){
        position += travelDistance;
        double t = position/100;
        // plug the new position into the bezier formula, and move. if at 100%, finish.
        int x = (int)Math.floor((1 - t) * (1 - t) * p[0].x + 2 * (1 - t) * t * p[1].x + t * t * p[2].x);
        int y = (int)Math.floor((1 - t) * (1 - t) * p[0].y + 2 * (1 - t) * t * p[1].y + t * t * p[2].y);
        traveler.setLocation(x, y);
        if (position > 100){
            finished();
        }
    }

    /**
     * Redefine the destination of the traveler in terms of a new bezier curve endpoint
     * @param update The new point to travel to
     */
    public void updateDestination(Point update){
        p[2] = update;
    }

    /**
     * Event handler for when the traveler has reached its destination point.
     */
    public abstract void finished();
}

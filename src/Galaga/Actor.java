package Galaga;

import java.awt.*;

/**
 * The base unit for visible, colliding elements in the Galaga game.
 *
 * @author chschleif
 * Last modified: 2017 March 24
 */
public abstract class Actor {

    private boolean alive = true;

    private boolean canCollide = true;
    private Dimension size;
    private Point myLocation;
    private TravelingPath path = null;
    int[] xVals;
    int[] yVals;
    private GalagaGame parent;

    /**
     * Draw the actor with its custom shape/appearance.
     * @param g The graphics to draw with
     */
    abstract void drawActor(Graphics g);

    /**
     * Create the actor object, adding it to the static library for collisions and rendering
     */
    Actor(GalagaGame parent) {
        this.parent = parent;
        this.parent.collLibrary.add(this);
    }

    /**
     * If possible, remove the actor from the collection. Never really happens unless a throw-away (projectile)
     */
    public void dispose(){
        this.parent.collLibrary.remove(this);
    }

    /**
     * Set the swooping travel path (most likely of an enemy)
     * @param p The path to set to this enemy
     */
    public void setPath(TravelingPath p){
        this.path = p;
    }

    /**
     * Determine if the actor overlaps with another actor's x and y coordinates
     * @param x The array of x coordinates
     * @param y The array of y coordinates
     * @return Whether or not the arrays overlap on the 2D plane
     * @see <a href='http://www.sevenson.com.au/actionscript/sat/'>Separating Axis Theorem explanation </a>
     */
    private boolean doesOverlap(int[] x, int[] y){

        // an object has potentially attempted to test collisions without being properly rendered. Ignore them for now.
        if (xVals == null || x == null){
            return false;
        }

        // Test the SAT using the foreign actor's sides first.
        for(int i = 0; i < x.length; i++){
            Line compareAxis = fromPoints(x[i], y[i], x[(i+1)%x.length], y[(i+1)%x.length]);
            compareAxis = new Line(compareAxis.pt, -1 / compareAxis.slope);

            // Check if there is a proper gap between the shapes on the axis. If yes, we're done.
            boolean test = getGapOneSide(x, y, compareAxis);
            if (!test){
                return false;
            }
        }

        // now test SAT using local actor's sides
        for(int i = 0; i < xVals.length; i++){
            Line compareAxis = fromPoints(xVals[i], yVals[i], xVals[(i+1)%xVals.length], yVals[(i+1)%xVals.length]);
            compareAxis = new Line(compareAxis.pt, -1 / compareAxis.slope);

            // Again, check for a usable gap between shapes on the new axis
            boolean test = getGapOneSide(x, y, compareAxis);
            if (!test){
                return false;
            }
        }

        // No comparison axis had considerable gaps, so we are entirely colliding somewhere (assuming convex)
        return true;
    }

    /**
     * Check if there is a usable gap between a foreign object's x-y coordinates and the local actor's on a particular axis
     * @param x The array of x coordinates
     * @param y The array of y coordinates
     * @param compareAxis The axis to compare for collision status with
     * @return Whether or not there is an acceptable gap between point projections.
     * @see Actor:doesOverlap
     */
    private boolean getGapOneSide(int[] x, int[] y, Line compareAxis){

        // I tried not to have to make this distinction. Floating point math helps. Ultimately, if the line is vertical
        // we need a plan B.
        if (Double.isFinite(compareAxis.slope)) {

            //Compare the min and max x-value intersections of points being projected on the comparison axis.
            // (first, generate them)
            double minXMatch = Integer.MAX_VALUE;
            double maxXMatch = Integer.MIN_VALUE;

            for (int i = 0; i < x.length; i++) {
                PointF intersect = getIntersection(compareAxis, new Line(new PointF(x[i], y[i]), -1 / compareAxis.slope));
                if (minXMatch > intersect.x) {
                    minXMatch = intersect.x;
                }
                if (maxXMatch < intersect.x) {
                    maxXMatch = intersect.x;
                }
            }

            // of course, generate the other shape's, too
            double thisMinXMatch = Integer.MAX_VALUE;
            double thisMaxXMatch = Integer.MIN_VALUE;
            for (int i = 0; i < xVals.length; i++) {
                PointF intersect = getIntersection(compareAxis, new Line(new PointF(xVals[i], yVals[i]), -1 / compareAxis.slope));
                if (thisMinXMatch > intersect.x) {
                    thisMinXMatch = intersect.x;
                }
                if (thisMaxXMatch < intersect.x) {
                    thisMaxXMatch = intersect.x;
                }
            }

            // now test if the ranges overlap or not.
            return (isBetween(minXMatch, maxXMatch, thisMinXMatch) || isBetween(minXMatch, maxXMatch, thisMaxXMatch));
        } else {

            // do what we did earlier, except compare the Y values instead of X values.
            double minYMatch = Integer.MAX_VALUE;
            double maxYMatch = Integer.MIN_VALUE;

            // get the range of projections on the comparison axis for this shape
            for (int i = 0; i < x.length; i++) {
                PointF intersect = getIntersection(compareAxis, new Line(new PointF(x[i], y[i]), -1 / compareAxis.slope));
                if (minYMatch > intersect.y) {
                    minYMatch = intersect.y;
                }
                if (maxYMatch < intersect.y) {
                    maxYMatch = intersect.y;
                }
            }

            // then get the range of projections on the comp axis for the other shape
            double thisMinYMatch = Integer.MAX_VALUE;
            double thisMaxYMatch = Integer.MIN_VALUE;
            for (int i = 0; i < xVals.length; i++) {
                PointF intersect = getIntersection(compareAxis, new Line(new PointF(xVals[i], yVals[i]), -1 / compareAxis.slope));
                if (thisMinYMatch > intersect.y) {
                    thisMinYMatch = intersect.y;
                }
                if (thisMaxYMatch < intersect.y) {
                    thisMaxYMatch = intersect.y;
                }
            }
            return (isBetween(minYMatch, maxYMatch, thisMinYMatch) || isBetween(minYMatch, maxYMatch, thisMaxYMatch));
        }
    }

    /**
     * Determine whether or not a number is between two bounds
     * @param boundA The lower bound to test
     * @param boundB The upper bound to test
     * @param testVal The value to check containment
     * @return Whether or not testVal is between the bounds.
     */
    private boolean isBetween(double boundA, double boundB, double testVal){
        return (boundA <= testVal && testVal <= boundB) || (boundA >= testVal && testVal >= boundB);
    }

    /**
     * Set the location of this object. Also will detect collisions
     * @param x The x coordinate to relocate to
     * @param y The y coordinate to relocate to
     */
    public void setLocation(int x, int y) {

        // Set the new location
        this.setPos(new Point(x, y));

        // Re-determine the new x/y array representations of the vertices/edges of this shape
        generateRepresentation();

        // if relevant, check for collisions. We just move, so see if we're on top of something
        if (this.canCollide && this.isAlive()) {
            for (Actor other : parent.collLibrary) {
                // a checklist:
                // is the other thing alive? are these both enemies? can the other thing even collide?
                // the enemy check might be overzealous and out of position, but it is optimizing
                //          (most ignored collisions would be enemies passing through)
                // if so, then do the more expensive collision check... then dispatch event (to both) if applicable.
                if (other != this && other.isAlive()
                        && !(this instanceof Enemy && other instanceof Enemy)
                        && other.canCollide) {
                    if (doesOverlap(other.xVals, other.yVals)) {
                        handleCollision(other);
                        other.handleCollision(this);
                    }
                }
            }
        }


    }

    /**
     * Determine where the new x/y arrays should point to for this actor's shape
     */
    abstract void generateRepresentation();

    /**
     * Respond to a collision with custom logic if necessary
     */
    abstract void handleCollision(Actor other);

    /**
     * Shift the actor if no rotation occurs and if this is less computationally expensive than a new rendering
     * @param x The x distance to shift
     * @param y The y distance to shift
     */
    public void shift(int x, int y){
        setLocation(getPos().x + x, getPos().y + y);
        for(int i = 0; i < xVals.length; i++){
            xVals[i] += x;
            yVals[i] += y;
        }
    }

    /**
     * Get the size of the current actor
     * @return The size of the actor
     */
    public Dimension getSize() {
        return new Dimension(size);
    }

    /**
     * Set the size of the actor.
     * @param size The new size for the actor.
     */
    void setSize(Dimension size) {
        this.size = size;
        // Regenerate the x/y arrays as we've changed
        // TODO reconsider this
        //generateRepresentation();
    }

    /**
     * Determine the line connecting two points
     * @param x1 The x-coordinate of point 1
     * @param y1 The y-coordinate of point 1
     * @param x2 The x-coordinate of point 2
     * @param y2 The y-coordinate of point 2
     * @return The line connecting the two points
     */
    private Line fromPoints(int x1, int y1, int x2, int y2){

        // we define the lines as slope-point combinations, so just calculate the slope and take a point. easy.
        double slope;
        /*if (x2 == x1) {
            slope = (double)(y2 - y1) / 0;
        } else if (y1 == y2){
            slope = 0 / (double)(x2-x1);
        } else {*/
            slope = (double)(y2 - y1) / (double)(x2 - x1);
        //}-
        return new Line(new PointF(x1, y1), slope);
    }

    /**
     * Determine where two lines (point/slope combinations) intersect
     * @param lineA One of the lines intersecting
     * @param lineB The other line intersecting
     * @return A potentially null pointF representing intersection
     */
    private PointF getIntersection(Line lineA, Line lineB){

        // using some algebra
        double slope = lineA.slope - lineB.slope;
        double x = (lineA.slope * lineA.pt.x - lineA.pt.y - lineB.slope * lineB.pt.x + lineB.pt.y) / (slope);
        double y = ((lineA.slope) * (x - lineA.pt.x) + lineA.pt.y);

        // If the general method does not work (a line is vertical or horizontal) then use the more specific method
        if (Double.isNaN(x) || Double.isNaN(y)){
            return getAuxiliaryIntersection(lineA, lineB);
        }
        return new PointF(x, y);
    }

    /**
     * Determine where two lines intersect if they have unusual slopes
     * @param lineA One line of a pair to calculate
     * @param lineB The other line to calculate with
     * @return The intersection point (if applicable)
     */
    private PointF getAuxiliaryIntersection(Line lineA, Line lineB) {
        if (Double.isInfinite(lineA.slope) && Double.isInfinite(lineB.slope)) {
            // two vertical lines will not intersect
            return null;
        } else if (!Double.isInfinite(lineA.slope) && Double.isInfinite(lineB.slope)) {
            // one line is vertical, one is not, check where the one intersects the other.
            return new PointF(lineB.pt.x, ((lineA.slope) * (lineB.pt.x - lineA.pt.x) + lineA.pt.y));
        } else if (Double.isInfinite(lineA.slope) && !Double.isInfinite(lineB.slope)) {
            return new PointF(lineA.pt.x, ((lineB.slope) * (lineA.pt.x - lineB.pt.x) + lineB.pt.y));
        } else {
            // two horizontal lines will not intersect
            // or, our first method failed and this one can't help. Instead of false answers, we'll just be silent.
            return null;
        }
    }

    /**
     * Whether or not this actor is alive
     * @return Whether or not the actor is alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Set whether or not the actor is alive.
     * @param alive The new living/dead status.
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * Determine whether or not the actor can collide
     * @return The collision status
     */
    public boolean canCollide() {
        return canCollide;
    }

    /**
     * Set whether or not the actor is a ghost
     * @param canCollide Whether or not the actor collides
     */
    public void setCanCollide(boolean canCollide) {
        this.canCollide = canCollide;
    }

    public Point getPos() {
        return new Point(myLocation);
    }

    void setPos(Point myLocation) {
        this.myLocation = new Point(myLocation);
    }

    public TravelingPath getPath() {
        return path;
    }

    /**
     * A simple class to represent a line as a combination of a point and a slope.
     */
    class Line {
        public PointF pt;
        public double slope;
        public Line(PointF pt, double slope){
            this.pt = pt;
            this.slope = slope;
        }
    }

    /**
     * A simple class to represent a point with some floating point precision.
     */
    protected class PointF {
        public double x = 0.0;
        public double y = 0.0;
        public PointF(double x, double y){
            this.x = x;
            this.y = y;
        }
    }
}

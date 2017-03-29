package Galaga;

import java.awt.*;

/**
 * Created by christian on 3/29/17.
 */
public class TractorBeam extends Actor {

    private static final int TRACTOR_WIDTH = 100;
    private static final int TRACTOR_HEIGHT = 100;
    private static final double TRACTOR_TAPER = 0.8;
    private final Point basePoint;
    private double progress = 0;
    private Enemy tiedEnemy;
    private CaptureStatus status = CaptureStatus.APPROACHING;
    private PlayerFighter capturedFighter = null;
    private int pausedTicks = 0;

    public TractorBeam(GalagaGame parent, Point basePoint, Enemy enemy){
        super(parent);
        this.basePoint = new Point(basePoint.x, basePoint.y + parent.DEFAULT_ENEMY_SIZE);
        this.tiedEnemy = enemy;

        TravelingPath p = new TravelingPath(enemy, new Point(basePoint.x, basePoint.y - TRACTOR_HEIGHT)){
            @Override
            public void finished() {
                status = CaptureStatus.ENGAGING_TRACTOR;
                enemy.setPath(null);
            }
        };
        enemy.setLocation(0, 0);
    }

    public CaptureStatus getStatus(){
        return status;
    }

    @Override
    void drawActor(Graphics g) {
        final int LINE_DISTANCE = 10;
        g.setColor(Color.YELLOW);
        for(int y = basePoint.y - TRACTOR_HEIGHT; y < basePoint.y - (TRACTOR_HEIGHT * (1-progress)); y+= LINE_DISTANCE){
            double individualProgress = (double)Math.abs(y - (basePoint.y-TRACTOR_HEIGHT))/TRACTOR_HEIGHT;
            int[] xValues = getLineXCoords(individualProgress);
            g.fillRect(xValues[0], y, xValues[1] - xValues[0], 4);
        }
    }

    @Override
    void generateRepresentation() {
        int[] topXCoords = getLineXCoords(0);
        int[] bottomXCoords = getLineXCoords(0.01);
        int topY = basePoint.y - TRACTOR_HEIGHT;
        int bottomY = (int)(basePoint.y - (TRACTOR_HEIGHT * progress));

        // clockwise
        this.xVals = new int[] { topXCoords[0], topXCoords[1], bottomXCoords[1], bottomXCoords[0]};
        this.yVals = new int[] { topY, topY, bottomY, bottomY};
    }

    @Override
    void handleCollision(Actor other) {
        if (other instanceof PlayerFighter){

            // replace the old fighter for the time being. they die anyway apparently
            System.out.println("haha!");
            PlayerFighter original = (PlayerFighter)other;
            capturedFighter = new PlayerFighter(parent, original);
            capturedFighter.setPos(original.getPos());
            capturedFighter.setAlive(true);

            TravelingPath fighterpath = new TravelingPath(capturedFighter, new Point(tiedEnemy.getPos().x, tiedEnemy.getPos().y + parent.DEFAULT_ENEMY_SIZE)){
                @Override
                public void finished() {
                    // do nothing?!
                }
            };
            fighterpath.updateInfluence(new Point(capturedFighter.getPos().x, basePoint.y - (TRACTOR_HEIGHT/2)));

            status = CaptureStatus.DISENGAGING_TRACTOR;
        }
    }

    /**
     * Determines where the left and right bounds are for the line
     * @param distance Distance, where 0 is base and 1 is peak
     * @return The left and right x coords in array
     */
    private int[] getLineXCoords(double distance) {
        int width = (int) ((TRACTOR_TAPER + (distance * (1 - TRACTOR_TAPER))) * TRACTOR_WIDTH);
        return new int[]{basePoint.x - width / 2, basePoint.x + width / 2};
    }

    /**
     * Advance the distance aspect of the beam
     */
    public void handleTick() {
        if (status == CaptureStatus.ENGAGING_TRACTOR) {
            progress += 0.1;
            if (progress >= 1){
                status = CaptureStatus.PAUSING;
            }
        } else if (status == CaptureStatus.DISENGAGING_TRACTOR) {
            progress -= 0.1;
        }


        if (status == CaptureStatus.PAUSING && pausedTicks++ > 10) {
            status = CaptureStatus.DISENGAGING_TRACTOR;
        }
        generateRepresentation();
        if (progress > 0) {
            checkAllCollision();
        }


        if (progress == 0 && status == CaptureStatus.DISENGAGING_TRACTOR){
            TravelingPath enemypath = new TravelingPath(tiedEnemy, new Point(basePoint.x, basePoint.y - TRACTOR_HEIGHT)) {
                @Override
                public void finished() {

                }

                /**
                 * Return the enemy to its position
                 */
                @Override
                public void move() {
                    super.move();
                    Point pos = parent.getEnemyArrayLocation(tiedEnemy);
                    this.updateDestination(parent.getSingleClusterOffset(pos.x, pos.y));
                }
            };

            TravelingPath fighterpath = new TravelingPath(capturedFighter, new Point(basePoint.x, basePoint.y - TRACTOR_HEIGHT)) {
                @Override
                public void finished() {

                }

                /**
                 * Return the enemy to its position
                 */
                @Override
                public void move() {
                    super.move();
                    Point pos = parent.getEnemyArrayLocation(tiedEnemy);
                    this.updateDestination(parent.getSingleClusterOffset(pos.x, pos.y - parent.DEFAULT_ENEMY_SIZE));
                }
            };
            fighterpath.updateDestination(enemypath.getInfluence());
            status = CaptureStatus.LEAVING;
        }
    }

    public enum CaptureStatus{
        APPROACHING,
        ENGAGING_TRACTOR,
        PAUSING,
        DISENGAGING_TRACTOR,
        LEAVING
    }
}

package Galaga;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * Game logic related to running a game that's similar and not entirely unlike Galaga. Or galaxa? Or space invaders?
 *
 * @author Christian Schleif
 * Last modified: 2017 March 24
 */
public class GalagaGame {

    private final int DEFAULT_PLAYER_LIVES = 3;
    private final int DEFAULT_PLAYER_DIED_TIMEOUT = 200;
    private final int DEFAULT_ENEMY_SIZE = 24;
    private final int ENEMY_SPACING = 12;
    private final int WINDOW_MARGIN = 20;

    private int enemyRows = 6;
    private int enemyCols;
    private int NUDGE_DISTANCE = 1;

    private int playerLives = DEFAULT_PLAYER_LIVES;
    private int deadPlayerTimeout = -1;
    private int lastFighterShot = 0;

    private int maximumMovingEnemies = 2;
    private int enemyDispatchDelay = 40;
    private int enemyFireDelay =40;

    private Direction clusterDir = Direction.EAST;
    private Enemy[][] enemies;
    private Point enemyCorner;

    private PlayerFighter fighter;
    private PlayerFighter[] lifeCounter;
    private static Dimension levelSize;

    ArrayList<Actor> collLibrary = new ArrayList<>();
    private ArrayList<Projectile> shots = new ArrayList<>();
    private final int PROJECTILE_TICK_DISTANCE = 8;
    private int level = 1;

    private int score = 0;

    /**
     * Create a new Galaga Game, supplying the width and height for positioning
     * @param width Width of the galaga window (margins are handled internally)
     * @param height Height of the galaga window
     */
    public GalagaGame(int width, int height){
        levelSize = new Dimension(width, height);

        // Create player's actor: fighter. Override collision to gain direct collision response.
        fighter = new PlayerFighter(this, DEFAULT_ENEMY_SIZE){
            @Override
            public void handleCollision(Actor other){
                super.handleCollision(other);
                deadPlayerTimeout = DEFAULT_PLAYER_DIED_TIMEOUT;
            }
        };

        // Place fighter horizontally centered, vertically slightly off the bottom.
        fighter.setLocation(levelSize.width / 2 - fighter.getSize().width / 2,
                levelSize.height - WINDOW_MARGIN - DEFAULT_ENEMY_SIZE);

        setLevelParams(level);

        Actor scoreDisplay = new Actor(this){

            @Override
            void drawActor(Graphics g) {
                g.setColor(Color.GREEN);
                g.setFont(new Font("Arial", Font.PLAIN, 16));
                g.drawString("Score: " + score, getPos().x, getPos().y);
            }

            @Override
            void generateRepresentation() {
                this.xVals = new int[0];
                this.yVals = new int[0];
            }

            @Override
            void handleCollision(Actor other) {

            }
        };
        scoreDisplay.setLocation(5, 16);
        scoreDisplay.setCanCollide(false);
        collLibrary.add(scoreDisplay);
    }

    /**
     * Update the galaga game by supplying a moment tick. (25 ms interval recommended)
     * @param tick The tick number to base movements off of
     * @param space Whether or not the space bar is pressed
     * @param right Whether or not the left button is pressed
     * @param left Whether or not the right button is pressed
     */
    public void handleTick (int tick, boolean space, boolean right, boolean left){

        // Move enemies every other 'frame'
        if (tick % 2 == 0){
            this.moveCluster();
        }
        this.moveShots();

        // If desired, fire the fighter weapon, if alloweed
        if (space && tick - lastFighterShot > 16){
            lastFighterShot = tick;
            this.fireFighterWeapon();
        }

        // move left if only left, move right if only right
        if (left && !right) {
            this.moveFighter(GalagaGame.Direction.WEST);
        }
        else if (right && !left) {
            this.moveFighter(GalagaGame.Direction.EAST);
        }

        // Fire enemy weapon every 40 frames
        if ((enemyFireDelay == 0 || tick % enemyFireDelay == 0) && fighter.isAlive()){
            fireEnemyWeapon();
        }

        // If player died recently, tick off some time. If they died a while ago, consume a life and begin the game again
        // if they haven't died recently or a while ago, send the enemies swooping down.
        if (deadPlayerTimeout > 0){
            deadPlayerTimeout -= 1;
        } else if (deadPlayerTimeout == 0){
            deadPlayerTimeout = -1;
            if (playerLives > 0){
                consumeLife();
            }
        } else if ((enemyDispatchDelay == 0 || tick % enemyDispatchDelay == 0)
                && getEnemyPathCount() <= maximumMovingEnemies){

            // select an enemy to swoop
            Random r = new Random();
            int tempx =r.nextInt(enemyCols);
            int tempy = r.nextInt(enemyRows);
            while (!enemies[tempx][tempy].isAlive()){
                if (getExtreme(Direction.NORTH) == -1){
                    // new level!
                    level++;
                    setLevelParams(level);
                    return;
                }
                tempx =r.nextInt(enemyCols);
                tempy = r.nextInt(enemyRows);
            }

            // the anonymous object requires "effectively final" variables, so restate them
            int x = tempx;
            int y =tempy;

            Point fighterPoint = fighter.getPos();
            // Send this enemy on a path. Define which it is, where it's going, and override its finish function
            Enemy chosen = enemies[x][y];
            chosen.setPath(new TravelingPath(chosen, new Point(fighterPoint.x, fighterPoint.y+100)){

                /**
                 * When the enemy has finished going past the fighter, restart it above the view to swoop back in.
                 */
                @Override
                public void finished() {
                    // above the view
                    chosen.setLocation(chosen.getPos().x, -100);
                    // a new path to swoop back in. when done, clear references to the path.
                    chosen.setPath(new TravelingPath(chosen, getSingleClusterOffset(x, y)){

                        /**
                         * When done, clear the references.
                         */
                        @Override
                        public void finished() {
                            if (chosen.getPath() != null) {
                                chosen.setPath(null);
                                score+=5; // five points for surviving a pass-over
                            }
                        }
                        /**
                         * Allow for potential upper-level logic... and update the destination as the goal keeps moving
                         */
                        @Override
                        public void move(){
                            super.move();
                            this.updateDestination(getSingleClusterOffset(x, y));
                        }
                    });
                }
            });
        }
    }

    private void setLevelParams(int level){

        TravelingPath.setTravelDistance(1.2 + (0.3)*level);
        for(Projectile shot : (new ArrayList<>(shots))){
            shot.dispose();
            shots.remove(shot);
        }

        enemyRows = 6 + (level-1)/2;
        maximumMovingEnemies = 2 + (level-1)*2;

        enemyDispatchDelay = 40 - 5 * (level-1);
        enemyFireDelay = (40 - 5 * (level - 1));

        score += 30 * (level-1);

        if (level % 4 == 0){
            playerLives += 1;
        }

        // erase old enemies
        if (enemies != null){
            for (Enemy[] enemy : enemies) {
                for (Enemy anEnemy : enemy) {
                    anEnemy.dispose();
                }
            }
        }

        // clear life counter
        if (lifeCounter != null){
            for (PlayerFighter aLifeCounter : lifeCounter) {
                aLifeCounter.dispose();
            }
        }

        // Initialize sizes, create enemies
        int usableWidth = (levelSize.width - WINDOW_MARGIN*2);
        enemyCols = usableWidth / (DEFAULT_ENEMY_SIZE + ENEMY_SPACING);
        this.enemies = new Enemy[enemyCols][enemyRows];
        for(int x = 0; x < enemyCols; x++){
            for(int y = 0; y < enemyRows; y++){
                enemies[x][y] = new Enemy(this, DEFAULT_ENEMY_SIZE, new Point(0, 0));
            }
        }

        // Position enemies
        Dimension clusterSize = getClusterSize();
        enemyCorner = new Point((levelSize.width - clusterSize.width)/2, WINDOW_MARGIN);
        setClusterPos(enemyCorner);

        // Create dummy fighter actors to represent remaining lives.
        lifeCounter = new PlayerFighter[playerLives];
        for(int i = 0; i < playerLives; i++){
            PlayerFighter pf = new PlayerFighter(this, DEFAULT_ENEMY_SIZE-6);
            pf.setLocation(pf.getSize().width * i, levelSize.height - pf.getSize().height);
            pf.setCanCollide(false);
            lifeCounter[i] = pf;
        }
    }

    /**
     * Subtract one life from the count and hide one fighter marker.
     */
    private void consumeLife(){
        // if there's a life left, subtract it and hide a "life marker"
        if (playerLives > 0) {
            playerLives--;
            fighter.setAlive(true);
            for (int i = lifeCounter.length - 1; i >= 0; i--) {
                if (lifeCounter[i].isAlive()) {
                    lifeCounter[i].setAlive(false);
                    break;
                }
            }
        }
    }

    /**
     * select a random enemy near the fighter to fire.
     */
    private void fireEnemyWeapon(){

        // make sure to pick the bottom ones or they'll shoot each other.
        Random r = new Random();
        while (getExtreme(Direction.WEST) != -1) { // -1 means no one is alive
            int col = r.nextInt(enemyCols);
            for (int y = enemyRows - 1; y >= 0; y--) {
                if (enemies[col][y].isAlive()) {

                    // found a suitable enemy
                    Enemy e = enemies[col][y];
                    Point p = e.getPos();

                    // "fire" (create) the projectile
                    Dimension dim = e.getSize();
                    Projectile proj = new Projectile(this, p.x + dim.width / 2, p.y + dim.height + 4, 0, PROJECTILE_TICK_DISTANCE);

                    shots.add(proj);
                    return;
                }
            }
        }
    }

    /**
     * Fire the player's fighter weapon from its current position
     */
    private void fireFighterWeapon(){
        if (!fighter.isAlive()){
            return;
        }

        // Create a new projectile, position it properly, add it to animation collection
        Point p = fighter.getPos();
        Dimension dim = fighter.getSize();
        Projectile proj = new Projectile(this,p.x + dim.width / 2, p.y - Projectile.height - 4, 0, -PROJECTILE_TICK_DISTANCE){
            @Override
            public void handleCollision(Actor other){
                super.handleCollision(other);
                // handle scoring
                if (other instanceof Enemy){
                    score += 20;
                } else if (other instanceof Projectile){
                    score += 10;
                }
            }
        };
        shots.add(proj);
    }

    /**
     * Iterate over the "animation" collection of shots to move them as necessary
     */
    private void moveShots(){
        // remove if necessary, but keep in mind we can't operate on a list that's being operated on
        ArrayList<Projectile> toRemove = new ArrayList<>();
        for(Projectile x : shots){
            x.move();
            Point p = x.getPos();
            if (p.x < 0 || p.x > levelSize.width || p.y < 0 || p.y > levelSize.height){
                toRemove.add(x);
            }
        }
        for(Projectile y : toRemove){
            y.dispose();
            shots.remove(y);
        }
    }

    /**
     * Move the player's fighter a given direction (East or West)
     * @param dir Move in a given direction, limited to EAST or WEST
     */
    private void moveFighter(Direction dir){
        // do not move a dead fighter
        if (fighter.isAlive()) {
            Point pos = fighter.getPos();

            // Do not move outside the window margins, move based on the base nudge distance
            if (dir == Direction.EAST && pos.x + fighter.getSize().width < levelSize.width - WINDOW_MARGIN) {
                fighter.setLocation(pos.x + NUDGE_DISTANCE * 4, pos.y);
            } else if (dir == Direction.WEST && pos.x > WINDOW_MARGIN) {
                fighter.setLocation(pos.x - NUDGE_DISTANCE * 4, pos.y);
            }
        }
    }

    /**
     * Move the enemy swarm/cluster one step.
     */
    private void moveCluster(){
        // move cluster. then calculate if we're overstepping the bounds. if so, switch directions and move back.
        if (clusterDir == Direction.EAST){
            setClusterPos(new Point(enemyCorner.x + NUDGE_DISTANCE, enemyCorner.y));
            if (enemyCorner.x + (enemyCols * ((DEFAULT_ENEMY_SIZE+ENEMY_SPACING))-ENEMY_SPACING) - getRightClusterSpace()
                    > levelSize.width - WINDOW_MARGIN){
                clusterDir = Direction.WEST;
                setClusterPos(new Point(enemyCorner.x - NUDGE_DISTANCE, enemyCorner.y + NUDGE_DISTANCE*2));
            }
        } else if (clusterDir == Direction.WEST) {
            setClusterPos(new Point(enemyCorner.x - NUDGE_DISTANCE, enemyCorner.y));
            if (enemyCorner.x + getLeftClusterSpace() < WINDOW_MARGIN){
                clusterDir = Direction.EAST;
                setClusterPos(new Point(enemyCorner.x + NUDGE_DISTANCE, enemyCorner.y + NUDGE_DISTANCE*2));
            }
        }
    }

    /**
     * (Copy) Accessor for the levelSize variable
     * @return A copy of the levelSize information.
     */
    public static Dimension getLevelSize(){
        return new Dimension(levelSize);
    }

    /**
     * Calculate how many enemies are 'missing' from the grid in terms of columns on the left
     * @return The empty columns' pixel space on the left
     */
    private int getLeftClusterSpace(){
        return (DEFAULT_ENEMY_SIZE+ENEMY_SPACING)*getExtreme(GalagaGame.Direction.WEST);
    }

    /**
     * Calculate how many enemies are 'missing' from the grid in terms of columns on the right
     * @return The empty columns' pixel space on the right
     */
    private int getRightClusterSpace(){
        return (DEFAULT_ENEMY_SIZE+ENEMY_SPACING)*(this.enemyCols - getExtreme(GalagaGame.Direction.EAST));
    }

    /**
     * Set the cluster/swarm of enemies positioning by specifying the upper left corner
     * @param pos The upper-left corner Point
     */
    private void setClusterPos(Point pos){
        enemyCorner = pos;

        for(int x = 0; x < enemyCols; x++){
            for(int y = 0; y < enemyRows; y++){

                // for each enemy, move them along with the grid IFF the enemy is not on some other predefined path
                if (enemies[x][y].getPath() == null) {
                    Point p = getSingleClusterOffset(x, y);
                    enemies[x][y].setLocation(p.x, p.y);
                } else {
                    // they have their own path, so move them accordingly (unless dead)
                    // update our destination in case it has moved
                    if (enemies[x][y].isAlive()) {
                        enemies[x][y].getPath().move();
                    } else {
                        enemies[x][y].setPath(null);
                    }
                }
            }
        }
    }

    private int getEnemyPathCount(){
        int count = 0;
        for (Enemy[] enemy : enemies) {
            for (Enemy anEnemy : enemy) {
                if (anEnemy.getPath() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Determine where an enemy should be if part of the cluster in their original position
     * @param x The column the enemy was in the grid
     * @param y The row the enemy was in the grid
     * @return The position of where the enemy would be, relative to the upper left swarm corner. (not the level!)
     */
    private Point getSingleClusterOffset(int x, int y){
        return new Point(x * (DEFAULT_ENEMY_SIZE + ENEMY_SPACING) + enemyCorner.x,
        y * (DEFAULT_ENEMY_SIZE + ENEMY_SPACING) + enemyCorner.y);
    }

    /**
     * Determine which row or column of the swarm is first filled in, starting with the given direction.
     * @param dir The side to start on. (NORTH checks the top row, then the next)
     * @return The first row or column to have a living enemy in the swarm
     */
    private int getExtreme(Direction dir){
        switch(dir){
            case NORTH:
            case SOUTH:
                for(int y = 0; y < enemyRows; y++){
                    if (dir == Direction.SOUTH && anyAliveInRow(enemyRows -y-1)){
                        return enemyRows -y;
                    } else if (dir == Direction.NORTH && anyAliveInRow(y)){
                        return y;
                    }
                }
                break;
            case EAST:
            case WEST:
                for(int x = 0; x < enemyCols; x++){
                    if (dir == Direction.EAST && anyAliveInCol(enemyCols - x - 1)){
                        return enemyCols-x;
                    } else if (dir == Direction.WEST && anyAliveInCol(x)){
                        return x;
                    }
                }
        }
        // error :(
        return -1;
    }

    /**
     * Calculate the true size of the cluster, based on living enemies
     * @return The dimensions of the cluster
     */
    private Dimension getClusterSize(){
        Dimension enemySize = enemies[0][0].getSize();
        int enemyWidth = Math.abs(getExtreme(Direction.WEST) - getExtreme(Direction.EAST))
                * (enemySize.width+ENEMY_SPACING)-ENEMY_SPACING;
        int enemyHeight = Math.abs(getExtreme(Direction.SOUTH) - getExtreme(Direction.NORTH))
                * (enemySize.height+ENEMY_SPACING) - ENEMY_SPACING;
        return new Dimension(enemyWidth, enemyHeight);
    }

    /**
     * Loop through the row to determine if any enemies are alive
     * @param row The row index to check
     * @return Whether or not any living enemies were found
     */
    private boolean anyAliveInRow(int row){
        for(int x = 0; x < enemyCols; x++){
            if (enemies[x][row].isAlive()){
                return true;
            }
        }
        return false;
    }

    /**
     * Loop through the column to determine if any enemies are alive
     * @param col The column index to check
     * @return Whether or not any living enemies were found
     */
    private boolean anyAliveInCol(int col){
        for(int y = 0; y < enemyRows; y++){
            if (enemies[col][y].isAlive()){
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if the game can still continue
     * @return Whether or not the player is alive or has more lives
     */
    public boolean isRunning(){
        return fighter.isAlive() || playerLives > 0;
    }

    /**
     * Get the game's score total
     * @return The score of the game
     */
    public int getScore(){
        return score;
    }

    /**
     * A simple enum to dictate screen directions
     */
    public enum Direction{
        NORTH, SOUTH, EAST, WEST
    }
}

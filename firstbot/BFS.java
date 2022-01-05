package firstbot;

import battlecode.common.*;

public abstract class BFS {

    final int BYTECODE_REMAINING = 1000;
    final int GREEDY_TURNS = 4;

    Pathfinding path;
    static RobotController rc;
    MapTracker mapTracker = new MapTracker();

    int turnsGreedy = 0;
    MapLocation currentTarget = null;




    BFS(RobotController rc){
        this.rc = rc;
        this.path = new Pathfinding(rc);
    }

    void reset(){
        turnsGreedy = 0;
        mapTracker.reset();
    }

    void update(MapLocation target){
        if (currentTarget == null || target.distanceSquaredTo(currentTarget) > 0){
            reset();
        } else --turnsGreedy;
        currentTarget = target;
        mapTracker.add(rc.getLocation());
    }

    void activateGreedy(){
        turnsGreedy = GREEDY_TURNS;
    }

    void initTurn(){
        path.initTurn();
    }

    void move(MapLocation target){
        move(target, false);
    }

    void move(MapLocation target, boolean greedy){
        if (target == null) return;
        if (!rc.isMovementReady()) return;
        if (rc.getLocation().distanceSquaredTo(target) == 0) return;

        update(target);

        if (!greedy && turnsGreedy <= 0){

            rc.setIndicatorString("Using bfs!!!");
            Direction dir = getBestDir(target);
            if (dir != null && !mapTracker.check(rc.getLocation().add(dir))){
                move(dir);
                return;
            } else activateGreedy();
        }

        if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING){
            path.move(target);
            --turnsGreedy;
        }
    }

    void move(Direction dir){
        try{
            if (!rc.canMove(dir)) return;
            rc.move(dir);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    abstract Direction getBestDir(MapLocation target);


}
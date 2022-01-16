package fifteeen;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class BFS {

    final int BYTECODE_REMAINING = 1000;
    final int GREEDY_TURNS = 4;

    Pathfinding path;
    Micro micro;
    static RobotController rc;
    MapTracker mapTracker = new MapTracker();

    int turnsGreedy = 0;
    MapLocation currentTarget = null;




    BFS(RobotController rc){
        this.rc = rc;
        this.path = new Pathfinding(rc);
        if (Util.isAttacker(rc.getType())){
            this.micro = new MicroAttacker(rc);
        } else{
            this.micro = new MicroMiners(rc);
            this.micro.setBFS(this);
        }
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
        if (!rc.isMovementReady()){
            return;
        }

        //System.out.println("Before micro " + Clock.getBytecodeNum());

        if (micro.doMicro()){
            rc.setIndicatorString("Did micro");
            reset();
            return;
        }

        //System.out.println("After micro " + Clock.getBytecodeNum());

        if (target == null) return;

        if (rc.getLocation().distanceSquaredTo(target) == 0) return;

        update(target);

        if (!greedy && turnsGreedy <= 0){

            int t = Clock.getBytecodesLeft();
            Direction dir = getBestDir(target);
            t = Clock.getBytecodesLeft() - t;
            rc.setIndicatorString("Using bfs!!! " + t);
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
package fourteen;

import battlecode.common.*;

public abstract class Robot {

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };

    static RobotController rc;
    static BFS bfs;
    static Explore explore;
    static Communication comm;
    int creationRound;
    boolean reportLeadAtBeginning;


    public Robot(RobotController rc){
        this.rc = rc;

        //BFS
        if (rc.getType().isBuilding()){
            //TODO
        } else{
            bfs = new BFSDroid(rc);
        }

        //Explore class
        explore = new Explore(rc);

        //Communication class
        comm = new Communication(rc);


        creationRound = rc.getRoundNum();
        reportLeadAtBeginning = rc.getType() == RobotType.ARCHON || rc.getType() == RobotType.MINER;
        switch(rc.getType()){
        }
    }

    abstract void play();

    void initTurn(){
        comm.reportSelf();
        if (reportLeadAtBeginning) explore.reportLead();
        if (bfs != null) bfs.initTurn();
    }

    void endTurn(){
        //if (!reportLeadAtBeginning) explore.reportLead();
        //explore.reportUnits();
        checkDanger();
    }

    boolean constructRobotGreedy(RobotType t){
        return constructRobotGreedy(t, null);
    }

    boolean constructRobotGreedy(RobotType t, MapLocation target){
        try {
            MapLocation myLoc = rc.getLocation();
            Direction bestDir = null;
            int leastEstimation = 0;
            for (Direction d : directions) {
                if (!rc.canBuildRobot(t,d)) continue;
                int e;
                if (target != null) e = myLoc.add(d).distanceSquaredTo(target);
                else e = rc.senseRubble(myLoc.add(d));
                if (bestDir == null || e < leastEstimation){
                    leastEstimation = e;
                    bestDir = d;
                }
            }
            if (bestDir != null){
                if (rc.canBuildRobot(t, bestDir)) rc.buildRobot(t, bestDir);
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    void moveRandom(){
        try {
            int d = (int) (Math.random() * 8.0);
            Direction dir = directions[d];
            for (int i = 0; i < 8; ++i) {
                if (rc.canMove(dir)) rc.move(dir);
                dir = dir.rotateLeft();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    boolean enemyNearby(){
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), explore.myVisionRange, (rc.getTeam()).opponent());
        for (RobotInfo enemy : enemies){
            if (Clock.getBytecodesLeft() < 100) return false;
            if (Util.isAttacker(enemy.getType())) return true;
        }
        return false;
    }

    void checkDanger(){
        if (enemyNearby()) comm.activateDanger();
    }

}

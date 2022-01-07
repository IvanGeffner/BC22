package secondbot;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

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

    RobotController rc;
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
    }

    void endTurn(){
        if (!reportLeadAtBeginning) explore.reportLead();
        explore.reportUnits();
    }

    boolean constructRobotGreedy(RobotType t){
        //System.out.println("Trying to make " + t.name());
        try {
            MapLocation myLoc = rc.getLocation();
            Direction bestDir = null;
            int minRubble = -1;
            for (Direction d : directions) {
                if (rc.canBuildRobot(t, d)) {
                    int r = rc.senseRubble(myLoc);
                    if (minRubble < 0 || r < minRubble){
                        minRubble = r;
                        bestDir = d;
                    }
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

}

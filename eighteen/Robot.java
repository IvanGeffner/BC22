package eighteen;

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
            bfs = new BFSArchon(rc);
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
        checkDanger();
        comm.symmetryChecker.checkSymmetry();
    }

    boolean constructRobotGreedy(RobotType t){
        return constructRobotGreedy(t, null);
    }

    boolean constructRobotGreedy(RobotType t, MapLocation target){
        try {
            MapLocation myLoc = rc.getLocation();
            BuildRobotLoc bestBRL = null;
            for (Direction d : directions) {
                BuildRobotLoc brl = new BuildRobotLoc(t, d, target);
                if (brl.isBetterThan(bestBRL)) bestBRL = brl;
            }
            if (bestBRL != null){
                if (rc.canBuildRobot(t, bestBRL.dir)) rc.buildRobot(t, bestBRL.dir);
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    class BuildRobotLoc {

        MapLocation loc;
        Direction dir;
        int rubble;
        int distToTarget;
        boolean canBuild;

        BuildRobotLoc(RobotType r, Direction dir, MapLocation target){
            this.canBuild = rc.canBuildRobot(r, dir);
            try {
                if (canBuild) {
                    this.loc = rc.getLocation().add(dir);
                    this.dir = dir;
                    this.rubble = rc.senseRubble(loc);
                    if (target != null) distToTarget = loc.distanceSquaredTo(target);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        boolean isBetterThan(BuildRobotLoc brl){
            if (!canBuild) return false;
            if (brl == null || !brl.canBuild) return true;
            if (rubble < brl.rubble) return true;
            if (rubble > brl.rubble) return false;
            return distToTarget < brl.distToTarget;
        }

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

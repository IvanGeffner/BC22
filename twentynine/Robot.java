package twentynine;

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
    boolean attacker = false;
    boolean archon = false;

    int[] minDistAttackers;

    static int IC = Direction.CENTER.ordinal();
    static int IN = Direction.NORTH.ordinal();
    static int IS = Direction.SOUTH.ordinal();
    static int IE = Direction.EAST.ordinal();
    static int IW = Direction.WEST.ordinal();
    static int INE = Direction.NORTHEAST.ordinal();
    static  int INW = Direction.NORTHWEST.ordinal();
    static int ISE = Direction.SOUTHEAST.ordinal();
    static int ISW = Direction.SOUTHWEST.ordinal();




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
        if (rc.getType() == RobotType.SOLDIER || rc.getType() == RobotType.SAGE) attacker = true;
        if (rc.getType() == RobotType.ARCHON) archon = true;
        switch(rc.getType()){
        }
    }

    void computeMinDistAttackers(){
        minDistAttackers = new int[9];

        minDistAttackers[0] = 100000;
        minDistAttackers[1] = 100000;
        minDistAttackers[2] = 100000;
        minDistAttackers[3] = 100000;
        minDistAttackers[4] = 100000;
        minDistAttackers[5] = 100000;
        minDistAttackers[6] = 100000;
        minDistAttackers[7] = 100000;
        minDistAttackers[8] = 100000;

        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().visionRadiusSquared, rc.getTeam().opponent());

        MapLocation newLocC = rc.getLocation();
        MapLocation newLocN = rc.adjacentLocation(Direction.NORTH);
        MapLocation newLocS = rc.adjacentLocation(Direction.SOUTH);
        MapLocation newLocE = rc.adjacentLocation(Direction.EAST);
        MapLocation newLocW = rc.adjacentLocation(Direction.WEST);
        MapLocation newLocNE = rc.adjacentLocation(Direction.NORTHEAST);
        MapLocation newLocNW = rc.adjacentLocation(Direction.NORTHWEST);
        MapLocation newLocSE = rc.adjacentLocation(Direction.SOUTHEAST);
        MapLocation newLocSW = rc.adjacentLocation(Direction.SOUTHWEST);

        for (RobotInfo r : enemies){
            if (!Util.isAttacker(r.getType()) && r.getType() != RobotType.WATCHTOWER) continue;
            MapLocation aLoc = r.getLocation();
            int d = aLoc.distanceSquaredTo(newLocC);
            if (d < minDistAttackers[IC]) minDistAttackers[IC] = d;
            d = aLoc.distanceSquaredTo(newLocN);
            if (d < minDistAttackers[IN]) minDistAttackers[IN] = d;
            d = aLoc.distanceSquaredTo(newLocS);
            if (d < minDistAttackers[IS]) minDistAttackers[IS] = d;
            d = aLoc.distanceSquaredTo(newLocE);
            if (d < minDistAttackers[IE]) minDistAttackers[IE] = d;
            d = aLoc.distanceSquaredTo(newLocW);
            if (d < minDistAttackers[IW]) minDistAttackers[IW] = d;
            d = aLoc.distanceSquaredTo(newLocNE);
            if (d < minDistAttackers[INE]) minDistAttackers[INE] = d;
            d = aLoc.distanceSquaredTo(newLocNW);
            if (d < minDistAttackers[INW]) minDistAttackers[INW] = d;
            d = aLoc.distanceSquaredTo(newLocSE);
            if (d < minDistAttackers[ISE]) minDistAttackers[ISE] = d;
            d = aLoc.distanceSquaredTo(newLocSW);
            if (d < minDistAttackers[ISW]) minDistAttackers[ISW] = d;
        }
    }

    abstract void play();

    void initTurn(){
        comm.reportSelf();
        if (reportLeadAtBeginning) explore.reportLead();
        if (attacker || archon) comm.unitReporter.readReports(false, 6000);
        if (bfs != null) bfs.initTurn();
    }

    void endTurn(){
        checkDanger();
        if (!comm.unitReporter.ready) comm.unitReporter.checkReady();
        else {
            if (!attacker && !archon) comm.unitReporter.readReports(false, 1500);
            comm.unitReporter.reportBestEnemy(1000);
        }
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

        boolean isDangerous(){
            return minDistAttackers[dir.ordinal()] <= 20;
        }

        boolean isBetterThan(BuildRobotLoc brl){
            if (!canBuild) return false;
            if (brl == null || !brl.canBuild) return true;
            if (rubble < brl.rubble) return true;
            if (rubble > brl.rubble) return false;

            if (!isDangerous() && brl.isDangerous()) return true;
            if (isDangerous() && !brl.isDangerous()) return false;

            if (isDangerous()){
                return minDistAttackers[dir.ordinal()] > minDistAttackers[brl.dir.ordinal()];
            }

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

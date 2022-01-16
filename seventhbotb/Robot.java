package seventhbotb;

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


    void tryAttack(boolean onlyAttackers){
        if (!rc.isActionReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        AttackTarget bestTarget = null;
        for (RobotInfo enemy : enemies){
            if (onlyAttackers && Util.isAttacker(enemy.getType())) continue;
            if (rc.canAttack(enemy.location)){
                AttackTarget at = new AttackTarget(enemy);
                if (at.isBetterThan(bestTarget)) bestTarget = at;
            }
        }
        try {
            if (bestTarget != null && rc.canAttack(bestTarget.mloc)) rc.attack(bestTarget.mloc);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    MapLocation getBestTarget(){
        MoveTarget bestTarget = null;
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), explore.myVisionRange, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies){
            MoveTarget mt = new MoveTarget(enemy);
            if (mt.isBetterThan(bestTarget)) bestTarget = mt;
        }
        if (bestTarget != null) return bestAim(bestTarget.mloc);
        return null;
    }

    MapLocation bestAim(MapLocation target){
        AimTarget[] aims = new AimTarget[9];
        for (Direction dir : directions) aims[dir.ordinal()] = new AimTarget(dir, target);
        int minRubble = aims[8].rubble;
        for (int i = 8; i-- > 0; ){
            if (aims[i].canMove && aims[i].rubble < minRubble) minRubble = aims[i].rubble;
        }
        minRubble += Constants.MAX_RUBBLE_DIFF_MOVE;
        for (int i = 8; i-- > 0; ){
            if (aims[i].rubble > minRubble) aims[i].canMove = false;
        }
        AimTarget bestTarget = aims[8];
        for (int i = 8; i-- > 0; ){
            if (aims[i].isBetterThan(bestTarget)){
                bestTarget = aims[i];
            }
        }
        return bestTarget.loc;

    }



    class AttackTarget{
        RobotType type;
        int health;
        boolean attacker = false;
        MapLocation mloc;

        boolean isBetterThan(AttackTarget t){
            if (t == null) return true;
            if (attacker & !t.attacker) return true;
            if (!attacker & t.attacker) return false;
            return health <= t.health;
        }

        AttackTarget(RobotInfo r){
            type = r.getType();
            health = r.getHealth();
            mloc = r.getLocation();
            switch(type){
                case SOLDIER:
                case SAGE:
                case WATCHTOWER:
                case ARCHON:
                    attacker = true;
                default:
                    break;
            }
        }
    }

    class MoveTarget{
        RobotType type;
        int health;
        int priority;
        MapLocation mloc;

        boolean isBetterThan(MoveTarget t){
            if (priority <= 1) return false;
            if (t == null) return true;
            if (t.priority <= 1) return true;
            if (priority > t.priority) return true;
            if (priority < t.priority) return true;
            return health <= t.health;
        }

        MoveTarget(RobotInfo r){
            this.type = r.getType();
            this.health = r.getHealth();
            this.mloc = r.getLocation();
            switch (r.getType()){
                case ARCHON:
                    priority = 0;
                    break;
                case SOLDIER:
                    priority = 5;
                    break;
                case MINER:
                    priority = 4;
                    break;
                case SAGE:
                    priority = 6;
                    break;
                case LABORATORY:
                    priority = 3;
                    break;
                case BUILDER:
                    priority = 1;
                    break;
                case WATCHTOWER:
                    priority = 2;
                    break;
            }
        }
    }

    class AimTarget{
        MapLocation loc;
        int dist;
        int rubble;
        boolean canMove = true;

        AimTarget(Direction dir, MapLocation target){
            if (dir != Direction.CENTER && !rc.canMove(dir)){
                canMove = false;
                return;
            }
            this.loc = rc.adjacentLocation(dir);
            dist = loc.distanceSquaredTo(target);
            try {
                rubble = rc.senseRubble(loc);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        boolean isBetterThan(AimTarget at){
            if (!canMove) return false;
            if (!at.canMove) return true;

            return dist < at.dist;
        }
    }

}

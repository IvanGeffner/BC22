package eleven;

import battlecode.common.*;

//TODO:

public class Attacker {

    RobotController rc;
    int[] minDists;
    static int IC = Direction.CENTER.ordinal();
    static int IN = Direction.NORTH.ordinal();
    static int IS = Direction.SOUTH.ordinal();
    static int IE = Direction.EAST.ordinal();
    static int IW = Direction.WEST.ordinal();
    static int INE = Direction.NORTHEAST.ordinal();
    static  int INW = Direction.NORTHWEST.ordinal();
    static int ISE = Direction.SOUTHEAST.ordinal();
    static int ISW = Direction.SOUTHWEST.ordinal();
    static int myRange;

    Attacker (RobotController rc){
        this.rc = rc;
        myRange = rc.getType().actionRadiusSquared;
    }

    MapLocation getAdjacentAttackerStep(){

        minDists = new int[9];
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
            switch (r.getType()) {
                case LABORATORY:
                case BUILDER:
                case MINER:
                case ARCHON:
                case WATCHTOWER:
                    MapLocation aLoc = r.getLocation();
                    int d = aLoc.distanceSquaredTo(newLocC);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[IC] < p) minDists[IC] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocN);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[IN] < p) minDists[IN] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocS);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[IS] < p) minDists[IS] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocE);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[IE] < p) minDists[IE] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocW);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[IW] < p) minDists[IW] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocNE);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[INE] < p) minDists[INE] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocSE);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[ISE] < p) minDists[ISE] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocNW);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[INW] < p) minDists[INW] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocSW);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[ISW] < p) minDists[ISW] = p;
                    }
                    d = aLoc.distanceSquaredTo(newLocC);
                    if (d <= myRange){
                        int p = getPriority(r.getType());
                        if (minDists[IC] < p) minDists[IC] = p;
                    }
                default:
                    break;
            }
        }
        return null;
    }

    static int getPriority(RobotType t){
        switch (t){
            case ARCHON: return 0;
            case SOLDIER: return 5;
            case MINER: return 4;
            case SAGE: return 6;
            case LABORATORY: return 3;
            case BUILDER: return 1;
            case WATCHTOWER: return 2;
        }
        return -1;
    }



}

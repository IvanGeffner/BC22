package wammawink;

import battlecode.common.*;

public class Builder extends Robot {

    BuildingTarget closestRepairBuilding;

    Builder(RobotController rc){
        super(rc);
        myID = rc.getID();
    }

    static int minDists[];
    static int IC = Direction.CENTER.ordinal();
    static int IN = Direction.NORTH.ordinal();
    static int IS = Direction.SOUTH.ordinal();
    static int IE = Direction.EAST.ordinal();
    static int IW = Direction.WEST.ordinal();
    static int INE = Direction.NORTHEAST.ordinal();
    static  int INW = Direction.NORTHWEST.ordinal();
    static int ISE = Direction.SOUTHEAST.ordinal();
    static int ISW = Direction.SOUTHWEST.ordinal();

    int roundBuilt = -10;
    int myID;

    void play(){
        computeMinDists();
        computeMinDistAttackers();
        computeClosestRepairBuilding();
        tryAct();
        tryMove();
        computeClosestRepairBuilding();
        tryAct();
    }

    void computeMinDists(){
        minDists = new int[9];
        minDists[0] = 100000;
        minDists[1] = 100000;
        minDists[2] = 100000;
        minDists[3] = 100000;
        minDists[4] = 100000;
        minDists[5] = 100000;
        minDists[6] = 100000;
        minDists[7] = 100000;
        minDists[8] = 100000;


        RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), explore.myVisionRange, rc.getTeam());
        MapLocation newLocC = rc.getLocation();
        MapLocation newLocN = rc.adjacentLocation(Direction.NORTH);
        MapLocation newLocS = rc.adjacentLocation(Direction.SOUTH);
        MapLocation newLocE = rc.adjacentLocation(Direction.EAST);
        MapLocation newLocW = rc.adjacentLocation(Direction.WEST);
        MapLocation newLocNE = rc.adjacentLocation(Direction.NORTHEAST);
        MapLocation newLocNW = rc.adjacentLocation(Direction.NORTHWEST);
        MapLocation newLocSE = rc.adjacentLocation(Direction.SOUTHEAST);
        MapLocation newLocSW = rc.adjacentLocation(Direction.SOUTHWEST);

        for (RobotInfo r : allies){
            MapLocation aLoc = r.getLocation();
            int d = aLoc.distanceSquaredTo(newLocC);
            if (d < minDists[IC]) minDists[IC] = d;
            d = aLoc.distanceSquaredTo(newLocN);
            if (d < minDists[IN]) minDists[IN] = d;
            d = aLoc.distanceSquaredTo(newLocS);
            if (d < minDists[IS]) minDists[IS] = d;
            d = aLoc.distanceSquaredTo(newLocE);
            if (d < minDists[IE]) minDists[IE] = d;
            d = aLoc.distanceSquaredTo(newLocW);
            if (d < minDists[IW]) minDists[IW] = d;
            d = aLoc.distanceSquaredTo(newLocNE);
            if (d < minDists[INE]) minDists[INE] = d;
            d = aLoc.distanceSquaredTo(newLocNW);
            if (d < minDists[INW]) minDists[INW] = d;
            d = aLoc.distanceSquaredTo(newLocSE);
            if (d < minDists[ISE]) minDists[ISE] = d;
            d = aLoc.distanceSquaredTo(newLocSW);
            if (d < minDists[ISW]) minDists[ISW] = d;
        }
    }

    void tryAct(){
        try {
            if (closestRepairBuilding != null && closestRepairBuilding.prototype) {
                if (rc.canRepair(closestRepairBuilding.loc)) rc.repair(closestRepairBuilding.loc);
            }
            tryBuildLab();
            if (closestRepairBuilding != null) {
                if (rc.canRepair(closestRepairBuilding.loc)) rc.repair(closestRepairBuilding.loc);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    void tryMove(){
        MapLocation target = getTarget();
        if (target != null) rc.setIndicatorLine(rc.getLocation(), target, 255, 0, 0);
        bfs.move(target);
    }

    MapLocation getBestMoveLocation(){
        Direction bestDir = Direction.CENTER;
        for (Direction dir : directions){
            if (!rc.canMove(dir)) continue;
            if (bestDir == null || minDists[dir.ordinal()] > minDists[bestDir.ordinal()]){
                bestDir = dir;
            }
        }
        if (bestDir != null){
            return rc.getLocation().add(bestDir);
        }
        return null;
    }


    MapLocation getClosestBuilding(){
        if (closestRepairBuilding != null){
            if (closestRepairBuilding.d <= 2) return rc.getLocation();
            return closestRepairBuilding.loc;
        }

        RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), explore.myVisionRange, rc.getTeam());

        BuildingTarget bestTarget = null;

        for (RobotInfo r : allies){
            if (!r.getType().isBuilding()) continue;
            if (r.getHealth() >= r.getType().getMaxHealth(r.getLevel())) continue;
            BuildingTarget bt = new BuildingTarget(r);
            if (bt.isBetterThan(bestTarget)) bestTarget = bt;
        }

        if (bestTarget != null){
            int d = bestTarget.loc.distanceSquaredTo(rc.getLocation());
            if (d <= 2) return rc.getLocation();
            return bestTarget.loc;
        }
        return null;
    }

    MapLocation getTarget(){
        MapLocation ans = getClosestBuilding();
        if (ans == null){
            if (rc.getRoundNum() - roundBuilt < 10){
                ans = getBestMoveLocation();
                if (ans != null) return ans;
            }
            ans = getBestLocationBuilder();
            if (ans != null) ans = adapt(ans);
        }
        return ans;
    }

    MapLocation adapt(MapLocation leadLoc){
        try {
            if (leadLoc == null) return null;
            if (!rc.canSenseLocation(leadLoc)) return leadLoc;
            MapLocation ans = null;
            int bestRubble = 0;
            for (int i = 9; i-- > 0; ) {
                Direction dir = directions[i];
                MapLocation newLoc = leadLoc.add(dir);
                if (rc.canSenseLocation(newLoc) && rc.onTheMap(newLoc)) {
                    RobotInfo ri = rc.senseRobotAtLocation(newLoc);
                    if (ri != null && ri.getID() != myID) continue;
                    int r = rc.senseRubble(newLoc);
                    if (ans == null || r < bestRubble){
                        bestRubble = r;
                        ans = newLoc;
                    }
                }
            }
            if (ans != null) return ans;
        } catch (Exception e){
            e.printStackTrace();
        }
        return leadLoc;
    }

    void tryBuildLab(){
        if (rc.getTeamLeadAmount(rc.getTeam()) < RobotType.LABORATORY.getLeadWorth(0)) return;
        LabLocation bestLoc = null;
        for (Direction dir : directions){
            LabLocation l = new LabLocation(dir);
            if (l.isBetterThan(bestLoc)) bestLoc = l;
        }
        if (bestLoc != null){
            if (rc.canBuildRobot(RobotType.LABORATORY, bestLoc.dir)) {
                try {
                    rc.buildRobot(RobotType.LABORATORY, bestLoc.dir);
                    roundBuilt = rc.getRoundNum();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    class LabLocation {
        Direction dir;
        int minDist;
        int rubble = 0;
        MapLocation loc;
        boolean canConstruct;

        LabLocation(Direction dir){
            this.dir = dir;
            minDist = minDists[dir.ordinal()];
            loc = rc.getLocation().add(dir);
            canConstruct = rc.canBuildRobot(RobotType.LABORATORY, dir);
            try {
                if (canConstruct) rubble = rc.senseRubble(loc);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        boolean isBetterThan(LabLocation l){
            if (l == null) return true;

            if (canConstruct && !l.canConstruct) return true;
            if (!canConstruct && l.canConstruct) return false;

            if (rubble < l.rubble) return true;
            if (l.rubble < rubble) return false;

            return minDist < l.minDist;
        }
    }

    void computeClosestRepairBuilding(){
        closestRepairBuilding = null;
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().visionRadiusSquared, rc.getTeam());

        BuildingTarget bestTarget = null;

        for (RobotInfo r : allies){
            if (!r.getType().isBuilding()) continue;
            if (r.getHealth() >= r.getType().getMaxHealth(r.getLevel())) continue;
            BuildingTarget bt = new BuildingTarget(r);
            if (bt.isBetterThan(bestTarget)) bestTarget = bt;
        }
        try {
             closestRepairBuilding = bestTarget;
            //if (bestTarget != null && rc.canRepair(bestTarget.loc)) rc.repair(bestTarget.loc);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    class BuildingTarget {
        boolean prototype;
        int health;
        int maxHealth;
        MapLocation loc;
        int d;

        BuildingTarget(RobotInfo r){
            prototype = r.getMode() == RobotMode.PROTOTYPE;
            health = r.getHealth();
            maxHealth = r.getType().getMaxHealth(r.getLevel());
            loc = r.getLocation();
            d = loc.distanceSquaredTo(rc.getLocation());
        }

        boolean isBetterThan(BuildingTarget l){
            if (health >= maxHealth) return true;
            if (l == null) return true;

            if (prototype && !l.prototype) return true;
            if (!prototype && l.prototype) return false;

            return d < l.d;
        }
    }

    MapLocation getBestLocationBuilder(){
        try {
            MapLocation[] visibleLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), explore.myVisionRange);
            MapLocation myLoc = rc.getLocation();
            MapLocation bestLoc = null;
            int bestRubble = 0;
            int bestDist = 0;
            for (MapLocation loc : visibleLocs){
                int r = rc.senseRubble(loc);
                int d = myLoc.distanceSquaredTo(loc);
                if (bestLoc == null || r < bestRubble || (r == bestRubble && d < bestDist)){
                    RobotInfo rob = rc.senseRobotAtLocation(loc);
                    if (rob != null && rob.getID() != myID) continue;
                    bestRubble = r;
                    bestLoc = loc;
                    bestDist = d;
                }
            }
            if (bestLoc != null) rc.setIndicatorDot(bestLoc, 0, 255, 0);
            return bestLoc;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}

package eighteen;

import battlecode.common.*;

public class Archon extends Robot {

    int builderScore;
    int minerScore;
    int soldierScore;
    int lastBuildTurn = 0;
    int lastMinerBuilt = 0;

    static final int MIN_WAIT_MINERS = 15;

    MapLocation closestEnemy = null;

    MapLocation bestRepairRobot = null;

    Archon(RobotController rc) {
        super(rc);
    }

    int remainingLead = 0;

    void play(){
        computeClosestEnemy();
        checkReserves();
        buildUnit();
        tryRepair();
        //tryMove();
    }

    void checkReserves(){
        if (rc.getMode() == RobotMode.TURRET) return;
        Reservation r = comm.getReservedRobot();
        if (r.t == null) return;
        if (r.first) comm.cancelFirstReservation();

    }

    void tryMove(){
        try {
            if (rc.getRoundNum() <= Constants.ARCHON_FIX_INITIAL_TURNS) return;
            if (rc.getRoundNum() - lastBuildTurn <= Constants.MIN_TURNS_NO_BUILD) return;
            MapLocation targetLoc = getBestLocArchon();
            if (targetLoc == null) return;
            if (rc.getLocation().distanceSquaredTo(targetLoc) <= 0) {
                if (rc.getMode() != RobotMode.TURRET && rc.canTransform()) rc.transform();
                return;
            }
            if (rc.getMode() != RobotMode.PORTABLE && rc.canTransform()) rc.transform();
            if (rc.getMode() == RobotMode.PORTABLE) bfs.move(targetLoc);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void buildUnit(){
        if (rc.getMode() != RobotMode.TURRET) return;
        remainingLead = 0;

        //try build sage
        if (tryConstructSage()) return;

        if (endangered()){
            comm.resetReservations();
            comm.reserveRobot(RobotType.SOLDIER);
        }

        if (tryBuildReservation()) return;

        builderScore = comm.getBuildingScore(RobotType.BUILDER);
        soldierScore = comm.getBuildingScore(RobotType.SOLDIER);
        minerScore = comm.getBuildingScore(RobotType.MINER) - Util.getMinMiners();

        if (tryBuildBuilder()) return;
        if (tryBuildMiner()) return;
        if (tryBuildSoldier()) return;
    }

    boolean tryBuildSoldier(){
        if (remainingLead < RobotType.SOLDIER.getLeadWorth(0)){
            comm.reportBuilt(RobotType.SOLDIER, updateSoldierScore(soldierScore));
            comm.reserveRobot(RobotType.SOLDIER);
            return true;
        }
        if (tryConstructEnvelope(RobotType.SOLDIER, comm.getClosestEnemyArchon())){
            comm.reportBuilt(RobotType.SOLDIER, updateSoldierScore(soldierScore));
            return true;
        }
        return false;
    }

    boolean tryBuildMiner(){
        if (rc.getTeamLeadAmount(rc.getTeam()) >= Constants.MIN_LEAD_STOP_MINERS) {
            if (minerScore <= soldierScore) {
                //we don't build it if too much lead
                comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) + Util.getMinMiners());
            }
            return false;
        }
        if (explore.cumulativeLead < 50 && minerScore > soldierScore) return false;
        if (remainingLead < RobotType.MINER.getLeadWorth(0)) {
            comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) + Util.getMinMiners());
            comm.reserveRobot(RobotType.MINER);
            return true;
        } else if (tryConstructEnvelope(RobotType.MINER, explore.closestLead)) {
            comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) + Util.getMinMiners());
            return true;
        }
        return false;
    }

    boolean tryBuildBuilder(){
        if (!shouldBuildBuilder()) return false;
        if (builderScore >= minerScore) return false;
        if (builderScore >= soldierScore) return false;
        if (tryConstructEnvelope(RobotType.BUILDER, null)) {
            comm.reportBuilt(RobotType.BUILDER, updateBuilderScore(builderScore));
            return true;
        }
        return false;
    }

    boolean tryConstructSage(){
        if (tryConstructEnvelope(RobotType.SAGE, comm.getClosestEnemyArchon())) return true;
        return false;
    }

    boolean tryBuildReservation(){
        //try build whatever we reserved
        Reservation r = comm.getReservedRobot();
        remainingLead = rc.getTeamLeadAmount(rc.getTeam()) - r.savedLead;

        if (r.t != null){
            rc.setIndicatorString("Reserved " + r.t.name() + " with lead " + r.savedLead + ".");

            if (!r.first) return true;
            if (remainingLead < r.t.getLeadWorth(0)) return true;

            MapLocation target = null;
            if (r.t == RobotType.MINER) target = explore.closestLead;
            else if (r.t == RobotType.SOLDIER) target = getBestLocSolider();
            if (tryConstructEnvelope(r.t, target)) comm.cancelFirstReservation();
            return true;
        }

        return false;
    }

    int updateMinerScore(int oldScore){
        if (oldScore <= 0) return oldScore + 1;
        return oldScore +3;
    }

    int updateSoldierScore(int oldScore){
        return oldScore + 1;
    }

    int updateBuilderScore(int oldScore) {
        return oldScore + 100;
    }

    boolean shouldBuildBuilder(){
        return rc.getTeamLeadAmount(rc.getTeam()) > Constants.MIN_LEAD_LABORATORY;
    }

    void tryRepair(){
        if (!rc.isActionReady()) return;
        bestRepairRobot = null;
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().actionRadiusSquared, rc.getTeam());

        BuildingTarget bestTarget = null;

        for (RobotInfo r : allies){
            if (r.getType().isBuilding()) continue;
            if (r.getHealth() >= r.getType().getMaxHealth(r.getLevel())) continue;
            BuildingTarget bt = new BuildingTarget(r);
            if (bt.isBetterThan(bestTarget)) bestTarget = bt;
        }
        try {
            if (bestTarget != null){
                if (rc.canRepair(bestTarget.loc)) rc.repair(bestTarget.loc);
                rc.setIndicatorString("Repairing!");
            }
            //if (bestTarget != null && rc.canRepair(bestTarget.loc)) rc.repair(bestTarget.loc);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    class BuildingTarget {
        boolean attacker;
        int health;
        int maxHealth;
        MapLocation loc;
        int d;

        BuildingTarget(RobotInfo r){
            attacker = Util.isAttacker(r.getType());
            health = r.getHealth();
            maxHealth = r.getType().getMaxHealth(r.getLevel());
            loc = r.getLocation();
            d = loc.distanceSquaredTo(rc.getLocation());
        }

        boolean isBetterThan(BuildingTarget l){
            if (health >= maxHealth) return true;
            if (l == null) return true;

            if (attacker && !l.attacker) return true;
            if (!attacker && l.attacker) return false;

            return health < l.health;
        }
    }

    void computeClosestEnemy(){
        MapLocation myLoc = rc.getLocation();
        closestEnemy = null;
        int closestDist = 0;
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies){
            if (!Util.isAttacker(enemy.getType())) continue;
            int d = enemy.getLocation().distanceSquaredTo(myLoc);
            if (closestEnemy == null || d < closestDist){
                closestEnemy = enemy.location;
                closestDist = d;
            }
        }
    }

    boolean endangered(){
        return closestEnemy != null;
    }

    MapLocation getBestLocSolider(){
        return comm.getClosestEnemyArchon();
    }

    boolean tryConstructEnvelope(RobotType t, MapLocation target){
        if (constructRobotGreedy(t, target)){
            lastBuildTurn = rc.getRoundNum();
            if (t == RobotType.MINER) lastMinerBuilt = rc.getRoundNum();
            return true;
        }
        return false;
    }

    MapLocation getBestLocArchon(){
        MapLocation center = comm.getCentralArchon();
        if (center == null) return null;
        rc.setIndicatorLine(rc.getLocation(), center, 255, 0, 0);
        if (rc.getLocation().distanceSquaredTo(center) <= 0) return center;
        try {
            MapLocation[] mLocs = rc.getAllLocationsWithinRadiusSquared(center, 8);
            if (!rc.canSenseLocation(center)) return center;
            //if (mLocs.length == 0) return center;
            MapLocation ans = null;
            int bestRubble = 0;
            int bestDist = 0;
            for (MapLocation loc : mLocs) {
                if (!rc.canSenseLocation(loc)) continue;
                if (rc.isLocationOccupied(loc) && rc.getLocation().distanceSquaredTo(loc) > 0) continue;
                int r = rc.senseRubble(loc);
                int d = loc.distanceSquaredTo(center);
                if (ans == null || isBetter(r,d,bestRubble,bestDist)){
                    ans = loc;
                    bestRubble = r;
                    bestDist = d;
                }
            }
            return ans;
        } catch (Exception e){
            e.printStackTrace();
        }
        return center;
    }

    boolean isBetter(int rubble, int distance, int oldRubble, int oldDistance){
        if (rubble < oldRubble) return true;
        if (oldRubble < rubble) return false;
        //TODO check this
        //if (distance <= 2 && oldDistance > 2) return false;
        //if (oldDistance <= 2 && distance > 2) return true;
        return distance < oldDistance;
    }

}

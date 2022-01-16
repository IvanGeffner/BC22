package fifteeen;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Archon extends Robot {

    int builderScore;
    int minerScore;
    int soldierScore;

    MapLocation closestEnemy = null;

    MapLocation bestRepairRobot = null;

    Archon(RobotController rc) {
        super(rc);
    }

    int remainingLead = 0;

    void play(){
        computeClosestEnemy();
        buildUnit();
        tryRepair();
    }

    void buildUnit(){
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
        if (constructRobotGreedy(RobotType.SOLDIER, comm.getClosestEnemyArchon())){
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
        } else if (constructRobotGreedy(RobotType.MINER, explore.closestLead)) {
            comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) + Util.getMinMiners());
            return true;
        }
        return false;
    }

    boolean tryBuildBuilder(){
        if (!shouldBuildBuilder()) return false;
        if (builderScore >= minerScore) return false;
        if (builderScore >= soldierScore) return false;
        if (constructRobotGreedy(RobotType.BUILDER)) {
            comm.reportBuilt(RobotType.BUILDER, updateBuilderScore(builderScore));
            return true;
        }
        return false;
    }

    boolean tryConstructSage(){
        if (constructRobotGreedy(RobotType.SAGE, comm.getClosestEnemyArchon())) return true;
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
            if (constructRobotGreedy(r.t, target)) comm.cancelFirstReservation();
            return true;
        }

        return false;
    }

    int updateMinerScore(int oldScore){
        /*
        if (oldScore < 8) return oldScore + 1;
        else if (oldScore < 30) return oldScore + 2;
        return oldScore + 5;*/
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

}

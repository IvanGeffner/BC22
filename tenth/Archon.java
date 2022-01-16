package tenth;

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

    void play(){
        buildUnit();
        tryRepair();
    }

    void buildUnit(){
        //try build sage
        if (constructRobotGreedy(RobotType.SAGE)) return;

        computeClosestEnemy();
        if (closestEnemy != null){
            if (rc.getRoundNum() > 5) comm.activateDanger();
            comm.resetReservations();
            comm.reserveRobot(RobotType.SOLDIER);
        }

        builderScore = comm.getBuildingScore(RobotType.BUILDER);
        soldierScore = comm.getBuildingScore(RobotType.SOLDIER);
        minerScore = comm.getBuildingScore(RobotType.MINER) - Util.getMinMiners();

        //try build whatever we reserved
        Reservation r = comm.getReservedRobot();
        if (minerScore >= 0 && closestEnemy != null && r.t != null && r.t != RobotType.SOLDIER) r.t = RobotType.SOLDIER; // no miners when attacked
        int myLead = rc.getTeamLeadAmount(rc.getTeam()) - r.savedLead;

        if (r.t != null){
            rc.setIndicatorString("Reserved " + r.t.name() + " with lead " + r.savedLead + ".");

            if (!r.first) return;
            if (myLead < r.t.getLeadWorth(0)) return;

            MapLocation target = null;
            if (r.t == RobotType.MINER) target = explore.closestLead;
            else if (r.t == RobotType.SOLDIER) target = comm.getClosestEnemyArchon();
            if (constructRobotGreedy(r.t, target)) comm.cancelFirstReservation();
            return;
        }

        //case in which no reservation has been made

        //no reservations for builders
        if (closestEnemy == null || minerScore < 0) {
            if (shouldBuildBuilder() && builderScore < minerScore && builderScore < soldierScore) {
                if (constructRobotGreedy(RobotType.BUILDER)) {
                    comm.reportBuilt(RobotType.BUILDER, updateBuilderScore(builderScore));
                    return;
                }
            }

            if (rc.getTeamLeadAmount(rc.getTeam()) >= Constants.MIN_LEAD_STOP_MINERS) {
                if (minerScore <= soldierScore) {
                    comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) + Util.getMinMiners());
                }
            } else {
                if ((explore.cumulativeLead > 50 && noMiners()) || minerScore <= soldierScore) {
                    if (myLead < RobotType.MINER.getLeadWorth(0)) {
                        //rc.setIndicatorString("Trying to reserve a miner.");
                        comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) + Util.getMinMiners());
                        comm.reserveRobot(RobotType.MINER);
                        return;
                    } else if (constructRobotGreedy(RobotType.MINER, explore.closestLead)) {
                        comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) + Util.getMinMiners());
                        return;
                    }
                }
            }
        }

        if (myLead < RobotType.SOLDIER.getLeadWorth(0)){
            comm.reportBuilt(RobotType.SOLDIER, updateSoldierScore(soldierScore));
            comm.reserveRobot(RobotType.SOLDIER);
            return;
        }
        if (constructRobotGreedy(RobotType.SOLDIER, comm.getClosestEnemyArchon())){
            comm.reportBuilt(RobotType.SOLDIER, updateSoldierScore(soldierScore));
        }
    }

    int updateMinerScore(int oldScore){
        /*
        if (oldScore < 8) return oldScore + 1;
        else if (oldScore < 30) return oldScore + 2;
        return oldScore + 5;*/
        if (oldScore <= 0) return oldScore + 1;
        return oldScore + 2;
    }

    int updateSoldierScore(int oldScore){
        return oldScore + 2;
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

    boolean noMiners(){
        RobotInfo[] units = rc.senseNearbyRobots(rc.getLocation(), explore.myVisionRange, rc.getTeam());
        for (RobotInfo r : units){
            if (r.getType() == RobotType.MINER) return false;
        }
        return true;
    }

}

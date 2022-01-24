package twentyseven;

import battlecode.common.*;

public class Archon extends Attacker {

    int builderScore;
    int minerScore;
    int soldierScore;
    int lastBuildTurn = 0;
    int lastMinerBuilt = 0;

    static final int MIN_WAIT_MINERS = 15;
    static int minSoldierScore = 100; //TODO?

    MapLocation closestEnemy = null;

    MapLocation bestRepairRobot = null;

    Archon(RobotController rc) {
        super(rc);
        if (minSoldierScore > Util.getMinMiners()) minSoldierScore = Util.getMinMiners();
    }

    int remainingLead = 0;

    void play(){
        computeClosestEnemy();
        computeMinDistAttackers();
        checkReserves();
        buildUnit();
        tryRepair();
        tryMove();
        comm.setArchonState(rc.getMode() == RobotMode.TURRET);
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
            if (rc.getMode() == RobotMode.TURRET && comm.allArchonsMoving()) return;
            if (hasWorkToDo()){
                rc.setIndicatorString("Has work to do!");
                if (rc.getMode() == RobotMode.PORTABLE){
                    MapLocation targetLoc = getBestLocArchon(2);
                    if (targetLoc.equals(rc.getLocation())) {
                        if (rc.canTransform()) rc.transform();
                    }
                    else bfs.move(targetLoc);
                }
                return;
            }
            rc.setIndicatorString("Trying to move!");
            MapLocation targetLoc = getAttackingArchonTarget();
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
        soldierScore = Math.max(comm.getBuildingScore(RobotType.SOLDIER), minSoldierScore);
        minerScore = comm.getBuildingScore(RobotType.MINER);

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
                comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore));
            }
            return false;
        }
        if ((explore.cumulativeLead < 50 || rc.getRoundNum() - lastBuildTurn <= MIN_WAIT_MINERS) && minerScore > soldierScore) return false;
        if (remainingLead < RobotType.MINER.getLeadWorth(0)) {
            comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore));
            comm.reserveRobot(RobotType.MINER);
            return true;
        } else if (tryConstructEnvelope(RobotType.MINER, explore.closestLead)) {
            comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore));
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
            //rc.setIndicatorString("Reserved " + r.t.name() + " with lead " + r.savedLead + ".");

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
        if (oldScore < Util.getMinMiners()) return oldScore + 1;
        return oldScore + 3;
    }

    int updateSoldierScore(int oldScore){
        if (oldScore < Util.getMinMiners()){
            if (oldScore + 3 > Util.getMinMiners()) return Util.getMinMiners();
            return oldScore + 3;
        }
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
                //rc.setIndicatorString("Repairing!");
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
        boolean criticalHealth;

        BuildingTarget(RobotInfo r){
            attacker = Util.isAttacker(r.getType());
            health = r.getHealth();
            maxHealth = r.getType().getMaxHealth(r.getLevel());
            loc = r.getLocation();
            d = loc.distanceSquaredTo(rc.getLocation());
            criticalHealth = health < Constants.CRITICAL_HEALTH;
        }

        boolean isBetterThan(BuildingTarget l){
            if (health >= maxHealth) return true;
            if (l == null) return true;

            if (attacker && !l.attacker) return true;
            if (!attacker && l.attacker) return false;

            if (criticalHealth && !l.criticalHealth) return true;
            if (!criticalHealth && l.criticalHealth) return false;

            if (criticalHealth){
                return health < l.health;
            }

            return health > l.health;
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
        if (closestEnemy == null) return false;
        int totalHP = 0;
        int maxHP = RobotType.SOLDIER.getMaxHealth(0);
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo r : robots){
            if (!Util.isAttacker(r.getType())) continue;
            int hp = r.getHealth();
            if (hp > maxHP) hp = maxHP;
            if (r.getTeam() == rc.getTeam()){
                totalHP += hp;
            } else totalHP -= hp;
        }
        return totalHP < 0;
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

    MapLocation getBestLocArchon(int r){
        try {
            MapLocation myLoc = rc.getLocation();
            MapLocation ans = rc.getLocation();
            int rubble = rc.senseRubble(myLoc);
            int bestDist = 0;
            MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), r);
            for (MapLocation loc : locs){
                int rub= rc.senseRubble(loc);
                int d = loc.distanceSquaredTo(myLoc);
                if (d > 0 && d <= 2 && rc.isLocationOccupied(loc)) continue;
                if (rub < rubble || (rub == rubble && d < bestDist)){
                    ans = loc;
                    rubble = rub;
                    bestDist = d;
                }
            }
            return ans;
        } catch (Exception e){
            e.printStackTrace();
        }
        return rc.getLocation();
    }

    boolean isBetter(int rubble, int distance, int oldRubble, int oldDistance){
        if (rubble < oldRubble) return true;
        if (oldRubble < rubble) return false;
        return distance < oldDistance;
    }

    boolean hasWorkToDo(){
        try {
            Team team = rc.getTeam();
            boolean turret = rc.getMode() == RobotMode.TURRET;
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots){
                if (robot.getTeam() == team){
                    if (Util.isAttacker(robot.getType()) && robot.getHealth() < 30){
                        rc.setIndicatorString("Gotta transform and repair");
                        return true;
                    }
                    else if (turret && Util.isAttacker(robot.getType()) && robot.getHealth() < robot.getType().getMaxHealth(0)){
                        rc.setIndicatorString("Gotta repair");
                        return true;
                    }
                } else{
                    if (Util.isAttacker(robot.getType()) || robot.getType() == RobotType.ARCHON || robot.getType() == RobotType.WATCHTOWER){
                        rc.setIndicatorString("Gotta attack!");
                        return true;
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    MapLocation getAttackingArchonTarget(){
        MapLocation ans = getBestAttackingTarget();
        if (ans != null) return ans;
        ans = comm.getClosestEnemyArchon();
        if (ans != null) return ans;
        return explore.getExploreTarget(true);
    }

}

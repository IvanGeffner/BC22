package fifthbot;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Archon extends Robot {

    static final int BUILDER_LEAD = 1000;
    static final int MINER_SCORE_OFFSET = -10;

    Archon(RobotController rc) {
        super(rc);
    }

    boolean mainArchon = false;

    void play(){
        if (explore.visibleLead) mainArchon = true;
        if (mainArchon){
            buildUnit();
        }
    }

    void buildUnit(){
        //try build sage
        if (constructRobotGreedy(RobotType.SAGE)) return;

        //try build whatever we reserved
        Reservation r = comm.getReservedRobot();
        int myLead = rc.getTeamLeadAmount(rc.getTeam()) - r.savedLead;

        if (r.t != null){
            if (myLead < r.t.getLeadWorth(0)) return;

            MapLocation target = null;
            if (r.t == RobotType.MINER) target = explore.closestLead;
            else if (r.t == RobotType.SOLDIER) target = comm.getClosestEnemyArchon();
            if (constructRobotGreedy(r.t, target)) comm.cancelFirstReservation();
            return;
        }


        //case in which no reservation has been made
        int builderScore = comm.getBuildingScore(RobotType.BUILDER);
        int soldierScore = comm.getBuildingScore(RobotType.SOLDIER);
        int minerScore = comm.getBuildingScore(RobotType.MINER) + MINER_SCORE_OFFSET;


        //no reservations for builders
        if (shouldBuildBuilder() && builderScore < minerScore && builderScore < soldierScore){
            if (constructRobotGreedy(RobotType.BUILDER)){
                comm.reportBuilt(RobotType.BUILDER, updateBuilderScore(soldierScore));
                return;
            }
        }


        if (minerScore <= soldierScore) {
            if (myLead < RobotType.MINER.getLeadWorth(0)){
                comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) - MINER_SCORE_OFFSET);
                comm.reserveRobot(RobotType.MINER);
                return;
            }
            else if (constructRobotGreedy(RobotType.MINER, explore.closestLead)){
                comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) - MINER_SCORE_OFFSET);
                return;
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
        if (oldScore < 8) return oldScore + 1;
        else if (oldScore < 30) return oldScore + 2;
        return oldScore + 5;
    }

    int updateSoldierScore(int oldScore){
        return oldScore + 2;
    }

    int updateBuilderScore(int oldScore) {
        return oldScore + 100;
    }

    boolean shouldBuildBuilder(){
        return rc.getTeamLeadAmount(rc.getTeam()) > BUILDER_LEAD;
    }

}

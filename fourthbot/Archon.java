package fourthbot;

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
        if (constructRobotGreedy(RobotType.SAGE)) return;
        int builderScore = comm.getBuildingScore(RobotType.BUILDER);
        int soldierScore = comm.getBuildingScore(RobotType.SOLDIER);
        int minerScore = comm.getBuildingScore(RobotType.MINER) + MINER_SCORE_OFFSET;
        if (shouldBuildBuilder() && builderScore < minerScore && builderScore < soldierScore){
            if (constructRobotGreedy(RobotType.BUILDER)){
                comm.reportBuilt(RobotType.BUILDER, updateBuilderScore(soldierScore));
                return;
            }
        }
        if (minerScore <= soldierScore) {
            if (constructRobotGreedy(RobotType.MINER, explore.closestLead)){
                comm.reportBuilt(RobotType.MINER, updateMinerScore(minerScore) - MINER_SCORE_OFFSET);
                return;
            }
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

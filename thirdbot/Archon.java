package thirdbot;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Archon extends Robot {

    static final int BUILDER_LEAD = 1000;

    Archon(RobotController rc) {
        super(rc);
    }

    boolean mainArchon = false;

    int minerScore = -2;
    int soldierScore = 0;
    int builderScore = 0;

    void play(){
        if (explore.visibleLead) mainArchon = true;
        if (mainArchon){
            while (true) {
                if (constructRobotGreedy(RobotType.SAGE)) continue;
                if (shouldBuildBuilder() && builderScore < minerScore && builderScore < soldierScore){
                    if (constructRobotGreedy(RobotType.BUILDER)){
                        updateMinerScore();
                        continue;
                    }
                }
                if (minerScore <= soldierScore) {
                    if (constructRobotGreedy(RobotType.MINER)){
                        updateMinerScore();
                        continue;
                    }
                } else if (constructRobotGreedy(RobotType.SOLDIER)){
                    updateSoldierScore();
                    continue;
                }
                break;
            }
        }
    }

    void updateMinerScore(){
        if (minerScore < 8) minerScore += 1;
        else if (minerScore < 30) minerScore += 2;
        else minerScore += 5;
    }

    void updateSoldierScore(){
        soldierScore += 2;
    }

    void updateBuilderScore() {
        builderScore += 100;
    }

    boolean shouldBuildBuilder(){
        return rc.getTeamLeadAmount(rc.getTeam()) > BUILDER_LEAD;
    }

}

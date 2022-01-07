package thirdbot;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Archon extends Robot {

    Archon(RobotController rc) {
        super(rc);
    }

    boolean mainArchon = false;

    int minerScore = 0;
    int soldierScore = 0;

    void play(){
        if (explore.visibleLead) mainArchon = true;
        if (mainArchon){
            while (true) {
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
        if (rc.getRoundNum() < 100) minerScore += 1;
        else if (rc.getRoundNum() < 300) minerScore += 2;
        else minerScore += 5;
    }

    void updateSoldierScore(){
        soldierScore += 2;
    }

}

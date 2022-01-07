package thirdbot;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Archon extends Robot {

    Archon(RobotController rc) {
        super(rc);
    }

    boolean mainArchon = false;

    int minerScore = -2;
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
        if (minerScore < 8) minerScore += 1;
        else if (minerScore < 30) minerScore += 2;
        else minerScore += 5;
    }

    void updateSoldierScore(){
        soldierScore += 2;
    }

}

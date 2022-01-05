package firstbot;

import battlecode.common.*;

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
            if (minerScore <= soldierScore) {
                if (constructRobotGreedy(RobotType.MINER)) updateMinerScore();
            }
            else if (constructRobotGreedy(RobotType.SOLDIER)) updateSoldierScore();
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

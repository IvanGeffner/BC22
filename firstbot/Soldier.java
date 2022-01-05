package firstbot;

import battlecode.common.RobotController;

public class Soldier extends Robot {

    Soldier(RobotController rc){
        super(rc);
    }

    void play(){
        moveRandom();
    }

}

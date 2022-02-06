package wammawink;

import battlecode.common.RobotController;
import battlecode.common.RobotMode;
import battlecode.common.RobotType;

public class Lab extends Robot {

    Lab(RobotController rc){
        super(rc);
    }

    void play(){
        try{
            if (rc.getMode() == RobotMode.PORTABLE && rc.canTransform()) rc.transform();
            if (rc.getMode() == RobotMode.TURRET) {
                if (rc.getTeamLeadAmount(rc.getTeam()) >= RobotType.MINER.getLeadWorth(0) + rc.getTransmutationRate()){
                    if (rc.canTransmute()) rc.transmute();
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
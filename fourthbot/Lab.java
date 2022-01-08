package fourthbot;

import battlecode.common.*;

public class Lab extends Robot {

    Lab(RobotController rc){
        super(rc);
    }

    void play(){
        try{
            if (rc.getMode() == RobotMode.PORTABLE && rc.canTransform()) rc.transform();
            if (rc.getMode() == RobotMode.TURRET) {
                if (rc.getTeamLeadAmount(rc.getTeam()) > 200) {
                    rc.setIndicatorString("Trying to transmute!");
                    if (rc.canTransmute()) rc.transmute();
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
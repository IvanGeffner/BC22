package twentynine;

import battlecode.common.RobotController;
import battlecode.common.RobotMode;

public class Lab extends Robot {

    Lab(RobotController rc){
        super(rc);
    }

    void play(){
        try{
            if (rc.getMode() == RobotMode.PORTABLE && rc.canTransform()) rc.transform();
            if (rc.getMode() == RobotMode.TURRET) {
                /*if (comm.getSoldiersAlive() >= Util.getMinSoldiers()){
                    if (rc.canTransmute()) rc.transmute();
                }*/
                Reservation r = comm.getReservedRobot();
                if (rc.getTeamLeadAmount(rc.getTeam()) >= r.savedLead + rc.getTransmutationRate()){
                    if (rc.canTransmute()) rc.transmute();
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
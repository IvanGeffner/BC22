package firstbot;

import battlecode.common.*;

public class Explore {

    RobotController rc;
    boolean visibleLead;
    int myVisionRange;
    boolean compareLead = false;
    MapLocation closestLead = null;
    int distLead = 0;


    static final int BYTECODE_EXPLORE_LEAD = 1500;

    Explore(RobotController rc){
        this.rc = rc;
        myVisionRange = rc.getType().visionRadiusSquared;
        if (rc.getType() == RobotType.MINER) compareLead = true;
    }

    void reportLead(){
        try {
            visibleLead = false;
            closestLead = null;
            distLead = 0;
            MapLocation[] mapLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), myVisionRange);
            int i = mapLocs.length;
            MapLocation myLoc = rc.getLocation();
            while (i-- > 0){
                if (Clock.getBytecodeNum() > BYTECODE_EXPLORE_LEAD) break;
                MapLocation m = mapLocs[i];
                if (!rc.onTheMap(m)) continue;
                int lead = rc.senseLead(m);
                if (lead > 0){
                    visibleLead = true;
                    Robot.comm.reportLead(m, lead);
                    if (compareLead && lead > Constants.MIN_LEAD_RELEVANT){
                        int d = m.distanceSquaredTo(myLoc);
                        if (closestLead == null || d < distLead){
                            closestLead = m;
                            distLead = d;
                        }
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    MapLocation getClosestLead(){
        return closestLead;
    }

    MapLocation getExploreTarget(){
        return null; //TODO
    }

    void reportUnits(){

    }

}

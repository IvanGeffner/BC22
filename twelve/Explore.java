package twelve;

import battlecode.common.*;

public class Explore {

    RobotController rc;
    boolean visibleLead;
    int myVisionRange;
    boolean compareLead = false;
    int cumulativeLead = 0;
    MapLocation closestLead = null;
    int distLead = 0;
    MapLocation exploreLoc = null;
    MapLocation closestEnemyArchon = null;
    int distEnemyArchon = 0;


    static int BYTECODE_EXPLORE_LEAD;

    Explore(RobotController rc){
        this.rc = rc;
        myVisionRange = rc.getType().visionRadiusSquared;
        if (rc.getType() == RobotType.MINER || rc.getType() == RobotType.ARCHON) compareLead = true;
        switch (rc.getType()){
            case MINER:
                BYTECODE_EXPLORE_LEAD = 2000;
                break;
            case ARCHON:
            case LABORATORY:
            case WATCHTOWER:
                BYTECODE_EXPLORE_LEAD = 5000;
                break;
            default:
                BYTECODE_EXPLORE_LEAD = 3000;
                break;
        }
    }

    void reportLead(){
        try {
            visibleLead = false;
            closestLead = null;
            distLead = 0;
            cumulativeLead = 0;
            MapLocation[] mapLocs = rc.senseNearbyLocationsWithLead(myVisionRange);
            if(mapLocs.length > 0) visibleLead = true;
            int i = mapLocs.length;
            MapLocation myLoc = rc.getLocation();
            while (i-- > 0){
                if (Clock.getBytecodeNum() > BYTECODE_EXPLORE_LEAD) break;
                int lead = rc.senseLead(mapLocs[i]);
                    //Robot.comm.reportLead(m, lead); TODO comms?
                if (compareLead && lead > Constants.MIN_LEAD_RELEVANT){
                    cumulativeLead += (lead - Constants.MIN_LEAD_RELEVANT);
                    int d = mapLocs[i].distanceSquaredTo(myLoc);
                    if (closestLead == null || d < distLead){
                        closestLead = mapLocs[i];
                        distLead = d;
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

    void getEmergencyTarget(int tries) {
        MapLocation myLoc = rc.getLocation();
        int maxX = rc.getMapWidth();
        int maxY = rc.getMapHeight();
        while (tries-- > 0){
            if (exploreLoc != null) return;
            MapLocation newLoc = new MapLocation((int)(Math.random()*maxX), (int)(Math.random()*maxY));
            if (myLoc.distanceSquaredTo(newLoc) > myVisionRange){
                exploreLoc = newLoc;
            }
        }
    }

    MapLocation getExploreTarget() {
        if (exploreLoc != null && rc.getLocation().distanceSquaredTo(exploreLoc) <= myVisionRange) exploreLoc = null;
        if (exploreLoc == null) getEmergencyTarget(15);
        return exploreLoc;
    }

    void reportUnits(){
        closestEnemyArchon = null;
        RobotInfo[] enemies = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies){
            if (enemy.getType() != RobotType.ARCHON) continue;
            int d = enemy.getLocation().distanceSquaredTo(rc.getLocation());
            if (closestEnemyArchon == null || distEnemyArchon < d){
                distEnemyArchon = d;
                closestEnemyArchon = enemy.getLocation();
            }
            //TODO: report archon
        }
    }

}

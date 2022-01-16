package tenth;

import battlecode.common.*;

public class Explore {

    RobotController rc;
    int myVisionRange;
    boolean compareLead = false;
    int cumulativeLead = 0;
    MapLocation closestLead = null;
    int distLead = 0;
    MapLocation exploreLoc = null;
    MapLocation closestEnemyArchon = null;
    int distEnemyArchon = 0;
    RobotType myType;
    MapLocation explore2Target = null;
    int roundExplore2Target = -100;


    static final int TURNS_EXPLORE2 = 30;
    static final double EXPLORE2_STRIDE = 60;

    static int BYTECODE_EXPLORE_LEAD;

    Explore(RobotController rc){
        this.rc = rc;
        myType = rc.getType();
        myVisionRange = myType.visionRadiusSquared;
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
            closestLead = null;
            distLead = 0;
            cumulativeLead = 0;
            MapLocation[] mapLocs = rc.senseNearbyLocationsWithLead(myVisionRange);
            int i = mapLocs.length;
            MapLocation myLoc = rc.getLocation();
            int round = rc.getRoundNum();
            int turnsToLead = 20 - (round%20);
            if (turnsToLead == 20) turnsToLead = 0;
            rc.setIndicatorString("" + turnsToLead);
            while (i-- > 0){
                if (Clock.getBytecodeNum() > BYTECODE_EXPLORE_LEAD) break;
                if (compareLead){
                    int lead = rc.senseLead(mapLocs[i]);
                    if (lead <= Constants.MIN_LEAD_RELEVANT && (round < 120 || Util.distance(myLoc, mapLocs[i]) < turnsToLead)){
                        rc.setIndicatorDot(mapLocs[i], 0, 0, 255);
                        continue;
                    }
                    rc.setIndicatorDot(mapLocs[i], 0, 255, 0);
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

    MapLocation normalize (int x, int y){
        double dx = x, dy = y;
        double norm = Math.sqrt((x*x) + (y*y));
        dx/=norm;
        dy/=norm;
        dx *= EXPLORE2_STRIDE;
        dy *= EXPLORE2_STRIDE;
        int xi = rc.getLocation().x + (int) dx, yi = rc.getLocation().y + (int) dy;
        if (xi < 0) xi = 0;
        if (xi >= rc.getMapWidth()) xi = rc.getMapWidth() - 1;
        if (yi >= rc.getMapHeight()) yi = rc.getMapHeight() - 1;
        return new MapLocation (xi, yi);
    }

    MapLocation getExploreTarget2(){
        if (rc.getRoundNum() - roundExplore2Target < TURNS_EXPLORE2){
            if (explore2Target != null && !rc.canSenseLocation(explore2Target)) return explore2Target;
        }
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), myVisionRange, rc.getTeam());
        MapLocation myLoc = rc.getLocation();
        int x = 0;
        int y = 0;
        boolean found = false;
        for (RobotInfo r : allies){
            if (r.getType() != myType) continue;
            MapLocation loc = r.getLocation();
            int dx = myLoc.x - loc.x, dy = myLoc.y - loc.y;
            if (dx < 0) dx = (-5) - dx;
            else if (dx > 0) dx = 5 - dx;
            if (dy < 0) dy = (-5) - dy;
            if (dy > 0) dy = 5 - dy;
            x += dx;
            y += dy;
            found = true;
        }
        if (!found || (x == 0 && y == 0)){
            explore2Target = getExploreTarget();
            roundExplore2Target = rc.getRoundNum();
            return explore2Target;
        }
        explore2Target = normalize(x,y);
        roundExplore2Target = rc.getRoundNum();
        return explore2Target;
    }

    void resetExplore2(){
        explore2Target = null;
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

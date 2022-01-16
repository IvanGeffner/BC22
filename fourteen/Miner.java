package fourteen;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Miner extends Robot {

    int actionRadius;
    boolean shouldMove = true;
    int profitTurns = 0;
    int myID;

    Miner(RobotController rc){
        super(rc);
        actionRadius = rc.getType().actionRadiusSquared;
        myID = rc.getID();
    }

    void play(){
        tryMine();
        moveToTarget();
        tryMine();
    }

    void moveToTarget(){
        if (!rc.isMovementReady()) return;
        MapLocation loc = getTarget();
        bfs.move(loc);
    }

    MapLocation getTarget(){
        if (!shouldMove) return rc.getLocation();
        MapLocation loc = getClosestLead();
        if (loc == null) return explore.getExploreTarget();
        return loc;
    }

    MapLocation getClosestLead(){
        MapLocation ans = explore.getClosestLead();
        if (ans == null) ans = comm.getClosestLead();
        ans = adapt(ans);
        return ans;
    }

    MapLocation adapt(MapLocation leadLoc){
        try {
            if (leadLoc == null) return null;
            if (!rc.canSenseLocation(leadLoc)) return leadLoc;
            MapLocation ans = null;
            int bestRubble = 0;
            for (int i = 9; i-- > 0; ) {
                Direction dir = directions[i];
                MapLocation newLoc = leadLoc.add(dir);
                if (rc.canSenseLocation(newLoc) && rc.onTheMap(newLoc)) {
                    RobotInfo ri = rc.senseRobotAtLocation(newLoc);
                    if (ri != null && ri.getID() != myID) continue;
                    int r = rc.senseRubble(newLoc);
                    if (ans == null || r < bestRubble){
                        bestRubble = r;
                        ans = newLoc;
                    }
                }
            }
            if (ans != null) return ans;
        } catch (Exception e){
            e.printStackTrace();
        }
        return leadLoc;
    }


    void tryMine(){
        if (!rc.isActionReady()) return;
        try {
            MapLocation[] goldLocs = rc.senseNearbyLocationsWithGold(actionRadius);
            for (MapLocation loc : goldLocs){
                while (rc.canMineGold(loc)){
                    rc.mineGold(loc);
                }
            }

            MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead(actionRadius);
            for (MapLocation loc : leadLocs){
                int lead = rc.senseLead(loc);
                if (lead <= 1) continue;
                while (rc.canMineLead(loc)){
                    rc.mineLead(loc);
                    lead--;
                    if (lead <= 1) break;
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

}

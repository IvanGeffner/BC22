package eleven;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Miner extends Robot {

    int actionRadius;
    boolean shouldMove = true;
    int profitTurns = 0;

    Miner(RobotController rc){
        super(rc);
        actionRadius = rc.getType().actionRadiusSquared;
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
        return ans;
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

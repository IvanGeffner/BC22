package firstbot;

import battlecode.common.*;

public class Miner extends Robot {

    Miner(RobotController rc){
        super(rc);
    }

    void play(){
        tryMine();
        moveToTarget();
        //TODO: also move in danger
        tryMine();
    }

    void moveToTarget(){
        if (!rc.isMovementReady()) return;
        MapLocation loc = getTarget();
        bfs.move(loc);
    }

    MapLocation getTarget(){
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
            for (Direction d : directions) {
                MapLocation newLoc = rc.getLocation().add(d);
                if (rc.canMineGold(newLoc)){
                    rc.mineGold(newLoc);
                    return;
                }
            }

            for (Direction d : directions) {
                MapLocation newLoc = rc.getLocation().add(d);
                if (rc.senseLead(newLoc) <= 1) continue;
                if (rc.canMineLead(newLoc)){
                    rc.mineLead(newLoc);
                    return;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}

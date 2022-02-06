package wammawink;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Sage extends Attacker {

    MicroSage micro;


    Sage(RobotController rc){
        super(rc);
        micro = new MicroSage(rc);
    }

    void play(){

        if (!micro.doMicro()) tryMove();
        tryAttack(false);

    }

    void tryMove(){
        if (!rc.isMovementReady()) return;
        MapLocation target = getTarget();
        bfs.move(target);
    }

    MapLocation getTarget(){
        if (rc.getRoundNum() < Constants.ATTACK_TURN && comm.isEnemyTerritoryRadial(rc.getLocation())) return comm.getClosestAllyArchon();
        MapLocation ans = getBestTarget();
        if (ans != null) return ans;
        ans = comm.getClosestEnemyArchon();
        if (ans != null) return ans;
        return explore.getExploreTarget(true);
    }

}

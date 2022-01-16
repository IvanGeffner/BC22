package seventhbotb;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Soldier extends Robot {

    Soldier(RobotController rc){
        super(rc);
    }

    void play(){
        tryAttack(true);
        tryMove();
        tryAttack(false);
    }

    void tryMove(){
        if (!rc.isMovementReady()) return;
        MapLocation target = getBestTarget();
        if (target == null) target = comm.getClosestEnemyArchon();
        if (target == null) target = explore.getExploreTarget();
        bfs.move(target);
    }



}

package twentyone;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Sage extends Attacker {


    Sage(RobotController rc){
        super(rc);
    }

    void play(){

        checkChickenBehavior();
        tryAttack(true);
        tryMove();
        tryAttack(false);
    }

    void tryMove(){
        if (!rc.isMovementReady()) return;
        MapLocation target = getBestTarget();
        if (target == null && rc.getRoundNum() >= Constants.ATTACK_TURN) target = comm.getClosestEnemyArchon();
        if (target == null) target = explore.getExploreTarget(true);
        bfs.move(target);
    }


}
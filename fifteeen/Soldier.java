package fifteeen;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Soldier extends Attacker {

    boolean explorer = false;

    Soldier(RobotController rc){
        super(rc);
        if (comm.getBuildingScore(RobotType.SOLDIER)%3 == 1) explorer = true;
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
        if (target != null){
            rc.setIndicatorDot(target, 255, 0, 0);
        }
        if (!explorer && target == null) target = comm.getClosestEnemyArchon();
        if (target == null) target = explore.getExploreTarget();
        bfs.move(target);
    }
}

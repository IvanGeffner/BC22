package nineteen;

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
        MapLocation target = getTarget();
        bfs.move(target);
    }

    MapLocation getTarget(){
        if (rc.getRoundNum() < Constants.ATTACK_TURN && comm.isEnemyTerritoryRadial(rc.getLocation())) return comm.getClosestAllyArchon();
        MapLocation ans = getBestTarget();
        if (ans != null) return ans;
        if (!explorer) ans = comm.getClosestEnemyArchon();
        if (ans != null) return ans;
        return explore.getExploreTarget(true);
    }
}

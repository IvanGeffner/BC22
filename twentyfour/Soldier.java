package twentyfour;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Soldier extends Attacker {

    boolean explorer = false;

    Soldier(RobotController rc){
        super(rc);
        checkExploreBehavior();
    }

    void play(){
        checkChickenBehavior();
        tryAttack(true);
        tryMove();
        tryAttack(false);
    }

    void checkExploreBehavior(){
        try {
            int soldierIndex = rc.readSharedArray(comm.SOLDIER_COUNT);
            if (soldierIndex%3 == 2) explorer = true;
            comm.increaseIndex(comm.SOLDIER_COUNT, 1);
        } catch (Exception e){
            e.printStackTrace();
        }
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
        //if (!explorer) ans = comm.getClosestEnemyArchon();
        ans = comm.getClosestEnemyArchon();
        if (ans != null) return ans;
        return explore.getExploreTarget(true);
    }
}

package firstbot;

import battlecode.common.*;

public class Soldier extends Robot {

    Soldier(RobotController rc){
        super(rc);
    }

    void play(){
        tryAttack();
        tryMove();
        tryAttack();
    }

    void tryAttack(){
        if (!rc.isActionReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(explore.myVisionRange, rc.getTeam().opponent());
        AttackTarget bestTarget = null;
        for (RobotInfo enemy : enemies){
            if (rc.canAttack(enemy.location)){
                AttackTarget at = new AttackTarget(enemy);
                if (at.isBetterThan(bestTarget)) bestTarget = at;
            }
        }
        try {
            if (bestTarget != null && rc.canAttack(bestTarget.mloc)) rc.attack(bestTarget.mloc);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void tryMove(){
        if (!rc.isMovementReady()) return;
        MapLocation target = comm.getClosestEnemyArchon();
        //if (target == null) target = explore.getExploreTarget();
        bfs.move(target);
    }

    class AttackTarget{
        RobotType type;
        int health;
        boolean attacker = false;
        MapLocation mloc;

        boolean isBetterThan(AttackTarget t){
            if (t == null) return true;
            if (attacker & !t.attacker) return true;
            if (!attacker & t.attacker) return false;
            return health <= t.health;
        }

        AttackTarget(RobotInfo r){
            type = r.getType();
            health = r.getHealth();
            mloc = r.getLocation();
            switch(type){
                case SOLDIER:
                case SAGE:
                case WATCHTOWER:
                case ARCHON:
                    attacker = true;
                default:
                    break;
            }
        }
    }

}

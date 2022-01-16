package twelve;

import battlecode.common.*;

//TODO:

public abstract class Attacker extends Robot {

    boolean chickenBehavior = false;

    Attacker (RobotController rc){
        super(rc);
    }

    abstract void play();

    void checkChickenBehavior(){
        if (!chickenBehavior && hurt()) chickenBehavior = true;
        if (chickenBehavior && rc.getHealth() >= rc.getType().getMaxHealth(0)) chickenBehavior = false;
    }


    void tryAttack(boolean onlyAttackers){
        if (!rc.isActionReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        AttackTarget bestTarget = null;
        for (RobotInfo enemy : enemies){
            if (onlyAttackers && !Util.isAttacker(enemy.getType())) continue;
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

    MapLocation getBestTarget(){
        if (chickenBehavior){
            MapLocation ans = comm.getClosestAllyArchon();
            if (ans != null){
                int d = ans.distanceSquaredTo(rc.getLocation());
                if (d <= 13) return rc.getLocation();
                return ans;
            }
        }
        MoveTarget bestTarget = null;
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), explore.myVisionRange, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies){
            MoveTarget mt = new MoveTarget(enemy);
            if (mt.isBetterThan(bestTarget)) bestTarget = mt;
        }
        if (bestTarget != null) {
            MapLocation ans = bestAim(bestTarget.mloc);
            if (ans.distanceSquaredTo(bestTarget.mloc) <= rc.getType().actionRadiusSquared) return ans;
            return bestTarget.mloc;
        }
        return null;
    }

    MapLocation bestAim(MapLocation target){
        rc.setIndicatorDot(target, 0, 255, 0);
        AimTarget[] aims = new AimTarget[9];
        for (Direction dir : directions) aims[dir.ordinal()] = new AimTarget(dir, target);
        int minRubble = aims[8].rubble;
        for (int i = 8; i-- > 0; ){
            if (aims[i].canMove && aims[i].rubble < minRubble) minRubble = aims[i].rubble;
        }
        minRubble += Constants.MAX_RUBBLE_DIFF_MOVE;
        for (int i = 8; i-- > 0; ){
            if (aims[i].rubble > minRubble) aims[i].canMove = false;
        }
        AimTarget bestTarget = aims[8];
        for (int i = 8; i-- > 0; ){
            if (aims[i].isBetterThan(bestTarget)){
                bestTarget = aims[i];
            }
        }
        return bestTarget.loc;

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

    class MoveTarget{
        RobotType type;
        int health;
        int priority;
        MapLocation mloc;

        boolean isBetterThan(MoveTarget t){
            if (t == null) return true;
            if (priority > t.priority) return true;
            if (priority < t.priority) return true;
            return health <= t.health;
        }

        MoveTarget(RobotInfo r){
            this.type = r.getType();
            this.health = r.getHealth();
            this.mloc = r.getLocation();
            switch (r.getType()){
                case ARCHON:
                    priority = 0;
                    break;
                case SOLDIER:
                    priority = 5;
                    break;
                case MINER:
                    priority = 4;
                    break;
                case SAGE:
                    priority = 6;
                    break;
                case LABORATORY:
                    priority = 3;
                    break;
                case BUILDER:
                    priority = 1;
                    break;
                case WATCHTOWER:
                    priority = 2;
                    break;
            }
        }
    }

    class AimTarget{
        MapLocation loc;
        int dist;
        int rubble;
        boolean canMove = true;

        AimTarget(Direction dir, MapLocation target){
            if (dir != Direction.CENTER && !rc.canMove(dir)){
                canMove = false;
                return;
            }
            this.loc = rc.adjacentLocation(dir);
            dist = loc.distanceSquaredTo(target);
            try {
                rubble = rc.senseRubble(loc);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        boolean isBetterThan(AimTarget at){
            if (!canMove) return false;
            if (!at.canMove) return true;

            return dist < at.dist;
        }
    }

    boolean hurt(){
        return rc.getHealth()*3 <= rc.getType().getMaxHealth(0);
    }



}

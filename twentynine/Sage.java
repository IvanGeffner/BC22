package twentynine;

import battlecode.common.AnomalyType;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Sage extends Attacker {

    MicroSage micro;


    Sage(RobotController rc){
        super(rc);
    }

    void play(){

        doMicro();
        tryMove();

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

    void tryAttackSage(boolean onlyAttackers){

    }


    class AttackType{

        AnomalyType type = null;
        MapLocation loc = null;
        int kills = 0;
        int damage = 0;


        AttackType(AnomalyType type, MapLocation loc){
            this.type = type;
            this.loc = loc;
        }

        boolean isBetterThan(AttackType at){
            if (kills > 10) kills = 10;
            return (1 << kills)*damage > (1 << at.kills)*at.damage;
        }

        void addAttackerEnemy(RobotInfo r){

        }

        void add




    }


}

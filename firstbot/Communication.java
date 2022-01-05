package firstbot;

import battlecode.common.*;

public class Communication {

    //ARCHONS
    static final int MAX_ARCHONS = 4;
    static final int ARCHON_INDEX = 0;

    //LEAD
    static final int LEAD_INDEX = 15;
    static final int LEAD_QUEUE_SIZE = 9;

    //ARCHONS
    static final int ENEMY_ARCHON_INDEX = 55;
    static final int ENEMY_ARCHON_SIZE = 4;

    //SOLDIERS
    static final int ENEMY_SOLDIER_INDEX = 35;
    static final int ENEMY_SOLDIER_SIZE = 9;

    //GOLD
    static final int GOLD_INDEX = 30;
    static final int GOLD_SIZE = 9;

    static int myID;
    static int myArchonIndex = -1;
    static boolean archon = false;

    static final int INF_COMM = (1 << 15) - 1;

    RobotController rc;

    Communication(RobotController rc){
        this.rc = rc;
        myID = rc.getID();
        if (rc.getType() == RobotType.ARCHON) archon = true;
        if (archon) setArchonIndex();
    }

    void setArchonIndex(){
        try {
            int i = MAX_ARCHONS;
            while (i-- > 0) {
                ++myArchonIndex;
                int id = rc.readSharedArray(3 * myArchonIndex);
                if (id == 0){
                    rc.writeSharedArray(3 * myArchonIndex, myID+1);
                    return;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //only archons
    void reportSelf(){
        if (!archon) return;
        try {
            int locCode = Util.encodeLoc(rc.getLocation());
            rc.writeSharedArray(3*myArchonIndex+1, locCode);
            rc.writeSharedArray(3*myArchonIndex+2, rc.getRoundNum());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void reportLead(MapLocation loc, int lead){
        try {
            int endQueue = rc.readSharedArray(LEAD_INDEX + 2*LEAD_QUEUE_SIZE);
            rc.writeSharedArray(LEAD_INDEX + 2*LEAD_QUEUE_SIZE, (endQueue+1));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE), Target.getCode(loc, Target.LEAD_TYPE));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE) + 1, Math.min(INF_COMM, lead));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void reportSoldier(MapLocation loc, int id){
        try {
            int endQueue = rc.readSharedArray(ENEMY_SOLDIER_INDEX + ENEMY_SOLDIER_SIZE);
            rc.writeSharedArray(ENEMY_SOLDIER_INDEX + ENEMY_SOLDIER_SIZE, (endQueue+1));
            rc.writeSharedArray(ENEMY_SOLDIER_INDEX + (endQueue%ENEMY_SOLDIER_SIZE), Target.getCode(loc, Target.SOLDIER_TYPE));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE) + 1, id);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void reportGold(MapLocation loc, int gold){
        try {
            int endQueue = rc.readSharedArray(GOLD_INDEX + GOLD_SIZE);
            rc.writeSharedArray(GOLD_INDEX + GOLD_SIZE, (endQueue+1));
            rc.writeSharedArray(GOLD_INDEX + (endQueue%GOLD_SIZE), Target.getCode(loc, Target.GOLD_TYPE));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE) + 1, Math.min(INF_COMM, gold));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void reportArchon(MapLocation loc, int id){
        try {
            int endQueue = rc.readSharedArray(ENEMY_ARCHON_INDEX + ENEMY_ARCHON_SIZE);
            rc.writeSharedArray(ENEMY_ARCHON_INDEX + ENEMY_ARCHON_SIZE, (endQueue+1));
            rc.writeSharedArray(ENEMY_ARCHON_INDEX + (endQueue%ENEMY_ARCHON_SIZE), Target.getCode(loc, Target.ARCHON_TYPE));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE) + 1, id);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    MapLocation getClosestLead(){
        return null;
    }

}

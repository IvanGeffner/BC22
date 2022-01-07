package secondbot;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Communication {

    //ARCHONS
    static final int MAX_ARCHONS = 4;
    static final int ARCHON_INDEX = 0;
    static final int ARCHON_NB_INDEX = 14;

    //LEAD
    static final int LEAD_INDEX = 15;
    static final int LEAD_QUEUE_SIZE = 9;

    //ARCHONS
    static final int ENEMY_ARCHON_INDEX = 50;
    static final int ENEMY_ARCHON_SIZE = 4;

    //SOLDIERS
    static final int ENEMY_SOLDIER_INDEX = 35;
    static final int ENEMY_SOLDIER_SIZE = 7;

    //SYMMETRIES:
    static final int H_SYM  = 63;
    static final int V_SYM = 62;
    static final int R_SYM = 61;

    int dim1, dim2;

    static int myID;
    static int myArchonIndex = -1;
    static boolean archon = false;

    static final int INF_COMM = (1 << 16) - 1;

    RobotController rc;

    Communication(RobotController rc){
        this.rc = rc;
        myID = rc.getID();
        if (rc.getType() == RobotType.ARCHON) archon = true;
        if (archon) setArchonIndex();
        dim1 = rc.getMapWidth();
        dim2 = rc.getMapHeight();
    }

    void setArchonIndex(){
        try {
            int i = MAX_ARCHONS;
            while (i-- > 0) {
                ++myArchonIndex;
                int id = rc.readSharedArray(3 * myArchonIndex);
                if (id == 0){
                    rc.writeSharedArray(3 * myArchonIndex, myID+1);
                    break;
                }
            }
            rc.writeSharedArray(ARCHON_NB_INDEX, myArchonIndex+1);
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
            rc.writeSharedArray(LEAD_INDEX + 2*LEAD_QUEUE_SIZE, (endQueue+1)%INF_COMM);
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE), Target.getCode(loc, Target.LEAD_TYPE));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE) + 1, Math.min(INF_COMM, lead));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void reportSoldier(MapLocation loc, int id){
        try {
            int endQueue = rc.readSharedArray(ENEMY_SOLDIER_INDEX + ENEMY_SOLDIER_SIZE);
            rc.writeSharedArray(ENEMY_SOLDIER_INDEX + ENEMY_SOLDIER_SIZE, (endQueue+1)%INF_COMM);
            rc.writeSharedArray(ENEMY_SOLDIER_INDEX + (endQueue%ENEMY_SOLDIER_SIZE), Target.getCode(loc, Target.SOLDIER_TYPE));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE) + 1, id);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void reportArchon(MapLocation loc, int id){
        try {
            int endQueue = rc.readSharedArray(ENEMY_ARCHON_INDEX + ENEMY_ARCHON_SIZE);
            rc.writeSharedArray(ENEMY_ARCHON_INDEX + ENEMY_ARCHON_SIZE, (endQueue+1)%INF_COMM);
            rc.writeSharedArray(ENEMY_ARCHON_INDEX + (endQueue%ENEMY_ARCHON_SIZE), Target.getCode(loc, Target.ARCHON_TYPE));
            rc.writeSharedArray(LEAD_INDEX + 2*(endQueue%LEAD_QUEUE_SIZE) + 1, id);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    MapLocation getClosestLead(){
        return null;
    }

    MapLocation getHSym(MapLocation loc){
        return new MapLocation (dim1 - loc.x - 1, loc.y);
    }

    MapLocation getVSym(MapLocation loc){
        return new MapLocation (loc.x, dim2 - loc.y - 1); }

    MapLocation getRSym(MapLocation loc){
        return new MapLocation (dim1 - loc.x - 1, dim2 - loc.y - 1);
    }

    //Sym stuff
    MapLocation getClosestEnemyArchon(){
        try {
            MapLocation myLoc = rc.getLocation();
            MapLocation ans = null;
            int bestDist = 0;
            int i = rc.readSharedArray(ARCHON_NB_INDEX);
            int hSym = rc.readSharedArray(H_SYM);
            boolean updateh = false;
            int vSym = rc.readSharedArray(V_SYM);
            boolean updatev = false;
            int rSym = rc.readSharedArray(R_SYM);
            boolean updater = false;
            while (i-- > 0){
                MapLocation newLoc = Util.getLocation(rc.readSharedArray(3*i+1));
                if ((hSym&1) == 0 && (hSym & (1 << (i+1))) == 0){
                    MapLocation symLoc = getHSym(newLoc);
                    if (rc.canSenseLocation(symLoc)){
                        RobotInfo r = rc.senseRobotAtLocation(symLoc);
                        if (r == null || r.getType() != RobotType.ARCHON || r.getTeam() != rc.getTeam().opponent()){
                            hSym += (1 << (i+1));
                            updateh = true;
                        }
                    }
                    int d = myLoc.distanceSquaredTo(symLoc);
                    if (ans == null || bestDist > d){
                        bestDist = d;
                        ans = symLoc;
                    }
                }
                if ((vSym&1) == 0 && (vSym & (1 << (i+1))) == 0){
                    MapLocation symLoc = getVSym(newLoc);
                    if (rc.canSenseLocation(symLoc)){
                        RobotInfo r = rc.senseRobotAtLocation(symLoc);
                        if (r == null || r.getType() != RobotType.ARCHON || r.getTeam() != rc.getTeam().opponent()){
                            vSym += (1 << (i+1));
                            updatev = true;
                        }
                    }
                    int d = myLoc.distanceSquaredTo(symLoc);
                    if (ans == null || bestDist > d){
                        bestDist = d;
                        ans = symLoc;
                    }
                }if ((rSym&1) == 0 && (rSym & (1 << (i+1))) == 0){
                    MapLocation symLoc = getRSym(newLoc);
                    if (rc.canSenseLocation(symLoc)){
                        RobotInfo r = rc.senseRobotAtLocation(symLoc);
                        if (r == null || r.getType() != RobotType.ARCHON || r.getTeam() != rc.getTeam().opponent()){
                            rSym += (1 << (i+1));
                            updater = true;
                        }
                    }
                    int d = myLoc.distanceSquaredTo(symLoc);
                    if (ans == null || bestDist > d){
                        bestDist = d;
                        ans = symLoc;
                    }
                }
            }
            if (updateh) rc.writeSharedArray(H_SYM, hSym);
            if (updatev) rc.writeSharedArray(V_SYM, vSym);
            if (updater) rc.writeSharedArray(R_SYM, rSym);
            return ans;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}

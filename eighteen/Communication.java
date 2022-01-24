package eighteen;

import battlecode.common.*;

public class Communication {

    //ARCHONS
    static final int MAX_ARCHONS = 4;
    static final int ARCHON_INDEX = 0;
    static final int ARCHON_NB_INDEX = 14;

    //MINERS
    static final int MINER_COUNT = 46;

    //RESERVE QUEUE
    static final int RESERVE_LEAD_INDEX = 49;
    final int RESERVE_LEAD_SIZE = 5;
    static final int RESERVE_LEAD_FINAL_ELEMENT = 54;
    static final int RESERVE_LEAD_FIRST_ELEMENT = 48;

    //CENTRAL ARCHON
    static final int CENTRAL_ARCHON_INDEX = 47;

    //BUILDING QUEUE
    static final int BUILDING_QUEUE_INDEX = 54;

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

    RobotType[] rTypes = RobotType.values();

    SymmetryChecker symmetryChecker;

     Communication(RobotController rc){
        this.rc = rc;
        myID = rc.getID();
        if (rc.getType() == RobotType.ARCHON) archon = true;
        if (archon) setArchonIndex();
        dim1 = rc.getMapWidth();
        dim2 = rc.getMapHeight();
        symmetryChecker = new SymmetryChecker();
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
            boolean updateSymmetries = rc.getRoundNum() <= 5;
            while (i-- > 0){
                MapLocation newLoc = Util.getLocation(rc.readSharedArray(3*i+1));
                if ((hSym&1) == 0 && (hSym & (1 << (i+1))) == 0){
                    MapLocation symLoc = getHSym(newLoc);
                    if (rc.canSenseLocation(symLoc)){
                        RobotInfo r = rc.senseRobotAtLocation(symLoc);
                        if (r == null || r.getType() != RobotType.ARCHON || r.getTeam() != rc.getTeam().opponent()){
                            hSym += (1 << (i+1));
                            updateh = true;
                            if (updateSymmetries){
                                hSym +=1;
                                System.out.println("Not Horizontal!");
                            }
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
                            if (updateSymmetries){
                                vSym += 1;
                                System.out.println("Not Vertical!");
                            }
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
                            if (updateSymmetries){
                                rSym += 1;
                                System.out.println("Not Rotational!");
                            }
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

    MapLocation getClosestAllyArchon(){
        MapLocation ans = null;
        int bestDist = 0;
        MapLocation myLoc = rc.getLocation();
        try {
            RobotInfo[] allies = rc.senseNearbyRobots(myLoc, rc.getType().visionRadiusSquared, rc.getTeam());
            for (RobotInfo r : allies){
                if (r.getType() != RobotType.ARCHON) continue;
                int d = r.getLocation().distanceSquaredTo(myLoc);
                if (ans == null || bestDist > d) {
                    bestDist = d;
                    ans = r.getLocation();
                }
            }
            if (ans != null) return ans;

            int i = rc.readSharedArray(ARCHON_NB_INDEX);
            while (i-- > 0) {
                MapLocation newLoc = Util.getLocation(rc.readSharedArray(3 * i + 1));
                int d = myLoc.distanceSquaredTo(newLoc);
                if (ans == null || bestDist > d) {
                    bestDist = d;
                    ans = newLoc;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return ans;
    }

    void reportBuilt(RobotType t, int amount){
        try {
            rc.writeSharedArray(BUILDING_QUEUE_INDEX + t.ordinal(), amount);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void increaseIndex(int index, int amount){
        try {
            rc.writeSharedArray(index, rc.readSharedArray(index) + amount);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    int getBuildingScore(RobotType r){
        try {
            return rc.readSharedArray(BUILDING_QUEUE_INDEX + r.ordinal());
        } catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }


    void cancelFirstReservation(){
        try {
            rc.writeSharedArray(RESERVE_LEAD_FIRST_ELEMENT, (rc.readSharedArray(RESERVE_LEAD_FIRST_ELEMENT) + 1)%RESERVE_LEAD_SIZE);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void reserveRobot(RobotType r){
        try {
            int finalElement = rc.readSharedArray(RESERVE_LEAD_FINAL_ELEMENT);
            rc.writeSharedArray(RESERVE_LEAD_INDEX + finalElement, (myArchonIndex << 3) | r.ordinal());
            rc.writeSharedArray(RESERVE_LEAD_FINAL_ELEMENT, (finalElement + 1)%RESERVE_LEAD_SIZE);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    boolean archonAlive (int archonIndex){
        try {
            int r = rc.readSharedArray(3 * archonIndex + 2);
            if (rc.getRoundNum() - r > 2) return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    Reservation getReservedRobot(){
        Reservation r = new Reservation();
        try {
            int i = rc.readSharedArray(RESERVE_LEAD_FIRST_ELEMENT);
            int finalElement = rc.readSharedArray(RESERVE_LEAD_FINAL_ELEMENT);
            if (finalElement < i) finalElement += RESERVE_LEAD_SIZE;
            for (; i != finalElement; ++i){
                int code = rc.readSharedArray(RESERVE_LEAD_INDEX + (i%RESERVE_LEAD_SIZE));
                int aIndex = (code >>> 3);
                if (aIndex != myArchonIndex){
                    if (!archonAlive(aIndex)) resetReservations();
                    r.savedLead += rTypes[code&7].getLeadWorth(0);
                    r.first = false;
                }
                else {
                    r.t = rTypes[code & 7];
                    return r;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return r;
    }

    void resetReservations(){
        try {
            rc.setIndicatorString("Resetting reservations!!");
            rc.writeSharedArray(RESERVE_LEAD_FIRST_ELEMENT, 0);
            rc.writeSharedArray(RESERVE_LEAD_FINAL_ELEMENT, 0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void activateDanger(){
        try {
            int minerScore = getBuildingScore(RobotType.MINER);
            if (minerScore <= Util.getMinMiners()) {
                rc.writeSharedArray(BUILDING_QUEUE_INDEX + RobotType.MINER.ordinal(), Util.getMinMiners() + 1);
                resetReservations();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    int getNewCentralArchon(){
        int w = rc.getMapWidth()/2;
        int h = rc.getMapHeight()/2;
        MapLocation center = new MapLocation(w,h);
        int bestIndex = -1;
        int bestDist = 0;
        try {
            int i = rc.readSharedArray(ARCHON_NB_INDEX);
            while (i-- > 0) {
                if (!archonAlive(i)) continue;
                MapLocation newLoc = Util.getLocation(rc.readSharedArray(3 * i + 1));
                int d = center.distanceSquaredTo(newLoc);
                if (bestIndex < 0 || bestDist > d) {
                    bestDist = d;
                    bestIndex = i;
                }
            }
            if (bestIndex >= 0) rc.writeSharedArray(CENTRAL_ARCHON_INDEX, bestIndex+1);
            return bestIndex;
        } catch (Exception e){
            e.printStackTrace();
        }
        return myArchonIndex;
    }

    MapLocation getCentralArchon(){
        try {
            int i = rc.readSharedArray(CENTRAL_ARCHON_INDEX) - 1;
            if (i < 0 || !archonAlive(i)) i = getNewCentralArchon();
            return Util.getLocation(rc.readSharedArray(3*i+1));
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public class SymmetryChecker {

        int W;
        int H;
        int[][] rubble;
        boolean ready = false;
        int initW = 0;

        SymmetryChecker (){
            W = rc.getMapWidth();
            H = rc.getMapHeight();
            rubble = new int[W][];
        }

        boolean checkReady(){
            if (ready) return true;
            while (initW < W){
                if (Clock.getBytecodesLeft() < 300) return false;
                rubble[initW] = new int[H];
                initW++;
            }
            ready = true;
            return true;
        }

        void checkSymmetry(){
            if (!checkReady()) return;
            try {
                if (Clock.getBytecodesLeft() < 500) return;
                MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), rc.getType().visionRadiusSquared);
                boolean hSym = (rc.readSharedArray(H_SYM) & 1) == 0;
                boolean vSym = (rc.readSharedArray(V_SYM) & 1) == 0;
                boolean rSym = (rc.readSharedArray(R_SYM) & 1) == 0;
                if (hSym && !vSym && !rSym) return;
                if (!hSym && vSym && !rSym) return;
                if (!hSym && !vSym && rSym) return;
                for (MapLocation loc : locs){
                    if (Clock.getBytecodesLeft() < 500) return;
                    int r = rc.senseRubble(loc) + 1;
                    rubble[loc.x][loc.y] = r;
                    if (hSym){
                        MapLocation newLoc = getHSym(loc);
                        int newR = rubble[newLoc.x][newLoc.y];
                        if (newR != 0 && newR != r){
                            hSym = false;
                            rc.writeSharedArray(H_SYM, rc.readSharedArray(H_SYM) + 1);
                            System.out.println("Not horizontal!");
                        }
                    }
                    if (vSym){
                        MapLocation newLoc = getVSym(loc);
                        int newR = rubble[newLoc.x][newLoc.y];
                        if (newR != 0 && newR != r){
                            vSym = false;
                            rc.writeSharedArray(V_SYM, rc.readSharedArray(V_SYM) + 1);
                            System.out.println("Not vertical!");
                        }
                    }
                    if (rSym){
                        MapLocation newLoc = getRSym(loc);
                        int newR = rubble[newLoc.x][newLoc.y];
                        if (newR != 0 && newR != r){
                            rSym = false;
                            rc.writeSharedArray(R_SYM, rc.readSharedArray(R_SYM) + 1);
                            System.out.println("Not rotational!");
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    void checkSymmetryArchon(){
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
                            hSym += 1;
                            updateh = true;
                        }
                    }
                }
                if ((vSym&1) == 0 && (vSym & (1 << (i+1))) == 0){
                    MapLocation symLoc = getVSym(newLoc);
                    if (rc.canSenseLocation(symLoc)){
                        RobotInfo r = rc.senseRobotAtLocation(symLoc);
                        if (r == null || r.getType() != RobotType.ARCHON || r.getTeam() != rc.getTeam().opponent()){
                            vSym += (1 << (i+1));
                            vSym += 1;
                            updatev = true;
                        }
                    }
                }if ((rSym&1) == 0 && (rSym & (1 << (i+1))) == 0){
                    MapLocation symLoc = getRSym(newLoc);
                    if (rc.canSenseLocation(symLoc)){
                        RobotInfo r = rc.senseRobotAtLocation(symLoc);
                        if (r == null || r.getType() != RobotType.ARCHON || r.getTeam() != rc.getTeam().opponent()){
                            rSym += (1 << (i+1));
                            rSym += 1;
                            updater = true;
                        }
                    }
                }
            }
            if (updateh) rc.writeSharedArray(H_SYM, hSym);
            if (updatev) rc.writeSharedArray(V_SYM, vSym);
            if (updater) rc.writeSharedArray(R_SYM, rSym);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    boolean isEnemyTerritory(MapLocation loc, double x){
         try {
             double minDistAlly = -1, minDistEnemy = -1;
             int i = rc.readSharedArray(ARCHON_NB_INDEX);
             int hSym = rc.readSharedArray(H_SYM);
             int vSym = rc.readSharedArray(V_SYM);
             int rSym = rc.readSharedArray(R_SYM);
             while (i-- > 0) {
                 MapLocation newLoc = Util.getLocation(rc.readSharedArray(3 * i + 1));
                 int d = loc.distanceSquaredTo(newLoc);
                 if (minDistAlly < 0 || d < minDistAlly) minDistAlly = d;
                 if ((hSym & 1) == 0 && (hSym & (1 << (i + 1))) == 0) {
                     MapLocation symLoc = getHSym(newLoc);
                     d = loc.distanceSquaredTo(symLoc);
                     if (minDistEnemy < 0 || d < minDistEnemy) minDistEnemy = d;
                 }
                 if ((vSym & 1) == 0 && (vSym & (1 << (i + 1))) == 0) {
                     MapLocation symLoc = getVSym(newLoc);
                     d = loc.distanceSquaredTo(symLoc);
                     if (minDistEnemy < 0 || d < minDistEnemy) minDistEnemy = d;
                 }
                 if ((rSym & 1) == 0 && (rSym & (1 << (i + 1))) == 0) {
                     MapLocation symLoc = getRSym(newLoc);
                     d = loc.distanceSquaredTo(symLoc);
                     if (minDistEnemy < 0 || d < minDistEnemy) minDistEnemy = d;
                 }
             }
             return x * minDistAlly > minDistEnemy;
         } catch (Exception e){
            e.printStackTrace();
         }
        return false;
    }

    boolean isEnemyTerritoryRadial(MapLocation loc){
        try {
            double minDistAlly = -1, minDistEnemy = -1;
            int i = rc.readSharedArray(ARCHON_NB_INDEX);
            int hSym = rc.readSharedArray(H_SYM);
            int vSym = rc.readSharedArray(V_SYM);
            int rSym = rc.readSharedArray(R_SYM);
            while (i-- > 0) {
                MapLocation newLoc = Util.getLocation(rc.readSharedArray(3 * i + 1));
                int d = loc.distanceSquaredTo(newLoc);
                if (minDistAlly < 0 || d < minDistAlly) minDistAlly = d;
                if ((hSym & 1) == 0 && (hSym & (1 << (i + 1))) == 0) {
                    MapLocation symLoc = getHSym(newLoc);
                    d = loc.distanceSquaredTo(symLoc);
                    if (minDistEnemy < 0 || d < minDistEnemy) minDistEnemy = d;
                }
                if ((vSym & 1) == 0 && (vSym & (1 << (i + 1))) == 0) {
                    MapLocation symLoc = getVSym(newLoc);
                    d = loc.distanceSquaredTo(symLoc);
                    if (minDistEnemy < 0 || d < minDistEnemy) minDistEnemy = d;
                }
                if ((rSym & 1) == 0 && (rSym & (1 << (i + 1))) == 0) {
                    MapLocation symLoc = getRSym(newLoc);
                    d = loc.distanceSquaredTo(symLoc);
                    if (minDistEnemy < 0 || d < minDistEnemy) minDistEnemy = d;
                }
            }
            if (minDistAlly <= minDistEnemy) return false;
            if (minDistEnemy <= 100) return true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    boolean shouldMineAggressively(){
         try{
             boolean hSym = (rc.readSharedArray(H_SYM) & 1) == 0;
             boolean vSym = (rc.readSharedArray(V_SYM) & 1) == 0;
             boolean rSym = (rc.readSharedArray(R_SYM) & 1) == 0;
             if (hSym && vSym && rSym) return false;

         } catch (Exception e){
             e.printStackTrace();
         }
         return true;
    }

}

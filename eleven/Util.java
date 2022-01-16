package eleven;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;

public class Util {

    static int distance(MapLocation A, MapLocation B){
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y));
    }
    static int encodeLoc(MapLocation loc){
        return (loc.x << 6) | loc.y;
    }
    static MapLocation getLocation(int code) { return new MapLocation(code >>> 6, code&63); }
    static boolean isAttacker(RobotType r){
        switch (r){
            case SOLDIER:
            case SAGE: return true;
            default: return false;
        }
    }

    static Integer minMiners = null;

    static int getMinMiners(){
        if (minMiners != null) return minMiners;
        minMiners = (Robot.rc.getMapHeight()* Robot.rc.getMapWidth())/80;
        if (minMiners > Constants.INITIAL_MINERS) minMiners = Constants.INITIAL_MINERS;
        return minMiners;
        //return Constants.INITIAL_MINERS;
    }

}

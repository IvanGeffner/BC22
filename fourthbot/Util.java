package fourthbot;

import battlecode.common.MapLocation;

public class Util {

    static int distance(MapLocation A, MapLocation B){
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y));
    }
    static int encodeLoc(MapLocation loc){
        return (loc.x << 6) | loc.y;
    }
    static MapLocation getLocation(int code) { return new MapLocation(code >>> 6, code&63); }

}

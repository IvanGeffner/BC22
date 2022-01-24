package twentyfour;

import battlecode.common.*;

public class MicroMiners  extends Micro {

    MicroMiners(RobotController rc){
        super(rc);
    }

    RobotInfo[] enemies;
    static final int FLEE_STRIDE = 20;



    boolean doMicro2(){
        try {
            if (!rc.isMovementReady()) return true;
            MapLocation myLoc = rc.getLocation();
            enemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().visionRadiusSquared, rc.getTeam().opponent());

            double minDist[] = computeMinDistsToAttackers();
            if (minDist[0] == 0) return false;

            double centerValue = minDist[Direction.CENTER.ordinal()];
            double bestValue = 0;
            Direction bestDir = null;

            for (int i = dirs.length; i-- > 0; ) {
                Direction dir = dirs[i];
                if (dir == Direction.CENTER) continue;
                if (!rc.canMove(dir)) continue;
                double value = getFleeValue(minDist[i] - centerValue, rc.senseRubble(myLoc.add(dir)));
                if (bestDir == null || value > bestValue){
                    bestValue = value;
                    bestDir = dir;
                }
            }
            if (bestDir != null) rc.move(bestDir);
        } catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    MapLocation normalize (int x, int y){
        double dx = x, dy = y;
        double norm = Math.sqrt((x*x) + (y*y));
        dx/=norm;
        dy/=norm;
        dx *= FLEE_STRIDE;
        dy *= FLEE_STRIDE;
        int xi = rc.getLocation().x + (int) dx, yi = rc.getLocation().y + (int) dy;
        if (xi < 0) xi = 0;
        if (xi >= rc.getMapWidth()) xi = rc.getMapWidth() - 1;
        if (yi >= rc.getMapHeight()) yi = rc.getMapHeight() - 1;
        return new MapLocation (xi, yi);
    }

    boolean doMicro(){
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().visionRadiusSquared, rc.getTeam().opponent());
        MapLocation myLoc = rc.getLocation();
        int x = 0;
        int y = 0;
        boolean found = false;
        for (RobotInfo r : enemies){
            if (!Util.isAttacker(r.getType())) continue;
            MapLocation loc = r.getLocation();
            int dx = myLoc.x - loc.x, dy = myLoc.y - loc.y;
            if (dx < 0) dx = (-5) - dx;
            else if (dx > 0) dx = 5 - dx;
            if (dy < 0) dy = (-5) - dy;
            if (dy > 0) dy = 5 - dy;
            x += dx;
            y += dy;
            found = true;
        }
        if (!found) return false;
        if (x == 0 && y == 0) return true;
        MapLocation target = normalize(x,y);
        Direction dir = bfs.getBestDir(target);
        if (dir == null) return true;
        if (dir == Direction.CENTER) return true;
        try {
            if (rc.canMove(dir)) rc.move(dir);
        } catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    double getFleeValue(double muckrakerDist, int rubble){ //TODO
        return (muckrakerDist)/(10 + rubble);
    }

    double[] computeMinDistsToAttackers(){
        MapLocation myLoc = rc.getLocation();

        double[] muckDists = new double[dirs.length];

        for (RobotInfo r : enemies){
            if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING){
                return muckDists;
            }
            if (!Util.isAttacker(r.getType())) continue;
            MapLocation loc = r.getLocation();
            for (int i = dirs.length; i-- > 0; ) {
                double d = Util.fleeDist(myLoc.add(dirs[i]), loc);
                double md = muckDists[i];
                if (md <= 0 || md > d) {
                    muckDists[i] = d;
                }
            }
        }


        return muckDists;
    }

}

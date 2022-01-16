package tenth;

import battlecode.common.*;

public class MicroMiners  extends Micro {

    MicroMiners(RobotController rc){
        super(rc);
    }

    //MapLocation latestEnemy = null;
    int turnsSinceEnemy = -100;
    final static int MEMORY = 2;
    RobotInfo[] enemies;

    boolean playSafe(){
        int uIndex = enemies.length;
        while (uIndex-- > 0){
            RobotInfo r = enemies[uIndex];
            switch(r.getType()){
                case SOLDIER:
                case SAGE:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }



    boolean doMicro(){
        try {
            if (!rc.isMovementReady()) return true;
            MapLocation myLoc = rc.getLocation();
            enemies = rc.senseNearbyRobots(rc.getLocation(), rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            //if (!playSafe() && rc.getRoundNum()  - turnsSinceEnemy > MEMORY) return false;


            double minDist[] = computeMinDistsToAttackers();
            if (minDist[0] == 0) return false;

            double centerValue = minDist[Direction.CENTER.ordinal()];
            double bestValue = 0;
            Direction bestDir = null;

            for (int i = dirs.length; i-- > 0; ) {
                Direction dir = dirs[i];
                if (dir == Direction.CENTER) continue;
                if (!rc.canMove(dir)) continue;
                double value = getFleeValue(minDist[i] - centerValue, rc.senseRubble(myLoc.add(dir))); //TODO: rubble instead of passability
                if (bestDir == null || value > bestValue){
                    bestValue = value;
                    bestDir = dir;
                }
            }
            if (bestDir != null){
                if (rc.canMove(bestDir)) rc.move(bestDir);
            }
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

        //if (latestEnemy != null  && rc.canSenseLocation(latestEnemy)) latestEnemy = null;

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
                    /*if (i == 8){
                        latestEnemy = loc;
                        turnsSinceEnemy = rc.getRoundNum();
                    }*/
                    muckDists[i] = d;
                }
            }
        }


        /*if (latestEnemy != null && rc.getRoundNum()  - turnsSinceEnemy <= MEMORY){
            MapLocation loc = latestEnemy;
            for (int i = dirs.length; i-- > 0; ) {
                double d = Util.fleeDist(myLoc.add(dirs[i]), loc);
                double md = muckDists[i];
                if (md <= 0 || md > d) {
                    muckDists[i] = d;
                }
            }
        }*/


        return muckDists;
    }

}

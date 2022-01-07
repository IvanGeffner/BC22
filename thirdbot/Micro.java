package thirdbot;

import battlecode.common.*;

public class Micro {

    final int INF = 1000000;
    RobotController rc;
    Direction[] dirs = Direction.values();
    boolean attacker = false;
    boolean shouldPlaySafe = false;
    boolean alwaysInRange = false;
    boolean hurt = false; //TODO: if hurt we want to go back to archon
    static int myRange;
    static int myVisionRange;
    static double myDPS;
    static final int MAX_MICRO_BYTECODE = 6000;

    double[] DPS = new double[]{0, 0, 0, 0, 9, 12, 0};
    int[] rangeExtended = new int[]{0, 0, 0, 0, 25, 25, 0};

    Micro(RobotController rc){
        this.rc = rc;
        if (rc.getType() == RobotType.SOLDIER || rc.getType() == RobotType.SAGE) attacker = true;
        myRange = rc.getType().actionRadiusSquared;
        myVisionRange = rc.getType().visionRadiusSquared;
        myDPS = DPS[rc.getType().ordinal()];
    }

    static double currentDPS = 0;
    static double currentRangeExtended;
    static double currentActionRadius;
    static boolean canAttack;

    boolean doMicro(){
        try {
            if (!rc.isMovementReady()) return false;
            shouldPlaySafe = false;
            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            canAttack = rc.isActionReady();

            int uIndex = units.length;
            while (uIndex-- > 0){
                RobotInfo r = units[uIndex];
                switch(r.getType()){
                    case SOLDIER:
                    case SAGE:
                        shouldPlaySafe = true;
                        break;
                }
            }
            if (!shouldPlaySafe) return false;

            alwaysInRange = false;
            if (!attacker || !rc.isActionReady()) alwaysInRange = true;

            MicroInfo[] microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(dirs[i]);

            for (RobotInfo unit : units) {
                if (Clock.getBytecodeNum() > MAX_MICRO_BYTECODE) break;
                int t = unit.getType().ordinal();
                currentDPS = DPS[t] / (10 + rc.senseRubble(unit.getLocation()));
                if (currentDPS <= 0) continue; //TODO: maybe not?
                currentRangeExtended = rangeExtended[t];
                currentActionRadius = unit.getType().actionRadiusSquared;
                microInfo[0].updateEnemy(unit);
                microInfo[1].updateEnemy(unit);
                microInfo[2].updateEnemy(unit);
                microInfo[3].updateEnemy(unit);
                microInfo[4].updateEnemy(unit);
                microInfo[5].updateEnemy(unit);
                microInfo[6].updateEnemy(unit);
                microInfo[7].updateEnemy(unit);
                microInfo[8].updateEnemy(unit);
            }

            //TODO: take into account allies?
            if (myDPS > 0) {
                units = rc.senseNearbyRobots(myVisionRange, rc.getTeam());
                for (RobotInfo unit : units) {
                    if (Clock.getBytecodeNum() > MAX_MICRO_BYTECODE) break;
                    currentDPS = DPS[unit.getType().ordinal()] / (10 + rc.senseRubble(unit.getLocation()));
                    microInfo[0].updateAlly(unit);
                    microInfo[1].updateAlly(unit);
                    microInfo[2].updateAlly(unit);
                    microInfo[3].updateAlly(unit);
                    microInfo[4].updateAlly(unit);
                    microInfo[5].updateAlly(unit);
                    microInfo[6].updateAlly(unit);
                    microInfo[7].updateAlly(unit);
                    microInfo[8].updateAlly(unit);
                }
            }

            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isBetter(bestMicro)) bestMicro = microInfo[i];
            }

            if (rc.canMove(bestMicro.dir)) {
                if (bestMicro.dir == Direction.CENTER) return true;
                rc.move(bestMicro.dir);
                return true;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    class MicroInfo{
        Direction dir;
        MapLocation location;
        int minDistanceToEnemy = INF;
        double DPSreceived = 0;
        double enemiesTargeting = 0;
        double alliesTargeting = 0;
        boolean canMove = true;
        int rubble = 0;

        public MicroInfo(Direction dir){
            this.dir = dir;
            this.location = rc.getLocation().add(dir);
            if (!rc.canMove(dir)) canMove = false;
            else{
                try {
                    rubble = rc.senseRubble(this.location);
                } catch (Exception e){
                    e.printStackTrace();
                }
                if (!hurt){
                    try{
                        if (canAttack){
                            this.DPSreceived -= myDPS/(10 + rc.senseRubble(this.location));
                            this.alliesTargeting += myDPS/(10 + rc.senseRubble(this.location));
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    minDistanceToEnemy = rangeExtended[RobotType.SOLDIER.ordinal()];
                } else minDistanceToEnemy = INF;
            }
        }

        void updateEnemy(RobotInfo unit){
            if (!canMove) return;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist < minDistanceToEnemy)  minDistanceToEnemy = dist;
            if (dist <= currentActionRadius) DPSreceived += currentDPS;
            if (dist <= currentRangeExtended) enemiesTargeting += currentDPS;
        }

        void updateAlly(RobotInfo unit){
            if (!canMove) return;
            alliesTargeting += currentDPS;
        }

        int safe(){
            if (!canMove) return -1;
            if (DPSreceived > 0) return 0;
            if (enemiesTargeting > alliesTargeting) return 1;
            return 2;
        }

        boolean inRange(){
            if (alwaysInRange) return true;
            return minDistanceToEnemy <= myRange;
        }

        //equal => true
        boolean isBetter(MicroInfo M){

            if (safe() > M.safe()) return true;
            if (safe() < M.safe()) return false;

            if (hurt) return true;

            if (inRange() && !M.inRange()) return true;
            if (!inRange() && M.inRange()) return false;

            if (rubble < M.rubble) return true;
            if (M.rubble < rubble) return false;

            if (alliesTargeting > M.alliesTargeting) return true;
            if (alliesTargeting < M.alliesTargeting) return false;

            if (inRange()) return minDistanceToEnemy >= M.minDistanceToEnemy;
            else return minDistanceToEnemy <= M.minDistanceToEnemy;

            //TODO: this doesn't take into account rubble besides yes/no for advancing
        }
    }

}

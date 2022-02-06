package wammawink;

import battlecode.common.*;

public class MicroSage extends Micro {

    final int INF = 1000000;
    boolean shouldPlaySafe = false;
    static int myRange;
    static int myVisionRange;
    static final double myDamage = 45;

    double[] Damage = new double[]{0, 0, 0, 0, 0, 0, 0};
    int[] rangeExtended = new int[]{0, 0, 0, 0, 0, 0, 0};

    MicroSage(RobotController rc){
        super(rc);
        myRange = rc.getType().actionRadiusSquared;
        myVisionRange = rc.getType().visionRadiusSquared;

        Damage[RobotType.SOLDIER.ordinal()] = 12;
        Damage[RobotType.SAGE.ordinal()] = 9;
        rangeExtended[RobotType.SOLDIER.ordinal()] = 20;
        rangeExtended[RobotType.SAGE.ordinal()] = 34;
    }

    static double currentDamage = 0;
    static double currentRangeExtended;
    static double currentActionRadius;
    static double currentDMGCharge;
    static boolean canAttack;
    static double currentAttackScore;
    static int myHealth;
    static MicroInfo[] microInfo;

    final static int MAX_RUBBLE_DIFF = 5;
    final static int MAX_RUBBLE_DIFF_FLEE = 10;

    boolean moveReady, actReady;

    final static int KILL_SCORE = 1 << 20;

    double getCurrentAttackScore(RobotInfo r){
        int ans = 0;
        if (r.getHealth() <= 45) ans += KILL_SCORE + (r.getHealth() << 10) + (1000 - r.getHealth());
        else ans = (45 << 10) + (1000 - r.getHealth());
        return ans;
    }

    boolean doMicro(){
        try {
            moveReady = rc.isMovementReady();
            actReady = rc.isActionReady();
            if (!moveReady && !actReady) return true;
            //severelyHurt = Util.hurt(rc.getHealth(), rc.getType().getMaxHealth(0));
            myHealth = rc.getHealth();

            RobotInfo[] units = rc.senseNearbyRobots(myVisionRange, rc.getTeam().opponent());
            if (units.length == 0) return false;
            canAttack = rc.isActionReady();

            int uIndex = units.length;
            while (uIndex-- > 0){
                RobotInfo r = units[uIndex];
                switch(r.getType()){
                    case SOLDIER:
                    case SAGE:
                        shouldPlaySafe = true;
                        break;
                    default:
                        break;
                }
            }

            if (!shouldPlaySafe) return false;

            microInfo = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) microInfo[i] = new MicroInfo(dirs[i], i);

            int minRubble = microInfo[8].rubble;
            if (microInfo[7].canMove && minRubble > microInfo[7].rubble) minRubble = microInfo[7].rubble;
            if (microInfo[6].canMove && minRubble > microInfo[6].rubble) minRubble = microInfo[6].rubble;
            if (microInfo[5].canMove && minRubble > microInfo[5].rubble) minRubble = microInfo[5].rubble;
            if (microInfo[4].canMove && minRubble > microInfo[4].rubble) minRubble = microInfo[4].rubble;
            if (microInfo[3].canMove && minRubble > microInfo[3].rubble) minRubble = microInfo[3].rubble;
            if (microInfo[2].canMove && minRubble > microInfo[2].rubble) minRubble = microInfo[2].rubble;
            if (microInfo[1].canMove && minRubble > microInfo[1].rubble) minRubble = microInfo[1].rubble;
            if (microInfo[0].canMove && minRubble > microInfo[0].rubble) minRubble = microInfo[0].rubble;

            minRubble += canAttack ? MAX_RUBBLE_DIFF : MAX_RUBBLE_DIFF_FLEE;

            if (microInfo[8].rubble > minRubble) microInfo[8].canMove = false;
            if (microInfo[7].rubble > minRubble) microInfo[7].canMove = false;
            if (microInfo[6].rubble > minRubble) microInfo[6].canMove = false;
            if (microInfo[5].rubble > minRubble) microInfo[5].canMove = false;
            if (microInfo[4].rubble > minRubble) microInfo[4].canMove = false;
            if (microInfo[3].rubble > minRubble) microInfo[3].canMove = false;
            if (microInfo[2].rubble > minRubble) microInfo[2].canMove = false;
            if (microInfo[1].rubble > minRubble) microInfo[1].canMove = false;
            if (microInfo[0].rubble > minRubble) microInfo[0].canMove = false;


            for (RobotInfo unit : units) {
                if (Clock.getBytecodesLeft() < MAX_MICRO_BYTECODE_REMAINING) break;
                int t = unit.getType().ordinal();
                currentDamage = Damage[t];
                if (currentDamage <= 0) continue;
                currentRangeExtended = rangeExtended[t];
                currentActionRadius = unit.getType().actionRadiusSquared;
                currentDMGCharge = (double)((unit.getType().getMaxHealth(0)*22)/100);
                currentAttackScore = getCurrentAttackScore(unit);
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

            microInfo[0].computeScores();
            microInfo[1].computeScores();
            microInfo[2].computeScores();
            microInfo[3].computeScores();
            microInfo[4].computeScores();
            microInfo[5].computeScores();
            microInfo[6].computeScores();
            microInfo[7].computeScores();
            microInfo[8].computeScores();


            if (moveReady && actReady) return tryAttackMove();
            if (moveReady) return tryMove();
            if (actReady) return tryAttack(8);

            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isBetter(bestMicro)) bestMicro = microInfo[i];
            }

            if (bestMicro.dir == Direction.CENTER) return true;

            if (rc.canMove(bestMicro.dir)) {
                rc.move(bestMicro.dir);
                return true;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    boolean tryAttack(int index){
        try {
            if (microInfo[index].chargeScore <= 0) return true;
            if (microInfo[index].chargeScore >= microInfo[index].attackScore) {
                if (rc.canEnvision(AnomalyType.CHARGE)) rc.envision((AnomalyType.CHARGE));
            } else {
                RobotInfo r = microInfo[index].bestTarget;
                if (r != null){
                    if (rc.canAttack(r.getLocation())) rc.attack(r.getLocation());
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    boolean tryMove(){
        try {
            MicroInfo bestMicro = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isSafer(bestMicro)) bestMicro = microInfo[i];
            }

            if (bestMicro.dir == Direction.CENTER) return true;

            if (rc.canMove(bestMicro.dir)) {
                rc.move(bestMicro.dir);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    boolean tryAttackMove(){
        try {
            rc.setIndicatorString("Trying to move and attack");
            MicroInfo bestMicroAttack = microInfo[8], bestMicroSafe = microInfo[8];
            for (int i = 0; i < 8; ++i) {
                if (microInfo[i].isSafer(bestMicroSafe)) bestMicroSafe = microInfo[i];
                if (microInfo[i].isBetter(bestMicroAttack)) bestMicroAttack = microInfo[i];
            }

            if ((bestMicroAttack.bestScore + 1) * (bestMicroSafe.enemyScore + 1) * (microInfo[8].rubble + 10) > (microInfo[8].bestScore + 1) * (bestMicroAttack.enemyScore + 1) * (bestMicroAttack.rubble + 10)) {
                if (rc.canMove(bestMicroAttack.dir)) rc.move(bestMicroAttack.dir);
                tryAttack(bestMicroAttack.index);
            } else {
                tryAttack(8);
                tryMove();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    class MicroInfo{
        Direction dir;
        MapLocation location;
        int minDistanceToEnemy = INF;
        double damageReceived = 0;
        double attackScore = 0;
        double damageCharge = 0;
        double chargeScore  = 0;
        double enemyScore = 0;
        RobotInfo bestTarget = null;
        int killCharge = 0;
        double bestScore = 0;
        int index;

        boolean canMove = true;
        int rubble = 0;

        public MicroInfo(Direction dir, int index){
            this.dir = dir;
            this.index = index;
            this.location = rc.getLocation().add(dir);
            if (dir != Direction.CENTER && !rc.canMove(dir)) canMove = false;
            else{
                try {
                    rubble = rc.senseRubble(this.location);
                } catch (Exception e){
                    e.printStackTrace();
                }
                minDistanceToEnemy = rangeExtended[RobotType.SAGE.ordinal()];
            }
        }

        void updateEnemy(RobotInfo unit){
            if (!canMove) return;
            int dist = unit.getLocation().distanceSquaredTo(location);
            if (dist < minDistanceToEnemy)  minDistanceToEnemy = dist;
            if (dist <= currentActionRadius) damageReceived += currentDamage;
            //if (dist <= currentRangeExtended) enemiesTargeting += currentDamage;

            if (dist <= myRange){
                if (unit.getHealth() <= currentDMGCharge) {
                    ++killCharge;
                    damageCharge += unit.getHealth();
                } else {
                    damageCharge += currentDMGCharge;
                }

                if(bestTarget == null || currentAttackScore > attackScore) {
                    attackScore = currentAttackScore;
                    bestTarget = unit;
                }
            }
        }

        void computeScores(){
            chargeScore = killCharge*KILL_SCORE + (damageCharge *(1 << 10));
            if (damageReceived >= myHealth) enemyScore = KILL_SCORE + myHealth*(1 << 10);
            else enemyScore = damageReceived*(1 << 10);
            bestScore = Math.max(chargeScore, attackScore);
        }

        boolean isBetter(MicroInfo M){

            if (canMove && !M.canMove) return true;
            if (!canMove && M.canMove) return false;

            return ((bestScore + 1)*(M.enemyScore+1)*(10 + M.rubble) >= (M.bestScore+1)*(enemyScore+1)*(10 + rubble));
        }

        boolean isSafer(MicroInfo M){

            if (canMove && !M.canMove) return true;
            if (!canMove && M.canMove) return false;

            if (enemyScore > M.enemyScore) return false;
            if (M.enemyScore > enemyScore) return true;

            return minDistanceToEnemy >= M.minDistanceToEnemy;
        }

    }

}

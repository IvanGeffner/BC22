package thirdbot;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class RobotPlayer {

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Robot robot;
        switch(rc.getType()) {
            case ARCHON:
                robot = new Archon(rc);
                break;
            case MINER:
                robot = new Miner(rc);
                break;
            case BUILDER:
                robot = new Builder(rc);
                break;
            case SOLDIER:
                robot = new Soldier(rc);
                break;
            case SAGE:
                robot = new Sage(rc);
                break;
            case LABORATORY:
                robot = new Lab(rc);
                break;
            default:
                robot = new Tower(rc);
                break;
        }

        while(true){
            robot.initTurn();
            robot.play();
            robot.endTurn();
            Clock.yield();
        }
    }
}

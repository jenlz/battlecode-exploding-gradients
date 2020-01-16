package testplayer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

public class HQ extends Robots {
    static void runHQ() throws GameActionException {
        int minersBuilt = 0;
        for (Direction dir : directions) {
            if (rc.getTeamSoup() > RobotType.REFINERY.cost) {
                tryBuild(RobotType.MINER, dir);
                if (tryBuild(RobotType.MINER, dir)) {
                    minersBuilt++;
                }
            }
        }

    }
}

package testplayer;

import battlecode.common.*;

import java.util.HashMap;
import java.util.Map;

public class BlockChain extends RobotPlayer{

    /* Standards for messages
        [identifier (343 for now), message category, cost/urgency, desc1, desc2, x-coord, y-coord]

     */

    static int identifier = 343;

    enum category {
        UNITLOCATED, SOUPLOCATED
    }

    static boolean tryBlockChain(int[] message, int cost) throws GameActionException {
        System.out.println("Trying to send a message");
        if (rc.canSubmitTransaction(message, cost)) {
            rc.submitTransaction(message, cost);
            System.out.println("Sent a message!");
            return true;
        }
            return false;
    }

    static void sendUnitLocated(RobotInfo info, MapLocation loc)  throws GameActionException{
        int cost = 1;

        int[] message = new int[]{identifier, category.UNITLOCATED.ordinal(), cost, info.getType().ordinal(), info.getTeam().ordinal(), loc.x, loc.y};
        // TODO if tryblockChain() returns false leave to later turn to send message
        tryBlockChain(message, cost);
    }

}

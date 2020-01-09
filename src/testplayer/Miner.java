package testplayer;

import battlecode.common.*;

import java.util.ArrayList;

public class Miner extends Robots {

    //static int sensorRange = (int) Math.round(this.type.sensorRadiusSquared * GameConstants.getSensorRadiusPollutionCoefficient(this.gameWorld.getPollution(getLocation())));

    private static MapLocation HQLoc;
    static ArrayList<MapLocation> allyRefineries = new ArrayList<MapLocation>();

    static void runMiner() throws GameActionException {
        // TODO Decide whether HQ should be one of refineries in list. Currently added.
        System.out.println("Before code: " + Clock.getBytecodesLeft());

        if (HQLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.HQ) {
                    HQLoc = robot.getLocation();
                    allyRefineries.add(HQLoc);
                }
            }
        }

        Transaction[] currentMessages = rc.getBlock(rc.getRoundNum() - 1);

        for (Transaction message : currentMessages) {
            if (message.getMessage()[0] == BlockChain.identifier) {
                switch (message.getMessage()[1]) {
                    case 0: // BlockChain.category.UNITLOCATED.ordinal()
                        storeUnitLocation(message.getMessage()[2], message.getMessage()[3],message.getMessage()[4], message.getMessage()[5], message.getMessage()[6]);
                        break;
                    }
            }

        }
        System.out.println("After transaction: " + Clock.getBytecodesLeft());
        mineSoup();
        tryMove(randomDirection());

        System.out.println("End: " + Clock.getBytecodesLeft());
    }

    /**
     * Process in which Miner goes about mining and refining soup
     * @throws GameActionException
     */
    static void mineSoup() throws GameActionException{
        for (Direction dir : directions)
            if (tryRefine(dir))
                System.out.println("I refined soup! " + rc.getTeamSoup());
        for (Direction dir : directions)
            if (tryMine(dir))
                System.out.println("I mined soup! " + rc.getSoupCarrying());
        System.out.println("After refine and mine " + Clock.getBytecodesLeft());
        for (Direction dir : directions)
            if (shouldBuildRefinery()) {
                tryBuild(RobotType.REFINERY, dir);
            }
        System.out.println("After refine, mine, build " + Clock.getBytecodesLeft());
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            int closestRefineryIndex = closestLocation(rc.getLocation(), allyRefineries);
            tryMove(rc.getLocation().directionTo(allyRefineries.get(closestRefineryIndex)));
        }
    }

    /**
     * Decides whether to build refinery
     * @return boolean
     * @throws GameActionException
     */
    static boolean shouldBuildRefinery() throws GameActionException {
        // TODO somehow separate cost of message from how shouldBuildRefinery functions
        int minimumDistance = 50;

        for (Direction dir : directions) {
            //Finds closest refinery
            MapLocation soupLocation = rc.adjacentLocation(dir);
            int closestRefineryDistance = closestDistance(soupLocation, allyRefineries);
            // Decides whether to build refinery
            if (rc.senseSoup(soupLocation) != 0 && rc.getTeamSoup() > RobotType.REFINERY.cost + 1 && closestRefineryDistance > minimumDistance) {
                return true;
            }
        }
        return false;
    }

    /**
     * Interprets and stores category UNITLOCATED
     */
    static void storeUnitLocation(int urgency, int unitType, int team, int x, int y) {
        if (team == 0) {
            switch(unitType) {
                case 0: // RobotType.REFINERY.ordinal()
                    allyRefineries.add(rc.getLocation().translate(-rc.getLocation().x + x, -rc.getLocation().y + y));
            }
        }
    }

    /**
     * Returns index of location within ArrayList closest to loc1
     * @param loc1
     * @param locs
     * @return
     */
    static int closestLocation(MapLocation loc1, ArrayList<MapLocation> locs) {
        int closestLocationIndex = 0;
        int closestDistance = Integer.MAX_VALUE;
        for (MapLocation loc2 : locs) {
            System.out.println();
            int dist = loc1.distanceSquaredTo(loc2);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestLocationIndex = locs.indexOf(loc2);
            }
        }
        return closestLocationIndex;
    }

    /**
     * Returns distance between the closest location within locs to loc1
     * @param loc1
     * @param locs
     * @return
     */
    static int closestDistance(MapLocation loc1, ArrayList<MapLocation> locs) {
        int closestDistance = Integer.MAX_VALUE;
        for (MapLocation loc2 : locs) {
            int dist = loc1.distanceSquaredTo(loc2);
            if (dist < closestDistance) {
                closestDistance = dist;
            }
        }
        return closestDistance;
    }

}

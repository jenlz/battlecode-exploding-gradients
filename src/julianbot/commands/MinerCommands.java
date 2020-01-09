package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import julianbot.robotdata.MinerData;

public class MinerCommands {
	
	static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	public static boolean locateNearbyDesignSchool(RobotController rc) throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
		for(RobotInfo robot : robots) {
			if(robot.type == RobotType.DESIGN_SCHOOL) return true;
		}
		
		return false;
	}
	
	public static boolean attemptDesignSchoolConstruction(RobotController rc) throws GameActionException {
		for(Direction buildDirection : directions) {
			if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL, buildDirection)) {
				rc.buildRobot(RobotType.DESIGN_SCHOOL, buildDirection);
				return true;
			}
		}
		
		System.out.println("Failed to build design school...");
		
		return false;
	}
	
	public static Direction getAdjacentRefineryDirection(RobotController rc) throws GameActionException {		
		RobotInfo[] robots = rc.senseNearbyRobots(3, rc.getTeam());
		for(RobotInfo robot : robots) {
			if(robot.type == RobotType.REFINERY) return rc.getLocation().directionTo(robot.getLocation());
		}
		
		return Direction.CENTER;
	}
	
	public static Direction getAnyRefineryDirection(RobotController rc) throws GameActionException {		
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
		for(RobotInfo robot : robots) {
			if(robot.type == RobotType.REFINERY) return rc.getLocation().directionTo(robot.getLocation());
		}
		
		return Direction.CENTER;
	}
	
	public static boolean attemptRefineryConstruction(RobotController rc) throws GameActionException {
		for(Direction buildDirection : directions) {
			if(rc.canBuildRobot(RobotType.REFINERY, buildDirection)) {
				rc.buildRobot(RobotType.REFINERY, buildDirection);
				return true;
			}
		}
		
		System.out.println("Failed to build refinery...");
		
		return false;
	}
	
	public static void depositRawSoup(RobotController rc, Direction dir) throws GameActionException {
		if(rc.canDepositSoup(dir)) rc.depositSoup(dir, rc.getSoupCarrying());
	}

	/**
	 * Finds location with the most soup within 1 tile radius
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	public static Direction getAdjacentSoupDirection(RobotController rc) throws GameActionException {
		Direction mostSoupDirection = Direction.CENTER;
		int mostSoupLocated = 0;
		
		for(Direction searchDirection : directions) {
			if (rc.canSenseLocation(rc.adjacentLocation(searchDirection).add(searchDirection))) {
				int foundSoup = rc.senseSoup(rc.adjacentLocation(searchDirection));
				mostSoupDirection = foundSoup > mostSoupLocated ? searchDirection : mostSoupDirection;
			}
		}
		
		return mostSoupDirection;
	}

	/**
	 * Finds location with the most soup within a 2 tile radius
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	public static Direction getDistantSoupDirection(RobotController rc) throws GameActionException {
		Direction mostSoupDirection = Direction.CENTER;
		int mostSoupLocated = 0;
		
		for(Direction searchDirection : directions) {
			if (rc.canSenseLocation(rc.adjacentLocation(searchDirection).add(searchDirection))) {
				int foundSoup = rc.senseSoup(rc.adjacentLocation(searchDirection).add(searchDirection));
				mostSoupDirection = foundSoup > mostSoupLocated ? searchDirection : mostSoupDirection;
			}
		}
		
		return mostSoupDirection;
	}

	/**
	 * Mines soup if able
	 * @param rc Robot Controller
	 * @param dir Direction
	 * @throws GameActionException
	 */
	public static void mineRawSoup(RobotController rc, Direction dir) throws GameActionException {
		if(rc.canMineSoup(dir)) rc.mineSoup(dir);
	}

	/**
	 * Moves in same direction as before, otherwise moves in random direction
	 * @param rc
	 * @param data
	 * @throws GameActionException
	 */
	public static void continueSearch(RobotController rc, MinerData data) throws GameActionException {
		//The move function is deliberately unused here.
		if(!rc.isReady()) return;
		if(rc.canMove(data.getSearchDirection()) && !rc.senseFlooding(rc.getLocation().add(data.getSearchDirection()))) {
			rc.move(data.getSearchDirection());
			return;
		}
			data.setSearchDirection(goTo(rc, data.getSearchDirection()));

	}

	/**
	 * If can't go directly in direction, attempts to go to direction rotated
	 * @param rc
	 * @param dir
	 * @return
	 * @throws GameActionException
	 */
	public static Direction goTo(RobotController rc, Direction dir) throws GameActionException {
		Direction[] dirs = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};

		for (Direction newDir : dirs) {
			if (tryMove(rc)) {
				return newDir;
			};
		}

		return directions[(int) (Math.random() * directions.length)];

	}

	/**
	 * Checks if robot can move without destroying itself
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	public static boolean tryMove(RobotController rc) throws GameActionException {
		for (Direction dir : directions) {
			if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
				return true;
			}
		}
		return false;
	}
	
}

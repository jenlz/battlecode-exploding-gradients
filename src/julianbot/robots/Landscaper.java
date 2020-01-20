package julianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.LandscaperData;
import julianbot.utils.NumberMath;

public class Landscaper extends Robot {

	private static final int DEFAULT_PATTERN_ARRAY_SHIFT = 2;
	private int gridXShift;
	private int gridYShift;
	
	private static Direction[][] clockwiseMovePattern = new Direction[][]{
		{Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH},
		{Direction.NORTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.WEST}
	};
	
	private static Direction[][] clockwiseDigPattern = new Direction[][]{
		{Direction.NORTH, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHWEST, Direction.EAST},
		{Direction.SOUTHWEST, null, null, null, Direction.NORTHEAST},
		{Direction.WEST, null, null, null, Direction.EAST},
		{Direction.SOUTHWEST, null, null, null, Direction.NORTHEAST},
		{Direction.WEST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTH}
	};
	
	private static Direction[][][] clockwiseBuildPattern = new Direction[][][] {
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
	};
	
	private static Direction[][] counterClockwiseMovePattern = new Direction[][]{
		{Direction.SOUTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.WEST},
		{Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.NORTH, Direction.NORTH},
		{Direction.SOUTH, Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH},
		{Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.EAST, Direction.NORTH},
		{Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.NORTH}
	};
	
	private static Direction[][] counterClockwiseDigPattern = new Direction[][]{
		{Direction.WEST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTHEAST, Direction.NORTH},
		{Direction.NORTHWEST, null, null, null, Direction.SOUTHEAST},
		{Direction.WEST, null, null, null, Direction.EAST},
		{Direction.NORTHWEST, null, null, null, Direction.SOUTHEAST},
		{Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHWEST, Direction.EAST}
	};
	
	private static Direction[][][] counterClockwiseBuildPattern = new Direction[][][] {
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
	};
	
	private LandscaperData landscaperData;
	private Direction[][] movePattern;
	private Direction[][] digPattern;
	private Direction[][][] buildPattern;
	
	public Landscaper(RobotController rc) {
		super(rc);
		this.data = new LandscaperData(rc, getSpawnerLocation());
		this.landscaperData = (LandscaperData) this.data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
    	if(turnCount == 1) {
    		learnHQLocation();
    		determineWallDirections();
    	}
    	
    	discernAttackRole();
    	
		RobotInfo enemyDesign = senseUnitType(RobotType.DESIGN_SCHOOL, landscaperData.getOpponent());
    	
		System.out.println("Landscaper Role = " + landscaperData.getCurrentRole());
		
    	if(buryEnemyHQ()) {
    		/*Do nothing else*/
    	} else if(enemyDesign!=null){
    		buryEnemyDesign(enemyDesign);
    	} else if(landscaperData.getCurrentRole() == LandscaperData.TRAVEL_TO_HQ) {
    		if(!approachComplete()) {
    			routeTo(landscaperData.getHqLocation());
    		} else {
    			landscaperData.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
    		}
    	} else if(landscaperData.getCurrentRole() == LandscaperData.DEFEND_HQ_FROM_FLOOD) {
    		buildHQWall();
    	}
	}
	
	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				landscaperData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}		
	}
	
	private void determineWallDirections() {
		MapLocation hqLocation = landscaperData.getHqLocation();
		
		boolean leftEdge = hqLocation.x <= 0;
		boolean rightEdge = hqLocation.x >= rc.getMapWidth() - 1;
		boolean topEdge = hqLocation.y >= rc.getMapHeight() - 1;
		boolean bottomEdge = hqLocation.y <= 0;
		
		if((topEdge && leftEdge) || (bottomEdge && rightEdge)) {
			digPattern = counterClockwiseDigPattern;
			movePattern = counterClockwiseMovePattern;
			buildPattern = counterClockwiseBuildPattern;
		} else {
			digPattern = clockwiseDigPattern;
			movePattern = clockwiseMovePattern;
			buildPattern = clockwiseBuildPattern;
		}
		
		if(topEdge) {
			gridXShift = DEFAULT_PATTERN_ARRAY_SHIFT;
			gridYShift = DEFAULT_PATTERN_ARRAY_SHIFT - 1;
		} else if(bottomEdge) {
			gridXShift = DEFAULT_PATTERN_ARRAY_SHIFT;
			gridYShift = DEFAULT_PATTERN_ARRAY_SHIFT + 1;
		} else {
			gridXShift = gridYShift = DEFAULT_PATTERN_ARRAY_SHIFT;
		}
	}
	
	private void toggleDirection() {
		if(digPattern == clockwiseDigPattern) {
			digPattern = counterClockwiseDigPattern;
			movePattern = counterClockwiseMovePattern;
			buildPattern = counterClockwiseBuildPattern;
		} else {
			digPattern = clockwiseDigPattern;
			movePattern = clockwiseMovePattern;
			buildPattern = clockwiseBuildPattern;
		}
	}

	private void discernAttackRole() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = landscaperData.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
				
		int gridX = dx + gridXShift;
		int gridY = -dy + gridYShift;
		
		int distanceSquaredFromHq = landscaperData.getHqLocation().distanceSquaredTo(rc.getLocation());
		if(distanceSquaredFromHq <= 3 &&
				rc.senseElevation(rc.getLocation().add(movePattern[gridY][gridX])) - rc.senseElevation(rc.getLocation()) > GameConstants.MAX_DIRT_DIFFERENCE) {
			landscaperData.setCurrentRole(LandscaperData.ATTACK);
		} else if(4 <= distanceSquaredFromHq && distanceSquaredFromHq <= 8) {
			landscaperData.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
		}
	}
	
	private boolean dig(Direction dir) throws GameActionException {
		waitUntilReady();
		if(rc.isReady() && rc.canDigDirt(dir)) {
			rc.setIndicatorDot(rc.getLocation().add(dir), 255, 0, 0);
			rc.digDirt(dir);
			return true;
		}
		
		return false;
	}
	
	private boolean depositDirt(Direction dir) throws GameActionException {
		waitUntilReady();
		if(rc.isReady() && rc.canDepositDirt(dir)) {
			rc.setIndicatorDot(rc.getLocation().add(dir), 0, 255, 0);
			rc.depositDirt(dir);
			return true;
		}
		
		return false;
	}
	
	private boolean approachComplete() {
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = landscaperData.getHqLocation();
		return Math.abs(rcLocation.x - hqLocation.x) <= 2 && Math.abs(rcLocation.y - hqLocation.y) <= 2;
	}
	
	private void buildHQWall() throws GameActionException {
		if(rc.getDirtCarrying() > 0) constructWallUnits();
		else digWallDirt();
	}
	
	private void constructWallUnits() throws GameActionException {
		Direction[] constructDirections = new Direction[0];
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = landscaperData.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
				
		int gridX = dx + gridXShift;
		int gridY = -dy + gridYShift;
		
		if(gridX < 0 || gridX >= movePattern[0].length || gridY < 0 || gridY >= movePattern.length) return;
		
		//In the event that our wall reaches the end of the map, we just want to go back and forth about .
		if(!rc.onTheMap(rcLocation.add(movePattern[gridY][gridX]))) {
			toggleDirection();
			return;
		}
		
		//If where we're going is too low, deposit dirt there.
		if(rc.canSenseLocation(rcLocation.add(movePattern[gridY][gridX])) && rc.senseElevation(rcLocation) - rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) > GameConstants.MAX_DIRT_DIFFERENCE) {
			depositDirt(movePattern[gridY][gridX]);
			return;
		}
		
		constructDirections = buildPattern[gridY][gridX];
		if(constructDirections.length == 0) {
			move(movePattern[gridY][gridX]);
			return;
		}
		
		int[] constructElevations = new int[constructDirections.length];
		int lowestElevation = Integer.MAX_VALUE;
		for(int i = 0; i < constructElevations.length; i++) {
			if (rc.canSenseLocation(rcLocation.add(constructDirections[i]))) {
				constructElevations[i] = rc.senseElevation(rcLocation.add(constructDirections[i]));
				lowestElevation = (constructElevations[i] < lowestElevation) ? constructElevations[i] : lowestElevation;
			}
		}
		
		MapLocation nextLocation = rcLocation.add(movePattern[gridY][gridX]);
		
		if(rc.senseFlooding(nextLocation)) depositDirt(rcLocation.directionTo(nextLocation));
		else if(rc.senseElevation(rcLocation) - rc.senseElevation(nextLocation) < GameConstants.MAX_DIRT_DIFFERENCE) depositDirt(constructDirections[NumberMath.indexOfLeast(constructElevations)]);
		else move(movePattern[gridY][gridX]);
	}
	
	private void digWallDirt() throws GameActionException {
		Direction digDirection = null;
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = landscaperData.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
		
		int gridX = dx + gridXShift;
		int gridY = -dy + gridYShift;
		
		//In the event that our wall reaches the end of the map, we just want to make as tall a pillar as possible.
		if(!rc.onTheMap(rcLocation.add(movePattern[gridY][gridX]))) {
			dig(digPattern[gridY][gridX]);
			return;
		}
		
		//If where we're going is too high, dig from there.
		if(rc.canSenseLocation(rcLocation.add(movePattern[gridY][gridX])) && rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) - rc.senseElevation(rcLocation) > GameConstants.MAX_DIRT_DIFFERENCE) {
			dig(movePattern[gridY][gridX]);
			return;
		}
		
		digDirection = digPattern[gridY][gridX];
		
		if(digDirection != null) dig(digDirection);
		if(!rc.senseFlooding(rcLocation.add(movePattern[gridY][gridX]))) move(movePattern[gridY][gridX]);
	}
	
	private boolean buryEnemyHQ() throws GameActionException {
		if(landscaperData.getEnemyHQLocation() != null) {
			Direction dirToHQ = rc.getLocation().directionTo(landscaperData.getEnemyHQLocation());
			if(!rc.getLocation().isAdjacentTo(landscaperData.getEnemyHQLocation())) {
				if (!routeTo(landscaperData.getEnemyHQLocation())) {
					int dirtDifference = rc.senseElevation(rc.getLocation()) - rc.senseElevation(rc.adjacentLocation(dirToHQ));
					RobotInfo[] robots = rc.senseNearbyRobots();
					boolean robotInTheWay = false;
					for (RobotInfo robot : robots) {
						if ((!robot.getType().isBuilding() || robot.getTeam() == rc.getTeam()) && robot.getLocation() == rc.getLocation().add(dirToHQ)) {
							robotInTheWay = true;
						}
					}
					if (!robotInTheWay) {
						if (dirtDifference > GameConstants.MAX_DIRT_DIFFERENCE) {
							if (!dig(dirToHQ.rotateRight().rotateRight())) {
								dig(dirToHQ.rotateLeft().rotateLeft());
							}
							depositDirt(dirToHQ);
						} else if (dirtDifference < -GameConstants.MAX_DIRT_DIFFERENCE) {
							dig(dirToHQ);
							if (!depositDirt(dirToHQ.rotateRight().rotateRight())) {
								depositDirt(dirToHQ.rotateLeft().rotateLeft());
							}
						} else {
							// Destroys building in the way
							if (!dig(dirToHQ.rotateRight().rotateRight())) {
								dig(dirToHQ.rotateLeft().rotateLeft());
							}
							depositDirt(dirToHQ);
						}
					} else {
						routeTo(landscaperData.getEnemyHQLocation());
					}
				}
			} else if(rc.getDirtCarrying() > 0) {
				depositDirt(rc.getLocation().directionTo(landscaperData.getEnemyHQLocation()));
			} else {
				determineDigDirection();
				dig(landscaperData.getEnemyHQBuryDigDirection());
			}
			
			return true;
		}
		
		RobotInfo enemyHQ = senseUnitType(RobotType.HQ, rc.getTeam().opponent());
		if(enemyHQ != null) {
			landscaperData.setEnemyHQLocation(enemyHQ.getLocation());
			return true;
		}
		
		return false;
	}
	
	private void determineDigDirection() {
		MapLocation rcLocation = rc.getLocation();
		
		Direction enemyHQDirection = rcLocation.directionTo(landscaperData.getEnemyHQLocation());
		
		if(rc.canDigDirt(enemyHQDirection.rotateLeft()) && !directionOnEnemyHq(enemyHQDirection.rotateLeft())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight()) && !directionOnEnemyHq(enemyHQDirection.rotateRight())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight());
		else if(rc.canDigDirt(enemyHQDirection.rotateLeft().rotateLeft()) && !directionOnEnemyHq(enemyHQDirection.rotateLeft().rotateLeft())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft().rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight().rotateRight()) && !directionOnEnemyHq(enemyHQDirection.rotateRight().rotateRight())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight().rotateRight());
		else landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection);
	}
	
	private boolean directionOnEnemyHq(Direction direction) {
		return rc.getLocation().add(direction).equals(landscaperData.getEnemyHQLocation());
	}

	private void buryEnemyDesign(RobotInfo enemy) throws GameActionException {
		if(rc.getDirtCarrying() > 0) depositDirt(rc.getLocation().directionTo(enemy.location));
		else dig(rc.getLocation().directionTo(landscaperData.getHqLocation()));
	}
	
}

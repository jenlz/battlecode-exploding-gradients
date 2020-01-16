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

	private static final int DIG_PATTERN_ARRAY_SHIFT = 2;
	private static Direction[][] movePattern = new Direction[][]{
		{Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH},
		{Direction.NORTH, Direction.NORTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH},
		{Direction.NORTH, Direction.WEST, Direction.WEST, Direction.WEST, Direction.WEST}
	};
	
	private static Direction[][] digPattern = new Direction[][]{
		{Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.EAST},
		{Direction.WEST, null, null, null, Direction.EAST},
		{Direction.WEST, null, null, null, Direction.EAST},
		{Direction.WEST, null, null, null, Direction.EAST},
		{Direction.WEST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH}
	};
	
	private static Direction[][][] buildPattern = new Direction[][][] {
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {}, {}, {}, {Direction.CENTER}},
		{{Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}},
	};
	
	private LandscaperData landscaperData;
	
	public Landscaper(RobotController rc) {
		super(rc);
		this.data = new LandscaperData(rc, getSpawnerLocation());
		this.landscaperData = (LandscaperData) this.data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
    	if(turnCount == 1) learnHQLocation();
		RobotInfo enemyDesign = senseUnitType(RobotType.DESIGN_SCHOOL, landscaperData.getOpponent());
    	
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
	
	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				landscaperData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}		
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
				
		int gridX = dx + DIG_PATTERN_ARRAY_SHIFT;
		int gridY = -dy + DIG_PATTERN_ARRAY_SHIFT;
		
		//If where we're going is too low, deposit dirt there.
		if(rc.senseElevation(rcLocation) - rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) > GameConstants.MAX_DIRT_DIFFERENCE) {
			depositDirt(movePattern[gridY][gridX]);
			return;
		}
		
		constructDirections = buildPattern[gridY][gridX];
		if(constructDirections.length == 0) {
			move(movePattern[gridY][gridX]);
			return;
		}
		
		int[] constructElevations = new int[constructDirections.length];
		for(int i = 0; i < constructElevations.length; i++) {
			constructElevations[i] = rc.senseElevation(rcLocation.add(constructDirections[i]));
		}
		
		depositDirt(constructDirections[NumberMath.indexOfLeast(constructElevations)]);
	}
	
	private void digWallDirt() throws GameActionException {
		Direction digDirection = null;
		
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = landscaperData.getHqLocation();
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
		
		int gridX = dx + DIG_PATTERN_ARRAY_SHIFT;
		int gridY = -dy + DIG_PATTERN_ARRAY_SHIFT;
		
		//If where we're going is too high, dig from there.
		if(rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) - rc.senseElevation(rcLocation) > GameConstants.MAX_DIRT_DIFFERENCE) {
			dig(movePattern[gridY][gridX]);
			return;
		}
		
		digDirection = digPattern[gridY][gridX];
		
		if(digDirection != null) dig(digDirection);
		move(movePattern[gridY][gridX]);
	}
	
	private boolean buryEnemyHQ() throws GameActionException {
		if(landscaperData.getEnemyHQLocation() != null) {
			Direction dirToHQ = rc.getLocation().directionTo(landscaperData.getEnemyHQLocation());
			if(!rc.getLocation().isAdjacentTo(landscaperData.getEnemyHQLocation())) {
				if (!move(dirToHQ)) {
					int dirtDifference = rc.senseElevation(rc.getLocation()) - rc.senseElevation(rc.adjacentLocation(dirToHQ));
					RobotInfo[] robots = rc.senseNearbyRobots();
					boolean robotInTheWay = false;
					for (RobotInfo robot : robots) {
						if (!robot.getType().isBuilding() && robot.getLocation() == rc.getLocation().add(dirToHQ)) {
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
			} else if(rc.getDirtCarrying() > 0) depositDirt(rc.getLocation().directionTo(landscaperData.getEnemyHQLocation()));
			else dig(landscaperData.getEnemyHQBuryDigDirection());
			
			return true;
		}
		
		RobotInfo enemyHQ = senseUnitType(RobotType.HQ, rc.getTeam().opponent());
		if(enemyHQ != null) {
			landscaperData.setEnemyHQLocation(enemyHQ.getLocation());
			determineDigDirection();
			return true;
		}
		
		return false;
	}
	
	private void determineDigDirection() {
		Direction enemyHQDirection = rc.getLocation().directionTo(landscaperData.getEnemyHQLocation());
		if(rc.canDigDirt(enemyHQDirection.rotateLeft())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight());
		else if(rc.canDigDirt(enemyHQDirection.rotateLeft().rotateLeft())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft().rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight().rotateRight())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight().rotateRight());
		else landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection);
	}

	private void buryEnemyDesign(RobotInfo enemy) throws GameActionException {
		if(rc.getDirtCarrying() > 0) depositDirt(rc.getLocation().directionTo(enemy.location));
		else dig(rc.getLocation().directionTo(landscaperData.getHqLocation()));
	}
	
}

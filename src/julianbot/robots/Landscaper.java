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
		{Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH},
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
    		landscaperData.initializeWallData(landscaperData.getHqLocation(), rc.getMapWidth(), rc.getMapHeight());
    		determineWallDirections();
    		determineHqElevation();
    	}
    	
    	seekEnemyHq();
		seekClosestEnemyBuilding();
    	
    	discernAttackRole();
    	    	
		System.out.println("Landscaper Role = " + landscaperData.getCurrentRole());
		
		if(landscaperData.getCurrentRole() == LandscaperData.ATTACK) {
	    	if(landscaperData.getEnemyHQLocation() != null) {
	    		System.out.println("Attempting burial of enemy HQ");
	    		buryEnemyHq();
	    	} else if(oughtTargetEnemyBuilding()) {
	    		buryEnemyBuilding();
	    	} 
		} else if(landscaperData.getCurrentRole() == LandscaperData.TRAVEL_TO_HQ) {
    		if(!approachComplete()) {
    			routeTo(landscaperData.getHqLocation());
    		} else {
    			landscaperData.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
    		}
    	} else if(landscaperData.getCurrentRole() == LandscaperData.DEFEND_HQ_FROM_FLOOD) {
    		if(seekAdjacentEnemyBuilding()) {
    			System.out.println("Burying enemy building");
    			buryEnemyBuilding();
    		} else {
    			System.out.println("Attempting to build HQ wall");
    			buildHQWall();
    		}
    	} else if(!isOnWall(rc.getLocation(), landscaperData.getHqLocation()) && !isWithinWall(rc.getLocation(), landscaperData.getHqLocation())) {
    		System.out.println("Ought to return to HQ.");
    		landscaperData.setCurrentRole(LandscaperData.TRAVEL_TO_HQ);
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
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(1, 1)});
			else if(topEdge) landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(1, -1)});
			else landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(1, 1)});
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(-1, 1)});
			else if(topEdge) landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(-1, -1)});
			else landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(-1, -1)});
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(1, -1)});
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(-1, 1)});
		} else {
			landscaperData.setLastResortBuildLocations(new MapLocation[] {hqLocation.translate(0, 1), hqLocation.translate(0, -1), hqLocation.translate(-1, -1)});
		}
	}
	
	private void determineHqElevation() throws GameActionException {
		if(!rc.canSenseLocation(landscaperData.getHqLocation())) return;
		landscaperData.setHqElevation(rc.senseElevation(landscaperData.getHqLocation()));
	}
	
	private void seekEnemyHq() {
		RobotInfo enemyHQ = senseUnitType(RobotType.HQ, rc.getTeam().opponent());
		if(enemyHQ != null) {
			landscaperData.setEnemyHQLocation(enemyHQ.getLocation());
		}
	}
	
	private boolean seekAdjacentEnemyBuilding() throws GameActionException {
		for(Direction direction : Robot.directions) {
			MapLocation searchLocation = rc.getLocation().add(direction);
			if(rc.canSenseLocation(searchLocation)) {
				RobotInfo building = rc.senseRobotAtLocation(searchLocation);
				if(building != null && building.getType().isBuilding() && rc.getTeam() != building.getTeam()) {
					landscaperData.setClosestEnemyBuilding(building);
					return true;
				}
			}
		}
		
		return false;
	}
	
	private RobotInfo seekClosestEnemyBuilding() {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if(enemies.length == 0) return null;
		
		int[] buildingDistances = new int[enemies.length];
		
		for(int i = 0; i < enemies.length; i++) {
			buildingDistances[i] = enemies[i].getType().isBuilding() ? rc.getLocation().distanceSquaredTo(enemies[i].getLocation()) : Integer.MAX_VALUE;
		}
		
		RobotInfo closestBuilding = enemies[NumberMath.indexOfLeast(buildingDistances)];
		landscaperData.setClosestEnemyBuilding(closestBuilding.getType().isBuilding() ? closestBuilding : null);
		return landscaperData.getClosestEnemyBuilding();
	}
	
	private boolean oughtTargetEnemyBuilding() throws GameActionException {
		RobotInfo closestEnemyBuilding = landscaperData.getClosestEnemyBuilding();
		if(closestEnemyBuilding == null) return false;
		
		if(floodingImminent()) return false;
		
		if(closestEnemyBuilding.getLocation().isWithinDistanceSquared(rc.getLocation(), 3)) return true;
		
		int buildingElevation = rc.senseElevation(closestEnemyBuilding.getLocation());
		
		//TODO: I did this for the sake of keeping landscapers from jumping off the wall. If I can find a better way of sensing wall locations, I'll switch this up.
		return rc.senseElevation(rc.getLocation()) - buildingElevation <= GameConstants.MAX_DIRT_DIFFERENCE;
	}
	
	private void buryEnemyBuilding() throws GameActionException {
		RobotInfo closestEnemyBuilding = landscaperData.getClosestEnemyBuilding();
		if(closestEnemyBuilding == null) return;
		
		if(!rc.getLocation().isWithinDistanceSquared(closestEnemyBuilding.getLocation(), 3)) {
			System.out.println("Too far from enemy building.");
			routeTo(closestEnemyBuilding.getLocation());
		} else {
			System.out.println("Adjacent to enemy building.");
			if(rc.getDirtCarrying() > 0) {
				depositDirt(rc.getLocation().directionTo(closestEnemyBuilding.getLocation()));
			} else {
				Direction digDirection = rc.getLocation().directionTo(closestEnemyBuilding.getLocation()).rotateLeft();
				for(int i = 0; i < 7; i++) {
					MapLocation digLocation = rc.getLocation().add(digDirection);
					boolean onWall = isOnWall(digLocation, landscaperData.getHqLocation());
					boolean withinWall = isWithinWall(digLocation, landscaperData.getHqLocation());
					
					System.out.println("On wall? " + onWall + " Within wall? " + withinWall);
					
					if(!onWall && !withinWall) {
						if(dig(digDirection)) break;
					} else {
						System.out.println("Dig failed. Rotating dig direction.");
						digDirection = digDirection.rotateLeft();
					}
				}
			}
		}
	}
	
	private boolean buryEnemyHq() throws GameActionException {
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
			determineEnemyHqBuryDigDirection();
			dig(landscaperData.getEnemyHQBuryDigDirection());
		}
		
		return true;		
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
		
		if(!isOnWall(rcLocation, hqLocation) && !isWithinWall(rcLocation, hqLocation)) {
			landscaperData.setCurrentRole(landscaperData.getClosestEnemyBuilding() != null ? LandscaperData.ATTACK : LandscaperData.TRAVEL_TO_HQ);
			return;
		}
		
		MapLocation nextLocation = rc.getLocation().add(movePattern[gridY][gridX]);
		boolean elevationDifferenceHigh = rc.onTheMap(nextLocation) ? rc.senseElevation(nextLocation) - rc.senseElevation(rcLocation) > GameConstants.MAX_DIRT_DIFFERENCE : true;

		if(isWithinWall(rcLocation, hqLocation) && elevationDifferenceHigh) {
			landscaperData.setCurrentRole(LandscaperData.ATTACK);
		} else if(isOnWall(rcLocation, hqLocation)) {
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
		if((rc.getDirtCarrying() > 0 && !landscaperData.isClearingObstruction()) || rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) constructWallUnits();
		else digWallDirt();
	}
	
	private void constructWallUnits() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = landscaperData.getHqLocation();
		
		if(landscaperData.isClearingObstruction()) {
			if(depositDirt(hqLocation.directionTo(rcLocation))) return;
			if(depositDirt(hqLocation.directionTo(rcLocation).rotateLeft())) return;
			if(depositDirt(hqLocation.directionTo(rcLocation).rotateRight())) return;
			return;
		}
		
		Direction[] constructDirections = new Direction[0];
		
		int dx = rcLocation.x - hqLocation.x;
		int dy = rcLocation.y - hqLocation.y;
				
		int gridX = dx + gridXShift;
		int gridY = -dy + gridYShift;
		
		if(gridX < 0 || gridX >= movePattern[0].length || gridY < 0 || gridY >= movePattern.length) return;
		
		//In the event that our wall reaches the end of the map, we just want to go back and forth along the wall.
		boolean nextLocationIrrelevant = !rc.onTheMap(rcLocation.add(movePattern[gridY][gridX])) || (onMapEdge(rcLocation) && onMapEdge(rcLocation.add(movePattern[gridY][gridX])));
		if(nextLocationIrrelevant || landscaperAtLocation(rcLocation.add(movePattern[gridY][gridX])) || enemyAtLocation(rcLocation.add(movePattern[gridY][gridX]))) {
			System.out.println("Toggling");
			toggleDirection();
		}
		
		//If where we're going is too low, deposit dirt there.
		if(rc.canSenseLocation(rcLocation.add(movePattern[gridY][gridX])) && rc.senseElevation(rcLocation) - rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) > GameConstants.MAX_DIRT_DIFFERENCE) {
			System.out.println(movePattern[gridY][gridX] + " is too low -- depositing dirt there.");
			depositDirt(movePattern[gridY][gridX]);
			return;
		}
		
		constructDirections = buildPattern[gridY][gridX];
		if(constructDirections.length == 0) {
			System.out.println("Nowhere hard-coded to build. Moving on.");
			Direction moveDirection = movePattern[gridY][gridX];
			
			if(rc.senseFlooding(rcLocation.add(moveDirection))) {
				System.out.println("OH, NO! The wall is flooded already?! Trying to save it...");
				depositDirt(moveDirection);
			} else {
				move(moveDirection);
			}
			
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
		
		MapLocation innerWallLocation = rcLocation.add(rcLocation.directionTo(hqLocation));
		MapLocation nextLocation = rcLocation.add(movePattern[gridY][gridX]);
		
		Direction constructDirection = constructDirections[NumberMath.indexOfLeast(constructElevations)];
		MapLocation constructLocation = rcLocation.add(constructDirection);
		System.out.println("The place to build is to the " + constructDirection);
		
		if(rc.senseFlooding(innerWallLocation)) depositDirt(rcLocation.directionTo(hqLocation));
		else if(rc.senseFlooding(nextLocation)) depositDirt(rcLocation.directionTo(nextLocation));
		else if(rc.senseElevation(rcLocation) - rc.senseElevation(constructLocation) < GameConstants.MAX_DIRT_DIFFERENCE) {
			System.out.println("Current elevation not too high! Depositing dirt to the " + constructDirection);
			depositDirt(constructDirection);
		}
		else move(movePattern[gridY][gridX]);
	}
	
	private boolean landscaperAtLocation(MapLocation location) throws GameActionException {
		if(rc.canSenseLocation(location)) {
			RobotInfo potentialLandscaper = rc.senseRobotAtLocation(location);
			if(potentialLandscaper == null) return false;
			return potentialLandscaper.getType() == RobotType.LANDSCAPER;
		}
		
		return false;
	}
	
	
	private boolean enemyAtLocation(MapLocation location) throws GameActionException {
		if(rc.canSenseLocation(location)) {
			RobotInfo potentialEnemy = rc.senseRobotAtLocation(location);
			if(potentialEnemy == null) return false;
			return potentialEnemy.getTeam() != rc.getTeam();
		}
		
		return false;
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
			digDirection = digPattern[gridY][gridX];
			
			if(!rc.onTheMap(rcLocation.add(digDirection))) {
				System.out.println("Dig direction is off the map. Turning around...");
				toggleDirection();
			} else {
				System.out.println("Digging to the " + digDirection);
				dig(digDirection);
				return;
			}
		}
		
		//TODO: Shouldn't this only return false if the occupant is a building?
		if(!rc.isLocationOccupied(rcLocation.add(rcLocation.directionTo(hqLocation))) && innerWallObstructed()) {
			System.out.println("Inner wall obstructed");
			//If we are next to an obstructed build site, dig from there.
			dig(rcLocation.directionTo(hqLocation));
			landscaperData.setClearingObstruction(true);
			return;
		} else {
			System.out.println("Inner wall clear");
			landscaperData.setClearingObstruction(false);
			
			MapLocation nextLocation = rcLocation.add(movePattern[gridY][gridX]);
			
			if(rc.canSenseLocation(nextLocation)) {
				if(!isOnWall(rcLocation, hqLocation) && rc.senseFlooding(nextLocation)) {
					System.out.println("The wall is already flooded! Trying to save it...");
					for(MapLocation location : landscaperData.getLastResortBuildLocations()) {
						if(rcLocation.isWithinDistanceSquared(location, 3) && dig(rcLocation.directionTo(location))) break; 
					}
				} else if(rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) - rc.senseElevation(rcLocation) > GameConstants.MAX_DIRT_DIFFERENCE) {
					//If where we're going is too high, dig from there.
					dig(movePattern[gridY][gridX]);
					return;
				}
			}
		}
		
		digDirection = digPattern[gridY][gridX];
		if(digDirection != null) {
			System.out.println("Non-null dig direction of " + digDirection);
			if(rc.onTheMap(rcLocation.add(digDirection))) {
				System.out.println("As hard-coded, digging to the " + digDirection);
				dig(digDirection);
			} else {
				System.out.println("Hard-coded, dig direction is obstructed, so trying a rotation.");
				
				for(Direction direction : Robot.directions) {
					MapLocation digLocation = rcLocation.add(direction);
					if(rc.onTheMap(digLocation) && !isOnWall(digLocation, hqLocation) && !isWithinWall(digLocation, hqLocation)) {
						dig(direction);
					}
				}
			}
		}
		
		if(!rc.senseFlooding(rcLocation.add(movePattern[gridY][gridX]))) {
			System.out.println("No flooding! Moving on...");
			move(movePattern[gridY][gridX]);
		}
	}
	
	private boolean innerWallObstructed() throws GameActionException {
		MapLocation innerWallLocation = rc.getLocation().add(rc.getLocation().directionTo(landscaperData.getHqLocation()));
		if(!rc.canSenseLocation(innerWallLocation)) return false;
		
		return rc.senseElevation(innerWallLocation) - landscaperData.getHqElevation() > GameConstants.MAX_DIRT_DIFFERENCE;
	}
	
	private void determineEnemyHqBuryDigDirection() {
		MapLocation rcLocation = rc.getLocation();
		
		Direction enemyHQDirection = rcLocation.directionTo(landscaperData.getEnemyHQLocation());
		
		waitUntilReady();
		
		if(rc.canDigDirt(enemyHQDirection.rotateLeft()) && !directionOnEnemyHq(enemyHQDirection.rotateLeft())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight()) && !directionOnEnemyHq(enemyHQDirection.rotateRight())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight());
		else if(rc.canDigDirt(enemyHQDirection.rotateLeft().rotateLeft()) && !directionOnEnemyHq(enemyHQDirection.rotateLeft().rotateLeft())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateLeft().rotateLeft());
		else if(rc.canDigDirt(enemyHQDirection.rotateRight().rotateRight()) && !directionOnEnemyHq(enemyHQDirection.rotateRight().rotateRight())) landscaperData.setEnemyHQBuryDigDirection(enemyHQDirection.rotateRight().rotateRight());
	}
	
	private boolean directionOnEnemyHq(Direction direction) {
		return rc.getLocation().add(direction).equals(landscaperData.getEnemyHQLocation());
	}
	
	private boolean floodingImminent() throws GameActionException {
		int wallElevation = rc.senseElevation(rc.getLocation());
		return getFloodingAtRound(rc.getRoundNum() + 100) >= wallElevation;
	}
}

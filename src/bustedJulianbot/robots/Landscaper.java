package bustedJulianbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import bustedJulianbot.robotdata.LandscaperData;
import bustedJulianbot.utils.NumberMath;

public class Landscaper extends Robot {

	private static final int DEFAULT_PATTERN_ARRAY_SHIFT = 2;
	private int gridXShift;
	private int gridYShift;
	
	private static Direction[][] clockwiseMovePattern = new Direction[][]{
		{Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST},
		{Direction.EAST, Direction.CENTER, Direction.CENTER, Direction.CENTER, Direction.WEST},
		{Direction.EAST, Direction.CENTER, Direction.CENTER, Direction.CENTER, Direction.WEST},
		{Direction.EAST, Direction.CENTER, Direction.CENTER, Direction.CENTER, Direction.WEST},
		{Direction.NORTHEAST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHWEST}
	};
	
	private static Direction[][] clockwiseDigPattern = new Direction[][]{
		{null, null, null, null, null},
		{null, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, null},
		{null, Direction.WEST, null, Direction.EAST, null},
		{null, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST, null},
		{null, null, null, null, null}
	};
	
	private static Direction[][][] clockwiseBuildPattern = new Direction[][][] {
		{{}, {}, {}, {}, {}},
		{{}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {}},
		{{}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {}},
		{{}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {}},
		{{}, {}, {}, {}, {}}
	};
	
	private static Direction[][] counterClockwiseMovePattern = new Direction[][]{
		{Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST},
		{Direction.EAST, Direction.CENTER, Direction.CENTER, Direction.CENTER, Direction.WEST},
		{Direction.EAST, Direction.CENTER, Direction.CENTER, Direction.CENTER, Direction.WEST},
		{Direction.EAST, Direction.CENTER, Direction.CENTER, Direction.CENTER, Direction.WEST},
		{Direction.NORTHEAST, Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTHWEST}
	};
	
	private static Direction[][] counterClockwiseDigPattern = new Direction[][]{
		{null, null, null, null, null},
		{null, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, null},
		{null, Direction.WEST, null, Direction.EAST, null},
		{null, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST, null},
		{null, null, null, null, null}
	};
	
	private static Direction[][][] counterClockwiseBuildPattern = new Direction[][][] {
		{{}, {}, {}, {}, {}},
		{{}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {}},
		{{}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {}},
		{{}, {Direction.CENTER}, {Direction.CENTER}, {Direction.CENTER}, {}},
		{{}, {}, {}, {}, {}}
	};
	
	private static Direction[][] outpostMovePattern = new Direction[][]{
		{null, null, null, null, null},
		{null, null, null, null, null},
		{null, null, Direction.CENTER, null, null},
		{null, null, null, null, null},
		{null, null, null, null, null}
	};
	
	private static Direction[][] outpostDigPattern = new Direction[][]{
		{null, null, null, null, null},
		{null, null, null, null, null},
		{null, null, Direction.NORTHWEST, null, null},
		{null, null, null, null, null},
		{null, null, null, null, null}
	};
	
	private static Direction[][][] outpostBuildPattern = new Direction[][][] {
		{{}, {}, {}, {}, {}},
		{{}, {}, {}, {}, {}},
		{{}, {}, {Direction.CENTER, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}, {}, {}},
		{{}, {}, {}, {}, {}},
		{{}, {}, {}, {}, {}}
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
    		updateRole();
    	}
    	
    	seekEnemyHq();
		seekClosestEnemyBuilding();
		
		readTransactions();
		
		System.out.println("Landscaper Role = " + landscaperData.getCurrentRole());
		
		if(landscaperData.getCurrentRole() == LandscaperData.OUTPOST_KEEPER) {
			movePattern = outpostMovePattern;
			digPattern = outpostDigPattern;
			buildPattern = outpostBuildPattern;
			
			if(landscaperData.isOutpostBuildStarted()) {
				buildWall(rc.getLocation(), false);
				return;
			}
			
			if(rc.getLocation().isWithinDistanceSquared(landscaperData.getHqLocation(), 36) || onMapEdge(rc.getLocation())) {
				continueSearch();
				return;
			}
			
			RobotInfo[] landscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
			for(RobotInfo landscaper : landscapers) {
				if(rc.getLocation().isWithinDistanceSquared(landscaper.getLocation(), 18)) {
					continueSearch();
					return;
				}
			}
			
			if(senseUnitType(RobotType.MINER, rc.getTeam(), 1) != null) {
				landscaperData.setOutpostBuildStarted(true);
				sendTransaction(1, Type.TRANSACTION_OUTPOST_AT_LOC, rc.getLocation());
			}
			
		} else if(landscaperData.getCurrentRole() == LandscaperData.ATTACK) {
	    	if(landscaperData.getEnemyHQLocation() != null) {
	    		System.out.println("Attempting burial of enemy HQ");
	    		buryEnemyHq();
	    	} else if(oughtTargetEnemyBuilding()) {
	    		buryEnemyBuilding();
	    	} else if(isWithinWall(rc.getLocation(), landscaperData.getHqLocation()) &&
	    			rc.getLocation().distanceSquaredTo(landscaperData.getFulfillmentCenterBuildSite()) < rc.getLocation().distanceSquaredTo(landscaperData.getDesignSchoolBuildSite())) {
	    		//The landscaper is likely standing where drones are supposed to be spawning.
	    		routeTo(landscaperData.getDesignSchoolBuildSite());
	    	}
	    	
	    	updateRole();
		} else if(landscaperData.getCurrentRole() == LandscaperData.TRAVEL_TO_HQ) {
    		routeTo(landscaperData.getHqLocation());
    		updateRole();
    	} else if(landscaperData.getCurrentRole() == LandscaperData.DEFEND_HQ_FROM_FLOOD) {
    		boolean onWall = isOnWall(rc.getLocation(), landscaperData.getHqLocation());
    		
    		if(seekAdjacentEnemyBuilding()) {
    			System.out.println("Burying enemy building");
    			buryEnemyBuilding();
    		} else if(onWall) {
    			System.out.println("Attempting to build HQ wall");
    			buildWall(landscaperData.getHqLocation(), true);
    			
    			if(!landscaperData.isWallBuildHandled()) {
    				if(landscapersFillWall(landscaperData.getHqLocation()) && wallBuilt(landscaperData.getHqLocation())) {
    					System.out.println("Landscapers fill the wall!");
    					sendTransaction(1, Robot.Type.TRANSACTION_WALL_BEING_BUILT, landscaperData.getHqLocation());
    				}
    			}
    		}
    	}
	}

	private void updateRole() {
		RobotInfo enemyBuilding = seekClosestEnemyBuilding();
		boolean enemiesNearby = enemyBuilding != null && 
				(enemyBuilding.getLocation().isWithinDistanceSquared(landscaperData.getHqLocation(), 18) || enemyBuilding.getType() == RobotType.HQ);
				
		if(enemiesNearby && !isOnWall(rc.getLocation(), landscaperData.getHqLocation())) landscaperData.setCurrentRole(LandscaperData.ATTACK);
		else if(landscaperData.isWallBuildHandled() && !isOnWall(rc.getLocation(), landscaperData.getHqLocation())) landscaperData.setCurrentRole(LandscaperData.OUTPOST_KEEPER);
		else if(approachComplete()) landscaperData.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
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
		} else if(digPattern == counterClockwiseDigPattern) {
			digPattern = clockwiseDigPattern;
			movePattern = clockwiseMovePattern;
			buildPattern = clockwiseBuildPattern;
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
		return rc.getLocation().isWithinDistanceSquared(landscaperData.getHqLocation(), 3);
	}
	
	private void buildWall(MapLocation center, boolean hqWall) throws GameActionException {
		if((rc.getDirtCarrying() > 0 && !landscaperData.isClearingObstruction()) || rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) constructWallUnits(center, hqWall);
		else digWallDirt(center);
	}
	
	private void constructWallUnits(MapLocation center, boolean hqWall) throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		
		if(landscaperData.isClearingObstruction()) {
			if(depositDirt(center.directionTo(rcLocation))) return;
			if(depositDirt(center.directionTo(rcLocation).rotateLeft())) return;
			if(depositDirt(center.directionTo(rcLocation).rotateRight())) return;
			return;
		}
		
		Direction[] constructDirections = new Direction[0];
		
		int dx = rcLocation.x - center.x;
		int dy = rcLocation.y - center.y;
				
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
			MapLocation constructLocation = rcLocation.add(constructDirections[i]);
			
			if (rc.canSenseLocation(constructLocation)) {
				boolean buildingPresent = false;
				
				//An outpost wall should not bury friendly buildings.
				if(!hqWall) {
					RobotInfo building = rc.senseRobotAtLocation(constructLocation);
					buildingPresent = building != null && building.getType().isBuilding() && building.getTeam() == rc.getTeam();
				}
				
				constructElevations[i] = buildingPresent ? Integer.MAX_VALUE : rc.senseElevation(rcLocation.add(constructDirections[i]));
				lowestElevation = (constructElevations[i] < lowestElevation) ? constructElevations[i] : lowestElevation;
			}
		}
		
		MapLocation innerWallLocation = rcLocation.add(rcLocation.directionTo(center));
		MapLocation nextLocation = rcLocation.add(movePattern[gridY][gridX]);
		
		Direction constructDirection = constructDirections[NumberMath.indexOfLeast(constructElevations)];
		MapLocation constructLocation = rcLocation.add(constructDirection);
		System.out.println("The place to build is to the " + constructDirection);
		
		if(rc.senseFlooding(innerWallLocation)) depositDirt(rcLocation.directionTo(center));
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
	
	private void digWallDirt(MapLocation center) throws GameActionException {
		Direction digDirection = null;
		
		MapLocation rcLocation = rc.getLocation();
		
		int dx = rcLocation.x - center.x;
		int dy = rcLocation.y - center.y;
		
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
		} else if(rc.senseElevation(rcLocation.add(movePattern[gridY][gridX])) - rc.senseElevation(rcLocation) > GameConstants.MAX_DIRT_DIFFERENCE) {
			//If where we're going is too high, dig from there.
			toggleDirection();
			return;
		}
		
		//TODO: Shouldn't this only return false if the occupant is a building?
		if(!rc.isLocationOccupied(rcLocation.add(rcLocation.directionTo(center))) && innerWallObstructed()) {
			System.out.println("Inner wall obstructed");
			//If we are next to an obstructed build site, dig from there.
			dig(rcLocation.directionTo(center));
			landscaperData.setClearingObstruction(true);
			return;
		} else {
			System.out.println("Inner wall clear");
			landscaperData.setClearingObstruction(false);
			
			MapLocation nextLocation = rcLocation.add(movePattern[gridY][gridX]);
			
			if(rc.canSenseLocation(nextLocation)) {
				if(!isOnWall(rcLocation, center) && rc.senseFlooding(nextLocation)) {
					System.out.println("The wall is already flooded! Trying to save it...");
					for(MapLocation location : landscaperData.getLastResortBuildLocations()) {
						if(rcLocation.isWithinDistanceSquared(location, 3) && dig(rcLocation.directionTo(location))) break; 
					}
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
					if(rc.onTheMap(digLocation) && !isOnWall(digLocation, center) && !isWithinWall(digLocation, center)) {
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
	
	 protected boolean landscapersFillWall(MapLocation center) throws GameActionException {
			MapLocation rcLocation = rc.getLocation();
	    	
	    	int minDx = data.getWallOffsetXMin();
	    	int maxDx = data.getWallOffsetXMax();
	    	int minDy = data.getWallOffsetYMin();
	    	int maxDy = data.getWallOffsetYMax();
	    	
	    	for(int dx = minDx; dx <= maxDx; dx++) {
	    		for(int dy = minDy; dy <= maxDy; dy++) {
	    			MapLocation wallLocation = center.translate(dx, dy);
	    			if(rc.canSenseLocation(wallLocation) && wallLocation.isWithinDistanceSquared(rcLocation, 3) && isOnWall(wallLocation, center)) {
	    				if(!rc.isLocationOccupied(wallLocation)) return false;
	    				
	    				RobotInfo robot = rc.senseRobotAtLocation(wallLocation);
	    				if(robot.getType() != RobotType.LANDSCAPER) return false;
	    			} else if(!rc.canSenseLocation(wallLocation)) {
	    				return false;
	    			}
	    		}
	    	}
	    	
	    	return true;
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

	//TRANSACTIONS
	private void readTransactions() throws GameActionException {
    	for(int i = NumberMath.clamp(landscaperData.getTransactionRound(), 1, Integer.MAX_VALUE); i < rc.getRoundNum(); i++) {
    		System.out.println("Reading transactions from round " + i);
    		
    		if(Clock.getBytecodesLeft() <= 500) break;
    		
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			
    			if (message.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
    				Robot.Type category = Robot.Type.enumOfValue(message[1]);
    				MapLocation loc = new MapLocation(message[2], message[3]);

    				if (category == null) {
    					System.out.println("Something is terribly wrong. enumOfValue returns null. Miner readTransaction line ~621");
    				}
    				
    				switch(category) {
    					case TRANSACTION_WALL_BEING_BUILT:
    						System.out.println("Wall build confirmed handled!");
    						landscaperData.setWallBuildHandled(true);
    						break;
    					default:
    						break;
    				}
    			}
    		}
    		
    		landscaperData.setTransactionRound(i + 1);
    	}
    }
}

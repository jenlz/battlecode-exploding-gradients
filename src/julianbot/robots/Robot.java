package julianbot.robots;

import battlecode.common.*;
import julianbot.robotdata.RobotData;
import julianbot.utils.pathfinder.Pathfinder;

import java.util.ArrayList;
import java.util.List;

public class Robot {
	
	protected RobotController rc;
	protected RobotData data;
	
	protected static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	protected int spawnRound;
	protected int turnCount;
	
	public enum Type{
		TRANSACTION_SOS_AT_LOC(-104),
		TRANSACTION_SOUP_AT_LOC(249),
		TRANSACTION_FRIENDLY_HQ_AT_LOC(9),
		TRANSACTION_ENEMY_HQ_AT_LOC(29),
		TRANSACTION_FRIENDLY_REFINERY_AT_LOC(-477),
		TRANSACTION_ENEMY_REFINERY_AT_LOC(-443),
		TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC(793), // Not sure if we'd ever need this, but putting it just in case
		TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC(740),
		TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC(117), // Not sure if we'd ever need this, but putting it just in case
		TRANSACTION_ENEMY_FULFILLMENT_CENTER_AT_LOC(177),
		TRANSACTION_ATTACK_AT_LOC(-171),
		TRANSACTION_KILL_ORDER(88),
		TRANSACTION_PAUSE_LANDSCAPER_BUILDING(8482),
		TRANSACTION_BUILD_SITE_BLOCKED(642),
		TRANSACTION_BLOCKED_BUILD_SITE_ADDRESSED(1533);
		
		private int val;
		
		Type(int val){
			this.val = val;
		}
		
		public int getVal() {
			return val;
		}
		
		public static Type enumOfValue(int value) {
		    for (Type e : values()) {
		        if (e.val==value) {
		            return e;
		        }
		    }
		    return null;
		}
		//Call Type.enumOfValue(plaintxt[1]) to get enum from value
	}
	
	public Robot(RobotController rc) {
		this.rc = rc;
		this.spawnRound = rc.getRoundNum();
	}
	
	public RobotController getRobotController() {
		return rc;
	}

	public void setRobotController(RobotController rc) {
		this.rc = rc;
	}

	public RobotData getRobotData() {
		return data;
	}

	public void setRobotData(RobotData data) {
		this.data = data;
	}
	
	public void run() throws GameActionException {
		sendPendingTransaction();
    	updateTurnCount();
	}

	public void updateTurnCount() {
		 turnCount = rc.getRoundNum() - spawnRound + 1;
	}
	
	//FLOODING
	protected int getFloodingAtRound(double roundNumber) {
		return (int) (Math.pow(Math.E, (0.0028 * roundNumber) - (1.38 * Math.sin(0.00157 * roundNumber - 1.73)) + (1.38 * Math.sin(-1.73))) - 1);
	}
	
	//BOUNDS
	public boolean onMapEdge(MapLocation location) {
		return location.x == 0 || location.x == rc.getMapWidth() - 1 || location.y == 0 || location.y == rc.getMapHeight() - 1;
	}
	
	//WALL
	public boolean isOnWall(MapLocation location, MapLocation hqLocation) {    	
    	int minDx = data.getWallOffsetXMin();
    	int maxDx = data.getWallOffsetXMax();
    	int minDy = data.getWallOffsetYMin();
    	int maxDy = data.getWallOffsetYMax();
    	
    	int dx = location.x - hqLocation.x;
    	int dy = location.y - hqLocation.y;
    	
    	boolean dxOnBound = (dx == minDx || dx == maxDx);
    	boolean dyInRange = minDy <= dy && dy <= maxDy;
    	if(dxOnBound && dyInRange) return true;
    	
    	
    	boolean dyOnBound = (dy == minDy || dy == maxDy);
    	boolean dxInRange = minDx <= dx && dx <= maxDx;
    	if(dyOnBound && dxInRange) return true;
    	
    	return false;
	}
	
	public boolean isWithinWall(MapLocation location, MapLocation hqLocation) {    	
    	int minDx = data.getWallOffsetXMin();
    	int maxDx = data.getWallOffsetXMax();
    	int minDy = data.getWallOffsetYMin();
    	int maxDy = data.getWallOffsetYMax();
    	
    	int dx = location.x - hqLocation.x;
    	int dy = location.y - hqLocation.y;
    	
    	boolean dxInRange = minDx < dx && dx < maxDx;
    	boolean dyInRange = minDy < dy && dy < maxDy;
    	return dxInRange && dyInRange;
	}
	
	public int getLowestWallElevation(MapLocation hqLocation) throws GameActionException {
		if(!rc.canSenseLocation(hqLocation)) return Integer.MAX_VALUE;
		int lowestElevation = Integer.MAX_VALUE;
		
    	int minDx = data.getWallOffsetXMin();
    	int maxDx = data.getWallOffsetXMax();
    	int minDy = data.getWallOffsetYMin();
    	int maxDy = data.getWallOffsetYMax();
    	
    	for(int dx = minDx; dx <= maxDx; dx++) {
    		for(int dy = minDy; dy <= maxDy; dy++) {
    			MapLocation location = hqLocation.translate(dx, dy);
    			if(rc.canSenseLocation(location) && !onMapEdge(location) && isOnWall(location, hqLocation)) {
    				int elevation = rc.senseElevation(location);
    				lowestElevation = elevation < lowestElevation ? elevation : lowestElevation;
    			}
    		}
    	}
    	
    	return lowestElevation;
	}
	
	/*
     * TODO: This function ignores tiles on the edge of the map, but there are map edges that should be part of the wall.
     * This is likely not a problem, as an estimate is sufficient for desired behavior, but this needs to be officially decided.
     */
    public boolean wallBuilt(MapLocation hqLocation) throws GameActionException {
    	if(!rc.canSenseLocation(hqLocation)) return false;
    	int hqElevation = rc.senseElevation(hqLocation);
    	
    	int minDx = data.getWallOffsetXMin();
    	int maxDx = data.getWallOffsetXMax();
    	int minDy = data.getWallOffsetYMin();
    	int maxDy = data.getWallOffsetYMax();
    	
    	for(int dx = minDx; dx <= maxDx; dx++) {
    		for(int dy = minDy; dy <= maxDy; dy++) {
    			MapLocation location = hqLocation.translate(dx, dy);
    			if(rc.canSenseLocation(location) && !onMapEdge(location) && isOnWall(location, hqLocation)) {
    				if(rc.senseElevation(location) - hqElevation <= GameConstants.MAX_DIRT_DIFFERENCE) return false;
    			}
    		}
    	}
    	
    	return true;
    }
    
    public boolean wallBarringFloodwaters(MapLocation hqLocation) throws GameActionException {
    	if(!rc.canSenseLocation(hqLocation)) return false;
    	
    	int minDx = data.getWallOffsetXMin();
    	int maxDx = data.getWallOffsetXMax();
    	int minDy = data.getWallOffsetYMin();
    	int maxDy = data.getWallOffsetYMax();
    	
    	for(int dx = minDx -1; dx <= maxDx + 1; dx++) {
    		for(int dy = minDy - 1; dy <= maxDy + 1; dy++) {
    			MapLocation location = hqLocation.translate(dx, dy);
    			if(rc.canSenseLocation(location) && !isOnWall(location, hqLocation) && !isWithinWall(location, hqLocation)) {
    				if(!rc.senseFlooding(location)) return false;
    			}
    		}
    	}
    	
    	return true;
    }
	
	//RECONNAISSANCE
	protected MapLocation getSpawnerLocation() {
		RobotInfo[] robots = rc.senseNearbyRobots(3, rc.getTeam());
		RobotType targetType = getSpawnerTypeFor(rc.getType());
		
		for(RobotInfo robot : robots) {
			if(robot.type == targetType) return robot.getLocation();
		}
		
		return null;
	}
	
	protected RobotType getSpawnerTypeFor(RobotType type) {
		if(type == RobotType.MINER) return RobotType.HQ;
		if(type == RobotType.REFINERY) return RobotType.MINER;
		if(type == RobotType.LANDSCAPER) return RobotType.DESIGN_SCHOOL;
		if(type == RobotType.DELIVERY_DRONE) return RobotType.FULFILLMENT_CENTER;
		return RobotType.HQ;
	}
	
	/**
	 * Senses unit type within sensor radius.
	 * @param rc
	 * @param type
	 * @return First unit of given type. Null if not found
	 */
	protected RobotInfo senseUnitType(RobotType type) {
		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * Senses number of units matching given criteria within sensor radius
	 * @param rc
	 * @param type
	 * @return Number of units of given type
	 */
	protected int senseNumberOfUnits(RobotType type) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}

	/**
	 * Senses unit type within sensor radius.
	 * @param rc
	 * @param type
	 * @param team
	 * @return First unit of given type and team. Null if not found
	 */
	protected RobotInfo senseUnitType(RobotType type, Team team) {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	protected RobotInfo[] senseAllUnitsOfType(RobotType type, Team team) {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		List<RobotInfo> matchingRobots = new ArrayList<>();
		
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				matchingRobots.add(robot);
			}
		}
		
		RobotInfo[] matchingRobotsArray = new RobotInfo[matchingRobots.size()];
		for(int i = 0; i < matchingRobots.size(); i++) {
			matchingRobotsArray[i] = matchingRobots.get(i);
		}
		
		return matchingRobotsArray;
	}
	
	/**
	 * Senses number of units matching given criteria within sensor radius
	 * @param rc
	 * @param type
	 * @param team
	 * @return Number of units of given type and team
	 */
	protected int senseNumberOfUnits(RobotType type, Team team) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots(-1, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}

	/**
	 * Senses unit type within inputted radius
	 * @param type
	 * @param team
	 * @param radiusSquared
	 * @return First unit of given type and team. Null if not found.
	 */
	protected RobotInfo senseUnitType(RobotType type, Team team, int radiusSquared) {
		RobotInfo[] robots = rc.senseNearbyRobots(radiusSquared, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				return robot;
			}
		}
		return null;
	}
	
	/**
	 * Senses number of units matching given criteria within inputted radius
	 * @param type
	 * @param team
	 * @param radiusSquared
	 * @return Number of units of given type and team
	 */
	protected int senseNumberOfUnits(RobotType type, Team team, int radiusSquared) {
		int unitCount = 0;
		
		RobotInfo[] robots = rc.senseNearbyRobots(radiusSquared, team);
		for (RobotInfo robot : robots) {
			if (robot.getType() == type) {
				unitCount++;
			}
		}
		
		return unitCount;
	}
	
	//TURN MANAGEMENT
	protected void waitUntilReady() {
		while(!rc.isReady()) {
			Clock.yield();
		}
	}
	
	//MOVEMENT
	protected boolean move(Direction dir) throws GameActionException {
		stopFollowingPath();
		MapLocation targetLocation = rc.getLocation().add(dir);
		
		waitUntilReady();
		
		if(rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(targetLocation)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	protected boolean moveOnPath(Direction dir) throws GameActionException {
		MapLocation targetLocation = rc.getLocation().add(dir);
		
		waitUntilReady();
		
		if(rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(targetLocation)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	protected boolean moveAnywhere() throws GameActionException {
		stopFollowingPath();
		
		Direction dir = directions[(int)(Math.random() * directions.length)];
		int rotateLimit = 8;
		
		waitUntilReady();
		
		while (!move(dir) && rotateLimit > 0) {
			dir.rotateRight();
			rotateLimit--;
		}
		
		if(rotateLimit > 0) data.setPreviousLocation(rc.getLocation().add(dir.opposite()));
		return rotateLimit > 0;
	}

	/**
	 * Moves in same direction as before, otherwise moves in random direction
	 * @throws GameActionException
	 */
	public void continueSearch() throws GameActionException {
		//The move function is deliberately unused here.
		waitUntilReady();

		if (data.getSearchDirection() == null) {
			data.setSearchDirection(data.getSpawnerLocation().directionTo(rc.getLocation()));
		}

		if(rc.canMove(data.getSearchDirection()) && !rc.senseFlooding(rc.getLocation().add(data.getSearchDirection()))) {
			rc.move(data.getSearchDirection());
			return;
		}

		data.setSearchDirection(directions[(int) (Math.random() * directions.length)]);
	}

	/**
	 * Moves in search direction, returns false if unable to. Used in bugNav
	 * @return
	 * @throws GameActionException
	 */
	public boolean continueSearchNonRandom() throws GameActionException {
		//The move function is deliberately unused here.
		waitUntilReady();

		if(rc.canMove(data.getSearchDirection()) && !rc.senseFlooding(rc.getLocation().add(data.getSearchDirection()))) {
			rc.move(data.getSearchDirection());
			return true;
		}
		return false;
	}

	//TRANSACTIONS
	protected boolean sendTransaction(int soupBid, Type type, MapLocation loc) throws GameActionException {		
		return sendTransaction(soupBid, type, loc, 0);
	}
	
	protected boolean sendTransaction(int soupBid, Type type, MapLocation loc, int bonusInt) throws GameActionException {		
		int transactionTag = (int) (Math.random()*500); //This use of parentheses will prevent truncation of the random number.
		int[] message = new int[]{transactionTag, type.getVal()+transactionTag, loc.x+transactionTag, loc.y+transactionTag, rc.getRoundNum()+transactionTag, bonusInt+transactionTag, 0};
		int odd = 0;
		for (int i : message) {
			if (i%2 == 1)
				odd++;
		}
		message[6] = odd;
		
		if(rc.canSubmitTransaction(message, soupBid)) {
			rc.submitTransaction(message, soupBid);
			return true;
		} else {
			data.setPendingTransaction(type, loc, soupBid);
		}
		
		return false;
	}

	/**
	 * Decodes transaction. Returns empty array if message is from enemy team.
	 * @param transaction
	 * @return
	 * @throws GameActionException
	 */
	protected int[] decodeTransaction(Transaction transaction) throws GameActionException {		
		int[] message = transaction.getMessage();
		int transactionTag = message[0];
		int[] plaintxt = new int[7];
		
		int odd = 0;
		for (int i = 0; i<message.length-1; i++) {
			if (message[i]%2 == 1)
				odd++;
		}
		
		if (odd!=message[6]) return new int[] {0}; //empty means message not from own team
		for (int i = 0; i<message.length-1; i++) {
			plaintxt[i] = message[i] - transactionTag;
		}
		if (Type.enumOfValue(plaintxt[1]) == null) return new int[] {0}; //Checks if matches one of categories.
		return plaintxt;
	}

	/**
	 * Returns the corresponding Transaction type of locating inputted RobotType
	 * @param rc
	 * @param unitType
	 * @param unitTeam
	 * @return
	 */
	protected static Type getLocationType(RobotController rc, RobotType unitType, Team unitTeam) {
		if (unitTeam == rc.getTeam()) {
			switch (unitType) {
				case HQ:
					return Type.TRANSACTION_FRIENDLY_HQ_AT_LOC;
				case REFINERY:
					return Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC;
				case DESIGN_SCHOOL:
					return Type.TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC;
				case FULFILLMENT_CENTER:
					return Type.TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC;
				default:
					return null;
			}
		} else {
			switch (unitType) {
				case HQ:
					return Type.TRANSACTION_ENEMY_HQ_AT_LOC;
				case REFINERY:
					return Type.TRANSACTION_ENEMY_REFINERY_AT_LOC;
				case DESIGN_SCHOOL:
					return Type.TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC;
				case FULFILLMENT_CENTER:
					return Type.TRANSACTION_ENEMY_FULFILLMENT_CENTER_AT_LOC;
				default:
					return null;
			}
		}
	}
	
	public void sendPendingTransaction() throws GameActionException {
		if(!data.hasPendingTransaction()) return;
		
		if(sendTransaction(data.getPendingTransactionSoupBid(), data.getPendingTransactionType(), data.getPendingTransactionLocation())) {
			System.out.println("Submitted pending transaction!");
			data.clearPendingTransaction();
		}
	}
	
	//PATHFINDING
	protected boolean routeTo(MapLocation destination) throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), destination, 0, 0, 255);
		//If we're already pathfinding, continue on.	
		if(!data.getCurrentDestination().equals(destination)) {
			System.out.println("Change in destination.");
			data.setBugNaving(false);
			data.setObstacleLoc(null);
			data.setClosestDist(-1);
			data.setPath(null);
		}
		
		if(data.isBugNaving()) {
			System.out.println("Continuing bug nav.");
			boolean bugNavingSuccessful = bugNav(destination);
			if(!bugNavingSuccessful) {
				data.setBugNaving(false);
				System.out.println("Bug nav failure.");
			}
			return bugNavingSuccessful;
		} else if(data.hasPath()) {
			boolean pathfindingSuccessful = pathfind(destination);
			if(!pathfindingSuccessful) data.setPath(null);
			return pathfindingSuccessful;
		}

		//Otherwise, simply try to move directly towards the destination.
		MapLocation rcLocation = rc.getLocation();
		
		Direction initialDirection = rc.getLocation().directionTo(destination);
		if(move(initialDirection)) return true;
		
		//If this isn't possible, try to move around whatever is blocking us.
		//Directions closer to the destination will be favored.
		Direction initialDirectionLeft = initialDirection.rotateLeft();
		Direction initialDirectionRight = initialDirection.rotateRight();
		
		Direction[] nextDirections = new Direction[2];
		
		if(rcLocation.add(initialDirectionLeft).distanceSquaredTo(destination) < rcLocation.add(initialDirectionRight).distanceSquaredTo(destination)) {
			nextDirections[0] = initialDirectionLeft;
			nextDirections[1] = initialDirectionRight;
		} else {
			nextDirections[0] = initialDirectionRight;
			nextDirections[1] = initialDirectionLeft;
		}
		
		for(Direction direction : nextDirections) {
			if(rcLocation.add(direction).equals(data.getPreviousLocation())) continue;
			if(move(direction)) return true;
		}
		
		//If all of these measures have failed, we'll need to use pathfinding to get around.
		//However, just in case, we will allow for the previous location to be used next turn.
		data.setPreviousLocation(rcLocation);
		System.out.println("Resorting to intensive pathfinding.");
		/*if (rc.canSenseLocation(destination) && pathfind(destination)) {
			System.out.println("BFS succeeded.");
			data.setCurrentDestination(destination);
			return true;
		} else */if (bugNav(destination)) {
			System.out.println("Bug Nav succeeded.");
			data.setBugNaving(true);
			return true;
		}
		System.out.println("Intensive pathfinding failed.");
		
		return false;
	}
	
	protected boolean bfsRouteTo(MapLocation destination) throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), destination, 0, 0, 255);
		//If we're already pathfinding, continue on.
		if(data.hasPath()) {
			boolean pathfindingSuccessful = pathfind(destination);
			if(!pathfindingSuccessful) data.setPath(null);
			return pathfindingSuccessful;
		}

		//Otherwise, simply try to move directly towards the destination.
		MapLocation rcLocation = rc.getLocation();
		
		Direction initialDirection = rc.getLocation().directionTo(destination);
		if(move(initialDirection)) return true;
		
		//If this isn't possible, try to move around whatever is blocking us.
		//Directions closer to the destination will be favored.
		Direction initialDirectionLeft = initialDirection.rotateLeft();
		Direction initialDirectionRight = initialDirection.rotateRight();
		
		Direction[] nextDirections = new Direction[4];
		
		if(rcLocation.add(initialDirectionLeft).distanceSquaredTo(destination) < rcLocation.add(initialDirectionRight).distanceSquaredTo(destination)) {
			nextDirections[0] = initialDirectionLeft;
			nextDirections[1] = initialDirectionRight;
			nextDirections[2] = initialDirectionLeft.rotateLeft();
			nextDirections[3] = initialDirectionRight.rotateRight();
		} else {
			nextDirections[0] = initialDirectionRight;
			nextDirections[1] = initialDirectionLeft;
			nextDirections[2] = initialDirectionRight.rotateRight();
			nextDirections[3] = initialDirectionLeft.rotateLeft();
		}
		
		for(Direction direction : nextDirections) {
			if(rcLocation.add(direction).equals(data.getPreviousLocation())) continue;
			if(move(direction)) return true;
		}
		
		data.setPreviousLocation(rcLocation);
		System.out.println("Resorting to intensive pathfinding.");
		if (pathfind(destination)) {
			System.out.println("BFS succeeded.");
			data.setCurrentDestination(destination);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Tries to move closer to destination. If it can't follows wall until it can move closer.
	 * @param destination
	 * @return
	 */
	public boolean bugNav(MapLocation destination) throws GameActionException {

		if (!data.getCurrentDestination().equals(destination)) {
			data.setCurrentDestination(destination);
			data.setClosestDist(-1);
		}

		if (data.getClosestDist() == -1) {
			System.out.println("Initializing closestDist");
			data.setClosestDist(rc.getLocation().distanceSquaredTo(destination));
		}
		
		if (rc.getLocation().equals(destination)) {
			// After robot moves, checks if it is now at its destination
			System.out.println("Reached destination");
			data.setClosestDist(-1);
			return true;
		}

		System.out.println("Closest Dist = " + data.getClosestDist());
		
		Direction dirToDest = rc.getLocation().directionTo(destination);
		Direction dirToDestLeft = dirToDest.rotateLeft();
		Direction dirToDestRight = dirToDest.rotateRight();
		rc.setIndicatorDot(rc.getLocation().add(dirToDest), 255, 182, 193); // Pink dot
		if (rc.getLocation().add(dirToDest).distanceSquaredTo(destination) < data.getClosestDist()) {
			// If the next move toward the destination is closer than the closest its been
			System.out.println("Raw bug nav move -- DIRECT");
			return bugNavMove(destination, dirToDest);
		} else if (rc.getLocation().add(dirToDestLeft).distanceSquaredTo(destination) < data.getClosestDist()) {
			System.out.println("Raw bug nav move -- LEFT");
			return bugNavMove(destination, dirToDestLeft);
		} else if (rc.getLocation().add(dirToDestRight).distanceSquaredTo(destination) < data.getClosestDist()) {
			System.out.println("Raw bug nav move -- RIGHT");
			return bugNavMove(destination, dirToDestRight);
		} else {
			return followWall(dirToDest, destination);
		}
	}

	/**
	 * Tries to move and checks if adjacent to destination but cant move to dest
	 * @param destination
	 * @param dir
	 * @return
	 * @throws GameActionException
	 */
	public boolean bugNavMove(MapLocation destination, Direction dir) throws GameActionException {
		System.out.println("Attempting bug nav move to the " + dir);
		
		if (move(dir)) {
			//If you can move in that direction
			data.setClosestDist(rc.getLocation().distanceSquaredTo(destination));
			data.setObstacleLoc(null);
			System.out.println("Moved to new closest location. Dist: " + data.getClosestDist());
			return true;
		} else {
			return followWall(dir, destination);
		}
	}

	/**
	 * Decides whether to follow left or right wall
	 * @param dirToDest
	 * @param destination
	 * @return
	 */
	public boolean followWall(Direction dirToDest, MapLocation destination) throws GameActionException {
		System.out.println("Simulating following wall");
		// Left Wall Initialization
		MapLocation simulatedLocLeft = rc.getLocation();
		int closestDistLeft = data.getClosestDist();

		Direction simulatedSearchDirection = data.getSearchDirection();
		MapLocation simulatedObstacleLoc = data.getObstacleLoc();

		// Simulates 5 steps ahead for left wall
		for (int i = 0; i < 5; i++) {
			if (simulatedObstacleLoc == null) {
				simulatedSearchDirection = dirToDest;
			} else {
				simulatedSearchDirection = simulatedLocLeft.directionTo(simulatedObstacleLoc);
			}
			// Rotates search Direction 8 times at max
			for (int j = 0; j < 8; j++) {
				if (simulateCanMove(simulatedLocLeft, simulatedSearchDirection)) {
					simulatedLocLeft = simulatedLocLeft.add(simulatedSearchDirection);
					int dist = simulatedLocLeft.distanceSquaredTo(destination);
					if (dist < closestDistLeft) {closestDistLeft = dist;}
					break;
				} else {
					if (simulatedSearchDirection == Direction.CENTER) {
						simulatedSearchDirection = dirToDest;
					}
					simulatedObstacleLoc = simulatedLocLeft.add(simulatedSearchDirection);
					simulatedSearchDirection = simulatedSearchDirection.rotateRight();
				}
			}
		}

		// Right Wall Initialization
		MapLocation simulatedLocRight = rc.getLocation();
		int closestDistRight = data.getClosestDist();

		// Resetting variables for right wall
		simulatedSearchDirection = data.getSearchDirection();
		simulatedObstacleLoc = data.getObstacleLoc();

		// Simulates 5 steps ahead for right wall
		for (int i = 0; i < 5; i++) {
			if (simulatedObstacleLoc == null) {
				simulatedSearchDirection = dirToDest;
			} else {
				simulatedSearchDirection = simulatedLocRight.directionTo(simulatedObstacleLoc);
			}
			// Rotates search Direction 8 times at max
			for (int j = 0; j < 8; j++) {
				if (simulateCanMove(simulatedLocRight, simulatedSearchDirection)) {
					simulatedLocRight = simulatedLocRight.add(simulatedSearchDirection);
					int dist = simulatedLocRight.distanceSquaredTo(destination);
					if (dist < closestDistRight) {closestDistRight = dist;}
					break;
				} else {
					if (simulatedSearchDirection == Direction.CENTER) {
						simulatedSearchDirection = dirToDest;
					}
					simulatedObstacleLoc = simulatedLocRight.add(simulatedSearchDirection);
					simulatedSearchDirection = simulatedSearchDirection.rotateLeft();
				}
			}
		}

		System.out.println("Closest dist left: " + closestDistLeft + " Closest dist right: " + closestDistRight + " closest dist: " + data.getClosestDist());
		rc.setIndicatorDot(simulatedLocLeft, 204, 204, 0); //Puke yellow - Left
		rc.setIndicatorDot(simulatedLocRight, 153, 0, 153); //Purple - Right
		if (closestDistLeft <= closestDistRight) {
			return followLeftWall(dirToDest, destination);
		} else {
			return followRightWall(dirToDest, destination);
		}

	}

	/**
	 * Attempts to move in same direction as last turn, otherwise rotates right
	 * @throws GameActionException
	 */
	public boolean followLeftWall(Direction dirToDest, MapLocation destination) throws GameActionException {
		System.out.println("Can't move in closer direction. Resorting to left wall hugging.");
		if (data.getObstacleLoc() == null) {
			data.setSearchDirection(dirToDest);
		} else {
			data.setSearchDirection(rc.getLocation().directionTo(data.getObstacleLoc()));
		}
		
		rc.setIndicatorLine(rc.getLocation(), rc.adjacentLocation(data.getSearchDirection()), 0, 0, 255);
		
		boolean successfulWallFollow = false;
		// Follows wall on left side
		for (int i = 0; i < 8; i++) {
			if (continueSearchNonRandom()) {
				System.out.println("Searched in direction " + data.getSearchDirection());
				int distance = rc.getLocation().distanceSquaredTo(destination);
				System.out.println("Distance = " + distance);
				if(distance < data.getClosestDist()) data.setClosestDist(distance);
				System.out.println("Closest Distance = " + data.getClosestDist());
				successfulWallFollow = true;
				break;
			} else {
				if (data.getSearchDirection() == Direction.CENTER) {
					data.setSearchDirection(dirToDest);
				}
				data.setObstacleLoc(rc.getLocation().add(data.getSearchDirection()));
				data.setSearchDirection(data.getSearchDirection().rotateRight());
				System.out.println("Can't move, setting obstacle at " + data.getObstacleLoc() + " Loc: " + rc.getLocation());
				rc.setIndicatorDot(data.getObstacleLoc(), 0, 0, 0);
			}
		}
		rc.setIndicatorLine(rc.getLocation().subtract(data.getSearchDirection()), rc.getLocation(), 102, 255, 255); //Teal line
		return successfulWallFollow;
	}

	/**
	 * Attempts to move in same direction as last turn, otherwise rotates left
	 * @throws GameActionException
	 */
	public boolean followRightWall(Direction dirToDest, MapLocation destination) throws GameActionException {
		System.out.println("Can't move in closer direction. Resorting to right wall hugging.");
		if (data.getObstacleLoc() == null) {
			data.setSearchDirection(dirToDest);
		} else {
			data.setSearchDirection(rc.getLocation().directionTo(data.getObstacleLoc()));
		}

		rc.setIndicatorLine(rc.getLocation(), rc.adjacentLocation(data.getSearchDirection()), 0, 0, 255);

		boolean successfulWallFollow = false;
		// Follows wall on left side
		for (int i = 0; i < 8; i++) {
			if (continueSearchNonRandom()) {
				System.out.println("Searched in direction " + data.getSearchDirection());
				int distance = rc.getLocation().distanceSquaredTo(destination);
				System.out.println("Distance = " + distance);
				if(distance < data.getClosestDist()) data.setClosestDist(distance);
				System.out.println("Closest Distance = " + data.getClosestDist());
				successfulWallFollow = true;
				break;
			} else {
				if (data.getSearchDirection() == Direction.CENTER) {
					data.setSearchDirection(dirToDest);
				}
				data.setObstacleLoc(rc.getLocation().add(data.getSearchDirection()));
				data.setSearchDirection(data.getSearchDirection().rotateLeft());
				System.out.println("Can't move, setting obstacle at " + data.getObstacleLoc() + " Loc: " + rc.getLocation());
				rc.setIndicatorDot(data.getObstacleLoc(), 0, 0, 0);
			}
		}
		rc.setIndicatorLine(rc.getLocation().subtract(data.getSearchDirection()), rc.getLocation(), 102, 255, 255); //Teal line
		return successfulWallFollow;
	}

	/**
	 * Used with bugNav to test whether the robot could move in direction if it was hypothetically in a certain location. Ignores isReady condition of normal canMove. Treats non-buildings as not obstacles
	 * @return
	 */
	public boolean simulateCanMove(MapLocation currentLoc, Direction dirToMove) throws GameActionException {
		MapLocation locToMove = currentLoc.add(dirToMove);
		if (rc.getType().isBuilding() || !rc.onTheMap(locToMove)) {
			// If unit is building or location moving to isn't on the map
			return false;
		} else if (rc.canSenseLocation(locToMove)) {
			RobotInfo robot = rc.senseRobotAtLocation(locToMove);
			if (robot != null && robot.getType().isBuilding()) {
				// If the location is occupied by a building
				return false;
			} else if (rc.canSenseLocation(currentLoc)) {
				// If locToMove is not flooded and the elevation difference is not too great
				int elevationDiff = rc.senseElevation(locToMove) - rc.senseElevation(currentLoc);
				if (rc.senseFlooding(locToMove) || Math.abs(elevationDiff) > GameConstants.MAX_DIRT_DIFFERENCE) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 *
	 * @param destination
	 * @param rc
	 * @param data
	 * @return
	 * @throws GameActionException
	 */
	protected boolean pathfind(MapLocation destination) throws GameActionException {
		if(!data.hasPath() && destination != null) {
			calculatePathTo(destination);
			return data.hasPath();
		}
		
		return proceedAlongPath();
	}
	
	protected void calculatePathTo(MapLocation destination) throws GameActionException {
		data.setMapGraph(Pathfinder.buildMapGraph(rc, rc.getCurrentSensorRadiusSquared()));
		data.setPath(Pathfinder.getRoute(rc.getLocation(), destination, data.getMapGraph()));
		data.setPathProgression(0);
	}
	
	protected boolean proceedAlongPath() throws GameActionException {
		if(data.hasPath() && moveOnPath(data.getNextPathDirection())) {
			data.incrementPathProgression();
			
			if(data.pathCompleted()) {
				stopFollowingPath();
			}
			
			return true;
		}
		
		stopFollowingPath();
		return false;
	}
	
	protected void stopFollowingPath() {
		data.setPath(null);
		data.setPathProgression(0);
	}

	static Direction randomDirection() {
		return Direction.allDirections()[(int) (Math.random() * Direction.allDirections().length)];
	}

	/**
	 * Returns closest location of array to target location
	 * @param rc
	 * @param locs
	 * @param targetLoc
	 * @return
	 */
	protected MapLocation locateClosestLocation(ArrayList<MapLocation> locs, MapLocation targetLoc) {
		MapLocation closestLoc = null;
		int closestDistance = Integer.MAX_VALUE;
		for (MapLocation loc : locs) {
			int dist = loc.distanceSquaredTo(targetLoc);
			if (dist < closestDistance) {
				closestLoc = loc;
				closestDistance = dist;
			}
		}
		return closestLoc;
	}

}

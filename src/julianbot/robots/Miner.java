package julianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.MinerData;

public class Miner extends Robot {

	private static final int SENSOR_RADIUS = (int) Math.ceil(Math.sqrt(RobotType.MINER.sensorRadiusSquared));
	
	private MinerData minerData;
	
	public Miner(RobotController rc) {
		super(rc);
		this.data = new MinerData(rc, getSpawnerLocation());
		this.minerData = (MinerData) this.data;
	}

	@Override
	public void run() throws GameActionException {
    	if(turnCount == 1) discernRole();

    	//TODO: We can split this up over multiple rounds to avoid reading transactions past initial cooldown turns or finishing early, then starting again on round 9, only to finish after initial cooldown.
    	if(turnCount < GameConstants.INITIAL_COOLDOWN_TURNS) {
    		for (int i = 1; i < rc.getRoundNum(); i++) {
				readTransaction(rc.getBlock(i));
			}
		}
    	
		readTransaction(rc.getBlock(rc.getRoundNum() - 1));
      
		switch(minerData.getCurrentRole()) {
			case MinerData.ROLE_DESIGN_BUILDER:
				designMinerProtocol();
				break;
			case MinerData.ROLE_FULFILLMENT_BUILDER:
				fulfillmentMinerProtocol();
				break;
			case MinerData.ROLE_REFINERY_BUILDER:
				refineryMinerProtocol();
				break;
			case MinerData.ROLE_SOUP_MINER:
				if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
					fullMinerProtocol();
				} else {
					emptyMinerProtocol();
				}
				break;
			case MinerData.ROLE_DEFENSE_BUILDER:
				defenseMinerProtocol();
				break;
			case MinerData.ROLE_SCOUT:
				scoutMinerProtocol();
				break;
			default:
				break;
		}
	}
	
	/**
	 * Builds a design school and then switches to a soup miner
	 * @throws GameActionException
	 */
	private void designMinerProtocol() throws GameActionException {
    	MapLocation designSchoolBuildSite = minerData.getSpawnerLocation().translate(-1, 0);
    	
    	RobotInfo designSchool = senseUnitType(RobotType.DESIGN_SCHOOL, rc.getTeam());
    	
    	if(designSchool != null) {
    		System.out.println("\tDesign School already exists.");
    		RobotInfo fulfillmentCenter = senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam());
    		minerData.setCurrentRole((fulfillmentCenter != null) ? MinerData.ROLE_SOUP_MINER : MinerData.ROLE_FULFILLMENT_BUILDER);
    		minerData.setDesignSchoolBuilt(true);
    		return;
    	} else if(rc.getLocation().equals(designSchoolBuildSite)) {
    		//Move off of design school build site.
    		System.out.println("\tMoving off of DS site.");
    		moveAnywhere();
    		return;
    	} else if(rc.getLocation().isWithinDistanceSquared(designSchoolBuildSite, 3)) {
    		System.out.println("\tAttempting to build DS.");
    		if(attemptDesignSchoolConstruction(rc.getLocation().directionTo(designSchoolBuildSite))) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else {
    		routeTo(designSchoolBuildSite);
    		return;
    	}
    }
    
    private void fulfillmentMinerProtocol() throws GameActionException {
    	MapLocation fulfillmentCenterBuildSite = minerData.getSpawnerLocation().translate(1, 0);
    	   
    	RobotInfo fulfillmentCenter = senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam());
    	
    	if(fulfillmentCenter != null || rc.getTeamSoup() < RobotType.FULFILLMENT_CENTER.cost * 0.8) {
    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(rc.getLocation().equals(fulfillmentCenterBuildSite)) {
    		//Move off of fulfillment center build site.
    		moveAnywhere();
    		return;
    	} else if(rc.getLocation().isWithinDistanceSquared(fulfillmentCenterBuildSite, 3)) {
    		if(attemptFulfillmentCenterConstruction(rc.getLocation().directionTo(fulfillmentCenterBuildSite))) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(move(rc.getLocation().directionTo(fulfillmentCenterBuildSite))) {
    		return;
    	} else {
    		routeTo(fulfillmentCenterBuildSite);
    		return;
    	}
    }
    
    private void refineryMinerProtocol() throws GameActionException {
    	System.out.println("refinery protocol");
    	
    	RobotInfo refinery = senseUnitType(RobotType.REFINERY, rc.getTeam());
    	if(refinery != null) {
    		minerData.addRefineryLoc(refinery.getLocation());
    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	}
    	
    	if(oughtBuildRefinery(rc)) {
	    	if(attemptRefineryConstruction()) {
	    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
	    		
	    		MapLocation refineryLocation = senseUnitType(RobotType.REFINERY, rc.getTeam()).getLocation();
	    		minerData.addRefineryLoc(refineryLocation);
	    		
	    		if(!sendTransaction(10, Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC, refineryLocation)) {
	    			System.out.println("Refinery transaction pending!");
	    			minerData.setPendingTransaction(Robot.Type.TRANSACTION_ENEMY_REFINERY_AT_LOC, refineryLocation, 10);
	    		} else {
	    			System.out.println("Completed refinery transaction!");
	    		}
	    		
	    		return;
    		} else if(rc.getLocation().distanceSquaredTo(minerData.getSpawnerLocation()) <= 18) {
    			//Move away from range of the wall.
    			moveMinerFromHQ();
    			return;
    		}
    	} else {
    		//If you ought not build a refinery right now, keep doing soup miner stuff!
    		if(rc.getSoupCarrying() > RobotType.MINER.soupLimit / 2) fullMinerProtocol();
    		else emptyMinerProtocol();
    	}
    }

	/**
	 * Builds fulfillment center near HQ
	 * @throws GameActionException
	 */
	private void defenseMinerProtocol() throws GameActionException {    	
    	if(!minerData.isFulfillmentCenterBuilt()) {
    		if(routeToFulfillmentCenterSite()) {
    			buildDefenseFulfillmentCenter();
    			return;
    		}
    	}
    }

	/**
	 * Miner whose soup carrying capacity is full
	 * @throws GameActionException
	 */
	private void fullMinerProtocol() throws GameActionException {
    	System.out.println("full protocol");
    	
		//Start by trying to deposit into a refinery.
		Direction adjacentRefineryDirection = getAdjacentRefineryDirection(rc);
		
		if (adjacentRefineryDirection != Direction.CENTER) {
			depositRawSoup(adjacentRefineryDirection);
			minerData.addRefineryLoc(rc.getLocation().add(adjacentRefineryDirection));
			
			//TODO: This first condition for refinery building is not yet satisfactory. Soup locations need to be taken into account everywhere and removed when vacant before this yet has entirely desirable effects.
			System.out.println("This miner knows of " + minerData.getSoupLocs().size() + " soup location(s) and " + minerData.getRefineryLocs().size() + " refiner(ies).");
			if(getBuildPriority(minerData) == RobotType.REFINERY) minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
			else if(rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && !canSenseHubDesignSchool()) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
			else if(rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && !canSenseHubFulfillmentCenter()) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
			return;
		}

		//If no refinery is adjacent, look for one.
		//RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam(), ((int) Math.sqrt(RobotType.MINER.sensorRadiusSquared)) - 2); //For pathfind, change senseUnitType if add back in
		
		//TODO: Once the landscapers get going, miners should no longer return to the HQ to refine soup. We need to communicate that via a transaction.
		
		RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
		RobotInfo landscaper = senseUnitType(RobotType.LANDSCAPER, rc.getTeam());
		
		if(hq != null) {
			if(landscaper != null) {
				System.out.println("Moving from landscaper site.");
	    		moveMinerFromHQ();
	    		minerData.removeRefineryLoc(hq.getLocation());
	    		if(minerData.getRefineryLocs().size() == 0) {
	        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
	        	}
	    		return;
			} else {
				System.out.println("No landscaper present.");
				if(minerData.getRefineryLocs().size() > 0) {
					System.out.println("Routing to alternative refinery.");
					routeTo(locateClosestLocation(minerData.getRefineryLocs(), rc.getLocation()));
				} else {
					System.out.println("Switching from soup miner to refinery builder.");
					minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
				}
	    	}
		} else {
			System.out.println("The HQ cannot be detected.");
			if(minerData.getRefineryLocs().size() > 0) {
				System.out.println("Routing to alternative refinery.");
				routeTo(locateClosestLocation(minerData.getRefineryLocs(), rc.getLocation()));
			} else {
				System.out.println("Switching from soup miner to refinery builder.");
				minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
			}
		}
    }

	/**
	 * Miner under soup carrying limit
	 * @throws GameActionException
	 */
	private void emptyMinerProtocol() throws GameActionException {		
    	System.out.println("empty protocol");
    	
    	RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
    	RobotInfo fulfillmentCenter = senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam());
    	RobotInfo landscaper = senseUnitType(RobotType.LANDSCAPER, rc.getTeam());
    	
    	//TODO: Miners can approach again once the landscaper gets high on a wall. We need to test for elevation to see to it that this happens.
    	if(landscaper != null) {
    		//We only need the miners to back off if the wall is not yet built.
    		if(rc.getLocation().distanceSquaredTo(minerData.getSpawnerLocation()) <= 8 && rc.senseElevation(landscaper.getLocation()) - rc.senseElevation(rc.getLocation()) <= GameConstants.MAX_DIRT_DIFFERENCE) {
	    		System.out.println("\tLandscaper detected");
	        	moveMinerFromHQ();
	        	minerData.removeRefineryLoc(hq.getLocation());
	        	if(minerData.getRefineryLocs().size() == 0) {
	        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
	        	}
	        	return;
    		}
    	} else if(fulfillmentCenter != null && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost && rc.getLocation().equals(minerData.getSpawnerLocation().translate(-2, 0))) {
    		//This miner is standing on the landscaper spawn point, so it needs to mvoe.
    		moveMinerFromHQ();
    	}
    	
    	//TODO: Clarify these conditionals. They're causing miners to become idle when they shouldn't be.
    	if(hq != null && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
    		if(getBuildPriority(minerData) == RobotType.REFINERY) {
    			System.out.println("\tSetting role to refinery builder");
    			minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
        		return;
    		} else if(!minerData.isDesignSchoolBuilt()){
    			System.out.println("\tSetting role to design school builder");
        		minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
        		return;
    		}
    	}
    	
    	if(!mineRawSoup(getAdjacentSoupDirection())) {
    		System.out.println("Could not mine adjacent soup.");
    		if(minerData.getSoupLocs().size() > 0) refreshSoupLocations();
    		if(minerData.getSoupLocs().size() == 0) findNearbySoup();
    		
    		if(minerData.getSoupLocs().size() > 0) {
    			MapLocation closestSoup = locateClosestLocation(minerData.getSoupLocs(), rc.getLocation());
    			if(!routeTo(closestSoup)) minerData.removeSoupLoc(closestSoup);
    		} else {
    			continueSearch();
    		}
		} else {
			System.out.println("Mined soup. (" + rc.getSoupCarrying() + ")");
		}
    }
    
    private void moveMinerFromHQ() throws GameActionException {
    	Direction fromHQDirection = data.getSpawnerLocation().directionTo(rc.getLocation());
    	routeTo(rc.getLocation().add(fromHQDirection));
    }

	/**
	 * Searches map until it finds enemy unit, then follows that unit. Reports enemy building locations.
	 * @throws GameActionException
	 */
	private void scoutMinerProtocol() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		// Scans for enemy robots, if it's a building, reports it and if it's a unit, sets it as unit to follow.
		for (RobotInfo robot : robots) {
			RobotType unitType = robot.getType();
			if (unitType.isBuilding()) {
				int soupBid = (robot.getType() == RobotType.HQ) ? 10 : 5; //HQ Location is more important than other buildings hence higher cost
				// Add check here if location already reported
				sendTransaction(soupBid, Robot.getLocationType(rc, unitType, robot.getTeam()), robot.getLocation());
			} else {
				if (minerData.getTargetRobot() == null) {
					if (minerData.getPreviousTarget() == null) {
						// Sets as target if there was no previous target
						minerData.setTargetRobot(robot);
						System.out.println("Target acquired. Loc: " + minerData.getTargetRobot().getLocation());
					} else if (robot.getID() != minerData.getPreviousTarget().getID()) {
						// If there was previous target, checks to ensure it is not that previous target
						minerData.setTargetRobot(robot);
						System.out.println("Target acquired. Loc: " + minerData.getTargetRobot().getLocation());
					}
				} else if (minerData.getTargetRobot().getID() == robot.getID()) {
					//If the bot scanned is the same bot it was following the turn before and it has been following it for some turns
					if (minerData.getTurnsScouted() < 100) {
						minerData.setTargetRobot(robot); // To update robot's location
						minerData.incrementTurnsScouted();
						System.out.println("Following target. Loc: " + minerData.getTargetRobot().getLocation());
					} else {
						minerData.setPreviousTarget(minerData.getTargetRobot());
						minerData.setTargetRobot(null);
						minerData.resetTurnsScouted();
						System.out.println("Switching target...");
					}
				}
			}
		}

		if (minerData.getTargetRobot() != null) {
			//Sets search direction to be two spaces away from where the target robot is
			MapLocation scoutLoc = rc.getLocation();
			MapLocation targetLoc = minerData.getTargetRobot().getLocation();
			Direction targetToScout = targetLoc.directionTo(scoutLoc);
			minerData.setSearchDirection(scoutLoc.directionTo(targetLoc.add(targetToScout).add(targetToScout)));
		}

		// Sensing and reporting Soup
		MapLocation soupLoc = getSoupLocation();
		if (soupLoc != null) {
			if (minerData.addSoupLoc(soupLoc)) {
				System.out.println("Found Soup! Loc: " + soupLoc);
				sendTransaction(5, Robot.Type.TRANSACTION_SOUP_AT_LOC, soupLoc);
			}
		}


		// Either searches in direction of target or last known position of target
		continueSearch();

	}
	
	private void discernRole() throws GameActionException {		
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
		boolean fulfillmentCenterBuilt = false;
		boolean designSchoolBuilt = false;
		
		RobotInfo[] enemy = rc.senseNearbyRobots(-1, data.getOpponent());
		boolean enemyDesignSchoolAdjacent = false;
		
		for(RobotInfo robot : robots) {
			if(robot.type == RobotType.FULFILLMENT_CENTER) fulfillmentCenterBuilt = true;
			else if(robot.type == RobotType.DESIGN_SCHOOL) designSchoolBuilt = true;
		}
		
		for(RobotInfo robot : enemy) {
			if(robot.type == RobotType.DESIGN_SCHOOL) enemyDesignSchoolAdjacent = true;
		}
		
		
		if(enemyDesignSchoolAdjacent) minerData.setCurrentRole(MinerData.ROLE_BLOCK);
		else if(fulfillmentCenterBuilt) minerData.setCurrentRole(MinerData.ROLE_DEFENSE_BUILDER);
		else if(designSchoolBuilt && rc.getTeamSoup() >= ((float) RobotType.FULFILLMENT_CENTER.cost * 0.8f)) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
		else if(!designSchoolBuilt && rc.getTeamSoup() >= ((float) RobotType.DESIGN_SCHOOL.cost * 0.8f)) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
//		else if (rc.getRoundNum() % 3 == 0) data.setCurrentRole(MinerData.ROLE_SCOUT);
		else minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
	}
	
	private RobotType getBuildPriority(MinerData data) {
		if(data.getSoupLocs().size() > 3 && data.getRefineryLocs().size() == 1 && data.getRefineryLocs().contains(data.getSpawnerLocation())) return RobotType.REFINERY;
		return RobotType.DESIGN_SCHOOL;
	}
	
	private boolean canSenseHubDesignSchool() throws GameActionException {
		RobotInfo designSchoolInfo = rc.senseRobotAtLocation(data.getSpawnerLocation().translate(-1, 0));
		if(designSchoolInfo == null) return false;
		return designSchoolInfo.type == RobotType.DESIGN_SCHOOL;
	}
	
	private boolean attemptDesignSchoolConstruction(Direction buildDirection) throws GameActionException {
		waitUntilReady();
		
		if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL, buildDirection)) {
			rc.buildRobot(RobotType.DESIGN_SCHOOL, buildDirection);
			return true;
		}
		
		System.out.println("Failed to build design school...");
		
		return false;
	}
	
	private boolean attemptFulfillmentCenterConstruction(Direction buildDirection) throws GameActionException {
		waitUntilReady();
		
		if(rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, buildDirection)) {
			rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDirection);
			return true;
		}
		
		System.out.println("Failed to build fulfillment center...");
		
		return false;
	}

	//TODO Should be unnecessary once communication is fully running. Should remove if running into bytecode limit
	private Direction getAdjacentRefineryDirection(RobotController rc) throws GameActionException {
		RobotInfo refinery = senseUnitType(RobotType.REFINERY, rc.getTeam(), 3);
		RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam(), 3);

		if (refinery != null) {
			return rc.getLocation().directionTo(refinery.getLocation());
		} else if (hq != null) {
			return rc.getLocation().directionTo(hq.getLocation());
		} else {
			return Direction.CENTER;
		}
	}
	
	private boolean oughtBuildRefinery(RobotController rc) {
		return rc.getTeamSoup() >= RobotType.REFINERY.cost;
	}
	
	private boolean attemptRefineryConstruction() throws GameActionException {		
		waitUntilReady();
		
		for(Direction buildDirection : directions) {
			//The distance check is to make sure that we don't build the refinery where the wall ought to be.
			if(rc.canBuildRobot(RobotType.REFINERY, buildDirection) && rc.getLocation().add(buildDirection).distanceSquaredTo(data.getSpawnerLocation()) > 18) {
				rc.buildRobot(RobotType.REFINERY, buildDirection);
				return true;
			}
		}
		
		System.out.println("Failed to build refinery...");
		
		return false;
	}
	
	public void depositRawSoup(Direction dir) throws GameActionException {
		waitUntilReady();
		if(rc.canDepositSoup(dir)) rc.depositSoup(dir, rc.getSoupCarrying()); rc.setIndicatorDot(rc.getLocation().add(dir), 255, 0, 0);
	}

	/**
	 * Finds location with the most soup within 1 tile radius
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	private Direction getAdjacentSoupDirection() throws GameActionException {
		Direction mostSoupDirection = Direction.CENTER;
		int mostSoupLocated = 0;
		
		for(Direction direction : directions) {
			if (rc.canSenseLocation(rc.getLocation().add(direction))) {
				int foundSoup = rc.senseSoup(rc.adjacentLocation(direction));
				mostSoupDirection = foundSoup > mostSoupLocated ? direction : mostSoupDirection;
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
	private void findNearbySoup() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		
		for(int dx = -SENSOR_RADIUS; dx <= SENSOR_RADIUS; dx++) {
			for(int dy = -SENSOR_RADIUS; dy <= SENSOR_RADIUS; dy++) {
				MapLocation potentialSoupLocation = rcLocation.translate(dx, dy);
				if(rc.canSenseLocation(potentialSoupLocation)) {
					if(rc.senseSoup(potentialSoupLocation) > 0) minerData.addSoupLoc(potentialSoupLocation);
				}
			}
		}
	}

	private void refreshSoupLocations() throws GameActionException {		
		//Use of "int i" rather than MapLocation location : data.getSoupLocs() was intentional. This will throw an error otherwise.
		for(int i = 0; i < minerData.getSoupLocs().size(); i++) {
			MapLocation allegedSoupLocation = minerData.getSoupLocs().get(i);
			if(rc.canSenseLocation(allegedSoupLocation)) {
				if(rc.senseSoup(allegedSoupLocation) == 0) {
					minerData.removeSoupLoc(allegedSoupLocation);
					i--;
				}
			}
		}
	}
	
	/**
	 * Returns location of soup within two radius of robot. If not found, will return null.
	 * @param rc
	 * @return
	 * @throws GameActionException
	 */
	private MapLocation getSoupLocation() throws GameActionException{
		Direction soupDir = getAdjacentSoupDirection();
		MapLocation soupLoc = rc.adjacentLocation(soupDir);
		if (soupDir == Direction.CENTER) {
//			soupDir = getDistantSoupDirection(rc);
			if (soupDir != Direction.CENTER) {
				//Now checks non-adjacent tiles
				soupLoc = rc.adjacentLocation(soupDir).add(soupDir);
			}
		}
		return (rc.senseSoup(soupLoc) > 0) ? soupLoc : null;
	}

	/**
	 * Mines soup if able
	 * @param rc Robot Controller
	 * @param dir Direction
	 * @throws GameActionException
	 */
	private boolean mineRawSoup(Direction dir) throws GameActionException {
		waitUntilReady();

		if(rc.isReady() && rc.canMineSoup(dir) && rc.getSoupCarrying() != RobotType.MINER.soupLimit) {
			rc.setIndicatorDot(rc.getLocation().add(dir), 0, 255, 0);
			rc.mineSoup(dir);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Moves in same direction as before, otherwise moves in random direction
	 * @param rc
	 * @param data
	 * @throws GameActionException
	 */
	private void continueSearch() throws GameActionException {		
		//The move function is deliberately unused here.
		waitUntilReady();
		
		if(rc.canMove(minerData.getSearchDirection()) && !rc.senseFlooding(rc.getLocation().add(minerData.getSearchDirection()))) {
			rc.move(minerData.getSearchDirection());
			return;
		}
		
		minerData.setSearchDirection(directions[(int) (Math.random() * directions.length)]);
	}
	
	private boolean routeToFulfillmentCenterSite() throws GameActionException {		
		if(!minerData.hasPath()) {
			return pathfind(minerData.getSpawnerLocation().add(rc.getLocation().directionTo(minerData.getSpawnerLocation())));
    	}
    	
		return pathfind(null);
	}
	
	private boolean canSenseHubFulfillmentCenter() throws GameActionException {
		RobotInfo designSchoolInfo = rc.senseRobotAtLocation(data.getSpawnerLocation().translate(1, 0));
		if(designSchoolInfo == null) return false;
		return designSchoolInfo.type == RobotType.FULFILLMENT_CENTER;
	}
	
	private boolean buildDefenseFulfillmentCenter() throws GameActionException {
		MapLocation hqLocation = data.getSpawnerLocation().add(Direction.EAST);
		Direction buildDirection = rc.getLocation().directionTo(hqLocation);
		
		waitUntilReady();
		if(rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, buildDirection)) {
			rc.buildRobot(RobotType.FULFILLMENT_CENTER, buildDirection);
			return true;
		}
		
		System.out.println("Failed to build fulfillment center.");
		
		return false;
	}

	/**
	 * Reads block and uses useful information such as refinery and soup locations
	 * @param rc
	 * @param minerdata
	 * @param block
	 * @throws GameActionException
	 */
	private void readTransaction(Transaction[] block) throws GameActionException {		
		for (Transaction message : block) {
			int[] decodedMessage = decodeTransaction(message);
			if (decodedMessage != new int[] {0}) {
				Robot.Type category = Robot.Type.enumOfValue(decodedMessage[1]);
				MapLocation loc = new MapLocation(decodedMessage[2], decodedMessage[3]);

				if (category == null) {
					System.out.println("Something is terribly wrong. enumOfValue returns null");
				}
				switch(category) {
					case TRANSACTION_SOUP_AT_LOC:
						minerData.addSoupLoc(loc);
						break;
					case TRANSACTION_FRIENDLY_REFINERY_AT_LOC:
						minerData.addRefineryLoc(loc);
						break;
					default:
						break;
				}
			}

		}
	}
	
}

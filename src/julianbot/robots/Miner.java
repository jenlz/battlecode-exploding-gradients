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

public class Miner extends Scout {
	
	private MinerData minerData;
	
	private MapLocation designSchoolBuildSite;
	private MapLocation fulfillmentCenterBuildSite;
	private MapLocation vaporatorBuildMinerLocation;
		private MapLocation vaporatorBuildSite;
	
	private static final int SELF_DESTRUCT_COUNTDOWN_RESET = 5;
	private int selfDestructCountdown = SELF_DESTRUCT_COUNTDOWN_RESET;
	
	public Miner(RobotController rc) {
		super(rc);
		this.data = new MinerData(rc, getSpawnerLocation());
		this.scoutData = (MinerData) this.data;
		this.minerData = (MinerData) this.data;
	}
	
	private void initializeBuildSites() {
		MapLocation hqLocation = data.getSpawnerLocation();
		
		boolean leftEdge = hqLocation.x <= 0;
		boolean rightEdge = hqLocation.x >= rc.getMapWidth() - 1;
		boolean topEdge = hqLocation.y >= rc.getMapHeight() - 1;
		boolean bottomEdge = hqLocation.y <= 0;
		minerData.setBaseOnEdge(leftEdge || rightEdge || topEdge || bottomEdge);
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) {
				//Lucky us, the HQ is also next to the southern wall.
				designSchoolBuildSite = hqLocation.translate(0, 2);
				fulfillmentCenterBuildSite = hqLocation.translate(1, 0);
				vaporatorBuildMinerLocation = hqLocation.translate(1, 1);
				vaporatorBuildSite = hqLocation.translate(0, 1);
			} else if(topEdge) {
				//Lucky us, the HQ is also next to the northern wall.
				designSchoolBuildSite = hqLocation.translate(0, -2);
				fulfillmentCenterBuildSite = hqLocation.translate(1, 0);
				vaporatorBuildMinerLocation = hqLocation.translate(1, -1);
				vaporatorBuildSite = hqLocation.translate(0, -1);
			} else {
				//The HQ is next to the western wall, but not cornered.
				designSchoolBuildSite = hqLocation.translate(0, 2);
				fulfillmentCenterBuildSite = hqLocation.translate(1, 0);
				vaporatorBuildMinerLocation = hqLocation.translate(1, 1);
				vaporatorBuildSite = hqLocation.translate(0, 1);
			}
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) {
				//Lucky us, the HQ is also next to the southern wall.
				designSchoolBuildSite = hqLocation.translate(0, 2);
				fulfillmentCenterBuildSite = hqLocation.translate(-1, 0);
				vaporatorBuildMinerLocation = hqLocation.translate(-1, 1);
				vaporatorBuildSite = hqLocation.translate(0, 1);
			} else if(topEdge) {
				//Lucky us, the HQ is also next to the northern wall.
				designSchoolBuildSite = hqLocation.translate(0, -2);
				fulfillmentCenterBuildSite = hqLocation.translate(-1, 0);
				vaporatorBuildMinerLocation = hqLocation.translate(-1, -1);
				vaporatorBuildSite = hqLocation.translate(0, -1);
			} else {
				designSchoolBuildSite = hqLocation.translate(0, -2);
				fulfillmentCenterBuildSite = hqLocation.translate(-1, 0);
				vaporatorBuildMinerLocation = hqLocation.translate(-1, -1);
				vaporatorBuildSite = hqLocation.translate(0, -1);
			}
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			designSchoolBuildSite = hqLocation.translate(2, 0);
			fulfillmentCenterBuildSite = hqLocation.translate(0, -1);
			vaporatorBuildMinerLocation = hqLocation.translate(1, -1);
			vaporatorBuildSite = hqLocation.translate(1, 0);
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			designSchoolBuildSite = hqLocation.translate(-2, 0);
			fulfillmentCenterBuildSite = hqLocation.translate(0, 1);
			vaporatorBuildMinerLocation = hqLocation.translate(-1, 1);
			vaporatorBuildSite = hqLocation.translate(-1, 0);
		} else {
			designSchoolBuildSite = hqLocation.translate(-1, 0);
			fulfillmentCenterBuildSite = hqLocation.translate(1, 0);
			vaporatorBuildMinerLocation = hqLocation.translate(0, -1);
			vaporatorBuildSite = hqLocation.translate(1, -1);
		}
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) minerData.setWallOffsetBounds(0, 2, 0, 3);
			else if(topEdge) minerData.setWallOffsetBounds(0, 2, -3, 0);
			else minerData.setWallOffsetBounds(0, 2, -1, 3);
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) minerData.setWallOffsetBounds(-2, 0, 0, 3);
			else if(topEdge) minerData.setWallOffsetBounds(-2, 0, -3, 0);
			else minerData.setWallOffsetBounds(-2, 0, -3, 1);
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			minerData.setWallOffsetBounds(-1, 3, 0, -2);
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			minerData.setWallOffsetBounds(-3, 1, 0, 2);
		} else {
			minerData.setWallOffsetBounds(-2, 2, -2, 2);
		}
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
    	if(turnCount == 1) {
    		discernRole();
    		initializeBuildSites();
    	}
    	
    	if(oughtSelfDestruct()) {
    		selfDestructCountdown--;
    		System.out.println("SELF-DESTRUCT COUNTDOWN " + selfDestructCountdown);
    		if(selfDestructCountdown <= 0) {
	    		System.out.println("So long, cruel world.");
	    		rc.disintegrate();
    		}
    	} else {
    		selfDestructCountdown = SELF_DESTRUCT_COUNTDOWN_RESET;
    	}

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
			case MinerData.ROLE_VAPORATOR_BUILDER:
				vaporatorMinerProtocol();
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
			case MinerData.ROLE_RUSH:
				rushMinerProtocol();
				break;
			default:
				break;
		}
	}
	
	private boolean oughtSelfDestruct() throws GameActionException {
		if(minerData.getCurrentRole() == MinerData.ROLE_VAPORATOR_BUILDER) {
			int numVaporators = this.senseNumberOfUnits(RobotType.VAPORATOR, rc.getTeam());
			return(minerData.isBaseOnEdge()) ? numVaporators >= 1 : numVaporators >= 2;
		}
		
		//TODO: On the wall detection for edge-case maps.
		return (this.senseUnitType(RobotType.LANDSCAPER, rc.getTeam(), 3) != null && isOnWall());
	}
	
	/**
	 * Builds a design school and then switches to a soup miner
	 * @throws GameActionException
	 */
	private void designMinerProtocol() throws GameActionException {    	
		if(canSenseHubDesignSchool()) {
			minerData.setDesignSchoolBuilt(true);
			minerData.setCurrentRole(canSenseHubFulfillmentCenter() ? MinerData.ROLE_SOUP_MINER : MinerData.ROLE_FULFILLMENT_BUILDER);
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
    	if(canSenseHubFulfillmentCenter()) {
    		minerData.setFulfillmentCenterBuilt(true);
    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(rc.getTeamSoup() < RobotType.FULFILLMENT_CENTER.cost * 0.8) {
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
		if (refinery != null) {
			minerData.addRefineryLoc(refinery.getLocation());
			minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
			return;
		}

		if(minerData.getSoupLocs().size() > 0) {
			for (MapLocation soupLoc : minerData.getSoupLocs()) {
				if (minerData.getSpawnerLocation().distanceSquaredTo(soupLoc) > 9) {
					System.out.println("/tRouting to " + soupLoc);
					routeTo(soupLoc);
					//TODO: is this break what we really want?
					break;
				}
			}
		}

    	if(oughtBuildRefinery()) {
    		if(isOnWall()) {
    			//Move away from range of the wall.
    			moveMinerFromHQ();
    		} else if(attemptRefineryConstruction()) {
	    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
	    		
	    		MapLocation refineryLocation = senseUnitType(RobotType.REFINERY, rc.getTeam(), 3).getLocation();
	    		minerData.addRefineryLoc(refineryLocation);
	    		
	    		if(!sendTransaction(10, Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC, refineryLocation)) {
	    			System.out.println("Refinery transaction pending!");
	    		} else {
	    			System.out.println("Completed refinery transaction!");
	    		}	    		
    		}
    	} else {
    		//If you ought not build a refinery right now, keep doing soup miner stuff!
    		if(rc.getSoupCarrying() > RobotType.MINER.soupLimit / 2) fullMinerProtocol();
    		else emptyMinerProtocol();
    	}
    }
    
    private void vaporatorMinerProtocol() throws GameActionException {
		System.out.println("vaporator protocol");
		
		if(vaporatorBuildMinerLocation == null || vaporatorBuildSite == null) {
			minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
			return;
		}
		
		if(rc.canSenseLocation(vaporatorBuildMinerLocation) && rc.isLocationOccupied(vaporatorBuildMinerLocation) && !rc.getLocation().equals(vaporatorBuildMinerLocation)) {
			minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
			return;
		}
		
		if(!rc.getLocation().equals(vaporatorBuildMinerLocation)) {
			routeTo(vaporatorBuildMinerLocation);
			return;
		} else if(oughtBuildVaporator()) {
	    	attemptVaporatorConstruction();
	    	return;
    	}
		
		Direction adjacentSoupDirection = getAdjacentSoupDirection();
		
		if(adjacentSoupDirection != Direction.CENTER && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			mineRawSoup(getAdjacentSoupDirection());
		} else if(rc.getSoupCarrying() > 0 && getAdjacentRefineryDirection() != Direction.CENTER) {
    		depositRawSoup(rc.getLocation().directionTo(minerData.getHqLocation()));
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
    	
    	//Update refinery locations
    	RobotInfo[] refineries = this.senseAllUnitsOfType(RobotType.REFINERY, rc.getTeam());
		for(RobotInfo refinery : refineries) {
			minerData.addRefineryLoc(refinery.getLocation());
		}
    	
		//Immediately try to deposit into an adjacent refinery.
		Direction adjacentRefineryDirection = getAdjacentRefineryDirection();
		
		if (adjacentRefineryDirection != null) {
			depositRawSoup(adjacentRefineryDirection);
			minerData.addRefineryLoc(rc.getLocation().add(adjacentRefineryDirection));
			
			//TODO: This first condition for refinery building is not yet satisfactory. Soup locations need to be taken into account everywhere and removed when vacant before this yet has entirely desirable effects.
			System.out.println("This miner knows of " + minerData.getSoupLocs().size() + " soup location(s) and " + minerData.getRefineryLocs().size() + " refiner(ies).");
			if(getBuildPriority(minerData) == RobotType.REFINERY) minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
			else if(rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && !minerData.isDesignSchoolBuilt()) {
				if(!canSenseHubDesignSchool()) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
				else minerData.setDesignSchoolBuilt(true);
			} else if(rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && !minerData.isFulfillmentCenterBuilt()) {
				if(!canSenseHubFulfillmentCenter()) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
				else minerData.setFulfillmentCenterBuilt(true);
			}
			
			return;
		}
						
		RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
		RobotInfo landscaper = senseUnitType(RobotType.LANDSCAPER, rc.getTeam());
		
		if(hq != null) {
			if(landscaper != null) {
				System.out.println("Miner sees a landscaper! It may become a vaporator builder.");
				RobotInfo buildSiteOccupant = null;
				
				if(rc.getLocation().equals(vaporatorBuildMinerLocation)) {
					minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
					return;
				}
				
				if(rc.canSenseLocation(vaporatorBuildMinerLocation)) buildSiteOccupant = rc.senseRobotAtLocation(vaporatorBuildMinerLocation);
				
				if(buildSiteOccupant == null || buildSiteOccupant.getType() != RobotType.MINER) {
					routeTo(vaporatorBuildMinerLocation);
				} else {
		    		minerData.removeRefineryLoc(hq.getLocation());
		    		if(minerData.getRefineryLocs().size() == 0) {
		        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
		        	}
				}
			} else {
				System.out.println("No landscaper present.");
				if(minerData.getRefineryLocs().size() == 0) {
					minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
					return;
				}
	    	}
		} else {
			System.out.println("The HQ cannot be detected.");
			if(minerData.getRefineryLocs().size() == 0) {
				minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
				return;
			}
		}
		
		if(minerData.getRefineryLocs().size() > 0) routeTo(locateClosestLocation(minerData.getRefineryLocs(), rc.getLocation()));
    }

	/**
	 * Miner under soup carrying limit
	 * @throws GameActionException
	 */
	private void emptyMinerProtocol() throws GameActionException {		
    	System.out.println("empty protocol");
    	
    	//Update refinery locations
    	RobotInfo[] refineries = this.senseAllUnitsOfType(RobotType.REFINERY, rc.getTeam());
		for(RobotInfo refinery : refineries) {
			minerData.addRefineryLoc(refinery.getLocation());
		}
    	
    	RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
    	RobotInfo fulfillmentCenter = senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam());
    	RobotInfo landscaper = senseUnitType(RobotType.LANDSCAPER, rc.getTeam());
    	
    	if(landscaper != null) {
    		System.out.println("Saw a landscaper!");
    		if(vaporatorBuildMinerLocation != null && isClosestMinerTo(vaporatorBuildMinerLocation)) {
    			System.out.println("Becoming a vaporator builder!");
    			routeTo(vaporatorBuildMinerLocation);
    			minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
    			return;
    		} else if(isOnWall()) {
	    		System.out.println("On the wall -- moving away");
	        	moveMinerFromHQ();
	        	minerData.removeRefineryLoc(data.getSpawnerLocation());
	        	if(minerData.getRefineryLocs().size() == 0) {
	        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
	        	}
	        	return;
    		}
    	} else if(fulfillmentCenter != null) {
    		System.out.println("Saw a fulfillment center!");
    		if(isClosestMinerTo(vaporatorBuildSite)) {
    			routeTo(vaporatorBuildSite);
    			minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
    			return;
    		} else if(isOnWall()){
    			System.out.println("On the wall -- moving away");
    			moveMinerFromHQ();
    		}
    	}
    	
    	if(hq != null && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
    		if(getBuildPriority(minerData) == RobotType.REFINERY) {
    			System.out.println("\tSetting role to refinery builder");
    			minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
        		return;
    		} else if(!minerData.isDesignSchoolBuilt() && isClosestMinerTo(designSchoolBuildSite)){
    			System.out.println("\tSetting role to design school builder");
        		minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
        		return;
    		}
    	}
    	
    	if(!mineRawSoup(getAdjacentSoupDirection())) {
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
	
	private boolean isOnWall() {
		MapLocation hqLocation = minerData.getHqLocation();
    	
    	int minDx = minerData.getWallOffsetXMin();
    	int maxDx = minerData.getWallOffsetXMax();
    	int minDy = minerData.getWallOffsetYMin();
    	int maxDy = minerData.getWallOffsetYMax();
    	
    	int dx = rc.getLocation().x - hqLocation.x;
    	int dy = rc.getLocation().y - hqLocation.y;
    	
    	boolean dxOnBound = (dx == minDx || dx == maxDx);
    	boolean dyInRange = minDy <= dy && dy <= maxDy;
    	if(dxOnBound && dyInRange) return true;
    	
    	
    	boolean dyOnBound = (dy == minDy || dy == maxDy);
    	boolean dxInRange = minDx <= dx && dx <= maxDx;
    	if(dyOnBound && dxInRange) return true;
    	
    	return false;
	}
	
	private boolean isClosestMinerTo(MapLocation location) {
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
		int thisDistanceSquared = rc.getLocation().distanceSquaredTo(location);
		
		for(RobotInfo robot : robots) {
			int thatDistanceSquared = robot.getLocation().distanceSquaredTo(location);
			if(thatDistanceSquared < thisDistanceSquared) return false;
			if(thatDistanceSquared == thisDistanceSquared) {
				//To break ties, favor the robot on the bottom-right.
				if(robot.getLocation().x > rc.getLocation().x || robot.getLocation().y < rc.getLocation().y) return false;
			}
		}
		
		return true;
	}
    
    private void moveMinerFromHQ() throws GameActionException {
    	Direction fromHQDirection = data.getSpawnerLocation().directionTo(rc.getLocation());
    	routeTo(rc.getLocation().add(fromHQDirection));
    	data.setSearchDirection(fromHQDirection);
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

	/**
	 * Miner that finds enemy HQ and builds design school to bury enemy HQ
	 */
	private void rushMinerProtocol() throws GameActionException {
		System.out.println("Rush Miner Protocol");
		findNearbySoup();
		if (rc.getRoundNum() > 250) {minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);};
		if (minerData.getEnemyHqLocation() == null) {
			if (!minerData.searchDestinationsDetermined()) {
				minerData.calculateSearchDestinations(rc);
			}

			routeTo(minerData.getActiveSearchDestination());
			attemptEnemyHQDetection();
			if (minerData.getEnemyHqLocation() != null) {
				sendTransaction(10, Robot.Type.TRANSACTION_ENEMY_HQ_AT_LOC, minerData.getEnemyHqLocation());
			}
		} else {
			if (!rc.getLocation().isAdjacentTo(minerData.getEnemyHqLocation())) {
				routeTo(minerData.getEnemyHqLocation());
			} else if (senseUnitType(RobotType.DESIGN_SCHOOL, rc.getTeam()) == null) {
				for (Direction dir : Direction.allDirections()) {
					if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
						rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
					}
				}
			} else {
				minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
			}
		}
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
		else if(fulfillmentCenterBuilt) minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
		else if(designSchoolBuilt && rc.getTeamSoup() >= ((float) RobotType.FULFILLMENT_CENTER.cost * 0.8f)) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
		else if(!designSchoolBuilt && rc.getTeamSoup() >= ((float) RobotType.DESIGN_SCHOOL.cost * 0.8f)) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
//		else if (rc.getRoundNum() % 3 == 0) data.setCurrentRole(MinerData.ROLE_SCOUT);
		else if(rc.getRoundNum() <= 2) minerData.setCurrentRole(MinerData.ROLE_RUSH);
		else minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
	}
	
	private RobotType getBuildPriority(MinerData data) {
		if(data.getSoupLocs().size() > 3 && data.getRefineryLocs().size() == 1 && data.getRefineryLocs().contains(data.getSpawnerLocation())) return RobotType.REFINERY;
		return RobotType.DESIGN_SCHOOL;
	}
	
	private boolean canSenseHubDesignSchool() throws GameActionException {
		if(!rc.canSenseLocation(designSchoolBuildSite)) return false;
		
		RobotInfo designSchoolInfo = rc.senseRobotAtLocation(designSchoolBuildSite);
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
	private Direction getAdjacentRefineryDirection() throws GameActionException {
		RobotInfo refinery = senseUnitType(RobotType.REFINERY, rc.getTeam(), 3);
		RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam(), 3);

		if (refinery != null) {
			return rc.getLocation().directionTo(refinery.getLocation());
		} else if (hq != null) {
			return rc.getLocation().directionTo(hq.getLocation());
		} else {
			return null;
		}
	}
	
	private boolean oughtBuildRefinery() {
		return rc.getTeamSoup() >= RobotType.REFINERY.cost;
	}
	
	private boolean attemptRefineryConstruction() throws GameActionException {		
		waitUntilReady();
		
		for(Direction buildDirection : directions) {
			//The distance check is to make sure that we don't build the refinery where the wall ought to be.
			if(rc.canBuildRobot(RobotType.REFINERY, buildDirection) && rc.getLocation().add(buildDirection).distanceSquaredTo(data.getSpawnerLocation()) > 31) {
				rc.buildRobot(RobotType.REFINERY, buildDirection);
				return true;
			}
		}
		
		System.out.println("Failed to build refinery...");
		
		return false;
	}
	
	private boolean oughtBuildVaporator() {
		return rc.getTeamSoup() >= RobotType.VAPORATOR.cost;
	}
	
	private boolean attemptVaporatorConstruction() throws GameActionException {		
		Direction buildDirection = rc.getLocation().directionTo(vaporatorBuildSite);
		
		if(rc.canBuildRobot(RobotType.VAPORATOR, buildDirection) && rc.getLocation().add(buildDirection).distanceSquaredTo(data.getSpawnerLocation()) <= 3) {
			rc.buildRobot(RobotType.VAPORATOR, buildDirection);
			return true;
		}
		
		System.out.println("Failed to build vaporator...");
		
		return false;
	}
	
	public void depositRawSoup(Direction dir) throws GameActionException {
		waitUntilReady();
		if(rc.canDepositSoup(dir)) rc.depositSoup(dir, rc.getSoupCarrying()); rc.setIndicatorDot(rc.getLocation().add(dir), 255, 0, 0);
	}

	/**
	 * Finds location with the most soup within 1 tile radius
	 * @return
	 * @throws GameActionException
	 */
	private Direction getAdjacentSoupDirection() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		Direction mostSoupDirection = Direction.CENTER;
		int mostSoupLocated = 0;
		
		for(Direction direction : directions) {
			if (rc.canSenseLocation(rcLocation.add(direction))) {
				int foundSoup = rc.senseSoup(rcLocation.add(direction));
				mostSoupDirection = foundSoup > mostSoupLocated ? direction : mostSoupDirection;
			}
		}
		
		return mostSoupDirection;
	}

	/**
	 * Finds location with the most soup within sensor radius
	 * @return
	 * @throws GameActionException
	 */
	private void findNearbySoup() throws GameActionException {
		// Might use a lot of bytecode. Not 100% sure. Trying to prevent an overly large ArrayList of soupLocs.
		MapLocation[] soupLocs = rc.senseNearbySoup();
		if (soupLocs.length > 0) {
			MapLocation bestSoupLoc = null;
			int bestSoupCount = 0;
			for (MapLocation soupLoc : soupLocs) {
				if (rc.senseSoup(soupLoc) > bestSoupCount) {
					bestSoupLoc = soupLoc;
					bestSoupCount = rc.senseSoup(soupLoc);
				}
			}
			if(minerData.addSoupLoc(bestSoupLoc)) {
				sendTransaction(1, Type.TRANSACTION_SOUP_AT_LOC, bestSoupLoc);
				rc.setIndicatorDot(bestSoupLoc, 255, 165, 0);
			}
			System.out.println("Transmitted soup!");
		}

	}

	/**
	 * Removes stored soup locations that are now empty
	 * @throws GameActionException
	 */
	private void refreshSoupLocations() throws GameActionException {		
		//Use of "int i" rather than MapLocation location : data.getSoupLocs() was intentional. This will throw an error otherwise.
		for(int i = 0; i < minerData.getSoupLocs().size(); i++) {
			MapLocation allegedSoupLocation = minerData.getSoupLocs().get(i);
			if(allegedSoupLocation != null && rc.canSenseLocation(allegedSoupLocation)) {
				if(rc.senseSoup(allegedSoupLocation) == 0) {
					minerData.removeSoupLoc(allegedSoupLocation);
					i--;
				}
			}
		}
	}
	
	/**
	 * Returns location of soup within two radius of robot. If not found, will return null.
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
	
	private boolean routeToFulfillmentCenterSite() throws GameActionException {		
		if(!minerData.hasPath()) {
			return pathfind(minerData.getSpawnerLocation().add(rc.getLocation().directionTo(minerData.getSpawnerLocation())));
    	}
    	
		return pathfind(null);
	}
	
	private boolean canSenseHubFulfillmentCenter() throws GameActionException {
		if(!rc.canSenseLocation(fulfillmentCenterBuildSite)) return false;
		
		RobotInfo fulfillmentCenterInfo = rc.senseRobotAtLocation(fulfillmentCenterBuildSite);
		if(fulfillmentCenterInfo == null) return false;
		return fulfillmentCenterInfo.type == RobotType.FULFILLMENT_CENTER;
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
	 * @param block
	 * @throws GameActionException
	 */
	private void readTransaction(Transaction[] block) throws GameActionException {		
		for (Transaction message : block) {
			int[] decodedMessage = decodeTransaction(message);
			if (decodedMessage.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
				Robot.Type category = Robot.Type.enumOfValue(decodedMessage[1]);
				MapLocation loc = new MapLocation(decodedMessage[2], decodedMessage[3]);

				if (category == null) {
					System.out.println("Something is terribly wrong. enumOfValue returns null. Miner readTransaction line ~621");
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

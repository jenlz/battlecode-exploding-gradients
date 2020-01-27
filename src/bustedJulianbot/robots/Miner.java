package bustedJulianbot.robots;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import bustedJulianbot.robotdata.MinerData;
import bustedJulianbot.utils.NumberMath;

public class Miner extends Scout {
	
	private MinerData minerData;
	
	private static final int SELF_DESTRUCT_COUNTDOWN_RESET = 5;
	private int selfDestructCountdown = SELF_DESTRUCT_COUNTDOWN_RESET;
	
	public Miner(RobotController rc) {
		super(rc);
		this.data = new MinerData(rc, getSpawnerLocation());
		this.scoutData = (MinerData) this.data;
		this.minerData = (MinerData) this.data;
	}
	
	@Override
	public void run() throws GameActionException {
		super.run();
		
    	if(turnCount == 1) {
    		discernInitialRole();
    		minerData.initializeWallData(data.getSpawnerLocation(), rc.getMapWidth(), rc.getMapHeight());
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

		readTransactions();
		senseBuildings();
		
		respondToThreats();
      
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
			case MinerData.ROLE_DEFENSE:
				defenseMinerProtocol();
				break;
			case MinerData.ROLE_SCOUT:
				scoutMinerProtocol();
				break;
			case MinerData.ROLE_RUSH:
				rushMinerProtocol();
				break;
			case MinerData.ROLE_DRONE_RUSH_FINISHER:
				droneRushFinisherProtocol();
				break;
			case MinerData.ROLE_OUTPOST_KEEPER:
				outpostKeeperProtocol();
				break;
			default:
				break;
		}
	}
	
	//INTERFERENCE
	private boolean oughtSelfDestruct() throws GameActionException {
		if(minerData.getCurrentRole() == MinerData.ROLE_VAPORATOR_BUILDER) {
			int numVaporators = this.senseNumberOfUnits(RobotType.VAPORATOR, rc.getTeam());
			return(minerData.isBaseOnEdge()) ? numVaporators >= 1 : numVaporators >= 2;
		}
		
		boolean interferingWithBase = isOnWall(rc.getLocation(), minerData.getSpawnerLocation()) || isWithinWall(rc.getLocation(), minerData.getSpawnerLocation());
		boolean wallBuilt = wallBuilt(minerData.getSpawnerLocation());
		
		System.out.println("Self destruct? Base Interference = " + interferingWithBase + ", Role = " + minerData.getCurrentRole() + ", Wall Built = " + wallBuilt);
		
		return this.senseUnitType(RobotType.LANDSCAPER, rc.getTeam(), 3) != null && interferingWithBase;
	}
	
	//ROLES
	private void discernInitialRole() throws GameActionException {		
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam());
		
		RobotInfo[] enemy = rc.senseNearbyRobots(-1, data.getOpponent());
		boolean enemyDesignSchoolNearby = false;
		
		for(RobotInfo robot : robots) {
			if(robot.type == RobotType.FULFILLMENT_CENTER) minerData.setFulfillmentCenterBuilt(true);
			else if(robot.type == RobotType.DESIGN_SCHOOL) minerData.setDesignSchoolBuilt(true);
			else if(robot.type == RobotType.VAPORATOR) minerData.setVaporatorBuilt(true);
		}
		
		for(RobotInfo robot : enemy) {
			if(robot.type == RobotType.DESIGN_SCHOOL) enemyDesignSchoolNearby = true;
		}
		
		boolean vaporatorBuilt = minerData.isVaporatorBuilt();
		boolean designSchoolBuilt = minerData.isDesignSchoolBuilt();
		boolean fulfillmentCenterBuilt = minerData.isFulfillmentCenterBuilt();
		
		System.out.println("Vaporator built? " + vaporatorBuilt);
		System.out.println("Design school built? " + designSchoolBuilt);
		System.out.println("Fulfillment center built? " + fulfillmentCenterBuilt);
		
		/*
		if(enemyDesignSchoolAdjacent) minerData.setCurrentRole(MinerData.ROLE_BLOCK);
		else if(fulfillmentCenterBuilt) minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
		else if(designSchoolBuilt && rc.getTeamSoup() >= ((float) RobotType.FULFILLMENT_CENTER.cost * 0.8f)) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
		else if(!designSchoolBuilt && rc.getTeamSoup() >= ((float) RobotType.DESIGN_SCHOOL.cost * 0.8f)) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
//		else if (rc.getRoundNum() % 3 == 0) data.setCurrentRole(MinerData.ROLE_SCOUT);
		else if(rc.getRoundNum() <= 2) minerData.setCurrentRole(MinerData.ROLE_RUSH);
		else minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
		*/
		
		RobotInfo[] landscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
		System.out.println("Sensed " + landscapers.length + " landscapers");
		
		boolean landscaperOnWall = false;
		MapLocation hqLocation = minerData.getSpawnerLocation();
		System.out.println("HQ Location = " + hqLocation);
		
		for(RobotInfo landscaper : landscapers) {
			if(isOnWall(landscaper.getLocation(), hqLocation)) {
				landscaperOnWall = true;
				break;
			} else {
				System.out.println(landscaper.getLocation() + " is NOT on the wall.");
			}
		}
		
		System.out.println("Miner in vaporator build miner location? " + (rc.getLocation().equals(minerData.getVaporatorBuildMinerLocation())));
		System.out.println("Landscaper on wall? " + landscaperOnWall);
		
		if(enemyDesignSchoolNearby) minerData.setCurrentRole(MinerData.ROLE_BLOCK);
		else if(rc.getLocation().equals(minerData.getVaporatorBuildMinerLocation()) && landscaperOnWall) minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
		else if(!vaporatorBuilt && buildingIsAffordable(RobotType.VAPORATOR, 0.95f)) minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
		else if(vaporatorBuilt && !designSchoolBuilt && buildingIsAffordable(RobotType.DESIGN_SCHOOL, 0.8f)) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
//		else if(vaporatorBuilt && !fulfillmentCenterBuilt && buildingIsAffordable(RobotType.FULFILLMENT_CENTER)) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
//		else if (rc.getRoundNum() % 3 == 0) data.setCurrentRole(MinerData.ROLE_SCOUT);
		else if(rc.getRoundNum() <= 2) minerData.setCurrentRole(MinerData.ROLE_RUSH);
		else minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
		
		System.out.println("Initial role set to " + minerData.getCurrentRole());
	}
	
	private boolean buildingIsAffordable(RobotType type, float costThreshold) {
		return rc.getTeamSoup() >= (int) ((float) type.cost * costThreshold);
	}
	
	private void updateRole() throws GameActionException {
		boolean vaporatorBuilt = minerData.isVaporatorBuilt();
		boolean designSchoolBuilt = minerData.isDesignSchoolBuilt();
		boolean fulfillmentCenterBuilt = minerData.isFulfillmentCenterBuilt();
		
		if(!vaporatorBuilt && buildingIsAffordable(RobotType.VAPORATOR, 0.95f)) minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
		else if(vaporatorBuilt && !designSchoolBuilt && buildingIsAffordable(RobotType.DESIGN_SCHOOL, 0.8f)) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
//		else if(!fulfillmentCenterBuilt && buildingIsAffordable(RobotType.FULFILLMENT_CENTER)) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
		else if(isFloodingImminent(rc.getLocation(), 100) && minerData.getOutpostLocs().size() > 0) minerData.setCurrentRole(MinerData.ROLE_OUTPOST_KEEPER);
		else if(vaporatorBuilt && minerData.getRefineryLocs().size() == 0) minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
		else minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
	}
	
	private void respondToThreats() {
		if(minerData.getCurrentRole() == MinerData.ROLE_RUSH) return;
		
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		for(RobotInfo enemy : enemies) {
			if(enemy.getType().canBePickedUp() || enemy.getType() == RobotType.DESIGN_SCHOOL) {
				if(enemy.getLocation().isWithinDistanceSquared(minerData.getHqLocation(), 35) && !minerData.isFulfillmentCenterBuilt()) minerData.setCurrentRole(MinerData.ROLE_DEFENSE);
			}
		}
	}
	
	//PROTOCOLS
	/**
	 * Builds a design school and then switches to a soup miner
	 * @throws GameActionException
	 */
	private void designMinerProtocol() throws GameActionException {
		System.out.println("---DESIGN MINER PROTOCOL---");
		
		if(minerData.isDesignSchoolBuilt()) {
			//TODO: We may want other design schools. We may not want to jump ship on all of them because the first has been built.
			updateRole();
			return;
		} else {
			if(buildProtocol(RobotType.DESIGN_SCHOOL, 9, 18, 1, Robot.Type.TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC)) {
				System.out.println("Sent design school transaction.");
				updateRole();
			} else {
				routeTo(minerData.getHqLocation());
			}
		}
    	
		//TODO: FIX THIS!!!
		/*
    	if(buildDesignSchoolForClearance()) {
    		//If the previous conditions failed, then the design school build site is obstructed. We need to build a design school anywhere we can so that the landscaper can clear obstructions.
    		if(minerData.getBuildSitesBlocked()) this.sendTransaction(3, Type.TRANSACTION_BLOCKED_BUILD_SITE_ADDRESSED, buildLocation);
    		updateRole();
    	}
    	*/
    }
    
    private void fulfillmentMinerProtocol() throws GameActionException {    
    	System.out.println("---FULFILLMENT PROTOCOL---");
    	
    	MapLocation fulfillmentCenterBuildSite = minerData.getFulfillmentCenterBuildSite();
    	
    	if(minerData.isFulfillmentCenterBuilt()) {
    		updateRole();
    	} else if(rc.getLocation().equals(fulfillmentCenterBuildSite)) {
    		//Move off of fulfillment center build site.
    		System.out.println("\tMoving off of FC site.");
    		moveAnywhere();
    		return;
    	} else if(rc.getLocation().isWithinDistanceSquared(fulfillmentCenterBuildSite, 3)) {
    		System.out.println("\tAttempting to build FC.");
    		if(attemptConstruction(RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(fulfillmentCenterBuildSite))) updateRole();
    		return;
    	} else {
    		routeTo(fulfillmentCenterBuildSite);
    		return;
    	}
    }
    
    private void refineryMinerProtocol() throws GameActionException {
		System.out.println("---REFINERY PROTOCOL---");
		
		//If we have found another refinery via reading transactions, go back to soup mining.
		//TODO: Should we only accept refineries within a certain distance? Is it worth paying 200 more soup?
		for(MapLocation refineryLocation : minerData.getRefineryLocs()) {
			if(!refineryLocation.equals(minerData.getHqLocation()) && refineryLocation.isWithinDistanceSquared(rc.getLocation(), 256)) {
				updateRole();
				return;
			}
		}
		
		if (minerData.getRefineryLocs().size() > 1 || (minerData.getRefineryLocs().size() == 1 && !minerData.hqRefineryStored())) {
			updateRole();
			return;
		}
		
		if(isOnWall(rc.getLocation(), minerData.getSpawnerLocation())) {
			//Move away from range of the wall.
			moveFrom(minerData.getSpawnerLocation());
			return;
		}

    	if(oughtBuildRefinery()) {
    		MapLocation refineryBuildSite = getBuildSiteNearWall(minerData.getHqLocation(), 18, Integer.MAX_VALUE);
    		
    		 if(refineryBuildSite != null && attemptConstruction(RobotType.REFINERY, rc.getLocation().directionTo(refineryBuildSite))) {
	    		MapLocation refineryLocation = senseUnitType(RobotType.REFINERY, rc.getTeam(), 3).getLocation();
	    		minerData.addRefineryLoc(refineryLocation);
	    		
	    		sendTransaction(2, Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC, refineryLocation);
	    		updateRole();
    		} else if(minerData.getSoupLocs().size() > 0) {
    			MapLocation closestSoupLocation = closestLocationAwayFromHq(minerData.getSoupLocs(), rc.getLocation(), 9);
    			if(closestSoupLocation != null && !routeTo(closestSoupLocation)) minerData.removeSoupLoc(closestSoupLocation);
    		}
    	} else {
    		//If you ought not build a refinery right now, keep doing soup miner stuff!
    		if(minerData.getRefineryLocs().size() > 0 && rc.getTeamSoup() + rc.getSoupCarrying() >= RobotType.REFINERY.cost) fullMinerProtocol();
    		else if(rc.getTeamSoup() < RobotType.MINER.soupLimit) emptyMinerProtocol();
    	}
    }
     
    private void vaporatorMinerProtocol() throws GameActionException {
		System.out.println("---VAPORATOR PROTOCOL---");
		
		//TODO: In the case that the devs are absolutely evil and decide to place the HQs RIGHT NEXT TO EACH OTHER, this may or may not be a problem. We can decide if we want to do anything aobut this.
		
		//TODO: Make this clause general.
		RobotInfo enemyHq = senseUnitType(RobotType.HQ, rc.getTeam().opponent(), 8);
		boolean enemyHqAdjacent = enemyHq != null;
		if(enemyHqAdjacent) {
			minerData.setEnemyHqLocation(enemyHq.getLocation());
			minerData.setCurrentRole(MinerData.ROLE_DRONE_RUSH_FINISHER);
			return;
		}
		
		if(minerData.isVaporatorBuilt()) {
			//TODO: We may want other design schools. We may not want to jump ship on all of them because the first has been built.
			updateRole();
			return;
		} else {
			if(buildProtocol(RobotType.VAPORATOR, 9, Integer.MAX_VALUE, 1, Robot.Type.TRANSACTION_FRIENDLY_VAPORATOR_AT_LOC)) {
				System.out.println("Sent vaporator transaction.");
				updateRole();
			} else {
				routeTo(minerData.getHqLocation());
			}
		}
    }

	/**
	 * Builds fulfillment center near HQ
	 * @throws GameActionException
	 */
	private void defenseMinerProtocol() throws GameActionException {
		System.out.println("defense protocol");
		
		if(senseUnitType(RobotType.NET_GUN, rc.getTeam().opponent()) != null) {
			defensiveDesignSchoolBuild();
			defensiveFulfillmentCenterBuild();
		} else {
			defensiveFulfillmentCenterBuild();
			defensiveDesignSchoolBuild();
		}
		
		defensiveHqBlock();
		
		//A defensive miner can still mine.
		Direction adjacentSoupDirection = getAdjacentSoupDirection();
		
		if(adjacentSoupDirection != Direction.CENTER && rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
			mineRawSoup(getAdjacentSoupDirection());
			return;
		} else if(rc.getSoupCarrying() > 0 && getAdjacentRefineryDirection() != Direction.CENTER) {
    		depositRawSoup(rc.getLocation().directionTo(minerData.getHqLocation()));
    		return;
    	}
    }
	
	/**
	 * Miner whose soup carrying capacity is full
	 * @throws GameActionException
	 */
	private void fullMinerProtocol() throws GameActionException {
    	System.out.println("---FULL PROTOCOL---");
    	
    	updateRefineryLocations();
    	
		//Immediately try to deposit into an adjacent refinery.
		Direction adjacentRefineryDirection = getAdjacentRefineryDirection();
		
		if (adjacentRefineryDirection != null) {
			depositRawSoup(adjacentRefineryDirection);
			minerData.addRefineryLoc(rc.getLocation().add(adjacentRefineryDirection));			
		}
						
		RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
		System.out.println("We have taken note of " + minerData.getRefineryLocs().size() + " refiner(ies).");
		
		if(hq != null && !findVacanciesOnWall(minerData.getHqLocation())) {
			System.out.println("FULL MINER REMOVED HQ AS REFINERY");
			minerData.removeRefineryLoc(hq.getLocation());
		}
		
		if(minerData.getRefineryLocs().size() > 0) routeTo(locateClosestLocation(minerData.getRefineryLocs(), rc.getLocation()));
		
		updateRole();
    }

	/**
	 * Miner under soup carrying limit
	 * @throws GameActionException
	 */
	private void emptyMinerProtocol() throws GameActionException {		
    	System.out.println("---EMPTY PROTOCOL---");
    	
    	updateRefineryLocations();
    	
    	RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
    	
    	if(hq != null) {
    		if(!wallOccupied(minerData.getHqLocation())) {
    			System.out.println("EMPTY MINER REMOVED HQ AS REFINERY");
    			minerData.removeRefineryLoc(hq.getLocation());
    		}
    		
    		updateRole();
    	}
    	
    	if(!mineRawSoup(getAdjacentSoupDirection())) {
    		System.out.println("Failed to mine adjacent soup.");
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
		System.out.println("---RUSH MINER PROTOCOL---");
		findNearbySoup();
		if (rc.getRoundNum() > 250) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
		
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
	
	
	private void droneRushFinisherProtocol() throws GameActionException {
		System.out.println("---DRONE RUSH FINISHER PROTOCOL---");
		
		waitUntilReady();
		
		if(senseUnitType(RobotType.NET_GUN, rc.getTeam()) != null) return;
		if(senseUnitType(RobotType.DELIVERY_DRONE, rc.getTeam().opponent()) == null) return;
		
		for(Direction direction : Robot.directions) {
			if(rc.canBuildRobot(RobotType.NET_GUN, direction)) {
				rc.buildRobot(RobotType.NET_GUN, direction);
				System.out.println("Built a net gun!");
				return;
			} else {
				System.out.println("Cannot build a robot to the " + direction);
			}
		}
		
		System.out.println("Net gun build failed. (" + rc.getCooldownTurns() + ")");
		if(minerData.getEnemyHqLocation() != null && !rc.getLocation().isWithinDistanceSquared(minerData.getEnemyHqLocation(), 3)) routeTo(minerData.getEnemyHqLocation());
	}
	
	
	private void outpostKeeperProtocol() throws GameActionException {
		System.out.println("---OUTPOST KEEPER PROTOCOL---");
		
		RobotInfo outpostLandscaper = senseUnitType(RobotType.LANDSCAPER, rc.getTeam(), 1);
		if(outpostLandscaper != null && !isOnWall(outpostLandscaper.getLocation(), minerData.getHqLocation())) {
			if(!isClosestMinerTo(outpostLandscaper.getLocation())) {
				continueSearch();
			} else {
				if(senseUnitType(RobotType.VAPORATOR, rc.getTeam(), 3) == null) {
					MapLocation buildSite = getAdjacentBuildSite();
					if(buildSite != null) {
						int elevation = rc.senseElevation(buildSite);
						if(getFloodingAtRound(rc.getRoundNum() + (RobotType.VAPORATOR.cost / 2)) < elevation) attemptConstruction(RobotType.VAPORATOR, rc.getLocation().directionTo(buildSite));
					}
				} else if(!minerData.isFulfillmentCenterBuilt()) {
					MapLocation buildSite = getAdjacentBuildSite();
					if(buildSite != null) {
						if(attemptConstruction(RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(buildSite))) {
							minerData.setFulfillmentCenterBuilt(true);
							this.sendTransaction(1, Type.TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC, buildSite);
						}
					}
				}
			}
			
			return;
		}
		
		RobotInfo[] adjacentLandscapers = this.senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam(), 3);
		
		for(RobotInfo landscaper : adjacentLandscapers) {
			if(!isOnWall(landscaper.getLocation(), minerData.getHqLocation()) && isClosestMinerTo(landscaper.getLocation())) {
				System.out.println("I\'m closest to the landscaper at " + landscaper.getLocation() + "! Heading there...");
				if(!rc.getLocation().isWithinDistanceSquared(landscaper.getLocation(), 1)) {
					routeTo(landscaper.getLocation());
					return;
				} else {
					//We are next to a landscaper already.
					return;
				}
			}
		}
		
		if(minerData.getSearchDirection() == Direction.CENTER) minerData.setSearchDirection(getRandomNonCenterDirection());
		System.out.println("I will search for landscapers to the " + minerData.getSearchDirection());
		continueSearch();
	}
	
	//ROUTING AND LANDMARKS
	private void updateRefineryLocations() {
		RobotInfo[] refineries = this.senseAllUnitsOfType(RobotType.REFINERY, rc.getTeam());
		for(RobotInfo refinery : refineries) {
			minerData.addRefineryLoc(refinery.getLocation());
		}
	}
	
	private boolean isClosestMinerTo(MapLocation location) {
		RobotInfo[] miners = this.senseAllUnitsOfType(RobotType.MINER, rc.getTeam());
		int thisDistanceSquared = rc.getLocation().distanceSquaredTo(location);
		
		for(RobotInfo miner : miners) {
			int thatDistanceSquared = miner.getLocation().distanceSquaredTo(location);
			if(thatDistanceSquared < thisDistanceSquared) return false;
			if(thatDistanceSquared == thisDistanceSquared) {
				//To break ties, favor the robot on the bottom-right.
				if(miner.getLocation().x > rc.getLocation().x || miner.getLocation().y < rc.getLocation().y) return false;
			}
		}
		
		return true;
	}
    
	private MapLocation closestLocationAwayFromHq(ArrayList<MapLocation> locations, MapLocation reference, int minimumSquaredDistance) {
	    	MapLocation closestLocation = null;
	    	int closestDistance = Integer.MAX_VALUE;
	    	
	    	for(MapLocation location : locations) {
	    		int distanceSquared = location.distanceSquaredTo(reference);
	    		if(minimumSquaredDistance <= distanceSquared && distanceSquared < closestDistance) closestLocation = location;
	    	}
	    	
	    	return closestLocation;
	    }
	
    
	private void moveFrom(MapLocation location) throws GameActionException {
    	Direction fromLocationDirection = location.directionTo(rc.getLocation());
    	routeTo(rc.getLocation().add(fromLocationDirection));
    	data.setSearchDirection(fromLocationDirection);
    }
	
    //MINING
    /**
	 * Mines soup if able
	 * @param dir Direction
	 * @throws GameActionException
	 */
	private boolean mineRawSoup(Direction dir) throws GameActionException {
		waitUntilReady();

		if(rc.isReady() && rc.canMineSoup(dir)) {
			rc.setIndicatorDot(rc.getLocation().add(dir), 0, 255, 0);
			rc.mineSoup(dir);
			return true;
		} else {
			return false;
		}
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
	
	private Direction getAdjacentSoupDirection() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		Direction mostSoupDirection = Direction.CENTER;
		int mostSoupLocated = 0;
		
		for(Direction direction : Direction.allDirections()) {
			if (rc.canSenseLocation(rcLocation.add(direction))) {
				int foundSoup = rc.senseSoup(rcLocation.add(direction));
				if(foundSoup > mostSoupLocated) {
					mostSoupDirection = direction;
					mostSoupLocated = foundSoup;
					System.out.println("Found " + mostSoupLocated + " soup to the " + mostSoupDirection + " at " + rcLocation.add(direction));
				}
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
				if(rc.senseSoup(allegedSoupLocation) == 0 || (wallBuilt(minerData.getHqLocation()) && isWithinWall(allegedSoupLocation, minerData.getHqLocation()))) {
					minerData.removeSoupLoc(allegedSoupLocation);
					i--;
				}
			}
		}
	}
	
	//BUILDING
	private MapLocation getClosestBuildableLocation(MapLocation buildSite) throws GameActionException {
		if(!rc.canSenseLocation(buildSite)) return null;
		int buildSiteElevation = rc.senseElevation(buildSite);
		
		int[] distances = new int[Robot.directions.length];
		
		for(int i = 0; i < distances.length; i++) {
			Direction direction = Robot.directions[i];
			MapLocation adjacentLocation = buildSite.add(direction);
			
			if(rc.canSenseLocation(adjacentLocation)) {
				int adjacentLocationElevation = rc.senseElevation(adjacentLocation);
				int elevationDifference = Math.abs(adjacentLocationElevation - buildSiteElevation);
				
				distances[i] = (elevationDifference <= GameConstants.MAX_DIRT_DIFFERENCE) ? rc.getLocation().distanceSquaredTo(adjacentLocation) : Integer.MAX_VALUE;
			}
		}
		
		int closestDistanceIndex = NumberMath.indexOfLeast(distances);
		int closestDistance = distances[closestDistanceIndex];
		
		return closestDistance < Integer.MAX_VALUE ? buildSite.add(Robot.directions[closestDistanceIndex]) : null;
	}
	

	private boolean buildProtocol(RobotType buildType, int minimumWallDistance, int maximumWallDistance, int transactionSoupBid, Robot.Type transactionType) throws GameActionException {
		MapLocation buildSite = getBuildSiteNearWall(minerData.getHqLocation(), minimumWallDistance, maximumWallDistance);
		
		if(buildSite != null && attemptConstruction(buildType, rc.getLocation().directionTo(buildSite))) {
			sendTransaction(transactionSoupBid, transactionType, buildSite);
			return true;
		}
		
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
	
	
	private boolean oughtBuildVaporator() {
		return rc.getTeamSoup() >= RobotType.VAPORATOR.cost;
	}
	
	private boolean attemptVaporatorConstruction() throws GameActionException {		
		Direction buildDirection = rc.getLocation().directionTo(minerData.getVaporatorBuildSite());
		
		if(rc.canBuildRobot(RobotType.VAPORATOR, buildDirection)) {
			rc.buildRobot(RobotType.VAPORATOR, buildDirection);
			minerData.setVaporatorBuilt(true);
			return true;
		}
		
		System.out.println("Failed to build vaporator...");
		
		return false;
	}
	
	private boolean oughtBuildNetGun() {
		return rc.getTeamSoup() >= RobotType.NET_GUN.cost && this.senseUnitType(RobotType.DELIVERY_DRONE, rc.getTeam().opponent()) != null;
	}
	
	private boolean attemptNetGunConstruction() throws GameActionException {
		if(minerData.getNetGunBuildSite() == null) return false;
		Direction buildDirection = rc.getLocation().directionTo(minerData.getNetGunBuildSite());
		
		if(rc.canBuildRobot(RobotType.NET_GUN, buildDirection) && rc.getLocation().add(buildDirection).distanceSquaredTo(data.getSpawnerLocation()) <= 3) {
			rc.buildRobot(RobotType.NET_GUN, buildDirection);
			minerData.setNetGunBuilt(true);
			return true;
		}
		
		System.out.println("Failed to build net gun...");
		
		return false;
	}
	
	
	//DEFENSE
	private void defensiveFulfillmentCenterBuild() throws GameActionException {
		MapLocation fulfillmentCenterBuildSite = minerData.getFulfillmentCenterBuildSite();
		if(rc.canSenseLocation(fulfillmentCenterBuildSite)) {
			RobotInfo fulfillmentCenter = rc.senseRobotAtLocation(fulfillmentCenterBuildSite);
			if(fulfillmentCenter != null && fulfillmentCenter.getType() == RobotType.FULFILLMENT_CENTER) {
				System.out.println("Fulfillment center confirmed built!");
				minerData.setFulfillmentCenterBuilt(true);
				return;
			}
		}
		
		if(rc.getLocation().equals(fulfillmentCenterBuildSite) && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
			//TODO: This rudimentary move is a bit of a risk, but it's intended to allow for the building of a fulfillment center to carry the enemies away.
			//We will likely need to add logic to make this work as desired, and may even need to draw upon other miners building other fulfillment centers.
			moveFrom(minerData.getHqLocation());
		} else if(rc.getLocation().isWithinDistanceSquared(fulfillmentCenterBuildSite, 3) && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
			System.out.println("Attempting to build fulfillment center...");
			if(attemptConstruction(RobotType.FULFILLMENT_CENTER, rc.getLocation().directionTo(fulfillmentCenterBuildSite))) minerData.setFulfillmentCenterBuilt(true);
		} else if(!rc.getLocation().isWithinDistanceSquared(minerData.getSpawnerLocation(), 3)) {
			System.out.println("Routing to HQ...");
			routeTo(data.getSpawnerLocation());
			//TODO: Add logic to favor routing to locations that are closest to the most enemies.
		}
	}
	
	private void defensiveDesignSchoolBuild() throws GameActionException {
		MapLocation designSchoolBuildSite = minerData.getDesignSchoolBuildSite();
		if(rc.canSenseLocation(designSchoolBuildSite)) {
			RobotInfo designSchool = rc.senseRobotAtLocation(designSchoolBuildSite);
			if(designSchool != null && designSchool.getType() == RobotType.DESIGN_SCHOOL) {
				System.out.println("Design school confirmed built!");
				minerData.setDesignSchoolBuilt(true);
				return;
			}
		}
		
		if(rc.getLocation().equals(designSchoolBuildSite) && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
			//TODO: This rudimentary move is a bit of a risk, but it's intended to allow for the building of a fulfillment center to carry the enemies away.
			//We will likely need to add logic to make this work as desired, and may even need to draw upon other miners building other fulfillment centers.
			moveFrom(minerData.getHqLocation());
		} else if(rc.getLocation().isWithinDistanceSquared(designSchoolBuildSite, 3) && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
			System.out.println("Attempting to build fulfillment center...");
			if(attemptConstruction(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(designSchoolBuildSite))) minerData.setDesignSchoolBuilt(true);
		} else if(!rc.getLocation().isWithinDistanceSquared(minerData.getSpawnerLocation(), 3)) {
			System.out.println("Routing to HQ...");
			routeTo(data.getSpawnerLocation());
			//TODO: Add logic to favor routing to locations that are closest to the most enemies.
		}
	}
	

	private void defensiveHqBlock() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		System.out.println("Scouting nearby region yielded " + enemies.length + " enemies.");
		if(enemies.length < 2) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
		else if(!rc.getLocation().isWithinDistanceSquared(minerData.getSpawnerLocation(), 3)) routeTo(minerData.getSpawnerLocation());
	}
	
	//TRANSACTIONS
	private void readTransactions() throws GameActionException {
    	for(int i = NumberMath.clamp(minerData.getTransactionRound(), 1, Integer.MAX_VALUE); i < rc.getRoundNum(); i++) {
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
    					case TRANSACTION_SOUP_AT_LOC:
    						minerData.addSoupLoc(loc);
    						System.out.println("read soup loc " + loc);
    						break;
    					case TRANSACTION_FRIENDLY_REFINERY_AT_LOC:
    						minerData.addRefineryLoc(loc);
    						System.out.println("read refinery loc " + loc);
    						break;
    					case TRANSACTION_FRIENDLY_DESIGN_SCHOOL_AT_LOC:
    						minerData.setDesignSchoolBuilt(true);
    						System.out.println("Design school build confirmed.");
    						break;
    					case TRANSACTION_FRIENDLY_FULFILLMENT_CENTER_AT_LOC:
    						minerData.setFulfillmentCenterBuilt(true);
    						System.out.println("Fulfillment center build confirmed.");
    						break;
    					case TRANSACTION_FRIENDLY_VAPORATOR_AT_LOC:
    						minerData.setVaporatorBuilt(true);
    						System.out.println("Vaporator build confirmed.");
    						break;
    					case TRANSACTION_BUILD_SITE_BLOCKED:
    						minerData.setBuildSitesBlocked(true);
    						break;
    					case TRANSACTION_BLOCKED_BUILD_SITE_ADDRESSED:
    						minerData.setBuildSitesBlocked(false);
    						break;
    					case TRANSACTION_WALL_BEING_BUILT:
    						minerData.setWallBuildHandled(true);
    						break;
    					case TRANSACTION_OUTPOST_AT_LOC:
    						minerData.addOutpostLoc(loc);
    						break;
    					default:
    						break;
    				}
    			}
    		}
    		
    		minerData.setTransactionRound(i + 1);
    	}
    }
	
	//TRACKING

	//TRACKING
	private void senseBuildings() throws GameActionException {
		RobotInfo[] refineries = this.senseAllUnitsOfType(RobotType.REFINERY, rc.getTeam());
		for(RobotInfo refinery : refineries) {
			minerData.addRefineryLoc(refinery.getLocation());
		}
	}	
	
	
	
}

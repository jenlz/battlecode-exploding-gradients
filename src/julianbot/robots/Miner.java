package julianbot.robots;

import java.util.ArrayList;

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
    		discernRole();
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
		boolean interferingWithBase = isOnWall(rc.getLocation(), minerData.getSpawnerLocation()) || isWithinWall(rc.getLocation(), minerData.getSpawnerLocation());
		return this.senseUnitType(RobotType.LANDSCAPER, rc.getTeam(), 3) != null && interferingWithBase;
	}
	
	private void respondToThreats() {
		if(minerData.getCurrentRole() == MinerData.ROLE_RUSH) return;
		
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		for(RobotInfo enemy : enemies) {
			if(enemy.getType().canBePickedUp() || enemy.getType() == RobotType.DESIGN_SCHOOL) {
				if(enemy.getLocation().isWithinDistanceSquared(data.getSpawnerLocation(), 35) && !minerData.isFulfillmentCenterBuilt()) minerData.setCurrentRole(MinerData.ROLE_DEFENSE);
			}
		}
	}
	
	/**
	 * Builds a design school and then switches to a soup miner
	 * @throws GameActionException
	 */
	private void designMinerProtocol() throws GameActionException {
		MapLocation designSchoolBuildSite = minerData.getDesignSchoolBuildSite();
		
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
    	System.out.println("fulfillment protocol");
    	
    	MapLocation fulfillmentCenterBuildSite = minerData.getFulfillmentCenterBuildSite();
    	
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
		
		//If we have found another refinery via reading transactions, go back to soup mining.
		//TODO: Should we only accept refineries within a certain distance? Is it worth paying 200 more soup?
		for(MapLocation refineryLocation : minerData.getRefineryLocs()) {
			if(!refineryLocation.equals(minerData.getHqLocation())) {
				minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
				return;
			}
		}
		
		RobotInfo refinery = senseUnitType(RobotType.REFINERY, rc.getTeam());
		if (refinery != null) {
			minerData.addRefineryLoc(refinery.getLocation());
			minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
			return;
		}
		
		if(isOnWall(rc.getLocation(), minerData.getSpawnerLocation())) {
			//Move away from range of the wall.
			moveMinerFromHQ();
			return;
		}

    	if(oughtBuildRefinery()) {
    		 if(attemptRefineryConstruction()) {
	    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
	    		
	    		MapLocation refineryLocation = senseUnitType(RobotType.REFINERY, rc.getTeam(), 3).getLocation();
	    		minerData.addRefineryLoc(refineryLocation);
	    		
	    		//TODO: Is this too much soup for this transaction?
	    		sendTransaction(10, Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC, refineryLocation);
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
    
    private MapLocation closestLocationAwayFromHq(ArrayList<MapLocation> locations, MapLocation reference, int minimumSquaredDistance) {
    	MapLocation closestLocation = null;
    	int closestDistance = Integer.MAX_VALUE;
    	
    	for(MapLocation location : locations) {
    		int distanceSquared = location.distanceSquaredTo(reference);
    		if(minimumSquaredDistance <= distanceSquared && distanceSquared < closestDistance) closestLocation = location;
    	}
    	
    	return closestLocation;
    }
    
    private void vaporatorMinerProtocol() throws GameActionException {
		System.out.println("vaporator protocol");
		
		MapLocation vaporatorBuildMinerLocation = minerData.getVaporatorBuildMinerLocation();
		MapLocation vaporatorBuildSite = minerData.getVaporatorBuildSite();
		
		if(vaporatorBuildMinerLocation == null || vaporatorBuildSite == null) {
			minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
			return;
		}
		
		if(rc.canSenseLocation(vaporatorBuildMinerLocation) && !rc.getLocation().equals(vaporatorBuildMinerLocation)) {
			RobotInfo potentialVaporatorMiner = rc.senseRobotAtLocation(vaporatorBuildMinerLocation);
			if(potentialVaporatorMiner != null && potentialVaporatorMiner.type == RobotType.MINER) {
				minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
				return;
			}
		}
		
		if(!rc.getLocation().equals(vaporatorBuildMinerLocation)) {
			routeTo(vaporatorBuildMinerLocation);
			return;
		} else if(!canSenseHubDesignSchool() && rc.getLocation().isWithinDistanceSquared(minerData.getDesignSchoolBuildSite(), 3)) {
			attemptDesignSchoolConstruction(rc.getLocation().directionTo(minerData.getDesignSchoolBuildSite()));
		} else if(!canSenseHubFulfillmentCenter() && rc.getLocation().isWithinDistanceSquared(minerData.getFulfillmentCenterBuildSite(), 3)) {
			attemptFulfillmentCenterConstruction(rc.getLocation().directionTo(minerData.getFulfillmentCenterBuildSite()));
		} if(oughtBuildVaporator()) {
	    	attemptVaporatorConstruction();
	    	return;
    	}
		
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
    }
	
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
			moveMinerFromHQ();
		} else if(rc.getLocation().isWithinDistanceSquared(fulfillmentCenterBuildSite, 3) && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost) {
			System.out.println("Attempting to build fulfillment center...");
			if(attemptFulfillmentCenterConstruction(rc.getLocation().directionTo(fulfillmentCenterBuildSite))) minerData.setFulfillmentCenterBuilt(true);
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
			moveMinerFromHQ();
		} else if(rc.getLocation().isWithinDistanceSquared(designSchoolBuildSite, 3) && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
			System.out.println("Attempting to build fulfillment center...");
			if(attemptDesignSchoolConstruction(rc.getLocation().directionTo(designSchoolBuildSite))) minerData.setDesignSchoolBuilt(true);
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
	
	/**
	 * Miner whose soup carrying capacity is full
	 * @throws GameActionException
	 */
	private void fullMinerProtocol() throws GameActionException {
    	System.out.println("full protocol");
    	
    	updateRefineryLocations();
    	
		//Immediately try to deposit into an adjacent refinery.
		Direction adjacentRefineryDirection = getAdjacentRefineryDirection();
		
		if (adjacentRefineryDirection != null) {
			depositRawSoup(adjacentRefineryDirection);
			minerData.addRefineryLoc(rc.getLocation().add(adjacentRefineryDirection));
			
			if(setRoleAccordingToBuildPriority()) return;
		}
						
		RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
		RobotInfo landscaper = senseUnitType(RobotType.LANDSCAPER, rc.getTeam());
		
		if(hq != null) {
			if(landscaper != null) {
				System.out.println("Miner sees a landscaper! It may become a vaporator builder.");
				
				minerData.removeRefineryLoc(hq.getLocation());
								
				if(oughtBecomeVaporatorMiner()) {
					System.out.println("This miner ought become a vaporator miner.");
					routeTo(minerData.getVaporatorBuildMinerLocation());
					minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
					return;
				} else {
					System.out.println("This miner ought NOT become a vaporator miner.");
					minerData.setVaporatorBuilderClaimed(true);
					
		    		if(minerData.getRefineryLocs().size() == 0) {
		        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
		        		return;
		        	}
				}
			} else {
				System.out.println("No landscaper present.");
				setRoleAccordingToBuildPriority();
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
    	
    	updateRefineryLocations();
    	
    	RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());
    	RobotInfo fulfillmentCenter = senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam());
    	RobotInfo landscaper = senseUnitType(RobotType.LANDSCAPER, rc.getTeam());
    	
    	if(landscaper != null) {
    		System.out.println("Miner sees a landscaper! It may become a vaporator builder.");
    		MapLocation vaporatorBuildMinerLocation = minerData.getVaporatorBuildMinerLocation();
    		
    		if(oughtBecomeVaporatorMiner()) {
    			System.out.println("Becoming a vaporator builder!");
    			routeTo(vaporatorBuildMinerLocation);
    			minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
    			return;
    		} else {
    			minerData.setVaporatorBuilderClaimed(true);
    			
    			if(isOnWall(rc.getLocation(), minerData.getSpawnerLocation())) {
		        	moveMinerFromHQ();
		        	minerData.removeRefineryLoc(data.getSpawnerLocation());
		        	if(minerData.getRefineryLocs().size() == 0) {
		        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
		        	}
		        	return;
	    		}
    		}
    	} else if(fulfillmentCenter != null) {
    		MapLocation vaporatorBuildSite = minerData.getVaporatorBuildSite();
    		
    		if(oughtBecomeVaporatorMiner()) {
    			routeTo(vaporatorBuildSite);
    			minerData.setCurrentRole(MinerData.ROLE_VAPORATOR_BUILDER);
    			return;
    		} else {
    			minerData.setVaporatorBuilderClaimed(true);
    			
    			if(isOnWall(rc.getLocation(), minerData.getSpawnerLocation())){
	    			moveMinerFromHQ();
	    			return;
	    		}
    		}
    	}
    	
    	if(hq != null) {
    		if(setRoleAccordingToBuildPriority()) return;
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
	
	private boolean oughtBecomeVaporatorMiner() throws GameActionException {
		MapLocation vaporatorBuildMinerLocation = minerData.getVaporatorBuildMinerLocation();
		RobotInfo buildSiteOccupant = (rc.canSenseLocation(vaporatorBuildMinerLocation) && !rc.getLocation().equals(vaporatorBuildMinerLocation)) ? rc.senseRobotAtLocation(vaporatorBuildMinerLocation) : null;
				
		return !minerData.isVaporatorBuilderClaimed() && (buildSiteOccupant == null || buildSiteOccupant.getType() != RobotType.MINER) && isClosestMinerTo(vaporatorBuildMinerLocation);
	}
	
	private void updateRefineryLocations() {
		RobotInfo[] refineries = this.senseAllUnitsOfType(RobotType.REFINERY, rc.getTeam());
		for(RobotInfo refinery : refineries) {
			minerData.addRefineryLoc(refinery.getLocation());
		}
	}
	
	private boolean setRoleAccordingToBuildPriority() {
		RobotType buildPriority = getBuildPriority();
		
		if(buildPriority == RobotType.REFINERY && rc.getTeamSoup() >= RobotType.REFINERY.cost && minerData.getCurrentRole() != MinerData.ROLE_REFINERY_BUILDER) {
			System.out.println("\tSetting role to refinery builder");
			minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
    		return true;
		} else if(buildPriority == RobotType.DESIGN_SCHOOL && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && isClosestMinerTo(minerData.getDesignSchoolBuildSite()) && minerData.getCurrentRole() != MinerData.ROLE_DESIGN_BUILDER) {
			System.out.println("\tSetting role to design school builder");
    		minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
    		return true;
		} else if(buildPriority == RobotType.FULFILLMENT_CENTER && rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && isClosestMinerTo(minerData.getDesignSchoolBuildSite()) && minerData.getCurrentRole() != MinerData.ROLE_FULFILLMENT_BUILDER) {
			System.out.println("\tSetting role to fulfillmentCenter builder");
    		minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
    		return true;
		}
		
		return false;
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
	
	private RobotType getBuildPriority() {
		if(minerData.getSoupLocs().size() > 3 && minerData.getRefineryLocs().size() == 1 && minerData.getRefineryLocs().contains(minerData.getSpawnerLocation())) return RobotType.REFINERY;
		if(!minerData.isDesignSchoolBuilt()) return RobotType.DESIGN_SCHOOL;
		if(!minerData.isFulfillmentCenterBuilt()) return RobotType.FULFILLMENT_CENTER;
		return null;
	}
	
	private boolean canSenseHubDesignSchool() throws GameActionException {
		if(!rc.canSenseLocation(minerData.getDesignSchoolBuildSite())) return false;
		
		RobotInfo designSchoolInfo = rc.senseRobotAtLocation(minerData.getDesignSchoolBuildSite());
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
		Direction buildDirection = rc.getLocation().directionTo(minerData.getVaporatorBuildSite());
		
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
	
	private boolean canSenseHubFulfillmentCenter() throws GameActionException {
		if(!rc.canSenseLocation(minerData.getFulfillmentCenterBuildSite())) return false;
		
		RobotInfo fulfillmentCenterInfo = rc.senseRobotAtLocation(minerData.getFulfillmentCenterBuildSite());
		if(fulfillmentCenterInfo == null) return false;
		return fulfillmentCenterInfo.type == RobotType.FULFILLMENT_CENTER;
	}
	
	private void readTransactions() throws GameActionException {
    	for(int i = minerData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		minerData.setTransactionRound(i);
    		
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
    					default:
    						break;
    				}
    			}
    		}
    	}
    }
	
}

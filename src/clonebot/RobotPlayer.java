package clonebot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import clonebot.commands.DesignSchoolCommands;
import clonebot.commands.DroneCommands;
import clonebot.commands.FulfillmentCenterCommands;
import clonebot.commands.GeneralCommands;
import clonebot.commands.GeneralCommands.Type;
import clonebot.commands.HQCommands;
import clonebot.commands.LandscaperCommands;
import clonebot.commands.MinerCommands;
import clonebot.robotdata.DesignSchoolData;
import clonebot.robotdata.DroneData;
import clonebot.robotdata.FulfillmentCenterData;
import clonebot.robotdata.HQData;
import clonebot.robotdata.LandscaperData;
import clonebot.robotdata.MinerData;
import clonebot.robotdata.NetGunData;
import clonebot.robotdata.RobotData;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static RobotData robotData;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.	
        System.out.println("Initializing Robot.");
    	RobotPlayer.rc = rc;
        robotData = initializeRobotData(rc.getType());
        turnCount = 0;
        
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
            	if(robotData.hasPendingTransaction()) GeneralCommands.sendPendingTransaction(rc, robotData);
            	
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                    default:                                         break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
    
    static RobotData initializeRobotData(RobotType type) {
    	RobotData robotData = null;

    	switch(type) {
    		case HQ:                 robotData = new HQData(rc);                 break;
    		case MINER:              robotData = new MinerData(rc);              break;
    		case DESIGN_SCHOOL:      robotData = new DesignSchoolData(rc);       break;
    		case LANDSCAPER:         robotData = new LandscaperData(rc);         break;
    		case FULFILLMENT_CENTER: robotData = new FulfillmentCenterData(rc);  break;
    		case DELIVERY_DRONE:     robotData = new DroneData(rc);              break;
    		case NET_GUN:            robotData = new NetGunData(rc);             break;
    		default:                 robotData = new RobotData(rc);              break;
    	}

    	return robotData;
    }

    static void runHQ() throws GameActionException {
    	HQData hqData = (HQData) robotData;
    	
    	if(rc.getRoundNum() == 1) {
    		HQCommands.makeInitialReport(rc);
    		HQCommands.setBuildDirectionTowardsSoup(rc, hqData);
    	}
    	    	
        if(HQCommands.oughtBuildMiner(rc, hqData)) {        	
        	HQCommands.tryBuild(rc, RobotType.MINER, hqData);
        }
                
        HQCommands.storeForeignTransactions(rc, hqData);
        if(rc.getRoundNum() % 100 == 0) HQCommands.repeatForeignTransaction(rc, hqData);        
        
        //Disabled HQ net gun for drone attack testing
        /*
        RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), hqData.getOpponent());
                
        if(enemy.length > 0) {
        	if(enemy.length > 10) {
	        	HQCommands.sendSOS(rc);
	        }
	        for(RobotInfo bullseye : enemy) { 
	        	if(bullseye.type.equals(RobotType.DESIGN_SCHOOL)) {
	        		//HQCommands.sendSOS(rc);
	        		//GeneralCommands.sendTransaction(rc, 10, Type.TRANSACTION_ENEMY_DESIGN_SCHOOL_AT_LOC, bullseye.location);
	        		hqData.setBuildDirection(rc.getLocation().directionTo(bullseye.location).rotateLeft());
	        		while(!HQCommands.tryBuild(rc, RobotType.MINER, hqData)) {
	        			
	        		}
	        	}
		    	if(rc.canShootUnit(bullseye.getID())) {
		    		HQCommands.shootUnit(rc, bullseye.getID());
		    	}
	        }
        }
        */
    }

    static void runMiner() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	if(turnCount == 1) MinerCommands.discernRole(rc, minerData);

    	//TODO: We can split this up over multiple rounds to avoid reading transactions past initial cooldown turns or finishing early, then starting again on round 9, only to finish after initial cooldown.
    	if(turnCount < GameConstants.INITIAL_COOLDOWN_TURNS) {
    		for (int i = 1; i < rc.getRoundNum(); i++) {
				MinerCommands.readTransaction(rc, minerData, rc.getBlock(i));
			}
		}
    	
		MinerCommands.readTransaction(rc, minerData, rc.getBlock(rc.getRoundNum() - 1));
      
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
	static void designMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	MapLocation designSchoolBuildSite = minerData.getSpawnerLocation().translate(-1, 0);
    	
    	RobotInfo designSchool = GeneralCommands.senseUnitType(rc, RobotType.DESIGN_SCHOOL, rc.getTeam());
    	
    	if(designSchool != null) {
    		System.out.println("\tDesign School already exists.");
    		RobotInfo fulfillmentCenter = GeneralCommands.senseUnitType(rc, RobotType.FULFILLMENT_CENTER, rc.getTeam());
    		minerData.setCurrentRole((fulfillmentCenter != null) ? MinerData.ROLE_SOUP_MINER : MinerData.ROLE_FULFILLMENT_BUILDER);
    		minerData.setDesignSchoolBuilt(true);
    		return;
    	} else if(rc.getLocation().equals(designSchoolBuildSite)) {
    		//Move off of design school build site.
    		System.out.println("\tMoving off of DS site.");
    		GeneralCommands.moveAnywhere(rc, minerData);
    		return;
    	} else if(rc.getLocation().isWithinDistanceSquared(designSchoolBuildSite, 3)) {
    		System.out.println("\tAttempting to build DS.");
    		if(MinerCommands.attemptDesignSchoolConstruction(rc, rc.getLocation().directionTo(designSchoolBuildSite))) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else {
    		GeneralCommands.routeTo(designSchoolBuildSite, rc, minerData);
    		return;
    	}
    }
    
    static void fulfillmentMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	MapLocation fulfillmentCenterBuildSite = minerData.getSpawnerLocation().translate(1, 0);
    	   
    	RobotInfo fulfillmentCenter = GeneralCommands.senseUnitType(rc, RobotType.FULFILLMENT_CENTER, rc.getTeam());
    	
    	if(fulfillmentCenter != null || rc.getTeamSoup() < RobotType.FULFILLMENT_CENTER.cost * 0.8) {
    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(rc.getLocation().equals(fulfillmentCenterBuildSite)) {
    		//Move off of fulfillment center build site.
    		GeneralCommands.moveAnywhere(rc, minerData);
    		return;
    	} else if(rc.getLocation().isWithinDistanceSquared(fulfillmentCenterBuildSite, 3)) {
    		if(MinerCommands.attemptFulfillmentCenterConstruction(rc, rc.getLocation().directionTo(fulfillmentCenterBuildSite))) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(GeneralCommands.move(rc, rc.getLocation().directionTo(fulfillmentCenterBuildSite), minerData)) {
    		return;
    	} else {
    		GeneralCommands.routeTo(fulfillmentCenterBuildSite, rc, minerData);
    		return;
    	}
    }
    
    static void refineryMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	
    	System.out.println("refinery protocol");
    	
    	RobotInfo refinery = GeneralCommands.senseUnitType(rc, RobotType.REFINERY, rc.getTeam());
    	if(refinery != null) {
    		minerData.addRefineryLoc(refinery.getLocation());
    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	}
    	
    	if(MinerCommands.oughtBuildRefinery(rc)) {
	    	if(MinerCommands.attemptRefineryConstruction(rc, minerData)) {
	    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
	    		
	    		MapLocation refineryLocation = GeneralCommands.senseUnitType(rc, RobotType.REFINERY, rc.getTeam()).getLocation();
	    		minerData.addRefineryLoc(refineryLocation);
	    		
	    		if(!GeneralCommands.sendTransaction(rc, 10, GeneralCommands.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC, refineryLocation)) {
	    			System.out.println("Refinery transaction pending!");
	    			minerData.setPendingTransaction(GeneralCommands.Type.TRANSACTION_ENEMY_REFINERY_AT_LOC, refineryLocation, 10);
	    		} else {
	    			System.out.println("Completed refinery transaction!");
	    		}
	    		
	    		return;
    		} else if(rc.getLocation().distanceSquaredTo(minerData.getSpawnerLocation()) <= 18) {
    			//Move away from range of the wall.
    			moveMinerFromHQ(minerData);
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
	static void defenseMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	
    	if(!minerData.isFulfillmentCenterBuilt()) {
    		if(MinerCommands.routeToFulfillmentCenterSite(rc, minerData)) {
    			MinerCommands.buildDefenseFulfillmentCenter(rc, minerData);
    			return;
    		}
    	}
    }

	/**
	 * Miner whose soup carrying capacity is full
	 * @throws GameActionException
	 */
	static void fullMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;

    	System.out.println("full protocol");
    	
		//Start by trying to deposit into a refinery.
		Direction adjacentRefineryDirection = MinerCommands.getAdjacentRefineryDirection(rc);
		
		if (adjacentRefineryDirection != Direction.CENTER) {
			MinerCommands.depositRawSoup(rc, adjacentRefineryDirection);
			minerData.addRefineryLoc(rc.getLocation().add(adjacentRefineryDirection));
			
			//TODO: This first condition for refinery building is not yet satisfactory. Soup locations need to be taken into account everywhere and removed when vacant before this yet has entirely desirable effects.
			System.out.println("This miner knows of " + minerData.getSoupLocs().size() + " soup location(s) and " + minerData.getRefineryLocs().size() + " refiner(ies).");
			if(MinerCommands.getBuildPriority(minerData) == RobotType.REFINERY) minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
			else if(rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && !MinerCommands.canSenseHubDesignSchool(rc, minerData)) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
			else if(rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && !MinerCommands.canSenseHubFulfillmentCenter(rc, minerData)) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
			return;
		}

		//If no refinery is adjacent, look for one.
		//RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam(), ((int) Math.sqrt(RobotType.MINER.sensorRadiusSquared)) - 2); //For pathfind, change senseUnitType if add back in
		
		//TODO: Once the landscapers get going, miners should no longer return to the HQ to refine soup. We need to communicate that via a transaction.
		
		RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam());
		RobotInfo landscaper = GeneralCommands.senseUnitType(rc, RobotType.LANDSCAPER, rc.getTeam());
		
		if(hq != null) {
			if(landscaper != null) {
				System.out.println("Moving from landscaper site.");
	    		moveMinerFromHQ(minerData);
	    		minerData.removeRefineryLoc(hq.getLocation());
	    		if(minerData.getRefineryLocs().size() == 0) {
	        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
	        	}
	    		return;
			} else {
				System.out.println("No landscaper present.");
				if(minerData.getRefineryLocs().size() > 0) {
					System.out.println("Routing to alternative refinery.");
					GeneralCommands.routeTo(GeneralCommands.locateClosestLocation(rc, minerData.getRefineryLocs(), rc.getLocation()), rc, minerData);
				} else {
					System.out.println("Switching from soup miner to refinery builder.");
					minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
				}
	    	}
		} else {
			System.out.println("The HQ cannot be detected.");
			if(minerData.getRefineryLocs().size() > 0) {
				System.out.println("Routing to alternative refinery.");
				GeneralCommands.routeTo(GeneralCommands.locateClosestLocation(rc, minerData.getRefineryLocs(), rc.getLocation()), rc, minerData);
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
	static void emptyMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	
    	System.out.println("empty protocol");
    	
    	RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam());
    	RobotInfo fulfillmentCenter = GeneralCommands.senseUnitType(rc, RobotType.FULFILLMENT_CENTER, rc.getTeam());
    	RobotInfo landscaper = GeneralCommands.senseUnitType(rc, RobotType.LANDSCAPER, rc.getTeam());
    	
    	//TODO: Miners can approach again once the landscaper gets high on a wall. We need to test for elevation to see to it that this happens.
    	if(landscaper != null) {
    		//We only need the miners to back off if the wall is not yet built.
    		if(rc.getLocation().distanceSquaredTo(minerData.getSpawnerLocation()) <= 8 && rc.senseElevation(landscaper.getLocation()) - rc.senseElevation(rc.getLocation()) <= GameConstants.MAX_DIRT_DIFFERENCE) {
	    		System.out.println("\tLandscaper detected");
	        	moveMinerFromHQ(minerData);
	        	minerData.removeRefineryLoc(hq.getLocation());
	        	if(minerData.getRefineryLocs().size() == 0) {
	        		minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
	        	}
	        	return;
    		}
    	} else if(fulfillmentCenter != null && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost && rc.getLocation().equals(minerData.getSpawnerLocation().translate(-2, 0))) {
    		//This miner is standing on the landscaper spawn point, so it needs to mvoe.
    		moveMinerFromHQ(minerData);
    	}
    	
    	//TODO: Clarify these conditionals. They're causing miners to become idle when they shouldn't be.
    	if(hq != null && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
    		if(MinerCommands.getBuildPriority(minerData) == RobotType.REFINERY) {
    			System.out.println("\tSetting role to refinery builder");
    			minerData.setCurrentRole(MinerData.ROLE_REFINERY_BUILDER);
        		return;
    		} else if(!minerData.isDesignSchoolBuilt()){
    			System.out.println("\tSetting role to design school builder");
        		minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
        		return;
    		}
    	}
    	
    	if(!MinerCommands.mineRawSoup(rc,  MinerCommands.getAdjacentSoupDirection(rc))) {
    		System.out.println("Could not mine adjacent soup.");
    		if(minerData.getSoupLocs().size() > 0) MinerCommands.refreshSoupLocations(rc, minerData);
    		if(minerData.getSoupLocs().size() == 0) MinerCommands.findNearbySoup(rc, minerData);
    		
    		if(minerData.getSoupLocs().size() > 0) {
    			MapLocation closestSoup = GeneralCommands.locateClosestLocation(rc, minerData.getSoupLocs(), rc.getLocation());
    			if(!GeneralCommands.routeTo(closestSoup, rc, minerData)) minerData.removeSoupLoc(closestSoup);
    		} else {
    			MinerCommands.continueSearch(rc, minerData);
    		}
		} else {
			System.out.println("Mined soup. (" + rc.getSoupCarrying() + ")");
		}
    }
    
    static void moveMinerFromHQ(MinerData minerData) throws GameActionException {
    	Direction fromHQDirection = minerData.getSpawnerLocation().directionTo(rc.getLocation());
    	GeneralCommands.routeTo(rc.getLocation().add(fromHQDirection), rc, minerData);
    }

	/**
	 * Searches map until it finds enemy unit, then follows that unit. Reports enemy building locations.
	 * @throws GameActionException
	 */
	static void scoutMinerProtocol() throws GameActionException {
		MinerData minerData = (MinerData) robotData;
		RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		// Scans for enemy robots, if it's a building, reports it and if it's a unit, sets it as unit to follow.
		for (RobotInfo robot : robots) {
			RobotType unitType = robot.getType();
			if (unitType.isBuilding()) {
				int soupBid = (robot.getType() == RobotType.HQ) ? 10 : 5; //HQ Location is more important than other buildings hence higher cost
				// Add check here if location already reported
				GeneralCommands.sendTransaction(rc, soupBid, GeneralCommands.getLocationType(rc, unitType, robot.getTeam()), robot.getLocation());
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
		MapLocation soupLoc = MinerCommands.getSoupLocation(rc);
		if (soupLoc != null) {
			if (minerData.addSoupLoc(soupLoc)) {
				System.out.println("Found Soup! Loc: " + soupLoc);
				GeneralCommands.sendTransaction(rc, 5, GeneralCommands.Type.TRANSACTION_SOUP_AT_LOC, soupLoc);
			}
		}


		// Either searches in direction of target or last known position of target
		MinerCommands.continueSearch(rc, minerData);

	}

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    	
    }

    static void runVaporator() throws GameActionException {
    	
    }

    static void runDesignSchool() throws GameActionException {
    	DesignSchoolData designSchoolData = (DesignSchoolData) robotData;
    	
    	if(!designSchoolData.isStableSoupIncomeConfirmed()) DesignSchoolCommands.confirmStableSoupIncome(rc, designSchoolData);
    	if(DesignSchoolCommands.oughtBuildLandscaper(rc, designSchoolData)) DesignSchoolCommands.tryBuild(rc, RobotType.LANDSCAPER, designSchoolData);
    	
    	RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), designSchoolData.getOpponent());
        
        if(enemy.length > 0) {
	        for(RobotInfo bullseye : enemy) { 
	        	if(bullseye.type.equals(RobotType.DESIGN_SCHOOL)) {
	        		designSchoolData.setBuildDirection(rc.getLocation().directionTo(bullseye.location).rotateLeft());
	        		while(!DesignSchoolCommands.tryBuild(rc, RobotType.LANDSCAPER, designSchoolData)) {
	        			
	        		}
	        	}
	        }
        }
    	
    }

    static void runFulfillmentCenter() throws GameActionException {
    	FulfillmentCenterData fulfillmentCenterData = (FulfillmentCenterData) robotData;
    	
    	if(!fulfillmentCenterData.isStableSoupIncomeConfirmed()) FulfillmentCenterCommands.confirmStableSoupIncome(rc, fulfillmentCenterData);
    	if(FulfillmentCenterCommands.oughtBuildDrone(rc, fulfillmentCenterData)) FulfillmentCenterCommands.tryBuild(rc, RobotType.DELIVERY_DRONE, fulfillmentCenterData);
    }

    static void runLandscaper() throws GameActionException {
    	LandscaperData landscaperData = (LandscaperData) robotData;
    	if(turnCount == 1) LandscaperCommands.learnHQLocation(rc, landscaperData);
		RobotInfo enemyDesign = GeneralCommands.senseUnitType(rc, RobotType.DESIGN_SCHOOL, landscaperData.getOpponent());
    	
    	if(LandscaperCommands.buryEnemyHQ(rc, landscaperData)) {
    		/*Do nothing else*/
    	} else if(enemyDesign!=null){
    		LandscaperCommands.buryEnemyDesign(rc,landscaperData,enemyDesign);
    	} else if(landscaperData.getCurrentRole() == LandscaperData.TRAVEL_TO_HQ) {
    		if(!LandscaperCommands.approachComplete(rc, landscaperData)) {
    			GeneralCommands.routeTo(landscaperData.getHqLocation(), rc, landscaperData);
    		} else {
    			landscaperData.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
    		}
    	} else if(landscaperData.getCurrentRole() == LandscaperData.DEFEND_HQ_FROM_FLOOD) {
    		LandscaperCommands.buildHQWall(rc, landscaperData);
    	}
    }

    static void runDeliveryDrone() throws GameActionException {
    	DroneData data = (DroneData) robotData;
    	
    	if(turnCount == 1) DroneCommands.learnHQLocation(rc, data);
    	
    	if(turnCount < GameConstants.INITIAL_COOLDOWN_TURNS) {
    		for(int i = (rc.getRoundNum() > 100) ? rc.getRoundNum() - 100 : 1; i < rc.getRoundNum(); i++)
    		DroneCommands.readTransaction(rc, data, rc.getBlock(i));
    	}

    	DroneCommands.readTransaction(rc, data, rc.getBlock(rc.getRoundNum() - 1));
    	
    	if(data.getEnemyHQLocation() != null) {
    		
    		if(!rc.isCurrentlyHoldingUnit()) {
        		if(GeneralCommands.senseUnitType(rc,RobotType.LANDSCAPER,data.getOpponent())!=null && (GeneralCommands.senseUnitType(rc,RobotType.HQ,data.getTeam())!=null || GeneralCommands.senseUnitType(rc,RobotType.HQ,data.getOpponent())!=null)) {
        			if(!DroneCommands.pickUpUnit(rc, data, RobotType.LANDSCAPER, data.getOpponent())) {
        				if(GeneralCommands.senseUnitType(rc, RobotType.HQ, data.getTeam())!=null)
        					GeneralCommands.routeTo(data.getHqLocation(),rc,data);
        				else
        					GeneralCommands.routeTo(data.getEnemyHQLocation(),rc,data);
        			}
        			else {
        				data.setHoldingEnemy(true);
        				if(GeneralCommands.senseUnitType(rc, RobotType.HQ, data.getTeam())!=null)
        					data.setEnemyFrom(data.getTeam());
        				else
        					data.setEnemyFrom(data.getOpponent());
        			}
        		}
        	}
        	
        	if(data.getHoldingEnemy()) {
        		for(Direction d : Direction.allDirections()) {
        			if(rc.senseFlooding(rc.adjacentLocation(d))) {
        				rc.dropUnit(d);
        				data.setHoldingEnemy(false);
        				break;
        			}
        		}
        		if(data.getHoldingEnemy()) {
        			if(GeneralCommands.senseUnitType(rc, RobotType.HQ, data.getTeam())!=null)
						data.setEnemyFrom(data.getTeam());
					else
						data.setEnemyFrom(data.getOpponent());
        			if(data.getEnemyFrom().equals(data.getTeam()))
        				GeneralCommands.routeTo(data.getEnemyHQLocation(), rc, data);
        			else
        				GeneralCommands.routeTo(data.getHqLocation(), rc, data);
        		}
        	}
    		
    		if(rc.isCurrentlyHoldingUnit() && !data.getHoldingEnemy()) {
    			if(rc.getLocation().isWithinDistanceSquared(data.getEnemyHQLocation(), 3)) {
    				DroneCommands.dropUnitNextToHQ(rc, data);
    			} else {
    				GeneralCommands.routeTo(data.getEnemyHQLocation(), rc, data);
    			}
    		} else if (!rc.isCurrentlyHoldingUnit()){
    			boolean oughtPickUpCow = DroneCommands.oughtPickUpCow(rc, data);
    			boolean oughtPickUpLandscaper = DroneCommands.oughtPickUpLandscaper(rc, data);
    			
    			if(GeneralCommands.senseUnitType(rc, RobotType.COW) != null && oughtPickUpCow) {
	    			if(!DroneCommands.pickUpUnit(rc, data, RobotType.COW)) {
	    				GeneralCommands.routeTo(data.getHqLocation(), rc, data);
	    			}
	    		} else if(oughtPickUpLandscaper) {
	    			if(!DroneCommands.pickUpUnit(rc, data, RobotType.LANDSCAPER, rc.getTeam())) {
	    				GeneralCommands.routeTo(data.getHqLocation(), rc, data);
	    			}
	    		} else if(!rc.getLocation().isWithinDistanceSquared(data.getHqLocation(), 3)) {
	    			GeneralCommands.routeTo(data.getSpawnerLocation(), rc, data);
	    		} else {
	    			GeneralCommands.routeTo(data.getEnemyHQLocation(), rc, data);
	    		}
    		}
    	} else {
    		if(!data.searchDestinationsDetermined()) {
    			data.calculateSearchDestinations(rc);
    		}
    		
    		GeneralCommands.routeTo(data.getActiveSearchDestination(), rc, data);
    		DroneCommands.attemptEnemyHQDetection(rc, data);
    		if(data.getEnemyHQLocation() != null) {
    			GeneralCommands.sendTransaction(rc, 10, GeneralCommands.Type.TRANSACTION_ENEMY_HQ_AT_LOC, data.getEnemyHQLocation());
    		}
    	}
    }

    static void runNetGun() throws GameActionException {
    	NetGunData ngData = (NetGunData) robotData;
    	RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), ngData.getOpponent());
    	if(enemy.length > 0) {
    		for (RobotInfo target : enemy) {
    			if(rc.canShootUnit(target.getID())) {
    				rc.shootUnit(target.getID());
    			}
    		}
    	}
    }
}

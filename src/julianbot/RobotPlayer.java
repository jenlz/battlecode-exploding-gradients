package julianbot;
import battlecode.common.*;
import com.sun.tools.javah.Gen;
import julianbot.commands.DesignSchoolCommands;
import julianbot.commands.DroneCommands;
import julianbot.commands.FulfillmentCenterCommands;
import julianbot.commands.GeneralCommands;
import julianbot.commands.HQCommands;
import julianbot.commands.LandscaperCommands;
import julianbot.commands.MinerCommands;
import julianbot.robotdata.DesignSchoolData;
import julianbot.robotdata.DroneData;
import julianbot.robotdata.FulfillmentCenterData;
import julianbot.robotdata.HQData;
import julianbot.robotdata.LandscaperData;
import julianbot.robotdata.MinerData;
import julianbot.robotdata.RobotData;

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
    		default:                 robotData = new RobotData(rc);              break;
    	}

    	return robotData;
    }

    static void runHQ() throws GameActionException {
    	HQData hqData = (HQData) robotData;
    	if(rc.getRoundNum() == 100) HQCommands.sendSOS(rc);
    	if(rc.getRoundNum() == 1) {
    		HQCommands.makeInitialReport(rc);
    		HQCommands.setBuildDirectionTowardsSoup(rc, hqData);
    	}
    	    	
        if(HQCommands.oughtBuildMiner(rc, hqData)) {        	
        	HQCommands.tryBuild(rc, RobotType.MINER, hqData);
        }
                
        HQCommands.storeForeignTransactions(rc, hqData);
        if(rc.getRoundNum() % 100 == 0) HQCommands.repeatForeignTransaction(rc, hqData);        
        
        RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), hqData.getOpponent());
        
        if(enemy.length > 0) {
	        RobotInfo target = enemy[0];
	        for (RobotInfo unit : enemy) {
	        	if (unit.getType() == RobotType.DELIVERY_DRONE) {
	        		target = unit;
				}
			}

	    	if(rc.canShootUnit(target.getID())) {
	    		rc.shootUnit(target.getID());
	    	}
	        if(enemy.length > 3) {
	        	HQCommands.sendSOS(rc);
	        }
        }
    }

    static void runMiner() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	if(turnCount == 1) MinerCommands.discernRole(rc, minerData);

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
			case MinerData.ROLE_SOS:
				sosMinerProtocol();
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
			if(rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost && !MinerCommands.canSenseHubDesignSchool(rc, minerData)) minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
			else if(rc.getTeamSoup() >= RobotType.FULFILLMENT_CENTER.cost && !MinerCommands.canSenseHubFulfillmentCenter(rc, minerData)) minerData.setCurrentRole(MinerData.ROLE_FULFILLMENT_BUILDER);
			return;
		}

		//If no refinery is adjacent, look for one.
		//RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam(), ((int) Math.sqrt(RobotType.MINER.sensorRadiusSquared)) - 2); //For pathfind, change senseUnitType if add back in
		
		//TODO: Once the landscapers get going, miners should no longer return to the HQ to refine soup. We need to communicate that via a transaction.
		
		RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam());
		Direction distantRefineryDirection = MinerCommands.getAnyRefineryDirection(rc);
		
		if(hq != null) {
			if(GeneralCommands.senseUnitType(rc, RobotType.LANDSCAPER, rc.getTeam()) != null) {
				System.out.println("Moving from landscaper site.");
	    		moveMinerFromHQ(minerData);
			} else if(Math.abs(rc.getLocation().x - minerData.getSpawnerLocation().x) == 1) {
	    		//Get out of the way. This is a building site.
				System.out.println("Moving from building site.");
	    		moveMinerFromHQ(minerData);
	    		return;
	    	} else {
	    		System.out.println("Routing to HQ");
	    		GeneralCommands.routeTo(hq.getLocation(), rc, minerData);
	    		return;
	    	}
		}

		if (distantRefineryDirection != Direction.CENTER) {
			GeneralCommands.move(rc, distantRefineryDirection, minerData);
			return;
		} else {
			System.out.println("Moving toward hq");
			GeneralCommands.routeTo(minerData.getSpawnerLocation(), rc, minerData);
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
    	RobotInfo designSchool = GeneralCommands.senseUnitType(rc, RobotType.DESIGN_SCHOOL, rc.getTeam());
    	RobotInfo fulfillmentCenter = GeneralCommands.senseUnitType(rc, RobotType.LANDSCAPER, rc.getTeam());
    	
    	if(designSchool != null || fulfillmentCenter != null) {
    		System.out.println("\tDS or FC detected");
        	moveMinerFromHQ(minerData);
        	return;
    	} else if(hq != null && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
    		System.out.println("\tSetting role to design school builder");
    		minerData.setCurrentRole(MinerData.ROLE_DESIGN_BUILDER);
    		return;
    	}
    	
    	if(!MinerCommands.mineRawSoup(rc, MinerCommands.getAdjacentSoupDirection(rc))) {

		}

    	//If we can't find distant soup, move in the search direction.
    	MinerCommands.continueSearch(rc, minerData);
    }
    
    static void moveMinerFromHQ(MinerData minerData) throws GameActionException {
    	Direction fromHQDirection = minerData.getSpawnerLocation().directionTo(rc.getLocation());
    	if(GeneralCommands.move(rc, fromHQDirection, minerData)) minerData.setSearchDirection(fromHQDirection);
    	else if(GeneralCommands.move(rc, fromHQDirection.rotateLeft(), minerData)) minerData.setSearchDirection(fromHQDirection.rotateLeft());
    	else if(GeneralCommands.move(rc, fromHQDirection.rotateRight(), minerData)) minerData.setSearchDirection(fromHQDirection.rotateRight());
    	else GeneralCommands.pathfind(minerData.getSpawnerLocation().add(fromHQDirection).add(fromHQDirection), rc, minerData);
    }

	/**
	 * Searches map until it finds enemy unit, then follows that unit. If enemy drone appears, avoids it. Reports enemy building locations.
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

	/**
	 * Protocol for miners in times of SOS. Surrounds HQ when it sends SOS.
	 * @throws GameActionException
	 */
	static void sosMinerProtocol() throws GameActionException{
		MinerData minerData = (MinerData) robotData;

		if (minerData.getSosLoc().equals(minerData.getSpawnerLocation())) {
			boolean inPosition = false;
			if (GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam()) != null && !inPosition) {
				for (Direction dir : Direction.allDirections()) {
					MapLocation adjacentToHQ = minerData.getSosLoc().add(dir);
					if (rc.getLocation().equals(adjacentToHQ)) {
						System.out.println("In position: " + rc.getLocation());
						inPosition = true;
						break;
					}
					GeneralCommands.pathfind(adjacentToHQ, rc, minerData);
				}
			} else if (!inPosition) {
				GeneralCommands.routeTo(minerData.getSosLoc(), rc, minerData);
			}
		}

	}

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    	
    }

    static void runVaporator() throws GameActionException {
    	
    }

    static void runDesignSchool() throws GameActionException {
    	DesignSchoolData designSchoolData = (DesignSchoolData) robotData;
    	if(DesignSchoolCommands.oughtBuildLandscaper(rc, designSchoolData)) DesignSchoolCommands.tryBuild(rc, RobotType.LANDSCAPER, designSchoolData);
    }

    static void runFulfillmentCenter() throws GameActionException {
    	FulfillmentCenterData fulfillmentCenterData = (FulfillmentCenterData) robotData;
    	if(FulfillmentCenterCommands.oughtBuildDrone(rc, fulfillmentCenterData)) FulfillmentCenterCommands.tryBuild(rc, RobotType.DELIVERY_DRONE, fulfillmentCenterData);
    }

    static void runLandscaper() throws GameActionException {
    	LandscaperData data = (LandscaperData) robotData;
    	if(turnCount == 1) LandscaperCommands.learnHQLocation(rc, data);
    	
    	if(LandscaperCommands.buryEnemyHQ(rc, data)) {
    		/*Do nothing else*/
    	} else if(data.getCurrentRole() == LandscaperData.TRAVEL_TO_HQ) {
    		if(!LandscaperCommands.approachComplete(rc, data)) {
    			LandscaperCommands.approachHQ(rc, data);
    		} else {
    			data.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
    		}
    	} else if(data.getCurrentRole() == LandscaperData.DEFEND_HQ_FROM_FLOOD) {
    		LandscaperCommands.buildHQWall(rc, data);
    	}
    }

    static void runDeliveryDrone() throws GameActionException {
    	DroneData data = (DroneData) robotData;
    	
    	if(data.getEnemyHQLocation() != null) {
    		if(rc.isCurrentlyHoldingUnit()) {
    			if(rc.getLocation().isWithinDistanceSquared(data.getEnemyHQLocation(), 3)) {
    				DroneCommands.dropUnitNextToHQ(rc, data);
    			} else {
    				GeneralCommands.move(rc, rc.getLocation().directionTo(data.getEnemyHQLocation()), data);
    			}
    		} else if(DroneCommands.oughtPickUpUnit(rc, data)){
    			if(!DroneCommands.pickUpUnit(rc, data, RobotType.LANDSCAPER)) {
    				GeneralCommands.move(rc, rc.getLocation().directionTo(data.getSpawnerLocation()), data);
    			}
    		} else {
    			GeneralCommands.move(rc, rc.getLocation().directionTo(data.getSpawnerLocation()), data);
    		}
    	} else {
    		DroneCommands.continueSearch(rc, data);
    		DroneCommands.searchForEnemyHQ(rc, data);
    	}
    }

    static void runNetGun() throws GameActionException {
    	/*
    	NetGunData ngData = (NetGunData) robotData;
    	RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), ngData.getOpponent());
    	int target = (int) (Math.random()*enemy.length);
    	if(rc.canShootUnit(enemy[target].getID())) {
    		rc.shootUnit(enemy[target].getID());
    	}
    	*/
    }
}

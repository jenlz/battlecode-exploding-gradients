package julianbot;
import battlecode.common.*;
import com.sun.tools.javah.Gen;
import julianbot.commands.DesignSchoolCommands;
import julianbot.commands.GeneralCommands;
import julianbot.commands.HQCommands;
import julianbot.commands.LandscaperCommands;
import julianbot.commands.MinerCommands;
import julianbot.robotdata.DesignSchoolData;
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

    	System.out.println("Robot Type = " + type);

    	switch(type) {
    		case HQ:             robotData = new HQData(rc);             break;
    		case MINER:          robotData = new MinerData(rc);          break;
    		case DESIGN_SCHOOL:  robotData = new DesignSchoolData(rc);   break;
    		case LANDSCAPER:     robotData = new LandscaperData(rc);     break;
    		default:             robotData = new RobotData(rc);          break;
    	}
    	System.out.println(robotData);
    	return robotData;
    }

    static void runHQ() throws GameActionException {
    	HQData hqData = (HQData) robotData;
    	
    	if(rc.getRoundNum() == 1) HQCommands.makeInitialReport(rc);
        if(HQCommands.oughtBuildMiner(rc)) HQCommands.tryBuild(rc, RobotType.MINER, hqData);
        HQCommands.storeForeignTransactions(rc, hqData);
        if(rc.getRoundNum() % 100 == 0) HQCommands.repeatForeignTransaction(rc, hqData);        
        
        RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), hqData.getOpponent());
        
        if(enemy.length > 0) {
	        int target = (int) (Math.random()*enemy.length);
	    	if(rc.canShootUnit(enemy[target].getID())) {
	    		rc.shootUnit(enemy[target].getID());
	    	}
	        if(enemy.length > 10) {
	        	HQCommands.sendSOS(rc);
	        }
        }
    }

    static void runMiner() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
//    	topographicallyAdeptMinerProtocol();

		switch(minerData.getCurrentRole()) {
			case MinerData.ROLE_DESIGN_BUILDER:
				designMinerProtocol();
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
			case MinerData.ROLE_SCOUT:
				scoutMinerProtocol();
			default:

		}
    }
    
    static void topographicallyAdeptMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	GeneralCommands.pathfind(minerData.getSpawnerLocation().add(rc.getLocation().directionTo(minerData.getSpawnerLocation())), rc, minerData);
    }

	/**
	 * Builds a design school and then switches to a soup miner
	 * @throws GameActionException
	 */
	static void designMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	if(MinerCommands.locateNearbyDesignSchool(rc)) {
    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    	} else if(rc.getLocation().isWithinDistanceSquared(robotData.getSpawnerLocation(), 15)) {
    		MinerCommands.continueSearch(rc, minerData);
    	} else {
    		if(MinerCommands.attemptDesignSchoolConstruction(rc)) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		else MinerCommands.continueSearch(rc, minerData);
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
			return;
		}

		//If no refinery is adjacent, look for one.
		//RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam(), ((int) Math.sqrt(RobotType.MINER.sensorRadiusSquared)) - 2); //For pathfind, change senseUnitType if add back in
		RobotInfo hq = GeneralCommands.senseUnitType(rc, RobotType.HQ, rc.getTeam());
		Direction distantRefineryDirection = MinerCommands.getAnyRefineryDirection(rc);
		if (distantRefineryDirection != Direction.CENTER) {
			GeneralCommands.move(rc, distantRefineryDirection, minerData);
			return;
		} else if (hq != null) {
			GeneralCommands.pathfind(hq.getLocation().add(hq.getLocation().directionTo(rc.getLocation())), rc, minerData);
			return;
		} else {
			System.out.println("Moving toward hq");
			if (!GeneralCommands.move(rc, rc.getLocation().directionTo(minerData.getSpawnerLocation()), minerData)) {
				Direction canMoveDir = rc.getLocation().directionTo(minerData.getSpawnerLocation()).rotateRight();
				int rotateLimit = 8;
				while (!GeneralCommands.move(rc, canMoveDir, minerData) && rotateLimit > 0) {
					canMoveDir.rotateRight();
					rotateLimit--;
				}
			}
		}
		//If we can't find a refinery, build one.
		if (MinerCommands.attemptRefineryConstruction(rc)) return;

    	//If we can't build a refinery, start searching for better ground.
		//TODO check soup locations for next location to go to
    	MinerCommands.continueSearch(rc, minerData);
    }

	/**
	 * Miner under soup carrying limit
	 * @throws GameActionException
	 */
	static void emptyMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
		System.out.println("empty protocol");
		if	(! MinerCommands.mineRawSoup(rc, MinerCommands.getAdjacentSoupDirection(rc))) {
			Direction soupDir = MinerCommands.getDistantSoupDirection(rc);
			if (soupDir != Direction.CENTER) {
				minerData.setSearchDirection(soupDir);
			}
		}
    	//If we can't find distant soup, move in the search direction.
    	MinerCommands.continueSearch(rc, minerData);
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
			MapLocation scoutLoc = rc.getLocation();
			MapLocation targetLoc = minerData.getTargetRobot().getLocation();
			Direction targetToScout = targetLoc.directionTo(scoutLoc);
			minerData.setSearchDirection(scoutLoc.directionTo(targetLoc.add(targetToScout).add(targetToScout)));
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
    	if(DesignSchoolCommands.oughtBuildLandscaper(rc, designSchoolData)) DesignSchoolCommands.tryBuild(rc, RobotType.LANDSCAPER, designSchoolData);
    }

    static void runFulfillmentCenter() throws GameActionException {
    	
    }

    static void runLandscaper() throws GameActionException {
    	LandscaperData data = (LandscaperData) robotData;
    	if(turnCount == 1) LandscaperCommands.learnHQLocation(rc, data);
    	
    	if(data.getCurrentRole() == LandscaperData.TRAVEL_TO_HQ) {
    		LandscaperCommands.approachHQ(rc, data);
    		LandscaperCommands.determineApproachCompletion(rc, data);
    	} else if(data.getCurrentRole() == LandscaperData.DEFEND_HQ_FROM_FLOOD) {
    		LandscaperCommands.buildHQWall(rc, data);
    	}
    }

    static void runDeliveryDrone() throws GameActionException {
       
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

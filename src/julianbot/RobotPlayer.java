package julianbot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
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

    	switch(type) {
    		case HQ:             robotData = new HQData(rc);             break;
    		case MINER:          robotData = new MinerData(rc);          break;
    		case DESIGN_SCHOOL:  robotData = new DesignSchoolData(rc);   break;
    		case LANDSCAPER:     robotData = new LandscaperData(rc);     break;
    		default:             robotData = new RobotData(rc);          break;
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
    	if(turnCount == 1) MinerCommands.discernRole(rc, minerData);
    	
//    	topographicallyAdeptMinerProtocol();

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
			default:
				break;
		}
    }
    
    static void topographicallyAdeptMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	GeneralCommands.pathfind(minerData.getSpawnerLocation().add(rc.getLocation().directionTo(minerData.getSpawnerLocation())), rc, minerData);
    }
    
    static void designMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	MapLocation designSchoolBuildSite = minerData.getSpawnerLocation().translate(-1, 0);
    	
    	RobotInfo designSchool = GeneralCommands.senseUnitType(rc, RobotType.DESIGN_SCHOOL, rc.getTeam());
    	
    	if(designSchool != null) {
    		RobotInfo fulfillmentCenter = GeneralCommands.senseUnitType(rc, RobotType.FULFILLMENT_CENTER, rc.getTeam());
    		minerData.setCurrentRole((fulfillmentCenter != null) ? MinerData.ROLE_SOUP_MINER : MinerData.ROLE_FULFILLMENT_BUILDER);
    		return;
    	} else if(rc.getLocation().equals(designSchoolBuildSite)) {
    		//Move off of design school build site.
    		GeneralCommands.moveAnywhere(rc, minerData);
    		return;
    	} else if(rc.getLocation().isWithinDistanceSquared(designSchoolBuildSite, 3)) {
    		if(MinerCommands.attemptDesignSchoolConstruction(rc, rc.getLocation().directionTo(designSchoolBuildSite))) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(GeneralCommands.move(rc, rc.getLocation().directionTo(designSchoolBuildSite), minerData)) {
    		return;
    	} else {
    		//TODO: Improved, longer-range general pathfinding calls here. We need to navigate around obstacles intelligently, but I expect that that will end up being a protocol nested in a function.
    		GeneralCommands.pathfind(designSchoolBuildSite, rc, minerData);
    		if(!minerData.hasPath()) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	}
    }
    
    static void fulfillmentMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	MapLocation fulfillmentCenterBuildSite = minerData.getSpawnerLocation().translate(1, 0);
    	    	
    	RobotInfo fulfillmentCenter = GeneralCommands.senseUnitType(rc, RobotType.FULFILLMENT_CENTER, rc.getTeam());
    	
    	if(fulfillmentCenter != null) {
    		minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(rc.getLocation().equals(fulfillmentCenterBuildSite)) {
    		//Move off of design school build site.
    		GeneralCommands.moveAnywhere(rc, minerData);
    		return;
    	} else if(rc.getLocation().isWithinDistanceSquared(fulfillmentCenterBuildSite, 3)) {
    		if(MinerCommands.attemptFulfillmentCenterConstruction(rc, rc.getLocation().directionTo(fulfillmentCenterBuildSite))) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	} else if(GeneralCommands.move(rc, rc.getLocation().directionTo(fulfillmentCenterBuildSite), minerData)) {
    		return;
    	} else {
    		//TODO: Improved, longer-range general pathfinding calls here. We need to navigate around obstacles intelligently, but I expect that that will end up being a protocol nested in a function.
    		GeneralCommands.pathfind(fulfillmentCenterBuildSite, rc, minerData);
    		if(!minerData.hasPath()) minerData.setCurrentRole(MinerData.ROLE_SOUP_MINER);
    		return;
    	}
    }

    static void defenseMinerProtocol() throws GameActionException {
    	MinerData minerData = (MinerData) robotData;
    	
    	if(!minerData.isFulfillmentCenterBuilt()) {
    		if(MinerCommands.routeToFulfillmentCenterSite(rc, minerData)) {
    			MinerCommands.buildDefenseFulfillmentCenter(rc, minerData);
    			return;
    		}
    	}
    }
    
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
	    		if(!GeneralCommands.move(rc, rc.getLocation().directionTo(hq.getLocation()), minerData)) {
	    			GeneralCommands.pathfind(hq.getLocation().add(hq.getLocation().directionTo(rc.getLocation())), rc, minerData);
	    		}
	    		return;
	    	}
		}
		
		if (distantRefineryDirection != Direction.CENTER) {
			GeneralCommands.move(rc, distantRefineryDirection, minerData);
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
    }
    
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
			Direction soupDir = MinerCommands.getDistantSoupDirection(rc);
			if (soupDir != Direction.CENTER) {
				minerData.setSearchDirection(soupDir);
			}
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

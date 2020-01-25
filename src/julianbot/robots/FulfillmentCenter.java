package julianbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.FulfillmentCenterData;

public class FulfillmentCenter extends Robot {

	private FulfillmentCenterData fulfillmentCenterData;
	
	public FulfillmentCenter(RobotController rc) {
		super(rc);
		this.data = new FulfillmentCenterData(rc, getSpawnerLocation());
		this.fulfillmentCenterData = (FulfillmentCenterData) this.data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
		if(turnCount == 1) {
			learnHQLocation();
			determineEdgeState();
		}

    	readTransactions();
    	
		if(!fulfillmentCenterData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
		
		fulfillmentCenterData.setPauseBuildTimer(fulfillmentCenterData.getPauseBuildTimer() - 1);
		
		if(buildDefensiveDrones()) return;
    	if(oughtBuildDrone()) tryBuild();
	}
	
	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				fulfillmentCenterData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}		
	}

	private void determineEdgeState() {
		MapLocation hqLocation = fulfillmentCenterData.getHqLocation();
		
		int mapWidth = rc.getMapWidth();
		int mapHeight = rc.getMapHeight();

		boolean leftEdge = hqLocation.x <= 0;
		boolean rightEdge = hqLocation.x >= mapWidth - 1;
		boolean topEdge = hqLocation.y >= mapHeight - 1;
		boolean bottomEdge = hqLocation.y <= 0;

		fulfillmentCenterData.initializeWallData(hqLocation, mapWidth, mapHeight);
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) fulfillmentCenterData.setBuildDirection(Direction.NORTH);
			else if(topEdge) fulfillmentCenterData.setBuildDirection(Direction.SOUTH);
			else fulfillmentCenterData.setBuildDirection(Direction.NORTH);
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) fulfillmentCenterData.setBuildDirection(Direction.NORTH);
			else if(topEdge) fulfillmentCenterData.setBuildDirection(Direction.SOUTH);
			else fulfillmentCenterData.setBuildDirection(Direction.SOUTH);
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			fulfillmentCenterData.setBuildDirection(Direction.EAST);
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			fulfillmentCenterData.setBuildDirection(Direction.WEST);
		} else {
			fulfillmentCenterData.setBuildDirection(Direction.NORTH);
		}
	}
	
	private boolean buildDefensiveDrones() throws GameActionException {
		int numFriendlyDrones = this.senseNumberOfUnits(RobotType.DELIVERY_DRONE, rc.getTeam());
		
		if(numFriendlyDrones < 3 && rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost) {
			RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
			for(RobotInfo enemy : enemies) {
				if(enemy.type.canBePickedUp()) {
					return defensiveBuild();
				}
			}
		}
		
		return false;
	}
	
	private boolean oughtBuildDrone() {
		if (fulfillmentCenterData.getPauseBuildTimer() > 0) return false;
		
		if(fulfillmentCenterData.isStableSoupIncomeConfirmed()) {
			MapLocation hqLocation = fulfillmentCenterData.getHqLocation();
			
			RobotInfo[] landscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
			int xMin = fulfillmentCenterData.getWallOffsetXMin();
			int xMax = fulfillmentCenterData.getWallOffsetYMax();
			int yMin = fulfillmentCenterData.getWallOffsetYMin();
			int yMax = fulfillmentCenterData.getWallOffsetYMax();
			
			for(RobotInfo landscaper : landscapers) {
				MapLocation location = landscaper.getLocation();
				int dx = location.x - hqLocation.x;
				int dy = location.y - hqLocation.y;
				
				boolean xInWall = xMin < dx && dx < xMax;
				boolean yInWall = yMin < dy && dy < yMax;
				
				//A landscaper inside the wall is an attack landscaper. We ought to build a drone to pair with it.
				if(xInWall && yInWall) return true;
			}
			
			//Otherwise, we can still produce scouting drones if we need to.
			return (!fulfillmentCenterData.isEnemyHqLocated()) ? rc.getTeamSoup() >= RobotType.DELIVERY_DRONE.cost + 1 : false;
		} else {
			//If a stable soup income is not confirmed, give the miners time to build a refinery before allocating soup to drones.
			return rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5;
		}
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    private boolean tryBuild() throws GameActionException {
    	Direction buildDirection = fulfillmentCenterData.getBuildDirection();
    	
    	waitUntilReady();
        if(rc.canBuildRobot(RobotType.DELIVERY_DRONE, buildDirection)) {
            rc.buildRobot(RobotType.DELIVERY_DRONE, buildDirection);
            fulfillmentCenterData.incrementDronesBuilt();
            return true;
        }
        
        return false;
    }
    
    private boolean defensiveBuild() throws GameActionException {
    	waitUntilReady();
    	
    	RobotInfo[] netGuns = this.senseAllUnitsOfType(RobotType.NET_GUN, rc.getTeam().opponent());
    	
    	for(Direction direction : Direction.allDirections()) {
    		if(rc.canBuildRobot(RobotType.DELIVERY_DRONE, direction) && !buildIntoNetGunRange(netGuns, direction)) {
                rc.buildRobot(RobotType.DELIVERY_DRONE, direction);
                fulfillmentCenterData.incrementDronesBuilt();
                return true;
            }
    	}
    	
    	return false;
    }
    
    private boolean buildIntoNetGunRange(RobotInfo[] netGuns, Direction buildDirection) {
    	MapLocation buildLocation = rc.getLocation().add(buildDirection);
    	
    	for(RobotInfo netGun : netGuns) {
    		if(buildLocation.isWithinDistanceSquared(netGun.getLocation(), RobotType.NET_GUN.sensorRadiusSquared)) return true;
    	}
    	
    	return false;
    }
    
    private void confirmStableSoupIncome() throws GameActionException {
    	if(senseUnitType(RobotType.VAPORATOR, rc.getTeam()) != null) {
    		fulfillmentCenterData.setStableSoupIncomeConfirmed(true);
    	}
    	
    	/*
    	for(int i = fulfillmentCenterData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			if(message.length >= 4) {
    				if(message[1] == Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC.getVal()) {
    					fulfillmentCenterData.setStableSoupIncomeConfirmed(true);
    					System.out.println("Stable soup income confirmed!");
    					return;
    				}
    			}
    		}
    		
    		if(Clock.getBytecodesLeft() <= 200) {
    			fulfillmentCenterData.setTransactionRound(i);
    			break;
    		}
    	}
    	*/
    }
    
    private void readTransactions() throws GameActionException {
    	for(int i = fulfillmentCenterData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			
    			if (message.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
	    			Robot.Type category = Robot.Type.enumOfValue(message[1]);
	
					//System.out.println("Category of message: " + category);
					switch(category) {
						case TRANSACTION_ENEMY_HQ_AT_LOC:
							fulfillmentCenterData.setEnemyHqLocated(true);
							break;
						case TRANSACTION_KILL_ORDER:
    						System.out.println("Pausing building...");
    						fulfillmentCenterData.setPauseBuildTimer(message[5]);
    						break;
						default:
							break;
					}
    			}
    		}
    		
    		if(Clock.getBytecodesLeft() <= 500) {
    			fulfillmentCenterData.setTransactionRound(i);
    			break;
    		}
    	}
    }
	
}

package julianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.HQData;

public class HQ extends Robot {

	private HQData hqData;
	
	private static final int KILL_ORDER_COOLDOWN_ROUNDS = 50;
	private int killOrderCooldownCount;
	
	public HQ(RobotController rc) {
		super(rc);
		this.data = new HQData(rc, getSpawnerLocation());
		this.hqData = (HQData) this.data;
	}

	@Override
	public void run() throws GameActionException { 
		super.run();
		
    	if(rc.getRoundNum() == 1) {
    		makeInitialReport();
    		setBuildDirectionTowardsSoup();
    		hqData.initializeWallData(rc.getLocation(), rc.getMapWidth(), rc.getMapHeight());
    		reportBlockedBuildSites();
    	}
    	    	
        if(oughtBuildMiner()) {        	
        	tryBuild(RobotType.MINER);
        }
        
        if(wallBuilt() && lacksVaporatorMiner()) {
        	buildVaporatorMiner();
        }
        
        if(hqData.getEnemyHqLocation() == null) readForEnemyHq();
        
        storeForeignTransactions();
        if(rc.getRoundNum() % 100 == 0) repeatForeignTransaction();    
        
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), hqData.getOpponent());
                
        if(enemies.length > 0) {
        	if(enemies.length > 10) {
	        	sendSOS();
	        }
        	
	        for(RobotInfo enemy : enemies) { 
	        	if(enemy.type.equals(RobotType.DESIGN_SCHOOL)) {
	        		hqData.setBuildDirection(rc.getLocation().directionTo(enemy.location).rotateLeft());
	        		tryBuild(RobotType.MINER);
	        	}
	        	
		    	if(rc.canShootUnit(enemy.getID())) {
		    		shootUnit(enemy.getID());
		    	}
	        }
        }
        
        if(killOrderCooldownCount <= 0) {
        	int projectedFlooding = getFloodingAtRound(rc.getRoundNum() + 150);
        	
        	if(rc.getRoundNum() > 1000 && projectedFlooding > lowestWallHeight()) {
        		sendKillOrder();
        		return;
        	}
        	
	        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
	        int attackDroneCount = 0;
	        
	        for(RobotInfo ally : allies) {
	        	if(ally.type == RobotType.DELIVERY_DRONE && (Math.abs(ally.getLocation().x - rc.getLocation().x) == 3 || Math.abs(ally.getLocation().y - rc.getLocation().y) == 3)) attackDroneCount++;
	        }
	        
	        if(attackDroneCount >= 15) sendKillOrder();
        } else {
        	killOrderCooldownCount--;
        }
	}
	
	private void makeInitialReport() throws GameActionException {
		//Since there can be seven transactions per round, we can be guaranteed to get one message through on the first round if that message is sent with a bid of one more than a seventh of the inital soup cost.
		sendTransaction((GameConstants.INITIAL_SOUP / 7) + 1, Type.TRANSACTION_FRIENDLY_HQ_AT_LOC, rc.getLocation());
	}
	
	private boolean oughtBuildMiner() {
		if(senseUnitType(RobotType.DESIGN_SCHOOL, rc.getTeam()) != null) return false;
		
		return rc.getTeamSoup() >= RobotType.MINER.cost + hqData.getMinersBuilt() * 30 || rc.getRoundNum() < 50;
	}
	
	private void reportBlockedBuildSites() throws GameActionException {
		MapLocation hqLocation = rc.getLocation();		
		int hqElevation = rc.senseElevation(hqLocation);
    	int designSchoolElevation = rc.senseElevation(hqData.getDesignSchoolBuildSite());
    	int fulfillmentCenterElevation = rc.senseElevation(hqData.getFulfillmentCenterBuildSite());
    	int vaporatorElevation = rc.senseElevation(hqData.getVaporatorBuildSite());
    	int vaporatorMinerElevation = rc.senseElevation(hqData.getVaporatorBuildMinerLocation());
		
		if(designSchoolElevation - hqElevation > GameConstants.MAX_DIRT_DIFFERENCE) sendTransaction(1, Type.TRANSACTION_BUILD_SITE_BLOCKED, hqData.getDesignSchoolBuildSite());
		else if(fulfillmentCenterElevation - hqElevation > GameConstants.MAX_DIRT_DIFFERENCE) sendTransaction(1, Type.TRANSACTION_BUILD_SITE_BLOCKED, hqData.getFulfillmentCenterBuildSite());
		else if(vaporatorElevation - hqElevation > GameConstants.MAX_DIRT_DIFFERENCE) sendTransaction(1, Type.TRANSACTION_BUILD_SITE_BLOCKED, hqData.getVaporatorBuildSite());
		else if(vaporatorMinerElevation - hqElevation > GameConstants.MAX_DIRT_DIFFERENCE) sendTransaction(1, Type.TRANSACTION_BUILD_SITE_BLOCKED, hqData.getVaporatorBuildMinerLocation());
	}
	
	
	
	//DEFENSE
	private void sendSOS() throws GameActionException {
		//Since there can be seven transactions per round, we can be guaranteed to get one message through if that message is sent with a bid of one more than a seventh of the inital soup cost.
		sendTransaction(10, Type.TRANSACTION_SOS_AT_LOC, rc.getLocation());
	}
	
	private void shootUnit(int robotID) throws GameActionException {
		waitUntilReady();
		
		if(rc.canShootUnit(robotID)) {
			rc.shootUnit(robotID);
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
    private boolean tryBuild(RobotType type) throws GameActionException {
    	Direction buildDirection = hqData.getBuildDirection();
    	
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            hqData.setBuildDirection(buildDirection.rotateRight());
            if(type == RobotType.MINER) hqData.incrementMinersBuilt();
            return true;
        } 
        
        hqData.setBuildDirection(buildDirection.rotateRight());
        return false;
    }
    
    //TODO: Rewrite this using rc.senseNearbySoup()
    private void setBuildDirectionTowardsSoup() throws GameActionException {
    	MapLocation rcLocation = rc.getLocation();
    	int dimension = (int) Math.ceil(Math.sqrt(RobotType.HQ.sensorRadiusSquared));
    	for(int dx = -dimension; dx <= dimension; dx++) {
    		for(int dy = -dimension; dy <= dimension; dy++) {
    			MapLocation searchLocation = rcLocation.translate(dx, dy);
    			if(rc.canSenseLocation(searchLocation)) {
    				if(rc.senseSoup(searchLocation) > 0) {
    					hqData.setBuildDirection(rcLocation.directionTo(searchLocation));
    					return;
    				}
    			}
    		}
    	}
    }
    
    /*
     * TODO: This function ignores tiles on the edge of the map, but there are map edges that should be part of the wall.
     * This is likely not a problem, as an estimate is sufficient for desired behavior, but this needs to be officially decided.
     */
    private boolean wallBuilt() throws GameActionException {
    	MapLocation rcLocation = rc.getLocation();
    	int hqElevation = rc.senseElevation(rcLocation);
    	
    	int minDx = data.getWallOffsetXMin();
    	int maxDx = data.getWallOffsetXMax();
    	int minDy = data.getWallOffsetYMin();
    	int maxDy = data.getWallOffsetYMax();
    	
    	for(int dx = minDx; dx <= maxDx; dx++) {
    		for(int dy = minDy; dy <= maxDy; dy++) {
    			MapLocation location = rcLocation.translate(dx, dy);
    			if(rc.canSenseLocation(location) && !onMapEdge(location) && isOnWall(location, rcLocation)) {
    				if(rc.senseElevation(location) - hqElevation <= GameConstants.MAX_DIRT_DIFFERENCE) return false;
    			}
    		}
    	}
    	
    	return true;
    }
    
    private int lowestWallHeight() throws GameActionException {
    	MapLocation rcLocation = rc.getLocation();
    	int lowestElevation = Integer.MAX_VALUE;
    	int waterLevel = getFloodingAtRound(rc.getRoundNum());
    	
    	int minDx = data.getWallOffsetXMin();
    	int maxDx = data.getWallOffsetXMax();
    	int minDy = data.getWallOffsetYMin();
    	int maxDy = data.getWallOffsetYMax();
    	
    	for(int dx = minDx; dx <= maxDx; dx++) {
    		for(int dy = minDy; dy <= maxDy; dy++) {
    			MapLocation location = rcLocation.translate(dx, dy);
    			if(rc.canSenseLocation(location) && isOnWall(location, rcLocation)) {
    				int elevation = rc.senseElevation(location);
    				if(elevation < lowestElevation && elevation > waterLevel) lowestElevation = elevation;
    			}
    		}
    	}
    	
    	return lowestElevation;
    }
    
    private boolean lacksVaporatorMiner() {
    	RobotInfo[] potentialMiners = rc.senseNearbyRobots(3, rc.getTeam());
    	for(RobotInfo robot : potentialMiners) {
    		if(robot.getType() == RobotType.MINER) return false;
    	}
    	
    	return true;
    }
    
    private boolean buildVaporatorMiner() throws GameActionException {
    	if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, Direction.SOUTH)) {
            rc.buildRobot(RobotType.MINER, Direction.SOUTH);
            return true;
        } 
        
        return false;
    }
    
    private void readForEnemyHq() throws GameActionException {
    	if(rc.getRoundNum() == 1) return;
    	
    	for(Transaction transaction : rc.getBlock(rc.getRoundNum() - 1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length >= 4) {
				if(message[1] == Robot.Type.TRANSACTION_ENEMY_HQ_AT_LOC.getVal()) {
					hqData.setEnemyHqLocation(new MapLocation(message[2], message[3]));
					return;
				}
			}
		}
    }
	
    private void sendKillOrder() throws GameActionException {
        sendTransaction(10, Type.TRANSACTION_KILL_ORDER, hqData.getEnemyHqLocation());
        killOrderCooldownCount = KILL_ORDER_COOLDOWN_ROUNDS;
    }
    
    private void storeForeignTransactions() throws GameActionException {
    	if(rc.getRoundNum() > 1) {
    		Transaction[] block = rc.getBlock(rc.getRoundNum() - 1);
	    	for(Transaction transaction : block) {
	    		if(transaction.getMessage().length > 0) {
	    			hqData.addForeignTransaction(transaction);
	    		}
	    	}
    	}
    }
    
    private void repeatForeignTransaction() throws GameActionException {
    	Transaction interceptedTransaction = hqData.getRandomForeignTransaction();
    	if(interceptedTransaction == null || interceptedTransaction.getMessage().length == 0) return;
    	    	
    	if(rc.canSubmitTransaction(interceptedTransaction.getMessage(), 1)) {
    		rc.submitTransaction(interceptedTransaction.getMessage(), 1);
    	}
    }
	
}

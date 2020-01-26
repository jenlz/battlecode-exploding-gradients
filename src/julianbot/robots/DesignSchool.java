package julianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import julianbot.robotdata.DesignSchoolData;

public class DesignSchool extends Robot {
	
	private DesignSchoolData designSchoolData;

	public DesignSchool(RobotController rc) {
		super(rc);
		this.data = new DesignSchoolData(rc, getSpawnerLocation());
		this.designSchoolData = (DesignSchoolData) this.data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
		if(turnCount == 1) determineBuildDirection();

		readTransactions();

    	RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), designSchoolData.getOpponent());
    	RobotInfo enemyHq = this.senseUnitType(RobotType.HQ, rc.getTeam().opponent());
    	
        if(enemy.length > 0) {
        	if (enemyHq != null) {
        		System.out.println("ATTACK THE HQ!");
	        	attackDesignSchoolProtocol(enemyHq.getLocation());
	        	return;
			}
	        
	        for(RobotInfo potentialThreat : enemy) {
	        	if(potentialThreat.type.isBuilding()) {
	        		System.out.println("ATTACK THE BUILDING!");
	        		attackDesignSchoolProtocol(potentialThreat.getLocation());
	        		return;
	        	} 
	        }
        } else {
        	designSchoolData.setBuildDirection(designSchoolData.getDefaultBuildDirection());
        }

        designSchoolData.setPauseBuildTimer(designSchoolData.getPauseBuildTimer() - 1);
        if(!designSchoolData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
        
        boolean oughtBuildLandscaper = oughtBuildLandscaper();
        boolean adjacentFlooding = isFloodingAdjacent();
        System.out.println("Ought build for building's sake? " + oughtBuildLandscaper + " Flooding imminent? " + adjacentFlooding);
    	if(oughtBuildLandscaper || adjacentFlooding) {
    		System.out.println("Design school ought build a landscaper.");
    		if(designSchoolData.getLandscapersBuilt() > 0 && onMapEdge(rc.getLocation().add(designSchoolData.getDefaultBuildDirection()))) {
    			designSchoolData.setDefaultBuildDirection(designSchoolData.getDefaultAttackBuildDirection());
    			designSchoolData.setBuildDirection(designSchoolData.getDefaultAttackBuildDirection());
    		}
    		
    		if(!tryBuild() && designSchoolData.getBuildDirection().equals(designSchoolData.getDefaultBuildDirection())) {
    			MapLocation attemptedBuildLocation = rc.getLocation().add(designSchoolData.getBuildDirection());
    			if(rc.canSenseLocation(attemptedBuildLocation)) {
    				boolean wallAtBuildLocation = rc.senseElevation(attemptedBuildLocation) - rc.senseElevation(rc.getLocation()) > GameConstants.MAX_DIRT_DIFFERENCE;
    				boolean mapEdgeAtBuildLocation = onMapEdge(attemptedBuildLocation);
    				if(wallAtBuildLocation || mapEdgeAtBuildLocation && rc.getTeamSoup() >= RobotType.LANDSCAPER.cost) {
    					System.out.println("Wall built! We can proceed to attack landscapers.");
    					//The wall already exists, so we can start building northwards to generate attack landscapers.  
    					designSchoolData.setDefaultBuildDirection(designSchoolData.getDefaultAttackBuildDirection());
    				} else if(adjacentFlooding) {
    					System.out.println("Flooding is still imminent! We need to force a build.");
    					//A wall build failed, so we need to try building in all directions.
    					forceBuild();
    				}
    			}
    		}
    	}
	}

	private void determineBuildDirection() {
		RobotInfo hq = senseUnitType(RobotType.HQ, rc.getTeam());

		if (hq != null) {
			MapLocation hqLocation = hq.getLocation();

			int mapWidth = rc.getMapWidth();
			int mapHeight = rc.getMapHeight();

			boolean leftEdge = hqLocation.x <= 0;
			boolean rightEdge = hqLocation.x >= mapWidth - 1;
			boolean topEdge = hqLocation.y >= mapHeight - 1;
			boolean bottomEdge = hqLocation.y <= 0;
			
			designSchoolData.initializeWallData(hqLocation, mapWidth, mapHeight);

			if (leftEdge) {
				if (bottomEdge) {
					designSchoolData.setBuildDirection(Direction.NORTH);
					designSchoolData.setDefaultBuildDirection(Direction.NORTH);
					designSchoolData.setDefaultAttackBuildDirection(Direction.EAST);
				} else if (topEdge) {
					designSchoolData.setBuildDirection(Direction.SOUTH);
					designSchoolData.setDefaultBuildDirection(Direction.SOUTH);
					designSchoolData.setDefaultAttackBuildDirection(Direction.EAST);
				} else {
					designSchoolData.setBuildDirection(Direction.NORTH);
					designSchoolData.setDefaultBuildDirection(Direction.NORTH);
					designSchoolData.setDefaultAttackBuildDirection(Direction.EAST);
				}
			} else if (rightEdge) {
				if (bottomEdge) {
					designSchoolData.setBuildDirection(Direction.NORTH);
					designSchoolData.setDefaultBuildDirection(Direction.NORTH);
					designSchoolData.setDefaultAttackBuildDirection(Direction.WEST);
				} else if (topEdge) {
					designSchoolData.setBuildDirection(Direction.SOUTH);
					designSchoolData.setDefaultBuildDirection(Direction.SOUTH);
					designSchoolData.setDefaultAttackBuildDirection(Direction.WEST);
				} else {
					designSchoolData.setBuildDirection(Direction.SOUTH);
					designSchoolData.setDefaultBuildDirection(Direction.SOUTH);
					designSchoolData.setDefaultAttackBuildDirection(Direction.WEST);
				}
			} else if (topEdge) {
				designSchoolData.setBuildDirection(Direction.EAST);
				designSchoolData.setDefaultBuildDirection(Direction.EAST);
				designSchoolData.setDefaultAttackBuildDirection(Direction.SOUTH);
			} else if (bottomEdge) {
				designSchoolData.setBuildDirection(Direction.WEST);
				designSchoolData.setDefaultBuildDirection(Direction.WEST);
				designSchoolData.setDefaultAttackBuildDirection(Direction.NORTH);
			} else {
				designSchoolData.setBuildDirection(Direction.WEST);
				designSchoolData.setDefaultBuildDirection(Direction.WEST);
				designSchoolData.setDefaultAttackBuildDirection(Direction.NORTH);
			}
		}
	}
	
	/**
	 * If design school senses enemy HQ, continually builds landscapers and sends out pause build transaction.
	 * @throws GameActionException
	 */
	private void attackDesignSchoolProtocol(MapLocation target) throws GameActionException {
		System.out.println("Attack Design School Protocol");
		if (!designSchoolData.getIsAttackSchool()) {
			designSchoolData.setIsAttackSchool(true);
			sendTransaction(15, Type.TRANSACTION_PAUSE_LANDSCAPER_BUILDING, rc.getLocation());
		}
		
		int allyLandscapers = this.senseNumberOfUnits(RobotType.LANDSCAPER, rc.getTeam());
		int opposingLandscapers = this.senseNumberOfUnits(RobotType.LANDSCAPER, rc.getTeam().opponent());
		if((allyLandscapers >= 3 && allyLandscapers > opposingLandscapers) || allyLandscapers >= 5) return;
		
		Direction directionToTarget = rc.getLocation().directionTo(target);
		designSchoolData.setBuildDirection(directionToTarget);
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft());
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateRight());
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft().rotateLeft());
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateRight().rotateRight());
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft().rotateLeft().rotateLeft());
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateRight().rotateRight().rotateRight());
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft().rotateLeft());
		if(tryBuild()) return;
		
		designSchoolData.setBuildDirection(directionToTarget.opposite());
		tryBuild();
	}

	private boolean oughtBuildLandscaper() {
		//Build a landscaper if the fulfillment center has been built but no landscapers are present.
//		int landscapersPresent = GeneralCommands.senseNumberOfUnits(rc, RobotType.LANDSCAPER, rc.getTeam());
		if (designSchoolData.getPauseBuildTimer() > 0) return false;
		if (designSchoolData.getLandscapersBuilt() == 0) return designSchoolData.getBuildSitesBlocked() || senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam()) != null;
		
		return (designSchoolData.isStableSoupIncomeConfirmed() || designSchoolData.getBuildSitesBlocked()) ? rc.getTeamSoup() >= RobotType.LANDSCAPER.cost : rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5;
	}
	
	private boolean isFloodingAdjacent() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		
		for(Direction direction : Robot.directions) {
			MapLocation potentialFloodingLocation = rcLocation.add(direction);
			if(rc.canSenseLocation(potentialFloodingLocation) && rc.senseFlooding(potentialFloodingLocation)) return true;
		}
		
		return false;
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @return true if a move was performed
     * @throws GameActionException
     */
    private boolean tryBuild() throws GameActionException {
    	Direction buildDirection = designSchoolData.getBuildDirection();
    	
    	waitUntilReady();
        if (rc.canBuildRobot(RobotType.LANDSCAPER, buildDirection)) {
            rc.buildRobot(RobotType.LANDSCAPER, buildDirection);
            designSchoolData.incrementLandscapersBuilt();
            return true;
        }
        
        return false;
    }
    
    private boolean forceBuild() throws GameActionException {
    	waitUntilReady();
    	
    	for(Direction direction : Robot.directions) {
    		if (rc.canBuildRobot(RobotType.LANDSCAPER, direction)) {
    			rc.buildRobot(RobotType.LANDSCAPER, direction);
    			designSchoolData.incrementLandscapersBuilt();
    			return true;
    		}
    	}
    
    	return false;
    }
    
    private void confirmStableSoupIncome() throws GameActionException {
    	if(senseUnitType(RobotType.VAPORATOR, rc.getTeam()) != null) {
    		designSchoolData.setStableSoupIncomeConfirmed(true);
    	}
    		    	
    	/*
    	for(int i = designSchoolData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			if(message.length >= 4) {
    				if(message[1] == Robot.Type.TRANSACTION_FRIENDLY_REFINERY_AT_LOC.getVal()) {
    					designSchoolData.setStableSoupIncomeConfirmed(true);
    					System.out.println("Stable soup income confirmed!");
    					return;
    				}
    			}
    		}
    		
    		if(Clock.getBytecodesLeft() <= 200) {
    			designSchoolData.setTransactionRound(i + 1);
    			break;
    		}
    	}
    	*/
    }
	
	private void readTransactions() throws GameActionException {
    	for(int i = designSchoolData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		designSchoolData.setTransactionRound(i);
    		
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			
    			if (message.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
    				Robot.Type category = Robot.Type.enumOfValue(message[1]);

    				if (category == null) {
    					System.out.println("Something is terribly wrong. enumOfValue returns null. Miner readTransaction line ~621");
    				}
    				
    				switch(category) {
    					case TRANSACTION_PAUSE_LANDSCAPER_BUILDING:
    						System.out.println("Pausing building...");
    						designSchoolData.setPauseBuildTimer(150);
    						break;
    					case TRANSACTION_BUILD_SITE_BLOCKED:
    						designSchoolData.setBuildSitesBlocked(true);
    						break;
    					default:
    						break;
    				}
    			}
    		}
    	}
    }
	
}

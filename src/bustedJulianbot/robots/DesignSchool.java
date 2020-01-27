package bustedJulianbot.robots;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Transaction;
import bustedJulianbot.robotdata.DesignSchoolData;

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
		
		if(turnCount == 1) {
			learnHqLocation();
			designSchoolData.initializeWallData(designSchoolData.getHqLocation(), rc.getMapWidth(), rc.getMapHeight());
			discernInitialRole();
			determineBuildDirection();
		}

		readTransactions();
		
		designSchoolData.setPauseBuildTimer(designSchoolData.getPauseBuildTimer() - 1);

    	
		switch(designSchoolData.getCurrentRole()) {
			case DesignSchoolData.ROLE_ATTACKER:
				attackDesignSchoolProtocol();
				break;
			case DesignSchoolData.ROLE_OBSTRUCTION_CLEARER:
				obstructionClearerDesignSchoolProtocol();
				break;
			case DesignSchoolData.ROLE_WALL_BUILDER:
				wallBuilderDesignSchoolProtocol();
				break;
		}
	}
	
	private void learnHqLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				designSchoolData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}
	}
	
	private void discernInitialRole() {		
		RobotInfo enemyHq = senseUnitType(RobotType.HQ, rc.getTeam().opponent());
		if(enemyHq != null && rc.getLocation().distanceSquaredTo(enemyHq.getLocation()) < rc.getLocation().distanceSquaredTo(designSchoolData.getHqLocation())) {
			designSchoolData.setCurrentRole(DesignSchoolData.ROLE_ATTACKER);
			designSchoolData.setEnemyHqLocation(enemyHq.getLocation());
		} else {
			designSchoolData.setCurrentRole(DesignSchoolData.ROLE_WALL_BUILDER);
		}
	}

	private void determineBuildDirection() {
		if(!rc.getLocation().equals(designSchoolData.getDesignSchoolBuildSite())) return;

		MapLocation hqLocation = designSchoolData.getHqLocation();

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
	
	/**
	 * If design school senses enemy HQ, continually builds landscapers and sends out pause build transaction.
	 * @throws GameActionException
	 */
	
	private void attackDesignSchoolProtocol() throws GameActionException {
		if(designSchoolData.getCurrentRole() == DesignSchoolData.ROLE_ATTACKER) {
    		if (designSchoolData.getEnemyHqLocation() != null) {
        		System.out.println("ATTACK THE HQ!");
	        	attackTarget(designSchoolData.getEnemyHqLocation());
	        	return;
			}
	        
    		RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), designSchoolData.getOpponent());
	        for(RobotInfo potentialThreat : enemy) {
	        	if(potentialThreat.type.isBuilding()) {
	        		System.out.println("ATTACK THE BUILDING!");
	        		attackTarget(potentialThreat.getLocation());
	        		return;
	        	} 
	        }
    	}
	}
	
	private void attackTarget(MapLocation target) throws GameActionException {
		System.out.println("Attack Design School Protocol");
		if (!designSchoolData.getIsAttackSchool()) {
			designSchoolData.setIsAttackSchool(true);
			sendTransaction(15, Type.TRANSACTION_PAUSE_LANDSCAPER_BUILDING, rc.getLocation());
		}
		
		int allyLandscapers = this.senseNumberOfUnits(RobotType.LANDSCAPER, rc.getTeam());
		int opposingLandscapers = this.senseNumberOfUnits(RobotType.LANDSCAPER, rc.getTeam().opponent());
		if((allyLandscapers >= 3 && allyLandscapers > opposingLandscapers) || allyLandscapers >= 5 || isFloodingImminent(designSchoolData.getHqLocation(), (int)(RobotType.LANDSCAPER.cost * 1.5)) || isFloodingAdjacent()) return;
		
		forceBuildTowardsTarget(target);
	}
	
	private void obstructionClearerDesignSchoolProtocol() throws GameActionException {
		if(designSchoolData.getLandscapersBuilt() == 0) forceBuildTowardsTarget(designSchoolData.getHqLocation());
	}
	
	private void wallBuilderDesignSchoolProtocol() throws GameActionException {
		designSchoolData.setBuildDirection(designSchoolData.getDefaultBuildDirection());
        if(!designSchoolData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
        
        boolean oughtBuildLandscaper = oughtBuildLandscaper();
        boolean adjacentFlooding = isFloodingAdjacent();
        System.out.println("Ought build for building's sake? " + oughtBuildLandscaper + " Flooding imminent? " + adjacentFlooding);
        if(adjacentFlooding && !designSchoolData.getIsAttackSchool()) {
			System.out.println("Flooding is imminent! We need to force a build.");
			forceBuild();
		} else if(oughtBuildLandscaper) {
    		MapLocation buildLocation = getBuildSiteNearWall(designSchoolData.getHqLocation(), 3, Integer.MAX_VALUE);
    		attemptConstruction(RobotType.LANDSCAPER, rc.getLocation().directionTo(buildLocation));
    	}
	}

	private boolean oughtBuildLandscaper() throws GameActionException {
		//Build a landscaper if the fulfillment center has been built but no landscapers are present.
		System.out.println("Ought build landscaper?");
		if(designSchoolData.getPauseBuildTimer() > 0 && !designSchoolData.getBuildSitesBlocked()) return false;
		
		boolean floodingImminent = isFloodingImminent(designSchoolData.getHqLocation(), (int)(RobotType.LANDSCAPER.cost * 1.5));
		boolean floodingAdjacent = isFloodingAdjacent();
		
		if(designSchoolData.getLandscapersBuilt() < 10) return true;
				
		return (designSchoolData.isStableSoupIncomeConfirmed() || designSchoolData.getBuildSitesBlocked()) ? rc.getTeamSoup() >= RobotType.LANDSCAPER.cost : rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5;
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
    
    private boolean forceBuildTowardsTarget(MapLocation target) throws GameActionException {
    	waitUntilReady();
    	
    	Direction directionToTarget = rc.getLocation().directionTo(target);
		designSchoolData.setBuildDirection(directionToTarget);
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft());
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateRight());
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft().rotateLeft());
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateRight().rotateRight());
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft().rotateLeft().rotateLeft());
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateRight().rotateRight().rotateRight());
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.rotateLeft().rotateLeft());
		if(tryBuild()) return true;
		
		designSchoolData.setBuildDirection(directionToTarget.opposite());
		if(tryBuild()) tryBuild();
    
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
    					case TRANSACTION_KILL_ORDER:
    						System.out.println("Pausing building...");
    						designSchoolData.setPauseBuildTimer(message[5]);
    						break;
    					case TRANSACTION_BUILD_SITE_BLOCKED:
    						designSchoolData.setBuildSitesBlocked(true);
    						break;
    					case TRANSACTION_FRIENDLY_REFINERY_AT_LOC:
    						designSchoolData.setRefineryBuilt(true);
    						designSchoolData.setWaitingOnRefinery(false);
    						break;
    					default:
    						break;
    				}
    			}
    		}
    	}
    }
	
}

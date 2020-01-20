package julianbot.robots;

import battlecode.common.*;
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

		//TODO: Either rebuild this for loop not to repeat read or scrap it altogether.
		if (turnCount < GameConstants.INITIAL_COOLDOWN_TURNS) {
			for (int i = 1; i < rc.getRoundNum(); i++) {
				readTransaction(rc.getBlock(i));
			}
		}

		readTransaction(rc.getBlock(rc.getRoundNum() - 1));

		if (designSchoolData.getIsAttackSchool()) {
			attackDesignSchoolProtocol();
			return;
		}

    	RobotInfo[] enemy = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), designSchoolData.getOpponent());

        if(enemy.length > 0) {
	        for(RobotInfo potentialThreat : enemy) { 
	        	if(potentialThreat.type.equals(RobotType.DESIGN_SCHOOL)) {
	        		designSchoolData.setBuildDirection(rc.getLocation().directionTo(potentialThreat.location).rotateLeft());
	        		tryBuild(RobotType.LANDSCAPER);
	        		return;
	        	} else if (potentialThreat.getType() == RobotType.HQ) {
	        		designSchoolData.setBuildDirection(rc.getLocation().directionTo(potentialThreat.getLocation()).rotateLeft());
	        		attackDesignSchoolProtocol();
				}
	        }
        } else {
        	designSchoolData.setBuildDirection(designSchoolData.getDefaultBuildDirection());
        }

        designSchoolData.setPauseBuildTimer(designSchoolData.getPauseBuildTimer() - 1);
        if(!designSchoolData.isStableSoupIncomeConfirmed()) confirmStableSoupIncome();
    	if(oughtBuildLandscaper()) {
    		if(!tryBuild(RobotType.LANDSCAPER) && designSchoolData.getBuildDirection().equals(designSchoolData.getDefaultBuildDirection())) {
    			MapLocation attemptedBuildLocation = rc.getLocation().add(designSchoolData.getBuildDirection());
    			if(rc.canSenseLocation(attemptedBuildLocation)) {
    				if(rc.senseElevation(attemptedBuildLocation) - rc.senseElevation(rc.getLocation()) > GameConstants.MAX_DIRT_DIFFERENCE) {
    					//The wall already exists, so we can start building northwards to generate attack landscapers.  
    					designSchoolData.setDefaultBuildDirection(designSchoolData.getDefaultAttackBuildDirection());
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
	private void attackDesignSchoolProtocol() throws GameActionException {
		System.out.println("Attack Design School Protocol");
		if (!designSchoolData.getIsAttackSchool()) {
			designSchoolData.setIsAttackSchool(true);
			sendTransaction(15, Type.TRANSACTION_PAUSE_LANDSCAPER_BUILDING, rc.getLocation());
		}
		while(!tryBuild(RobotType.LANDSCAPER)) {
			designSchoolData.setBuildDirection(designSchoolData.getBuildDirection().rotateRight());
		}
	}

	private boolean oughtBuildLandscaper() {
		//Build a landscaper if the fulfillment center has been built but no landscapers are present.
//		int landscapersPresent = GeneralCommands.senseNumberOfUnits(rc, RobotType.LANDSCAPER, rc.getTeam());
		if (designSchoolData.getPauseBuildTimer() > 0) return false;
		if (designSchoolData.getLandscapersBuilt() == 0) return senseUnitType(RobotType.FULFILLMENT_CENTER, rc.getTeam()) != null;
		return (designSchoolData.isStableSoupIncomeConfirmed()) ? rc.getTeamSoup() >= RobotType.LANDSCAPER.cost : rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 5;
	}
	
	/**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @return true if a move was performed
     * @throws GameActionException
     */
    private boolean tryBuild(RobotType type) throws GameActionException {
    	Direction buildDirection = designSchoolData.getBuildDirection();
    	
    	waitUntilReady();
        if (rc.isReady() && rc.canBuildRobot(type, buildDirection)) {
            rc.buildRobot(type, buildDirection);
            if(type == RobotType.LANDSCAPER) designSchoolData.incrementLandscapersBuilt();
            return true;
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

	/**
	 *
	 */
	private void readTransaction(Transaction[] block) throws GameActionException {
		for (Transaction message : block) {
			int[] decodedMessage = decodeTransaction(message);
			if (decodedMessage.length == GameConstants.NUMBER_OF_TRANSACTIONS_PER_BLOCK) {
				Robot.Type category = Robot.Type.enumOfValue(decodedMessage[1]);

				if (category == null) {
					System.out.println("Something is terribly wrong. enumOfValue returns null. Miner readTransaction line ~621");
				}
				switch(category) {
					case TRANSACTION_PAUSE_LANDSCAPER_BUILDING:
						System.out.println("Pausing building...");
						designSchoolData.setPauseBuildTimer(150);
						break;
					default:
						break;
				}
			}

		}
	}
	
}

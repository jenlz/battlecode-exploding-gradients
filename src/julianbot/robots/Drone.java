package julianbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Transaction;
import julianbot.robotdata.DroneData;

public class Drone extends Robot {

	private DroneData droneData;
	
	public Drone(RobotController rc) {
		super(rc);
		this.data = new DroneData(rc, getSpawnerLocation());
		this.droneData = (DroneData) data;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
		if(turnCount == 1) learnHQLocation();
    	if(droneData.getEnemyHqLocation() == null) learnEnemyHqLocation();
    	
    	if(droneData.receivedKillOrder()) {
    		if(rc.getRoundNum() - droneData.getKillOrderReceptionRound() >= 75) {
    			//ATTACK
    			routeTo(droneData.getEnemyHqLocation());
    			
    			if(rc.isCurrentlyHoldingUnit()) {
					if(!droneData.getHoldingEnemy()) {
						if(rc.getLocation().isWithinDistanceSquared(droneData.getEnemyHqLocation(), 8)) dropUnitNextToEnemyHq();
					} else {
						//Drop enemies in the ocean.
					}
				} else {
					RobotInfo[] enemies = rc.senseNearbyRobots(-1, droneData.getOpponent());
					
					for(RobotInfo enemy : enemies) {
						if(pickUpUnit(enemy)) {
							break;
						}
					}
				}
    		} else {
	    		//APPROACH
				if(rc.getLocation().distanceSquaredTo(droneData.getEnemyHqLocation()) > 25) routeTo(droneData.getEnemyHqLocation());
    		}
		} else if(droneData.isAwaitingKillOrder()) {
			if(rc.isCurrentlyHoldingUnit()) {
				if(!droneData.getHoldingEnemy()) {
					//Every other drone should replace their landscaper onto the wall.
					if(Math.abs(droneData.getHqLocation().y - rc.getLocation().y) == 1 || Math.abs(droneData.getHqLocation().x - rc.getLocation().x) == 1) 
						dropUnit(rc.getLocation().directionTo(droneData.getHqLocation()));
				}
			}
			
    		if(readKillOrder()) {
    			droneData.setReceivedKillOrder(true);
    			droneData.setKillOrderReceptionRound(rc.getRoundNum());
    		}
    	} else if(droneData.getEnemyHqLocation() != null) {
    	  senseAdjacentFlooding();    		
    		if(!rc.isCurrentlyHoldingUnit()) {
        		if(senseUnitType(RobotType.LANDSCAPER,data.getOpponent())!=null && (senseUnitType(RobotType.HQ,data.getTeam())!=null || senseUnitType(RobotType.HQ,data.getOpponent())!=null)) {
        			if(!pickUpUnit(RobotType.LANDSCAPER, data.getOpponent())) {
        				if(senseUnitType(RobotType.HQ, data.getTeam())!=null)
        					routeTo(droneData.getHqLocation());
        				else
        					routeTo(droneData.getEnemyHqLocation());
        			}
        			else {
        				droneData.setHoldingEnemy(true);
        				if(senseUnitType(RobotType.HQ, data.getTeam())!=null)
        					droneData.setEnemyFrom(data.getTeam());
        				else
        					droneData.setEnemyFrom(data.getOpponent());
        			}
        		}
        	}
        	
        	if(droneData.getHoldingEnemy()) {
					  drownEnemyProtocol();
        	}
    		
    		if(rc.isCurrentlyHoldingUnit() && !droneData.getHoldingEnemy()) {
				//Route to appropriate location (any location three units away from the HQ in either direction) and wait for kill order.
				if(rc.getLocation().equals(droneData.getAttackWaitLocation())) {
					droneData.setAwaitingKillOrder(true);
				} else if(rc.canSenseLocation(droneData.getAttackWaitLocation())) {
					if(!rc.isLocationOccupied(droneData.getAttackWaitLocation())) {
						routeTo(droneData.getAttackWaitLocation());
					} else {
						droneData.proceedToNextWaitLocation();
					}
				} else {
					routeTo(droneData.getAttackWaitLocation());
				}
    		} else if (!rc.isCurrentlyHoldingUnit()) {
    			boolean oughtPickUpCow = oughtPickUpCow();
    			boolean oughtPickUpLandscaper = oughtPickUpLandscaper();
    			
    			if(senseUnitType(RobotType.COW) != null && oughtPickUpCow) {
	    			if(!pickUpUnit(RobotType.COW)) {
	    				routeTo(droneData.getHqLocation());
	    			} else {
	    				droneData.setHoldingEnemy(true);
					}
	    		} else if(oughtPickUpLandscaper) {
	    			RobotInfo idleAttackLandscaper = senseAttackLandscaper();
	    			
	    			if(idleAttackLandscaper != null) {
	    				if(!pickUpUnit(idleAttackLandscaper)) {
		    				routeTo(idleAttackLandscaper.getLocation());
		    			}
	    			} else {
	    				routeTo(droneData.getHqLocation().translate(0, 3));
	    			}
	    		} else if(!rc.getLocation().isWithinDistanceSquared(droneData.getHqLocation(), 3)) {
	    			routeTo(droneData.getHqLocation().translate(0, 3));
	    		} else {
	    			routeTo(droneData.getEnemyHqLocation());
	    		}
    		}
    	} else {
    		if(!droneData.searchDestinationsDetermined()) {
    			droneData.calculateSearchDestinations(rc);
    		}
    		
    		routeTo(droneData.getActiveSearchDestination());
    		attemptEnemyHQDetection();
    		if(droneData.getEnemyHqLocation() != null) {
    			sendTransaction(10, Robot.Type.TRANSACTION_ENEMY_HQ_AT_LOC, droneData.getEnemyHqLocation());
    		}
    	}
	}


	/**
	 * Once drone is holding enemy, will search for flooded area and drop it there.
	 */
	private void drownEnemyProtocol() throws GameActionException {
		System.out.println("Entered Drown Enemy Protocol");
		System.out.println("Flooded locs " + droneData.getFloodedLocs());
		while (droneData.getHoldingEnemy()) {
			if (droneData.getFloodedLocs().size() > 0) {
				System.out.println("Moving toward flooded loc");
				MapLocation closestLoc = locateClosestLocation(droneData.getFloodedLocs(), rc.getLocation());
				if (rc.getLocation().distanceSquaredTo(closestLoc) > 3) {
					routeTo(closestLoc);
				}

				if (rc.isReady() && rc.getLocation().isAdjacentTo(closestLoc) && rc.canDropUnit(rc.getLocation().directionTo(closestLoc))) {
					System.out.println("Dropping enemy into water");
					rc.dropUnit(rc.getLocation().directionTo(closestLoc));
					droneData.setHoldingEnemy(false);
				}

			} else {
				System.out.println("Moving and searching for flooding");
				continueSearch();
				senseAdjacentFlooding();
			}
		}
	}

	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				droneData.setHqLocation(new MapLocation(message[2], message[3]));
				droneData.calculateInitialAttackWaitLocation();
				return;
			}
		}
	}
	
	private void learnEnemyHqLocation() throws GameActionException {
		for(int i = droneData.getTransactionRound(); i < rc.getRoundNum(); i++) {
    		for(Transaction transaction : rc.getBlock(i)) {
    			int[] message = decodeTransaction(transaction);
    			if(message.length >= 4) {
    				if(message[1] == Robot.Type.TRANSACTION_ENEMY_HQ_AT_LOC.getVal()) {
    					droneData.setEnemyHqLocation(new MapLocation(message[2], message[3]));
    					return;
    				}
    			}
    		}
    		
    		droneData.setTransactionRound(i + 1);
    		if(Clock.getBytecodesLeft() <= 200) {
    			break;
    		}
    	}
	}
	
	private void attemptEnemyHQDetection() {
		RobotInfo enemyHQ = senseUnitType(RobotType.HQ, rc.getTeam().opponent());
		if(enemyHQ != null) {
			droneData.setEnemyHqLocation(enemyHQ.getLocation());
		} else if(rc.canSenseLocation(droneData.getActiveSearchDestination())){
			droneData.proceedToNextSearchDestination();
		}
	}
	
	private boolean oughtPickUpCow() {
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from moving cows that are already nearer to the opponent's HQ.
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(droneData.getSpawnerLocation()) < rcLocation.distanceSquaredTo(droneData.getEnemyHqLocation());
	}
	
	private boolean oughtPickUpLandscaper() {		
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from dropping of a landscaper, then immediately detecting it and picking it up again.
		//Also, don't pick up landscapers until there is a surplus so our wall doesn't stop rising.
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(data.getSpawnerLocation()) < rcLocation.distanceSquaredTo(droneData.getEnemyHqLocation())
				&& senseNumberOfUnits(RobotType.LANDSCAPER, rc.getTeam()) > 2;
	}
	
	private RobotInfo senseAttackLandscaper() throws GameActionException {
		RobotInfo topLeft = senseAttackLandscaperAt(droneData.getHqLocation().translate(-1, 1));
		if(topLeft != null) return topLeft;
		
		RobotInfo topMiddle = senseAttackLandscaperAt(droneData.getHqLocation().translate(0, 1));
		if(topMiddle != null) return topMiddle;
		
		RobotInfo topRight = senseAttackLandscaperAt(droneData.getHqLocation().translate(1, 1));
		if(topRight != null) return topRight;
		
		return null;
	}
	
	private RobotInfo senseAttackLandscaperAt(MapLocation location) throws GameActionException {
		if(rc.canSenseLocation(location)) {
			RobotInfo info = rc.senseRobotAtLocation(location);
			if(info != null && info.type == RobotType.LANDSCAPER) return info;
		}
		
		return null;
	}
	
	private boolean pickUpUnit(RobotInfo info) throws GameActionException {
		if(info != null) {
			waitUntilReady();
			
			if(rc.canPickUpUnit(info.ID)) {
				rc.pickUpUnit(info.ID);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean pickUpUnit(RobotType targetType) throws GameActionException {
		RobotInfo info = senseUnitType(targetType);
		if(info != null) {
			waitUntilReady();
			
			if(rc.canPickUpUnit(info.ID)) {
				rc.pickUpUnit(info.ID);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean pickUpUnit(RobotType targetType, Team targetTeam) throws GameActionException {
		RobotInfo info = senseUnitType(targetType, targetTeam);
		
		if(info != null) {
			waitUntilReady();
			
			if(rc.canPickUpUnit(info.ID)) {
				rc.pickUpUnit(info.ID);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean dropUnit(Direction dropDirection) throws GameActionException {
		waitUntilReady();
		
		if(rc.canDropUnit(dropDirection)) {
			rc.dropUnit(dropDirection);
			return true;
		}
		
		return false;
	}

	/**
	 * Senses flooding of adjacent tiles
	 * @throws GameActionException
	 */
	private void senseAdjacentFlooding() throws GameActionException {
		for (Direction dir : Direction.allDirections()) {
			if (rc.canSenseLocation(rc.adjacentLocation(dir)) && rc.senseFlooding(rc.adjacentLocation(dir))) {
				System.out.println("Storing flooded loc");
				rc.setIndicatorDot(rc.adjacentLocation(dir), 255, 165, 0);
				droneData.addFloodedLoc(rc.adjacentLocation(dir));
			}
		}
	}
	
	private boolean dropUnitNextToEnemyHq() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = droneData.getEnemyHqLocation();
		
		Direction directionToHQ = rcLocation.directionTo(hqLocation);
		
		if(rcLocation.add(directionToHQ).isWithinDistanceSquared(hqLocation, 3))
			if(dropUnit(directionToHQ)) return true;
		
		if(rcLocation.add(directionToHQ.rotateLeft()).isWithinDistanceSquared(hqLocation, 3))
			if(dropUnit(directionToHQ.rotateLeft())) return true;
		
		if(rcLocation.add(directionToHQ.rotateRight()).isWithinDistanceSquared(hqLocation, 3))
			if(dropUnit(directionToHQ.rotateRight())) return true;
		
		if(rcLocation.add(directionToHQ.rotateLeft().rotateLeft()).isWithinDistanceSquared(hqLocation, 3))
			if(dropUnit(directionToHQ.rotateLeft().rotateLeft())) return true;
		
		if(rcLocation.add(directionToHQ.rotateRight().rotateRight()).isWithinDistanceSquared(hqLocation, 3))
			if(dropUnit(directionToHQ.rotateRight().rotateRight())) return true;
		
		return false;
	}
	
	private boolean readKillOrder() throws GameActionException {
		for(Transaction transaction : rc.getBlock(rc.getRoundNum() - 1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length >= 4) {
				if(message[1] == Robot.Type.TRANSACTION_KILL_ORDER.getVal()) {
					return true;
				}
			}
		}
		
		return false;
	}
	
}

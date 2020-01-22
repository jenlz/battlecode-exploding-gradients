package julianbot.robots;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Transaction;
import julianbot.robotdata.DroneData;
import julianbot.utils.NumberMath;

public class Drone extends Scout {

	private static final int CARGO_DRONE_ATTACK_DELAY = 7;
	
	private DroneData droneData;
	
	public Drone(RobotController rc) {
		super(rc);
		this.data = new DroneData(rc, getSpawnerLocation());
		this.scoutData = (DroneData) this.data;
		this.droneData = (DroneData) scoutData;
	}
	
	//MOVEMENT
	@Override
	protected boolean move(Direction dir) throws GameActionException {
		stopFollowingPath();
		
		waitUntilReady();
		
		if(rc.isReady() && rc.canMove(dir)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}
	
	@Override
	protected boolean moveOnPath(Direction dir) throws GameActionException {		
		waitUntilReady();
		
		if(rc.isReady() && rc.canMove(dir)) {
			data.setPreviousLocation(rc.getLocation());
			rc.move(dir);
			return true;
		}
		
		return false;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		
		System.out.println("Turn Count 1 Check");
		if(turnCount == 1) {
			learnHQLocation();
			droneData.calculateInitialAttackWaitLocation();
			determineEdgeState();
		}
		
		System.out.println("Enemy HQ Null Check");
    	if(droneData.getEnemyHqLocation() == null) learnEnemyHqLocation();
    	
    	
    	if(droneData.receivedKillOrder()) {
    		System.out.println("Received kill order");
    		killDroneProtocol();
		} else if(droneData.isAwaitingKillOrder()) {
			System.out.println("Idle hit man");
			idleHitManDroneProtocol();
    	} else if(droneData.getEnemyHqLocation() != null) {
    		System.out.println("Sensing flooding");
    		senseAdjacentFlooding();    
    		
    		if(!rc.isCurrentlyHoldingUnit()) {
    			System.out.println("No unit held");
    			int distanceSquaredFromEnemyHq = rc.getLocation().distanceSquaredTo(droneData.getEnemyHqLocation());
    			
    			//Distance checks are to prevent routing into the enemy HQ net gun's range.
        		if(distanceSquaredFromEnemyHq > 25 && liftNearbyEnemies()) {
        			/*Do nothing else.*/
        		} else if(distanceSquaredFromEnemyHq > 25 && approachNearbyCows()) {
        			liftNearbyCows();
        		} else {
        			System.out.println("Cargo seeking");
        			cargoSeekerProtocol();
        		}
        	} else if(droneData.getHoldingEnemy()) {
        		System.out.println("Drown enemy prot");
        		drownEnemyProtocol();
        	} else if(droneData.getHoldingCow())  {
        		System.out.println("Drown cow prot");
        		drownCowProtocol();
        	} else {
        		System.out.println("Attack prep prot");
    			attackPreparationProtocol();
    		}
    	} else {
    		reconDroneProtocol();
    	}
	}
	
	private void learnHQLocation() throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = decodeTransaction(transaction);
			if(message.length > 1 && message[1] == Type.TRANSACTION_FRIENDLY_HQ_AT_LOC.getVal()) {
				droneData.setHqLocation(new MapLocation(message[2], message[3]));
				return;
			}
		}
	}

	private void determineEdgeState() {
		MapLocation hqLocation = droneData.getHqLocation();
		
		boolean leftEdge = hqLocation.x <= 0;
		boolean rightEdge = hqLocation.x >= rc.getMapWidth() - 1;
		boolean topEdge = hqLocation.y >= rc.getMapHeight() - 1;
		boolean bottomEdge = hqLocation.y <= 0;
		droneData.setBaseOnEdge(leftEdge || rightEdge || topEdge || bottomEdge);
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) droneData.setWallOffsetBounds(0, 2, 0, 3);
			else if(topEdge) droneData.setWallOffsetBounds(0, 2, -3, 0);
			else droneData.setWallOffsetBounds(0, 2, -1, 3);
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) droneData.setWallOffsetBounds(-2, 0, 0, 3);
			else if(topEdge) droneData.setWallOffsetBounds(-2, 0, -3, 0);
			else droneData.setWallOffsetBounds(-2, 0, -3, 1);
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			droneData.setWallOffsetBounds(-1, 3, 0, -2);
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			droneData.setWallOffsetBounds(-3, 1, 0, 2);
		} else {
			droneData.setWallOffsetBounds(-2, 2, -2, 2);
		}
		

		if(topEdge) {
			droneData.setGridOffset(0, -1);
		} else if(bottomEdge) {
			droneData.setGridOffset(0, 1);
		} else {
			droneData.setGridOffset(0, 0);
		}
	}
	
	/**
	 * Once drone is holding enemy, will search for flooded area and drop it there.
	 */
	private void drownEnemyProtocol() throws GameActionException {
		System.out.println("Entered Drown Enemy Protocol");
		MapLocation rcLoation = rc.getLocation();
		
		for(Direction direction : Direction.allDirections()) {
			if(rc.senseFlooding(rcLoation.add(direction))) {
				if(dropUnit(direction)) {
					droneData.setHoldingEnemy(false);
					return;
				}
			}
		}
		
		if (droneData.getFloodedLocs().size() > 0) {
			System.out.println("Moving toward flooded loc");
			MapLocation closestLoc = locateClosestLocation(droneData.getFloodedLocs(), rcLoation);
			if (rcLoation.distanceSquaredTo(closestLoc) > 3) {
				routeTo(closestLoc);
			} else if (dropUnit(rcLoation.directionTo(closestLoc))) {
				droneData.setHoldingEnemy(false);
			}

		}
	}
	
	private void drownCowProtocol() throws GameActionException {
		System.out.println("Entered Drown Dow Protocol");
		MapLocation rcLocation = rc.getLocation();
		
		for(Direction direction : Direction.allDirections()) {
			if(rc.canSenseLocation(rcLocation.add(direction)) && rc.senseFlooding(rcLocation.add(direction))) {
				if(dropUnit(direction)) {
					droneData.setHoldingCow(false);
					return;
				}
			}
		}
		
		if (droneData.getFloodedLocs().size() > 0) {
			System.out.println("Moving toward flooded loc");
			MapLocation closestLoc = locateClosestLocation(droneData.getFloodedLocs(), rcLocation);
			if (rcLocation.distanceSquaredTo(closestLoc) > 3) {
				routeTo(closestLoc);
			} else if (dropUnit(rcLocation.directionTo(closestLoc))) {
				droneData.setHoldingCow(false);
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
	
	private void killDroneProtocol() throws GameActionException {
		if(rc.getRoundNum() - droneData.getKillOrderReceptionRound() >= NumberMath.clamp(rc.getMapWidth() + rc.getMapHeight(), 75, Integer.MAX_VALUE) + CARGO_DRONE_ATTACK_DELAY) {
			//ATTACK ENEMY HQ
			routeTo(droneData.getEnemyHqLocation());
			
			if(rc.isCurrentlyHoldingUnit()) {
				if(!droneData.getHoldingEnemy()) {
					if(rc.getLocation().isWithinDistanceSquared(droneData.getEnemyHqLocation(), 8)) dropUnitNextToEnemyHq();
				} else {
					//Drop enemies and cows in the ocean.
					Direction adjacentFloodingDirection = getAdjacentFloodingDirection();
					if(adjacentFloodingDirection != null) {
						rc.dropUnit(adjacentFloodingDirection);
					}
				}
			} else {
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, droneData.getOpponent());
				
				for(RobotInfo enemy : enemies) {
					if(pickUpUnit(enemy)) {
						droneData.setHoldingEnemy(true);
						break;
					}
				}
			}
		} else {
    		//APPROACH ENEMY HQ
			
			//Let those not holding units go first to clear spaces.
			if(rc.isCurrentlyHoldingUnit() && rc.getRoundNum() - droneData.getKillOrderReceptionRound() < CARGO_DRONE_ATTACK_DELAY) return;
			if(rc.getLocation().distanceSquaredTo(droneData.getEnemyHqLocation()) > 25) routeTo(droneData.getEnemyHqLocation());
		}
	}
	
	private void idleHitManDroneProtocol() throws GameActionException {
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
	}
	
	private boolean liftNearbyEnemies() throws GameActionException {
		if(senseUnitType(RobotType.LANDSCAPER,data.getOpponent())!=null && (senseUnitType(RobotType.HQ,data.getTeam())!=null || senseUnitType(RobotType.HQ,data.getOpponent())!=null)) {
			if(!pickUpUnit(RobotType.LANDSCAPER, data.getOpponent())) {
				if(senseUnitType(RobotType.HQ, data.getTeam())!=null)
					routeTo(droneData.getHqLocation());
				else
					routeTo(droneData.getEnemyHqLocation());
				return false;
			} else {
				droneData.setHoldingEnemy(true);
				if(senseUnitType(RobotType.HQ, data.getTeam())!=null)
					droneData.setEnemyFrom(data.getTeam());
				else
					droneData.setEnemyFrom(data.getOpponent());
				return true;
			}
		}
		
		return false;
	}
	
	private boolean approachNearbyCows() throws GameActionException {
		RobotInfo[] cows = this.senseAllUnitsOfType(RobotType.COW, Team.NEUTRAL);
		if(cows.length == 0) return false;
		
		System.out.println("I SEE MY DELICIOUS CATTLEY PREY!");
		
		int[] cowSquaredDistances = new int[cows.length];
		for(int i = 0; i < cows.length; i++) {
			cowSquaredDistances[i] = rc.getLocation().distanceSquaredTo(cows[i].getLocation());
		}
		
		RobotInfo targetCow = cows[NumberMath.indexOfLeast(cowSquaredDistances)];
		if(targetCow.getLocation().isWithinDistanceSquared(rc.getLocation(), 3)) return true;
		return routeTo(targetCow.getLocation());
	}
	
	private boolean liftNearbyCows() throws GameActionException {
		if(!rc.isCurrentlyHoldingUnit()) {
			RobotInfo[] cows = this.senseAllUnitsOfType(RobotType.COW, Team.NEUTRAL);
			for(RobotInfo cow : cows) {
				if(pickUpUnit(cow)) {
					droneData.setHoldingCow(true);
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void attackPreparationProtocol() throws GameActionException {
		if(droneData.isWallBuildConfirmed()) {
			//If the wall build is confirmed, the drone can prepare to attack.
			System.out.println("Wall build confirmed!");
			//Route to appropriate location (any location three units away from the HQ in either direction) and wait for kill order.
			if(rc.getLocation().equals(droneData.getAttackWaitLocation())) {
				System.out.println("Waiting on the kill order!");
				droneData.setAwaitingKillOrder(true);
			} else {
				cycleNewAttackLocation();
				System.out.println("Routing to attack wait location " + droneData.getAttackWaitLocation());
				routeTo(droneData.getAttackWaitLocation());
			}
		} else {
			//If the wall build is not confirmed, the drone can proceed to move a landscaper to the next wall segment.
			System.out.println("Wall build NOT confirmed!");
			
			MapLocation nextWallSegment = droneData.getNextWallSegment();
			if(nextWallSegment == null) {
				System.out.println("Null next wall segment. Have we checked yet? " + droneData.isWallBuildChecked());
				
				if(!droneData.isWallBuildChecked()) checkWallBuild();
				else droneData.setWallBuildConfirmed(true);
				
				System.out.println("Wall build confirmed now? " + droneData.isWallBuildConfirmed());
			} else if(rc.canSenseLocation(nextWallSegment)) {
				System.out.println("Can sense next wall segment " + nextWallSegment);
				
				if(rc.getLocation().isWithinDistanceSquared(droneData.getNextWallSegment(), 3)) {
					System.out.println("Dropping on adjacent wall segment.");
					if(!dropUnit(rc.getLocation().directionTo(droneData.getNextWallSegment()))) {
						System.out.println("The wall drop failed, so we'll consider the wall built.");
						droneData.setWallBuildConfirmed(true);
					}
				} else if(rc.isLocationOccupied(nextWallSegment)) {
					System.out.println("Next wall segment is occupied, so the wall will be considered built!");
					droneData.setWallBuildConfirmed(true);
				} else {
					System.out.println("Routing to next wall segment!");
					routeTo(nextWallSegment);
				}
			} else {
				if(rc.onTheMap(nextWallSegment)) routeTo(nextWallSegment);
				else droneData.setWallBuildConfirmed(true);	
			}
		}
	}
	
	private void cycleNewAttackLocation() throws GameActionException {
		MapLocation attackWaitLocation = droneData.getAttackWaitLocation();
		while(!rc.onTheMap(attackWaitLocation) || onMapEdge(attackWaitLocation) || ((rc.canSenseLocation(attackWaitLocation)) ? droneAtLocation(attackWaitLocation) : false)) {
			droneData.proceedToNextAttackWaitLocation();
			attackWaitLocation = droneData.getAttackWaitLocation();
		}
	}
	
	private boolean droneAtLocation(MapLocation location) throws GameActionException {
		RobotInfo potentialDrone = rc.senseRobotAtLocation(location);
		if(potentialDrone == null) return false;
		return potentialDrone.getType() == RobotType.DELIVERY_DRONE;
	}
	
	private void cargoSeekerProtocol() throws GameActionException {
		boolean oughtPickUpCow = oughtPickUpCow();
		boolean oughtPickUpLandscaper = oughtPickUpLandscaper();
		
		System.out.println("No unit held! Lift Cow? " + oughtPickUpCow + " Lift Landscaper? " + oughtPickUpLandscaper);
		
		if(senseUnitType(RobotType.COW) != null && oughtPickUpCow) {
			System.out.println("Sensed a cow that ought be lifted.");
			if(!pickUpUnit(RobotType.COW)) {
				routeTo(droneData.getHqLocation());
			} else {
				droneData.setHoldingEnemy(true);
			}
		} else if(oughtPickUpLandscaper) {
			System.out.println("Ought lift a landscaper rather than a cow.");
			RobotInfo idleAttackLandscaper = senseAttackLandscaper();
			if(!rc.canSenseLocation(droneData.getHqLocation())) {
				routeTo(droneData.getHqLocation());
			} else if(!findVacanciesOnWall()) {
				RobotInfo[] adjacentLandscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
				for(RobotInfo adjacentLandscaper : adjacentLandscapers) {
					if(idleAttackLandscaper != null && adjacentLandscaper.getID() == idleAttackLandscaper.getID()) continue;
					System.out.println(idleAttackLandscaper.getID() + " =/= " + adjacentLandscaper.getID());
					
					if(adjacentLandscaper.getLocation().isWithinDistanceSquared(rc.getLocation(), 3)) {
						pickUpUnit(adjacentLandscaper);
						break;
					}
				}
				
			} else if(idleAttackLandscaper != null) {
				System.out.println("\tFound an idle attack landscaper");
				if(!pickUpUnit(idleAttackLandscaper)) {
    				routeTo(idleAttackLandscaper.getLocation());
    			} else {
    				checkWallBuild();
    			}
			} else {
				System.out.println("Routing to waiting point");
				routeTo(droneData.getHqLocation().translate(0, 3));
			}
		} else if(!rc.getLocation().isWithinDistanceSquared(droneData.getHqLocation(), 3)) {
			System.out.println("Ought not lift a landscaper, so routing to waiting point");
			routeTo(droneData.getHqLocation().translate(0, 3));
		} else {
			System.out.println("On our way to the enemy HQ");
			routeTo(droneData.getEnemyHqLocation());
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
				&& rc.senseNearbyRobots(droneData.getHqLocation(), 3, rc.getTeam()) != null;
	}
	
	private RobotInfo senseAttackLandscaper() throws GameActionException {
		MapLocation hqLocation = droneData.getHqLocation();
		
		RobotInfo[] landscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
		int xMin = droneData.getWallOffsetXMin();
		int xMax = droneData.getWallOffsetYMax();
		int yMin = droneData.getWallOffsetYMin();
		int yMax = droneData.getWallOffsetYMax();
		
		for(RobotInfo landscaper : landscapers) {
			MapLocation location = landscaper.getLocation();
			int dx = location.x - hqLocation.x;
			int dy = location.y - hqLocation.y;
			
			boolean xInWall = xMin < dx && dx < xMax;
			boolean yInWall = yMin < dy && dy < yMax;
			
			if(xInWall && yInWall) return landscaper;
		}

		return null;
	}
	
	private void reconDroneProtocol() throws GameActionException {
		if(!droneData.searchDestinationsDetermined()) {
			droneData.calculateSearchDestinations(rc);
		}
		
		attemptEnemyHQDetection();
		
		if(droneData.getEnemyHqLocation() != null) {
			sendTransaction(10, Robot.Type.TRANSACTION_ENEMY_HQ_AT_LOC, droneData.getEnemyHqLocation());
			System.out.println("Detected the HQ and sent a transaction!");
		}
		else cautiouslyApproachNetGun();
	}
	
	private void cautiouslyApproachNetGun() throws GameActionException {
		MapLocation searchDestination = droneData.getActiveSearchDestination();
		int distanceSquared = rc.getLocation().distanceSquaredTo(searchDestination);
		
		System.out.println("Distance from prospective HQ Location = " + distanceSquared);
		if(distanceSquared <= 25) {
			//If we move diagonally towards the HQ, we will be within net gun range and will be shot. We have to be pragmatic about how we move.
			for(Direction direction : Direction.allDirections()) {
				MapLocation targetLocation = rc.getLocation().add(direction);
				int prospectiveDistanceSquared = targetLocation.distanceSquaredTo(searchDestination);
				if(16 <= prospectiveDistanceSquared && prospectiveDistanceSquared < distanceSquared) {
					if(rc.canMove(direction)) {
						System.out.println("Alright, we\'re going to cautiously move " + direction);
						move(direction);
						break;
					}
				}
			}
		} else {
			routeTo(droneData.getActiveSearchDestination());
		}
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
	
	private Direction getAdjacentFloodingDirection() throws GameActionException {
		for(Direction direction : Direction.allDirections()) {
			if(rc.canSenseLocation(rc.adjacentLocation(direction)) && rc.senseFlooding(rc.adjacentLocation(direction))) {
				return direction;
			}
		}
		
		return null;
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
	
	private void checkWallBuild() throws GameActionException {
		MapLocation hqLocation = droneData.getHqLocation();
    	int hqElevation = rc.senseElevation(hqLocation);
    	droneData.setWallBuildChecked(true);
    	droneData.setWallBuildConfirmed(true);
    	
    	int minDx = droneData.getWallOffsetXMin();
    	int maxDx = droneData.getWallOffsetXMax();
    	int minDy = droneData.getWallOffsetYMin();
    	int maxDy = droneData.getWallOffsetYMax();
    	
    	for(int dx = minDx; dx <= maxDx; dx++) {
    		for(int dy = minDy; dy <= maxDy; dy++) {
    			MapLocation wallLocation = hqLocation.translate(dx, dy);
    			if(isOnWall(wallLocation, hqLocation) && rc.canSenseLocation(wallLocation)) {
    				if(rc.senseElevation(wallLocation) - hqElevation <= GameConstants.MAX_DIRT_DIFFERENCE) {
    					droneData.setWallBuildConfirmed(false);
    					if(!rc.isLocationOccupied(wallLocation)) {
    						MapLocation nextWallSegment = droneData.getNextWallSegment();
    						
    						if(nextWallSegment == null) {
    							droneData.setNextWallSegment(wallLocation);
    						} else {
    							if(wallLocation.x > nextWallSegment.x) droneData.setNextWallSegment(wallLocation);
        						else if(wallLocation.x == nextWallSegment.x && wallLocation.y > nextWallSegment.y) droneData.setNextWallSegment(wallLocation);
    						}    						
    					}
    				}
    			}
    		}
    	}
	}
	
	private boolean findVacanciesOnWall() throws GameActionException {
		MapLocation hqLocation = droneData.getHqLocation();
    	
    	int minDx = droneData.getWallOffsetXMin();
    	int maxDx = droneData.getWallOffsetXMax();
    	int minDy = droneData.getWallOffsetYMin();
    	int maxDy = droneData.getWallOffsetYMax();
    	
    	for(int dx = minDx; dx <= maxDx; dx++) {
    		for(int dy = minDy; dy <= maxDy; dy++) {
    			MapLocation wallLocation = hqLocation.translate(dx, dy);
    			if(isOnWall(wallLocation, hqLocation) && rc.canSenseLocation(wallLocation)) {
    				if(!rc.isLocationOccupied(wallLocation)) {
    					return true;
    				}
    			}
    		}
    	}
    	
    	return false;
	}
	
	private boolean onMapEdge(MapLocation location) {
		return location.x <= 0 || location.y <= 0 || location.x >= rc.getMapWidth() - 1 || location.y >= rc.getMapHeight() - 1;
	}
	
}

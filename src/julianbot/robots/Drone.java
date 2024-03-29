package julianbot.robots;

import java.util.ArrayList;
import java.util.List;

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
	public boolean continueSearchNonRandom() throws GameActionException {
		//The move function is deliberately unused here.
		waitUntilReady();

		if(rc.canMove(data.getSearchDirection())) {
			rc.move(data.getSearchDirection());
			return true;
		}
		
		return false;
	}
	
	//RUNNING
	@Override
	public void run() throws GameActionException {
		super.run();
		
		System.out.println("Turn Count 1 Check");
		if(turnCount == 1) {
			learnHqLocation();
			droneData.calculateInitialAttackWaitLocation();
			droneData.initializeWallData(droneData.getHqLocation(), rc.getMapWidth(), rc.getMapHeight());
			determineEdgeState();
		}
		
		System.out.println("Enemy HQ Null Check");
    	if(droneData.getEnemyHqLocation() == null) learnEnemyHqLocation();
    	
    	RobotInfo[] drownableEnemies = getDrownableEnemies();
    	System.out.println("There are " + drownableEnemies.length + " drownable enem(ies).");
    	
    	if(droneData.receivedKillOrder()) {
    		System.out.println("Received kill order");
    		killDroneProtocol();
		} else if(checkPotentialThreats(drownableEnemies)) {
			System.out.println("Found potential threat.");
    		defenseDroneProtocol(drownableEnemies);
    		return;
    	} else if(droneData.isAwaitingKillOrder()) {
			System.out.println("Idle hit man");
			if(rc.isCurrentlyHoldingUnit() && droneData.getHoldingEnemy()) drownEnemyProtocol();
			else if(rc.isCurrentlyHoldingUnit() && droneData.getHoldingCow()) drownCowProtocol();
			else idleHitManDroneProtocol();
    	} else if(droneData.isPreparingToAttack()) {
    		System.out.println("Attack prep prot");
			attackPreparationProtocol();
    	} else if(droneData.getEnemyHqLocation() != null) {
    		System.out.println("Sensing flooding");
    		senseAdjacentFlooding();    
    		
    		if(!rc.isCurrentlyHoldingUnit()) {
    			System.out.println("No unit held");
    			int distanceSquaredFromEnemyHq = rc.getLocation().distanceSquaredTo(droneData.getEnemyHqLocation());
        		
    			if(distanceSquaredFromEnemyHq > 25 && approachNearbyCows()) {
    				//Distance check is to prevent routing into the enemy HQ net gun's range.
        			liftAdjacentCow();
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
        		droneData.setPreparingToAttack(true);
        	}
    		
    	} else {
    		System.out.println("Needs to scout for enemy HQ");
    		
    		
    		if(droneData.getHoldingEnemy()) {
    			System.out.println("Going to drown enemy");
    			drownEnemyProtocol();
    		} else {
    			System.out.println("No business with enemies -- can perform recon");
    			reconDroneProtocol();
    		}
    	}
	}
	
	private void learnHqLocation() throws GameActionException {
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
		
		boolean topEdge = hqLocation.y >= rc.getMapHeight() - 1;
		boolean bottomEdge = hqLocation.y <= 0;

		if(topEdge) {
			droneData.setGridOffset(0, -1);
		} else if(bottomEdge) {
			droneData.setGridOffset(0, 1);
		} else {
			droneData.setGridOffset(0, 0);
		}
	}
	
	private boolean checkPotentialThreats(RobotInfo[] drownableEnemies) {
		for(RobotInfo enemy : drownableEnemies) {
			if(enemy.getLocation().isWithinDistanceSquared(droneData.getHqLocation(), 25)) return true;
		}
		
		return false;
	}
	
	/**
	 * Once drone is holding enemy, will search for flooded area and drop it there.
	 */
	private void drownEnemyProtocol() throws GameActionException {
		System.out.println("Entered Drown Enemy Protocol");
		MapLocation rcLoation = rc.getLocation();
		
		for(Direction direction : Direction.allDirections()) {
			MapLocation potentialFloodLocation = rc.getLocation().add(direction);
			if(rc.canSenseLocation(potentialFloodLocation) && rc.senseFlooding(potentialFloodLocation)) {
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
				droneData.setCargoType(null);
			}

		} else {
			//TODO: Add a clause to protect them from net guns.
			continueSearchCautiously();
			senseAdjacentFlooding();
		}
	}
	
	private void drownCowProtocol() throws GameActionException {
		System.out.println("Entered Drown Cow Protocol");
		MapLocation rcLocation = rc.getLocation();
		
		for(Direction direction : Direction.allDirections()) {
			if(rc.canSenseLocation(rcLocation.add(direction)) && rc.senseFlooding(rcLocation.add(direction))) {
				if(dropUnit(direction)) {
					droneData.setHoldingCow(false);
					droneData.setCargoType(null);
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
				droneData.setCargoType(null);
			}
		} else {
			//TODO: Add a clause to protect them from net guns.
			continueSearchCautiously();
			senseAdjacentFlooding();
		}
	}
	
	public void continueSearchCautiously() throws GameActionException {
		//The move function is deliberately unused here.
		waitUntilReady();
		
		RobotInfo enemyHq = this.senseUnitType(RobotType.HQ, rc.getTeam().opponent());
		RobotInfo[] netGuns = this.senseAllUnitsOfType(RobotType.NET_GUN, rc.getTeam().opponent());
		MapLocation nextLocation = rc.getLocation().add(data.getSearchDirection());
		
		boolean inNetGunRange = enemyHq != null && enemyHq.getLocation().isWithinDistanceSquared(nextLocation, RobotType.NET_GUN.sensorRadiusSquared);
		
		if(!inNetGunRange) {
			for(RobotInfo netGun : netGuns) {
				if(netGun.getLocation().isWithinDistanceSquared(nextLocation, RobotType.NET_GUN.sensorRadiusSquared)) {
					inNetGunRange = true;
					break;
				}
			}
		}
		
		if(rc.canMove(data.getSearchDirection()) && !rc.senseFlooding(nextLocation) && !inNetGunRange) {
			rc.move(data.getSearchDirection());
			return;
		}

		data.setSearchDirection(directions[(int) (Math.random() * directions.length)]);
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
			
			if(!rc.getLocation().isWithinDistanceSquared(droneData.getEnemyHqLocation(), 8)) {
				System.out.println("Not yet near HQ!");
				if(rc.isCurrentlyHoldingUnit()) {
					if(droneData.getHoldingEnemy() || droneData.getHoldingCow()) {
						System.out.println("Holding hostile force!");
						//Drop enemies and cows in the ocean.
						Direction adjacentFloodingDirection = getAdjacentFloodingDirection();
						if(adjacentFloodingDirection != null) {
							System.out.println("Can drown it to the " + adjacentFloodingDirection);
							dropUnit(adjacentFloodingDirection);
							return;
						}
					}
				} else if(liftAdjacentEnemy()) {
					return;
				}
				
				bfsRouteTo(droneData.getEnemyHqLocation());
			} else {
				System.out.println("Near HQ!");
				if(rc.isCurrentlyHoldingUnit()) {
					if(droneData.getHoldingEnemy() || droneData.getHoldingCow()) {
						//Drop enemies and cows in the ocean.
						Direction adjacentFloodingDirection = getAdjacentFloodingDirection();
						if(adjacentFloodingDirection != null) {
							System.out.println("Can drown it to the " + adjacentFloodingDirection);
							dropUnit(adjacentFloodingDirection);
							return;
						}
					} else {
						dropUnitNextToEnemyHq();
						return;
					}
				} else {
					if(liftAdjacentEnemy()) return;
					
					//We failed to pick up an enemy if we got here, so we need to continue trying to get in close.
					bfsRouteTo(droneData.getEnemyHqLocation());
				}
			}
		} else {
    		//APPROACH ENEMY HQ
			
			//Every other drone needs to lift a landscaper for the attack.
			MapLocation waitingLocation = droneData.getAttackWaitLocation();
			boolean landscaperAttacker = Math.abs(waitingLocation.y - rc.getLocation().y) == 1 || Math.abs(waitingLocation.x - rc.getLocation().x) == 1;
			
			//Let those not holding units go first to clear spaces.
			if(landscaperAttacker && rc.getRoundNum() - droneData.getKillOrderReceptionRound() < CARGO_DRONE_ATTACK_DELAY) {
				if(!rc.isCurrentlyHoldingUnit()) pickUpUnit(RobotType.LANDSCAPER, rc.getTeam());
			} else if(rc.getLocation().distanceSquaredTo(droneData.getEnemyHqLocation()) > 25) bfsRouteTo(droneData.getEnemyHqLocation());
		}
	}
	
	private void idleHitManDroneProtocol() throws GameActionException {
		if(rc.isCurrentlyHoldingUnit()) {
			if(!droneData.getHoldingEnemy() && !droneData.getHoldingCow() && droneData.getCargoType() == RobotType.LANDSCAPER) {
				//Every drone should replace their landscaper onto the wall until it is time to attack.
				dropUnit(rc.getLocation().directionTo(droneData.getHqLocation()));
			} else if(droneData.getHoldingEnemy() || droneData.getHoldingCow()) {
				Direction adjacentFloodingDirection = getAdjacentFloodingDirection();
				if(adjacentFloodingDirection != null) {
					dropUnit(adjacentFloodingDirection);
				}
			}
		} else {
			RobotInfo[] drownableEnemies = getDrownableEnemies();
			for(RobotInfo enemy : drownableEnemies) {
				if(rc.getLocation().isWithinDistanceSquared(enemy.getLocation(), 3)) {
					if(pickUpUnit(enemy)) {
						droneData.setHoldingEnemy(true);
						break;
					}
				}
			}
		}
		
		if(readKillOrder()) {
			droneData.setReceivedKillOrder(true);
			droneData.setKillOrderReceptionRound(rc.getRoundNum());
		}
	}
	
	private boolean approachNearbyEnemies() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		if(enemies.length == 0) return false;
		
		System.out.println("ENEMY SPOTTED! KILL IT!");
		
		int[] enemySquaredDistances = new int[enemies.length];
		for(int i = 0; i < enemies.length; i++) {
			enemySquaredDistances[i] = enemies[i].getType().canBePickedUp() ? rc.getLocation().distanceSquaredTo(enemies[i].getLocation()) : Integer.MAX_VALUE;
		}
		
		RobotInfo targetEnemy = enemies[NumberMath.indexOfLeast(enemySquaredDistances)];
		
		if(!targetEnemy.getType().canBePickedUp()) return false;
		if(targetEnemy.getLocation().isWithinDistanceSquared(rc.getLocation(), 3)) return true;
		return bfsRouteTo(targetEnemy.getLocation());
	}
	
	private boolean liftAdjacentEnemy() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, droneData.getOpponent());
		
		for(RobotInfo enemy : enemies) {
			if(pickUpUnit(enemy)) {
				droneData.setHoldingEnemy(true);
				droneData.setCargoType(enemy.getType());
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
	
	private boolean liftAdjacentCow() throws GameActionException {
		if(!rc.isCurrentlyHoldingUnit()) {
			RobotInfo[] cows = this.senseAllUnitsOfType(RobotType.COW, Team.NEUTRAL);
			for(RobotInfo cow : cows) {
				if(pickUpUnit(cow)) {
					droneData.setHoldingCow(true);
					droneData.setCargoType(cow.getType());
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
				
				MapLocation rcLocation = rc.getLocation();
				MapLocation hqLocation = droneData.getHqLocation();
				Direction toHqDirection = rcLocation.directionTo(hqLocation);
				MapLocation potentialWallLocation = rcLocation.add(toHqDirection);
				
				System.out.println("Cargo type = " + droneData.getCargoType());
				if(!findVacanciesOnWall() && isWithinWall(rcLocation, hqLocation)) {
					if(rc.isCurrentlyHoldingUnit() && !droneData.getHoldingEnemy() && !droneData.getHoldingCow()) dropUnitAnywhereSafe();
					else pickUpWallUnit();
				} else if(rc.isCurrentlyHoldingUnit() && isOnWall(potentialWallLocation, hqLocation) && droneData.getCargoType() == RobotType.LANDSCAPER) {
					//Place landscapers back onto the wall until it is time to attack.
					dropUnit(toHqDirection);
				}
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
		boolean oughtPickUpLandscaper = oughtPickUpLandscaper();
		boolean oughtPickUpMiner = oughtPickUpMiner();
		
		System.out.println("No unit held! Lift Landscaper? " + oughtPickUpLandscaper);
		
		if(senseUnitType(RobotType.COW) != null) {
			System.out.println("Sensed a cow that ought be lifted.");
			if(!pickUpUnit(RobotType.COW)) {
				routeTo(droneData.getHqLocation());
			} else {
				droneData.setHoldingCow(true);
			}
		} else if(oughtPickUpMiner) {
			RobotInfo idleAttackMiner = senseAttackMiner();
			
			if(!rc.canSenseLocation(droneData.getHqLocation())) {
				routeTo(droneData.getHqLocation());
			} else if(idleAttackMiner != null) {
				System.out.println("\tFound an idle attack miner");
				if(!pickUpUnit(idleAttackMiner)) {
    				routeTo(idleAttackMiner.getLocation());
    			}
			} else {
				System.out.println("Routing to waiting point");
				routeToWaitingPoint();
			}
		} else if(oughtPickUpLandscaper) {
			System.out.println("Ought lift a landscaper rather than a cow.");
			RobotInfo idleAttackLandscaper = senseAttackLandscaper();
			
			if(!rc.canSenseLocation(droneData.getHqLocation())) {
				routeTo(droneData.getHqLocation());
			} else if(isWithinWall(rc.getLocation(), droneData.getHqLocation()) && !findVacanciesOnWall()) {
				RobotInfo[] adjacentLandscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
				for(RobotInfo adjacentLandscaper : adjacentLandscapers) {					
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
				routeToWaitingPoint();
			}
		} else if(!rc.canSenseLocation(droneData.getFulfillmentCenterBuildSite())){
			System.out.println("Ought not lift a landscaper, so routing to waiting point");
			routeToWaitingPoint();
		} else if(isOnWall(rc.getLocation(), droneData.getHqLocation())) {
			System.out.println("Drone on the wall needs to move");
			routeTo(droneData.getFulfillmentCenterBuildSite().add(droneData.getFulfillmentCenterBuildSite().directionTo(rc.getLocation())));
		}
	}
	
	private boolean oughtPickUpLandscaper() {		
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from dropping of a landscaper, then immediately detecting it and picking it up again.
		//Also, don't pick up landscapers until there is a surplus so our wall doesn't stop rising.
		
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(data.getSpawnerLocation()) < rcLocation.distanceSquaredTo(droneData.getEnemyHqLocation())
				&& rc.senseNearbyRobots(droneData.getHqLocation(), 3, rc.getTeam()) != null;
	}
	
	private boolean oughtPickUpMiner() throws GameActionException {		
		//Pick up the unit if we are closer to our own base than our opponent's.
		//This check is just to prevent the drone from dropping of a miner, then immediately detecting it and picking it up again.
		//Also, only the first delivery drone
		
		MapLocation rcLocation = rc.getLocation();
		return rcLocation.distanceSquaredTo(data.getSpawnerLocation()) < rcLocation.distanceSquaredTo(droneData.getEnemyHqLocation())
				&& senseAttackMiner() != null && this.senseUnitType(RobotType.VAPORATOR, rc.getTeam()) != null;
	}
	
	private RobotInfo senseAttackMiner() throws GameActionException {
		MapLocation hqLocation = droneData.getHqLocation();
		
		RobotInfo[] miners = senseAllUnitsOfType(RobotType.MINER, rc.getTeam());
		for(RobotInfo miner : miners) {
			if(isWithinWall(miner.getLocation(), hqLocation)) return miner;
		}

		return null;
	}
	
	private RobotInfo senseAttackLandscaper() throws GameActionException {
		MapLocation hqLocation = droneData.getHqLocation();
		RobotInfo[] landscapers = senseAllUnitsOfType(RobotType.LANDSCAPER, rc.getTeam());
		for(RobotInfo landscaper : landscapers) {
			if(isWithinWall(landscaper.getLocation(), hqLocation)) return landscaper;
		}
		
		return null;
	}
	
	private void routeToWaitingPoint() throws GameActionException {
		MapLocation spawnPoint = droneData.getSpawnLocation();
		
		//If we're at our target point -- the spawn point -- stop.
		if(rc.getLocation().equals(spawnPoint)) return;
		
		//If we can see the spawn point, we can ensure that it's not occupied.
		if(rc.canSenseLocation(spawnPoint)) {
			MapLocation hqLocation = droneData.getHqLocation();
			//If the spawn point is occupied, stay outside of the wall.
			if(rc.isLocationOccupied(spawnPoint)) {
				if(isOnWall(rc.getLocation(), hqLocation) || isWithinWall(rc.getLocation(), hqLocation)) {
					routeTo(rc.getLocation().add(hqLocation.directionTo(rc.getLocation())));
				}
			} else {
				routeTo(spawnPoint);
			}
		} else {
			routeTo(spawnPoint);
		}
	}
	
	private void defenseDroneProtocol(RobotInfo[] targets) throws GameActionException {
		if(approachNearbyEnemies()) {
			System.out.println("Successful enemy approach! Attempting lift...");
			liftAdjacentEnemy();
		}
	}
	
	private boolean liftNearbyEnemies() throws GameActionException {
		boolean landscaperNearby = senseUnitType(RobotType.LANDSCAPER,data.getOpponent())!=null;
		boolean hqNearby = senseUnitType(RobotType.HQ,data.getTeam())!=null;
		boolean enemyHqNearby = senseUnitType(RobotType.HQ,data.getOpponent())!=null;
		
		if(landscaperNearby && (hqNearby || enemyHqNearby)) {
			if(!pickUpUnit(RobotType.LANDSCAPER, data.getOpponent())) {
				if(hqNearby)
					routeTo(droneData.getHqLocation());
				else
					routeTo(droneData.getEnemyHqLocation());
				return false;
			} else {
				droneData.setHoldingEnemy(true);
				if(hqNearby)
					droneData.setEnemyFrom(data.getTeam());
				else
					droneData.setEnemyFrom(data.getOpponent());
				return true;
			}
		}
		
		return false;
	}
	
	private RobotInfo[] getDrownableEnemies() {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
		List<RobotInfo> drownableEnemies = new ArrayList<>();
		
		for(RobotInfo enemy : enemies) {
			if(enemy.type.canBePickedUp()) drownableEnemies.add(enemy);
		}
		
		return drownableEnemies.toArray(new RobotInfo[0]);
	}
	
	private void reconDroneProtocol() throws GameActionException {
		if(!droneData.searchDestinationsDetermined()) {
			droneData.calculateSearchDestinations(rc);
		}
		
		attemptEnemyHQDetection();
		
		if(droneData.getEnemyHqLocation() != null) {
			sendTransaction(10, Robot.Type.TRANSACTION_ENEMY_HQ_AT_LOC, droneData.getEnemyHqLocation());
			System.out.println("Detected the HQ and sent a transaction!");
		} else {
			cautiouslyApproachHqLocation();
		}
	}
	
	private void cautiouslyApproachHqLocation() throws GameActionException {
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
				droneData.setCargoType(info.getType());
				return true;
			}
		}
		
		return false;
	}
	
	private boolean pickUpUnit(RobotType targetType) throws GameActionException {
		return pickUpUnit(targetType, null);
	}
	
	private boolean pickUpUnit(RobotType targetType, Team targetTeam) throws GameActionException {
		RobotInfo[] units = senseAllUnitsOfType(targetType, targetTeam);
		
		waitUntilReady();
		
		for(RobotInfo unit : units) {
			if(rc.canPickUpUnit(unit.ID)) {
				rc.pickUpUnit(unit.ID);
				droneData.setCargoType(targetType);
				return true;
			}
		}
		
		return false;
	}
	
	private void pickUpWallUnit() throws GameActionException {
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = droneData.getHqLocation();
		
		for(Direction direction : directions) {
			MapLocation targetLocation = rcLocation.add(direction);
			if(rc.canSenseLocation(targetLocation) && isOnWall(targetLocation, hqLocation)) {
				RobotInfo robotOnWall = rc.senseRobotAtLocation(targetLocation);
				if(robotOnWall != null) {
					pickUpUnit(robotOnWall);
					return;
				}
			}
		}
	}
	
	private boolean dropUnit(Direction dropDirection) throws GameActionException {
		waitUntilReady();
		
		if(rc.canDropUnit(dropDirection)) {
			rc.dropUnit(dropDirection);
			droneData.setHoldingEnemy(false);
			droneData.setHoldingCow(false);
			droneData.setCargoType(null);
			return true;
		}
		
		return false;
	}
	
	private boolean dropUnitAnywhereSafe() throws GameActionException {
		for(Direction direction : directions) {
			if(rc.canSenseLocation(rc.getLocation().add(direction)) && !rc.senseFlooding(rc.getLocation().add(direction))) {
				if(dropUnit(direction)) return true;
			}
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
		for(Direction direction : Robot.directions) {
			MapLocation adjacentLocation = rc.adjacentLocation(direction);
			if(rc.canSenseLocation(adjacentLocation) && rc.senseFlooding(adjacentLocation) && !rc.isLocationOccupied(adjacentLocation)) {
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
		MapLocation rcLocation = rc.getLocation();
		MapLocation hqLocation = droneData.getHqLocation();
    	
    	int minDx = droneData.getWallOffsetXMin();
    	int maxDx = droneData.getWallOffsetXMax();
    	int minDy = droneData.getWallOffsetYMin();
    	int maxDy = droneData.getWallOffsetYMax();
    	
    	for(int dx = minDx; dx <= maxDx; dx++) {
    		for(int dy = minDy; dy <= maxDy; dy++) {
    			MapLocation wallLocation = hqLocation.translate(dx, dy);
    			if(rc.canSenseLocation(wallLocation) && wallLocation.isWithinDistanceSquared(rcLocation, 3) && isOnWall(wallLocation, hqLocation)) {
    				if(!rc.isLocationOccupied(wallLocation)) {
    					return true;
    				}
    			}
    		}
    	}
    	
    	return false;
	}
	
}

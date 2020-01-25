package julianbot.robotdata;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import julianbot.robots.Robot;
import julianbot.utils.pathfinder.MapGraph;

public class RobotData {

	//GENERAL
	protected final Team team;
	protected final Team opponent;
	protected MapLocation spawnLocation;
	protected MapLocation spawnerLocation;
	
	//ROUTING
	private MapLocation previousLocation;
	protected boolean bugNaving;
	protected MapGraph mapGraph;
		protected Direction[] path;
		protected int pathProgression;
	
	//TRANSACTIONS
	private boolean hasPendingTransaction;
		private Robot.Type pendingTransactionType;
		private MapLocation pendingTransactionLocation;
		private int pendingTransactionSoupBid;
	private Direction searchDirection;

	//BugNav Data
	private int bugNavClosestDist;
	private MapLocation obstacleLoc;
	private MapLocation currentDestination;
	
	//WALL DIMENSIONS
	private int wallOffsetXMin, wallOffsetXMax, wallOffsetYMin, wallOffsetYMax;
	private boolean baseOnEdge;
	
	//BUILDING
	private MapLocation designSchoolBuildSite;
	private MapLocation fulfillmentCenterBuildSite;
	private MapLocation vaporatorBuildMinerLocation;
		private MapLocation vaporatorBuildSite;
	private MapLocation netGunBuildSite;
	
	public RobotData(RobotController rc, MapLocation spawnerLocation) {
		team = rc.getTeam();
		opponent = team.opponent();
		spawnLocation = rc.getLocation();
		setSpawnerLocation(spawnerLocation);
		previousLocation = rc.getLocation();
		currentDestination = rc.getLocation();
		hasPendingTransaction = false;
		bugNavClosestDist = -1;
		setSpawnerLocation(spawnerLocation);

		if (!rc.getType().isBuilding()) {
			if(spawnerLocation == null || rc.getLocation() == null) {
				System.out.println("RobotData constructor on round " + rc.getRoundNum() + ":");
				System.out.println("Spawner Location = " + spawnerLocation + ", Self Location = " + rc.getLocation());
				searchDirection = Direction.NORTH;
			} else {
				searchDirection = spawnerLocation.directionTo(rc.getLocation());
			}
		}
	}
	
	public void initializeWallData(MapLocation hqLocation, int mapWidth, int mapHeight) {		
		boolean leftEdge = hqLocation.x <= 0;
		boolean rightEdge = hqLocation.x >= mapWidth - 1;
		boolean topEdge = hqLocation.y >= mapHeight - 1;
		boolean bottomEdge = hqLocation.y <= 0;
		setBaseOnEdge(leftEdge || rightEdge || topEdge || bottomEdge);
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) {
				//Lucky us, the HQ is also next to the southern wall.
				setDesignSchoolBuildSite(hqLocation.translate(0, 2));
				setFulfillmentCenterBuildSite(hqLocation.translate(1, 0));
				setVaporatorBuildMinerLocation(hqLocation.translate(1, 1));
				setVaporatorBuildSite(hqLocation.translate(0, 1));
				setWallOffsetBounds(0, 2, 0, 3);
			} else if(topEdge) {
				//Lucky us, the HQ is also next to the northern wall.
				setDesignSchoolBuildSite(hqLocation.translate(0, -2));
				setFulfillmentCenterBuildSite(hqLocation.translate(1, 0));
				setVaporatorBuildMinerLocation(hqLocation.translate(1, -1));
				setVaporatorBuildSite(hqLocation.translate(0, -1));
				setWallOffsetBounds(0, 2, -3, 0);
			} else {
				//The HQ is next to the western wall, but not cornered.
				setDesignSchoolBuildSite(hqLocation.translate(0, 2));
				setFulfillmentCenterBuildSite(hqLocation.translate(1, 0));
				setVaporatorBuildMinerLocation(hqLocation.translate(1, 1));
				setVaporatorBuildSite(hqLocation.translate(0, 1));
				setWallOffsetBounds(0, 2, -1, 3);
			}
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) {
				//Lucky us, the HQ is also next to the southern wall.
				setDesignSchoolBuildSite(hqLocation.translate(0, 2));
				setFulfillmentCenterBuildSite(hqLocation.translate(-1, 0));
				setVaporatorBuildMinerLocation(hqLocation.translate(-1, 1));
				setVaporatorBuildSite(hqLocation.translate(0, 1));
				setWallOffsetBounds(-2, 0, 0, 3);
			} else if(topEdge) {
				//Lucky us, the HQ is also next to the northern wall.
				setDesignSchoolBuildSite(hqLocation.translate(0, -2));
				setFulfillmentCenterBuildSite(hqLocation.translate(-1, 0));
				setVaporatorBuildMinerLocation(hqLocation.translate(-1, -1));
				setVaporatorBuildSite(hqLocation.translate(0, -1));
				setWallOffsetBounds(-2, 0, -3, 0);
			} else {
				setDesignSchoolBuildSite(hqLocation.translate(0, -2));
				setFulfillmentCenterBuildSite(hqLocation.translate(-1, 0));
				setVaporatorBuildMinerLocation(hqLocation.translate(-1, -1));
				setVaporatorBuildSite(hqLocation.translate(0, -1));
				setWallOffsetBounds(-2, 0, -3, 1);
			}
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			setDesignSchoolBuildSite(hqLocation.translate(2, 0));
			setFulfillmentCenterBuildSite(hqLocation.translate(0, -1));
			setVaporatorBuildMinerLocation(hqLocation.translate(1, -1));
			setVaporatorBuildSite(hqLocation.translate(1, 0));
			setWallOffsetBounds(-1, 3, 0, -2);
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			setDesignSchoolBuildSite(hqLocation.translate(-2, 0));
			setFulfillmentCenterBuildSite(hqLocation.translate(0, 1));
			setVaporatorBuildMinerLocation(hqLocation.translate(-1, 1));
			setVaporatorBuildSite(hqLocation.translate(-1, 0));
			setWallOffsetBounds(-3, 1, 0, 2);
		} else {
			setDesignSchoolBuildSite(hqLocation.translate(-1, 0));
			setFulfillmentCenterBuildSite(hqLocation.translate(1, 0));
			setVaporatorBuildMinerLocation(hqLocation.translate(0, -1));
			setVaporatorBuildSite(hqLocation.translate(1, -1));
			setNetGunBuildSite(hqLocation.translate(-1, -1));
			setWallOffsetBounds(-2, 2, -2, 2);
		}
		
		if(leftEdge) {
			//The HQ is next to the western wall.
			if(bottomEdge) setWallOffsetBounds(0, 2, 0, 3);
			else if(topEdge) setWallOffsetBounds(0, 2, -3, 0);
			else setWallOffsetBounds(0, 2, -1, 3);
		} else if(rightEdge) {
			//The HQ is next to the eastern wall.
			if(bottomEdge) setWallOffsetBounds(-2, 0, 0, 3);
			else if(topEdge) setWallOffsetBounds(-2, 0, -3, 0);
			else setWallOffsetBounds(-2, 0, -3, 1);
		} else if(topEdge) {
			//The HQ is next to the northern wall, but not cornered.
			setWallOffsetBounds(-1, 3, 0, -2);
		} else if(bottomEdge) {
			//The HQ is next to the southern wall, but not cornered.
			setWallOffsetBounds(-3, 1, 0, 2);
		} else {
			setWallOffsetBounds(-2, 2, -2, 2);
		}
	}
	
	public Team getTeam() {
		return team;
	}
	
	public Team getOpponent() {
		return opponent;
	}
	
	public MapLocation getSpawnLocation() {
		return spawnLocation;
	}

	public void setSpawnLocation(MapLocation spawnLocation) {
		this.spawnLocation = spawnLocation;
	}

	public int getClosestDist() {
		return bugNavClosestDist;
	}

	public void setClosestDist(int bugNavClosestDist) {
		this.bugNavClosestDist = bugNavClosestDist;
	}

	public MapLocation getObstacleLoc() {return obstacleLoc;}

	public void setObstacleLoc(MapLocation loc) {obstacleLoc = loc;}

	public MapLocation getSpawnerLocation() {
		return spawnerLocation;
	}

	public void setSpawnerLocation(MapLocation spawnerLocation) {
		this.spawnerLocation = spawnerLocation;
	}

	public Direction getSearchDirection() {
		return searchDirection;
	}

	public void setSearchDirection(Direction searchDirection) {
		this.searchDirection = searchDirection;
	}

	public MapLocation getPreviousLocation() {
		return previousLocation;
	}

	public void setPreviousLocation(MapLocation previousLocation) {
		this.previousLocation = previousLocation;
	}
	
	public boolean isBugNaving() {
		return bugNaving;
	}
	
	public void setBugNaving(boolean bugNaving) {
		this.bugNaving = bugNaving;
	}

	public MapGraph getMapGraph() {
		return mapGraph;
	}

	public void setMapGraph(MapGraph mapGraph) {
		this.mapGraph = mapGraph;
	}

	public Direction[] getPath() {
		return path;
	}
	
	public boolean hasPath() {
		return path != null && path.length > 0;
	}

	public void setPath(Direction[] path) {
		this.path = path;
	}
	
	public Direction getNextPathDirection() {
		return path[pathProgression];
	}

	public int getPathProgression() {
		return pathProgression;
	}

	public void incrementPathProgression() {
		pathProgression++;
	}
	
	public void setPathProgression(int pathProgression) {
		this.pathProgression = pathProgression;
	}
	
	public boolean pathCompleted() {
		return pathProgression >= path.length;
	}
	
	public boolean hasPendingTransaction() {
		return hasPendingTransaction;
	}
	
	public void setPendingTransaction(Robot.Type transactionType, MapLocation location, int soupBid) {
		this.hasPendingTransaction = true;
		this.pendingTransactionType = transactionType;
		this.pendingTransactionLocation = location;
		this.pendingTransactionSoupBid = soupBid;
	}
	
	public void clearPendingTransaction() {
		this.hasPendingTransaction = false;
	}

	public Robot.Type getPendingTransactionType() {
		return pendingTransactionType;
	}

	public void setPendingTransactionType(Robot.Type pendingTransactionType) {
		this.pendingTransactionType = pendingTransactionType;
	}

	public MapLocation getPendingTransactionLocation() {
		return pendingTransactionLocation;
	}

	public void setPendingTransactionLocation(MapLocation pendingTransactionLocation) {
		this.pendingTransactionLocation = pendingTransactionLocation;
	}

	public int getPendingTransactionSoupBid() {
		return pendingTransactionSoupBid;
	}

	public void setPendingTransactionSoupBid(int pendingTransactionSoupBid) {
		this.pendingTransactionSoupBid = pendingTransactionSoupBid;
	}

	public MapLocation getCurrentDestination() {
		return currentDestination;
	}

	public void setCurrentDestination(MapLocation currentDestination) {
		this.currentDestination = currentDestination;
	}
	
	public boolean isBaseOnEdge() {
		return baseOnEdge;
	}

	public void setBaseOnEdge(boolean baseOnEdge) {
		this.baseOnEdge = baseOnEdge;
	}
	
	public int getWallOffsetXMin() {
		return wallOffsetXMin;
	}

	public int getWallOffsetXMax() {
		return wallOffsetXMax;
	}

	public int getWallOffsetYMin() {
		return wallOffsetYMin;
	}

	public int getWallOffsetYMax() {
		return wallOffsetYMax;
	}
	
	public void setWallOffsetBounds(int wallOffsetXMin, int wallOffsetXMax, int wallOffsetYMin, int wallOffsetYMax) {
		this.wallOffsetXMin = wallOffsetXMin;
		this.wallOffsetXMax = wallOffsetXMax;
		this.wallOffsetYMin = wallOffsetYMin;
		this.wallOffsetYMax = wallOffsetYMax;
	}

	public MapLocation getDesignSchoolBuildSite() {
		return designSchoolBuildSite;
	}

	public void setDesignSchoolBuildSite(MapLocation designSchoolBuildSite) {
		this.designSchoolBuildSite = designSchoolBuildSite;
	}

	public MapLocation getFulfillmentCenterBuildSite() {
		return fulfillmentCenterBuildSite;
	}

	public void setFulfillmentCenterBuildSite(MapLocation fulfillmentCenterBuildSite) {
		this.fulfillmentCenterBuildSite = fulfillmentCenterBuildSite;
	}

	public MapLocation getVaporatorBuildMinerLocation() {
		return vaporatorBuildMinerLocation;
	}

	public void setVaporatorBuildMinerLocation(MapLocation vaporatorBuildMinerLocation) {
		this.vaporatorBuildMinerLocation = vaporatorBuildMinerLocation;
	}

	public MapLocation getVaporatorBuildSite() {
		return vaporatorBuildSite;
	}

	public void setVaporatorBuildSite(MapLocation vaporatorBuildSite) {
		this.vaporatorBuildSite = vaporatorBuildSite;
	}

	public MapLocation getNetGunBuildSite() {
		return netGunBuildSite;
	}

	public void setNetGunBuildSite(MapLocation netGunBuildSite) {
		this.netGunBuildSite = netGunBuildSite;
	}
	
}

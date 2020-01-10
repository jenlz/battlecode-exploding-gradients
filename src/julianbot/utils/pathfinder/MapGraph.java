package julianbot.utils.pathfinder;

import java.util.Iterator;
import java.util.LinkedList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class MapGraph {

	private static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	private int lengthToEdge;
	private int radiusSquared;
	private int dimension;
	
	private int nodeCount;
	private LinkedList<Integer> adj[];
	private int[] parentCodes;
	
	private MapLocation sourceLocation;
	
	public MapGraph(RobotController rc, int radiusSquared) {
		this.lengthToEdge = (int) Math.sqrt(radiusSquared);
		this.radiusSquared = radiusSquared;
		this.dimension = 1 + 2 * lengthToEdge;
		
		this.nodeCount = dimension * dimension;
		this.adj = new LinkedList[nodeCount];
		this.parentCodes = new int[nodeCount];
		
		this.sourceLocation = rc.getLocation();
		
		for(int i = 0; i < nodeCount; ++i) {
			this.adj[i] = new LinkedList<>();
		}
		
		for(int i = -lengthToEdge; i <= lengthToEdge; i++) {
			for(int j = -lengthToEdge; j <= lengthToEdge; j++) {
				if(i * i + j * j > radiusSquared) continue;				
				connectAllAdjacencies(rc, i, j);
			}
		}
	}
	
	private int getLocationCode(MapLocation testLocation) {
		int graphX = testLocation.x - sourceLocation.x + lengthToEdge;
		int graphY = testLocation.y - sourceLocation.y + lengthToEdge;
		return graphY * dimension + graphX;
	}
	
	private MapLocation getLocation(int locationCode) {
		int graphX = locationCode % dimension;
		int graphY = locationCode / dimension;
		return sourceLocation.translate(graphX - lengthToEdge, graphY - lengthToEdge);
	}
	
	private void connectAllAdjacencies(RobotController rc, int dx, int dy) {
		MapLocation location = sourceLocation.translate(dx, dy);
		int locationCode = getLocationCode(location);
		
		try {
			for(Direction direction : directions) {
				int totalDX = dx + direction.dx;
				int totalDY = dy + direction.dy;
				
				if(totalDX * totalDX + totalDY * totalDY > radiusSquared) continue;
				
				MapLocation adjacentLocation = location.add(direction);
				if(rc.canSenseLocation(adjacentLocation) && !rc.isLocationOccupied(adjacentLocation)) {
					addEdge(locationCode, getLocationCode(adjacentLocation));
				}
			}
		} catch(GameActionException e) {
			e.printStackTrace();
		}
	}
	
	private void addEdge(int v, int w) {
		adj[v].add(w);
	}
	
	public Direction[] BFS(MapLocation start, MapLocation mapDestination) {
		int source = getLocationCode(start);
		int destination = getLocationCode(mapDestination);
		
		boolean visited[] = new boolean[nodeCount];
		
		LinkedList<Integer> queue = new LinkedList<Integer>(); 
		  
        // Mark the current node as visited and enqueue it 
        visited[source]=true;
        queue.add(source);
  
        
        int node = source;
        boolean foundSolution = false;
        while (queue.size() != 0) 
        { 
            node = queue.poll(); 
            
            Iterator<Integer> i = adj[node].listIterator(); 
            while (i.hasNext()) 
            { 
                int n = i.next(); 
                if (!visited[n]) 
                { 
                    visited[n] = true; 
                    queue.add(n); 
                    parentCodes[n] = node;
                    if(n == destination) {
                    	foundSolution = true;
                    	break;
                    }
                } 
            }
        }
        
        if(!foundSolution) return new Direction[0];
        
        int depth = 1;
        int vertex = destination;
        while(vertex != source) {
        	vertex = parentCodes[vertex];
        	depth++;
        }
        
        Direction[] path = new Direction[depth - 1];
        vertex = destination;
        for(int i = path.length - 1; i >= 0; i--) {
        	MapLocation b = getLocation(vertex);
        	MapLocation a = getLocation((vertex = parentCodes[vertex]));
        	path[i] = a.directionTo(b);
        }
        
        return path;
	}
	
}

package julianbot.commands;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Transaction;
import julianbot.robotdata.LandscaperData;
import julianbot.utils.NumberMath;

public class LandscaperCommands {
	
	static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
	
	public static boolean dig(RobotController rc, Direction dir) throws GameActionException {
		if(rc.isReady() && rc.canDigDirt(dir)) {
			rc.digDirt(dir);
			return true;
		}
		
		return false;
	}
	
	public static boolean depositDirt(RobotController rc, Direction dir) throws GameActionException {
		if(rc.isReady() && rc.canDepositDirt(dir)) {
			rc.depositDirt(dir);
			return true;
		}
		
		return false;
	}
	
	public static void learnHQLocation(RobotController rc, LandscaperData data) throws GameActionException {
		for(Transaction transaction : rc.getBlock(1)) {
			int[] message = transaction.getMessage();
			if(message[0] == GeneralCommands.getTransactionTag(1)) {
				MapLocation landscaperLocation = rc.getLocation();
				MapLocation origin = landscaperLocation.translate(-landscaperLocation.x, -landscaperLocation.y);
				data.setHqLocation(origin.translate(message[1], message[2]));
			}
		}
	}
	
	public static void approachHQ(RobotController rc, LandscaperData data) throws GameActionException {
		GeneralCommands.move(rc, rc.getLocation().directionTo(data.getHqLocation()));
	}
	
	public static void determineApproachCompletion(RobotController rc, LandscaperData data) {
		if(Math.abs(rc.getLocation().x - data.getHqLocation().x) <= 1 && Math.abs(rc.getLocation().y - data.getHqLocation().y) <= 1) {
			data.setCurrentRole(LandscaperData.DEFEND_HQ_FROM_FLOOD);
		}
	}
	
	public static void buildHQWall(RobotController rc, LandscaperData data) throws GameActionException {
		if(rc.getDirtCarrying() > 0) constructWallUnits(rc, data);
		else digWallDirt(rc, data);
	}
	
	private static void constructWallUnits(RobotController rc, LandscaperData data) throws GameActionException {
		Direction constructDirection = data.getHqLocation().directionTo(rc.getLocation());
		int[] elevations = new int[]{
				rc.senseElevation(rc.getLocation().add(constructDirection.rotateLeft())),
				rc.senseElevation(rc.getLocation().add(constructDirection)),
				rc.senseElevation(rc.getLocation().add(constructDirection.rotateRight()))};
		
		int lowestElevation = NumberMath.indexOfLeast(elevations);
		if(lowestElevation == 0) rc.depositDirt(constructDirection.rotateLeft());
		if(lowestElevation == 1) rc.depositDirt(constructDirection);
		if(lowestElevation == 2) rc.depositDirt(constructDirection.rotateRight());
	}
	
	private static void digWallDirt(RobotController rc, LandscaperData data) throws GameActionException {
		Direction digDirection = Direction.CENTER;
		if(rc.getLocation().distanceSquaredTo(data.getHqLocation()) == 1) {
			//The Landscaper is not at a diagonal to the HQ.
			digDirection = data.getHqLocation().directionTo(rc.getLocation()).rotateLeft().rotateLeft();
		} else if(rc.getLocation().distanceSquaredTo(data.getHqLocation()) == 2) {
			//The Landscaper is at a diagonal to the HQ.
			digDirection = rc.getLocation().directionTo(data.getHqLocation()).rotateRight();
		} 
		
		int elevationDifference = rc.senseElevation(rc.getLocation()) - rc.senseElevation(rc.getLocation().add(digDirection));
		System.out.println("Relative to the HQ, I am " + data.getHqLocation().directionTo(rc.getLocation()));
		System.out.println("The relative elevation is " + elevationDifference);
		System.out.println("Going to attempt to dig in direction " + digDirection);
		
		if(elevationDifference < GameConstants.MAX_DIRT_DIFFERENCE) {
			System.out.println("Digging " + digDirection);
			LandscaperCommands.dig(rc, digDirection);
		} else {
			System.out.println("Moving " + digDirection);
			GeneralCommands.move(rc, digDirection);
		}
		
		System.out.println("=====");
	}
	
}

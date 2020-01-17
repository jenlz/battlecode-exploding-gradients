package julianbot;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import julianbot.robotdata.RobotData;
import julianbot.robots.DesignSchool;
import julianbot.robots.Drone;
import julianbot.robots.FulfillmentCenter;
import julianbot.robots.HQ;
import julianbot.robots.Landscaper;
import julianbot.robots.Miner;
import julianbot.robots.NetGun;
import julianbot.robots.Refinery;
import julianbot.robots.Robot;
import julianbot.robots.Vaporator;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static RobotData robotData;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.	
        System.out.println("Initializing Robot.");
    	RobotPlayer.rc = rc;
    	Robot robot = getRobot(rc.getType());
        
        while (true) {
            
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                robot.run();
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
    
    static Robot getRobot(RobotType type) {
    	switch(type) {
			case HQ:                 return new HQ(rc);
			case MINER:              return new Miner(rc);
			case REFINERY:           return new Refinery(rc);
			case DESIGN_SCHOOL:      return new DesignSchool(rc);
			case LANDSCAPER:         return new Landscaper(rc);
			case FULFILLMENT_CENTER: return new FulfillmentCenter(rc);
			case DELIVERY_DRONE:     return new Drone(rc);
			case VAPORATOR:          return new Vaporator(rc);
			case NET_GUN:            return new NetGun(rc);
			default:                 return new Robot(rc);
    	}
    }
    
}

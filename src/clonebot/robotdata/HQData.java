package clonebot.robotdata;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.Transaction;

public class HQData extends RobotData {

	private int minersBuilt;
	
	private Direction buildDirection;
	private List<Transaction> foreignTransactions;
	
	public HQData(RobotController rc) {
		super(rc);
		buildDirection = Direction.NORTH;
		foreignTransactions = new ArrayList<>();
	}

	public int getMinersBuilt() {
		return minersBuilt;
	}

	public void incrementMinersBuilt() {
		this.minersBuilt++;
	}
	
	public void setMinersBuilt(int minersBuilt) {
		this.minersBuilt = minersBuilt;
	}

	public Direction getBuildDirection() {
		return buildDirection;
	}

	public void setBuildDirection(Direction buildDirection) {
		this.buildDirection = buildDirection;
	}
	
	//TODO: Take note that this will likely get large, and could easily exceed the 8 MB limit. Further data management will be required.
	public void addForeignTransaction(Transaction transaction) {
		foreignTransactions.add(transaction);
	}
	
	public Transaction getRandomForeignTransaction() {
		if(foreignTransactions.size() == 0) return null;
		return foreignTransactions.get((int) (Math.random() * foreignTransactions.size()));
	}
	
}

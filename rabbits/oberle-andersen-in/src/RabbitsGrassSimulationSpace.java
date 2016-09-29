import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid agentSpace;

	public Object2DGrid getCurrentAgentSpace() {
		return agentSpace;
	}

	public RabbitsGrassSimulationSpace(int xSize, int ySize) {
		grassSpace = new Object2DGrid(xSize, ySize);
		agentSpace = new Object2DGrid(xSize, ySize);
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				grassSpace.putObjectAt(i, j, 0);
			}
		}
	}

	public void plantGrass(int amount) {
		// Randomly plant grass in the garden
		for (int i = 0; i < amount; i++) {

			// Choose coordinates
			int x = (int) (Math.random() * grassSpace.getSizeX());
			int y = (int) (Math.random() * grassSpace.getSizeY());

			// to simplify the project we ignore the case where the random grass
			// position comes to a cell that already contains grass. This could
			// lead to less grass being planted than the actual growth rate.
			grassSpace.putObjectAt(x, y, 1);
		}
	}

	public boolean hasGrass(int x, int y) {
		if (grassSpace.getObjectAt(x, y) != null) {
			int i = ((Integer) grassSpace.getObjectAt(x, y)).intValue();
			return i > 0 ? true : false;

		}
		return false;
	}

	public Object2DGrid getCurrentGrassSpace() {
		return grassSpace;
	}

	public boolean isCellOccupied(int x, int y) {
		return agentSpace.getObjectAt(x, y) != null ? true : false;
	}

	public boolean addAgent(RabbitsGrassSimulationAgent agent) {
		boolean retVal = false;
		int count = 0;
		int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

		while (retVal == false && count < countLimit) {
			int x = (int) (Math.random() * agentSpace.getSizeX());
			int y = (int) (Math.random() * agentSpace.getSizeY());
			if (isCellOccupied(x, y) == false) {
				agentSpace.putObjectAt(x, y, agent);
				agent.setXY(x, y);
				agent.setGarden(this);
				retVal = true;
			}
			count++;
		}

		return retVal;
	}

	public void removeAgentAt(int x, int y) {
		agentSpace.putObjectAt(x, y, null);
	}

	public boolean eatGrass(int x, int y) {
		boolean retVal = hasGrass(x, y);
		grassSpace.putObjectAt(x, y, 0);
		return retVal;
	}

	public boolean moveAgentAt(int x, int y, int newX, int newY) {
		boolean retVal = false;
		if (!isCellOccupied(newX, newY)) {
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x, y);
			removeAgentAt(x, y);
			rgsa.setXY(newX, newY);
			agentSpace.putObjectAt(newX, newY, rgsa);
			retVal = true;
		}
		return retVal;
	}

	public RabbitsGrassSimulationAgent getAgentAt(int x, int y) {
		RabbitsGrassSimulationAgent retVal = null;
		if (agentSpace.getObjectAt(x, y) != null) {
			retVal = (RabbitsGrassSimulationAgent) agentSpace.getObjectAt(x, y);
		}
		return retVal;
	}

	public int getAllGrass() {
		int grass = 0;
		for (int i = 0; i < agentSpace.getSizeX(); i++) {
			for (int j = 0; j < agentSpace.getSizeY(); j++) {
				grass += hasGrass(i, j) ? 1 : 0;
			}
		}
		return grass;
	}

}

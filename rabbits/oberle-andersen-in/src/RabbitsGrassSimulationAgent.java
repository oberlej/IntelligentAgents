import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	private int x;
	private int y;
	private int vX;
	private int vY;
	private int grass;
	private int stepsToLive;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace garden;

	public RabbitsGrassSimulationAgent(int minLifespan, int maxLifespan) {
		x = -1;
		y = -1;
		setVxVy();
		grass = 0;
		stepsToLive = (int) (Math.random() * (maxLifespan - minLifespan) + minLifespan);
		IDNumber++;
		ID = IDNumber;
	}

	private void setVxVy() {
		vX = 0;
		vY = 0;
		while (vX == 0 && vY == 0) {
			vX = (int) Math.floor(Math.random() * 3) - 1;
			vY = (int) Math.floor(Math.random() * 3) - 1;
		}
	}

	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}

	public String getID() {
		return "A-" + ID;
	}

	public int getGrass() {
		return grass;
	}

	public int getStepsToLive() {
		return stepsToLive;
	}

	public void report() {
		System.out.println(getID() + " at " + x + ", " + y + " has " + getGrass() + " dollars" + " and "
		        + getStepsToLive() + " steps to live.");
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public void draw(SimGraphics G) {
		if (stepsToLive > 10) {
			G.drawFastRoundRect(Color.green);
		} else {
			G.drawFastRoundRect(Color.blue);
		}
	}

	public void step() {
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = garden.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if (tryMove(newX, newY)) {
			grass += garden.eatGrass(x, y);
		} else {
			RabbitsGrassSimulationAgent rgsa = garden.getAgentAt(newX, newY);
			if (rgsa != null) {
				if (grass > 0) {
					rgsa.receiveGrass(1);
					grass--;
				}
			}
			setVxVy();
		}
		stepsToLive--;
	}

	public void receiveGrass(int amount) {
		grass += amount;
	}

	private boolean tryMove(int newX, int newY) {
		return garden.moveAgentAt(x, y, newX, newY);
	}

	/**
	 * @return the garden
	 */
	public RabbitsGrassSimulationSpace getGarden() {
		return garden;
	}

	/**
	 * @param garden
	 *            the garden to set
	 */
	public void setGarden(RabbitsGrassSimulationSpace garden) {
		this.garden = garden;
	}
}

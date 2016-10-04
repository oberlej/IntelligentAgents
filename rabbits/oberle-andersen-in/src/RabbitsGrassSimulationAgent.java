import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

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
	private int energy;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace garden;
	private final BufferedImage img = null;

	public RabbitsGrassSimulationAgent(int initEnergy) {
		x = -1;
		y = -1;
		setVxVy();
		energy = initEnergy;
		IDNumber++;
		ID = IDNumber;

		if (img == null) {
			try {
				ImageIO.read(new File("rabbit.png"));
			} catch (IOException e) {
				System.out.println("Cannot find Agent image!");
			}
		}
	}

	private void setVxVy() {
		vX = 0;
		vY = 0;
		while (vX == 0 && vY == 0 || vX != 0 && vY != 0) {
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

	public int getEnergy() {
		return energy;
	}

	public void report() {
		System.out.println(getID() + " at " + x + ", " + y + " has " + getEnergy() + " energy.");
	}

	@Override
	public int getX() {
		return x;
	}

	public void addEnergy(int energy) {
		this.energy += energy;
		if (this.energy < 0) {
			this.energy = 0;
		}
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public void draw(SimGraphics G) {
		// G.drawImage(img);
		G.drawCircle(Color.RED);
	}

	public boolean step() {
		setVxVy();
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = garden.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		if (tryMove(newX, newY)) {
			if (garden.eatGrass(x, y)) {
				return true;
			}
		}
		return false;
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

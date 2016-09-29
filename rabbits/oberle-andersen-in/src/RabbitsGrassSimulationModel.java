import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass simulation.
 * This is the first class which needs to be setup in order to run Repast
 * simulation. It manages the entire RePast environment and the simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {

	private Schedule schedule;
	private RabbitsGrassSimulationSpace garden;
	private DisplaySurface displaySurf;
	private ArrayList agentList;

	private OpenSequenceGraph plot;

	// Default Values
	private static final int NUM_AGENTS = 400;
	private static final int WORLD_XSIZE = 40;
	private static final int WORLD_YSIZE = 40;
	private static final int GROWTH_RATE = 30;
	private static final int BIRTH_TRESHOLD = 100;
	private static final int INIT_ENERGY = 60;
	private static final int INIT_GRASS = 10;
	private static final int GRASS_INDEX = 1;
	private static final int EMPTY_INDEX = 0;
	private final static int ENERGY_LOSS = 5;
	private final static int ENERGY_GAIN = 20;

	private int numAgents = NUM_AGENTS;
	private int worldXSize = WORLD_XSIZE;
	private int worldYSize = WORLD_YSIZE;
	private int growthRate = GROWTH_RATE;
	private int birthThreshold = BIRTH_TRESHOLD;
	private int initEnergy = INIT_ENERGY;
	private int initGrass = INIT_GRASS;
	private int energyLoss = ENERGY_LOSS;
	private int energyGain = ENERGY_GAIN;

	class grassSequence implements Sequence {

		@Override
		public double getSValue() {
			System.out.println("nb grass: " + garden.getAllGrass());
			return garden.getAllGrass();
		}
	}

	class rabbitSequence implements Sequence {

		@Override
		public double getSValue() {
			System.out.println("nb rabbits: " + agentList.size());
			return agentList.size();
		}
	}

	public static void main(String[] args) {
		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
		init.loadModel(model, "", false);
	}

	@Override
	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();

		displaySurf.display();
		plot.display();
	}

	public void buildModel() {
		System.out.println("Running BuildModel");

		garden = new RabbitsGrassSimulationSpace(worldXSize, worldYSize);
		garden.plantGrass(initGrass);
		for (int i = 0; i < numAgents; i++) {
			addNewAgent();
		}
		for (int i = 0; i < agentList.size(); i++) {
			RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent) agentList.get(i);
			rabbit.report();
		}
	}

	public void buildSchedule() {
		System.out.println("Running BuildSchedule");

		class RabbitsGrassSimulationStep extends BasicAction {
			@Override
			public void execute() {
				SimUtilities.shuffle(agentList);
				for (int i = 0; i < agentList.size(); i++) {
					RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent) agentList.get(i);

					if (rabbit.step()) {
						// rabbit ate grass
						System.out.println("eating grass");
						rabbit.addEnergy(energyGain);
					}
					// check for birth threshold
					if (rabbit.getEnergy() >= birthThreshold) {
						addNewAgent();
						rabbit.addEnergy(-initEnergy);
					} else {
						rabbit.addEnergy(-energyLoss);
					}
					rabbit.report();
				}
				// remove rabbits with energy < 1
				int deadAgents = reapDeadAgents();
				// plant grass
				garden.plantGrass(growthRate);

				displaySurf.updateDisplay();
			}
		}

		schedule.scheduleActionBeginning(0, new RabbitsGrassSimulationStep());

		class RabbitsGrassSimulationCountLiving extends BasicAction {
			@Override
			public void execute() {
				countLivingAgents();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationCountLiving());

		class PlotAction extends BasicAction {
			@Override
			public void execute() {
				plot.step();
			}
		}
		schedule.scheduleActionAtInterval(10, new PlotAction());
	}

	private int reapDeadAgents() {
		int count = 0;
		for (int i = agentList.size() - 1; i >= 0; i--) {
			RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent) agentList.get(i);
			if (rabbit.getEnergy() < 1) {
				garden.removeAgentAt(rabbit.getX(), rabbit.getY());
				agentList.remove(i);
				count++;
			}
		}
		return count;
	}

	public void buildDisplay() {
		System.out.println("Running BuildDisplay");

		ColorMap map = new ColorMap();

		map.mapColor(GRASS_INDEX, Color.green);
		map.mapColor(EMPTY_INDEX, Color.black);

		Value2DDisplay displayGrass = new Value2DDisplay(garden.getCurrentGrassSpace(), map);

		Object2DDisplay displayAgents = new Object2DDisplay(garden.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurf.addDisplayable(displayGrass, "Grass");
		displaySurf.addDisplayable(displayAgents, "Agents");
		plot.addSequence("Amount of grass", new grassSequence());
		plot.addSequence("Rabbit population", new rabbitSequence());
	}

	private int countLivingAgents() {
		int livingAgents = 0;
		for (int i = 0; i < agentList.size(); i++) {
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
			if (rgsa.getEnergy() > 0) {
				livingAgents++;
			}
		}
		System.out.println("Number of living agents is: " + livingAgents);

		return livingAgents;
	}

	private void addNewAgent() {
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(initEnergy);
		agentList.add(a);
		garden.addAgent(a);
	}

	@Override
	public String getName() {
		return "RabbitsGrassSimulation";
	}

	@Override
	public void setup() {
		System.out.close();
		System.out.println("Running setup");
		garden = null;
		agentList = new ArrayList();
		schedule = new Schedule(1);

		if (displaySurf != null) {
			displaySurf.dispose();
		}
		displaySurf = null;

		if (plot != null) {
			plot.dispose();
		}
		plot = null;

		displaySurf = new DisplaySurface(this, "Garden");
		plot = new OpenSequenceGraph("Comparison of rabbit population and grass amount", this);
		plot.setAxisTitles("Number of steps", "Count");

		registerDisplaySurface("Garden", displaySurf);
		registerMediaProducer("Plot", plot);
		System.out.println("Setup done");
	}

	@Override
	public Schedule getSchedule() {
		return schedule;
	}

	@Override
	public String[] getInitParam() {
		String[] initParams = { "NumAgents", "WorldXSize", "WorldYSize", "GrowthRate", "BirthThreshold", "InitEnergy",
		        "InitGrass", "EnergyLoss", "EnergyGain" };
		return initParams;
	}

	public int getInitEnergy() {
		return initEnergy;
	}

	public void setInitEnergy(int initEnergy) {
		this.initEnergy = initEnergy;
	}

	public int getEnergyLoss() {
		return energyLoss;
	}

	public void setEnergyLoss(int energyLoss) {
		this.energyLoss = energyLoss;
	}

	public int getEnergyGain() {
		return energyGain;
	}

	public void setEnergyGain(int energyGain) {
		this.energyGain = energyGain;
	}

	/**
	 * @return the numAgents
	 */
	public int getNumAgents() {
		return numAgents;
	}

	/**
	 * @param numAgents
	 *            the numAgents to set
	 */
	public void setNumAgents(int numAgents) {
		this.numAgents = numAgents;
	}

	/**
	 * @return the worldXSize
	 */
	public int getWorldXSize() {
		return worldXSize;
	}

	/**
	 * @param worldXSize
	 *            the worldXSize to set
	 */
	public void setWorldXSize(int worldXSize) {
		this.worldXSize = worldXSize;
	}

	/**
	 * @return the worldYSize
	 */
	public int getWorldYSize() {
		return worldYSize;
	}

	/**
	 * @param worldYSize
	 *            the worldYSize to set
	 */
	public void setWorldYSize(int worldYSize) {
		this.worldYSize = worldYSize;
	}

	/**
	 * @return the growthRate
	 */
	public int getGrowthRate() {
		return growthRate;
	}

	/**
	 * @param growthRate
	 *            the growthRate to set
	 */
	public void setGrowthRate(int growthRate) {
		this.growthRate = growthRate;
	}

	/**
	 * @return the birthThreshold
	 */
	public int getBirthThreshold() {
		return birthThreshold;
	}

	/**
	 * @param birthThreshold
	 *            the birthThreshold to set
	 */
	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}

	/**
	 * @return the initGrass
	 */
	public int getInitGrass() {
		return initGrass;
	}

	/**
	 * @param initGrass
	 *            the initGrass to set
	 */
	public void setInitGrass(int initGrass) {
		this.initGrass = initGrass;
	}
}

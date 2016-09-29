import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenHistogram;
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

	private OpenSequenceGraph amountOfGrassInGarden;
	private OpenHistogram agentWealthDistribution;

	// Default Values
	private static final int NUMAGENTS = 100;
	private static final int WORLDXSIZE = 40;
	private static final int WORLDYSIZE = 40;
	private static final int TOTALGRASS = 1000;
	private static final int AGENT_MIN_LIFESPAN = 30;
	private static final int AGENT_MAX_LIFESPAN = 50;

	private int numAgents = NUMAGENTS;
	private int worldXSize = WORLDXSIZE;
	private int worldYSize = WORLDYSIZE;
	private int grass = TOTALGRASS;
	private int agentMinLifespan = AGENT_MIN_LIFESPAN;
	private int agentMaxLifespan = AGENT_MAX_LIFESPAN;

	class grassInGarden implements DataSource, Sequence {

		@Override
		public Object execute() {
			return new Double(getSValue());
		}

		@Override
		public double getSValue() {
			return garden.getAllGrass();
		}
	}

	class agentGrass implements BinDataSource {
		@Override
		public double getBinValue(Object o) {
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) o;
			return rgsa.getGrass();
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
		amountOfGrassInGarden.display();
		agentWealthDistribution.display();
	}

	public void buildModel() {
		System.out.println("Running BuildModel");
		garden = new RabbitsGrassSimulationSpace(worldXSize, worldYSize);
		garden.plantGrass(grass);
		for (int i = 0; i < numAgents; i++) {
			addNewAgent();
		}
		for (int i = 0; i < agentList.size(); i++) {
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
			rgsa.report();
		}
	}

	public void buildSchedule() {
		System.out.println("Running BuildSchedule");

		class RabbitsGrassSimulationStep extends BasicAction {
			@Override
			public void execute() {
				SimUtilities.shuffle(agentList);
				for (int i = 0; i < agentList.size(); i++) {
					RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
					rgsa.step();
				}
				int deadAgents = reapDeadAgents();
				for (int i = 0; i < deadAgents; i++) {
					addNewAgent();
				}

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

		class RabbitsGrassSimulationUpdateGrassInGarden extends BasicAction {
			@Override
			public void execute() {
				amountOfGrassInGarden.step();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitsGrassSimulationUpdateGrassInGarden());

		class RabbitGrassSimulationUpdateAgentWealth extends BasicAction {
			@Override
			public void execute() {
				agentWealthDistribution.step();
			}
		}

		schedule.scheduleActionAtInterval(10, new RabbitGrassSimulationUpdateAgentWealth());
	}

	private int reapDeadAgents() {
		int count = 0;
		for (int i = agentList.size() - 1; i >= 0; i--) {
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
			if (rgsa.getStepsToLive() < 1) {
				garden.removeAgentAt(rgsa.getX(), rgsa.getY());
				garden.plantGrass(rgsa.getGrass());
				agentList.remove(i);
				count++;
			}
		}
		return count;
	}

	public void buildDisplay() {
		System.out.println("Running BuildDisplay");

		ColorMap map = new ColorMap();

		for (int i = 1; i < 16; i++) {
			map.mapColor(i, new Color(i * 8 + 127, 0, 0));
		}
		map.mapColor(0, Color.white);

		Value2DDisplay displayGrass = new Value2DDisplay(garden.getCurrentGrassSpace(), map);

		Object2DDisplay displayAgents = new Object2DDisplay(garden.getCurrentAgentSpace());
		displayAgents.setObjectList(agentList);

		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayAgents, "Agents");
		amountOfGrassInGarden.addSequence("Grass In Garden", new grassInGarden());
		agentWealthDistribution.createHistogramItem("Agent Wealth", agentList, new agentGrass());
	}

	private int countLivingAgents() {
		int livingAgents = 0;
		for (int i = 0; i < agentList.size(); i++) {
			RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent) agentList.get(i);
			if (rgsa.getStepsToLive() > 0) {
				livingAgents++;
			}
		}
		System.out.println("Number of living agents is: " + livingAgents);

		return livingAgents;
	}

	private void addNewAgent() {
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentMinLifespan, agentMaxLifespan);
		agentList.add(a);
		garden.addAgent(a);
	}

	@Override
	public String getName() {
		return "RabbitsGrassSimulation";
	}

	@Override
	public void setup() {
		System.out.println("Running setup");
		garden = null;
		agentList = new ArrayList();
		schedule = new Schedule(1);

		if (displaySurf != null) {
			displaySurf.dispose();
		}
		displaySurf = null;

		if (amountOfGrassInGarden != null) {
			amountOfGrassInGarden.dispose();
		}

		if (agentWealthDistribution != null) {
			agentWealthDistribution.dispose();
		}
		agentWealthDistribution = null;

		displaySurf = new DisplaySurface(this, "Carry Drop Model Window 1");
		amountOfGrassInGarden = new OpenSequenceGraph("Amount of grass in garden", this);
		agentWealthDistribution = new OpenHistogram("Agent Wealth", 8, 0);

		registerDisplaySurface("Carry Drop Model Window 1", displaySurf);
		registerMediaProducer("Plot", amountOfGrassInGarden);
	}

	@Override
	public Schedule getSchedule() {
		return schedule;
	}

	@Override
	public String[] getInitParam() {
		String[] initParams = { "NumAgents", "WorldXSize", "WorldYSize", "Grass", "AgentMinLifespan",
		        "AgentMaxLifespan" };
		return initParams;
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
	 * @return the grass
	 */
	public int getGrass() {
		return grass;
	}

	/**
	 * @param grass
	 *            the grass to set
	 */
	public void setGrass(int grass) {
		this.grass = grass;
	}

	/**
	 * @return the agentMinLifespan
	 */
	public int getAgentMinLifespan() {
		return agentMinLifespan;
	}

	/**
	 * @param agentMinLifespan
	 *            the agentMinLifespan to set
	 */
	public void setAgentMinLifespan(int agentMinLifespan) {
		this.agentMinLifespan = agentMinLifespan;
	}

	/**
	 * @return the agentMaxLifespan
	 */
	public int getAgentMaxLifespan() {
		return agentMaxLifespan;
	}

	/**
	 * @param agentMaxLifespan
	 *            the agentMaxLifespan to set
	 */
	public void setAgentMaxLifespan(int agentMaxLifespan) {
		this.agentMaxLifespan = agentMaxLifespan;
	}
}

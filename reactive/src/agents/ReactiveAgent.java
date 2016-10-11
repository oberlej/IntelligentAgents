package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import model.AgentAction;
import model.State;

public class ReactiveAgent implements ReactiveBehavior {
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private List<State> listOfStates;
	private TaskDistribution dist;
	private final double CHANGE_THRESHOLD = 0.0;
	private Topology topology;

	Map<State, AgentAction> bestAction;
	Map<State, Double> stateValue;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		System.out.println("Start ReactiveAgent Setup");
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		if (discount >= 1 || discount < 0) {
			System.out.println("Please enter a number in [0,1[");
			System.exit(-1);
		}

		this.topology = topology;
		pPickup = discount;
		numActions = 0;
		myAgent = agent;
		dist = distribution;

		bestAction = new HashMap<State, AgentAction>();
		stateValue = new HashMap<State, Double>();
		listOfStates = new ArrayList<State>();
		createStatesAndActions();
		MDP();
		System.out.println("Done with ReactiveAgent Setup");
	}

	private void createStatesAndActions() {
		System.out.println("Setting up States and Actions");
		for (City current : topology.cities()) {
			// Create one action per neighbor. The action is the movmement form
			// City current to City neighb without package
			List<AgentAction> neighborActions = new ArrayList<AgentAction>();

			for (City neighb : current.neighbors()) {
				AgentAction moveToNeighborWithoutPackage = new AgentAction(neighb, false);
				neighborActions.add(moveToNeighborWithoutPackage);
			}
			// Create the state of being in City current and not having a
			// package available, thus not having a detination city
			State noPAvailabeState = new State(current, null, neighborActions);
			listOfStates.add(noPAvailabeState);
			stateValue.put(noPAvailabeState, 0.0);

			for (City dest : topology.cities()) {
				if (!current.equals(dest)) {
					// Create the action of moving from City current to City
					// dest with a package
					List<AgentAction> packageActions = new ArrayList<AgentAction>();
					packageActions.add(new AgentAction(dest, true));
					// Create the state of being in City current and having a
					// package available to City dest
					State takingPState = new State(current, dest, packageActions);
					listOfStates.add(takingPState);
					stateValue.put(takingPState, 0.0);
				}
			}
		}
	}

	private void MDP() {
		System.out.println("Run MDP-Algorithm");
		int changed = 0;
		int nbStates = listOfStates.size();

		while (changed != nbStates) {
			changed = 0;
			for (State s : listOfStates) {
				double maxValue = -Double.MAX_VALUE;
				AgentAction bestAction = null;

				for (AgentAction a : s.getListOfActions()) {
					double sum = 0;

					for (State sp : listOfStates) {
						if (!s.getCurrent().equals(sp.getCurrent())) {
							sum += T(s, a, sp) * stateValue.get(sp);
						}
					}
					double currentValue = R(s, a) + pPickup * sum;
					if (currentValue >= maxValue) {
						maxValue = currentValue;
						bestAction = a;
					}
				}
				if (maxValue / stateValue.get(s) - 1 <= CHANGE_THRESHOLD) {
					changed++;
				}
				stateValue.put(s, maxValue);
				this.bestAction.put(s, bestAction);
			}
		}

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		City currentCity = vehicle.getCurrentCity();
		State s = null;

		if (availableTask == null) {
			s = getState(currentCity, null);
			AgentAction a = bestAction.get(s);
			action = new Move(a.getDestination());
		} else {
			s = getState(currentCity, availableTask.deliveryCity);
			AgentAction a = bestAction.get(s);
			if (a.isTackingPackage()) {
				action = new Pickup(availableTask);
			} else {
				action = new Move(a.getDestination());
			}
		}

		if (numActions >= 1) {
			System.out.println(
			        myAgent.name() + ": average profit: " + myAgent.getTotalProfit() / (double) numActions + ")");
		}
		numActions++;

		return action;
	}

	private double R(State s, AgentAction a) {
		Plan p = new Plan(s.getCurrent(), new Move(a.getDestination()));
		double costs = myAgent.vehicles().get(0).costPerKm() * p.totalDistance();

		if (!a.isTackingPackage()) {
			return -costs;
		} else {
			return dist.reward(s.getCurrent(), a.getDestination()) - costs;
		}
	}

	private double T(State s, AgentAction a, State sp) {
		if (s.getDestination() == null && sp.getCurrent().equals(a.getDestination())
		        || a.getDestination().equals(sp.getCurrent())) {
			// either destination is null (meaning no package is available) AND
			// sp.current is a neighbor or s.destination is equal to sp.current
			return dist.probability(sp.getCurrent(), sp.getDestination());
		}
		return 0;
	}

	private State getState(City current, City dest) {
		State tmp = new State(current, dest, null);
		for (State s : listOfStates) {
			if (s.equals(tmp)) {
				return s;
			}
		}
		System.out.println("No state found for " + tmp);
		System.exit(-1);
		return null;
	}
}

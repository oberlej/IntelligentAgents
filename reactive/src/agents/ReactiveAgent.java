package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
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
	private Random random;
	private List<State> listOfStates;
	private TaskDistribution dist;
	private final double CHANGE_THRESHOLD = 0.0005;

	Map<State, AgentAction> bestAction;
	Map<State, Double> stateValue;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		pPickup = discount;
		numActions = 0;
		myAgent = agent;
		random = new Random();
		dist = distribution;
		bestAction = new HashMap<State, AgentAction>();
		stateValue = new HashMap<State, Double>();

		listOfStates = new ArrayList<State>();
		for (City c : topology.cities()) {
			List<AgentAction> actions = new ArrayList<AgentAction>();
			State s = new State(c, null);

			for (City c2 : topology.cities()) {
				if (c != c2) {

					// add action for movement to c2 + taking a package
					AgentAction moveAndTake = new AgentAction(c2, true);
					actions.add(moveAndTake);
					if (c.hasNeighbor(c2)) {
						// add action for movement to c2 + without taking a
						// package
						AgentAction moveAndDontTake = new AgentAction(c2, false);
						actions.add(moveAndDontTake);
					}
				}
			}

			s.setListOfActions(actions);
			listOfStates.add(s);
			stateValue.put(s, 0.0);
		}

		// compute V(S) and Best(S)
		int changed = 0;
		int nbStates = listOfStates.size();
		while (changed != nbStates) {
			changed = 0;
			for (State s : listOfStates) {

				double maxValue = 0;
				AgentAction bestAction = null;
				for (AgentAction a : s.getListOfActions()) {
					double sum = 0;
					for (State sp : listOfStates) {
						sum += T(s, a, sp) * stateValue.get(sp);
					}
					double currentValue = R(s, a) + pPickup * sum;
					if (currentValue >= maxValue) {
						maxValue = currentValue;
						bestAction = a;
					}
				}
				if (Math.abs(maxValue) - stateValue.get(s) <= CHANGE_THRESHOLD) {
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
		try {
			s = getStateFromCity(currentCity);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		AgentAction a = bestAction.get(s);

		if (availableTask == null) {
			if (a.isTackingPackage()) {
				action = new Move(currentCity.randomNeighbor(random));
			} else {
				action = new Move(a.getDestination());
			}
		} else {
			if (a.isTackingPackage()) {
				action = new Pickup(availableTask);
			} else {
				action = new Move(a.getDestination());
			}
		}

		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit()
			        + " (average profit: " + myAgent.getTotalProfit() / (double) numActions + ")");
		}
		numActions++;

		return action;
	}

	private double R(State s, AgentAction a) {
		if (a.isTackingPackage()) {
			return dist.probability(s.getCity(), a.getDestination())
			        * (dist.reward(s.getCity(), a.getDestination()) - dist.weight(s.getCity(), a.getDestination()));
		}
		return -dist.weight(s.getCity(), a.getDestination());
	}

	private double T(State s, AgentAction a, State sp) {
		if (a.isTackingPackage()) {
			return dist.probability(s.getCity(), sp.getCity());
		}
		return a.getDestination() == sp.getCity() ? 1 : 0;
	}

	private State getStateFromCity(City c) throws Exception {
		for (State s : listOfStates) {
			if (s.getCity() == c) {
				return s;
			}
		}
		throw new Exception("No state found for city: " + c);
	}

}

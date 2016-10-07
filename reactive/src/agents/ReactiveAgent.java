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
	private Random random;
	private List<State> listOfStates;
	private TaskDistribution dist;
	private final double CHANGE_THRESHOLD = 0;

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
			// printV();
			printB();
			for (State s : listOfStates) {
				double maxValue = Double.MIN_NORMAL;
				AgentAction bestAction = null;
				for (AgentAction a : s.getListOfActions()) {
					double sum = 0;
					for (State sp : listOfStates) {
						if (!sp.equals(s)) {
							sum += T(s, a, sp) * stateValue.get(sp);
						}
					}
					// System.out.println("sum: " + sum);
					double currentValue = R(s, a) + pPickup * sum;
					// System.out.println("current value: " + currentValue);
					if (currentValue >= maxValue) {
						maxValue = currentValue;
						bestAction = a;
					}
				}
				if (Math.abs(maxValue - stateValue.get(s)) <= CHANGE_THRESHOLD) {
					changed++;
				}
				stateValue.put(s, maxValue);
				this.bestAction.put(s, bestAction);
			}
		}
		for (Map.Entry<State, Double> entry : stateValue.entrySet()) {
			System.out.println(entry.getKey().getCity() + ": " + entry.getValue());
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

		System.out.println(myAgent.name() + ": action planned: " + a);

		if (availableTask == null) {
			if (a.isTackingPackage()) {
				// check the best neighbors
				State bestState = null;
				Double bestValue = 0.0;
				for (City c : currentCity.neighbors()) {
					for (State ns : listOfStates) {
						if (ns.getCity().equals(c)) {
							if (stateValue.get(ns) >= bestValue) {
								bestState = ns;
							}

						}
					}
				}
				if (bestState != null) {
					action = new Move(bestState.getCity());
				} else {
					action = null;
					System.out.println("shouldnt happen");
					System.exit(0);
				}
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
			System.out.println(
			        myAgent.name() + ": average profit: " + myAgent.getTotalProfit() / (double) numActions + ")");
		}
		numActions++;

		return action;
	}

	private double R(State s, AgentAction a) {

		Plan p = new Plan(s.getCity(), new Move(a.getDestination()));
		double cost = 5 * p.totalDistance();

		if (a.isTackingPackage()) {
			// return dist.probability(s.getCity(), a.getDestination())
			// * (dist.reward(s.getCity(), a.getDestination()) -
			// dist.weight(s.getCity(), a.getDestination()));
			// System.out.println("prob: " + dist.probability(s.getCity(),
			// a.getDestination()));
			double x = dist.probability(s.getCity(), a.getDestination())
			        * (dist.reward(s.getCity(), a.getDestination()) - cost);
			// System.out.println("Compute R: " + x);
			return x;
		}
		// return -dist.weight(s.getCity(), a.getDestination());
		// System.out.println("Compute R: " + -cost);
		return -cost;
	}

	private double T(State s, AgentAction a, State sp) {
		if (a.isTackingPackage()) {
			double x = a.getDestination().equals(sp.getCity()) ? dist.probability(s.getCity(), sp.getCity()) : 0;
			// System.out.println("Compute T: " + x);
			return x;
		}
		double x = a.getDestination().equals(sp.getCity()) ? 1 : 0;
		// System.out.println("Compute T: " + x);
		return x;
	}

	private State getStateFromCity(City c) throws Exception {
		for (State s : listOfStates) {
			if (s.getCity().equals(c)) {
				return s;
			}
		}
		throw new Exception("No state found for city: " + c);
	}

	private void printV() {
		for (Map.Entry<State, Double> entry : stateValue.entrySet()) {
			System.out.print("V(" + entry.getKey().getCity() + ")= " + entry.getValue() + " | ");
		}
		System.out.println("");
	}

	private void printB() {
		for (Map.Entry<State, AgentAction> entry : bestAction.entrySet()) {
			System.out.println("B(" + entry.getKey().getCity() + ")= " + entry.getValue() + " | ");
		}
	}

}

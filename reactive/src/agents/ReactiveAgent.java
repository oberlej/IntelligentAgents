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
	private final double CHANGE_THRESHOLD = 0;
	private Topology topology;

	Map<State, AgentAction> bestAction;
	Map<State, Double> stateValue;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.topology = topology;
		pPickup = discount;

		numActions = 0;
		myAgent = agent;
		dist = distribution;
		bestAction = new HashMap<State, AgentAction>();
		stateValue = new HashMap<State, Double>();

		listOfStates = new ArrayList<State>();
		for (City c : topology.cities()) {
			List<AgentAction> actions = new ArrayList<AgentAction>();

			for (City c2 : c.neighbors()) {
				// add action for movement to c2 + without package
				AgentAction moveWithout = new AgentAction(c2, false);
				actions.add(moveWithout);
			}

			AgentAction moveWith = new AgentAction(null, true);
			actions.add(moveWith);

			State s = new State(c, actions);
			listOfStates.add(s);
			stateValue.put(s, 0.0);
		}

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
						sum += T(s, a, sp) * stateValue.get(sp);

					}
					if (a.isTackingPackage()) {
						// the case of no task when wanting to take a task
						sum += T(s, a, null) * stateValue.get(getBestNeighbor(s));
					}

					// System.out.println("sum: " + sum);
					double currentValue = R(s, a) + pPickup * sum;

					System.out.println("current value: " + currentValue);
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

				System.out.println(s);
				System.out.println(bestAction);

			}
		}
		for (Map.Entry<State, Double> entry : stateValue.entrySet()) {
			System.out.println(entry.getKey().getCity() + ": " + entry.getValue());
		}

	}

	private State getBestNeighbor(State s) {

		List<City> neighbors = s.getCity().neighbors();
		City bestC = neighbors.get(0);

		for (City c : neighbors) {
			try {
				if (stateValue.get(getStateFromCity(c)) > stateValue.get(getStateFromCity(bestC))) {
					bestC = c;
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		// TODO Auto-generated method stub
		try {
			return getStateFromCity(bestC);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
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

		// if (a == null) {
		// System.out.println(s);
		// System.out.println(a);
		// }

		// System.out.println(myAgent.name() + ": action planned: " + a);

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

		if (!a.isTackingPackage()) {
			Plan p = new Plan(s.getCity(), new Move(a.getDestination()));
			return -(5 * p.totalDistance());

		} else {
			double rewardSum = 0;
			for (City n : topology.cities()) {
				if (s.getCity() != n) {
					rewardSum += dist.reward(s.getCity(), n) * dist.probability(s.getCity(), n);
					Plan p = new Plan(s.getCity(), new Move(n));
					rewardSum += -(5 * p.totalDistance());
				}
			}
			return rewardSum / (topology.cities().size() - 1);
		}
	}

	private double T(State s, AgentAction a, State sp) {

		if (sp == null) {
			return dist.probability(s.getCity(), null);
		}

		if (a.isTackingPackage()) {
			return dist.probability(s.getCity(), sp.getCity());
			// System.out.println("Compute T: " + x);
		}

		else {
			return a.getDestination().equals(sp.getCity()) ? 1 : 0;
		}

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

package agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Action.Delivery;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import model.State;

public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm {
		BFS, ASTAR
	}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	public static double costPerKm;

	/* the planning class */
	Algorithm algorithm;

	boolean isJumboTaskSet = false;

	TaskSet pickedUpTasks = null;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		this.topology = topology;
		this.td = td;
		this.agent = agent;
		costPerKm = agent.vehicles().get(0).costPerKm();

		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		System.out.println("Agent " + (agent.id() + 1) + ": " + agent.name());
		Plan plan;

		switch (algorithm) {
		case ASTAR:
			plan = aStar(vehicle, tasks);
			break;
		case BFS:
			plan = BFS(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		return plan;
	}

	private Plan aStar(Vehicle vehicle, TaskSet tasks) {
		List<State> open = new ArrayList<State>();
		// add root
		open.add(new State(tasks, vehicle.getCurrentTasks(), vehicle.getCurrentCity(), agent.getTotalCost(),
		        vehicle.capacity(), new State(null, null, null, 0, 0, null)));

		State current;
		int nbStates = 1;
		State newNeighbor;

		current = open.get(0);
		open.remove(0);

		while (!current.getAvailableTasks().isEmpty() || !current.getPickedUpTasks().isEmpty()) {
			// add new neighbor states

			// pick up a new task if possible
			for (Task t : current.getAvailableTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.pickUpTask(t)) {
					// task was picked up
					newNeighbor.setParentState(current);
					newNeighbor.setFcost(newNeighbor.getCost() + h(newNeighbor));
					open.add(newNeighbor);
					nbStates++;
				}
			}

			// deliver one of our picked up tasks
			for (Task t : current.getPickedUpTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.deliverTask(t)) {
					// task was delivered
					newNeighbor.setParentState(current);
					newNeighbor.setFcost(newNeighbor.getCost() + h(newNeighbor));
					open.add(newNeighbor);
					nbStates++;
				}
			}
			Collections.sort(open);
			current = open.get(0);
			open.remove(0);
		}

		// System.out.println("While loop done after " + nbStates + ". Final
		// state: " + current);
		System.out.println("Final cost: " + current.getCost());
		Plan p = new Plan(vehicle.getCurrentCity(), createReversePathFromState(current));

		return p;
	}

	private double h(State s) {
		double cost = 0;
		for (Task t : s.getAvailableTasks()) {
			cost += t.pickupCity.distanceTo(t.deliveryCity) * costPerKm;
		}
		cost += s.getPickedUpTasks().size() * 800;
		return cost;
	}

	private Plan BFS(Vehicle vehicle, TaskSet tasks) {
		List<State> queue = new ArrayList<State>();
		// add root
		queue.add(new State(tasks, vehicle.getCurrentTasks(), vehicle.getCurrentCity(), agent.getTotalCost(),
		        vehicle.capacity(), null));

		State bestFinalState = new State(null, null, null, Integer.MAX_VALUE, 0, null);

		int nbStates = 1;
		State newNeighbor;
		State current;
		// this would stop after finding the first final solution
		// while (!queue.isEmpty() && bestFinalState.getCost() ==
		// Integer.MAX_VALUE) {
		while (!queue.isEmpty()) {
			current = queue.get(0);
			queue.remove(0);

			// add new neighbor states

			// pick up a new task if possible
			for (Task t : current.getAvailableTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.pickUpTask(t)) {
					// task was picked up
					if (newNeighbor.getCost() < bestFinalState.getCost()) {
						newNeighbor.setParentState(current);
						queue.add(newNeighbor);
						nbStates++;
					}
				}
			}
			// deliver one of our picked up tasks
			for (Task t : current.getPickedUpTasks()) {
				newNeighbor = current.clone();
				if (newNeighbor.deliverTask(t)) {
					// task was delivered
					if (newNeighbor.getCost() < bestFinalState.getCost()) {
						newNeighbor.setParentState(current);
						if (newNeighbor.getAvailableTasks().isEmpty() && newNeighbor.getPickedUpTasks().isEmpty()) {
							// we are in a final state s we don't need to add it
							// to the queue
							if (newNeighbor.getCost() < bestFinalState.getCost()) {
								bestFinalState = newNeighbor;
							}
						} else {
							queue.add(newNeighbor);
							nbStates++;
						}
					}
				}
			}
		}
		// System.out.println("While loop done after " + nbStates);
		return new Plan(vehicle.getCurrentCity(), createReversePathFromState(bestFinalState));
	}

	/**
	 * this function lets us create the list of actions for the final state. It
	 * takes the optimal path in reverse order and creates all the corresponding
	 * actions. In the end it reverses the list to put them in the correct
	 * order.
	 *
	 * @param finalState
	 * @return
	 */
	private List<Action> createReversePathFromState(State finalState) {
		List<Action> actions = new ArrayList<Action>();
		State current = finalState;
		do {
			Task t;
			if (current.hasPickedUpTaskInCurrentCity()) {
				// find task that has been picked up in the current.city
				t = (Task) TaskSet
				        .intersectComplement(current.getParentState().getAvailableTasks(), current.getAvailableTasks())
				        .toArray()[0];
				actions.add(new Pickup(t));
			} else {
				// find task that has been delivered in the current.city
				t = (Task) TaskSet
				        .intersectComplement(current.getParentState().getPickedUpTasks(), current.getPickedUpTasks())
				        .toArray()[0];
				actions.add(new Delivery(t));
			}
			actions.addAll(createReverseMoves(current.getParentState().getCurrentCity(), current.getCurrentCity()));

			current = current.getParentState();
		} while (current != null && current.getParentState() != null);
		Collections.reverse(actions);
		return actions;
	}

	/**
	 * this function creates the move actions to go from city source to city
	 * destination but in reverse order. Reverse order is needed to recreate the
	 * optimal path for a final state.
	 *
	 * @param source
	 * @param destination
	 * @return
	 */
	private List<Action> createReverseMoves(City source, City destination) {
		List<Action> moves = new ArrayList<Action>();
		if (!source.equals(destination)) {
			for (City city : source.pathTo(destination)) {
				moves.add(new Move(city));
			}
			Collections.reverse(moves);
		}
		return moves;
	}

	/**
	 * no actions are needed here because we take the task that are picked up
	 * but not delivered into account in the plan(). This is done by checking
	 * vehile.getCurrentTasks().
	 *
	 * @param carriedTasks
	 */
	@Override
	public void planCancelled(TaskSet carriedTasks) {
	}

}

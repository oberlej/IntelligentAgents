package agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import model.COD;
import model.VAction;
import model.VDeliveryAction;
import model.VPickupAction;

public class AuctionAgent implements AuctionBehavior {

	private List<Vehicle> listOfVehicles;
	private List<Task> listOfTasks;
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_bid;
	private long timeout_plan;
	private boolean isPlan = false;

	private final Double LOCAL_CHOICE_P = 0.5;
	private Set<COD> neighbors;
	private Random random;

	private COD currentPlan;
	private double currentCost;
	private COD newPlan;
	private double newCost;

	private double totalReward = 0;
	private double currentProfit = 0;

	private List<Task> opponentTasks;
	private double opponentTotalReward = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		listOfVehicles = agent.vehicles();
		listOfTasks = new ArrayList<Task>();
		opponentTasks = new ArrayList<Task>();
		City currentCity = listOfVehicles.get(0).homeCity();
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		random = new Random(seed);
		currentPlan = new COD(listOfVehicles);

		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config/settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}
		timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			// save last computed plan
			totalReward += bids[winner];
			currentProfit = totalReward - newCost;
			currentPlan = newPlan;
			currentCost = newCost;
		} else {
			// discard pland
			opponentTasks.add(previous);
			opponentTotalReward += bids[winner];

			listOfTasks.remove(listOfTasks.size() - 1);
		}
		// update oher agents information
	}

	@Override
	public Long askPrice(Task task) {
		List<Vehicle> vehicles = listOfVehicles;

		newPlan = null;
		newCost = -1;
		// add the task to a random vehicle
		for (Vehicle v : vehicles) {
			if (currentPlan.linkedVehicleTasks.get(v).size() < 1 && v.capacity() >= task.weight
			        || currentPlan.linkedVehicleTasks.get(v).getLast().remainingVCapacity >= task.weight) {
				newPlan = currentPlan.clone();
				VAction delivery = new VDeliveryAction(task, 0);
				VAction pickup = new VPickupAction(task, 0);

				if (newPlan.linkedVehicleTasks.get(v).size() > 0) {
					int indexP = random.nextInt(newPlan.linkedVehicleTasks.get(v).size() - 1);
					int indexD = random.nextInt(newPlan.linkedVehicleTasks.get(v).size() - indexP) + indexP;

					newPlan.linkedVehicleTasks.get(v).add(indexD, delivery);
					newPlan.linkedVehicleTasks.get(v).add(indexP, pickup);

					newPlan.taskVehiclePair.put(task, v);
				} else {
					newPlan.addTask(v, task);
				}

				listOfTasks.add(task);

				break;
			}
		}
		// no car can take the task
		if (newPlan == null) {
			return null;
		}

		// compute minimal cost/best plan with the new task
		SLS();

		double ratio = 1.0 + random.nextDouble() * 0.05 * task.id;
		double bid = ratio * (newCost - currentCost);
		if (bid < 0) {
			bid = 0;
		}

		double newProfit = totalReward + bid - newCost;

		if (newProfit < currentProfit) {
			bid += currentProfit - newProfit;
			newProfit = totalReward + bid - newCost;
		}

		System.out.println("stupid bids: " + bid);
		System.out.println("stupid profit: " + currentProfit);
		System.out.println();
		return (long) Math.round(bid);

	}

	// recreate currentPlan with the new tasks
	private void selectInitialSolution() {
		newPlan = new COD(listOfVehicles);
		HashMap<Integer, Task> taskMap = new HashMap<Integer, Task>();
		for (Task t : listOfTasks) {
			taskMap.put(t.id, t);
		}
		for (Vehicle v : listOfVehicles) {
			LinkedList<VAction> list = new LinkedList<VAction>();
			for (VAction va : currentPlan.linkedVehicleTasks.get(v)) {
				VAction newVa = va.clone();
				newVa.task = taskMap.get(va.task.id);
				list.add(newVa);
			}
			newPlan.linkedVehicleTasks.put(v, list);
		}
		for (Entry<Task, Vehicle> e : currentPlan.taskVehiclePair.entrySet()) {
			newPlan.taskVehiclePair.put(taskMap.get(e.getKey().id), e.getValue());
		}
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("Last best plan found: " + currentPlan);
		System.out.println("Cost: " + currentCost);
		List<Plan> plans = new ArrayList<Plan>();
		if (tasks.isEmpty()) {
			for (Vehicle v : listOfVehicles) {
				plans.add(new Plan(v.getCurrentCity()));
			}
			return plans;
		}

		isPlan = true;
		listOfVehicles = vehicles;
		listOfTasks.clear();
		for (Task t : tasks) {
			listOfTasks.add(t);
		}

		selectInitialSolution();
		SLS();
		System.out.println("New best plan found: " + newPlan);
		System.out.println("Cost: " + newCost);
		Plan p;
		double sum = 0;
		for (Vehicle v : vehicles) {
			p = new Plan(v.homeCity());
			City currentCity = v.homeCity();
			for (VAction va : newPlan.linkedVehicleTasks.get(v)) {
				if (!currentCity.equals(va.getCity())) {
					sum += currentCity.distanceTo(va.getCity()) * v.costPerKm();
					createMoves(currentCity, va.getCity(), p);
				}

				p.append(va.getAction());
				currentCity = va.getCity();
			}
			plans.add(p);
		}

		return plans;
	}

	private void createMoves(City source, City destination, Plan p) {
		for (City city : source.pathTo(destination)) {
			p.appendMove(city);
		}
	}

	private void SLS() {
		neighbors = new HashSet<COD>();
		COD A = newPlan;
		COD bestA = A;
		Double costBestA = C(A);
		long maxTimeout = isPlan ? timeout_plan : timeout_bid;
		long start_time = System.currentTimeMillis();

		do {
			COD oldA = A;
			chooseNeighbors(oldA);
			A = localChoice(oldA);

			Double costA = C(A);
			if (costA < costBestA) {
				bestA = A;
				costBestA = costA;
			}

		} while (start_time + maxTimeout - 300 > System.currentTimeMillis());

		newPlan = bestA;
		newCost = costBestA;
	}

	private COD localChoice(COD oldA) {
		HashMap<COD, Double> bestChoicesA = new HashMap<COD, Double>();
		if (neighbors.isEmpty()) {
			return oldA;
		}

		Iterator<COD> i = neighbors.iterator();

		COD choiceA = i.next();
		bestChoicesA.put(choiceA, C(choiceA));

		while (i.hasNext()) {
			COD A = i.next();
			double costA = C(A);

			if (costA <= bestChoicesA.get(choiceA)) {
				if (costA < bestChoicesA.get(choiceA)) {
					bestChoicesA.clear();
				}
				choiceA = A;
				bestChoicesA.put(A, costA);
			}
		}

		if (Math.random() < LOCAL_CHOICE_P) {
			COD choice = (COD) bestChoicesA.keySet().toArray()[(int) (Math.random() * bestChoicesA.size())];
			neighbors.clear();
			return choice;
		}

		return oldA;
	}

	private void chooseNeighbors(COD oldA) {
		// neighbor solutions

		// change vehicle
		Vehicle vRand;
		Random r = new Random();
		do {
			vRand = listOfVehicles.get(r.nextInt(listOfVehicles.size()));
		} while (oldA.nextVAction(vRand) == null);

		for (Vehicle v : listOfVehicles) {
			if (!v.equals(vRand)) {
				for (Task t : listOfTasks) {
					if (oldA.taskVehiclePair.get(t).equals(vRand) && t.weight <= v.capacity()) {
						COD A = changingVehicle(oldA, vRand, v, t);
						if (A != null) {
							neighbors.add(A);
						}
					}
				}
			}
		}

		// change tasks
		int length = oldA.linkedVehicleTasks.get(vRand).size();

		if (length >= 2) {
			for (int i = 0; i < length; i++) {
				for (int j = i + 1; j < length; j++) {
					COD A = changingVActionOrder(oldA, vRand, i, j);
					if (A != null) {
						neighbors.add(A);
					}
				}
			}
		}
	}

	private COD changingVehicle(COD A, Vehicle v1, Vehicle v2, Task t) {
		COD A1 = A.clone();

		int indexT = A.linkedVehicleTasks.get(v1).indexOf(new VPickupAction(t, 0));

		VAction delivery = new VDeliveryAction(t, 0);
		VAction pickup = new VPickupAction(t, 0);

		// remove both vactions from v1
		A1.linkedVehicleTasks.get(v1).remove(delivery);
		A1.linkedVehicleTasks.get(v1).remove(pickup);

		Random r = new Random();
		if (A1.linkedVehicleTasks.get(v2).size() > 2) {
			int indexP = r.nextInt(A1.linkedVehicleTasks.get(v2).size() - 1);
			int indexD = r.nextInt(A1.linkedVehicleTasks.get(v2).size() - indexP) + indexP;

			A1.linkedVehicleTasks.get(v2).add(indexD, delivery);
			A1.linkedVehicleTasks.get(v2).add(indexP, pickup);

			A1.taskVehiclePair.put(t, v2);
		} else {
			A1.addTask(v2, t);
		}

		if (indexT < A1.linkedVehicleTasks.get(v1).size()) {
			A1.updateCapacity(v1, indexT);
		}
		return A1.updateCapacity(v2, 0) ? A1 : null;
	}

	private COD changingVActionOrder(COD A, Vehicle v, int i, int j) {

		COD A1 = A.clone();

		VAction va1 = A1.linkedVehicleTasks.get(v).get(i);
		VAction va2 = A1.linkedVehicleTasks.get(v).get(j);
		if (va1.task.equals(va2.task)) {
			return null;
		}

		// verify that we pickup va1.task before delivering it even after
		// changing the order
		if (va1.isPickup()) {
			VAction partnerVa1 = new VDeliveryAction(va1.task, 0);
			int indexP1 = A1.linkedVehicleTasks.get(v).indexOf(partnerVa1);
			if (indexP1 <= j) {
				return null;
			}
		}

		if (!va2.isPickup()) {
			VAction partnerVa2 = new VPickupAction(va2.task, 0);
			int indexP2 = A1.linkedVehicleTasks.get(v).indexOf(partnerVa2);
			if (indexP2 >= i) {
				return null;
			}
		}

		VAction vaPre1 = A1.linkedVehicleTasks.get(v).get(i > 0 ? i - 1 : 0);
		VAction vaPre2 = A1.linkedVehicleTasks.get(v).get(j - 1);
		VAction vaPost1 = A1.linkedVehicleTasks.get(v).get(i + 1);

		if (vaPost1.equals(va2)) {
			A1.linkedVehicleTasks.get(v).remove(va1);
			A1.setNextVAction(va2, va1, v);
		} else {

			A1.linkedVehicleTasks.get(v).remove(va2);
			A1.setNextVAction(vaPre2, va1, v);
			A1.linkedVehicleTasks.get(v).remove(va1);
			if (va1.equals(vaPre1)) {
				// t1 was first in the list
				A1.setNextVAction(v, va2);
			} else {
				A1.setNextVAction(vaPre1, va2, v);
			}
		}
		return A1.updateCapacity(v, i) ? A1 : null;
	}

	public Double C(COD cod) {

		Double sum = 0.0;

		for (Vehicle v : listOfVehicles) {
			City currentCity = v.homeCity();
			for (VAction va : cod.linkedVehicleTasks.get(v)) {
				if (!currentCity.equals(va.getCity())) {
					sum += currentCity.distanceTo(va.getCity()) * v.costPerKm();
				}
				currentCity = va.getCity();
			}
		}
		return sum;
	}
}

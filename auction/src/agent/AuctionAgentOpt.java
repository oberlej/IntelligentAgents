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

public class AuctionAgentOpt implements AuctionBehavior {

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

	private final Double GREED_P = 1D;
	private final Double PROFIT_P = 1D;
	private final Double PICKUP_IC_P = 1D;
	private final Double OPPONENT_P = 1D;

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

	private void placeTask(Task task) {
		for (Vehicle v : listOfVehicles) {
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
	}

	@Override
	public Long askPrice(Task task) {
		newPlan = null;
		newCost = -1;
		// add the task to a random vehicle
		placeTask(task);

		// no car can take the task
		if (newPlan == null) {
			return null;
		}

		// compute minimal cost/best plan with the new task
		SLS();

		double addedCost = addedCost();
		System.out.println("addedCost " + addedCost);

		double profit = profit();
		System.out.println("profit " + profit);

		double pickupIncentiveValue = pickupIncentiveValue();
		System.out.println("pickupIncentiveValue " + pickupIncentiveValue);

		double greedValue = greedValue();
		System.out.println("greedValue " + greedValue);

		double opponentValue = opponentValue();
		System.out.println("opponentValue " + opponentValue);

		// compute bid
		double bid = addedCost + PROFIT_P * profit - PICKUP_IC_P * pickupIncentiveValue + GREED_P * greedValue
		        + OPPONENT_P * opponentValue;

		if (bid < 0) {
			System.out.println("bid was neg");
			bid = 0;
		}
		System.out.println("bid " + bid);
		System.out.println("our profit: " + currentProfit);
		System.out.println();
		return (long) Math.round(bid);

	}

	private double profit() {
		double randomize = 0.75 + (int) (Math.random() * 1.25);
		double averageReward = totalReward / listOfTasks.size();
		return averageReward * 0.1 * randomize;
	}

	private double opponentValue() {
		// listOfTaks.size() is nb of actual tasks + 1 (bc we add the current
		// auction task to it)

		double ourRatio = totalReward / listOfTasks.size();
		double opponentRatio = opponentTotalReward / opponentTasks.size();
		double ratioDiff = Math.abs(ourRatio - opponentRatio);

		System.out.println("ourRatio " + ourRatio);
		System.out.println("opponentRatio " + opponentRatio);

		double value = 0;
		if (ourRatio < opponentRatio) {
			if (listOfTasks.size() <= opponentTasks.size()) {
				// we have less tasks => be less greedy => value < 0
				value = -ratioDiff;
			} else {
				// we have more task => be more greedy => value > 0
				value = ratioDiff;
			}
		}
		return value;
	}

	/**
	 * todo: add time influence
	 *
	 * @return
	 */
	private double greedValue() {
		double value = 0;
		if (currentProfit < 0) {
			// compensate for neg proft
			value = -currentProfit / 3;
		}
		return value;
	}

	/**
	 * todo: add task prob
	 *
	 * @return
	 */
	private double pickupIncentiveValue() {
		double value = addedCost() * (1f / listOfTasks.size());
		return value;
	}

	private double addedCost() {
		double value = newCost - currentCost;
		return value < 0 ? 0 : value;
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
		isPlan = true;
		listOfVehicles = vehicles;
		listOfTasks.clear();
		for (Task t : tasks) {
			listOfTasks.add(t);
		}

		List<Plan> plans = new ArrayList<Plan>();
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

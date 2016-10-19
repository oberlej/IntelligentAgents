package model;

import agents.DeliberativeAgent;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State implements Cloneable {

	private TaskSet availableTasks;
	private TaskSet pickedUpTasks;

	private City currentCity;

	private double cost;
	private int remainingCapacity;

	private State parentState;
	private boolean pickedUpTaskInCurrentCity;

	public State(TaskSet availableTasks, TaskSet pickedUpTasks, City currentCity, double cost, int remainingCapacity,
	        State parentState) {
		super();
		this.availableTasks = availableTasks;
		this.pickedUpTasks = pickedUpTasks;
		this.currentCity = currentCity;
		this.cost = cost;
		this.remainingCapacity = remainingCapacity;

	}

	public TaskSet getAvailableTasks() {
		return availableTasks;
	}

	public TaskSet getPickedUpTasks() {
		return pickedUpTasks;
	}

	public City getCurrentCity() {
		return currentCity;
	}

	public double getCost() {
		return cost;
	}

	public int getRemainingCapacity() {
		return remainingCapacity;
	}

	@Override
	public State clone() {
		return new State(availableTasks.clone(), pickedUpTasks.clone(), currentCity, cost, remainingCapacity,
		        parentState);
	}

	public boolean pickUpTask(Task t) {
		// cannot accept task
		if (remainingCapacity < t.weight) {
			return false;
		}
		if (availableTasks.remove(t)) {
			if (pickedUpTasks.add(t)) {
				cost += currentCity.distanceTo(t.pickupCity) * DeliberativeAgent.costPerKm;
				remainingCapacity -= t.weight;
				currentCity = t.pickupCity;
				pickedUpTaskInCurrentCity = true;
				return true;
			} else {
				System.out.println("task " + t + " already picked up!");
				System.exit(-1);
			}
		} else {
			System.out.println("task " + t + " not available!");
			System.exit(-1);
		}
		return false;
	}

	public boolean deliverTask(Task t) {
		if (pickedUpTasks.remove(t)) {
			cost += currentCity.distanceTo(t.deliveryCity) * DeliberativeAgent.costPerKm;
			remainingCapacity += t.weight;
			currentCity = t.deliveryCity;
			pickedUpTaskInCurrentCity = false;
			return true;
		} else {
			System.out.println("task " + t + " has not been picked up!");
			System.exit(-1);
		}
		return false;
	}

	public void setParentState(State parent) {
		parentState = parent;
	}

	public State getParentState() {
		return parentState;
	}

	@Override
	public String toString() {
		return "S(" + currentCity + ", " + getAvailableTasks().size() + ", " + getPickedUpTasks().size() + ")";
	}

	// @Override
	// public String toString() {
	// return "S(" + currentCity + ", " + getAvailableTasks() + ", " +
	// getPickedUpTasks() + ")";
	// }

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		State other = (State) obj;
		if (availableTasks == null) {
			if (other.availableTasks != null) {
				return false;
			}
		} else if (!availableTasks.equals(other.availableTasks)) {
			return false;
		}
		if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost)) {
			return false;
		}
		if (currentCity == null) {
			if (other.currentCity != null) {
				return false;
			}
		} else if (!currentCity.equals(other.currentCity)) {
			return false;
		}
		if (parentState == null) {
			if (other.parentState != null) {
				return false;
			}
		} else if (!parentState.equals(other.parentState)) {
			return false;
		}
		if (pickedUpTasks == null) {
			if (other.pickedUpTasks != null) {
				return false;
			}
		} else if (!pickedUpTasks.equals(other.pickedUpTasks)) {
			return false;
		}
		if (remainingCapacity != other.remainingCapacity) {
			return false;
		}
		return true;
	}

	/**
	 * @return the pickedUpTaskInCurrentCity
	 */
	public boolean hasPickedUpTaskInCurrentCity() {
		return pickedUpTaskInCurrentCity;
	}
}

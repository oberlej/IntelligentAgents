package model;

import java.util.List;

import logist.task.Task;
import logist.topology.Topology.City;

public class State {

	private List<Task> availableTasks;
	private List<Task> pickedUpTasks;

	private City currentCity;

	private double cost;
	private int remainingCapacity;

	public State(List<Task> availableTasks, List<Task> pickedUpTasks, City currentCity, double cost,
			int remainingCapacity) {
		super();
		this.availableTasks = availableTasks;
		this.pickedUpTasks = pickedUpTasks;
		this.currentCity = currentCity;
		this.cost = cost;
		this.remainingCapacity = remainingCapacity;

	}

	public List<Task> getAvailableTasks() {
		return availableTasks;
	}

	public List<Task> getPickedUpTasks() {
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (availableTasks == null) {
			if (other.availableTasks != null)
				return false;
		} else if (!availableTasks.equals(other.availableTasks))
			return false;
		if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost))
			return false;
		if (remainingCapacity != other.remainingCapacity)
			return false;
		if (currentCity == null) {
			if (other.currentCity != null)
				return false;
		} else if (!currentCity.equals(other.currentCity))
			return false;
		if (pickedUpTasks == null) {
			if (other.pickedUpTasks != null)
				return false;
		} else if (!pickedUpTasks.equals(other.pickedUpTasks))
			return false;
		return true;
	}

}

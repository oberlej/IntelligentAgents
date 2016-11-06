package model;

import logist.simulation.Vehicle;

public class TaskInformation {

	public int time;
	public Vehicle vehicle;

	public TaskInformation(int time, Vehicle vehicle) {
		this.time = time;
		this.vehicle = vehicle;
	}

	@Override
	public TaskInformation clone() {
		return new TaskInformation(time, vehicle);
	}

}

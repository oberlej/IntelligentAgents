package model;

import logist.plan.Action;
import logist.task.Task;
import logist.topology.Topology.City;

public abstract class VAction {
	public Task task;

	protected boolean isPickup;

	// This is the remaining capacity of the vehicle after taking this action.
	// It increases if the taskAction is a delivery action and it decreases if
	// otherwise.
	public int remainingVCapacity;

	public VAction(Task t, int remainingVCapacity) {
		super();
		task = t;
		this.remainingVCapacity = remainingVCapacity;
	}

	public abstract boolean updateCapacity(int initCapactiy);

	public abstract City getCity();

	public abstract Action getAction();

	@Override
	public abstract VAction clone();

	public abstract boolean isPickup();

	@Override
	public String toString() {
		return "va(" + (isPickup() ? "p" : "d") + ", " + task.id + ")";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isPickup ? 1231 : 1237);
		result = prime * result + (task == null ? 0 : task.hashCode());
		return result;
	}

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
		if (!(obj instanceof VAction)) {
			return false;
		}
		VAction other = (VAction) obj;
		if (isPickup != other.isPickup) {
			return false;
		}
		if (task == null) {
			if (other.task != null) {
				return false;
			}
		} else if (!task.equals(other.task)) {
			return false;
		}
		return true;
	}

}
package model;

import java.util.List;

import logist.topology.Topology.City;

public class State {
	private City current;
	private City destination;
	private List<AgentAction> listOfActions;

	/**
	 * @param current
	 * @param destination
	 * @param listOfActions
	 */
	public State(City current, City destination, List<AgentAction> listOfActions) {
		super();
		this.current = current;
		this.destination = destination;
		this.listOfActions = listOfActions;
	}

	/**
	 * @return the current
	 */
	public City getCurrent() {
		return current;
	}

	/**
	 * @return the destination
	 */
	public City getDestination() {
		return destination;
	}

	/**
	 * @return the listOfActions
	 */
	public List<AgentAction> getListOfActions() {
		return listOfActions;
	}

	@Override
	public String toString() {
		return "S(" + current + ", " + destination + ")";
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
		if (getClass() != obj.getClass()) {
			return false;
		}
		State other = (State) obj;
		if (current == null) {
			if (other.current != null) {
				return false;
			}
		} else if (!current.equals(other.current)) {
			return false;
		}
		if (destination == null) {
			if (other.destination != null) {
				return false;
			}
		} else if (!destination.equals(other.destination)) {
			return false;
		}
		return true;
	}

}

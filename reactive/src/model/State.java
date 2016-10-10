package model;

import java.util.List;

import logist.topology.Topology.City;

public class State {
	private City city;
	private List<AgentAction> listOfActions;

	/**
	 * Constructor using fields
	 *
	 * @param city
	 * @param listOfActions
	 */
	public State(City city, List<AgentAction> listOfActions) {
		super();
		this.city = city;
		this.listOfActions = listOfActions;
	}

	/**
	 * @return the city
	 */
	public City getCity() {
		return city;
	}

	/**
	 * @return the listOfActions
	 */
	public List<AgentAction> getListOfActions() {
		return listOfActions;
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
		if (city == null) {
			if (other.city != null) {
				return false;
			}
		} else if (!city.equals(other.city)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (AgentAction a : listOfActions) {
			sb.append("action: " + a.toString() + "\n");
		}
		sb.append("city: " + city.toString());
		return sb.toString();
	}

}

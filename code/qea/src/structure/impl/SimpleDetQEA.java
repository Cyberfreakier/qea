package structure.impl;


/**
 * This class represents a simple Quantified Event Automaton (QEA) with the
 * following characteristics:
 * <ul>
 * <li>There is at most one quantified variable
 * <li>The transitions in the function delta consist of a start state, an event
 * and an end state, no guards or assigns are considered
 * <li>The QEA is deterministic
 * </ul>
 * 
 * @author Helena Cuenca
 * @author Giles Reger
 */
public class SimpleDetQEA extends SimpleQEA {



	private int[][] delta;

	public SimpleDetQEA(int numStates, int numEvents, int initialState,
			Quantification quantification) {
		super(numStates,initialState,quantification);
		delta = new int[numStates + 1][numEvents + 1];
	}

	/**
	 * Adds a transition for the transition function delta of this QEA
	 * 
	 * @param startState
	 *            Start state for this transition
	 * @param event
	 *            Name of the event
	 * @param endState
	 *            End state for this transition
	 */
	public void addTransition(int startState, int event, int endState) {
		delta[startState][event] = endState;
	}

	/**
	 * Retrieves the end state for a given start state and an event, according
	 * to the transition function delta of this QEA
	 * 
	 * @param previousState
	 *            Start state
	 * @param event
	 *            Name of the event
	 * @return End state name, or 0 if the transition is not defined in the
	 *         function delta
	 */
	public int getNextState(int previousState, int event) {
		return delta[previousState][event];
	}


	/**
	 * Retrieves the alphabet of events A for this QEA
	 * 
	 * @return Array of integers with the names of the events
	 */
	public int[] getEventsAlphabet() {
		int[] a = new int[delta[0].length];
		for (int i = 0; i < a.length; i++) {
			a[i] = i + 1;
		}
		return a;
	}



	/**
	 * Checks if the QEA is deterministic
	 * 
	 * @return True if the QEA is deterministic, false otherwise
	 */
	public boolean isDeterministic() {
		return true;
	}

}
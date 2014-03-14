package monitoring.impl.monitors;

import java.util.IdentityHashMap;

import monitoring.impl.configs.NonDetConfig;
import structure.impl.QVar01_FVar_NonDet_QEA;
import structure.impl.Transition;
import structure.impl.Verdict;

/**
 * An incremental monitor for a non-simple non-deterministic generic QEA
 * 
 * @author Helena Cuenca
 * @author Giles Reger
 * @param <Q>
 */
public class Incr_QVar1_FVar_NonDet_QEAMonitor extends
		IncrementalNonSimpleQEAMonitor<QVar01_FVar_NonDet_QEA> {

	/**
	 * Maps the current values of the quantified variable to the
	 * non-deterministic configuration for each binding. The configuration
	 * contains the set of states and set of bindings for the free variables
	 */
	private IdentityHashMap<Object, NonDetConfig> bindings;

	/**
	 * For each event stores a <code>true</code> indicating it has at least one
	 * signature that only contains free variables as parameters,
	 * <code>false</code> otherwise
	 */
	private boolean[] onlyFVarSignature;

	/**
	 * For each event stores the number of different positions of the quantified
	 * variable in the event signatures
	 */
	private int[] numQVarPositions;

	/**
	 * For each event stores an array showing (with a value <code>true</code>)
	 * the possible different positions of the quantified variables in the
	 * signatures
	 */
	private boolean[][] eventsMasks;

	/**
	 * Creates a <code>IncrementalNonSimpleNonDetGenQEAMonitor</code> to monitor
	 * the specified QEA property
	 * 
	 * @param qea
	 *            QEA property
	 */
	public Incr_QVar1_FVar_NonDet_QEAMonitor(QVar01_FVar_NonDet_QEA qea) {
		super(qea);
		bindings = new IdentityHashMap<>();
		buildEventsIndices();
	}

	/**
	 * Initialise the arrays <code>onlyFVarSignature</code>,
	 * <code>numQVarPositions</code> and <code>eventsMasks</code> according to
	 * the events defined in the QEA of this monitor
	 */
	private void buildEventsIndices() {

		int numEvents = qea.getEventsAlphabet().length;
		int[] states = qea.getStates();
		int[] eventsAlphabet = qea.getEventsAlphabet();

		onlyFVarSignature = new boolean[numEvents + 1];
		numQVarPositions = new int[numEvents + 1];
		eventsMasks = new boolean[numEvents + 1][];

		// Iterate over all start states and event names
		for (int state : states) {
			for (int eventName : eventsAlphabet) {

				// Get transitions for the specified start state and event
				Transition[] transitions = qea.getTransitions(state, eventName);
				if (transitions != null) {

					// Iterate over each transition
					for (Transition transition : transitions) {

						Transition transitionImpl = transition;
						boolean onlyFreeVar = true;

						// If needed, initialise array of mask for the event
						if (eventsMasks[eventName] == null) {
							eventsMasks[eventName] = new boolean[transitionImpl
									.getVariableNames().length];
						}

						// Iterate over the variables names of this signature
						int[] varNames = transitionImpl.getVariableNames();
						for (int k = 0; k < varNames.length; k++) {
							if (varNames[k] < 0) { // Quantified variable
								onlyFreeVar = false;
								if (!eventsMasks[eventName][k]) {
									eventsMasks[eventName][k] = true;
									numQVarPositions[eventName]++;
								}
								break; // Exit the loop
							}
						}

						if (onlyFreeVar) {
							// This signature only contains free variables
							onlyFVarSignature[eventName] = true;
						}
					}
				}
			}
		}
	}

	@Override
	public Verdict step(int eventName, Object[] args) {

		boolean eventProcessedForAllExistingBindings = false;
		if (onlyFVarSignature[eventName]) {

			// If there is a signature for the event with only free variables,
			// apply event to all existing bindings
			for (Object binding : bindings.keySet()) {
				stepNoVerdict(eventName, args, binding);
			}
			eventProcessedForAllExistingBindings = true;
		}

		if (numQVarPositions[eventName] == 1) { // Only one value for the QVar

			Object qVarBinding = getFirstQVarBinding(eventsMasks[eventName],
					args);
			if (!eventProcessedForAllExistingBindings
					|| bindings.get(qVarBinding) == null) {
				stepNoVerdict(eventName, args, qVarBinding);
			}
		} else if (numQVarPositions[eventName] > 1) { // Possibly multiple
														// values for the QVar

			Object[] qVarBindings = getUniqueQVarBindings(
					eventsMasks[eventName], args, numQVarPositions[eventName]);
			for (Object qVarBinding : qVarBindings) {
				if (!eventProcessedForAllExistingBindings
						|| bindings.get(qVarBinding) == null) {
					stepNoVerdict(eventName, args, qVarBinding);
				}
			}
		}

		// According to the quantification of the variable, return verdict
		if (qea.isQuantificationUniversal() && allBindingsInFinalState()
				|| !qea.isQuantificationUniversal()
				&& existsOneBindingInFinalState()) {
			return Verdict.WEAK_SUCCESS;
		}
		return Verdict.WEAK_FAILURE;
	}

	@Override
	public Verdict step(int eventName) {
		Verdict finalVerdict = null;
		for (Object binding : bindings.keySet()) {
			finalVerdict = step(eventName, binding);
		}
		return finalVerdict;
	}

	/**
	 * Processes the specified event with the specified arguments for the
	 * specified binding (quantified variable value) without producing a verdict
	 * 
	 * @param eventName
	 *            Name of the event
	 * @param args
	 *            Array of arguments
	 * @param qVarValue
	 *            Quantified variable value
	 */
	private void stepNoVerdict(int eventName, Object[] args, Object qVarValue) {

		boolean existingBinding = false;
		boolean startConfigFinal = false;
		NonDetConfig config;

		// Determine if the value received corresponds to an existing binding
		if (bindings.containsKey(qVarValue)) { // Existing quantified
												// variable binding

			// Get current configuration for the binding
			config = bindings.get(qVarValue);

			// Assign flags for counters update
			existingBinding = true;
			startConfigFinal = qea.containsFinalState(config);

		} else { // New quantified variable binding

			// Create configuration for the new binding
			config = new NonDetConfig(qea.getInitialState(), qea.newBinding());
		}

		// Compute next configuration
		config = qea.getNextConfig(config, eventName, args);

		// Flag needed to update counters later
		boolean endConfigFinal = qea.containsFinalState(config);

		// Update/add configuration for the binding
		bindings.put(qVarValue, config);

		// If applicable, update counters
		if (existingBinding) {
			if (startConfigFinal && !endConfigFinal) {
				bindingsInNonFinalStateCount++;
				bindingsInFinalStateCount--;
			} else if (!startConfigFinal && endConfigFinal) {
				bindingsInNonFinalStateCount--;
				bindingsInFinalStateCount++;
			}
		} else {
			if (endConfigFinal) {
				bindingsInFinalStateCount++;
			} else {
				bindingsInNonFinalStateCount++;
			}
		}
	}

	@Override
	public String getStatus() {
		String ret = "Map:\n";
		for (IdentityHashMap.Entry<Object, NonDetConfig> entry : bindings
				.entrySet()) {
			ret += entry.getKey() + "\t->\t" + entry.getValue() + "\n";
		}
		return ret;
	}

}
package monitoring.impl.monitors;

import java.util.HashSet;
import java.util.IdentityHashMap;

import monitoring.impl.GarbageMode;
import monitoring.impl.RestartMode;
import monitoring.impl.configs.DetConfig;
import structure.impl.other.Verdict;
import structure.impl.qeas.QVar1_FVar_Det_FixedQVar_QEA;

/**
 * An incremental monitor for a non-simple deterministic QEA
 * 
 * @author Helena Cuenca
 * @author Giles Reger
 */
public class Incr_QVar1_FVar_Det_FixedQVar_QEAMonitor extends
		Abstr_Incr_QVar1_FVar_QEAMonitor<QVar1_FVar_Det_FixedQVar_QEA> {

	/**
	 * Maps the current values of the quantified variable to the deterministic
	 * configuration for each binding. The configuration contains the state and
	 * the bindings for the free variables
	 */
	private final IdentityHashMap<Object, DetConfig> bindings;
	private final HashSet<Object> strong;

	/**
	 * Creates an IncrementalNonSimpleDetQEAMonitor for the specified QEA
	 * 
	 * @param qea
	 *            QEA
	 */
	public Incr_QVar1_FVar_Det_FixedQVar_QEAMonitor(RestartMode restart, GarbageMode garbage, 
			QVar1_FVar_Det_FixedQVar_QEA qea) {
		super(restart,garbage,qea);
		bindings = new IdentityHashMap<>();
		strong = new HashSet<Object>();
	}

	@Override
	public Verdict step(int eventName, Object[] args) {

		if(saved!=null){
			if(!restart()) return saved;
		}		
		
		boolean existingBinding = false;
		boolean startConfigFinal = false;
		DetConfig config;

		// Obtain the value for the quantified variable
		Object qVarValue = args[0];

		// Determine if the value received corresponds to an existing binding
		if (bindings.containsKey(qVarValue)) { // Existing quantified
												// variable binding

			// Get current configuration for the binding
			config = bindings.get(qVarValue);

			// Assign flags for counters update
			existingBinding = true;
			startConfigFinal = qea.isStateFinal(config.getState());

		} else { // New quantified variable binding

			// Create configuration for the new binding
			config = new DetConfig(qea.getInitialState(), qea.newBinding());
		}

		// Compute next configuration
		config = qea.getNextConfig(config, eventName, args, qVarValue);

		// Update/add configuration for the binding
		bindings.put(qVarValue, config);

		// Flag needed to update counters later
		boolean endConfigFinal = qea.isStateFinal(config.getState());

		// Determine if there is a final/non-final strong state
		if (qea.isStateStrong(config.getState())) {
			strong.add(qVarValue);
			if (endConfigFinal) {
				finalStrongState = true;
			} else {
				nonFinalStrongState = true;
			}
		}

		// Update counters
		updateCounters(existingBinding, startConfigFinal, endConfigFinal);

		return computeVerdict(false);
	}

	private static final Object[] dummyArgs = new Object[]{};
	@Override
	public Verdict step(int eventName) {
		Verdict finalVerdict = null;
		for (Object binding : bindings.keySet()) {
			throw new RuntimeException("Not implemented properly");
			//finalVerdict = step(eventName, dummyArgs);
		}
		return finalVerdict;
	}

	@Override
	public String toString() {
		String ret = "Map:\n";
		for (IdentityHashMap.Entry<Object, DetConfig> entry : bindings
				.entrySet()) {
			ret += entry.getKey() + "\t->\t" + entry.getValue() + "\n";
		}
		return ret;
	}

	@Override
	public String getStatus() {
		String ret = "Map:\n";
		for (IdentityHashMap.Entry<Object, DetConfig> entry : bindings
				.entrySet()) {
			ret += entry.getKey() + "\t->\t" + entry.getValue() + "\n";
		}
		return ret;
	}

	@Override
	protected int removeStrongBindings() {
		int removed = strong.size();
		for(Object o : strong){
			bindings.remove(o);
		}
		strong.clear();
		return removed;
	}

	@Override
	protected int rollbackStrongBindings() {
		int rolled = strong.size();
		for(Object o : strong){
			DetConfig c = bindings.get(o);
			c.setState(qea.getInitialState());
			c.getBinding().setEmpty();
		}
		strong.clear();
		return rolled;
	}
}

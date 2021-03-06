package qea.structure.impl.qeas;

import qea.monitoring.impl.configs.NonDetConfig;
import qea.structure.impl.other.FBindingImpl;
import qea.structure.impl.other.Quantification;
import qea.structure.impl.other.Transition;
import qea.structure.intf.Binding;
import qea.structure.intf.Guard;
import qea.structure.intf.QEA_nondet_free;
import qea.util.ArrayUtil;

/**
 * This class represents a Quantified Event Automaton (QEA) with the following
 * characteristics:
 * <ul>
 * <li>There is at most one quantified variable
 * <li>It can contain any number of free variables
 * <li>The transitions in the function delta consist of a start state, an event
 * and an end state. Optionally, it can be associated to a guard and/or an
 * assignment
 * <li>The QEA is non-deterministic
 * </ul>
 * 
 * @author Helena Cuenca
 * @author Giles Reger
 */
public class QVar01_FVar_NonDet_QEA extends Abstr_QVar01_FVar_QEA implements
		QEA_nondet_free {

	private final QEAType qeaType = QEAType.QVAR01_FVAR_NONDET_QEA;

	/**
	 * Transition function delta for this QEA. For a given Transition in the
	 * array, the first index corresponds to the start state and the second
	 * index corresponds to the event name. For a given start state and event
	 * name, there may be an array of transitions (non-deterministic)
	 */
	private Transition[][][] delta;

	/**
	 * Creates a <code>QVar01_FVar_NonDet_QEA</code> for the specified number of
	 * states, number of events, initial state, quantification type and number
	 * of free variables
	 * 
	 * @param numStates
	 *            Number of states
	 * @param numEvents
	 *            Number of events
	 * @param initialState
	 *            Initial state
	 * @param quantification
	 *            Quantification type
	 * @param freeVariablesCount
	 *            Number of free variables
	 */
	public QVar01_FVar_NonDet_QEA(int numStates, int numEvents,
			int initialState, Quantification quantification,
			int freeVariablesCount) {
		super(numStates, initialState, quantification, freeVariablesCount);
		delta = new Transition[numStates + 1][numEvents + 1][];
	}

	/**
	 * Adds a transition to the transition function delta of this QEA
	 * 
	 * @param startState
	 *            Start state for the transition
	 * @param event
	 *            Name of the event
	 * @param transitions
	 *            Object containing the rest of the information of the
	 *            transition: Parameters for the event, guard, assignment and
	 *            end state
	 */
	public void addTransition(int startState, int event, Transition transition) {
		if (delta[startState][event] == null) {
			delta[startState][event] = new Transition[] { transition };
		} else {
			// Resize transitions array and add the new transition
			int currentSize = delta[startState][event].length;
			delta[startState][event] = ArrayUtil.resize(
					delta[startState][event], currentSize + 1);
			delta[startState][event][currentSize] = transition;
		}
	}

	/**
	 * Adds a set of transitions to the transition function delta of this QEA
	 * 
	 * @param startState
	 *            Start state for the transition
	 * @param event
	 *            Name of the event
	 * @param transitions
	 *            Array of objects containing the rest of the information of the
	 *            transitions: Parameters for the event, guard, assignment and
	 *            end state
	 */
	public void addTransitions(int startState, int event,
			Transition[] transitions) {
		if (delta[startState][event] == null) {
			delta[startState][event] = transitions;
		} else {
			// Add new transitions
			delta[startState][event] = ArrayUtil.concat(
					delta[startState][event], transitions);
		}
	}

	/**
	 * Computes the next configuration for a given start configuration, event
	 * and arguments, according to the transition function delta of this QEA
	 * 
	 * @param config
	 *            Start configuration containing the set of start states and the
	 *            set of bindings
	 * @param event
	 *            Name of the event
	 * @param args
	 *            Arguments for the event
	 * @return End configuration containing the set of end state and the set of
	 *         bindings after the transition
	 */
	public NonDetConfig getNextConfig(NonDetConfig config, int event,
			Object[] args, Object qVarValue, boolean isQVarValue) {

		// TODO This method is very similar to getNextConfig in
		// QVar01_FVar_NonDet_FixedQVar_QEA
		
		if (config.getStates().length == 1) { // Only one start state

			Transition[] transitions = delta[config.getStates()[0]][event];

			// If the event is not defined for the unique start state, return
			// the failing state with an empty binding
			if (transitions == null) {
				if (!skipStates[config.getStates()[0]]) {
					config.setState(0, 0);
					config.getBindings()[0].setEmpty();
				}
				return config;
			}

			// Check number of arguments vs. number of parameters of the event
			// for the first transition only
			checkArgParamLength(args.length,
					transitions[0].getVariableNames().length);

			// 1 start state - 1 transition
			if (transitions.length == 1) {
				return getNextConfig1StartState1Transition(config, args,
						transitions[0], qVarValue, isQVarValue);
			}

			// 1 start state - Multiple transitions
			return getNextConfig1StartStateMultTransitions(config, args,
					transitions, qVarValue, isQVarValue);
		}

		// Multiple start states
		return getNextConfigMultStartStatesMultTransitions(config, event, args,
				qVarValue, isQVarValue);

	}

	private NonDetConfig getNextConfig1StartState1Transition(
			NonDetConfig config, Object[] args, Transition transition,
			Object qVarValue, boolean isQVarValue) {
		
		if(!isQVarValue){
			boolean hasq = false; 
			int[] ps = transition.getVariableNames();
			for(int p : ps){
				if(p<0) hasq=true;
			}
			if(hasq) return config;
		}		
		else if(!qVarMatchesBinding(qVarValue,args,transition)){
			return config;
		}
		
		// Update binding for free variables
		Binding binding = config.getBindings()[0];
		updateBinding(binding, args, transition);

		// If there is a guard and is not satisfied, rollback the binding and
		// return the failing state (if not skip)
		if (transition.getGuard() != null) {
			Guard guard = transition.getGuard();
			if (isQVarValue && !guard.check(binding, -1, qVarValue)
					|| !isQVarValue && !guard.check(binding)) {
				if (!skipStates[config.getStates()[0]]) {
					config.setState(0, 0); // Failing state
				}
				return config;
			}
		}

		// If there is an assignment, execute it
		if (transition.getAssignment() != null) {
			boolean use_q = false;
			int[] vused = transition.getAssignment().vars();
			for(int i=0;i<vused.length;i++) use_q |= vused[i]<0;
			if(use_q && !isQVarValue) throw new RuntimeException("qvar exepcted");
			if(use_q){
				  config.setBinding(0,
							transition.getAssignment().apply(binding, false,-1,qVarValue));				
			}
			else{
			  config.setBinding(0,
					transition.getAssignment().apply(binding, false));
			}
		}

		// Set the end state
		config.setState(0, transition.getEndState());

		return config;
	}

	private NonDetConfig getNextConfig1StartStateMultTransitions(
			NonDetConfig config, Object[] args, Transition[] transitions,
			Object qVarValue, boolean isQVarValue) {
				
		// Create as many states and bindings as there are transitions
		int[] endStates = new int[transitions.length];
		FBindingImpl[] bindings = new FBindingImpl[transitions.length];

		// Initialise end states count
		int endStatesCount = 0;

		// Iterate over the transitions
		for (Transition transition : transitions) {
			
			if(!isQVarValue){
				boolean hasq = false; 
				int[] ps = transition.getVariableNames();
				for(int p : ps){
					if(p<0) hasq=true;
				}
				if(hasq) continue;
			}
			else if(!qVarMatchesBinding(qVarValue,args,transition)){
				continue;
			}			
			
			//Check if state is a strong failure state
			// TODO : add an option to keep them
			int end_state = transition.getEndState();
					
				// Copy the binding of the start state
				FBindingImpl binding = (FBindingImpl) config.getBindings()[0]
						.copy();

				// Update binding for free variables
				updateBinding(binding, args, transition);

				// If there is a guard, check it is satisfied
				if (transition.getGuard() == null || 
						(isQVarValue && transition.getGuard().check(binding, -1, qVarValue))
						|| 
						(!isQVarValue && transition.getGuard().check(binding))) {

					
					// If there is an assignment, execute it
					if (transition.getAssignment() != null) {
						int[] avars = transition.getAssignment().vars();
						boolean useq = false;
						for(int i=0;i<avars.length;i++) useq |= avars[i]<0;
						if(useq){
							binding = (FBindingImpl) transition.getAssignment()
									.apply(binding, true, -1, qVarValue);
						}else{
							binding = (FBindingImpl) transition.getAssignment()
								.apply(binding, true);
						}
					}

					// Copy end state and binding in the arrays
					endStates[endStatesCount] = transition.getEndState();
					bindings[endStatesCount] = binding;

					endStatesCount++;
				}else{
				//	System.out.println("Guard fails");
				}

		}

		if (endStatesCount == 0) { // All end states are failing

			// Set failing state if not skip
			// leave binding as it is
			if (!skipStates[config.getStates()[0]]) {
				config.setState(0, 0);
			}
			return config;
		}
		
		// TODO - check for repetitions?

		config.setStates(ArrayUtil.resize(endStates, endStatesCount));
		config.setBindings(ArrayUtil.resize(bindings, endStatesCount));

		return config;
	}

	private NonDetConfig getNextConfigMultStartStatesMultTransitions(
			NonDetConfig config, int event, Object[] args, Object qVarValue,
			boolean isQVarValue) {

		
		int[] startStates = config.getStates();
		int maxEndStates = 0;
		boolean argsNumberChecked = false;
		Transition[][] transitions = new Transition[startStates.length][];

		// Compute maximum number of end states
		for (int i = 0; i < startStates.length; i++) {

			transitions[i] = delta[startStates[i]][event];
			if (transitions[i] != null) {
				maxEndStates += transitions[i].length;

				// Check number of arguments vs. number of parameters of the
				// event for one transition only
				if (!argsNumberChecked) {
					checkArgParamLength(args.length,
							transitions[i][0].getVariableNames().length);
					argsNumberChecked = true;
				}
			} else if (skipStates[startStates[i]]) {
				maxEndStates++;
			}
		}

		// If the event is not defined for any of the current start states,
		// return the failing state with an empty binding
		if (maxEndStates == 0) {
			config.setStates(new int[] { 0 });
			config.setBindings(new Binding[] { newBinding() });
			return config;
		}

		// Create as many states and bindings as there are end states
		int[] endStates = new int[maxEndStates];
		FBindingImpl[] bindings = new FBindingImpl[maxEndStates];

		// Initialise end states count
		int endStatesCount = 0;

		// Iterate over the transitions
		for (int i = 0; i < transitions.length; i++) {
			if (transitions[i] != null) {
				boolean taken_transition = false;
				for (Transition transition : transitions[i]) {
					
					// Check if strong failure state
					// TODO: add option to not remove these
					int end_state = transition.getEndState();
					if(!(strongStates[end_state] && !finalStates[end_state])){
					
					// Check the transition matches the value of the QVar
					if (!isQVarValue
							|| qVarMatchesBinding(qVarValue, args, transition)) {
						

						// Copy the initial binding
						FBindingImpl binding = (FBindingImpl) config
								.getBindings()[i].copy();

						// TODO It's updating bindings even when there are no
						// free variables
						// Comment: Later consider adding flag to Transition to
						// filter this case
						// - do this in the presence of benchmarking to check
						// performance difference?

						// Update binding for free variables
						updateBinding(binding, args, transition);

						// If there is a guard, check it is satisfied
						if (transition.getGuard() == null
								|| isQVarValue
								&& transition.getGuard().check(binding, -1,
										qVarValue) || !isQVarValue
								&& transition.getGuard().check(binding)) {

							// If there is an assignment, execute it
							if (transition.getAssignment() != null) {
								binding = (FBindingImpl) transition
										.getAssignment().apply(binding, true);
							}

							// Copy end state and binding in the arrays
							endStates[endStatesCount] = transition
									.getEndState();
							bindings[endStatesCount] = binding;

							endStatesCount++;
							taken_transition = true;
						}
					}
					}else{
						taken_transition = true;// transition was taken to failure
					}
				}
				if (!taken_transition) {
					if (skipStates[startStates[i]]) {
						endStates[endStatesCount] = startStates[i];
						bindings[endStatesCount] = (FBindingImpl) config
								.getBindings()[i].copy();
						;
						endStatesCount++;
					}
				}
			} else {
				if (skipStates[startStates[i]]) {
					endStates[endStatesCount] = startStates[i];
					bindings[endStatesCount] = (FBindingImpl) config
							.getBindings()[i].copy();
					;
					endStatesCount++;
				}
			}
		}

		if (endStatesCount == 0) { // All end states are failing states
			config.setStates(new int[] { 0 });
			config.setBindings(new Binding[] { newBinding() });
			return config;

		}

		config.setStates(ArrayUtil.resize(endStates, endStatesCount));
		config.setBindings(ArrayUtil.resize(bindings, endStatesCount));

		return config;
	}

	/**
	 * Determines if the set of states in the specified configuration contains
	 * at least one final state
	 * 
	 * @param config
	 *            Configuration encapsulating the set of states to be checked
	 * @return <code>true</code> if the set of states in the specified
	 *         configuration contains at least one final state;
	 *         <code>false</code> otherwise
	 */
	public boolean containsFinalState(NonDetConfig config) {
		for (int state : config.getStates()) {
			if (isStateFinal(state)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int[] getEventsAlphabet() {
		int[] a = new int[delta[0].length - 1];
		for (int i = 0; i < a.length; i++) {
			a[i] = i + 1;
		}
		return a;
	}

	@Override
	public boolean isDeterministic() {
		return false;
	}

	/**
	 * Returns the transitions for the specified start state and event name,
	 * according to the transition function delta of this QEA
	 * 
	 * @param startState
	 *            Start state
	 * @param eventName
	 *            Event name
	 * @return Transitions defined for the specified state and event name or
	 *         <code>null</code> if no transition is defined
	 */
	public Transition[] getTransitions(int startState, int eventName) {
		return delta[startState][eventName];
	}

	@Override
	public QEAType getQEAType() {
		return qeaType;
	}

	@Override
	public Transition[][][] getDelta() {
		return delta;
	}
}

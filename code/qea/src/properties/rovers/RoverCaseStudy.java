package properties.rovers;

import static structure.impl.other.Quantification.FORALL;
import structure.intf.Guard;
import structure.intf.QEA;
import creation.QEABuilder;

/*
 * External
 * - ExactlyOneSuccess
 * - IncreasingCommand 
 * - NestedCommand
 * - AcknowledgeCommand
 * - ExistsSatellite
 * - ExistsLeader
 * - MessageHashCorrect
 * 
 * Internal
 * - GrantCancel
 * - ResourceLifecycle
 * - ReleaseResource
 * - RespectConflicts
 * - RespectPriorities 
 * 
 */

public class RoverCaseStudy {

	/*
	 * 
	 */
	public static QEA makeGrantCancelSingleSwitch() {

		QEABuilder q = new QEABuilder("GrantCancelSingleSwitch");

		int GRANT = 1;
		int CANCEL = 2;
		int T1 = 1;
		int T2 = 2;
		int R = -1;

		q.addQuantification(FORALL, R);

		q.addTransition(1, GRANT, new int[] { R, T1 }, 2);
		q.addTransition(2, GRANT, new int[] { R, T2 }, 3);

		q.startTransition(2);
		q.eventName(CANCEL);
		q.addVarArg(R);
		q.addVarArg(T2);
		q.addGuard(Guard.isEqual(T1, T2));
		q.endTransition(1);

		q.addFinalStates(1, 2);

		QEA qea = q.make();

		qea.record_event_name("grant", 1);
		qea.record_event_name("cancel", 2);

		return qea;
	}

	/*
	 * 
	 */
	public static QEA makeGrantCancelSingle() {

		QEABuilder q = new QEABuilder("GrantCancelSingle");

		int GRANT = 1;
		int CANCEL = 2;
		int T1 = 1;
		int T2 = 2;
		int R = -1;

		q.addQuantification(FORALL, R);

		q.addTransition(1, GRANT, new int[] { T1, R }, 2);
		q.addTransition(2, GRANT, new int[] { T2, R }, 3);

		q.startTransition(2);
		q.eventName(CANCEL);
		q.addVarArg(T2);
		q.addVarArg(R);
		q.addGuard(Guard.isEqual(T1, T2));
		q.endTransition(1);

		q.addFinalStates(1, 2);

		QEA qea = q.make();

		qea.record_event_name("grant", 1);
		qea.record_event_name("cancel", 2);

		return qea;
	}

	/*
	 * 
	 */
	public static QEA makeGrantCancelDouble() {

		QEABuilder q = new QEABuilder("GrantCancelDouble");

		int GRANT = 1;
		int CANCEL = 2;
		int R = -1;
		int T = -2;
		int TT = 1;

		q.addQuantification(FORALL, R);
		q.addQuantification(FORALL, T);

		q.addTransition(1, GRANT, new int[] { T, R }, 2);
		q.addTransition(2, GRANT, new int[] { TT, R }, 3);
		q.addTransition(2, CANCEL, new int[] { T, R }, 1);

		q.addFinalStates(1, 2);

		QEA qea = q.make();

		qea.record_event_name("grant", 1);
		qea.record_event_name("cancel", 2);

		return qea;
	}

	/*
	 * 
	 */
	public static QEA makeResourceLifecycle() {

		QEABuilder q = new QEABuilder("ResourceLifecycle");

		int REQUEST = 1;
		int GRANT = 2;
		int DENY = 3;
		int RESCIND = 4;
		int CANCEL = 5;
		int R = -1;

		q.addQuantification(FORALL, R);

		int[] r = new int[] { R };
		q.addTransition(1, REQUEST, r, 2);
		q.addTransition(2, DENY, r, 1);
		q.addTransition(2, GRANT, r, 3);
		q.addTransition(3, RESCIND, r, 3);
		q.addTransition(3, CANCEL, r, 1);

		q.addFinalStates(1, 2);

		QEA qea = q.make();

		qea.record_event_name("request", 1);
		qea.record_event_name("grant", 2);
		qea.record_event_name("deny", 3);
		qea.record_event_name("rescind", 4);
		qea.record_event_name("cancel", 5);

		return qea;
	}

	/*
	 * 
	 */
	public static QEA makeReleaseResource() {

		QEABuilder q = new QEABuilder("ReleaseResource");

		int SCHEDULE = 1;
		int GRANT = 2;
		int CANCEL = 3;
		int FINISH = 4;

		int T = -1;
		int C = -2;
		int R = -3;

		q.addQuantification(FORALL, T);
		q.addQuantification(FORALL, C);
		q.addQuantification(FORALL, R);

		q.addTransition(1, SCHEDULE, new int[] { T, C }, 2);
		q.addTransition(2, GRANT, new int[] { T, R }, 3);
		q.addTransition(3, CANCEL, new int[] { T, R }, 2);
		q.addTransition(3, FINISH, new int[] { C }, 4);

		q.addFinalStates(1, 2);
		q.setSkipStates(1, 2, 3, 4);

		QEA qea = q.make();

		qea.record_event_name("schedule", 1);
		qea.record_event_name("grant", 2);
		qea.record_event_name("cancel", 2);
		qea.record_event_name("finish", 2);

		return qea;
	}
}

package com.sleepycat.je.dbi; 
import com.sleepycat.je.DatabaseException; 
import de.ovgu.cide.jakutil.*; 
 
class  DbEnvState {
	 private static final boolean DEBUG=false;

	 private String name;

	 public static final DbEnvState INIT=new DbEnvState("initialized");

	 public static final DbEnvState OPEN=new DbEnvState("open");

	 public static final DbEnvState CLOSED=new DbEnvState("closed");

	 public static final DbEnvState INVALID=new DbEnvState("invalid");

	 public static final DbEnvState[] VALID_FOR_OPEN={INIT,CLOSED};

	 public static final DbEnvState[] VALID_FOR_CLOSE={INIT,OPEN,INVALID};

	 public static final DbEnvState[] VALID_FOR_REMOVE={INIT,CLOSED};

	 DbEnvState( String name){ this.name=name; }

	 public String toString__wrappee__base(){ return name; }

	 public String toString(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	toString__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 void checkState__wrappee__base( DbEnvState[] validPrevStates, DbEnvState newState) throws DatabaseException { if (DEBUG) { System.out.println("newState = " + newState + " currentState = "+ name); } boolean transitionOk=false; for (int i=0; i < validPrevStates.length; i++) { if (this == validPrevStates[i]) { transitionOk=true; break; } } if (!transitionOk) { throw new DatabaseException("Can't go from environment state " + toString() + " to "+ newState.toString()); } }

	 void checkState( DbEnvState[] validPrevStates, DbEnvState newState) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	checkState__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	private Tracer t = new Tracer();

	public Tracer getTracer(){return t;}


}

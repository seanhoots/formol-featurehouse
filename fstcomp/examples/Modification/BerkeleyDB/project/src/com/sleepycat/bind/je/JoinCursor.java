package com.sleepycat.je; 
import java.util.Arrays; 
import java.util.Comparator; 
import java.util.logging.Level; 
import com.sleepycat.je.dbi.GetMode; 
import com.sleepycat.je.dbi.CursorImpl.SearchMode; 
import com.sleepycat.je.txn.Locker; 
import de.ovgu.cide.jakutil.*; 
public  class  JoinCursor {
	 private JoinConfig config;

	 private Database priDb;

	 private Cursor priCursor;

	 private Cursor[] secCursors;

	 private DatabaseEntry[] cursorScratchEntries;

	 private DatabaseEntry scratchEntry;

	 JoinCursor( Locker locker, Database primaryDb, final Cursor[] cursors, JoinConfig configParam) throws DatabaseException { priDb=primaryDb; config=(configParam != null) ? configParam.cloneConfig() : JoinConfig.DEFAULT; scratchEntry=new DatabaseEntry(); cursorScratchEntries=new DatabaseEntry[cursors.length]; Cursor[] sortedCursors=new Cursor[cursors.length]; System.arraycopy(cursors,0,sortedCursors,0,cursors.length); if (!config.getNoSort()) { final int[] counts=new int[cursors.length]; for (int i=0; i < cursors.length; i+=1) { counts[i]=cursors[i].countInternal(LockMode.READ_UNCOMMITTED); assert counts[i] >= 0; } Arrays.sort(sortedCursors,new Comparator(){ public int compare( Object o1, Object o2){ int count1=-1; int count2=-1; for (int i=0; i < cursors.length && (count1 < 0 || count2 < 0); i+=1) { if (cursors[i] == o1) { count1=counts[i]; } else if (cursors[i] == o2) { count2=counts[i]; } } assert count1 >= 0 && count2 >= 0; return (count1 - count2); } }
); } try { priCursor=new Cursor(priDb,locker,null); secCursors=new Cursor[cursors.length]; for (int i=0; i < cursors.length; i+=1) { secCursors[i]=new Cursor(sortedCursors[i],true); } } catch ( DatabaseException e) { close(e); } }

	 public void close__wrappee__base() throws DatabaseException { if (priCursor == null) { throw new DatabaseException("Already closed"); } close(null); }

	 public void close() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	close__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void close__wrappee__base( DatabaseException firstException) throws DatabaseException { if (priCursor != null) { try { priCursor.close(); } catch ( DatabaseException e) { if (firstException == null) { firstException=e; } } priCursor=null; } for (int i=0; i < secCursors.length; i+=1) { if (secCursors[i] != null) { try { secCursors[i].close(); } catch ( DatabaseException e) { if (firstException == null) { firstException=e; } } secCursors[i]=null; } } if (firstException != null) { throw firstException; } }

	 private void close( DatabaseException firstException) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	close__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 Cursor\[\] getSortedCursors__wrappee__base(){ return secCursors; }

	 Cursor[] getSortedCursors(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getSortedCursors__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public Database getDatabase__wrappee__base(){ return priDb; }

	 public Database getDatabase(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getDatabase__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public JoinConfig getConfig__wrappee__base(){ return config.cloneConfig(); }

	 public JoinConfig getConfig(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getConfig__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public OperationStatus getNext__wrappee__base( DatabaseEntry key, LockMode lockMode) throws DatabaseException { priCursor.checkEnv(); DatabaseUtil.checkForNullDbt(key,"key",false); this.hook62(lockMode); return retrieveNext(key,null,lockMode); }

	 public OperationStatus getNext( DatabaseEntry key, LockMode lockMode) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getNext__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public OperationStatus getNext__wrappee__base( DatabaseEntry key, DatabaseEntry data, LockMode lockMode) throws DatabaseException { priCursor.checkEnv(); DatabaseUtil.checkForNullDbt(key,"key",false); DatabaseUtil.checkForNullDbt(data,"data",false); this.hook63(lockMode); return retrieveNext(key,data,lockMode); }

	 public OperationStatus getNext( DatabaseEntry key, DatabaseEntry data, LockMode lockMode) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getNext__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private OperationStatus retrieveNext__wrappee__base( DatabaseEntry keyParam, DatabaseEntry dataParam, LockMode lockMode) throws DatabaseException { outerLoop: while (true) { Cursor secCursor=secCursors[0]; DatabaseEntry candidateKey=cursorScratchEntries[0]; OperationStatus status; if (candidateKey == null) { candidateKey=new DatabaseEntry(); cursorScratchEntries[0]=candidateKey; status=secCursor.getCurrentInternal(scratchEntry,candidateKey,lockMode); } else { status=secCursor.retrieveNext(scratchEntry,candidateKey,lockMode,GetMode.NEXT_DUP); } if (status != OperationStatus.SUCCESS) { return status; } for (int i=1; i < secCursors.length; i+=1) { secCursor=secCursors[i]; DatabaseEntry secKey=cursorScratchEntries[i]; if (secKey == null) { secKey=new DatabaseEntry(); cursorScratchEntries[i]=secKey; status=secCursor.getCurrentInternal(secKey,scratchEntry,lockMode); assert status == OperationStatus.SUCCESS; } scratchEntry.setData(secKey.getData(),secKey.getOffset(),secKey.getSize()); status=secCursor.search(scratchEntry,candidateKey,lockMode,SearchMode.BOTH); if (status != OperationStatus.SUCCESS) { continue outerLoop; } } if (dataParam != null) { status=priCursor.search(candidateKey,dataParam,lockMode,SearchMode.SET); if (status != OperationStatus.SUCCESS) { throw new DatabaseException("Secondary corrupt"); } } keyParam.setData(candidateKey.getData(),candidateKey.getOffset(),candidateKey.getSize()); return OperationStatus.SUCCESS; } }

	 private OperationStatus retrieveNext( DatabaseEntry keyParam, DatabaseEntry dataParam, LockMode lockMode) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	retrieveNext__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook62__wrappee__base( LockMode lockMode) throws DatabaseException { }

	 protected void hook62( LockMode lockMode) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook62__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook63__wrappee__base( LockMode lockMode) throws DatabaseException { }

	 protected void hook63( LockMode lockMode) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook63__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	private Tracer t = new Tracer();

	public Tracer getTracer(){return t;}


}

package com.sleepycat.je.txn; 
import java.nio.ByteBuffer; 
import java.util.HashMap; 
import java.util.HashSet; 
import java.util.Iterator; 
import java.util.Map; 
import java.util.Set; 
import java.util.logging.Level; 
import java.util.logging.Logger; 
import javax.transaction.xa.XAResource; 
import javax.transaction.xa.Xid; 
import com.sleepycat.je.Database; 
import com.sleepycat.je.DatabaseException; 
import com.sleepycat.je.DbInternal; 
import com.sleepycat.je.LockStats; 
import com.sleepycat.je.RunRecoveryException; 
import com.sleepycat.je.TransactionConfig; 
import com.sleepycat.je.dbi.CursorImpl; 
import com.sleepycat.je.dbi.DatabaseId; 
import com.sleepycat.je.dbi.DatabaseImpl; 
import com.sleepycat.je.dbi.EnvironmentImpl; 
import com.sleepycat.je.dbi.MemoryBudget; 
import com.sleepycat.je.log.LogManager; 
import com.sleepycat.je.log.LogReadable; 
import com.sleepycat.je.log.LogUtils; 
import com.sleepycat.je.log.LogWritable; 
import com.sleepycat.je.log.entry.LNLogEntry; 
import com.sleepycat.je.recovery.RecoveryManager; 
import com.sleepycat.je.tree.LN; 
import com.sleepycat.je.tree.TreeLocation; 
import com.sleepycat.je.utilint.DbLsn; 
import com.sleepycat.je.utilint.Tracer; 
import de.ovgu.cide.jakutil.*; 
public  class  Txn  extends Locker  implements LogWritable, LogReadable {
	 public static final byte TXN_NOSYNC=0;

	 public static final byte TXN_WRITE_NOSYNC=1;

	 public static final byte TXN_SYNC=2;

	 private static final String DEBUG_NAME=Txn.class.getName();

	 private byte txnState;

	 private CursorImpl cursorSet;

	 private static final byte USABLE=0;

	 private static final byte CLOSED=1;

	 private static final byte ONLY_ABORTABLE=2;

	 private static final byte STATE_BITS=3;

	 private static final byte IS_PREPARED=4;

	 private static final byte XA_SUSPENDED=8;

	 private Set readLocks;

	 private Map writeInfo;

	 private Map undoDatabases;

	 private long lastLoggedLsn=DbLsn.NULL_LSN;

	 private long firstLoggedLsn=DbLsn.NULL_LSN;

	 private byte defaultFlushSyncBehavior;

	 private boolean serializableIsolation;

	 private boolean readCommittedIsolation;

	 private int inMemorySize;

	 public static int ACCUMULATED_LIMIT=10000;

	 public Txn( EnvironmentImpl envImpl, TransactionConfig config) throws DatabaseException { super(envImpl,config.getReadUncommitted(),config.getNoWait()); init(envImpl,config); }

	 public Txn( EnvironmentImpl envImpl, TransactionConfig config, long id) throws DatabaseException { super(envImpl,config.getReadUncommitted(),config.getNoWait()); init(envImpl,config); this.id=id; }

	 public Txn(){ lastLoggedLsn=DbLsn.NULL_LSN; }

	
private static  class  DatabaseCleanupInfo {
		 DatabaseImpl dbImpl;

		 boolean deleteAtCommit;

		 DatabaseCleanupInfo( DatabaseImpl dbImpl, boolean deleteAtCommit){ this.dbImpl=dbImpl; this.deleteAtCommit=deleteAtCommit; }


	}

	
@MethodObject static  class  Txn_addLock {
		 Txn_addLock( Txn _this, Long nodeId, Lock lock, LockType type, LockGrantType grantStatus){ this._this=_this; this.nodeId=nodeId; this.lock=lock; this.type=type; this.grantStatus=grantStatus; }

		 protected Txn _this;

		 protected Long nodeId;

		 protected Lock lock;

		 protected LockType type;

		 protected LockGrantType grantStatus;

		 protected int delta;

		 void execute__wrappee__base() throws DatabaseException {
synchronized (_this) { this.hook815(); if (type.isWriteLock()) { if (_this.writeInfo == null) { _this.writeInfo=new HashMap(); _this.undoDatabases=new HashMap(); this.hook818(); } _this.writeInfo.put(nodeId,new WriteLockInfo(lock)); this.hook817(); if ((grantStatus == LockGrantType.PROMOTION) || (grantStatus == LockGrantType.WAIT_PROMOTION)) { _this.readLocks.remove(lock); this.hook819(); } this.hook816(); } else { _this.addReadLock(lock); } } }

		 void execute() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	execute__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

		 protected void hook815__wrappee__base() throws DatabaseException { }

		 protected void hook815() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook815__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

		 protected void hook816__wrappee__base() throws DatabaseException { }

		 protected void hook816() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook816__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

		 protected void hook817__wrappee__base() throws DatabaseException { }

		 protected void hook817() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook817__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

		 protected void hook818__wrappee__base() throws DatabaseException { }

		 protected void hook818() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook818__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

		 protected void hook819__wrappee__base() throws DatabaseException { }

		 protected void hook819() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook819__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }


	}

	
@MethodObject static  class  Txn_traceCommit {
		 Txn_traceCommit( Txn _this, int numWriteLocks, int numReadLocks){ this._this=_this; this.numWriteLocks=numWriteLocks; this.numReadLocks=numReadLocks; }

		 protected Txn _this;

		 protected int numWriteLocks;

		 protected int numReadLocks;

		 protected Logger logger;

		 protected StringBuffer sb;

		 void execute__wrappee__base(){ }

		 void execute(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	execute__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }


	}

	 private void init__wrappee__base( EnvironmentImpl envImpl, TransactionConfig config) throws DatabaseException { serializableIsolation=config.getSerializableIsolation(); readCommittedIsolation=config.getReadCommitted(); if (config.getSync()) { defaultFlushSyncBehavior=TXN_SYNC; } else if (config.getWriteNoSync()) { defaultFlushSyncBehavior=TXN_WRITE_NOSYNC; } else if (config.getNoSync()) { defaultFlushSyncBehavior=TXN_NOSYNC; } else { defaultFlushSyncBehavior=TXN_SYNC; } lastLoggedLsn=DbLsn.NULL_LSN; firstLoggedLsn=DbLsn.NULL_LSN; txnState=USABLE; this.hook809(); this.envImpl.getTxnManager().registerTxn(this); }

	 private void init( EnvironmentImpl envImpl, TransactionConfig config) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	init__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected long generateId__wrappee__base( TxnManager txnManager){ return txnManager.incTxnId(); }

	 protected long generateId( TxnManager txnManager){ t.in(Thread.currentThread().getStackTrace()[1].toString());	generateId__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 long getLastLsn__wrappee__base(){ return lastLoggedLsn; }

	 long getLastLsn(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getLastLsn__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void setPrepared__wrappee__base( boolean prepared){ if (prepared) { txnState|=IS_PREPARED; } else { txnState&=~IS_PREPARED; } }

	 public void setPrepared( boolean prepared){ t.in(Thread.currentThread().getStackTrace()[1].toString());	setPrepared__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void setSuspended__wrappee__base( boolean suspended){ if (suspended) { txnState|=XA_SUSPENDED; } else { txnState&=~XA_SUSPENDED; } }

	 public void setSuspended( boolean suspended){ t.in(Thread.currentThread().getStackTrace()[1].toString());	setSuspended__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean isSuspended__wrappee__base(){ return (txnState & XA_SUSPENDED) != 0; }

	 public boolean isSuspended(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	isSuspended__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 LockResult lockInternal__wrappee__base( long nodeId, LockType lockType, boolean noWait, DatabaseImpl database) throws DatabaseException { long timeout=0; boolean useNoWait=noWait || defaultNoWait;
synchronized (this) { checkState(false); if (!useNoWait) { timeout=lockTimeOutMillis; } } LockGrantType grant=lockManager.lock(nodeId,this,lockType,timeout,useNoWait,database); WriteLockInfo info=null; if (writeInfo != null) { if (grant != LockGrantType.DENIED && lockType.isWriteLock()) {
synchronized (this) { info=(WriteLockInfo)writeInfo.get(new Long(nodeId)); undoDatabases.put(database.getId(),database); } } } return new LockResult(grant,info); }

	 LockResult lockInternal( long nodeId, LockType lockType, boolean noWait, DatabaseImpl database) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	lockInternal__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public int prepare__wrappee__base( Xid xid) throws DatabaseException { if ((txnState & IS_PREPARED) != 0) { throw new DatabaseException("prepare() has already been called for Transaction " + id + "."); }
synchronized (this) { checkState(false); if (checkCursorsForClose()) { throw new DatabaseException("Transaction " + id + " prepare failed because there were open cursors."); } TxnPrepare prepareRecord=new TxnPrepare(id,xid); LogManager logManager=envImpl.getLogManager(); logManager.logForceFlush(prepareRecord,true); } setPrepared(true); return XAResource.XA_OK; }

	 public int prepare( Xid xid) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	prepare__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void commit__wrappee__base( Xid xid) throws DatabaseException { commit(TXN_SYNC); envImpl.getTxnManager().unRegisterXATxn(xid,true); return; }

	 public void commit( Xid xid) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	commit__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void abort__wrappee__base( Xid xid) throws DatabaseException { abort(true); envImpl.getTxnManager().unRegisterXATxn(xid,false); return; }

	 public void abort( Xid xid) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	abort__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public long commit__wrappee__base() throws DatabaseException { return commit(defaultFlushSyncBehavior); }

	 public long commit() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	commit__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public long commit__wrappee__base( byte flushSyncBehavior) throws DatabaseException { try { long commitLsn=DbLsn.NULL_LSN;
synchronized (this) { checkState(false); if (checkCursorsForClose()) { throw new DatabaseException("Transaction " + id + " commit failed because there were open cursors."); } if (handleLockToHandleMap != null) { Iterator handleLockIter=handleLockToHandleMap.entrySet().iterator(); while (handleLockIter.hasNext()) { Map.Entry entry=(Map.Entry)handleLockIter.next(); transferHandleLockToHandleSet((Long)entry.getKey(),(Set)entry.getValue()); } } LogManager logManager=envImpl.getLogManager(); int numReadLocks=clearReadLocks(); int numWriteLocks=0; if (writeInfo != null) { numWriteLocks=writeInfo.size(); TxnCommit commitRecord=new TxnCommit(id,lastLoggedLsn); if (flushSyncBehavior == TXN_SYNC) { commitLsn=logManager.logForceFlush(commitRecord,true); } else if (flushSyncBehavior == TXN_WRITE_NOSYNC) { commitLsn=logManager.logForceFlush(commitRecord,false); } else { commitLsn=logManager.log(commitRecord); } this.hook806(); Set alreadyCountedLsnSet=new HashSet(); Iterator iter=writeInfo.values().iterator(); while (iter.hasNext()) { WriteLockInfo info=(WriteLockInfo)iter.next(); lockManager.release(info.lock,this); if (info.abortLsn != DbLsn.NULL_LSN && !info.abortKnownDeleted) { Long longLsn=new Long(info.abortLsn); if (!alreadyCountedLsnSet.contains(longLsn)) { logManager.countObsoleteNode(info.abortLsn,null); alreadyCountedLsnSet.add(longLsn); } } } writeInfo=null; this.hook803(); } traceCommit(numWriteLocks,numReadLocks); } this.hook805(); close(true); return commitLsn; } catch ( RunRecoveryException e) { throw e; }
catch ( Throwable t) { try { abortInternal(flushSyncBehavior == TXN_SYNC,!(t instanceof DatabaseException)); this.hook800(t); } catch ( Throwable abortT2) { throw new DatabaseException("Failed while attempting to commit transaction " + id + ". The attempt to abort and clean up also failed. "+ "The original exception seen from commit = "+ t.getMessage()+ " The exception from the cleanup = "+ abortT2.getMessage(),t); } throw new DatabaseException("Failed while attempting to commit transaction " + id + ", aborted instead. Original exception = "+ t.getMessage(),t); } }

	 public long commit( byte flushSyncBehavior) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	commit__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public long abort__wrappee__base( boolean forceFlush) throws DatabaseException { return abortInternal(forceFlush,true); }

	 public long abort( boolean forceFlush) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	abort__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private long abortInternal__wrappee__base( boolean forceFlush, boolean writeAbortRecord) throws DatabaseException { try { int numReadLocks; int numWriteLocks; long abortLsn;
synchronized (this) { checkState(true); TxnAbort abortRecord=new TxnAbort(id,lastLoggedLsn); abortLsn=DbLsn.NULL_LSN; if (writeInfo != null) { if (writeAbortRecord) { if (forceFlush) { abortLsn=envImpl.getLogManager().logForceFlush(abortRecord,true); } else { abortLsn=envImpl.getLogManager().log(abortRecord); } } } undo(); numReadLocks=(readLocks == null) ? 0 : clearReadLocks(); this.hook808(); numWriteLocks=(writeInfo == null) ? 0 : clearWriteLocks(); this.hook804(); } this.hook807();
synchronized (this) { boolean openCursors=checkCursorsForClose(); this.hook799(numReadLocks,numWriteLocks,openCursors); if (openCursors) { throw new DatabaseException("Transaction " + id + " detected open cursors while aborting"); } if (handleToHandleLockMap != null) { Iterator handleIter=handleToHandleLockMap.keySet().iterator(); while (handleIter.hasNext()) { Database handle=(Database)handleIter.next(); DbInternal.dbInvalidate(handle); } } return abortLsn; } } finally { close(false); } }

	 private long abortInternal( boolean forceFlush, boolean writeAbortRecord) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	abortInternal__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void undo__wrappee__base() throws DatabaseException { Long nodeId=null; long undoLsn=lastLoggedLsn; LogManager logManager=envImpl.getLogManager(); try { Set alreadyUndone=new HashSet(); TreeLocation location=new TreeLocation(); while (undoLsn != DbLsn.NULL_LSN) { LNLogEntry undoEntry=(LNLogEntry)logManager.getLogEntry(undoLsn); LN undoLN=undoEntry.getLN(); nodeId=new Long(undoLN.getNodeId()); if (!alreadyUndone.contains(nodeId)) { alreadyUndone.add(nodeId); DatabaseId dbId=undoEntry.getDbId(); DatabaseImpl db=(DatabaseImpl)undoDatabases.get(dbId); undoLN.postFetchInit(db,undoLsn); long abortLsn=undoEntry.getAbortLsn(); boolean abortKnownDeleted=undoEntry.getAbortKnownDeleted(); this.hook802(undoLsn,location,undoEntry,undoLN,db,abortLsn,abortKnownDeleted); if (!undoLN.isDeleted()) { logManager.countObsoleteNode(undoLsn,null); } } undoLsn=undoEntry.getUserTxn().getLastLsn(); } } catch ( RuntimeException e) { throw new DatabaseException("Txn undo for node=" + nodeId + " LSN="+ DbLsn.getNoFormatString(undoLsn),e); }
catch ( DatabaseException e) { this.hook801(nodeId,undoLsn,e); throw e; } }

	 private void undo() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	undo__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private int clearWriteLocks__wrappee__base() throws DatabaseException { int numWriteLocks=writeInfo.size(); Iterator iter=writeInfo.values().iterator(); while (iter.hasNext()) { WriteLockInfo info=(WriteLockInfo)iter.next(); lockManager.release(info.lock,this); } writeInfo=null; return numWriteLocks; }

	 private int clearWriteLocks() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	clearWriteLocks__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private int clearReadLocks__wrappee__base() throws DatabaseException { int numReadLocks=0; if (readLocks != null) { numReadLocks=readLocks.size(); Iterator iter=readLocks.iterator(); while (iter.hasNext()) { Lock rLock=(Lock)iter.next(); lockManager.release(rLock,this); } readLocks=null; } return numReadLocks; }

	 private int clearReadLocks() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	clearReadLocks__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void addLogInfo__wrappee__base( long lastLsn) throws DatabaseException { lastLoggedLsn=lastLsn;
synchronized (this) { if (firstLoggedLsn == DbLsn.NULL_LSN) { firstLoggedLsn=lastLsn; } } }

	 public void addLogInfo( long lastLsn) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	addLogInfo__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 long getFirstActiveLsn__wrappee__base() throws DatabaseException {
synchronized (this) { return firstLoggedLsn; } }

	 long getFirstActiveLsn() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getFirstActiveLsn__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 void addLock__wrappee__base( Long nodeId, Lock lock, LockType type, LockGrantType grantStatus) throws DatabaseException { new Txn_addLock(this,nodeId,lock,type,grantStatus).execute(); }

	 void addLock( Long nodeId, Lock lock, LockType type, LockGrantType grantStatus) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	addLock__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void addReadLock__wrappee__base( Lock lock){ int delta=0; if (readLocks == null) { readLocks=new HashSet(); delta=this.hook811(delta); } readLocks.add(lock); this.hook810(delta); }

	 private void addReadLock( Lock lock){ t.in(Thread.currentThread().getStackTrace()[1].toString());	addReadLock__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 void removeLock__wrappee__base( long nodeId, Lock lock) throws DatabaseException {
synchronized (this) { if ((readLocks != null) && readLocks.remove(lock)) { this.hook812(); } else if ((writeInfo != null) && (writeInfo.remove(new Long(nodeId)) != null)) { this.hook813(); } } }

	 void removeLock( long nodeId, Lock lock) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	removeLock__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 void moveWriteToReadLock__wrappee__base( long nodeId, Lock lock){ boolean found=false;
synchronized (this) { if ((writeInfo != null) && (writeInfo.remove(new Long(nodeId)) != null)) { found=true; this.hook814(); } assert found : "Couldn't find lock for Node " + nodeId + " in writeInfo Map."; addReadLock(lock); } }

	 void moveWriteToReadLock( long nodeId, Lock lock){ t.in(Thread.currentThread().getStackTrace()[1].toString());	moveWriteToReadLock__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean createdNode__wrappee__base( long nodeId) throws DatabaseException { boolean created=false;
synchronized (this) { if (writeInfo != null) { WriteLockInfo info=(WriteLockInfo)writeInfo.get(new Long(nodeId)); if (info != null) { created=info.createdThisTxn; } } } return created; }

	 public boolean createdNode( long nodeId) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	createdNode__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public long getAbortLsn__wrappee__base( long nodeId) throws DatabaseException { WriteLockInfo info=null;
synchronized (this) { if (writeInfo != null) { info=(WriteLockInfo)writeInfo.get(new Long(nodeId)); } } if (info == null) { return DbLsn.NULL_LSN; } else { return info.abortLsn; } }

	 public long getAbortLsn( long nodeId) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getAbortLsn__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public WriteLockInfo getWriteLockInfo__wrappee__base( long nodeId) throws DatabaseException { WriteLockInfo info=WriteLockInfo.basicWriteLockInfo;
synchronized (this) { if (writeInfo != null) { info=(WriteLockInfo)writeInfo.get(new Long(nodeId)); } } return info; }

	 public WriteLockInfo getWriteLockInfo( long nodeId) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getWriteLockInfo__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean isTransactional__wrappee__base(){ return true; }

	 public boolean isTransactional(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	isTransactional__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean isSerializableIsolation__wrappee__base(){ return serializableIsolation; }

	 public boolean isSerializableIsolation(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	isSerializableIsolation__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean isReadCommittedIsolation__wrappee__base(){ return readCommittedIsolation; }

	 public boolean isReadCommittedIsolation(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	isReadCommittedIsolation__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public Txn getTxnLocker__wrappee__base(){ return this; }

	 public Txn getTxnLocker(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getTxnLocker__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public Locker newNonTxnLocker__wrappee__base() throws DatabaseException { return this; }

	 public Locker newNonTxnLocker() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	newNonTxnLocker__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void releaseNonTxnLocks__wrappee__base() throws DatabaseException { }

	 public void releaseNonTxnLocks() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	releaseNonTxnLocks__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void operationEnd__wrappee__base() throws DatabaseException { }

	 public void operationEnd() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	operationEnd__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void operationEnd__wrappee__base( boolean operationOK) throws DatabaseException { }

	 public void operationEnd( boolean operationOK) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	operationEnd__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void setHandleLockOwner__wrappee__base( boolean ignore, Database dbHandle, boolean dbIsClosing) throws DatabaseException { if (dbIsClosing) { Long handleLockId=(Long)handleToHandleLockMap.get(dbHandle); if (handleLockId != null) { Set dbHandleSet=(Set)handleLockToHandleMap.get(handleLockId); boolean removed=dbHandleSet.remove(dbHandle); assert removed : "Can't find " + dbHandle + " from dbHandleSet"; if (dbHandleSet.size() == 0) { Object foo=handleLockToHandleMap.remove(handleLockId); assert (foo != null) : "Can't find " + handleLockId + " from handleLockIdtoHandleMap."; } } unregisterHandle(dbHandle); } else { if (dbHandle != null) { DbInternal.dbSetHandleLocker(dbHandle,this); } } }

	 public void setHandleLockOwner( boolean ignore, Database dbHandle, boolean dbIsClosing) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	setHandleLockOwner__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void registerCursor__wrappee__base( CursorImpl cursor) throws DatabaseException {
synchronized (this) { cursor.setLockerNext(cursorSet); if (cursorSet != null) { cursorSet.setLockerPrev(cursor); } cursorSet=cursor; } }

	 public void registerCursor( CursorImpl cursor) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	registerCursor__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void unRegisterCursor__wrappee__base( CursorImpl cursor) throws DatabaseException {
synchronized (this) { CursorImpl prev=cursor.getLockerPrev(); CursorImpl next=cursor.getLockerNext(); if (prev == null) { cursorSet=next; } else { prev.setLockerNext(next); } if (next != null) { next.setLockerPrev(prev); } cursor.setLockerPrev(null); cursor.setLockerNext(null); } }

	 public void unRegisterCursor( CursorImpl cursor) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	unRegisterCursor__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean isHandleLockTransferrable__wrappee__base(){ return false; }

	 public boolean isHandleLockTransferrable(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	isHandleLockTransferrable__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private boolean checkCursorsForClose__wrappee__base() throws DatabaseException { CursorImpl c=cursorSet; while (c != null) { if (!c.isClosed()) { return true; } c=c.getLockerNext(); } return false; }

	 private boolean checkCursorsForClose() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	checkCursorsForClose__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void setOnlyAbortable__wrappee__base(){ txnState&=~STATE_BITS; txnState|=ONLY_ABORTABLE; }

	 public void setOnlyAbortable(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	setOnlyAbortable__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean getOnlyAbortable__wrappee__base(){ return (txnState & ONLY_ABORTABLE) != 0; }

	 public boolean getOnlyAbortable(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getOnlyAbortable__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void checkState__wrappee__base( boolean calledByAbort) throws DatabaseException { boolean ok=false; boolean onlyAbortable=false; byte state=(byte)(txnState & STATE_BITS); ok=(state == USABLE); onlyAbortable=(state == ONLY_ABORTABLE); if (!calledByAbort && onlyAbortable) { throw new DatabaseException("Transaction " + id + " must be aborted."); } if (ok || (calledByAbort && onlyAbortable)) { return; } throw new DatabaseException("Transaction " + id + " has been closed."); }

	 protected void checkState( boolean calledByAbort) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	checkState__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void close__wrappee__base( boolean isCommit) throws DatabaseException {
synchronized (this) { txnState&=~STATE_BITS; txnState|=CLOSED; } envImpl.getTxnManager().unRegisterTxn(this,isCommit); }

	 private void close( boolean isCommit) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	close__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public int getLogSize__wrappee__base(){ return LogUtils.LONG_BYTES + LogUtils.LONG_BYTES; }

	 public int getLogSize(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getLogSize__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void writeToLog__wrappee__base( ByteBuffer logBuffer){ LogUtils.writeLong(logBuffer,id); LogUtils.writeLong(logBuffer,lastLoggedLsn); }

	 public void writeToLog( ByteBuffer logBuffer){ t.in(Thread.currentThread().getStackTrace()[1].toString());	writeToLog__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void readFromLog__wrappee__base( ByteBuffer logBuffer, byte entryTypeVersion){ id=LogUtils.readLong(logBuffer); lastLoggedLsn=LogUtils.readLong(logBuffer); }

	 public void readFromLog( ByteBuffer logBuffer, byte entryTypeVersion){ t.in(Thread.currentThread().getStackTrace()[1].toString());	readFromLog__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void dumpLog__wrappee__base( StringBuffer sb, boolean verbose){ sb.append("<txn id=\""); sb.append(super.toString()); sb.append("\">"); sb.append(DbLsn.toString(lastLoggedLsn)); sb.append("</txn>"); }

	 public void dumpLog( StringBuffer sb, boolean verbose){ t.in(Thread.currentThread().getStackTrace()[1].toString());	dumpLog__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public long getTransactionId__wrappee__base(){ return getId(); }

	 public long getTransactionId(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getTransactionId__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean logEntryIsTransactional__wrappee__base(){ return true; }

	 public boolean logEntryIsTransactional(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	logEntryIsTransactional__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void transferHandleLockToHandleSet__wrappee__base( Long handleLockId, Set dbHandleSet) throws DatabaseException { int numHandles=dbHandleSet.size(); Database[] dbHandles=new Database[numHandles]; dbHandles=(Database[])dbHandleSet.toArray(dbHandles); Locker[] destTxns=new Locker[numHandles]; for (int i=0; i < numHandles; i++) { destTxns[i]=new BasicLocker(envImpl); } long nodeId=handleLockId.longValue(); lockManager.transferMultiple(nodeId,this,destTxns); for (int i=0; i < numHandles; i++) { destTxns[i].addToHandleMaps(handleLockId,dbHandles[i]); DbInternal.dbSetHandleLocker(dbHandles[i],destTxns[i]); } }

	 private void transferHandleLockToHandleSet( Long handleLockId, Set dbHandleSet) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	transferHandleLockToHandleSet__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void traceCommit__wrappee__base( int numWriteLocks, int numReadLocks){ new Txn_traceCommit(this,numWriteLocks,numReadLocks).execute(); }

	 private void traceCommit( int numWriteLocks, int numReadLocks){ t.in(Thread.currentThread().getStackTrace()[1].toString());	traceCommit__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 int getInMemorySize__wrappee__base(){ return inMemorySize; }

	 int getInMemorySize(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getInMemorySize__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook799__wrappee__base( int numReadLocks, int numWriteLocks, boolean openCursors) throws DatabaseException { }

	 protected void hook799( int numReadLocks, int numWriteLocks, boolean openCursors) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook799__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook800__wrappee__base( Throwable t) throws DatabaseException, Throwable { }

	 protected void hook800( Throwable t) throws DatabaseException, Throwable { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook800__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook801__wrappee__base( Long nodeId, long undoLsn, DatabaseException e) throws DatabaseException { }

	 protected void hook801( Long nodeId, long undoLsn, DatabaseException e) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook801__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook802__wrappee__base( long undoLsn, TreeLocation location, LNLogEntry undoEntry, LN undoLN, DatabaseImpl db, long abortLsn, boolean abortKnownDeleted) throws DatabaseException, RuntimeException { RecoveryManager.undo(Level.FINER,db,location,undoLN,undoEntry.getKey(),undoEntry.getDupKey(),undoLsn,abortLsn,abortKnownDeleted,null,false); }

	 protected void hook802( long undoLsn, TreeLocation location, LNLogEntry undoEntry, LN undoLN, DatabaseImpl db, long abortLsn, boolean abortKnownDeleted) throws DatabaseException, RuntimeException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook802__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook803__wrappee__base() throws DatabaseException, RunRecoveryException, Throwable { }

	 protected void hook803() throws DatabaseException, RunRecoveryException, Throwable { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook803__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook804__wrappee__base() throws DatabaseException { }

	 protected void hook804() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook804__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook805__wrappee__base() throws DatabaseException, RunRecoveryException, Throwable { }

	 protected void hook805() throws DatabaseException, RunRecoveryException, Throwable { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook805__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook806__wrappee__base() throws DatabaseException, RunRecoveryException, Throwable { }

	 protected void hook806() throws DatabaseException, RunRecoveryException, Throwable { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook806__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook807__wrappee__base() throws DatabaseException { }

	 protected void hook807() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook807__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook808__wrappee__base() throws DatabaseException { }

	 protected void hook808() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook808__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook809__wrappee__base() throws DatabaseException { }

	 protected void hook809() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook809__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook810__wrappee__base( int delta){ }

	 protected void hook810( int delta){ t.in(Thread.currentThread().getStackTrace()[1].toString());	hook810__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected int hook811__wrappee__base( int delta){ return delta; }

	 protected int hook811( int delta){ t.in(Thread.currentThread().getStackTrace()[1].toString());	hook811__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook812__wrappee__base() throws DatabaseException { }

	 protected void hook812() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook812__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook813__wrappee__base() throws DatabaseException { }

	 protected void hook813() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook813__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook814__wrappee__base(){ }

	 protected void hook814(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	hook814__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	private Tracer t = new Tracer();

	public Tracer getTracer(){return t;}


}

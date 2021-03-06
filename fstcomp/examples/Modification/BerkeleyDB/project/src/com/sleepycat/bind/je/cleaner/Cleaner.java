package com.sleepycat.je.cleaner; 
import java.io.IOException; 
import java.util.Arrays; 
import java.util.Collections; 
import java.util.Comparator; 
import java.util.Iterator; 
import java.util.Set; 
import java.util.logging.Level; 
import java.util.logging.Logger; 
import com.sleepycat.je.DatabaseException; 
import com.sleepycat.je.config.EnvironmentParams; 
import com.sleepycat.je.dbi.DatabaseId; 
import com.sleepycat.je.dbi.DatabaseImpl; 
import com.sleepycat.je.dbi.DbConfigManager; 
import com.sleepycat.je.dbi.DbTree; 
import com.sleepycat.je.dbi.EnvConfigObserver; 
import com.sleepycat.je.dbi.EnvironmentImpl; 
import com.sleepycat.je.log.FileManager; 
import com.sleepycat.je.tree.BIN; 
import com.sleepycat.je.tree.ChildReference; 
import com.sleepycat.je.tree.DIN; 
import com.sleepycat.je.tree.LN; 
import com.sleepycat.je.tree.Node; 
import com.sleepycat.je.tree.Tree; 
import com.sleepycat.je.tree.TreeLocation; 
import com.sleepycat.je.txn.BasicLocker; 
import com.sleepycat.je.txn.LockGrantType; 
import com.sleepycat.je.txn.LockResult; 
import com.sleepycat.je.txn.LockType; 
import com.sleepycat.je.utilint.DaemonRunner; 
import com.sleepycat.je.utilint.DbLsn; 
import com.sleepycat.je.utilint.PropUtil; 
import com.sleepycat.je.utilint.Tracer; 
import de.ovgu.cide.jakutil.*; 
public  class  Cleaner  implements EnvConfigObserver {
	 static final String CLEAN_IN="CleanIN:";

	 static final String CLEAN_LN="CleanLN:";

	 static final String CLEAN_MIGRATE_LN="CleanMigrateLN:";

	 static final String CLEAN_PENDING_LN="CleanPendingLN:";

	 static final boolean PROACTIVE_MIGRATION=true;

	 static final boolean UPDATE_GENERATION=false;

	 int nCleanerRuns=0;

	 long lockTimeout;

	 int readBufferSize;

	 int nDeadlockRetries;

	 boolean expunge;

	 boolean clusterResident;

	 boolean clusterAll;

	 int maxBatchFiles;

	 long cleanerBytesInterval;

	 boolean trackDetail;

	 Set mustBeCleanedFiles=Collections.EMPTY_SET;

	 Set lowUtilizationFiles=Collections.EMPTY_SET;

	 private String name;

	 private EnvironmentImpl env;

	 private UtilizationProfile profile;

	 private UtilizationTracker tracker;

	 private FileSelector fileSelector;

	 private FileProcessor[] threads;

	 private Object deleteFileLock;

	 public Cleaner( EnvironmentImpl env, String name) throws DatabaseException { this.env=env; this.name=name; tracker=new UtilizationTracker(env,this); profile=new UtilizationProfile(env,tracker); fileSelector=new FileSelector(); threads=new FileProcessor[0]; deleteFileLock=new Object(); trackDetail=env.getConfigManager().getBoolean(EnvironmentParams.CLEANER_TRACK_DETAIL); envConfigUpdate(env.getConfigManager()); env.addConfigObserver(this); }

	
@MethodObject static  class  Cleaner_processPending {
		 Cleaner_processPending( Cleaner _this){ this._this=_this; }

		 protected Cleaner _this;

		 protected DbTree dbMapTree;

		 protected LNInfo[] pendingLNs;

		 protected TreeLocation location;

		 protected LNInfo info;

		 protected DatabaseId dbId1;

		 protected DatabaseImpl db1;

		 protected byte[] key;

		 protected byte[] dupKey;

		 protected LN ln;

		 protected DatabaseId[] pendingDBs;

		 protected DatabaseId dbId2;

		 protected DatabaseImpl db2;

		 void execute__wrappee__base() throws DatabaseException { dbMapTree=_this.env.getDbMapTree(); pendingLNs=_this.fileSelector.getPendingLNs(); if (pendingLNs != null) { location=new TreeLocation(); for (int i=0; i < pendingLNs.length; i+=1) { info=pendingLNs[i]; dbId1=info.getDbId(); db1=dbMapTree.getDb(dbId1,_this.lockTimeout); key=info.getKey(); dupKey=info.getDupKey(); ln=info.getLN(); this.hook114(); _this.processPendingLN(ln,db1,key,dupKey,location); } } }

		 void execute() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	execute__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

		 protected void hook114__wrappee__base() throws DatabaseException { }

		 protected void hook114() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook114__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }


	}

	 public void envConfigUpdate__wrappee__base( DbConfigManager cm) throws DatabaseException { lockTimeout=PropUtil.microsToMillis(cm.getLong(EnvironmentParams.CLEANER_LOCK_TIMEOUT)); readBufferSize=cm.getInt(EnvironmentParams.CLEANER_READ_SIZE); if (readBufferSize <= 0) { readBufferSize=cm.getInt(EnvironmentParams.LOG_ITERATOR_READ_SIZE); } this.hook94(cm); nDeadlockRetries=cm.getInt(EnvironmentParams.CLEANER_DEADLOCK_RETRY); expunge=cm.getBoolean(EnvironmentParams.CLEANER_REMOVE); clusterResident=cm.getBoolean(EnvironmentParams.CLEANER_CLUSTER); clusterAll=cm.getBoolean(EnvironmentParams.CLEANER_CLUSTER_ALL); maxBatchFiles=cm.getInt(EnvironmentParams.CLEANER_MAX_BATCH_FILES); this.hook90(); if (clusterResident && clusterAll) { throw new IllegalArgumentException("Both " + EnvironmentParams.CLEANER_CLUSTER + " and "+ EnvironmentParams.CLEANER_CLUSTER_ALL+ " may not be set to true."); } int nThreads=cm.getInt(EnvironmentParams.CLEANER_THREADS); assert nThreads > 0; if (nThreads != threads.length) { for (int i=nThreads; i < threads.length; i+=1) { if (threads[i] != null) { threads[i].shutdown(); threads[i]=null; } } FileProcessor[] newThreads=new FileProcessor[nThreads]; for (int i=0; i < nThreads && i < threads.length; i+=1) { newThreads[i]=threads[i]; } threads=newThreads; for (int i=0; i < nThreads; i+=1) { if (threads[i] == null) { threads[i]=new FileProcessor(name + '-' + (i + 1),env,this,profile,fileSelector); } } } cleanerBytesInterval=cm.getLong(EnvironmentParams.CLEANER_BYTES_INTERVAL); if (cleanerBytesInterval == 0) { cleanerBytesInterval=cm.getLong(EnvironmentParams.LOG_FILE_MAX) / 4; } }

	 public void envConfigUpdate( DbConfigManager cm) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	envConfigUpdate__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public UtilizationTracker getUtilizationTracker__wrappee__base(){ return tracker; }

	 public UtilizationTracker getUtilizationTracker(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getUtilizationTracker__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public UtilizationProfile getUtilizationProfile__wrappee__base(){ return profile; }

	 public UtilizationProfile getUtilizationProfile(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getUtilizationProfile__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void wakeup__wrappee__base(){ for (int i=0; i < threads.length; i+=1) { if (threads[i] != null) { threads[i].wakeup(); } } }

	 public void wakeup(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	wakeup__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private boolean areThreadsRunning__wrappee__base(){ for (int i=0; i < threads.length; i+=1) { if (threads[i] != null) { return threads[i].isRunning(); } } return false; }

	 private boolean areThreadsRunning(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	areThreadsRunning__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public int doClean__wrappee__base( boolean cleanMultipleFiles, boolean forceCleaning) throws DatabaseException { FileProcessor processor=new FileProcessor("",env,this,profile,fileSelector); return processor.doClean(false,cleanMultipleFiles,forceCleaning); }

	 public int doClean( boolean cleanMultipleFiles, boolean forceCleaning) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	doClean__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 void deleteSafeToDeleteFiles__wrappee__base() throws DatabaseException { try {
synchronized (deleteFileLock) { Set safeFiles=fileSelector.copySafeToDeleteFiles(); if (safeFiles == null) { return; } env.checkIfInvalid(); if (env.mayNotWrite()) { return; } this.hook115(safeFiles); } } catch ( ReturnVoid r) { return; } }

	 void deleteSafeToDeleteFiles() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	deleteSafeToDeleteFiles__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void traceFileNotDeleted__wrappee__base( Exception e, long fileNum){ }

	 private void traceFileNotDeleted( Exception e, long fileNum){ t.in(Thread.currentThread().getStackTrace()[1].toString());	traceFileNotDeleted__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public Set\[\] getFilesAtCheckpointStart__wrappee__base() throws DatabaseException { processPending(); return fileSelector.getFilesAtCheckpointStart(); }

	 public Set[] getFilesAtCheckpointStart() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getFilesAtCheckpointStart__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void updateFilesAtCheckpointEnd__wrappee__base( Set[] files) throws DatabaseException { fileSelector.updateFilesAtCheckpointEnd(files); deleteSafeToDeleteFiles(); }

	 public void updateFilesAtCheckpointEnd( Set[] files) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	updateFilesAtCheckpointEnd__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void updateReadOnlyFileCollections__wrappee__base(){ mustBeCleanedFiles=fileSelector.getMustBeCleanedFiles(); lowUtilizationFiles=fileSelector.getLowUtilizationFiles(); }

	 public void updateReadOnlyFileCollections(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	updateReadOnlyFileCollections__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 void processPending__wrappee__base() throws DatabaseException { new Cleaner_processPending(this).execute(); }

	 void processPending() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	processPending__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void processPendingLN__wrappee__base( LN ln, DatabaseImpl db, byte[] key, byte[] dupKey, TreeLocation location) throws DatabaseException { boolean parentFound=false; boolean processedHere=true; boolean lockDenied=false; boolean obsolete=false; boolean completed=false; BasicLocker locker=null; BIN bin=null; DIN parentDIN=null; try { this.hook97(); boolean c=db == null; c=this.hook112(db,c); if (c) { this.hook113(db); this.hook98(); obsolete=true; completed=true; return; } Tree tree=db.getTree(); assert tree != null; locker=new BasicLocker(env); LockResult lockRet=locker.nonBlockingLock(ln.getNodeId(),LockType.READ,db); if (lockRet.getLockGrant() == LockGrantType.DENIED) { this.hook99(); lockDenied=true; completed=true; return; } parentFound=tree.getParentBINForChildLN(location,key,dupKey,ln,false,true,true,UPDATE_GENERATION); bin=location.bin; int index=location.index; if (!parentFound) { this.hook100(); obsolete=true; completed=true; return; } if (ln.containsDuplicates()) { parentDIN=(DIN)bin.fetchTarget(index); parentDIN.latch(UPDATE_GENERATION); ChildReference dclRef=parentDIN.getDupCountLNRef(); processedHere=false; migrateDupCountLN(db,dclRef.getLsn(),parentDIN,dclRef,true,true,ln.getNodeId(),CLEAN_PENDING_LN); } else { processedHere=false; migrateLN(db,bin.getLsn(index),bin,index,true,true,ln.getNodeId(),CLEAN_PENDING_LN); } completed=true; } catch ( DatabaseException DBE) { DBE.printStackTrace(); this.hook89(DBE); throw DBE; } finally { this.hook95(bin,parentDIN); if (locker != null) { locker.operationEnd(); } if (processedHere) { if (completed && !lockDenied) { fileSelector.removePendingLN(ln.getNodeId()); } this.hook91(ln,obsolete,completed); } } }

	 private void processPendingLN( LN ln, DatabaseImpl db, byte[] key, byte[] dupKey, TreeLocation location) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	processPendingLN__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void lazyMigrateLNs__wrappee__base( final BIN bin, boolean proactiveMigration) throws DatabaseException { DatabaseImpl db=bin.getDatabase(); boolean isBinInDupDb=db.getSortedDuplicates() && !bin.containsDuplicates(); Integer[] sortedIndices=null; int nSortedIndices=0; int nEntries=bin.getNEntries(); for (int index=0; index < nEntries; index+=1) { boolean migrateFlag=bin.getMigrate(index); boolean isResident=(bin.getTarget(index) != null); long childLsn=bin.getLsn(index); if (shouldMigrateLN(migrateFlag,isResident,proactiveMigration,isBinInDupDb,childLsn)) { if (isResident) { migrateLN(db,childLsn,bin,index,migrateFlag,false,0,CLEAN_MIGRATE_LN); } else { if (sortedIndices == null) { sortedIndices=new Integer[nEntries]; } sortedIndices[nSortedIndices++]=new Integer(index); } } } if (sortedIndices != null) { Arrays.sort(sortedIndices,0,nSortedIndices,new Comparator(){ public int compare( Object o1, Object o2){ int i1=((Integer)o1).intValue(); int i2=((Integer)o2).intValue(); return DbLsn.compareTo(bin.getLsn(i1),bin.getLsn(i2)); } }
); for (int i=0; i < nSortedIndices; i+=1) { int index=sortedIndices[i].intValue(); long childLsn=bin.getLsn(index); boolean migrateFlag=bin.getMigrate(index); migrateLN(db,childLsn,bin,index,migrateFlag,false,0,CLEAN_MIGRATE_LN); } } }

	 public void lazyMigrateLNs( final BIN bin, boolean proactiveMigration) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	lazyMigrateLNs__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void lazyMigrateDupCountLN__wrappee__base( DIN din, ChildReference dclRef, boolean proactiveMigration) throws DatabaseException { DatabaseImpl db=din.getDatabase(); boolean migrateFlag=dclRef.getMigrate(); boolean isResident=(dclRef.getTarget() != null); boolean isBinInDupDb=false; long childLsn=dclRef.getLsn(); if (shouldMigrateLN(migrateFlag,isResident,proactiveMigration,isBinInDupDb,childLsn)) { migrateDupCountLN(db,childLsn,din,dclRef,migrateFlag,false,0,CLEAN_MIGRATE_LN); } }

	 public void lazyMigrateDupCountLN( DIN din, ChildReference dclRef, boolean proactiveMigration) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	lazyMigrateDupCountLN__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private boolean shouldMigrateLN__wrappee__base( boolean migrateFlag, boolean isResident, boolean proactiveMigration, boolean isBinInDupDb, long childLsn){ boolean doMigration=false; if (migrateFlag) { doMigration=true; this.hook101(); } else if (!proactiveMigration || isBinInDupDb || env.isClosing()) { } else { Long fileNum=new Long(DbLsn.getFileNumber(childLsn)); if ((PROACTIVE_MIGRATION || isResident) && mustBeCleanedFiles.contains(fileNum)) { doMigration=true; this.hook102(); } else if ((clusterAll || (clusterResident && isResident)) && lowUtilizationFiles.contains(fileNum)) { doMigration=true; this.hook103(); } } return doMigration; }

	 private boolean shouldMigrateLN( boolean migrateFlag, boolean isResident, boolean proactiveMigration, boolean isBinInDupDb, long childLsn){ t.in(Thread.currentThread().getStackTrace()[1].toString());	shouldMigrateLN__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void migrateLN__wrappee__base( DatabaseImpl db, long lsn, BIN bin, int index, boolean wasCleaned, boolean isPending, long lockedPendingNodeId, String cleanAction) throws DatabaseException { boolean obsolete=false; boolean migrated=false; boolean lockDenied=false; boolean completed=false; boolean clearTarget=false; BasicLocker locker=null; LN ln=null; try { if (!bin.isEntryKnownDeleted(index)) { ln=(LN)bin.getTarget(index); if (ln == null) { ln=(LN)bin.fetchTarget(index); clearTarget=!db.getId().equals(DbTree.ID_DB_ID); } } if (ln == null) { this.hook105(wasCleaned); obsolete=true; completed=true; return; } if (lockedPendingNodeId != ln.getNodeId()) { locker=new BasicLocker(env); LockResult lockRet=locker.nonBlockingLock(ln.getNodeId(),LockType.READ,db); if (lockRet.getLockGrant() == LockGrantType.DENIED) { this.hook106(wasCleaned); lockDenied=true; completed=true; return; } } if (ln.isDeleted()) { bin.setKnownDeletedLeaveTarget(index); this.hook107(wasCleaned); obsolete=true; completed=true; return; } if (bin.getMigrate(index)) { Long fileNum=new Long(DbLsn.getFileNumber(lsn)); if (!fileSelector.isFileCleaningInProgress(fileNum)) { obsolete=true; completed=true; this.hook108(wasCleaned); return; } } byte[] key=getLNMainKey(bin,index); long newLNLsn=ln.log(env,db.getId(),key,lsn,locker); bin.updateEntry(index,newLNLsn); this.hook104(); migrated=true; completed=true; return; } finally { if (isPending) { if (completed && !lockDenied) { fileSelector.removePendingLN(lockedPendingNodeId); } } else { if (bin.getMigrate(index) && (!completed || lockDenied)) { byte[] key=getLNMainKey(bin,index); byte[] dupKey=getLNDupKey(bin,index,ln); fileSelector.addPendingLN(ln,db.getId(),key,dupKey); if (!areThreadsRunning()) { env.getUtilizationTracker().activateCleaner(); } clearTarget=false; } } bin.setMigrate(index,false); if (clearTarget) { bin.updateEntry(index,null); } if (locker != null) { locker.operationEnd(); } this.hook92(lsn,cleanAction,obsolete,migrated,completed,ln); } }

	 private void migrateLN( DatabaseImpl db, long lsn, BIN bin, int index, boolean wasCleaned, boolean isPending, long lockedPendingNodeId, String cleanAction) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	migrateLN__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void migrateDupCountLN__wrappee__base( DatabaseImpl db, long lsn, DIN parentDIN, ChildReference dclRef, boolean wasCleaned, boolean isPending, long lockedPendingNodeId, String cleanAction) throws DatabaseException { boolean obsolete=false; boolean migrated=false; boolean lockDenied=false; boolean completed=false; boolean clearTarget=false; BasicLocker locker=null; LN ln=null; try { ln=(LN)dclRef.getTarget(); if (ln == null) { ln=(LN)dclRef.fetchTarget(db,parentDIN); assert ln != null; clearTarget=!db.getId().equals(DbTree.ID_DB_ID); } if (lockedPendingNodeId != ln.getNodeId()) { locker=new BasicLocker(env); LockResult lockRet=locker.nonBlockingLock(ln.getNodeId(),LockType.READ,db); if (lockRet.getLockGrant() == LockGrantType.DENIED) { this.hook110(wasCleaned); lockDenied=true; completed=true; return; } } Long fileNum=new Long(DbLsn.getFileNumber(lsn)); if (!fileSelector.isFileCleaningInProgress(fileNum)) { obsolete=true; completed=true; this.hook111(wasCleaned); return; } byte[] key=parentDIN.getDupKey(); long newLNLsn=ln.log(env,db.getId(),key,lsn,locker); parentDIN.updateDupCountLNRef(newLNLsn); this.hook109(); migrated=true; completed=true; return; } finally { if (isPending) { if (completed && !lockDenied) { fileSelector.removePendingLN(lockedPendingNodeId); } } else { if (dclRef.getMigrate() && (!completed || lockDenied)) { byte[] key=parentDIN.getDupKey(); byte[] dupKey=null; fileSelector.addPendingLN(ln,db.getId(),key,dupKey); if (!areThreadsRunning()) { env.getUtilizationTracker().activateCleaner(); } clearTarget=false; } } dclRef.setMigrate(false); if (clearTarget) { parentDIN.updateDupCountLN(null); } if (locker != null) { locker.operationEnd(); } this.hook93(lsn,cleanAction,obsolete,migrated,completed,ln); } }

	 private void migrateDupCountLN( DatabaseImpl db, long lsn, DIN parentDIN, ChildReference dclRef, boolean wasCleaned, boolean isPending, long lockedPendingNodeId, String cleanAction) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	migrateDupCountLN__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private byte\[\] getLNMainKey__wrappee__base( BIN bin, int index) throws DatabaseException { if (bin.containsDuplicates()) { return bin.getDupKey(); } else { return bin.getKey(index); } }

	 private byte[] getLNMainKey( BIN bin, int index) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getLNMainKey__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private byte\[\] getLNDupKey__wrappee__base( BIN bin, int index, LN ln) throws DatabaseException { DatabaseImpl db=bin.getDatabase(); if (!db.getSortedDuplicates() || ln.containsDuplicates()) { return null; } else if (bin.containsDuplicates()) { return bin.getKey(index); } else { return ln.getData(); } }

	 private byte[] getLNDupKey( BIN bin, int index, LN ln) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getLNDupKey__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook88__wrappee__base( long fileNumValue) throws DatabaseException { }

	 protected void hook88( long fileNumValue) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook88__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook89__wrappee__base( DatabaseException DBE) throws DatabaseException { }

	 protected void hook89( DatabaseException DBE) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook89__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook90__wrappee__base() throws DatabaseException { }

	 protected void hook90() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook90__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook91__wrappee__base( LN ln, boolean obsolete, boolean completed) throws DatabaseException { }

	 protected void hook91( LN ln, boolean obsolete, boolean completed) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook91__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook92__wrappee__base( long lsn, String cleanAction, boolean obsolete, boolean migrated, boolean completed, LN ln) throws DatabaseException { }

	 protected void hook92( long lsn, String cleanAction, boolean obsolete, boolean migrated, boolean completed, LN ln) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook92__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook93__wrappee__base( long lsn, String cleanAction, boolean obsolete, boolean migrated, boolean completed, LN ln) throws DatabaseException { }

	 protected void hook93( long lsn, String cleanAction, boolean obsolete, boolean migrated, boolean completed, LN ln) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook93__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook94__wrappee__base( DbConfigManager cm) throws DatabaseException { }

	 protected void hook94( DbConfigManager cm) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook94__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook95__wrappee__base( BIN bin, DIN parentDIN) throws DatabaseException { }

	 protected void hook95( BIN bin, DIN parentDIN) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook95__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook96__wrappee__base() throws DatabaseException { }

	 protected void hook96() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook96__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook97__wrappee__base() throws DatabaseException { }

	 protected void hook97() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook97__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook98__wrappee__base() throws DatabaseException { }

	 protected void hook98() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook98__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook99__wrappee__base() throws DatabaseException { }

	 protected void hook99() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook99__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook100__wrappee__base() throws DatabaseException { }

	 protected void hook100() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook100__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook101__wrappee__base(){ }

	 protected void hook101(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	hook101__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook102__wrappee__base(){ }

	 protected void hook102(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	hook102__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook103__wrappee__base(){ }

	 protected void hook103(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	hook103__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook104__wrappee__base() throws DatabaseException { }

	 protected void hook104() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook104__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook105__wrappee__base( boolean wasCleaned) throws DatabaseException { }

	 protected void hook105( boolean wasCleaned) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook105__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook106__wrappee__base( boolean wasCleaned) throws DatabaseException { }

	 protected void hook106( boolean wasCleaned) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook106__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook107__wrappee__base( boolean wasCleaned) throws DatabaseException { }

	 protected void hook107( boolean wasCleaned) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook107__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook108__wrappee__base( boolean wasCleaned) throws DatabaseException { }

	 protected void hook108( boolean wasCleaned) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook108__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook109__wrappee__base() throws DatabaseException { }

	 protected void hook109() throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook109__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook110__wrappee__base( boolean wasCleaned) throws DatabaseException { }

	 protected void hook110( boolean wasCleaned) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook110__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook111__wrappee__base( boolean wasCleaned) throws DatabaseException { }

	 protected void hook111( boolean wasCleaned) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook111__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected boolean hook112__wrappee__base( DatabaseImpl db, boolean c) throws DatabaseException { return c; }

	 protected boolean hook112( DatabaseImpl db, boolean c) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook112__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook113__wrappee__base( DatabaseImpl db) throws DatabaseException { }

	 protected void hook113( DatabaseImpl db) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook113__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 protected void hook115__wrappee__base( Set safeFiles) throws DatabaseException { for (Iterator i=safeFiles.iterator(); i.hasNext(); ) { Long fileNum=(Long)i.next(); long fileNumValue=fileNum.longValue(); boolean deleted=false; try { if (expunge) { env.getFileManager().deleteFile(fileNumValue); } else { env.getFileManager().renameFile(fileNumValue,FileManager.DEL_SUFFIX); } deleted=true; } catch ( DatabaseException e) { traceFileNotDeleted(e,fileNumValue); }
catch ( IOException e) { traceFileNotDeleted(e,fileNumValue); } if (deleted) { this.hook88(fileNumValue); try { profile.removeFile(fileNum); } finally { fileSelector.removeDeletedFile(fileNum); } } this.hook96(); } }

	 protected void hook115( Set safeFiles) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	hook115__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	private Tracer t = new Tracer();

	public Tracer getTracer(){return t;}


}

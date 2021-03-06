package com.sleepycat.je.cleaner; 
import java.util.ArrayList; 
import java.util.List; 
import com.sleepycat.je.DatabaseException; 
import com.sleepycat.je.dbi.EnvironmentImpl; 
import com.sleepycat.je.dbi.MemoryBudget; 
import com.sleepycat.je.log.LogEntryType; 
import com.sleepycat.je.utilint.DbLsn; 
import de.ovgu.cide.jakutil.*; 
public  class  UtilizationTracker {
	 private EnvironmentImpl env;

	 private Cleaner cleaner;

	 private List files;

	 private long activeFile;

	 private TrackedFileSummary[] snapshot;

	 private long bytesSinceActivate;

	 public UtilizationTracker( EnvironmentImpl env) throws DatabaseException { this(env,env.getCleaner()); }

	 UtilizationTracker( EnvironmentImpl env, Cleaner cleaner) throws DatabaseException { assert cleaner != null; this.env=env; this.cleaner=cleaner; files=new ArrayList(); snapshot=new TrackedFileSummary[0]; activeFile=-1; }

	 public EnvironmentImpl getEnvironment__wrappee__base(){ return env; }

	 public EnvironmentImpl getEnvironment(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getEnvironment__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void activateCleaner__wrappee__base(){ env.getCleaner().wakeup(); bytesSinceActivate=0; }

	 public void activateCleaner(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	activateCleaner__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public TrackedFileSummary\[\] getTrackedFiles__wrappee__base(){ return snapshot; }

	 public TrackedFileSummary[] getTrackedFiles(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getTrackedFiles__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public TrackedFileSummary getTrackedFile__wrappee__base( long fileNum){ TrackedFileSummary[] a=snapshot; for (int i=0; i < a.length; i+=1) { if (a[i].getFileNumber() == fileNum) { return a[i]; } } return null; }

	 public TrackedFileSummary getTrackedFile( long fileNum){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getTrackedFile__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public boolean countNewLogEntry__wrappee__base( long lsn, LogEntryType type, int size){ TrackedFileSummary file=getFile(DbLsn.getFileNumber(lsn)); file.totalCount+=1; file.totalSize+=size; if (type.isNodeType()) { if (inArray(type,LogEntryType.IN_TYPES)) { file.totalINCount+=1; file.totalINSize+=size; } else { file.totalLNCount+=1; file.totalLNSize+=size; } } bytesSinceActivate+=size; return (bytesSinceActivate >= env.getCleaner().cleanerBytesInterval); }

	 public boolean countNewLogEntry( long lsn, LogEntryType type, int size){ t.in(Thread.currentThread().getStackTrace()[1].toString());	countNewLogEntry__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void countObsoleteNode__wrappee__base( long lsn, LogEntryType type){ TrackedFileSummary file=getFile(DbLsn.getFileNumber(lsn)); countOneNode(file,type); file.trackObsolete(DbLsn.getFileOffset(lsn)); }

	 public void countObsoleteNode( long lsn, LogEntryType type){ t.in(Thread.currentThread().getStackTrace()[1].toString());	countObsoleteNode__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void countObsoleteNodeInexact__wrappee__base( long lsn, LogEntryType type){ TrackedFileSummary file=getFile(DbLsn.getFileNumber(lsn)); countOneNode(file,type); }

	 public void countObsoleteNodeInexact( long lsn, LogEntryType type){ t.in(Thread.currentThread().getStackTrace()[1].toString());	countObsoleteNodeInexact__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void countOneNode__wrappee__base( TrackedFileSummary file, LogEntryType type){ if (type == null || type.isNodeType()) { if (type == null || !inArray(type,LogEntryType.IN_TYPES)) { file.obsoleteLNCount+=1; } else { file.obsoleteINCount+=1; } } }

	 private void countOneNode( TrackedFileSummary file, LogEntryType type){ t.in(Thread.currentThread().getStackTrace()[1].toString());	countOneNode__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public void addSummary__wrappee__base( long fileNumber, TrackedFileSummary other){ TrackedFileSummary file=getFile(fileNumber); file.addTrackedSummary(other); }

	 public void addSummary( long fileNumber, TrackedFileSummary other){ t.in(Thread.currentThread().getStackTrace()[1].toString());	addSummary__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 public TrackedFileSummary getUnflushableTrackedSummary__wrappee__base( long fileNum) throws DatabaseException { TrackedFileSummary file=getFile(fileNum); file.setAllowFlush(false); return file; }

	 public TrackedFileSummary getUnflushableTrackedSummary( long fileNum) throws DatabaseException { t.in(Thread.currentThread().getStackTrace()[1].toString());	getUnflushableTrackedSummary__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private TrackedFileSummary getFile__wrappee__base( long fileNum){ if (activeFile < fileNum) { activeFile=fileNum; } int size=files.size(); for (int i=0; i < size; i+=1) { TrackedFileSummary file=(TrackedFileSummary)files.get(i); if (file.getFileNumber() == fileNum) { return file; } } TrackedFileSummary file=new TrackedFileSummary(this,fileNum,cleaner.trackDetail); files.add(file); takeSnapshot(); return file; }

	 private TrackedFileSummary getFile( long fileNum){ t.in(Thread.currentThread().getStackTrace()[1].toString());	getFile__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 void resetFile__wrappee__base( TrackedFileSummary file){ if (file.getFileNumber() < activeFile && file.getAllowFlush()) { files.remove(file); takeSnapshot(); } }

	 void resetFile( TrackedFileSummary file){ t.in(Thread.currentThread().getStackTrace()[1].toString());	resetFile__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private void takeSnapshot__wrappee__base(){ TrackedFileSummary[] a=new TrackedFileSummary[files.size()]; files.toArray(a); snapshot=a; }

	 private void takeSnapshot(){ t.in(Thread.currentThread().getStackTrace()[1].toString());	takeSnapshot__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	 private boolean inArray__wrappee__base( Object o, Object[] a){ for (int i=0; i < a.length; i+=1) { if (a[i] == o) { return true; } } return false; }

	 private boolean inArray( Object o, Object[] a){ t.in(Thread.currentThread().getStackTrace()[1].toString());	inArray__wrappee__base(); t.out(Thread.currentThread().getStackTrace()[1].toString()); }

	private Tracer t = new Tracer();

	public Tracer getTracer(){return t;}


}

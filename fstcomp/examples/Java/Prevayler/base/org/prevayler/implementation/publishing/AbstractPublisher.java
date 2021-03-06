package org.prevayler.implementation.publishing;
import org.prevayler.Clock;
import org.prevayler.implementation.TransactionTimestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** 
 * This class provides basic subscriber addition and notification.
 */
public abstract class AbstractPublisher implements TransactionPublisher {
  protected final Clock _clock;
  private final List _subscribers=new LinkedList();
  public AbstractPublisher(  Clock clock){
    _clock=clock;
  }
  public Clock clock(){
    return _clock;
  }
  public synchronized void addSubscriber(  TransactionSubscriber subscriber){
    _subscribers.add(subscriber);
  }
  public synchronized void cancelSubscription(  TransactionSubscriber subscriber){
    _subscribers.remove(subscriber);
  }
  protected synchronized void notifySubscribers(  TransactionTimestamp transactionTimestamp){
    Iterator i=_subscribers.iterator();
    while (i.hasNext())     ((TransactionSubscriber)i.next()).receive(transactionTimestamp);
  }
}

package hrscala.futprom

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap

import scala.concurrent._
import scala.util.control.NonFatal

/** Defines cache interface
  */
trait Cache[V] { cache ⇒

  /** Selects the, potentially non-existing, cache entry with the given key
    */
  def apply(key: Any) = new Keyed(key)

  class Keyed(key: Any) {
    import ExecutionContext.Implicits.global

    /** Returns either the cached Future for the given key, or evaluates the
      * given call-by-name value which is then put inside a completed future.
      */
    def apply(value: ⇒ V): Future[V] =
      cache(key, () ⇒ try Future.successful(value) catch { case NonFatal(e) ⇒ Future.failed(e) })

    /** Returns either the cached Future for the given key, or provided Future
      */
    def apply(future: Future[V]): Future[V] = cache(key, () ⇒ future)

    /** Returns either the cached Future for the given key, or evaluates the
      * given function which should eventually lead to the completion of the
      * promise.
      */
    def apply[U](f: Promise[V] ⇒ U)(implicit ec: ExecutionContext): Future[V] =
      cache(
        key,
        () ⇒ {
          val p = Promise[V]()
          f(p)
          p.future
        }
      )
  }

  /** Returns either the cached Future for the given key or evaluates the given
    * function that produces a `Future[V]`.
    */
  def apply(key: Any, genValue: () ⇒ Future[V])(implicit ec: ExecutionContext): Future[V]

  /** Returns the `Some(future)` stored under the given key, or `None` if the key
    * has no corresponding cache entry.
    */
  def get(key: Any): Option[Future[V]]

  /** Removes the cache item for the given key and returnes it wrapped in `Some`.
    * Returns `None` if the key has no corresponding cache entry.
    */
  def remove(key: Any): Option[Future[V]]

  /** Clears the cache by removing all the entries */
  def clear(): Unit

  /** Returns the number of currently cached entries */
  def size: Int
}

/** Implementation of [[Cache]].
  *
  * The cache has defined maximum number of entries it can store. If that number
  * is reached, the next entry will cause the oldest one to be evicted.
  *
  * @param maxCapacity      maximum number of entries the cache can store
  * @param initialCapacity  initial capacity
  * @tparam V               the type of the stored values
  */
final class LruCache[V](val maxCapacity: Int, val initialCapacity: Int) extends Cache[V] {
  require(maxCapacity >= 0, "maxCapacity must not be negative")
  require(initialCapacity <= maxCapacity, "initialCapacity must be <= maxCapacity")

  /* ConcurrentLinkedHashMap provides LRU semantics for the cache
   */
  private[futprom] val store = new ConcurrentLinkedHashMap.Builder[Any, Future[V]]
    .initialCapacity(initialCapacity)
    .maximumWeightedCapacity(maxCapacity)
    .build()

  /** Returns either the stored Future for the given key, or evaluates the
    * `getValue` function, and stores and returns its result.
    *
    * Cache should not store exceptions.
    *
    * Hint 1: the `store` implements `java.util.concurrent.ConcurrentMap`. It
    * implements an atomic method particularly fit for this purpuse.
    *
    * Hint 2: the Promise should be used to evaluate `getValue` only in case
    * of a cache miss.
    */
  override def apply(key: Any, genValue: () => Future[V])(implicit ec: ExecutionContext): Future[V] = {//???
    val promise = Promise[V]()
    store.putIfAbsent(key, promise.future) match {
      case null ⇒
        val future = genValue()
        future.onComplete { value ⇒
          promise.complete(value)
          // in case of exceptions we remove the cache entry (i.e. try again later)
          if (value.isFailure) store.remove(key, promise)
        }
        future
      case existingFuture ⇒ existingFuture
    }
  }


  override def get(key: Any): Option[Future[V]] = Option(store.get(key)) //???

  override def clear(): Unit = store.clear() //???

  override def size: Int = store.size() //???

  override def remove(key: Any): Option[Future[V]] = Option(store.remove(key)) //???
}

object LruCache {

  def apply[V](maxCapacity: Int = 100, initialCapacity: Int = 5): Cache[V] =
    new LruCache[V](maxCapacity, initialCapacity)
}

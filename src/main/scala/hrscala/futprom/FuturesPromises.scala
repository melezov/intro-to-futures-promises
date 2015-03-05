package hrscala.futprom

import scala.concurrent._
import ExecutionContext.Implicits.global

object FuturesPromises {

  /** Returns a Future completed with given value
    */
  def complete[T](value: T): Future[T] = ???

  /** Returns a Future completed with given exception
    */
  def fail(ex: Throwable): Future[Nothing] = ???

  /** Returns a future that never completes
    */
  def neverCompletes[T]: Future[T] = ???

  /** Given a list of futures, return a future of a list of values of all
    * the futures from the list `fs`.
    *
    * The returned future is completed once all futures from the list are
    * completed. The values in the resulting list must be in the same order
    * as futures in `fs`.
    *
    * If any of the futures in the `fs` fail, the resulting future must
    * fail as well.
    */
  def listToFuture[T](fs: List[Future[T]]): Future[List[T]] = ???
}

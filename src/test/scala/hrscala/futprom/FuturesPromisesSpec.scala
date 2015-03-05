package hrscala.futprom

import scala.concurrent._
import ExecutionContext.Implicits.global
import org.scalatest._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class FuturesPromisesSpec extends FlatSpec with Matchers {

  "One of FuturesPromises functions" should "be completed with given value" in {
    val completed = FuturesPromises.complete(42)
    Await.result(completed, 0.millis) should be (42)
  }

  it should "be completed with an exception" in {
    val failed = FuturesPromises.fail(new RuntimeException("failed"))
    val Some(Failure(err)) = failed.value
    err.getMessage should be ("failed")
  }

  it should "never be completed" in {
    val never = FuturesPromises.neverCompletes[Int]
    a [TimeoutException] should be thrownBy Await.result(never, 1.second)
  }

  it should "convert a list of futures to a future of list of values" in {
      val fs1  = Future(1) :: Future { blocking { Thread.sleep(300) }; 2 } :: Future(3) :: Nil
//    val fs1 = List(Future(1), Future(2), Future(3))
    val fXs = FuturesPromises.listToFuture(fs1)

    Await.result(fXs, 1.second) should equal (List(1, 2, 3))

    val fs2 = Future(1) :: Future(sys.error("ouch!")) :: Future(3) :: Nil
    val failed = FuturesPromises.listToFuture(fs2)
    failed onComplete {
      case Success(_) ⇒ fail("listToFuture should produce failed future if any of the futures in the list failed themselves")
      case Failure(_) ⇒ // ok
    }
  }
}



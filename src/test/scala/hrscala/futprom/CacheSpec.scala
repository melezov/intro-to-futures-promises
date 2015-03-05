package hrscala.futprom

import java.util.Random
import java.util.concurrent.{CountDownLatch, Executors}

import org.scalatest._
import org.scalatest.matchers.{Matcher, MatchResult}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import ExecutionContext.Implicits.global

class CacheSpec extends FlatSpec with Matchers {

  "The Cache" should "initially be empty" in {
    val cache = lruCache()
    cache.store shouldBe empty
  }

  it should "store uncached values" in {
    val cache = lruCache[String]()
    cache(1)("A").await should be ("A")
    cache.store.size() should be (1)
  }

  it should "return cached values upon hit" in {
    val cache = lruCache[String]()
    cache(1)("A").await should be ("A")
    cache(1)(sys.error("should not be evaluated"): String).await should be ("A")
  }

  it should "have limited capacity" in {
    val cache = lruCache[String](3)
    cache(1)("A").await should be ("A")
    cache(2)("B").await should be ("B")
    cache(3)("C").await should be ("C")

    cache.size should be (3)

    cache(4)("D")
    Thread.sleep(10)
    cache.get("A") should be (None)
    cache.size should be (3)
  }

  it should "not store exceptions" in {
    val cache = lruCache[String]()
    val cached_1 = cache(1)(sys.error("Failed"): String)
    the [RuntimeException] thrownBy cached_1.await should have message "Failed"
    cache(1)("A").await should be ("A") // ... cached_1 is not really cached
  }

  it should "return uncompleted Futures upon request and put the values there after they are computed" in {
    val cache = lruCache[String]()
    val latch = new CountDownLatch(1)

    val futureA = cache(1, () ⇒ Future {
      latch.await()
      "A"
    })
    val futureX = cache(1)("X")

    cache.get(1).get.isCompleted should be (right = false)
    latch.countDown()
    futureA.await should be ("A")
    futureX.await should be ("A")
    cache.size should be (1)
  }

  it should "be thread-safe" in {
    val cache = lruCache[Int](maxCapacity = 1000)

    // running 10 tracks concurrently
    val views = Future.traverse(Seq.tabulate(10)(identity)) { track ⇒
      Future {
        val arr = Array.fill(1000)(0) // view of the cache
        val rnd = new Random(track)
        (1 to 10000) foreach { i ⇒
          val idx = rnd.nextInt(1000) // random cache index
          val value = cache(idx) {
            Thread.sleep(0)
            rnd.nextInt(1000000) + 1
          }.await
          if (arr(idx) == 0) arr(idx) = value // updating the view of the cache
          else arr(idx) should beConsistentWith (value, track)
        }
        arr
      }
    }.await

    views.transpose foreach { xs ⇒
      xs.filter(_ != 0).reduceLeft { (a, b) ⇒ if (a == b) a else 0 } should not be 0
    }
  }

  def beConsistentWith(right: Int, track: Int) = Matcher { (left: Int) ⇒
    MatchResult(
      left == right,
      s"Cache view is inconsistent (track=$track, expected=$left, read from cache=$right",
      "Cache view si consistent")
  }

  def lruCache[V](maxCapacity: Int = 10, initialCapacity: Int = 3) = new LruCache[V](maxCapacity, initialCapacity)

  implicit class FutureOps[T](future: Future[T]) {
    def await: T = Await.result(future, Duration.Inf)
  }
}

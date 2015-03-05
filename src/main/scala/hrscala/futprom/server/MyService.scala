package hrscala.futprom
package server

import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Actor
import spray.routing._
import spray.http._

import scala.concurrent._
import scala.collection.mutable.{ LinkedHashMap => MMap }

//import scala.concurrent.ExecutionContext.Implicits.global

class MyServiceActor extends Actor with HttpService {
  def actorRefFactory = context

  private implicit val ec = Worker.ec

  def receive = runRoute(
    path("calc-cache" / """\d+""".r) { key =>
      get {
        complete {
          Worker.calculateWithCache(key.toInt) map { result =>
            "Cached result: " + result
          }
        }
      }
    } ~
    path("calc-no-cache" / """\d+""".r) { key =>
      get {
        complete {
          Worker.calculateWithoutCache(key.toInt)
        }
      }
//    } ~
//    path("evict") {
//      get {
//        complete {
//          Future {
//            Worker.clearCache()
//            "Cleared cache!"
//          }
//        }}
    }
  )
}

object Worker {
  val random = new Random()

  implicit val ec =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())


  private def calculateCore(key: Int): String = {
    println(">>> Calculating result for: " + key)
    Thread.sleep(1000 * key + random.nextInt(1000))
    println("<<< Finished calculating result for: " + key)
    s"Probudio sam se iz mrtvih nakon $key sekundi"
  }

  private def calculateCore(key: Int, res: Promise[String], name: String): Unit = {
    println(name + ">>> Calculating result for: " + key)
    Thread.sleep(random.nextInt(1000))

    var index = key * 10
    while (index > 1) {
      index -= 1;
      Thread.sleep(100)
      if (res.isCompleted) {
        println("RES JE COMPLETED, jebes ovo!")
        index = 0;
      }
    }
    println(name + "rezultat: " + res.trySuccess(s"Probudio sam se iz mrtvih nakon $key sekundi"))
    println(name + "<<< Finished calculating result for: " + key)
  }

  def calculateWithoutCache(key: Int)  = {
    Future {
      calculateCore(key)
    }
  }

  def calculateWithCache(key: Int) = {

      val res = Promise[String]
      val kandidatZaRacunica = res.future

      val old = cache.putIfAbsent(key, kandidatZaRacunica)

      if (old == null) {
        Future {
          calculateCore(key, res, "AAA")
        }

        Future {
          calculateCore(key, res, "BBB")
        }

        Future {
          calculateCore(key, res, "CCC")
        }

        kandidatZaRacunica
      }
      else {
        old
      }

   }

  private val cache = new ConcurrentHashMap[Int, Future[String]]

  def clearCache() = {
    cache.clear()
  }
}

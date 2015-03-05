package hrscala.futprom
package server

import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Random

import akka.actor.Actor
import spray.routing._
import spray.http._

import scala.concurrent._
import scala.collection.mutable.{ LinkedHashMap => MMap }

class MyServiceActor extends Actor with HttpService {
  def actorRefFactory = context

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
          Worker.calculateWithoutCache(key.toInt) map { result =>
            "No cache result: " + result
          }
        }
      }
    } ~
    path("clear") {
      get {
        complete {
          Future {
            Worker.clearCache()
            "Cleared cache!"
          }
        }}
    }
  )
}

object Worker {
  val random = new Random()

  private def calculateCore(key: Int): Int = {
    println("Calculating result for: " + key)

    val sleepTime = random.nextInt(1000) + 1000
    Thread.sleep(sleepTime)

    key * 2
  }

  def calculateWithoutCache(key: Int)  = {
    Future(calculateCore(key))
  }

  def calculateWithCache(key: Int) = {
    if (!(cache contains key)) {
      cache(key) = Future {
        calculateCore(key)
      }
    }

    cache(key)
  }

  private val cache = new MMap[Int, Future[Int]]

  def clearCache() = {
    cache.clear()
  }
}

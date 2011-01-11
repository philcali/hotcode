package com.philipcali

import hotcode._
import hotcode.HotSwapCenter._
import scala.actors.remote.RemoteActor

object Main extends Application {
  // This is a bug on the jvm
  RemoteActor.classLoader = this.getClass().getClassLoader()

  val logger = new FileLogger 

  for(i <- 1 to 10) {
    println("Iteration %d" format(i))
    dynamic('main) {
      logger.log("Potentially dynamic code")
    }

    Thread.sleep(1000)
  }

  // Not manditory, but a nice teardown
  // shutdown()
}

object Main2 extends Application {
  println("trying to inject code")

  val logger = new FileLogger
  update('main) {
    logger.log("Changed")
  }

  println("If that failed, let's see if I can at least talk to the actor.")

  import scala.actors.remote.Node
  import scala.actors.Actor.actor
  
  RemoteActor.classLoader = this.getClass.getClassLoader

  actor {
    val a = RemoteActor.select(Node("127.0.0.1", 9000), 'main)
    a ! ListRevisions()
  }
}

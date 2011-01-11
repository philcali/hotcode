package com.philipcali

import hotcode._
import hotcode.HotSwapCenter._

object Main extends Application {
  val logger = new FileLogger 

  for(i <- 1 to 10) {
    println("Iteration %d" format(i))
    dynamic('main) {
      logger.log("Potentially dynamic code")
    }

    Thread.sleep(1000)
  }
}

object Main2 extends Application {
  val logger = new FileLogger
  update('main) {
    logger.log("Changed")
  }
}

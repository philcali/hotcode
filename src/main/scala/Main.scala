package com.philipcali

import hotcode._

import hotcode.HotSwapCenter._

case class Ping()
case class Pong()

object Main extends Application {
  val logger = new FileLogger 

  import scala.actors.remote.RemoteActor._
  import scala.actors.remote.Node
  import scala.actors.Actor._

  var pong = 0

  actor {
    alive(9010)
    register('test, self)

    loop {
      react {
        case Ping() => sender ! Pong()
        case Quit() => {
          logger.log("Made %d" format(pong))
        }
      }
    }
  }

  // Make sure actor is launched on node
  Thread.sleep(1000)

  actor {
    val c = select(Node("localhost", 9010), 'test)
    
    c ! Ping()
    loop {
      react {
        case Pong() => { 
          pong += 1
          sender ! Ping()
          if(pong == 1000) {
            sender ! Quit() 
          }
        }
      }
    }
  }

  // Not manditory, but a nice teardown
  // shutdown()
}

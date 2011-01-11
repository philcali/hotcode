package com.philipcali
package hotcode

import scala.actors.remote.RemoteActor
import scala.actors.remote.Node
import scala.actors.Actor
import Actor._


trait Logger {
  type IO = java.io.PrintStream
  def creation: IO
 
  def withFile(op: IO => Unit) {
    val writer = creation 

    try{ 
      op(writer)
    } finally {
      writer.flush()
    }
  }

  def log(text: String) {
    withFile { logged =>
      val s = "%s\n" format(text)
      logged.print(s)
    }
  }
}

class ConsoleLogger extends Logger {
  def creation = Console.out
}

class FileLogger extends Logger {
  def creation = new IO(new java.io.FileOutputStream("out.txt", true))
}

@serializable
class Computation(op: => Any) {
  def operation = { op }
}
case class Replace(comp: Computation)
case class Perform(key: String = "HEAD")
case class ListRevisions()
case class Quit()

import scala.collection.mutable.{ Map => MMap }

object HotSwapCenter {  

  private val dynamicBlocks = MMap[Symbol, Responder]()

  def dynamic(key: Symbol)(block: => Any) = {
    // If the code is already in the container, then run the code
    val code = if(dynamicBlocks.contains(key)) {
      dynamicBlocks(key)
    } else {
      val c = new Responder(key, new Computation(block))
      c.start
      dynamicBlocks.put(key, c)
      c
    }

    code ! Perform()
  }

  def update(key: Symbol, ip: String = "127.0.0.1")(block: => Any) {
    // First let's see of the actor exists locally
    val code = if(dynamicBlocks.contains(key)) dynamicBlocks(key) 
               else RemoteActor.select(Node(ip, 9000), key)

    println("Received actor")

    //code ! Replace(new Computation(block))
  }

  def shutdown() = {
    dynamicBlocks.values.foreach(_ ! Quit())
  }
}

class Responder(key: Symbol, init: Computation) extends Actor {
  RemoteActor.alive(9000)
  RemoteActor.register(key, self)

  val computations = MMap[String, Computation]("HEAD" -> init) 

  override def act() {
    loop {
      react {
        case Perform(key) => computations.getOrElse(key, computations("HEAD")).operation
        case Replace(comp) => { 
          // Increment revisions
          val revision = {
            val revisions = computations.keys.filter(_ != "HEAD").map(_.toDouble)
            val max = if(revisions.isEmpty) 0.0 else revisions.reduceLeft((i, n) => n.max(i))
            max + 1.0
          }
          val current = computations("HEAD")
          computations.put(revision.toString, current)  
          computations.put("HEAD", comp)
        }
        case ListRevisions() => {
          val logger = new FileLogger
          logger.log("Listing revisions:")
          computations.keys.foreach(logger.log)
        }
        case Quit() => this.exit()
      }
    }
  }
}

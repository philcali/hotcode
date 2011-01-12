package com.philipcali
package hotcode

import scala.actors.remote.RemoteActor
import scala.actors.remote.Node
import scala.actors.Actor
import Actor._

@serializable
class Computation(op: => Unit) {
  def operation = { op }
}
case class Replace(comp: Computation)
case class Perform(key: String = "HEAD")
case class Quit()

import scala.collection.mutable.{ Map => MMap }

object HotSwap {  
  // This is a bug on the jvm
  RemoteActor.classLoader = this.getClass().getClassLoader()

  private val dynamicBlocks = MMap[Symbol, HotSwap]()

  def dynamic(key: Symbol)(block: => Any) = {
    // If the code is already in the container, then run the code
    val code = if(dynamicBlocks.contains(key)) {
      dynamicBlocks(key)
    } else {
      val c = new HotSwap(key, new Computation(block))
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

    code ! Replace(new Computation(block))
  }

  def shutdown() = {
    dynamicBlocks.values.foreach(_ ! Quit())
  }
}

class HotSwap(key: Symbol, init: Computation) extends Actor {
  RemoteActor.alive(9000)
  RemoteActor.register(key, this)

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
        case Quit() => this.exit()
      }
    }
  }
}

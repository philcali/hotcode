package com.philipcali
package hotcode

import context._
import signals._
import scala.actors.remote.RemoteActor
import scala.actors.remote.Node
import scala.actors.Actor
import Actor._

import scala.collection.mutable.{ Map => MMap }
import scala.collection.mutable.{ ListBuffer => LB}

object HotSwap {  
  // This is a bug with how scala handles RemoteActors
  RemoteActor.classLoader = this.getClass().getClassLoader()

  private val dynamicBlocks = MMap[Symbol, HotSwap]()

  def dynamic(key: Symbol, ctx: Context = Context())(block: => Any): Any = {
    // If the code is already in the container, then run the code
    val code = if(dynamicBlocks.contains(key)) {
      dynamicBlocks(key)
    } else {
      val c = new HotSwap(key, new ZeroParamComputaion(block))
      c.start
      dynamicBlocks.put(key,c)
      c
    }

    code !? (1000, Perform) match {
      case Some(ops) => ops.asInstanceOf[List[Computation]].foreach { comp => 
        if(comp.description == "original") { block } else comp.op(ctx)
      }
      case _ => block
    }
  }

  def dynamic(key: Symbol, ctx: Any*)(block: => Any): Any = {
    val tups = for((elem, index) <- ctx.zipWithIndex) yield((index.toString, elem))
    dynamic(key, Context(tups: _*))(block)
  }

  def find(key: Symbol, ip: String = "127.0.0.1") = {
    // First let's see of the actor exists locally
    if(dynamicBlocks.contains(key)) dynamicBlocks(key) 
    else RemoteActor.select(Node(ip, 9000), key)
  }

  def insert(key: Symbol, ip: String = "127.0.0.1", 
             description: String="insert", at: Int=0)(block: Context => Any) {
    val code = find(key, ip)
    code ! Insert(Computation(block, description), at)    
  }

  def update(key: Symbol, ip: String = "127.0.0.1")(block: Context => Any) {
    insert(key, ip, "replace")(block)
  }

  def prepend(key: Symbol, ip: String= "127.0.0.1")(block: Context => Any) {
    insert(key, ip, "prepend")(block)
  }

  def append(key: Symbol, ip: String="127.0.0.1")(block: Context => Any) {
    insert(key, ip, "append")(block)
  }

  def reset(key: Symbol, ip: String="127.0.0.1") {
    find(key, ip) ! Reset
  }

  def shutdown() = {
    dynamicBlocks.values.foreach(_ ! Quit)
  }
}

class HotSwap(key: Symbol, init: Computation) extends Actor {
  RemoteActor.alive(9000)
  RemoteActor.register(key, this)

  val computations = LB[Computation](init)

  def replaceable(comp: Computation) = comp.description == "original" || 
                                       comp.description == "replace"

  override def act() {
    loop {
      react {
        case Perform => sender ! (if(computations.isEmpty) None else computations.toList)
        case Insert(comp, index) => comp.description match {
          case "replace" => {
            val i = computations.indexWhere(replaceable)
            computations(i) = comp
          }
          case "append" => computations += comp
          case "prepend" => comp +=: computations
          case "insert" => if(replaceable(computations(index))) {
            computations.insert(index, comp)
          } else computations(index) = comp
          case _ => computations.insert(index, comp)
        }  
        case Reset => computations.clear(); computations += init
        case Quit => this.exit()
      }
    }
  }
}

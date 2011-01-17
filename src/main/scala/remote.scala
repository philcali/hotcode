package com.philipcali
package remote 

import signals._
import scala.actors.remote.RemoteActor
import scala.actors.Actor
import Actor._
import context._

trait RemoteControlled {
  RemoteActor.classLoader = this.getClass.getClassLoader

  def id = 'remote
  def port = 1024

  def respond(msg: Any) = Actor.sender ! msg
  def selfContext = Context("self" -> this)

  def responder: PartialFunction[Any, Unit] = {
    case Computation(fun, descript) => 
      val results = fun(selfContext)
      respond(Response(results))
  }


  val server = actor {
    RemoteActor.alive(port)
    RemoteActor.register(id, self)

    loop {
      react {
        responder
      }
    }
  }

}

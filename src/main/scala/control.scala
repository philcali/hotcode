package com.philipcali
package control

import signals._
import context._
import scala.actors.remote.RemoteActor
import scala.actors.remote.Node

object Controller {
  RemoteActor.classLoader = this.getClass.getClassLoader

  def connect(id: Symbol='remote, ip: String="127.0.0.1", port:Int=1024) = {
    new Controller(id, ip, port)
  }

  def apply(id: Symbol='remote, ip: String="127.0.0.1", port:Int=1024) = {
    connect(id, ip, port)
  }
}

class Controller(id: Symbol='remote, ip: String="127.0.0.1", port:Int=1024) {
  val remote = RemoteActor.select(Node(ip, port), id)

  def signalFuture(sig: Signal): scala.actors.Future[Any] = {
    remote !! sig
  }

  def signal(sig: Signal, msec: Long=1000L): Option[Any] = {
    remote !? (msec, sig)
  }
}

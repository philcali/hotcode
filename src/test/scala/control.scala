package com.philipcali
package test

import control.Controller
import signals._
import context._

import scala.actors.remote.RemoteActor
import scala.actors.Actor._

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

case class Ack(text: String) extends Signal

class ControllerSpec extends FlatSpec with ShouldMatchers {
  RemoteActor.classLoader = this.getClass.getClassLoader

  val remote = actor {
    RemoteActor.alive(1024)
    RemoteActor.register('remote, self)

    val ctx = Context("name" -> "Philip Cali")

    loop {
      react {
        case Computation(comp, desc) => 
          val results = comp(ctx) 
          sender ! results
        case Ack(text) => assert(text == "connected")
      }
    }
  }

  "Controller" should "connect to a remote actor" in {
    Controller connect('remote) signal Ack("connected")
  }

  it should "be able to send arbitrary computations" in {
    val comp = new ZeroParamComputaion({
      val writer = new java.io.FileWriter("test.txt")
      writer.write("Blah Blah Blah")
      writer.close()
    })

    Controller('remote) signal(comp) match {
      case _ => {
        import scala.io.Source.{fromFile => open}
        open("test.txt").getLines.next should be === "Blah Blah Blah"
        new java.io.File("test.txt").delete
      }
    }
  }

  it should "be able to run code Context aware" in {
    Controller('remote) signal(Computation({
      case Context(name: String) => {
        val writer = new java.io.FileWriter("test.txt")
        writer.write(name)
        writer.close()
      }
    }, "A test Computation")) match {
      case _ => {
        import scala.io.Source.{fromFile => open}
        open("test.txt").getLines.next should be === "Philip Cali"
        new java.io.File("test.txt").delete
      }
    }
  }

  it should "return a future to work with" in {
    val result = Controller('remote) signalFuture(Computation({
      case Context(name: String) =>
        val names = name.split(" ")
        "%s %s %s" format(names(0), "Michael", names(1))
    }))

    result() should be === "Philip Michael Cali"
  }
}

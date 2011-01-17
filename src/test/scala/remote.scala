package com.philipcali
package test

import remote._
import signals._
import context._

import org.scalatest.FlatSpec
import scala.actors.remote.{RemoteActor, Node}
import scala.io.Source.{fromFile => open}
import scala.actors.Actor._


class Person(n: String, a: Int) extends RemoteControlled {
  require(a < 150)
  def name = {
    n.toUpperCase.split(" ").map(s => s(0) + s.drop(0).toString.toLowerCase).mkString(" ")
  }
  def age = a
}

case class Something(text: String) extends Signal
case object Respond extends Signal

class Custom(sym: Symbol, p: Int) extends RemoteControlled {
  override def id = sym
  override def port = p 

  def log(text: String) {
    val writer = new java.io.FileWriter("test.txt")
    writer.write(text)
    writer.close()
  }

  override def responder = {
    case Something(text) => 
      log("got %s" format(text))
      respond(Respond)
  }
}

class ResponderSpec extends FlatSpec {

  "Responder" should "react to portable function" in {
    val person = new Person("philip cali", 25)

    val responder = RemoteActor.select(Node("127.0.0.1", 1024), 'remote)

    responder ! Computation ({
      case Context(self: Person) => 
        if(self.name != "Philip Cali" && self.age != 25)
        throw new Exception("Invalid params")
    }, "A test")
  }

  it should "able to handle custom signals" in {
    val custom = new Custom('remote, 1024)
    val responder = RemoteActor.select(Node("127.0.0.1", 1024), 'remote)

    val testString = "Teddy had a little sheepy"
    responder !? (1000, Something(testString)) match {
      case Some(Respond) => if(open("test.txt").getLines.next != "got %s".format (testString)) 
        throw new Exception("Expected %s" format(testString))
      case _ => throw new Exception("Got nothing back :(")
    }

    new java.io.File("test.txt").delete
  }


  it should "handle the same request" in {
    val responder = RemoteActor.select(Node("127.0.0.1", 1024), 'remote)

    responder !? (1000, Something("Nothing!")) match {
      case Some(Respond) => if(open("test.txt").getLines.next != "got Nothing!".format("Nothing!"))
        throw new Exception("Not what was expected")
      case _ => throw new Exception("Got nothing back :(")
    }

    new java.io.File("test.txt").delete
  }


  "Standard Actor behavior" should "mimic the Responder" in {
    val server = actor {
      RemoteActor.alive(1024)
      RemoteActor.register('remote, self)

      loop {
        react {
          case Something(text) => assert(text == "Test")
            sender ! Respond
        }
      }
    }
    

    val client = RemoteActor.select(Node("127.0.0.1", 1024), 'remote)

    client !? (1000, Something("Test")) match {
      case Some(Respond) => println("Got it back")
      case _ => throw new Exception("Didn't work")
    }
  }
}

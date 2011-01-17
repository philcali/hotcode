package com.philipcali
package test

import hotcode.HotSwap._
import context.Context
import org.scalatest.{FlatSpec, BeforeAndAfterEach}
import org.scalatest.matchers.ShouldMatchers
import scala.io.Source.{fromFile => open}

class HotSwapSpec extends FlatSpec with ShouldMatchers with BeforeAndAfterEach {
  override def afterEach(config: Map[String, Any]) {
    new java.io.File("test.txt").delete
  }

  def log(s: String) {
    val writer = new java.io.FileWriter("test.txt", true)
    writer.write(s)
    writer.close()
  }

  def testFun(s: String) = {
    dynamic('test, Context("s" -> s)) {
      log("Writing some texts %s" format(s))
    }
  }

  "Dynamic" should "run as normal code" in {
    testFun("bob")
    open("test.txt").getLines.next should be === "Writing some texts bob"
  }

  it should "change original code" in {
    update('test) { ctx =>
      val s: String = ctx("s")
      log("Don't fart on me %s" format(s))
    }

    testFun("billy")
    open("test.txt").getLines.next should be === "Don't fart on me billy"
  }

  "Reset" should "return the dynamic code to its default state" in {
    reset('test)

    testFun("bob")
    open("test.txt").getLines.next should be === "Writing some texts bob"
  }

  "Append" should "add code after dynamic call" in {
    append('test) { ctx => 
      log(", dude")
    }

    testFun("bob")
    open("test.txt").getLines.next should be === "Writing some texts bob, dude"
  }

  "Prepend" should "add code before dynamic call" in {
    prepend('test) { ctx =>
      log("Ignore me %s " format(ctx.params("s")))
    }

    testFun("bob")
    open("test.txt").getLines.next should be === "Ignore me bob Writing some texts bob, dude"
  }

  "Reset" should "remove appended code" in {
    reset('test)

    testFun("bob")
    open("test.txt").getLines.next should be === "Writing some texts bob"
  }
}

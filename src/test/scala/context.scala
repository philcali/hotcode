package com.philipcali
package test

import context.Context
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ContextSpec extends FlatSpec with ShouldMatchers {
  "Context" should "apply into Context class" in {
    val ctx = Context("name" -> "Philip Cali", "age" -> 25)

    ctx.params.size should be === 2
  }

  it should "be easy to pull values from the Context" in {
    val ctx = Context("car" -> "Mustang", "speed" -> 10000)
    
    val speed: Int = ctx("speed")
    speed should be === 10000
  }

  it should "be exacted just as easy" in {
    Context("day" -> "Friday", "time" -> "to party") match {
      case Context(day, time) =>
        day should be === "Friday"
        time should be === "to party"
    } 
  }
}

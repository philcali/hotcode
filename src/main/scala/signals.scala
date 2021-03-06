package com.philipcali
package signals

import context._

trait Signal
trait Response[What] extends Signal {
  def unqpply(response: Any) = response.asInstanceOf[What]
}

// Arbtrary computations
case class Computation(op: Context => Any, description: String="Standard computation") extends Signal
class ZeroParamComputaion(block: => Any) extends Computation((ctx: Context) => block, "original")

// Hot swap signals
case class Insert(comp: Computation, at: Int=0) extends Signal
case object Perform extends Signal
case object Reset extends Signal
case object Quit extends Signal

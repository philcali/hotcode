package com.philipcali

import hotcode.HotSwap._
import context.Context

trait Logger {
  type IO = java.io.PrintStream
  def creation: IO
 
  def withFile(op: IO => Unit) {
    val writer = creation 

    try{ 
      op(writer)
    } finally {
      writer.close()
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

object Main extends Application {
  val logger = new FileLogger 

  for(i <- 1 to 10) {
    println("Iteration %d" format(i))
    dynamic('main, logger, i) {
      logger.log("Potentially dynamic code: %d" format(i))
    }

    Thread.sleep(1000)
  }
}

object Main2 extends Application { 
  update('main) {
    case Context(logger: Logger, x: Int) => 
      logger.log("Changed ... at %d" format(x))
  }
}

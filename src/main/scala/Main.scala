package com.philipcali

import hotcode.HotSwap._
import hotcode.Context

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
    dynamic('main, Context(("log" -> logger), ("i" -> i))) { ctx =>
      val l: Logger = ctx("log")
      val x: Int = ctx("i")
      l.log("Potentially dynamic code: %d" format(x))
    }

    Thread.sleep(1000)
  }
}

object Main2 extends Application {
  update('main) { ctx =>
    val l: Logger = ctx("log")
    val x: Int = ctx("i")
    l.log("Changed... at %d" format(x))
  }
}

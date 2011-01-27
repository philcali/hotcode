# Remote Control Library

The remote control library was an inception that originally started [here],
and details ironed out on [my blog]. This library gives a client the ability
to swap out dynamic portions of code, or send arbitrary commands to server
(which is just a nice wrapper for Scala `RemoteActor`s).


## Requirements

  * sbt 0.7.4

## Changing Code Real-Time

Changing server side code is fairly easy. In your long running process (web server,
irc robot, computer game, etc), wrap your potentially dynamic portions like so:

    import com.philipcali.hotcode.HotSwap._

    class LongRunningProcess {
      // Implementaions details
      def onMessage(msg: String) {
        dynamic('message, msg) {
          println("Printing out %s" format(msg))
        }
      }
    }

Calling `dynamic`, labels that code as replaceable. Change this code like this:

import com.philipcali.hotcode.HotSwap._

update('message) {
  case Context(msg: String) =>
    println("Changing code, dude! %s" format(msg))
}

That's all there is to it.

## Executing Commands

Using our `LongRunningProcess` example, simple inherit from the `RemoteControlled` trait.

    class LongRunningProcess extends RemoteControlled {
      ...
      def name = "Remote Control"
      def version = "0.1"
    }

Clients can send arbitrary calls through the `Controller` object.

    import com.philipcali.control.Controller._
    import com.philipcali.signals._

    // Sends one way command
    connect('remote) signal(Computation({
      case _ => println("Doing it big!")
    }))

    // Sends command expecting results
    connect('remote) signal(Computation({
      case Context(self: LongRunningProcess) =>
        "%s v%s" format(self.name, self.version)
    })) match {
      // Will print out Remote Control v0.1 
      case Some(resp: String) => println(resp)
    }

Use at your own risk.

[my blog]: http://philcalicode.blogspot.com/2011/01/remote-control-v01-alpha.html
[here]: http://yarivsblog.blogspot.com/2008/05/erlang-vs-scala.html

package com.philipcali
package context

object Context {
  def apply(ctxParams: (String, Any)*) = {
    new Context(ctxParams.toMap)
  }

  def unapplySeq(s: Context): Option[Seq[Any]] = {
    Some(s.params.values.toSeq)
  }
}

class Context(val params: Map[String, Any]) {
  def apply[A](key: String) = params(key).asInstanceOf[A]
}

import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  override def mainClass = Some("com.philipcali.Main")
  override def compileOptions = super.compileOptions ++ Seq(Unchecked)
  
  val scalatest = "org.scalatest" % "scalatest" % "1.2"
}

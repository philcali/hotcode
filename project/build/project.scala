import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  override def mainClass = Some("com.philipcali.Main")
}

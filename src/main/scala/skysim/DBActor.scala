package skysim

import java.sql.{Connection, DriverManager, Statement}

import akka.actor.Props
import com.mysql.cj.jdbc.NonRegisteringDriver
import skysim.DBActor._
import skysim.Log._

import scala.collection.mutable.ListBuffer

object DBActor {
  def props: Props = Props(new DBActor)

  case class StoreDb(seq: Seq[CharState])

  case class InitDb(
                     url: String = "jdbc:mysql://localhost:8889/mysql",
                     driver: String = "com.mysql.jdbc.Driver",
                     username: String = "root",
                     password: String = "root"
                   )

  object InitDbDone

  case class ExecSql(sql: String)

  case class SqlResult(results: List[Map[String, Any]], affected: Int)

  val setupSQL: String =
    """
drop table if exists skysim;

CREATE TABLE `skysim` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `name` text CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `parent` text CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `state` text CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `x` bigint(11) NOT NULL DEFAULT 0,
  `y` bigint(11) NOT NULL DEFAULT 0,
  `data` text CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `timestamp` bigint(22) NOT NULL,
  PRIMARY KEY (`id`)
);

drop table if exists skysim_jobs;

CREATE TABLE `skysim_jobs` (
  `job` varchar(255) NOT NULL,
  `states` text CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`job`)
);

insert into skysim_jobs values
('lumberjack', 'idle, chopping, eating, selling, sleeping, idle'),
('citizen',    'idle, buying, eating, sleeping, idle'),
('alchemist',  'idle, selling, eating, collecting, sleeping, idle'),
('mage',       'idle, studying, eating, practicing, sleeping, idle'),
('merchant',   'idle, selling, eating, sleeping, idle');
"""
}

class DBActor extends SimActor {
  var connectionData: InitDb = _

  def connection: Connection =
    DriverManager.getConnection(
      this.connectionData.url, this.connectionData.username, this.connectionData.password
    )

  def r(name: String) = "'" + name.replace("'", "''") + "'"

  def m(data: Map[String, Int]): String = r(
    data.toSeq.map(x => x._2 + ":" + x._1).mkString("|")
  )

  override def receive: Receive = {
    case i@InitDb(url, driver, username, password) =>
      this.connectionData = i

      try {
        //Class.forName(driver)
        //
        DriverManager.registerDriver(new NonRegisteringDriver)

        val statement = connection.createStatement
        val rs = statement.executeQuery("select count(*) from skysim;") //SELECT host, user FROM user")
        var count = 0
        while (rs.next) {
          count += 1
          //val host = rs.getString("host")
          //val user = rs.getString("user")
          //println("host = %s, user = %s".format(host, user))
        }
        println("skysim: " + count)
      } catch {
        case e: Exception =>
          e.printStackTrace()
      } finally {
        connection.close()
      }
      sender ! InitDbDone

    case StoreDb(seq) =>
      val now = System.currentTimeMillis
      val values = seq.map { x =>
        "(" + r(x.name) + ", " + r(x.city) + ", " + r(x.state) + ", " + x.x + ", " + x.y + ", " + m(x.data) + ", " + now + ")"
      }.mkString(", ")

      val sql =
        s"""
INSERT INTO skysim (parent, name, state, x, y, data, timestamp)
VALUES $values
ON DUPLICATE KEY UPDATE parent=parent,x=x,y=y,state=state,data=data,timestamp=timestamp;
          """
      this.self ! ExecSql(sql)

    case ExecSql(sql) =>
      try {
        val statement = connection.createStatement
        println(sql)
        if (sql.toLowerCase.startsWith("select ")) {
          val buf: ListBuffer[Map[String, Any]] = execSql(sql, statement)
          sender() ! SqlResult(buf.toList, 0)

        } else {
          val res = statement.executeUpdate(sql)
          sender() ! SqlResult(Nil, res)
          println("sql update: " + res)
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
      } finally {
        connection.close()
      }

    case SqlResult(_, _) => ()

    case other => sys.error(this.self.path + " UNKNOWN " + other)
  }

  def execSql(sql: String, statement: Statement): ListBuffer[Map[String, Any]] = {
    val rs = statement.executeQuery(sql)
    val meta = rs.getMetaData
    val fields = 1.to(meta.getColumnCount).map(i =>
      meta.getColumnName(i)
    ).toList
    var i = 0
    val buf = ListBuffer[Map[String, Any]]()
    while (rs.next) {
      val res = fields.map(x => x -> rs.getObject(x))
      buf += res.toMap
      println(i + ": " + res.map { case (a, b) => a + " -> " + b }.mkString(", "))
      i += 1
    }
    buf
  }
}
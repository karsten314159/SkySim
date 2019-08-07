package skysim

import java.sql.{Connection, DriverManager}

import akka.actor.Props
import com.mysql.cj.jdbc.NonRegisteringDriver
import skysim.DBActor.{InitDb, StoreDb}

object DBActor {
  def props: Props = Props(new DBActor)

  case class StoreDb(seq: Seq[CharState])

  case class InitDb(
                     url: String = "jdbc:mysql://localhost:8889/mysql",
                     driver: String = "com.mysql.jdbc.Driver",
                     username: String = "root",
                     password: String = "root"
                   )

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
);"""

  val insert: String =
    """
  INSERT INTO skysim (`name`, `parent`, `state`, `x`, `y`, `data`, `timestamp`)
  VALUES ('Uriel', 'whiterun', 'idle', '0', '2', '[]', '42');
"""

}

class DBActor extends SimActor {
  var connectionData: InitDb = _

  def connection: Connection =
    DriverManager.getConnection(
      this.connectionData.url, this.connectionData.username, this.connectionData.password
    )

  def r(name: String) = "'" + name.replace("'", "''") + "'"

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

    case StoreDb(seq) =>

      try {
        val statement = connection.createStatement
        val now = System.currentTimeMillis
        val sql =
          s"""
INSERT INTO skysim (name, parent, state, x, y, data, timestamp)
VALUES ${
            seq.map(x =>
              "(" + r(x.name) + ", " + r(x.city) + ", " + r(x.state) + ", " + x.x + ", " + x.y + ", '[]', " + now + ")"
            ).mkString(", ")
          }
ON DUPLICATE KEY UPDATE parent=parent,x=x,y=y,state=state,data=data,timestamp=timestamp;
          """
        println(sql)
        val rs = statement.executeUpdate(sql)

      } catch {
        case e: Exception =>
          e.printStackTrace()
      } finally {
        connection.close()
      }

    case other => sys.error(this.self.path + " UNKNOWN " + other)
  }
}
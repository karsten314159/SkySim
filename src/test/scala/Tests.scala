import org.junit.Test
import skysim.Program

class Tests {
  @Test def test(): Unit = {
    //    sys.error("yikes")
    val url = System.getenv("url")
    val username = System.getenv("username")
    val password = System.getenv("password")
    Program.main(Array(url, username, password))
  }
}

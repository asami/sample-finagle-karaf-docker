package sample.osgi

import org.osgi.framework._
import com.twitter.finagle.ListeningServer
import sample._

class Activator extends BundleActivator {
  var server: Option[ListeningServer] = None

  override def start(context: BundleContext) {
    server = Some(Server.start())
  }

  override def stop(context: BundleContext) {
    server.foreach(Server.stop)
  }
}

package sample

import com.twitter.finagle.{Http, Service, ListeningServer}
import com.twitter.util.{Await, Future}
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http._

object Server {
  val service = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest): Future[HttpResponse] =
      Future.value(new DefaultHttpResponse(
        req.getProtocolVersion, HttpResponseStatus.OK))
  }

  def start(): ListeningServer = {
    Http.serve(":8080", service)
  }

  def stop(server: ListeningServer) {
    server.close()
  }

  def main(args: Array[String]) {
    val server = start()
    Await.ready(server)
  }
}

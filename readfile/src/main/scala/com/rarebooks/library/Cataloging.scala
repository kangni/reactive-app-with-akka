package com.rarebooks.library

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Cataloging extends App {
  implicit val system = ActorSystem("catalog-loader")
  // ActorMaterializer 物化器的任务就是将流程转换成要被 Actor 执行的处理器.
  implicit val materializer = ActorMaterializer()

  var file = Paths.get("books.csv")

  var result = FileIO.fromPath(file)
      .to(Sink.foreach(println(_)))  // 附加处理槽
      .run() // 启动流处理

  // 等待流处理完成
  Await.ready(result, Duration.Inf)
  system.terminate()
}

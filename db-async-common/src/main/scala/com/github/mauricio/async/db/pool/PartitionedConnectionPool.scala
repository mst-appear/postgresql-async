package com.github.mauricio.async.db.pool;

import com.github.mauricio.async.db.util.ExecutorServiceUtils
import com.github.mauricio.async.db.{ChunkedBlob, QueryResult, Connection}
import scala.concurrent.{ExecutionContext, Future}

class PartitionedConnectionPool[T <: Connection](
    factory: ObjectFactory[T],
    configuration: PoolConfiguration,
    numberOfPartitions: Int,
    executionContext: ExecutionContext = ExecutorServiceUtils.CachedExecutionContext)
    extends PartitionedAsyncObjectPool[T](factory, configuration, numberOfPartitions)
    with Connection {

    def disconnect: Future[Connection] = if (this.isConnected) {
        this.close.map(item => this)(executionContext)
    } else {
        Future.successful(this)
    }

    def connect: Future[Connection] = Future.successful(this)

    def isConnected: Boolean = !this.isClosed

    def sendQuery(query: String): Future[QueryResult] =
        this.use(_.sendQuery(query))(executionContext)

    def sendPreparedStatement(query: String, values: Seq[Any] = List()): Future[QueryResult] =
        this.use(_.sendPreparedStatement(query, values))(executionContext)

    // TODO do not giveBack connection until the blob is finished
    def sendPreparedStatementWithBlob(query: String, values: Seq[Any]): Future[ChunkedBlob] = {
        implicit val _executionContext: ExecutionContext = executionContext
        take.flatMap { item =>
            item.sendPreparedStatementWithBlob(query, values) recoverWith {
                case error => giveBack(item).flatMap(_ => Future.failed(error))
            } flatMap { res =>
                giveBack(item).map { _ => res}
            }
        }
    }

    override def inTransaction[A](f: Connection => Future[A])(implicit context: ExecutionContext = executionContext): Future[A] =
        this.use(_.inTransaction[A](f)(context))(executionContext)
}

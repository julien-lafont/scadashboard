package utils

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable
import scala.concurrent.{Promise, ExecutionContext, Future}

package object rich {

  implicit class RichFutureEither[A, B](val f: Future[Either[A, B]]) extends AnyVal {
    def flatMapRight[C](mapping: B => Future[Either[A, C]])(implicit ec: ExecutionContext): Future[Either[A, C]] = {
      f.flatMap {
        case Left(o) => Future.successful(Left(o))
        case Right(o) => mapping(o)
      }
    }

    def flatMapLeft[C](mapping: A => Future[Either[C, B]])(implicit ec: ExecutionContext): Future[Either[C, B]] = {
      f.flatMap {
        case Left(o) => mapping(o)
        case Right(o) => Future.successful(Right(o))
      }
    }
  }

  implicit class FutureOps(FutureCompanion: Future.type) {

    import scala.language.higherKinds

    /**
     * Like Future.traverse, but elements are traversed sequentially.
     * Useful only if `f` has non independent side-effects.
     */
    def traverseSeq[F[X] <: Traversable[X], A, B](as: F[A])(f: A => Future[B])(implicit cbf: CanBuildFrom[F[A], B, F[B]], executor: ExecutionContext): Future[F[B]] = {
      as.foldLeft(Future.successful(cbf(as))) { (fr, a) =>
        for {
          r <- fr
          b <- f(a)
        } yield r += b
      } map (_.result())
    }

    /**
     * Splits a Traversable into chunks of `N` elements, and traverse these chunks sequentially
     * (note that the elements _within_ a chunk are traversed in parallel, though)
     */
    def traverseSeqChunk[F[X] <: Traversable[X], A, B](as: F[A])(N: Int)(f: A => Future[B])(implicit cbf: CanBuildFrom[F[A], B, F[B]], executor: ExecutionContext): Future[F[B]] = {
      as.toStream.grouped(N).foldLeft(Future.successful(cbf(as))) { (fr, a) =>
        for {
          r <- fr
          b <- Future.sequence(a.map(f))
        } yield r ++= b
      } map (_.result())
    }

    /**
     * Sequence a Map[B, Future[A]] into a Future[Map[B, A]]
     */
    def sequenceMap[A, B](in: Map[B, Future[A]])(implicit executor: ExecutionContext): Future[Map[B, A]] = {
      val mb = new mutable.MapBuilder[B,A, Map[B,A]](Map())
      in.foldLeft(Promise.successful(mb).future) {
        (fr, fa) => for (r <- fr; a <- fa._2.asInstanceOf[Future[A]]) yield r += ((fa._1, a))
      } map (_.result())
    }

    def flatten[X](doubleFut: Future[Future[X]])(implicit ec: ExecutionContext): Future[X] = {
      doubleFut.flatMap(fut => fut)
    }

  }

}

package teleporter.model

import munit.{FunSuite, ScalaCheckSuite}
import teleporter.model._
import cats.implicits._
import org.scalacheck.Prop._
import org.scalacheck.Gen
import cats.data.NonEmptyList
import scala.util.chaining._

class ModelTest extends FunSuite with ScalaCheckSuite:
  test("Empty name should return empty model error") {
    assert(
      Name("")
        == NonEmptyList.of(Errors.Empty).asLeft
        && Name("").isError,
      "empty name should return empty error"
    )
  }

  test("Hello name should return Name") {
    assert(
      Name("hello").isNotError,
      "hello name should return Name, not error"
    )
  }

  property("for every non empty string name is not error") {
    val nonEmptyGen = Gen.alphaStr.suchThat(_.nonEmpty)
    forAll(nonEmptyGen) { (s: String) =>
      Name(s).isNotError
    }
  }

  property("for every nonempty name, show will return same name as string") {
    val nonEmptyGen = Gen.alphaStr.suchThat(_.nonEmpty)
    forAll(nonEmptyGen) { (s: String) =>
      Name(s).map(_.show) == s.asRight
    }
  }

  property("for every teleporter with valid name and args") {
    val nonEmptyGen = Gen.alphaStr.suchThat(_.nonEmpty)
    forAll(nonEmptyGen, nonEmptyGen, Gen.listOf(nonEmptyGen)) {
      (s: String, app: String, args: List[String]) =>
        Teleporter.createRaw(s, app, args).isNotError
    }
  }
end ModelTest

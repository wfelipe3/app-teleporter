package teleporter.adapter.trie

import scala.util.chaining._

object Trie:

  case class Node(isValue: Boolean = false, values: Map[Char, Node])

  def empty = Node(isValue = false, values = Map.empty)
  def node(values: (Char, Node)*) = Node(false, values.toMap)
  def vNode(values: (Char, Node)*) = Node(true, values.toMap)

  extension (trie: Node)
    def get(c: Char) =
      trie.values.get(c)

    def add(s: String): Node =
      s.toList match
        case x :: xs =>
          trie
            .get(x)
            .getOrElse(empty)
            .pipe { node =>
              Node(node.isValue, trie.values + (x -> node.add(xs.mkString)))
            }
        case Nil =>
          Node(true, trie.values)

    def contains(text: String): Boolean =
      text.toList match
        case x :: xs =>
          trie
            .get(x)
            .fold(false) { n =>
              n.contains(xs.mkString)
            }
        case Nil =>
          trie.isValue

    def prefixesMatchingString(prefix: String) =
      trie.prefixTries(prefix, "", Set.empty).pipe {
        case (node, actual, found) =>
          found ++ node.depthSearch(actual)
      }

    def stringMatchingRegex(prefix: String): Set[String] =
      prefixTriesComplete(prefix, "", Set.empty)
        .map { case (node, actual, found) =>
          found ++ node.depthSearch(actual)
        }
        .getOrElse(Set.empty)

    def fzf(term: String): Set[String] =
      trie
        .prefixesMatchingString(term)
        .filter(w => isInTerm(w.toList, term.toList))

    private def depthSearch(
        s: String,
        found: Set[String] = Set.empty
    ): Set[String] =
      trie.fold(found)(_ => found + s).pipe { actualFound =>
        if trie.values.isEmpty then actualFound
        else
          trie.values.toSet.flatMap { case (c, n) =>
            n.depthSearch(s :+ c, actualFound)
          }
      }

    private def isInTerm(word: List[Char], term: List[Char]): Boolean =
      if word.isEmpty then false
      else
        term match
          case x :: xs =>
            isInTerm(word.toList.dropWhile(_ != x), xs)
          case Nil => true

    private def fold[A](ifEmpty: => A)(ifNotEmpty: Map[Char, Node] => A) =
      if trie.isValue then ifNotEmpty(trie.values)
      else ifEmpty

    private def prefixTries(
        prefix: String,
        actual: String,
        found: Set[String]
    ): (Node, String, Set[String]) =
      trie.fold(found)(_ => found + actual).pipe { actualFound =>
        prefix.toList match
          case x :: xs =>
            trie
              .get(x)
              .map { t =>
                t.prefixTries(xs.mkString, actual + x, actualFound)
              }
              .getOrElse(trie, actual, actualFound)
          case Nil =>
            (trie, actual, actualFound)
      }

    private def prefixTriesComplete(
        prefix: String,
        actual: String,
        found: Set[String]
    ): Option[(Node, String, Set[String])] =
      fold(found)(_ => found + actual).pipe { actualFound =>
        prefix.toList match
          case x :: xs =>
            trie.get(x).flatMap { t =>
              t.prefixTriesComplete(xs.mkString, actual + x, actualFound)
            }
          case Nil =>
            Some(trie, actual, actualFound)
      }

end Trie

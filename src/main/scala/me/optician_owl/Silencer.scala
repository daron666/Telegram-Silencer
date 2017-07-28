package me.optician_owl

import java.time.ZonedDateTime

import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.models.{ChatType, Message}

import scala.collection.mutable
import scala.io.Source

object Silencer {

  // ToDo Hide key
  System.setProperty("BOT_TOKEN", "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU")

  // ToDO persistence
  private val stats: mutable.Map[Int, Int] = mutable.Map()

  def main(args: Array[String]): Unit = {
    object SafeBot extends TelegramBot with Polling with Commands with ChatActions {
      // Use 'def' or 'lazy val' for the token, using a plain 'val' may/will
      // lead to initialization order issues.
      // Fetch the token from an environment variable or untracked file.
//      lazy val token = scala.util.Properties
//                       .envOrNone("BOT_TOKEN")
//                       .getOrElse(Source.fromFile("bot.token").getLines().mkString)

      lazy val token = "408189074:AAGtJQ7clw9eS9NES-sZxYWcA2ZTfyuULlU"

      // Todo use State
      // Todo is it possible to load group history
      // ToDo is it possible to request list of administrators
      // Todo add rules as some general approach
      val react: Message => Unit = {
        case m if m.chat.`type` == ChatType.Group || m.chat.`type` == ChatType.Supergroup =>
          m.from.foreach(u => stats += (u.id -> (stats.getOrElse(u.id, 0) + 1)))
          println(stats)
          println(m)
          println(m.text)
        case msg =>
          println(stats)
          println(msg)
          println(msg.text)
      }

      onCommand("/hello") { implicit msg =>
        reply("My token is SAFE!")
      }

      onMessage(react)
      onEditedMessage(react)
    }

    SafeBot.run()
  }
}

object BorderGuard {
  def validate(msg: String, chat: Long, user: Long) = {
    searchEvidences andThen fileFactsAndStats andThen judge andThen react
  }

  val judge: Facts => Boolean =
    facts => Rule.codex.foldLeft(false)((acc, el) => acc || el(facts))

  // Todo distinguish user and channel
  // Todo match telegram link via regex
  // Todo match domains by domain lists
  val searchEvidences: String => List[Evidence] = msg =>
    msg.split("\\s").collect{
      case x if x.startsWith("@") => TelegramLink
      case x if x.startsWith("http://") || x.startsWith("https://") => OuterLink
    }.toList

  val fileFactsAndStats = ???

  val react = ???
}

trait Rule {
  def apply(facts: Facts): Boolean
}

object NoviceAndSpammer extends Rule {
  override def apply(facts: Facts): Boolean = {
    // Danger place
    // ToDo check existence of stats
    val chatStats = facts.userStats.chatStats(facts.chat)

    facts.evidences.nonEmpty &&
    chatStats.amountOfMessages <= 10 &&
    chatStats.firstAppearance.isAfter(ZonedDateTime.now().minusMonths(1))
  }
}

object Rule {
  val codex: List[Rule] = List(NoviceAndSpammer)
}

case class Facts(userStats: UserStats, evidences: List[Evidence], chat: Long)

case class UserStats(
    firstAppearance: ZonedDateTime,
    amountOfMessages: Int,
    offences: Map[Offence, Int],
    chatStats: Map[Long, UserChatStats])

case class UserChatStats(
    chat: Long,
    firstAppearance: ZonedDateTime,
    amountOfMessages: Int,
    offences: Map[Offence, Int])

trait Offence
object Spam extends Offence

trait Evidence
object OuterLink extends Evidence
object TelegramLink extends Evidence

case class GuiltRecord()
case class GuiltJournal(journal: List[GuiltRecord])

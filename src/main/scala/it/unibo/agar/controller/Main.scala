package it.unibo.agar.controller

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import it.unibo.agar.distributed.players.{AIActor, UserActor}
import it.unibo.agar.distributed.{FoodManager, GameCoordinator, GameOverActor, GlobalViewActor}
import it.unibo.agar.model.{GameInitializer, PlayerId}
import it.unibo.agar.view.GlobalView
import it.unibo.agar.{seeds, startupWithRole}

import scala.swing.*
import scala.swing.Swing.onEDT

object Main extends SimpleSwingApplication:

  val width = 1000
  val height = 1000
  val initialMass = 120.0
  val massToWin = 10000
  val speed = 10

  private val numFoods = 100
  private val foods = GameInitializer.initialFoods(numFoods, width, height)

  private val numAIPlayers = 4
  private val numUsers = 2
  private val users: Seq[PlayerId] = 1 to numUsers map (n => s"User-$n")

  private val rand = scala.util.Random
  def randomX: Double = rand.nextDouble * width
  def randomY: Double = rand.nextDouble * height

  override def top: Frame = new Frame { visible = false }

  override def main(args: Array[String]): Unit =
    startupWithRole("AgarSystem", seeds.head)(systemBehavior)
    super.main(args)

  private def systemBehavior: Behavior[Nothing] = Behaviors.setup[Nothing] : ctx =>
    ctx spawn (GameCoordinator(Seq.empty, foods), "Game-Coordinator")
    ctx spawn (FoodManager(), "Food-Manager")

    1 to numAIPlayers foreach: i =>
      ctx spawn (AIActor(s"AI-$i"), s"AIPlayer-$i")

    users foreach: id =>
      ctx spawn (UserActor(id), id)

    ctx spawn (GameOverActor(), "Game-Over")

    val globalView = new GlobalView
    ctx spawn (GlobalViewActor(globalView), "Global-View")

    onEDT:
      globalView.open()

    Behaviors.empty
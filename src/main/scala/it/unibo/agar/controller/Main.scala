package it.unibo.agar.controller

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import it.unibo.agar.distributed.GameProtocol.SpawnPlayerMessage
import it.unibo.agar.distributed.GameProtocol.SpawnPlayerMessage.*
import it.unibo.agar.distributed.players.{AIActor, UserActor}
import it.unibo.agar.distributed.{FoodManager, GameCoordinator, GameOverActor, GlobalViewActor}
import it.unibo.agar.model.{GameInitializer, PlayerId}
import it.unibo.agar.view.GlobalView
import it.unibo.agar.{seeds, startupWithRole}

import scala.concurrent.duration.DurationInt
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

  private val numAIPlayers = 5
  private val spawnAIInterval = 2.second

  private val numUsers = 2
  private val spawnUserInterval = 3.second
  private val users: Seq[PlayerId] = 1 to numUsers map (n => s"User-$n")

  private val rand = scala.util.Random
  def randomX: Double = rand.nextDouble * width
  def randomY: Double = rand.nextDouble * height

  override def top: Frame = new Frame { visible = false }

  override def main(args: Array[String]): Unit =
    startupWithRole("AgarSystem", seeds.head)(systemBehavior)
    super.main(args)

  private def systemBehavior: Behavior[SpawnPlayerMessage] = Behaviors.setup : ctx =>
    ctx spawn (GameCoordinator(Seq.empty, foods), "Game-Coordinator")
//    ctx spawn (GameCoordinator(Seq.empty, foods), "Game-Coordinator2")
    ctx spawn (FoodManager(), "Food-Manager")
    ctx spawn (GameOverActor(), "Game-Over")

    val globalView = new GlobalView
    ctx spawn (GlobalViewActor(globalView), "Global-View")
    onEDT:
      globalView.open()

    Behaviors.withTimers: timers =>
      timers startTimerAtFixedRate (SpawnUser, spawnAIInterval)

      def spawningUsers(users: Seq[PlayerId]): Behavior[SpawnPlayerMessage] = Behaviors.receive: (ctx, msg) =>
        msg match
          case SpawnUser => users match
            case id +: tail =>
              ctx spawn(UserActor(id), id)
              ctx.log info s"Spawned $id!"
              spawningUsers(tail)
            case Nil =>
              timers cancel SpawnUser
              timers startTimerAtFixedRate (SpawnAI, spawnAIInterval)
              ctx.log info "All Users spawned"
              spawningAIs(i = 0)
          case SpawnAI =>
            ctx.log info "Still spawning users, cannot spawn AI"
            Behaviors.same

      def spawningAIs(i: Int): Behavior[SpawnPlayerMessage] = Behaviors.receive: (ctx, msg) =>
        msg match
          case SpawnAI =>
            if i < numAIPlayers then
              ctx spawn (AIActor(s"AI-$i"), s"AIPlayer-$i")
              ctx.log info s"Spawned AI $i!"
              spawningAIs(i + 1)
            else
              timers cancel SpawnAI
              ctx.log info "All AIs spawned"
              Behaviors.empty
          case SpawnUser =>
            ctx.log info "Users are all spawned, I'm spawning AI"
            Behaviors.same

      spawningUsers(users)

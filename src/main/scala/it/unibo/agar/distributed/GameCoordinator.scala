package it.unibo.agar.distributed

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import Receptionist.{Listing, Register, Subscribe}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.agar.controller.Main
import it.unibo.agar.distributed.GameProtocol.*
import GameMessage.*
import StandardPlayerMessage.*
import it.unibo.agar.model.{Food, GameWorld, Player, World, PlayerId}
import GameWorld.updatePlayerPosition

object GameCoordinator:
  private val WorldServiceKey = ServiceKey[GameMessage]("GameManager")

  def askManager[T](ctx: ActorContext[T]): Unit =
    ctx.system.receptionist ! Subscribe(WorldServiceKey, ctx.messageAdapter[Listing]:
      listing => AvailableManagers(listing serviceInstances WorldServiceKey).asInstanceOf[T])

  def apply(initialPlayers: Seq[Player], initialFood: Seq[Food]): Behavior[GameMessage] = Behaviors.setup: ctx =>
    ctx.system.receptionist ! Register(WorldServiceKey, ctx.self)
    var players = Map.empty[PlayerId, ActorRef[StandardPlayerMessage]]
    var world = World(Main.width, Main.height, initialPlayers, initialFood)

    def broadcastWorld(): Unit = players.values foreach(_ ! WorldUpdate(world))

    Behaviors.receiveMessage:
      case PlayerJoined(id, replyTo, isAI) =>
        ctx.log info s"Player $id joined"
        players += (id -> replyTo)
        world = world copy (players = world.players :+ Player(id, ai = isAI))
        broadcastWorld()
        replyTo ! Start
        Behaviors.same

      case PlayerLeft(id) =>
        ctx.log info s"Player $id left"
        players -= id
        world = world copy (players = world playersExcludingSelf id)
        broadcastWorld()
        Behaviors.same

      case PlayerMove(id, (dx, dy)) =>
        // Update player position and handle eating
        world playerById id foreach: player =>
          val movedPlayer = updatePlayerPosition(player, dx, dy)

          // Check for food eaten
          val (foodEaten, playersEaten, playerEatPlayers) = GameWorld getPlayerStats (world, movedPlayer)
          val remainingPlayers = (world playersExcludingSelf id filterNot playersEaten.contains) :+ playerEatPlayers

          val remainingFoods = world.foods filterNot foodEaten.contains
          world = world.copy(players = remainingPlayers, foods = remainingFoods)
          broadcastWorld()

        Behaviors.same

      case NewFood(food) =>
        world = world.copy(foods = world.foods :+ food)
        broadcastWorld()
        Behaviors.same

      case WorldRequest(replyTo) =>
        replyTo ! ListenerMessages.WorldUpdate(world)
        Behaviors.same

      case ThereIsAWinner(winner) =>
        players.values foreach(_ ! End(winner))
        Behaviors.stopped

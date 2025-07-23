package it.unibo.agar.distributed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.agar.Utils.+:
import it.unibo.agar.distributed.GameProtocol.*
import FoodMessages.*
import GameMessage.NewFood
import it.unibo.agar.distributed.GameCoordinator.askManager
import it.unibo.agar.model.Food

import scala.concurrent.duration.*

object FoodManager:
  private val generationInterval = 1.seconds

  def apply(): Behavior[FoodMessage] = Behaviors.setup: ctx =>
    askManager(ctx)
    Behaviors.withTimers: timers =>
      timers startTimerAtFixedRate (GenerateFood, generationInterval)
      active(None)

  private def active(managerOpt: Option[ActorRef[GameMessage]]): Behavior[FoodMessage] = Behaviors.receive:
    case (ctx, AvailableManagers(manager +: _)) if managerOpt.isEmpty =>
      ctx.log info "GameManager found, storing its reference"
      active(Some(manager))

    case (_, AvailableManagers(_)) => Behaviors.same
    
    case (_, GenerateFood) =>
      managerOpt foreach: ref =>
        ref ! NewFood(Food(newFoodId))
      Behaviors.same

  private def newFoodId: String = "f" + java.util.UUID.randomUUID().toString

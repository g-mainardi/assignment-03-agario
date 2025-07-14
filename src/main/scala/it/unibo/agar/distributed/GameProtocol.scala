package it.unibo.agar.distributed

import akka.actor.typed.ActorRef
import it.unibo.agar.model.{Food, World}

object GameProtocol:
  type PlayerId = String
  type FoodId = String
  type Position = (Double, Double)
  type Direction = (Double, Double)
  
  sealed trait Message

  case class GetWorld(world: World) extends Message

  // Players protocol
  sealed trait PlayerMessage extends Message
  
  enum StandardPlayerMessage extends PlayerMessage:
    case Start
    case WorldUpdate(gameState: World)
    case End(winner: PlayerId)
//    case class Move(playerId: PlayerId, direction: Direction)
//    case class FoodConsumed(foodId: FoodId, consumedBy: PlayerId)

  object UserPlayerMessage:
    export StandardPlayerMessage.*
    type UserPlayerMessage = StandardPlayerMessage
  
  object AIPlayerMessage:
    export StandardPlayerMessage.*
    case object Tick extends PlayerMessage
    type AIPlayerMessage = PlayerMessage
    
  // Food Manager protocol
  enum FoodMessage extends Message:
    case GenerateFood
    case ConsumeFood(foodId: FoodId, playerId: PlayerId)
    case FoodUpdate(foods: Set[Food])

  // Game Coordinator protocol
  enum GameMessage extends Message:
    case PlayerMove(playerId: PlayerId, direction: Direction)
    case PlayerJoined(playerId: PlayerId, ref: ActorRef[StandardPlayerMessage])
    case PlayerLeft(playerId: PlayerId)

    case NewFood(food: Food)

    case IAmTheWinner(winner: PlayerId)
    case WorldRequest(ref: ActorRef[GetWorld])
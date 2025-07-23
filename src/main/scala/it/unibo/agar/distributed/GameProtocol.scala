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

  object UserPlayerMessage:
    export StandardPlayerMessage.*
    type UserPlayerMessage = PlayerMessage
  
  object AIPlayerMessages:
    export StandardPlayerMessage.*
    case object Tick extends PlayerMessage
    type AIPlayerMessage = PlayerMessage
    
  // Food Manager protocol
  trait FoodMessage extends Message
  enum FoodMessages extends FoodMessage:
    case GenerateFood
    case ConsumeFood(foodId: FoodId, playerId: PlayerId)
    case FoodUpdate(foods: Set[Food])

  enum GlobalViewCommand:
    case RefreshTimer
    case WorldUpdate(world: World)
    case AvailableManagers(listings: Set[ActorRef[GameMessage]])

  enum GameOverCommand:
    case RefreshTimer
    case AvailableManagers(listings: Set[ActorRef[GameMessage]])
    case WorldUpdate(world: World)

  // Game Coordinator protocol
  enum GameMessage extends Message:
    case PlayerMove(playerId: PlayerId, direction: Direction)
    case PlayerJoined(playerId: PlayerId, ref: ActorRef[StandardPlayerMessage], ai: Boolean = false)
    case PlayerLeft(playerId: PlayerId)
    case NewFood(food: Food)
    case ThereIsAWinner(winner: PlayerId)
    case WorldRequest(ref: ActorRef[GetWorld])

  // ReceptionistListingMessage
  final case class AvailableManagers(listings: Set[ActorRef[GameMessage]])
    extends PlayerMessage
      with FoodMessage

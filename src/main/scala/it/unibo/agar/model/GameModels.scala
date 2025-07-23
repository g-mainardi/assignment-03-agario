package it.unibo.agar.model

import it.unibo.agar.controller.Main.{height, initialMass, randomX, randomY, width, speed}
import it.unibo.agar.distributed.GameProtocol.PlayerId

import scala.util.Random

sealed trait Entity:

  def id: String
  def mass: Double
  def x: Double
  def y: Double
  def radius: Double = math.sqrt(mass / math.Pi)

  def distanceTo(other: Entity): Double =
    val dx = x - other.x
    val dy = y - other.y
    math.hypot(dx, dy)

case class Player(id: String, x: Double = randomX, y: Double = randomY, mass: Double = initialMass, ai: Boolean = false)
  extends Entity:

  def grow(entity: Entity): Player =
    copy(mass = mass + entity.mass)

case class Food(id: String, x: Double = randomX, y: Double = randomY, mass: Double = 100.0) extends Entity

case class World(
    width: Int,
    height: Int,
    players: Seq[Player],
    foods: Seq[Food]
):

  def playersExcludingSelf(player: Player): Seq[Player] =
    playersExcludingSelf(player.id)

  def playersExcludingSelf(id: PlayerId): Seq[Player] =
    players filterNot (_.id == id)

  def playerById(id: PlayerId): Option[Player] =
    players find (_.id == id)
    
  def isPresent(id: PlayerId): Boolean = players exists (_.id == id)

  def updatePlayer(player: Player): World =
    copy(players = players.map(p => if (p.id == player.id) player else p))

  def removePlayers(ids: Seq[Player]): World =
    copy(players = players.filterNot(p => ids.map(_.id).contains(p.id)))

  def removeFoods(ids: Seq[Food]): World =
    copy(foods = foods.filterNot(f => ids.contains(f)))

object GameWorld:

  def updatePlayerPosition(player: Player, dx: Double, dy: Double): Player =
    val newX = (player.x + dx * speed).max(0).min(width)
    val newY = (player.y + dy * speed).max(0).min(height)
    player.copy(x = newX, y = newY)

  def getPlayerStats(world: World, player: Player): (Seq[Food], Seq[Player], Player) =
    val foodEaten = world.foods.filter(food => EatingManager.canEatFood(player, food))
    val playerEatsFood = foodEaten.foldLeft(player)((p, food) => p.grow(food))
    val playersEaten = world
      .playersExcludingSelf(player)
      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))
    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))

    (foodEaten, playersEaten, playerEatPlayers)
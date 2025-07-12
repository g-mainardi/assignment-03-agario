package it.unibo.agar.model

import it.unibo.agar.model.GameWorld.GameRegion

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

case class Player(id: String, x: Double, y: Double, mass: Double) extends Entity:

  def grow(entity: Entity): Player =
    copy(mass = mass + entity.mass)

case class Food(id: String, x: Double, y: Double, mass: Double = 100.0) extends Entity

object Food:
  def apply(id: String, region: GameRegion): Food =
    Food(id, region.bounds.x + Random.nextInt(region.bounds.width), region.bounds.y + Random.nextInt(region.bounds.height))

case class World(
    width: Int,
    height: Int,
    players: Seq[Player],
    foods: Seq[Food]
):

  def playersExcludingSelf(player: Player): Seq[Player] =
    players.filterNot(_.id == player.id)

  def playerById(id: String): Option[Player] =
    players.find(_.id == id)

  def updatePlayer(player: Player): World =
    copy(players = players.map(p => if (p.id == player.id) player else p))

  def removePlayers(ids: Seq[Player]): World =
    copy(players = players.filterNot(p => ids.map(_.id).contains(p.id)))

  def removeFoods(ids: Seq[Food]): World =
    copy(foods = foods.filterNot(f => ids.contains(f)))

object GameWorld:

  case class GameRegion(
                         id: String,
                         bounds: Rectangle, // Area geografica della regione
                         nodeId: String // Nodo del cluster responsabile
                       )
  case class Rectangle(x: Double, y: Double, width: Int, height: Int)                     
  
  // Suddividi il mondo in regioni 2x2
  def createRegions(worldSize: (Int, Int)): Set[GameRegion] =
    val (width, height) = worldSize
    val regionWidth = width / 2
    val regionHeight = height / 2

    Set(
      GameRegion("region-0-0", Rectangle(0, 0, regionWidth, regionHeight), "node-1"),
      GameRegion("region-0-1", Rectangle(0, regionHeight, regionWidth, regionHeight), "node-2"),
      GameRegion("region-1-0", Rectangle(regionWidth, 0, regionWidth, regionHeight), "node-3"),
      GameRegion("region-1-1", Rectangle(regionWidth, regionHeight, regionWidth, regionHeight), "node-4")
    )

  def getPlayerStats(world: World, player: Player): (Seq[Food], Seq[Player], Player) =
    val foodEaten = world.foods.filter(food => EatingManager.canEatFood(player, food))
    val playerEatsFood = foodEaten.foldLeft(player)((p, food) => p.grow(food))
    val playersEaten = world
      .playersExcludingSelf(player)
      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))
    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))

    (foodEaten, playersEaten, playerEatPlayers)
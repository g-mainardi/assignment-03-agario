package it.unibo.agar.model

import it.unibo.agar.controller.Main.{height, initialMass, randomX, randomY, width, speed}

type PlayerId = String
type FoodId = String
type Direction = (Double, Double)

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
  private val eating: (Player, Entity) => Player = (p, e) => p.grow(e)
  extension (p: Player)
    private def eats(seq: Seq[Entity]): Player = seq.foldLeft(p)(eating)
    private def eatable(e: Entity): Boolean = e match
      case f: Food   => EatingManager canEatFood   (p, f)
      case o: Player => EatingManager canEatPlayer (p, o)

  def updatePlayerPosition(player: Player, dx: Double, dy: Double): Player =
    val newX = (player.x + dx * speed) max 0 min width
    val newY = (player.y + dy * speed) max 0 min height
    player.copy(x = newX, y = newY)

  def getPlayerStats(world: World, player: Player): (Seq[Food], Seq[Player], Player) =
    val foodEatable = world.foods filter player.eatable
    val playerEatFood = player eats foodEatable

    val playersEatable = world playersExcludingSelf playerEatFood filter playerEatFood.eatable
    val playerEatPlayers = playerEatFood eats playersEatable

    (foodEatable, playersEatable, playerEatPlayers)
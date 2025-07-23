package it.unibo.agar.controller

import it.unibo.agar.model.{AIMovement, GameInitializer, MockGameStateManager, World}
import it.unibo.agar.view.{GlobalView, LocalView}

import scala.swing.*
import scala.swing.Swing.onEDT

object Main extends SimpleSwingApplication:

  val width = 1000
  val height = 1000
  val initialMass = 120.0
  val massToWin = 10000
  val speed = 10

  private val numFoods = 100
  private val players = GameInitializer.initialPlayers(numPlayers, width, height)
  private val foods = GameInitializer.initialFoods(numFoods, width, height)

  private val numAIPlayers = 4
  private val numUsers = 2
  private val users: Seq[PlayerId] = 1 to numUsers map (n => s"User-$n")

  private val rand = scala.util.Random
  def randomX: Double = rand.nextDouble * width
  def randomY: Double = rand.nextDouble * height

  override def top: Frame = new Frame { visible = false }

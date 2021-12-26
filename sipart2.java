import tester.Tester;
import java.awt.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import javalib.funworld.*;
import javalib.worldimages.*;

// our version of world
class OurWorld extends World {
  IGamePiece player;
  IList<IGamePiece> invaders;
  IList<IGamePiece> playerBullets;
  IList<IGamePiece> invaderBullets;
  Random rand;

  //constructor
  OurWorld(IGamePiece player, IList<IGamePiece> invaders, IList<IGamePiece> playerBullets,
      IList<IGamePiece> invaderBullets, Random rand) {
    this.player = player;
    this.playerBullets = playerBullets;
    this.invaders = invaders;
    this.invaderBullets = invaderBullets;
    this.rand = rand;
  }

  // convenience constructor to start game
  OurWorld() {
    Utils u = new Utils();
    this.player = new Player(new CartPt(300, 600), false);
    this.playerBullets = new MtList<IGamePiece>();
    this.invaders = u.buildList(new MakeInv(), 4, 9);
    this.invaderBullets = new MtList<IGamePiece>();
    this.rand = new Random(5);
  }

  // convenience constructor to exclude invaders
  OurWorld(IGamePiece player, IList<IGamePiece> playerBullets,
      IList<IGamePiece> invaderBullets, Random rand) {
    Utils u = new Utils();
    this.player = player;
    this.playerBullets = playerBullets;
    this.invaders = u.buildList(new MakeInv(), 4, 9);
    this.invaderBullets = invaderBullets;
    this.rand = rand;
  }

  // convenience constructor to exclude random
  OurWorld(IGamePiece player, IList<IGamePiece> invaders, IList<IGamePiece> playerBullets,
      IList<IGamePiece> invaderBullets) {
    Utils u = new Utils();
    this.player = player;
    this.playerBullets = playerBullets;
    this.invaders = invaders;
    this.invaderBullets = invaderBullets;
    this.rand = new Random(5);
  }
 

  // renders the world as a scene
  public WorldScene makeScene() {
    BiFunction<IGamePiece, WorldScene, WorldScene> ld = new ListDraw();
    return this.playerBullets.fold(ld, this.player
        .drawPiece(this.invaders.fold(ld, this.invaderBullets.fold(ld, new WorldScene(500, 700)))));
  }

  // changes the world on certain key presses
  public World onKeyEvent(String key) {
    if (key.equals(" ")) {
      return new OurWorld(this.player, this.invaders, this.player.shoot(this.playerBullets),
          this.invaderBullets);
    }
    else if (key.equals("right") || key.equals("left")) {
      return new OurWorld(this.player.flipDir(key), this.invaders, this.playerBullets,
          this.invaderBullets);
    }
    else {
      return this;
    }
  }

  // move the World
  public World onTick() {
    return new OurWorld(this.player.movePiece(),
        this.invaders.map(new GamePieceToCartPt())
            .filter(new RemoveIfSameCP(this.playerBullets.map(new GamePieceToCartPt())))
            .map(new CartPtToInvader()),
        this.playerBullets.map(new GamePieceToCartPt()).filter(new BullOnScreen())
           .filter(new RemoveIfSameCP(this.invaders.map(new GamePieceToCartPt())))
          .map(new CartPtToPlayerBullet()).map(new MoveList()),
        this.invaders.shootList(this.invaderBullets, rand.nextInt(this.invaders.fold(new Count(), 0)))
        .map(new MoveList()).map(new GamePieceToCartPt())
        .filter(new BullOnScreen()).map(new CartPtToInvBullet()),
        new Random());
  }

  // ending scene for win
  public WorldScene finalSceneWin() {
    return new WorldScene(800, 800).placeImageXY(new TextImage("You Win!", Color.GREEN), 400, 400);
  }

  // ending scene for lose
  public WorldScene finalSceneLose() {
    return new WorldScene(800, 800).placeImageXY(new TextImage("You Lose:(", Color.RED), 400, 400);
  }

  // creates the ending scene when the game ends
  public WorldEnd worldEnds() {
    if (this.invaders.fold(new Count(), 0) == 0) {
      return new WorldEnd(true, this.finalSceneWin());
    }
    else if (this.invaderBullets.map(new GamePieceToCartPt()).contains(this.player.makeBOP())) {
      return new WorldEnd(true, this.finalSceneLose());
    }
    else {
      return new WorldEnd(false, this.makeScene());
    }
  }
}

//represents a game piece
interface IGamePiece {
  // draws the game piece onto the world scene
  public WorldScene drawPiece(WorldScene acc);

  // moves the IGamePiece by a set amount
  public IGamePiece movePiece();

  // flips the direction if the piece is a player
  public IGamePiece flipDir(String key);

  // adds bullet to list
  public IList<IGamePiece> shoot(IList<IGamePiece> bullets);

  // gets the pos of the gamepiece
  public CartPt getPos();

  // makes a BullOnPlayer that can access the pos
  public BullOnPlayer makeBOP();
 }

//abstract class for a gamepiece
abstract class AGamePiece implements IGamePiece {
  CartPt pos;

  // constructor
  AGamePiece(CartPt pos) {
    this.pos = pos;
  }

  //returns position of gamepiece
  public CartPt getPos() {
    return this.pos;
  }

  //makes predicate if bullet is on player
  public BullOnPlayer makeBOP() {
    return new BullOnPlayer(this.pos);
  };
}

//represents the player piece onto the world scene
class Player extends AGamePiece {
  boolean movingRight;

  Player(CartPt pos, boolean movingRight) {
    super(pos);
    this.movingRight = movingRight;
  }

  // draws the game piece onto the world scene
  public WorldScene drawPiece(WorldScene acc) {
    return acc.placeImageXY(new RectangleImage(30, 20, "solid", Color.BLACK), this.pos.x,
        this.pos.y);
  }

  // checks if the player is at the left edge of the screen
  boolean isAtEdge() {
    return this.pos.x <= 0 || this.pos.x >= 500;
  }

  // moves the IGamePiece by a given amount
  public IGamePiece movePiece() {
    if (this.isAtEdge()) {
      return this;
    }
    else {
      if (this.movingRight) {
        return new Player(new CartPt(this.pos.x + 4, this.pos.y), this.movingRight);
      }
      else {
        return new Player(new CartPt(this.pos.x - 4, this.pos.y), this.movingRight);
      }
    }
  }

  // flips the direction of the player
  public IGamePiece flipDir(String key) {
    if (key.equals("left")) {
      if (this.pos.x >= 500) {
        return new Player(new CartPt(this.pos.x - 1, this.pos.y), false);
      }
      else {
        return new Player(new CartPt(this.pos.x, this.pos.y), false);
      }
    }
    else if (key.equals("right")) {
      if (this.pos.x <= 0) {
        return new Player(new CartPt(this.pos.x + 1, this.pos.y), true);
      }
      else {
        return new Player(new CartPt(this.pos.x, this.pos.y), true);
      }
    }
    else {
      return this;
    }
  }

  // shoots bullet from player
  public IList<IGamePiece> shoot(IList<IGamePiece> bullets) {
    if (bullets.fold(new Count(), 0) < 3) {
      return new ConsList<IGamePiece>(new PlayerBullets(this.pos), bullets);
    }
    else {
      return bullets;
    }
  }
}

//represents an invader
class Invader extends AGamePiece {

  // constructor
  Invader(CartPt pos) {
    super(pos);
  }

  // draws the game piece onto the world scene
  public WorldScene drawPiece(WorldScene acc) {
    return acc.placeImageXY(new RectangleImage(20, 20, "solid", Color.RED), this.pos.x, this.pos.y);
  }

  // invaders do not move
  public IGamePiece movePiece() {
    return this;
  }

  //changes direction of piece depending on key
  @Override
  public IGamePiece flipDir(String key) {
    return this;
  }

  // shoots from invader
  public IList<IGamePiece> shoot(IList<IGamePiece> bullets) {
    if (bullets.fold(new Count(), 0) < 10) {
      return new ConsList<IGamePiece>(new InvaderBullets(this.pos), bullets);
    }
    else {
      return bullets;
    }
  }
}

// abstract class representing bullets
abstract class ABullets extends AGamePiece {
  int speed;

  // constructor
  ABullets(CartPt pos, int speed) {
    super(pos);
    this.speed = speed;
  }

  // draws the game piece onto the world scene
  public abstract WorldScene drawPiece(WorldScene acc);

  // moves the bullet at its speed
  public abstract IGamePiece movePiece();

  public IGamePiece flipDir(String key) {
    return this;
  }

  // unused function shoot from bullet
  public IList<IGamePiece> shoot(IList<IGamePiece> bullets) {
    return bullets;
  }
}

// class representing player bullets
class PlayerBullets extends ABullets {
  // constructor
  PlayerBullets(CartPt pos) {
    super(pos, -4);
  }

  // draws the game piece onto the world scene
  public WorldScene drawPiece(WorldScene acc) {
    return acc.placeImageXY(new CircleImage(5, "solid", Color.BLACK), this.pos.x, this.pos.y);
  }

  // moves the bullet at its speed
  public IGamePiece movePiece() {
    return new PlayerBullets(new CartPt(this.pos.x, this.pos.y  + this.speed));
  }
}

class InvaderBullets extends ABullets {
  // constructor
  InvaderBullets(CartPt pos) {
    super(pos, 4);
  }

  // draws the game piece onto the world scene
  public WorldScene drawPiece(WorldScene acc) {
    return acc.placeImageXY(new CircleImage(5, "solid", Color.RED), this.pos.x, this.pos.y);
  }

  // moves the bullet at its speed
  public IGamePiece movePiece() {
    return new InvaderBullets(new CartPt(this.pos.x, this.pos.y + this.speed));
  }
}

//represents a point on the cartesian plane
class CartPt {
  int x;
  int y;

  // constructor
  CartPt(int x, int y) {
    this.x = x;
    this.y = y;
  }

  // checks if that cartpt is the same as this one
  boolean same(CartPt that) {
    return this.x == that.x && this.y == that.y;
  }

  //checks if bullet is within range on invader
  boolean onInv(CartPt that) {
    return (this.x >= that.x - 10) && (this.x <= that.x + 10) && (this.y >= that.y - 10)
        && (this.y <= that.y + 10);
  }

  // checks if bullet is within range on player
  boolean onPlayer(CartPt that) {
    return (this.x >= that.x - 15) && (this.x <= that.x + 15) && (this.y >= that.y - 10)
        && (this.y <= that.y + 10);
  }

  //checks if gamepiece is within limits of screen
  public boolean onScreen() {
    return this.x <= 500 && this.x >= 0 && this.y <= 700 && this.y >= 0;
  }

}

//represents a list of type T
interface IList<T> {
  // filter this IList using the given predicate
  IList<T> filter(Predicate<T> pred);

  //shoots bullets from a list of invaders
  IList<IGamePiece> shootList(IList<IGamePiece> invaderBullets, int index);

  // map the given function onto every member of this IList
  <U> IList<U> map(Function<T, U> converter);

  // combine the items in this IList using the given function
  <U> U fold(BiFunction<T, U, U> converter, U initial);

  // checks if the predicate is true for any items in the list
  boolean ormap(Predicate<T> pred);

  // checks if the predicate is true for all the items in the list
  boolean andmap(Predicate<T> pred);

  // checks if the given item is in the list
  boolean contains(Predicate<T> pred);
}

// empty list
class MtList<T> implements IList<T> {
  MtList() {
  }

  // filter this MtList using the given predicate
  public IList<T> filter(Predicate<T> pred) {
    return new MtList<T>();
  }

  // map the given function onto every member of this MtList
  public <U> IList<U> map(Function<T, U> converter) {
    return new MtList<U>();
  }

  // combine the items in this MtList using the given function
  public <U> U fold(BiFunction<T, U, U> converter, U initial) {
    return initial;
  }

  // checks if the predicate is true for any items in the list
  public boolean ormap(Predicate<T> pred) {
    return false;
  }

  // checks if the predicate is true for all the items in the list
  public boolean andmap(Predicate<T> pred) {
    return true;
  }

  // checks if the given item is in the list
  public boolean contains(Predicate<T> pred) {
    return false;
  }

  //shoots bullets from list of invaders
  @Override
  public IList<IGamePiece> shootList(IList<IGamePiece> invaderBullets, int index) {
    return new MtList<IGamePiece>();
  }
}

// cons list
class ConsList<T> implements IList<T> {
  T first;
  IList<T> rest;

  // constructor
  ConsList(T first, IList<T> rest) {
    this.first = first;
    this.rest = rest;
  }

  // filter this ConsList using the given predicate
  public IList<T> filter(Predicate<T> pred) {
    if (pred.test(this.first)) {
      return new ConsList<T>(this.first, this.rest.filter(pred));
    }
    else {
      return this.rest.filter(pred);
    }
  }

  // map the given function onto every member of this ConsList
  public <U> IList<U> map(Function<T, U> converter) {
    return new ConsList<U>(converter.apply(this.first), this.rest.map(converter));
  }

  // combine the items in this ConsList using the given function
  public <U> U fold(BiFunction<T, U, U> converter, U initial) {
    return converter.apply(this.first, this.rest.fold(converter, initial));
  }

  // checks if the predicate is true for any items in the list
  public boolean ormap(Predicate<T> pred) {
    return pred.test(this.first) || this.rest.ormap(pred);
  }

  // checks if the predicate is true for all the items in the list
  public boolean andmap(Predicate<T> pred) {
    return pred.test(this.first) && this.rest.andmap(pred);
  }

  // checks if the given item is in the list
  public boolean contains(Predicate<T> pred) {
    return this.ormap(pred);
  }

  // shoots bullets from list of invaders
  @Override
  public IList<IGamePiece> shootList(IList<IGamePiece> invaderBullets, int index) {
    if(index == 0) {
      return ((IGamePiece) this.first).shoot(invaderBullets);
    }
    else {
      return this.rest.shootList(invaderBullets, index-1);
    }
  }
}

// class for a BiFunction that converts a game piece to a worldscene
class ListDraw implements BiFunction<IGamePiece, WorldScene, WorldScene> {
  // draws the given GamePiece onto the world scene
  public WorldScene apply(IGamePiece t, WorldScene u) {
    return t.drawPiece(u);
  }
}

//predicate for tests
class EqualsA implements Predicate<String> {
  public boolean test(String t) {
    return t.equals("a");
  }
}

//function for tests
class DubString implements Function<String, String> {
  public String apply(String t) {
    return t + t;
  }
}

// bifunction for tests
class Concat implements BiFunction<String, String, String> {
  public String apply(String t, String u) {
    return t + u;
  }
}

// predicate that checks if the given cartpt is in the list of cartpts
class SameCartPt implements Predicate<CartPt> {
  CartPt predInList;

  // constructor
  SameCartPt(CartPt predInList) {
    this.predInList = predInList;
  }
// checks if individual cart pt is in list of cart pts
  public boolean test(CartPt t) {
    return (t.same(predInList));
  }
}

//checks if each bullet is on screen
class BullOnScreen implements Predicate<CartPt> {

  @Override
  public boolean test(CartPt t) {
    return t.onScreen();
  }
}

//checks if bullet is pos as invader
class RemoveIfSameCP implements Predicate<CartPt> {
  IList<CartPt> that;

  RemoveIfSameCP(IList<CartPt> that) {
    this.that = that;
  }

  public boolean test(CartPt t) {
    Predicate<CartPt> sameAsInvader = new BullOnInv(t);
    return !that.contains(sameAsInvader);
  }

}

//predicate that checks if the given cartpt is within a range of one of the cartpts in the list
// called using list of bullets
class BullOnInv implements Predicate<CartPt> {
  CartPt invaderCartPtInList;

// constructor
  BullOnInv(CartPt predInList) {
    this.invaderCartPtInList = predInList;
  }

  //checks if bullet is on invader
  public boolean test(CartPt t) {
    return t.onInv(invaderCartPtInList);
  }
}

//predicate that checks if the given cartpt is within a range of one of the cartpts in the list
class BullOnPlayer implements Predicate<CartPt> {
  CartPt predInList;

//constructor
  BullOnPlayer(CartPt predInList) {
    this.predInList = predInList;
  }

  // check if bullet is on player
  public boolean test(CartPt t) {
    return (t.onPlayer(predInList));
  }
}

// moving gamepieces
class MoveList implements Function<IGamePiece, IGamePiece> {
  @Override
  public IGamePiece apply(IGamePiece t) {
    return t.movePiece();
  }
}

//makes cartpt from gamepiece
class GamePieceToCartPt implements Function<IGamePiece, CartPt> {
  @Override
  public CartPt apply(IGamePiece t) {
    return t.getPos();
  }
}

//returns invader with position
class CartPtToInvader implements Function<CartPt, IGamePiece> {
  @Override
  public IGamePiece apply(CartPt t) {
    return new Invader(t);
  }
}

//returns bullet with position
class CartPtToPlayerBullet implements Function<CartPt, IGamePiece> {
  @Override
  public IGamePiece apply(CartPt t) {
    return new PlayerBullets(t);
  }
}

//makes invader bullet with cartpt
class CartPtToInvBullet implements Function<CartPt, IGamePiece> {
  @Override
  public IGamePiece apply(CartPt t) {
    return new InvaderBullets(t);
  }
}

//counts element in list
class Count implements BiFunction<IGamePiece, Integer, Integer> {
  public Integer apply(IGamePiece t, Integer u) {
    return u + 1;
  }
}

// utils class containing buildlist with columns 1 through n and rows 1 through m
class Utils {
  <U> IList<U> buildList(BiFunction<Integer, Integer, U> constructor, int row, int column) {
    if (row == 0) {
      return new MtList<U>();
    }
    else if (column == 0) {
      return this.buildList(constructor, row - 1, column + 9);
    }
    else {
      return new ConsList<U>(constructor.apply(row, column),
          this.buildList(constructor, row, column - 1));
    }
  }
}

// bifunction that creates a new invader base on the given integers
class MakeInv implements BiFunction<Integer, Integer, IGamePiece> {
  public IGamePiece apply(Integer row, Integer column) {
    return new Invader(new CartPt(((40 * column) + 50), (40 * row) + 50));
  }
}

// examples and test for the world
class ExamplesWorld {
  // examples for world
  IGamePiece player1 = new Player(new CartPt(300, 600), false);
  IGamePiece invader1 = new Invader(new CartPt(100, 100));
  IGamePiece player1a = new Player(new CartPt(300, 600), false);
  IGamePiece invader1a = new Invader(new CartPt(410, 130));
  IGamePiece invader2 = new Invader(new CartPt(370, 130));
  IGamePiece invader3 = new Invader(new CartPt(330, 130));
  IGamePiece invader4 = new Invader(new CartPt(290, 130));
  IGamePiece invader5 = new Invader(new CartPt(250, 130));
  IGamePiece invader6 = new Invader(new CartPt(210, 130));
  IGamePiece invader7 = new Invader(new CartPt(170, 130));
  IGamePiece invader8 = new Invader(new CartPt(130, 130));
  IGamePiece invader9 = new Invader(new CartPt(90, 130));
  IGamePiece invader10 = new Invader(new CartPt(410, 90));
  IGamePiece invader11 = new Invader(new CartPt(370, 90));
  IGamePiece invader12 = new Invader(new CartPt(330, 90));
  IGamePiece invader13 = new Invader(new CartPt(290, 90));
  IGamePiece invader14 = new Invader(new CartPt(250, 90));
  IGamePiece invader15 = new Invader(new CartPt(210, 90));
  IGamePiece invader16 = new Invader(new CartPt(170, 90));
  IGamePiece invader17 = new Invader(new CartPt(130, 90));
  IGamePiece invader18 = new Invader(new CartPt(90, 90));
  IList<IGamePiece> invList1 = new ConsList<IGamePiece>(invader1a,
      new ConsList<IGamePiece>(invader2,
          new ConsList<IGamePiece>(invader3, new ConsList<IGamePiece>(invader4,
              new ConsList<IGamePiece>(invader5, new ConsList<IGamePiece>(invader6,
                  new ConsList<IGamePiece>(invader7, new ConsList<IGamePiece>(invader8,
                      new ConsList<IGamePiece>(invader9, new ConsList<IGamePiece>(invader10,
                          new ConsList<IGamePiece>(invader11, new ConsList<IGamePiece>(invader12,
                              new ConsList<IGamePiece>(invader13,
                                  new ConsList<IGamePiece>(invader14,
                                      new ConsList<IGamePiece>(invader15,
                                          new ConsList<IGamePiece>(invader16,
                                              new ConsList<IGamePiece>(invader17,
                                                  new ConsList<IGamePiece>(invader18,
                                                      new MtList<IGamePiece>()))))))))))))))))));
  IGamePiece pBullet1 = new PlayerBullets(new CartPt(250, 250));
  IGamePiece pBullet2 = new PlayerBullets(new CartPt(300, 250));
  IList<IGamePiece> pBulletList1 = new ConsList<IGamePiece>(pBullet1,
      new ConsList<IGamePiece>(pBullet2, new MtList<IGamePiece>()));
  IGamePiece invBullet1 = new InvaderBullets(new CartPt(250, 300));
  IGamePiece invBullet2 = new InvaderBullets(new CartPt(300, 300));
  IList<IGamePiece> invBulletList1 = new ConsList<IGamePiece>(invBullet1,
      new ConsList<IGamePiece>(invBullet2, new MtList<IGamePiece>()));
  OurWorld mtWorld = new OurWorld(player1, new MtList<IGamePiece>(), new MtList<IGamePiece>(),
      new MtList<IGamePiece>(), new Random(1));
  BiFunction<Integer, Integer, IGamePiece> buildInv = new MakeInv();
  Utils ut = new Utils();
  IList<IGamePiece> invList1a = ut.buildList(buildInv, 4, 9);
  OurWorld world1 = new OurWorld(player1, invList1a, pBulletList1, invBulletList1, new Random(1));
  IList<CartPt> cartPtList = new ConsList<CartPt>(new CartPt(1, 2), new ConsList<CartPt>(
      new CartPt(5, 6), new ConsList<CartPt>(new CartPt(3, 3), new MtList<CartPt>())));
  OurWorld world2 = new OurWorld(player1, invList1a, new MtList<IGamePiece>(), 
      new MtList<IGamePiece>(), new Random());


  // test for big bang
  boolean testBigBang(Tester t) {
    OurWorld world = this.world2;
    int worldWidth = 500;
    int worldHeight = 700;
    double tickRate = 0.05;
    return world.bigBang(worldWidth, worldHeight, tickRate);
  }

  // test for makeScene
  boolean testMakeScene(Tester t) {
    BiFunction<IGamePiece, WorldScene, WorldScene> ld = new ListDraw();
    return t.checkExpect(this.mtWorld.makeScene(),
        new MtList<IGamePiece>().fold(ld,
            this.player1.drawPiece(new MtList<IGamePiece>().fold(ld,
                new MtList<IGamePiece>().fold(ld, new WorldScene(500, 700))))))
        && t.checkExpect(this.world1.makeScene(), this.pBulletList1.fold(ld, this.player1.drawPiece(
            this.invList1a.fold(ld, this.invBulletList1.fold(ld, new WorldScene(500, 700))))));
  }

  // test for onKeyEvent
  boolean testOnKeyEvent(Tester t) {
    return t.checkExpect(this.world1.onKeyEvent("left"), this.world1);
  }
  
  // test for onTick
  boolean testOnTick(Tester t) {
    return t.checkExpect(world1.onTick(), new OurWorld(this.player1.movePiece(),
        this.invList1a.map(new GamePieceToCartPt()).filter(
            new RemoveIfSameCP(this.pBulletList1.map(
                new GamePieceToCartPt()))).map(new CartPtToInvader()),
        this.pBulletList1.map(new GamePieceToCartPt()).filter(new BullOnScreen())
        .filter(new RemoveIfSameCP(this.invList1a.map(new GamePieceToCartPt())))
       .map(new CartPtToPlayerBullet()).map(new MoveList()),
       this.invList1a.shootList(this.invBulletList1, (new Random(5)).nextInt(this.invList1a.fold(new Count(), 0)))
       .map(new MoveList()).map(new GamePieceToCartPt())
       .filter(new BullOnScreen()).map(new CartPtToInvBullet()),
       new Random()));
  }

  IGamePiece invBulletLose = new InvaderBullets(new CartPt(300, 600));
  OurWorld worldWin = new OurWorld(player1, new MtList<IGamePiece>(), pBulletList1, invBulletList1,
      new Random(1));
  IGamePiece bulletLose = new InvaderBullets(new CartPt(300, 600));
  IList<IGamePiece> invBulletListLose = new ConsList<IGamePiece>(invBulletLose,
      new MtList<IGamePiece>());
  OurWorld worldLose = new OurWorld(player1, invList1, pBulletList1, invBulletListLose,
      new Random(1));

  boolean testWorldEnds(Tester t) {
    return t.checkExpect(this.world1.worldEnds(), new WorldEnd(false, this.world1.makeScene()))
        && t.checkExpect(this.worldWin.worldEnds(),
            new WorldEnd(true, this.worldWin.finalSceneWin()))
        && t.checkExpect(this.worldLose.worldEnds(),
            new WorldEnd(true, this.worldLose.finalSceneLose()));
  }

  // tests for apply
  boolean testApply(Tester t) {
    BiFunction<IGamePiece, WorldScene, WorldScene> ld = new ListDraw();
    return t.checkExpect(ld.apply(player1, new WorldScene(500, 500)),
        player1.drawPiece(new WorldScene(500, 500)))
        && t.checkExpect(ld.apply(invader10, mtWorld.makeScene()),
            invader10.drawPiece(mtWorld.makeScene()))
        && t.checkExpect(ld.apply(pBullet1, world1.makeScene()),
            pBullet1.drawPiece(world1.makeScene()))
        && t.checkExpect(ld.apply(invBullet2, world1.makeScene()),
            invBullet2.drawPiece(world1.makeScene()))
        && t.checkExpect(this.equalsA.test("a"), true)
        && t.checkExpect(this.dubString.apply("a"), "aa")
        && t.checkExpect(this.bi.apply("a", "a"), "aa")
        && t.checkExpect(new MakeInv().apply(5, 5), new Invader(new CartPt(250, 250)))
        && t.checkExpect(new Count().apply(invader1, 0), 1)
        //getpos
        //gptocartpt
        //cartptto inv
        //cartpttoplayer
        //cartpttoinvaderbullet
        ;
  }

  // test for test
  boolean testTest(Tester t) {
    Predicate<CartPt> pred = new SameCartPt(new CartPt(1, 5));
    return t.checkExpect(pred.test(new CartPt(1, 4)), false)
        && t.checkExpect(pred.test(new CartPt(1, 5)), true)
        //removeifsamecp test
        //bull on inv
        //bullonplayer
        ;
  }

  // test buildList with invader
  boolean testBuildList(Tester t) {
    return t.checkExpect(ut.buildList(buildInv, 1, 1),
        new ConsList<IGamePiece>(new Invader(new CartPt(90, 90)), new MtList<IGamePiece>()))
        && t.checkExpect(ut.buildList(buildInv, 1, 2),
            new ConsList<IGamePiece>(new Invader(new CartPt(130, 90)),
                new ConsList<IGamePiece>(new Invader(new CartPt(90, 90)),
                    new MtList<IGamePiece>())))
        && t.checkExpect(ut.buildList(buildInv, 2, 9), invList1)
        && t.checkExpect(ut.buildList(buildInv, 0, 0), new MtList<IGamePiece>());
  }

  // test for draw piece
  boolean testDrawPiece(Tester t) {
    return t.checkExpect(this.player1.drawPiece(new WorldScene(800, 800)),
        new WorldScene(800, 800).placeImageXY(new RectangleImage(30, 20, "solid", Color.BLACK), 300,
            600))
        && t.checkExpect(this.invader1.drawPiece(new WorldScene(800, 800)),
            new WorldScene(800, 800).placeImageXY(new RectangleImage(20, 20, "solid", Color.RED),
                100, 100))
        && t.checkExpect(this.pBullet1.drawPiece(new WorldScene(800, 800)),
            new WorldScene(800, 800).placeImageXY(new CircleImage(5, "solid", Color.BLACK), 250,
                250))
        && t.checkExpect(this.invBullet1.drawPiece(new WorldScene(800, 800)),
            new WorldScene(800, 800).placeImageXY(new CircleImage(5, "solid", Color.RED), 250,
                300));
  }

  // examples for movePiece
  CartPt rightEdge = new CartPt(800, 600);
  CartPt leftEdge = new CartPt(0, 600);
  CartPt middle = new CartPt(400, 600);
  IGamePiece pAtREdge = new Player(this.rightEdge, true);
  IGamePiece pMoveRight = new Player(this.rightEdge, true);

  // test for movePiece
  boolean testMovePiece(Tester t) {
    return t.checkExpect(this.player1.movePiece(), new Player(new CartPt(296, 600), false))
        && t.checkExpect(this.pAtREdge.movePiece(), this.pAtREdge);
  }

  // examples to test list
  Predicate<String> equalsA = new EqualsA();
  Function<String, String> dubString = new DubString();
  BiFunction<String, String, String> bi = new Concat();
  IList<String> list1 = new ConsList<String>("1",
      new ConsList<String>("2", new ConsList<String>("3", new MtList<String>())));
  IList<String> list2 = new ConsList<String>("a",
      new ConsList<String>("b", new ConsList<String>("c", new MtList<String>())));
  IList<String> list3 = new ConsList<String>("a",
      new ConsList<String>("a", new ConsList<String>("a", new MtList<String>())));

  // test for filter
  boolean testFilter(Tester t) {
    return t.checkExpect(this.list2.filter(this.equalsA),
        new ConsList<String>("a", new MtList<String>()))
        && t.checkExpect(this.list1.filter(this.equalsA), new MtList<String>());
  }

  // test for map
  boolean testMap(Tester t) {
    return t.checkExpect(this.list1.map(this.dubString), new ConsList<String>("11",
        new ConsList<String>("22", new ConsList<String>("33", new MtList<String>()))));
  }

  // test for fold
  boolean testFold(Tester t) {
    return t.checkExpect(this.list1.fold(bi, ""), "123")
        && t.checkExpect(this.list2.fold(bi, ""), "abc")
        && t.checkExpect(this.invList1a.fold(new Count(), 0), 36);

  }

  // test for ormap
  boolean testOrMap(Tester t) {
    return t.checkExpect(this.list2.ormap(this.equalsA), true)
        && t.checkExpect(this.list1.ormap(this.equalsA), false);
  }

  // test for andmap
  boolean testAndMap(Tester t) {
    return t.checkExpect(this.list3.andmap(this.equalsA), true)
        && t.checkExpect(this.list2.andmap(this.equalsA), false);
  }

  // examples for contains
  CartPt testCart = new CartPt(5, 5);
  IList<CartPt> listCart = new ConsList<CartPt>(this.testCart, new MtList<CartPt>());
  Predicate<CartPt> aPred = new SameCartPt(this.testCart);

  CartPt testInvCart = new CartPt(5, 5);
  IList<CartPt> listCart2 = new ConsList<CartPt>(new CartPt(10, 6), new MtList<CartPt>());
  Predicate<CartPt> aPred2 = new BullOnInv(this.testCart);

  CartPt testPlayerCart = new CartPt(5, 5);
  IList<CartPt> listCart3 = new ConsList<CartPt>(new CartPt(16, 3), new MtList<CartPt>());
  Predicate<CartPt> aPred3 = new BullOnPlayer(this.testCart);

  // test for contains
  boolean testContains(Tester t) {
    return t.checkExpect(this.listCart.contains(this.aPred), true)
        && t.checkExpect(this.listCart2.contains(this.aPred2), true)
        && t.checkExpect(this.listCart3.contains(this.aPred3), true);
  }

  // test flipDir
  boolean testFlipDir(Tester t) {
    return t.checkExpect(this.player1.flipDir("right"), new Player(new CartPt(300, 600), true))
        && t.checkExpect(this.pAtREdge.flipDir("right"), this.pAtREdge)
        && t.checkExpect(this.pAtREdge.flipDir("left"), new Player(new CartPt(799, 600), false))
        && t.checkExpect(this.player1.flipDir("left"), this.player1)
        && t.checkExpect(this.invader1.flipDir("right"), this.invader1)
        && t.checkExpect(this.pBullet1.flipDir("right"), this.pBullet1)
        && t.checkExpect(this.invBullet1.flipDir("right"), this.invBullet1);
  }

  CartPt p1Pos = new CartPt(300, 600);
  IList<IGamePiece> pBulletList2 = new ConsList<IGamePiece>(pBullet1, new ConsList<IGamePiece>(
      pBullet2, new ConsList<IGamePiece>(pBullet1, new MtList<IGamePiece>())));
  IList<IGamePiece> invBulletList2 = new ConsList<IGamePiece>(invBullet1,
      new ConsList<IGamePiece>(invBullet2,
          new ConsList<IGamePiece>(invBullet1,
              new ConsList<IGamePiece>(invBullet2, new ConsList<IGamePiece>(invBullet1,
                  new ConsList<IGamePiece>(invBullet2, new ConsList<IGamePiece>(invBullet1,
                      new ConsList<IGamePiece>(invBullet2, new ConsList<IGamePiece>(invBullet1,
                          new ConsList<IGamePiece>(invBullet2, new MtList<IGamePiece>()))))))))));

  // test shoot
  boolean testShoot(Tester t) {
    return t.checkExpect(this.player1.shoot(this.pBulletList1),
        new ConsList<IGamePiece>(new PlayerBullets(p1Pos), pBulletList1))
        && t.checkExpect(this.invader1.shoot(this.invBulletList1),
            new ConsList<IGamePiece>(new InvaderBullets(new CartPt(100, 100)), invBulletList1))
        && t.checkExpect(this.player1.shoot(this.pBulletList2), pBulletList2)
        && t.checkExpect(this.invader1.shoot(invBulletList2), this.invBulletList2);
  }
  
  // test getPos
  boolean testGetPos(Tester t) {
    return t.checkExpect(this.player1.getPos(), new CartPt(300, 600));
  }
  
  }
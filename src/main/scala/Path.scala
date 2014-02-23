/***     
**                  _______        
**                 |__   __|   reqT - a requriements engineering tool  
**   _ __  ___   __ _ | |      (c) 2011-2014, Lund University  
**  |  __|/ _ \ / _  || |      http://reqT.org
**  | |  |  __/| (_| || |   
**  |_|   \___| \__  ||_|   
**                 | |      
**                 |_|      
** reqT is open source, licensed under the BSD 2-clause license: 
** http://opensource.org/licenses/bsd-license.php 
**************************************************************************/

package reqT

sealed trait Path {
  def heads: Vector[Head]
  def tail: Path
  val isSingle = heads.size == 1
  val isEmpty = heads.size == 0
  def level: Int
  lazy val head = heads.head
  lazy val headOption: Option[Head] = heads.headOption
}

case class HeadPath(heads: Vector[Head]) extends Path {
  def toModel: Model = if (isEmpty) Model() 
    else if (isSingle) Model(Relation(head, Model())) 
    else Model(Relation(head, tail.toModel)) 
  def /(h: Head) = HeadPath(heads :+ h)
  def /(e: Entity) = HeadPath(heads :+ e.has)
  def /[T](at: AttributeType[T]) = AttrRef[T](this, at)
  def /[T](a: Attribute[T]) = AttrVal[T](this, a)
  def / = this
  lazy val tail = HeadPath(heads.tail)
  override lazy val level = heads.size 
  override lazy val toString = heads.mkString("", "/","/")
}
object HeadPath {
  def apply(hs: Head*) = new HeadPath(hs.toVector)
}

case class AttrRef[T](init: HeadPath, attrType: AttributeType[T]) extends Path {
  def / = this
  override lazy val heads = init.heads
  lazy val tail = AttrRef(HeadPath(heads.drop(1)), attrType)
  //def apply(m: Model) = ModelUpdater(m, this)
  override def level = heads.size + 1
  override def toString = ( if (init.isEmpty) "" else init.toString )  + attrType + "/"
}

case class AttrVal[T](init: HeadPath, attr: Attribute[T]) extends Path {
  def / = this
  def toModel: Model = if (isEmpty) Model(attr) 
    else if (isSingle) Model(Relation(head, Model(attr))) 
    else Model(Relation(head, tail.toModel)) 
  override lazy val heads = init.heads
  lazy val tail = AttrVal(HeadPath(heads.drop(1)), attr)
  override lazy val level = heads.size + 1
  override lazy val toString = ( if (init.isEmpty) "" else init.toString )  + attr + "/"
}

trait RootHeadPathFactory {
  def / = HeadPath()
}
//case class ModelUpdater[T](m: Model, r: AttrRef[T]) {
  // //to enable DSL syntax Stakeholder("a")/Req("x")/Prio(Model()) := 3 
  //def :=(value: T): Model =  m.updated(r, value)
// }
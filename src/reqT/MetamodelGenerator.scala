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
package metaprog 

trait MetamodelGenerator extends reqT.DSL with MetamodelToScala {
  import scala.collection.immutable.ListMap
  def enums: ListMap[String,Vector[String]]
  def attributes: ListMap[String,Vector[String]]
  def attributeDefaultValues: ListMap[String, String]
  def generalEntities: Vector[String]
  def contextEntities: Vector[String]
  def requriementEntities: Map[String,Vector[String]]
  def relations: Vector[String]
  def defaultEntities: Vector[EntityType]
  def defaultAttributes: Vector[AttributeType[_]]
  def defaultInterpretedAttributes: Vector[AttributeType[_]]
  def defaultRelations: Vector[RelationType]
}

trait MetamodelToScala {
  self: MetamodelGenerator =>

  override def toScala: String = 
    mkPreamble + mkObjectMetamodel + mkEnumTraits +  
      mkConcreteAttrs + mkAbstrReqs + mkConcreteEnts + mkConcreteRels + mkFactoryTraits

  lazy val mkPreamble = s"""// *** THIS IS A GENERATED SOURCE FILE 
// *** Generated by reqT> reqT.metaprog.makeMetamodel().save(<filename>)
// *** Generation date: ${(new java.util.Date).toString} 
// *** reqT version $reqT_VERSION build date $BUILD_DATE
""" + s"/*** ${reqT.PREAMBLE}***************************************************/\n" +
       "package reqT\n"

  lazy val mkObjectMetamodel = s"""
object metamodel extends MetamodelTypes {
  override lazy val types: Vector[MetaType] = entityTypes ++ attributeTypes ++ relationTypes
  lazy val entityTypes: Vector[EntityType] = $defaultEntities ++ generalEntities ++ contextEntities ++ requirementEntities
  $mkEntityTypes
  lazy val attributeTypes: Vector[AttributeType[_]] = $defaultAttributes ++ interpretedAttributes ++ $mkAttributeTypes 
  lazy val relationTypes: Vector[RelationType] = ${defaultRelations.map(_.toString)} ++ Vector($mkRelationTypes)
  lazy val interpretedAttributes: Vector[AttributeType[_]] = $defaultInterpretedAttributes
}
"""

  lazy val mkEntityTypes = s"""
  lazy val generalEntities: Vector[EntityType] = Vector(${generalEntities.mkString(", ")}) 
  lazy val contextEntities: Vector[EntityType] = Vector(${contextEntities.mkString(", ")})   
  lazy val requirementEntities: Vector[EntityType] = generalReqs ++ intentionalReqs
  $mkReqVectors
"""
  lazy val mkAttributeTypes = aggregateAttrTypes + attrVectors
  lazy val entities = generalEntities ++ contextEntities ++ 
    requriementEntities.collect { case (_, rs) =>  rs  } .flatten
  lazy val mkRelationTypes = relations.mkString(", ")
  lazy val mkReqVectors = 
    reqTypes.map(r => (r, r.decapitalize + "s")).collect { case (r, decap)  =>
      s"""lazy val $decap: Vector[EntityType] = Vector(${requriementEntities(r).mkString(", ")})"""
    } .mkString("\n  ")
  lazy val mkEnumTraits = "\n//Enum traits\n" + 
    enums.keysIterator.map(e => enumToScala(e, enums(e), attributeDefaultValues(e))).mkString("\n  ")  
  
  lazy val mkConcreteAttrs = "//Concrete attributes" + attributes.collect { 
    case (at, as) => as.map(a => attrToScala(a, at)).mkString 
  } .mkString
  
  lazy val mkAbstrReqs = "\n//Abstract requirement traits" + reqTypes.map(abstractReqToScala).mkString
  
  lazy val mkConcreteEnts = "\n//Concrete entities" +
    generalEntities.map(e => caseEntityToScala(e, "General")).mkString +
    contextEntities.map(e => caseEntityToScala(e, "Context")).mkString +
    requriementEntities.collect { case (rt, rs) =>  rs.map(r => caseEntityToScala(r, rt)).mkString } .mkString
  
  lazy val mkConcreteRels = "\n//Concrete relations\n" +
    relations.map(r => s"case object $r extends RelationType").mkString("\n")
    
  lazy val mkFactoryTraits = "\n\n//Factory traits" + 
    mkRelationFactory + mkHeadFactory + mkHeadTypeFactory + mkImplicitFactoryObjects
  
  lazy val reqTypes = requriementEntities.keysIterator.toVector
  lazy val attrTypes = attributes.keysIterator.toVector
  lazy val aggregateAttrTypes = attrTypes.map(a => a.toLowerCase + "Attributes").mkString(" ++ ") + "\n"
  lazy val attrVectors = attrTypes.map( a => 
      s"""  lazy val ${a.toLowerCase}Attributes: Vector[${a}Type] = Vector(${attributes(a).mkString(", ")})""" ).mkString("\n")
    
  def enumToScala(et: String, values: Seq[String], default: String) = s"""
trait $et extends Enum[$et] { val enumCompanion = $et }
trait ${et}Companion extends EnumCompanion[$et] { 
  val values = Vector(${values.mkString(", ")})
  val default = $default
}
trait ${et}Attribute extends Attribute[$et]
trait ${et}Type extends AttributeType[${et}] {
  val default = ${et}.default
  override  def apply(value: ${et}): ${et}Attribute
}
case object $et extends ${et}Companion
${values.map(v => s"case object $v extends $et").mkString("\n")}
   
"""

  def attrToScala(a: String, tpe: String) = s"""
case class $a(value: $tpe) extends ${tpe}Attribute { override val myType = $a }
case object $a extends ${tpe}Type 
"""

  def abstractReqToScala(r: String) = s"""
trait $r extends Requirement
case object $r extends AbstractSelector { type AbstractType = $r } 
"""

  def caseEntityToScala(e: String, extnds: String) = s"""
case class $e(id: String) extends $extnds { override val myType: EntityType = $e }
case object $e extends EntityType
"""

  def mkRelationFactory = s"""
trait RelationFactory {
  self: Entity =>
${relations.map(relationMethods).mkString("\n")}
}
"""  

  def relationMethods(r: String) = 
    s"  def $r(elems: Elem*) = Relation(this, reqT.$r, Model(elems:_*))\n" +
    s"  def $r(submodel: Model) = Relation(this, reqT.$r, submodel)"

  def mkHeadFactory = s"""
trait HeadFactory {
  self: Entity =>
${relations.map(headMethod).mkString("\n")}
}
"""
  def headMethod(r: String) = s"  def $r = Head(this, reqT.$r)"

  def mkHeadTypeFactory = s"""
trait HeadTypeFactory {
  self: EntityType =>
${relations.map(headTypeMethod).mkString("\n")}
}
"""
  def headTypeMethod(r: String) = s"  def $r = HeadType(this, reqT.$r)"

  def mkImplicitFactoryObjects = {
    s"""
trait ImplicitFactoryObjects extends CanMakeAttr { //mixed in by package object reqT
${defaultAttributes.map(_.toString).map(mkImplObj(_)).mkString("\n")}  
${defaultInterpretedAttributes.map(_.toString).map(mkImplObj(_)).mkString("\n")}  
$mkImplicitAttrMakers

$mkEnumImplicits
  lazy val attributeFromString = Map[String, String => Attribute[_]](
${defaultAttributes.map(_.toString).map(mkDefaultAttr(_)).mkString("\n")}
${defaultInterpretedAttributes.map(_.toString).map(mkDefaultAttr(_)).mkString("\n")}
$mkAttrFromStringMappings 
  )
  lazy val entityFromString = Map[String, String => Entity](
${defaultEntities.map(_.toString).map(mkDefaultEnt(_)).mkString("\n")}
$mkEntFromStringMappings
  )
}
"""
  }

  def mkImplObj(da: String) = 
    s"""  implicit object make$da extends AttrMaker[$da] { def apply(s: String): $da = $da(s.toString) }"""

  def mkImplicitAttrMakers = attributes.collect { 
    case (at, as) => as.map(a => attrMakerToScala(a, at)).mkString  } .mkString

  def attrMakerToScala(a: String, tpe: String) = s"""
  implicit object make$a extends AttrMaker[$a] { def apply(s: String): $a = $a(s.to$tpe) }"""
  
  def mkEnumImplicits = enums.collect { 
    case (e, _) => attributes(e).map(a => enumImplicit(a, e)).mkString  } .mkString

  def mkDefaultAttr(da: String) = s"""    "$da" -> makeAttribute[$da] _ ,"""  
  def mkDefaultEnt (de: String) = s"""    "$de" -> $de.apply _ ,"""  
    
  def enumImplicit(a: String, e: String) = s"""
  implicit class StringTo$e(s: String) { def to$e = $e.valueOf(s)}
"""
  def mkAttrFromStringMappings = attributes.collect { 
      case (_, as) => as.map(attrMapping).mkString(",\n")  } .mkString(",\n") 
  
  def mkEntFromStringMappings = entities.map(entMapping).mkString(",\n")
  
  def attrMapping(a: String) = s"""    "$a" -> makeAttribute[$a] _ """
  def entMapping(e: String) =  s"""    "$e" -> $e.apply _ """

}



























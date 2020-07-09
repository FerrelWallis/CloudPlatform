package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.MySQLProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import com.github.tototoshi.slick.MySQLJodaSupport._
  import org.joda.time.DateTime
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Ak.schema ++ Dutys.schema ++ Soft.schema ++ Users.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Ak
   *  @param accesskeyid Database column AccessKeyId SqlType(TEXT)
   *  @param accesskeysecret Database column AccessKeySecret SqlType(TEXT) */
  case class AkRow(accesskeyid: String, accesskeysecret: String)
  /** GetResult implicit for fetching AkRow objects using plain SQL queries */
  implicit def GetResultAkRow(implicit e0: GR[String]): GR[AkRow] = GR{
    prs => import prs._
    AkRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table ak. Objects of this class serve as prototypes for rows in queries. */
  class Ak(_tableTag: Tag) extends profile.api.Table[AkRow](_tableTag, Some("cloudplatform"), "ak") {
    def * = (accesskeyid, accesskeysecret) <> (AkRow.tupled, AkRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(accesskeyid), Rep.Some(accesskeysecret))).shaped.<>({r=>import r._; _1.map(_=> AkRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column AccessKeyId SqlType(TEXT) */
    val accesskeyid: Rep[String] = column[String]("AccessKeyId")
    /** Database column AccessKeySecret SqlType(TEXT) */
    val accesskeysecret: Rep[String] = column[String]("AccessKeySecret")
  }
  /** Collection-like TableQuery object for table Ak */
  lazy val Ak = new TableQuery(tag => new Ak(tag))

  /** Entity class storing rows of table Dutys
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param taskname Database column taskname SqlType(VARCHAR), Length(255,true)
   *  @param uid Database column uid SqlType(INT)
   *  @param sabbrename Database column sabbrename SqlType(TEXT)
   *  @param sname Database column sname SqlType(TEXT)
   *  @param subtime Database column subtime SqlType(TEXT)
   *  @param finitime Database column finitime SqlType(TEXT)
   *  @param status Database column status SqlType(TEXT)
   *  @param input Database column input SqlType(TEXT)
   *  @param param Database column param SqlType(TEXT)
   *  @param elements Database column elements SqlType(TEXT) */
  case class DutysRow(id: Int, taskname: String, uid: Int, sabbrename: String, sname: String, subtime: String, finitime: String, status: String, input: String, param: String, elements: String)
  /** GetResult implicit for fetching DutysRow objects using plain SQL queries */
  implicit def GetResultDutysRow(implicit e0: GR[Int], e1: GR[String]): GR[DutysRow] = GR{
    prs => import prs._
    DutysRow.tupled((<<[Int], <<[String], <<[Int], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table dutys. Objects of this class serve as prototypes for rows in queries. */
  class Dutys(_tableTag: Tag) extends profile.api.Table[DutysRow](_tableTag, Some("cloudplatform"), "dutys") {
    def * = (id, taskname, uid, sabbrename, sname, subtime, finitime, status, input, param, elements) <> (DutysRow.tupled, DutysRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(taskname), Rep.Some(uid), Rep.Some(sabbrename), Rep.Some(sname), Rep.Some(subtime), Rep.Some(finitime), Rep.Some(status), Rep.Some(input), Rep.Some(param), Rep.Some(elements))).shaped.<>({r=>import r._; _1.map(_=> DutysRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column taskname SqlType(VARCHAR), Length(255,true) */
    val taskname: Rep[String] = column[String]("taskname", O.Length(255,varying=true))
    /** Database column uid SqlType(INT) */
    val uid: Rep[Int] = column[Int]("uid")
    /** Database column sabbrename SqlType(TEXT) */
    val sabbrename: Rep[String] = column[String]("sabbrename")
    /** Database column sname SqlType(TEXT) */
    val sname: Rep[String] = column[String]("sname")
    /** Database column subtime SqlType(TEXT) */
    val subtime: Rep[String] = column[String]("subtime")
    /** Database column finitime SqlType(TEXT) */
    val finitime: Rep[String] = column[String]("finitime")
    /** Database column status SqlType(TEXT) */
    val status: Rep[String] = column[String]("status")
    /** Database column input SqlType(TEXT) */
    val input: Rep[String] = column[String]("input")
    /** Database column param SqlType(TEXT) */
    val param: Rep[String] = column[String]("param")
    /** Database column elements SqlType(TEXT) */
    val elements: Rep[String] = column[String]("elements")
  }
  /** Collection-like TableQuery object for table Dutys */
  lazy val Dutys = new TableQuery(tag => new Dutys(tag))

  /** Entity class storing rows of table Soft
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param types Database column types SqlType(VARCHAR), Length(255,true)
   *  @param abbrename Database column abbrename SqlType(VARCHAR), Length(255,true)
   *  @param sname Database column sname SqlType(TEXT)
   *  @param descreption Database column descreption SqlType(TEXT)
   *  @param pic Database column pic SqlType(TEXT)
   *  @param likefreq Database column likefreq SqlType(INT)
   *  @param money Database column money SqlType(INT) */
  case class SoftRow(id: Int, types: String, abbrename: String, sname: String, descreption: String, pic: String, likefreq: Int, money: Int)
  /** GetResult implicit for fetching SoftRow objects using plain SQL queries */
  implicit def GetResultSoftRow(implicit e0: GR[Int], e1: GR[String]): GR[SoftRow] = GR{
    prs => import prs._
    SoftRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[String], <<[String], <<[Int], <<[Int]))
  }
  /** Table description of table soft. Objects of this class serve as prototypes for rows in queries. */
  class Soft(_tableTag: Tag) extends profile.api.Table[SoftRow](_tableTag, Some("cloudplatform"), "soft") {
    def * = (id, types, abbrename, sname, descreption, pic, likefreq, money) <> (SoftRow.tupled, SoftRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(types), Rep.Some(abbrename), Rep.Some(sname), Rep.Some(descreption), Rep.Some(pic), Rep.Some(likefreq), Rep.Some(money))).shaped.<>({r=>import r._; _1.map(_=> SoftRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column types SqlType(VARCHAR), Length(255,true) */
    val types: Rep[String] = column[String]("types", O.Length(255,varying=true))
    /** Database column abbrename SqlType(VARCHAR), Length(255,true) */
    val abbrename: Rep[String] = column[String]("abbrename", O.Length(255,varying=true))
    /** Database column sname SqlType(TEXT) */
    val sname: Rep[String] = column[String]("sname")
    /** Database column descreption SqlType(TEXT) */
    val descreption: Rep[String] = column[String]("descreption")
    /** Database column pic SqlType(TEXT) */
    val pic: Rep[String] = column[String]("pic")
    /** Database column likefreq SqlType(INT) */
    val likefreq: Rep[Int] = column[Int]("likefreq")
    /** Database column money SqlType(INT) */
    val money: Rep[Int] = column[Int]("money")
  }
  /** Collection-like TableQuery object for table Soft */
  lazy val Soft = new TableQuery(tag => new Soft(tag))

  /** Entity class storing rows of table Users
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param phone Database column phone SqlType(VARCHAR), Length(255,true)
   *  @param email Database column email SqlType(VARCHAR), Length(255,true)
   *  @param pwd Database column pwd SqlType(VARCHAR), Length(255,true)
   *  @param name Database column name SqlType(VARCHAR), Length(255,true)
   *  @param company Database column company SqlType(VARCHAR), Length(255,true)
   *  @param authority Database column authority SqlType(VARCHAR), Length(255,true)
   *  @param like Database column like SqlType(TEXT) */
  case class UsersRow(id: Int, phone: String, email: String, pwd: String, name: String, company: String, authority: String, like: String)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Int], e1: GR[String]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends profile.api.Table[UsersRow](_tableTag, Some("cloudplatform"), "users") {
    def * = (id, phone, email, pwd, name, company, authority, like) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(phone), Rep.Some(email), Rep.Some(pwd), Rep.Some(name), Rep.Some(company), Rep.Some(authority), Rep.Some(like))).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column phone SqlType(VARCHAR), Length(255,true) */
    val phone: Rep[String] = column[String]("phone", O.Length(255,varying=true))
    /** Database column email SqlType(VARCHAR), Length(255,true) */
    val email: Rep[String] = column[String]("email", O.Length(255,varying=true))
    /** Database column pwd SqlType(VARCHAR), Length(255,true) */
    val pwd: Rep[String] = column[String]("pwd", O.Length(255,varying=true))
    /** Database column name SqlType(VARCHAR), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255,varying=true))
    /** Database column company SqlType(VARCHAR), Length(255,true) */
    val company: Rep[String] = column[String]("company", O.Length(255,varying=true))
    /** Database column authority SqlType(VARCHAR), Length(255,true) */
    val authority: Rep[String] = column[String]("authority", O.Length(255,varying=true))
    /** Database column like SqlType(TEXT) */
    val like: Rep[String] = column[String]("like")
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}

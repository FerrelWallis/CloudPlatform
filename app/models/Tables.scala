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
  lazy val schema: profile.SchemaDescription = Array(Duty.schema, Dutys.schema, Like.schema, Likefreq.schema, User.schema, Users.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Duty
   *  @param did Database column did SqlType(INT)
   *  @param phone Database column phone SqlType(VARCHAR), Length(255,true)
   *  @param sid Database column sid SqlType(INT)
   *  @param sname Database column sname SqlType(VARCHAR), Length(255,true)
   *  @param subtime Database column subtime SqlType(VARCHAR), Length(255,true)
   *  @param finitime Database column finitime SqlType(VARCHAR), Length(255,true), Default(None)
   *  @param status Database column status SqlType(VARCHAR), Length(255,true) */
  case class DutyRow(did: Int, phone: String, sid: Int, sname: String, subtime: String, finitime: Option[String] = None, status: String)
  /** GetResult implicit for fetching DutyRow objects using plain SQL queries */
  implicit def GetResultDutyRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[DutyRow] = GR{
    prs => import prs._
    DutyRow.tupled((<<[Int], <<[String], <<[Int], <<[String], <<[String], <<?[String], <<[String]))
  }
  /** Table description of table duty. Objects of this class serve as prototypes for rows in queries. */
  class Duty(_tableTag: Tag) extends profile.api.Table[DutyRow](_tableTag, Some("cloudplatform"), "duty") {
    def * = (did, phone, sid, sname, subtime, finitime, status) <> (DutyRow.tupled, DutyRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(did), Rep.Some(phone), Rep.Some(sid), Rep.Some(sname), Rep.Some(subtime), finitime, Rep.Some(status))).shaped.<>({r=>import r._; _1.map(_=> DutyRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column did SqlType(INT) */
    val did: Rep[Int] = column[Int]("did")
    /** Database column phone SqlType(VARCHAR), Length(255,true) */
    val phone: Rep[String] = column[String]("phone", O.Length(255,varying=true))
    /** Database column sid SqlType(INT) */
    val sid: Rep[Int] = column[Int]("sid")
    /** Database column sname SqlType(VARCHAR), Length(255,true) */
    val sname: Rep[String] = column[String]("sname", O.Length(255,varying=true))
    /** Database column subtime SqlType(VARCHAR), Length(255,true) */
    val subtime: Rep[String] = column[String]("subtime", O.Length(255,varying=true))
    /** Database column finitime SqlType(VARCHAR), Length(255,true), Default(None) */
    val finitime: Rep[Option[String]] = column[Option[String]]("finitime", O.Length(255,varying=true), O.Default(None))
    /** Database column status SqlType(VARCHAR), Length(255,true) */
    val status: Rep[String] = column[String]("status", O.Length(255,varying=true))

    /** Primary key of Duty (database name duty_PK) */
    val pk = primaryKey("duty_PK", (did, phone))
  }
  /** Collection-like TableQuery object for table Duty */
  lazy val Duty = new TableQuery(tag => new Duty(tag))

  /** Entity class storing rows of table Dutys
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param taskname Database column taskname SqlType(VARCHAR), Length(255,true)
   *  @param uid Database column uid SqlType(INT)
   *  @param sid Database column sid SqlType(TEXT)
   *  @param sname Database column sname SqlType(TEXT)
   *  @param subtime Database column subtime SqlType(TEXT)
   *  @param finitime Database column finitime SqlType(TEXT)
   *  @param status Database column status SqlType(TEXT) */
  case class DutysRow(id: Int, taskname: String, uid: Int, sid: String, sname: String, subtime: String, finitime: String, status: String)
  /** GetResult implicit for fetching DutysRow objects using plain SQL queries */
  implicit def GetResultDutysRow(implicit e0: GR[Int], e1: GR[String]): GR[DutysRow] = GR{
    prs => import prs._
    DutysRow.tupled((<<[Int], <<[String], <<[Int], <<[String], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table dutys. Objects of this class serve as prototypes for rows in queries. */
  class Dutys(_tableTag: Tag) extends profile.api.Table[DutysRow](_tableTag, Some("cloudplatform"), "dutys") {
    def * = (id, taskname, uid, sid, sname, subtime, finitime, status) <> (DutysRow.tupled, DutysRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(taskname), Rep.Some(uid), Rep.Some(sid), Rep.Some(sname), Rep.Some(subtime), Rep.Some(finitime), Rep.Some(status))).shaped.<>({r=>import r._; _1.map(_=> DutysRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column taskname SqlType(VARCHAR), Length(255,true) */
    val taskname: Rep[String] = column[String]("taskname", O.Length(255,varying=true))
    /** Database column uid SqlType(INT) */
    val uid: Rep[Int] = column[Int]("uid")
    /** Database column sid SqlType(TEXT) */
    val sid: Rep[String] = column[String]("sid")
    /** Database column sname SqlType(TEXT) */
    val sname: Rep[String] = column[String]("sname")
    /** Database column subtime SqlType(TEXT) */
    val subtime: Rep[String] = column[String]("subtime")
    /** Database column finitime SqlType(TEXT) */
    val finitime: Rep[String] = column[String]("finitime")
    /** Database column status SqlType(TEXT) */
    val status: Rep[String] = column[String]("status")
  }
  /** Collection-like TableQuery object for table Dutys */
  lazy val Dutys = new TableQuery(tag => new Dutys(tag))

  /** Entity class storing rows of table Like
   *  @param phone Database column phone SqlType(VARCHAR), Length(255,true)
   *  @param sid Database column sid SqlType(INT)
   *  @param sname Database column sname SqlType(VARCHAR), Length(255,true) */
  case class LikeRow(phone: String, sid: Int, sname: String)
  /** GetResult implicit for fetching LikeRow objects using plain SQL queries */
  implicit def GetResultLikeRow(implicit e0: GR[String], e1: GR[Int]): GR[LikeRow] = GR{
    prs => import prs._
    LikeRow.tupled((<<[String], <<[Int], <<[String]))
  }
  /** Table description of table like. Objects of this class serve as prototypes for rows in queries. */
  class Like(_tableTag: Tag) extends profile.api.Table[LikeRow](_tableTag, Some("cloudplatform"), "like") {
    def * = (phone, sid, sname) <> (LikeRow.tupled, LikeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(phone), Rep.Some(sid), Rep.Some(sname))).shaped.<>({r=>import r._; _1.map(_=> LikeRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column phone SqlType(VARCHAR), Length(255,true) */
    val phone: Rep[String] = column[String]("phone", O.Length(255,varying=true))
    /** Database column sid SqlType(INT) */
    val sid: Rep[Int] = column[Int]("sid")
    /** Database column sname SqlType(VARCHAR), Length(255,true) */
    val sname: Rep[String] = column[String]("sname", O.Length(255,varying=true))

    /** Primary key of Like (database name like_PK) */
    val pk = primaryKey("like_PK", (phone, sid))
  }
  /** Collection-like TableQuery object for table Like */
  lazy val Like = new TableQuery(tag => new Like(tag))

  /** Entity class storing rows of table Likefreq
   *  @param sid Database column sid SqlType(INT), PrimaryKey
   *  @param sname Database column sname SqlType(VARCHAR), Length(255,true)
   *  @param likenum Database column likenum SqlType(INT) */
  case class LikefreqRow(sid: Int, sname: String, likenum: Int)
  /** GetResult implicit for fetching LikefreqRow objects using plain SQL queries */
  implicit def GetResultLikefreqRow(implicit e0: GR[Int], e1: GR[String]): GR[LikefreqRow] = GR{
    prs => import prs._
    LikefreqRow.tupled((<<[Int], <<[String], <<[Int]))
  }
  /** Table description of table likefreq. Objects of this class serve as prototypes for rows in queries. */
  class Likefreq(_tableTag: Tag) extends profile.api.Table[LikefreqRow](_tableTag, Some("cloudplatform"), "likefreq") {
    def * = (sid, sname, likenum) <> (LikefreqRow.tupled, LikefreqRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(sid), Rep.Some(sname), Rep.Some(likenum))).shaped.<>({r=>import r._; _1.map(_=> LikefreqRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column sid SqlType(INT), PrimaryKey */
    val sid: Rep[Int] = column[Int]("sid", O.PrimaryKey)
    /** Database column sname SqlType(VARCHAR), Length(255,true) */
    val sname: Rep[String] = column[String]("sname", O.Length(255,varying=true))
    /** Database column likenum SqlType(INT) */
    val likenum: Rep[Int] = column[Int]("likenum")
  }
  /** Collection-like TableQuery object for table Likefreq */
  lazy val Likefreq = new TableQuery(tag => new Likefreq(tag))

  /** Entity class storing rows of table User
   *  @param phone Database column phone SqlType(VARCHAR), PrimaryKey, Length(255,true)
   *  @param email Database column email SqlType(VARCHAR), Length(255,true)
   *  @param pwd Database column pwd SqlType(VARCHAR), Length(255,true)
   *  @param name Database column name SqlType(VARCHAR), Length(255,true)
   *  @param company Database column company SqlType(VARCHAR), Length(255,true)
   *  @param authority Database column authority SqlType(VARCHAR), Length(255,true) */
  case class UserRow(phone: String, email: String, pwd: String, name: String, company: String, authority: String)
  /** GetResult implicit for fetching UserRow objects using plain SQL queries */
  implicit def GetResultUserRow(implicit e0: GR[String]): GR[UserRow] = GR{
    prs => import prs._
    UserRow.tupled((<<[String], <<[String], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table user. Objects of this class serve as prototypes for rows in queries. */
  class User(_tableTag: Tag) extends profile.api.Table[UserRow](_tableTag, Some("cloudplatform"), "user") {
    def * = (phone, email, pwd, name, company, authority) <> (UserRow.tupled, UserRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(phone), Rep.Some(email), Rep.Some(pwd), Rep.Some(name), Rep.Some(company), Rep.Some(authority))).shaped.<>({r=>import r._; _1.map(_=> UserRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column phone SqlType(VARCHAR), PrimaryKey, Length(255,true) */
    val phone: Rep[String] = column[String]("phone", O.PrimaryKey, O.Length(255,varying=true))
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
  }
  /** Collection-like TableQuery object for table User */
  lazy val User = new TableQuery(tag => new User(tag))

  /** Entity class storing rows of table Users
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param phone Database column phone SqlType(VARCHAR), Length(255,true)
   *  @param email Database column email SqlType(VARCHAR), Length(255,true)
   *  @param pwd Database column pwd SqlType(VARCHAR), Length(255,true)
   *  @param name Database column name SqlType(VARCHAR), Length(255,true)
   *  @param company Database column company SqlType(VARCHAR), Length(255,true)
   *  @param authority Database column authority SqlType(VARCHAR), Length(255,true) */
  case class UsersRow(id: Int, phone: String, email: String, pwd: String, name: String, company: String, authority: String)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Int], e1: GR[String]): GR[UsersRow] = GR{
    prs => import prs._
    UsersRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends profile.api.Table[UsersRow](_tableTag, Some("cloudplatform"), "users") {
    def * = (id, phone, email, pwd, name, company, authority) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(phone), Rep.Some(email), Rep.Some(pwd), Rep.Some(name), Rep.Some(company), Rep.Some(authority))).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

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
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}

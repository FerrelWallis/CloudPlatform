package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object eprimer extends MyFile with MyStringTool with MyMapTool{
  
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "eprimer Success!"

    val seqFile=new File(dutyDir,"seq.txt")
    val mishybFile=new File(dutyDir,"mishyb.txt")
    val mispriFile=new File(dutyDir,"mispri.txt")
    val file = request.body.file("table1").get
    file.ref.getPath.moveToFile(seqFile)

    val (input,mishyblibraryfile,mispriminglibraryfile)=
      if(request.body.file("table2").isEmpty && request.body.file("table3").isEmpty)
        (file.filename,"","")
      else if(request.body.file("table2").isEmpty) {
        request.body.file("table3").get.ref.getPath.moveToFile(mispriFile)
        (file.filename+"/"+request.body.file("table3").get.filename,""," -mispriminglibraryfile "+mispriFile.getAbsolutePath)
      } else if(request.body.file("table3").isEmpty) {
        request.body.file("table2").get.ref.getPath.moveToFile(mishybFile)
        (file.filename+"/"+request.body.file("table2").get.filename," -mishyblibraryfile "+mishybFile.getAbsolutePath,"")
      } else {
        request.body.file("table3").get.ref.getPath.moveToFile(mispriFile)
        request.body.file("table2").get.ref.getPath.moveToFile(mishybFile)
        (file.filename+"/"+request.body.file("table2").get.filename+"/"+request.body.file("table3").get.filename," -mishyblibraryfile "+mishybFile.getAbsolutePath," -mispriminglibraryfile "+mispriFile.getAbsolutePath)
      }

    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)
    mishybFile.setExecutable(true,false)
    mishybFile.setReadable(true,false)
    mishybFile.setWritable(true,false)
    mispriFile.setExecutable(true,false)
    mispriFile.setReadable(true,false)
    mispriFile.setWritable(true,false)

    val includedregion=if(params("includedregion")=="") "" else "/Included region(s)：" + params("includedregion")
    val targetregion=if(params("targetregion")=="") "" else "/Target region(s)：" + params("targetregion")
    val excludedregion=if(params("excludedregion")=="") "" else "/Excluded region(s)：" + params("excludedregion")
    val forwardinput=if(params("forwardinput")=="") "" else "/Forward input primer sequence to check：" + params("forwardinput")
    val reverseinput=if(params("reverseinput")=="") "" else "/Reverse input primer sequence to check：" + params("reverseinput")

    val param="/Pick hybridization probe：" + params("hybridprobe") + "/Number of results to return：" +
      params("numreturn") + includedregion + targetregion + excludedregion + forwardinput +
      reverseinput + "/GC clamp：" + params("gcclamp") + "/Primer optimum size：" + params("optsize") +
      "/Primer minimum size：" + params("minsize") + "/Primer maximum size：" + params("maxsize") +
      "/Primer optimum Tm：" + params("opttm") + "/Primer minimum Tm：" + params("mintm") +
      "/Primer maximum Tm：" + params("maxtm") + "/Maximum difference in Tm of primers：" + params("maxdifftm") +
      "/Primer optimum GC percent：" + params("ogcpercent") + "/Primer minimum GC percent：" + params("mingc") +
      "/Primer maximum GC percent：" + params("maxgc") + "/Salt concentration (mM)：" + params("saltconc") +
      "/DNA concentration (nM)：" + params("dnaconc") + "/Maximum polynucleotide repeat：" + params("maxpolyx") +
      "/Product optimum size：" + params("psizeopt") + "/Product size range：" + params("prange") +
      "/Product optimum Tm：" + params("ptmopt") + "/Product minimum Tm：" + params("ptmmin")

    try {
      val includedregion_command=
        if(params("includedregion")=="") "" else " -includedregion \"" + params("includedregion") + "\""
      val targetregion_command=
        if(params("targetregion")=="") "" else " -targetregion \"" + params("targetregion") + "\""
      val excludedregion_command=
        if(params("excludedregion")=="") "" else " -excludedregion \"" + params("excludedregion") + "\""
      val forwardinput_command=
        if(params("forwardinput")=="") "" else " -forwardinput \"" + params("forwardinput") + "\""
      val reverseinput_command=
        if(params("reverseinput")=="") "" else " -reverseinput \"" + params("reverseinput") + "\""

      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/eprimer32 -sequence " + seqFile.getAbsolutePath +
        " -outfile " + dutyDir + "/out/sequence.eprimer32" + mishyblibraryfile + mispriminglibraryfile +
        " -hybridprobe \"" + params("hybridprobe") + "\" -numreturn \"" + params("numreturn") + "\"" +
        includedregion_command + targetregion_command + excludedregion_command + forwardinput_command +
        reverseinput_command + " -gcclamp \"" + params("gcclamp") + "\" -optsize \"" + params("optsize") + "\" -minsize \"" +
        params("minsize") + "\" -maxsize \"" + params("maxsize") + "\" -opttm \"" + params("opttm") + "\" -mintm \"" + params("mintm") +
        "\" -maxtm \"" + params("maxtm") + "\" -maxdifftm \"" + params("maxdifftm") + "\" -ogcpercent \"" + params("ogcpercent") +
        "\" -mingc \"" + params("mingc") + "\" -maxgc \"" + params("maxgc") + "\" -saltconc \"" + params("saltconc") + "\" -dnaconc \"" +
        params("dnaconc") + "\" -maxpolyx \"" + params("maxpolyx") + "\" -psizeopt \"" + params("psizeopt") + "\" -prange \"" +
        params("prange") + "\" -ptmopt \"" + params("ptmopt") + "\" -ptmmin \"" + params("ptmmin") + "\" -ptmmax \"" +
        params("ptmmax") + "\""

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Emboss Eprimer3", input, param, "")
  }

}

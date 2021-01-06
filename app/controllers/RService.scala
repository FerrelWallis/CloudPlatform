package controllers

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}

import dao.dutyDao
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Headers, Result}
import utils.{CompressUtil, ExecCommand, MyStringTool, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.JavaConverters._
import utils.Implicits._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class RService @Inject()(cc: ControllerComponents,dutydao:dutyDao,dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool{

  val userDutyDir: String =Utils.path+"users/"

  //创建用户任务文件夹和结果文件夹
  def creatUserDir(uid:String,taskname:String): String ={
    new File(userDutyDir+uid+"/"+taskname).mkdir()
    new File(userDutyDir+uid+"/"+taskname+"/out").mkdir()
    new File(userDutyDir+uid+"/"+taskname+"/temp").mkdir()

    userDutyDir+uid+"/"+taskname
  }

  def creatZip(target:String,path:String): Unit ={
    new File(target).createNewFile()
    CompressUtil.zip(path,target)
  }

  def creatZip(path:String): Unit ={
    val target=path+"/outcome.zip"
    new File(target).createNewFile()
    CompressUtil.zip(path+"/out",target)
  }

  def getPics(id:String,taskname:String): Array[String] = {
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().filter(_.getName.contains("png")).map(_.getAbsolutePath)
    files
  }

  def getFiles(id:String,taskname:String): Array[String] = {
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().map(_.getAbsolutePath)
    files
  }



  val CCA: Array[String] =Array("rdacca.pdf", "percent.xls", "samples.xls", "species.xls", "envi.xls")
  val CCAins: Array[String] =Array("CCA/RDA结果图", "百分比表", "样本坐标表", "物种坐标表", "环境因子坐标表")

  val Heat: Array[String] =Array("heatmap.pdf")
  val Heatins: Array[String] =Array("热图")

  val PCA: Array[String] =Array("pca.pdf", "pca.sdev.xls", "pca.rotation.xls")
  val PCAins: Array[String] =Array("PCA结果图", "PCA值表格", "特征向量矩阵表格")

  var GO: Array[String] =Array("go.Go.enrich.pdf", "go_stack.pdf", "go.Go.enrich.xls")
  var GOins: Array[String] =Array("GO富集分析结果图", "GO富集分析柱状图", "GO富集分析结果")

  var KEGG: Array[String] =Array("ko.Ko.enrich.pdf", "ko_dodge.pdf", "ko.Ko.enrich.xls")
  var KEGGins: Array[String] =Array("KEGG富集分析结果图", "KEGG富集分析柱状图", "KEGG富集分析结果")

  var Box: Array[String] =Array("box.pdf")
  var Boxins: Array[String] =Array("盒型图")

  var Net: Array[String] =Array("result.xls")
  var Netins: Array[String] =Array("相关性系数及P值分析结果")

  var IGC: Array[String] =Array("cor.xls","pvalue.xls","pandv.xls","p_star.xls","heatmap.pdf")
  var IGCins: Array[String] =Array("相关性系数矩阵","p值矩阵","相关性系数c值和p值分析结果","根据p值生成的星星矩阵","热图")

  var TAX: Array[String] =Array("ko_table.xls","kegg_L1.txt","kegg_L2.txt","kegg_L3.txt","kegg_enzyme.txt","kegg_pathway.txt","pca.pdf","kegg_L1.pdf","kegg_L2.pdf","kegg_L3.pdf")
  var TAXins: Array[String] =Array("kegg丰度表","kegg pathway 第一个层级丰度表","kegg pathway 第二个层级丰度表","kegg pathway 第三个层级丰度表","kegg enzyme丰度表","kegg pathway丰度表","PCA图","第一个层级箱线图","第二个层级箱线图","第三个层级箱线图")

  var BAR: Array[String] =Array("bar_group.pdf")
  var BARins: Array[String] =Array("柱状图")

  var FAP: Array[String] =Array("functional_table.xls","functional_table.biom","outTables.zip","functional_otu.txt")
  var FAPins: Array[String] =Array("功能丰度表","biom格式的功能丰度表","每个功能相关的OTU的丰度表压缩包","与功能相关的otu列表，对outTables.zip中信息的综合统计")

  var PIC: Array[String] = Array("out.tre",
    "marker_predicted_and_nsti.tsv.gz",
    "pathways_out.zip",
    "EC_predicted.tsv.gz",
    "EC_metagenome_out.zip",
    "COG_predicted.tsv.gz",
    "COG_metagenome_out.zip",
    "KO_predicted.tsv.gz",
    "KO_metagenome_out.zip",
    "PFAM_predicted.tsv.gz",
    "PFAM_metagenome_out.zip",
    "TIGRFAM_predicted.tsv.gz",
    "TIGRFAM_metagenome_out.zip"
  )
  var PICins: Array[String] = Array("参考序列的树文件",
    "16S预测的拷贝数和NSTI",
    "压缩文件包括预测的通路丰度",
    "每个ASV/OTU中预测的EC数量",
    "EC结果,包括：预测EC数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的COG数量",
    "COG结果,包括：预测COG数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的KO数量",
    "KO结果,包括：预测KO数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的PFAM数量",
    "PFAM结果,包括：预测PFAM数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的TIGRFAM数量",
    "TIGRFAM结果,包括：预测TIGRFAM数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)"
  )

  var LEF: Array[String] =Array("lefse_LDA.xls","lefse_LDA.pdf","lefse_LDA.cladogram.pdf")
  var LEFins: Array[String] =Array("LDA判别分析结果","LDA分析柱图","进化分支图")

  var LF2: Array[String] =Array("lefse_LDA.xls","lefse_LDA_diff.xls","lefse_LDA.pdf","lefse_LDA.cladogram.pdf","lefse_LDA.features.pdf")
  var LF2ins: Array[String] =Array("LDA判别分析结果","LDA判别分析结果（仅含差异显著）","LDA分析柱图","进化分支图","差异特征图")

  var PMR: Array[String] =Array("sequence.eprimer32")
  var PMRins: Array[String] =Array("eprimer32结果序列")

  var VOC: Array[String] =Array("volcano.pdf")
  var VOCins: Array[String] =Array("火山图")

  var MHT: Array[String] =Array("manhattan.pdf")
  var MHTins: Array[String] =Array("曼哈顿图")

  var TRM: Array[String] =Array("treemap.pdf")
  var TRMins: Array[String] =Array("树状图")

  var VIO: Array[String] =Array("violin.pdf")
  var VIOins: Array[String] =Array("小提琴图")

  var FH: Array[String] =Array("Frequency_bar.pdf")
  var FHins: Array[String] =Array("频率直方图")

  var BRB: Array[String] =Array("breakHisto.pdf")
  var BRBins: Array[String] =Array("断轴柱状图")

  val PCO: Array[String] =Array("pca.pdf", "PCOA.sdev.xls")
  val PCOins: Array[String] =Array("PCoA结果图", "PCoA值表格")

  val BIS: Array[String] =Array("pie.pdf")
  val BISins: Array[String] =Array("饼图")

  val EBL: Array[String] =Array("error_bar.pdf")
  val EBLins: Array[String] =Array("误差折线图")

  val BB: Array[String] =Array("bubble.pdf")
  val BBins: Array[String] =Array("气泡图")

  val CIR: Array[String] =Array("circle.pdf")
  val CIRins: Array[String] =Array("circos圈图")

  val TT: Array[String] =Array("trans.xls")
  val TTins: Array[String] =Array("转置表格")

  val MTT: Array[String] =Array("merge2table.xls")
  val MTTins: Array[String] =Array("合并表格")

  val MMT: Array[String] =Array("mergeTables.xls")
  val MMTins: Array[String] =Array("合并表格")

  val TM: Array[String] =Array("treemap.pdf")
  val TMins: Array[String] =Array("树图")

  val TRY: Array[String] =Array("ternary.pdf")
  val TRYins: Array[String] =Array("三原图")

  val CCS: Array[String] =Array("phylum.xls", "phylum_circos.pdf")
  val CCSins: Array[String] =Array("作图OTU丰度信息表", "Circos物种关系图")

  val TC: Array[String] =Array("t_test_results.xls", "t_test_significant_results.xls")
  val TCins: Array[String] =Array("T检验结果文件", "T检验结果文件（仅差异显著）")

  val WT: Array[String] =Array("wilcox_test_results.xls", "wilcox_test_significant_results.xls")
  val WTins: Array[String] =Array("Wilcoxon秩和检验结果文件", "Wilcoxon秩和检验结果文件（仅差异显著）")

  val KWT: Array[String] =Array("kw_test_results.xls", "kw_test_significant_results.xls")
  val KWTins: Array[String] =Array("Kruskal-Wallis秩和检验结果文件", "Kruskal-Wallis秩和检验结果文件（仅差异显著）")

  val AOV: Array[String] =Array("aov_results.xls", "aov_significant_results.xls")
  val AOVins: Array[String] =Array("方差分析（ANOVA）结果文件", "方差分析（ANOVA）结果文件（仅差异显著）")

  val NMD: Array[String] =Array("nmds_sites.xls", "nmds.pdf")
  val NMDins: Array[String] =Array("NMDS分析结果文件", "nmds结果图")

  def getDownloadFiles(taskname:String,soft:String): Action[AnyContent] =Action { implicit request=>
    val id=request.session.get("userId").get

    val (outfiles,filesins)=
      if(soft=="PCA") (PCA,PCAins)
      else if(soft=="PCO") (PCO,PCOins)
      else if(soft=="CCA") (CCA,CCAins)
      else if(soft=="Heatmap") (Heat,Heatins)
      else if(soft=="Boxplot") (Box,Boxins)
      else if(soft=="NetWeight") (Net,Netins)
      else if(soft=="KEGG") (KEGG,KEGGins)
      else if(soft=="GO") (GO,GOins)
      else if(soft=="IGC" || soft=="ITC") (IGC,IGCins)
      else if(soft=="TAX") (TAX,TAXins)
      else if(soft=="BAR") (BAR,BARins)
      else if(soft=="FAP") (FAP,FAPins)
      else if(soft=="PIC") (PIC,PICins)
      else if(soft=="LEF") {
        val path=Utils.path+"/users/"+id+"/"+taskname
        if(new File(path+"/out/lefse_LDA.png").length()==0)
          (Array("lefse_LDA.xls","lefse_LDA.cladogram.pdf"),Array("LDA判别分析结果","进化分支图"))
        else (LEF,LEFins)
      }
      else if(soft=="LF2") (LF2,LF2ins)
      else if(soft=="ABI"){
        val path=Utils.path+"/users/"+id+"/"+taskname
        val seq=new File(path+"/out").listFiles().filter(_.getName.contains("sequence")).map(_.getName).head
        val seqins="序列文件"
        val pics=new File(path+"/out").listFiles().filter(x=>x.getName.contains("abiview")&&(!x.getName.contains("pdf"))).map(_.getName)
        val out=
          if(pics.length==1) "abiview.ps"
          else pics.filter(!_.contains("ps")).head
        val outins="abiview结果图"
        (Array(seq,out,"abiview.pdf"),Array(seqins,outins,"abiview结果图pdf文件"))
      }
      else if(soft=="RSQ" || soft=="GTF"){
        val path=Utils.path+"/users/"+id+"/"+taskname
        val seq=new File(path+"/out").listFiles().filter(_.getName.contains("sequence")).map(_.getName).head
        val seqins="输出序列文件"
        (Array(seq),Array(seqins))
      }
      else if(soft=="PMR") (PMR,PMRins)
      else if(soft=="VOC") (VOC,VOCins)
      else if(soft=="MHT") (MHT,MHTins)
      else if(soft=="TRM") (TRM,TRMins)
      else if(soft=="VIO") (VIO,VIOins)
      else if(soft=="FH") (FH,FHins)
      else if(soft=="BRB") (BRB,BRBins)
      else if(soft=="BIS") (BIS,BISins)
      else if(soft=="EBL") (EBL,EBLins)
      else if(soft=="BB") (BB,BBins)
      else if(soft=="CIR") (CIR,CIRins)
      else if(soft=="TT") (TT,TTins)
      else if(soft=="MTT") (MTT,MTTins)
      else if(soft=="MMT") (MMT,MMTins)
      else if(soft=="TM") (TM,TMins)
      else if(soft=="TRY") (TRY,TRYins)
      else if(soft=="CCS") (CCS,CCSins)
      else if(soft=="TC") (TC,TCins)
      else if(soft=="WT") (WT,WTins)
      else if(soft=="KWT") (KWT,KWTins)
      else if(soft=="AOV") (AOV,AOVins)
      else if(soft=="NMD") (NMD,NMDins)
      else (Array(""),Array(""))

    val files=outfiles.map{x=>
      Utils.path + "/users/" + id + "/" + taskname + "/out/" + x
    }

    Ok(Json.obj("files"->files,"name"->outfiles,"ins"->filesins))
  }

  def getReDrawPics(path:String): Array[String] ={
    val pics = new File(path+"/out").listFiles().filter(_.getName.contains("png")).map(_.getAbsolutePath)
    pics
  }

  def download(taskname:String,picname:String,suffix:String,num:Double): Action[AnyContent] = Action{ implicit request=>
    val file=new File(userDutyDir+request.session.get("userId").get+"/"+taskname+"/out/"+picname+"."+suffix)
    Ok.sendFile(file).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadFile(taskname:String,filename:String,num:Double): Action[AnyContent] =Action{ implicit request=>
    val path=new File(userDutyDir+request.session.get("userId").get+"/"+taskname+"/out/"+filename)
    Ok.sendFile(path).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + path.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadZip(taskname:String,num:Double): Action[AnyContent] =Action{ implicit request=>
    val path=new File(userDutyDir+request.session.get("userId").get+"/"+taskname+"/outcome.zip")
    Ok.sendFile(path).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + path.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadExamples(name:String): Action[AnyContent] =Action{ implicit request=>
    val file=new File(Utils.path+"files/examples/"+name)
    Ok.sendFile(file).withHeaders(
      CACHE_CONTROL->"max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def getPic(path:String,num:Double): Action[AnyContent] = Action{ implicit request=>
    val file = new File(path)
    SendImg(file,request.headers)
  }

  def getPdf(fileUrl:String): Action[AnyContent] = Action{ implicit request=>
    SendPdf(new File(fileUrl),request.headers)
  }

  def SendImg(file: File,headers:Headers): Result = {
    val lastModifiedStr = file.lastModified().toString
    val MimeType = "image/jpg"
    val byteArray = Files.readAllBytes(file.toPath)
    val ifModifiedSinceStr = headers.get(IF_MODIFIED_SINCE)
    if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
      NotModified
    } else {
      Ok(byteArray).as(MimeType).withHeaders(LAST_MODIFIED -> file.lastModified().toString)
    }
  }

  def SendPdf(file: File,headers:Headers): Result = {
    val lastModifiedStr = file.lastModified().toString
    val byteArray = Files.readAllBytes(file.toPath)
    val ifModifiedSinceStr = headers.get(IF_MODIFIED_SINCE)
    if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
      NotModified
    } else {
      Ok(byteArray).withHeaders(LAST_MODIFIED -> file.lastModified().toString)
    }
  }

  def jsonToMap(json:String): Map[String, String] = {
    scala.util.parsing.json.JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
  }

  def mapToJson(map:Map[String,String]): String = {
    Json.toJson(map).toString()
  }

}

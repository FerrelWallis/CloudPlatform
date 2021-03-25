package utils

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.json.Json
import slick.ast.JoinType.Zip

import scala.collection.mutable
import scala.io.Source
import scala.reflect.io.ZipArchive
import scala.util.parsing.json
//允许java代码
import scala.collection.JavaConverters._

object FilesUtils {

  def main(args: Array[String]): Unit = {

    var array = Array("test")
    println(array.toBuffer)
    array = array :+ "test2"
    println(array.toBuffer)

//    val elements=Json.obj("S.obs"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//      "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//      "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//      "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//      "flip"->"TRUE").toString,
//      "S.chao1"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//      "se.chao1"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//      "S.ACE"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//      "se.ACE"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//      "Shannon"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//      "Simpson"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//      "Pielou"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//      "goods_coverage"->Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
//        "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
//        "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
//        "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
//        "flip"->"TRUE").toString,
//    ).toString
//
//    println(elements)
//
//    var map1 = jsonToMap(elements)
//    map1 = map1.-("goods_coverage")
//    println(map1)
//    map1 = map1.+(("goods_coverage","test"))
//    println(map1)
//
//    println(map1.get("goods_coverage").get)
//
//    val map = jsonToMap(elements).get("Simpson").get
//    println(jsonToMap(map).get("lp").get)




//    println(elements.-("goods_coverage"))
//
//    println(elements.+("goods_coverage",Json.obj("test"->"test")))



//    val content = FileUtils.readLines(new File("F:\\CloudPlatform\\R\\bar\\input\\table.txt")).asScala.
//      filter(!_.trim.isEmpty).map{ line =>
//      line.trim.split("\\s+").mkString("\t")
//    }.mkString("\n")
//    println(content)
//    FileUtils.writeStringToFile(new File("F:\\CloudPlatform\\R\\bar\\input\\table2.txt"),content)

//    val result=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\test\\result.xls")).asScala
//    var resultFilter = Array("")
//    result.drop(1).foreach{ x=>
//      val ei = x.split("\t").filter(_.trim!="")
//      if(ei(3) != "NA" && ei(4) != "NA") {
//        val w=ei(3).trim.split("\t").last.toDouble
//        val c=ei(3).trim.split("\t").head.toDouble
//        if(w < 0.1 && Math.abs(c) < 1) {
//          resultFilter=resultFilter:+x
//        }
//      }
//    }
//
//    val (outedge, outnode) = resultFilter.drop(1).map{x =>
//      val e = x.split("\t").filter(_.trim!="")
//      (e(1) + "\t" + e(2) + "\t" + e(3) + "\t" + e(4),List(e(1), e(2)))
//    }.unzip
//
//    println("Node1\tNode2\tr\tP\n" + outedge.mkString("\n"))
//    println("Node\n" + outnode.flatten.distinct.mkString("\n"))


    //    val (names,colors) = FileUtils.readLines(new File("F:\\CloudPlatform\\R\\circos_species\\input\\colors.xls")).asScala.map{x=>
//      val temp = x.replaceAll("\"","").split("\t")
//      (temp.head,temp.last)
//    }.unzip
//
//
//    val groupnum = FileUtils.readLines(new File("F:\\CloudPlatform\\R\\circos_species\\input\\group.txt")).asScala.map{_.replaceAll("\"","").split("\t")(1)}.drop(1).distinct.length
//
//
//    println(names.toBuffer.takeRight(groupnum))
//    println(groupnum)

//    val file = FileUtils.readLines(new File("F:\\CloudPlatform\\files\\examples\\VennChart.txt")).asScala
//    val buffer = file.filter(_!="")
//    val file2 = FileUtils.readLines(new File("F:\\CloudPlatform\\files\\examples\\VennChartGroup.txt")).asScala
//    val head = file2.filter(_!="")
//
//    val out=buffer.map{x=>
//      x.split("\t").last
//    }.tail.mkString(":")
//
//    println(out)
//
//    (1 to 5).foreach{x=>
//      println(x)
//    }

  }


  def jsonToMap(json:String): Map[String, String] = {
    scala.util.parsing.json.JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
  }



  def test={
    val test=FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\学习文档\\云平台\\group.txt")).asScala
    val name=test.map{line=>
      val x=line.split('\t')

      line.split('\t').last
    }.distinct
    name.toList
  }


  def change={
    val fileFa=Source.fromFile("F:\\Eggplant\\files\\download\\" +
      "01.SME-HQ-reference.fasta")
    val lines=fileFa.getLines()
    var chr=""
    lines.filter(_.trim!="").foreach{x=>
      if(x.startsWith(">")){
        chr=x
        FileUtils.write(new File("F:\\Eggplant\\files\\download\\temp\\"+chr.substring(1)+".txt"),chr,true)
      }else{
        FileUtils.write(new File("F:\\Eggplant\\files\\download\\temp\\"+chr.substring(1)+".txt"),x,true)
      }
    }
  }

  def read={
    new File("F:\\Eggplant\\files\\download\\temp").listFiles().foreach{x=>
      val f=FileUtils.readLines(x).asScala
      f.foreach{line=>
        FileUtils.write(new File("F:\\Eggplant\\files\\download\\newhic.fasta"),line,true)
      }
    }
  }


  def joinAll ={
    val gff=takeGFF
    val kegg=takeKEGG
    val nr=takeNR
    val swiss=takeSwissport
    val pfam=takePfam
    val golist=joinGo(takeWego,takeGolist)
    gff.map{ g=>
      val k=kegg.getOrElse(g._1,("—","—"))
      val n=nr.getOrElse(g._1,"—")
      val s=swiss.getOrElse(g._1,"—")
      val p=pfam.getOrElse(g._1,("—","—"))
      val go=golist.getOrElse(g._1,("—","—"))
      Array(g._1,g._2,g._3,g._4,g._5,k._1,k._2,p._1,p._2,n,s,go._1,go._2)
    }
  }


  def takeLength={
    val fileFa=FileUtils.readLines(new File("F:\\Eggplant\\files\\download\\" +
      "hic.fasta")).asScala
    var num = 0
    var chr=""
    var mapFa=mutable.HashMap[String,Int]()
    var count=0;
      fileFa.filter(_.trim!="").foreach{x=>
        count=count+1
        if(x.startsWith(">")){
          if(chr.indexOf(">E")==0){
            mapFa+=(chr->num)
          }
          num =0
          chr=x
        }else{
          num += x.length
        }

      }
    mapFa
  }




  def takeGFF ={
    val fileGff=
      FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\" +
        "学习文档\\基因\\茄子基因组数据\\final_qiezi.gff")).asScala

    val mapGff= fileGff.map(_.split("\t")).filter(_(2)=="gene").map(list=>{
      val chr =list.head

      val start=list(3)
      val end=list(4)
      val strand=list(6)
      val idName=list(8).substring(list(8).indexOf("=")+1,list(8).indexOf(";"))
      (idName,chr,start,end,strand)
    })
    mapGff

  }


  def takeKEGG ={
    val fileKegg=FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\" +
      "学习文档\\基因\\茄子基因组数据\\04.function_annotation\\sort_KEGG")).asScala

    println("KEGG ID NUM:"+testIdNum(fileKegg))
    var mapKEGG=mutable.HashMap[String,(String,String)]()

    fileKegg.filter(_.trim!="").foreach{line=>
      val list=line.split('\t')
      val geneid=list.head.split('.').head
      val Knum=list(15).substring(list(15).indexOf("K"),list(15).indexOf(" "))
      val Kdiscription=judge(list(15))
      mapKEGG+=(geneid->(Knum,Kdiscription))
    }
    mapKEGG
  }


  def takeNR ={
    val fileNR=FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\" +
      "学习文档\\基因\\茄子基因组数据\\04.function_annotation\\sort_NR")).asScala

    println("NR ID NUM:"+testIdNum(fileNR))

    var mapNR=mutable.HashMap[String,String]()
    fileNR.filter(_.trim!="").foreach{line=>
      val list=line.split('\t')
      val geneid=list.head.split('.').head
      val Ndiscription=judge(list(13))
      mapNR+=(geneid->Ndiscription)
    }
    mapNR

  }

  def takeSwissport ={
    val fileSwiss=FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\学习文档\\基因\\" +
      "茄子基因组数据\\04.function_annotation\\sort_Swissprot")).asScala

    println("Swissport ID NUM:"+testIdNum(fileSwiss))

    var mapSwiss=mutable.HashMap[String,String]()
    fileSwiss.filter(_.trim!="").map{line=>
      val list=line.split('\t')
      val geneid=list.head.split('.').head
      val Sdiscription=judge(list(15))
      mapSwiss+=(geneid->Sdiscription)
    }
    mapSwiss
  }

  def takePfam ={
    val filePfam=FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\学习文档\\" +
      "基因\\茄子基因组数据\\04.function_annotation\\sort_stat.iprscan.pfamlist")).asScala

    println("Pfam ID NUM:"+testIdNum(filePfam))

    val lists=filePfam.filter(_.trim!="").map{line=>
      val list=line.split('\t')
      val geneid=list.head.split('.').head
      val pfam_num=list(5).split('(').head
      val pfam_des=list(5)
      (geneid,pfam_num,pfam_des)
    }

    val pmap=lists.distinct.groupBy(_._1).map{x=>
      val pf = x._2.map(_._2).mkString(";")
      val pf_d = x._2.map(_._3).mkString(";;")
      println(x._1 -> (pf,pf_d))
      x._1 -> (pf,pf_d)
    }
    pmap
  }



  def takeWego = {
    val fileWego = FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\" +
      "学习文档\\基因\\茄子基因组数据\\04.function_annotation\\sort_stat.iprscan.wego")).asScala

    println("Wego ID NUM:" + testIdNum(fileWego))

    //var mapWego = mutable.HashMap[String, List[String]]()

    val wegolist = fileWego.filter(_.trim != "").map { line =>
      val list = line.split('\t')
      val geneid = list.head.split('.').head
      val len = list.toList.drop(1).length
      val li = List(geneid).padTo(len, geneid)

      li zip list.toList.drop(1)
    }

    wegolist.flatten

  }


  def takeGolist={
    val fileGolist=FileUtils.readLines(new File("C:\\Users\\yingf\\Desktop\\" +
      "学习文档\\基因\\茄子基因组数据\\04.function_annotation\\sort_stat.iprscan.golist")).asScala

    println("Golist ID NUM:"+testIdNum(fileGolist))
    var mapGolist=mutable.HashMap[String,String]()

    fileGolist.filter(_.trim!="").foreach{line=>
      val list=line.split('\t')
      val geneid=list.head.split('.').head
      val discription=list(2)
      mapGolist+=(geneid->discription)
    }
    mapGolist

  }

  def joinGo(wegolist:mutable.Buffer[(String,String)],golist:mutable.HashMap[String,String])={

    val joinedGolist=wegolist.map{id_go=>
      val geneid=id_go._1
      val goid=id_go._2
      var discription=""
      if(golist(geneid).indexOf(goid)!=(-1))
        discription=golist(geneid).substring(0,golist(geneid).indexOf(goid)-1)
      while(discription.indexOf("GO:")!=(-1))
        discription=discription.substring(discription.indexOf("GO:")+12)
      discription=goid+" "+discription
      (geneid,goid,discription)
    }


    val go=joinedGolist.distinct.groupBy(_._1).map{x=>
      val go=x._2.map(_._2).mkString(";")
      val go_des=x._2.map(_._3).mkString(";;")
      x._1->(go,go_des)
    }
    go
  }


  def judge(x:String) ={
    if(x.indexOf('\"')!=(-1))
      x.substring(1,x.length-1)
    else
      x
  }

  def testIdNum(fileName:mutable.Buffer[String])={
    //检查id数量是否有重复
    val idlist = fileName.filter(_.trim != "").map{line=>
      val list=line.split('\t')
      list.head.split('.').head
    }
    val idlistDis=idlist.distinct
    if(idlist.size==idlistDis.size)
      true
    else
      false
  }

}

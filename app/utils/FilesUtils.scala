package utils

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.json.Json
import slick.ast.JoinType.Zip

import scala.collection.mutable
import scala.io.Source
import scala.reflect.io.ZipArchive
//允许java代码
import scala.collection.JavaConverters._

object FilesUtils {

  def main(args: Array[String]): Unit = {

    val file = FileUtils.readLines(new File("F:\\CloudPlatform\\files\\examples\\VennChart.txt")).asScala
    val buffer = file.filter(_!="")
    val file2 = FileUtils.readLines(new File("F:\\CloudPlatform\\files\\examples\\VennChartGroup.txt")).asScala
    val head = file2.filter(_!="")

    val out=buffer.map{x=>
      x.split("\t").last
    }.tail.mkString(":")

    println(out)

    (1 to 5).foreach{x=>
      println(x)
    }




//    val sample=buffer(0).split("\t").tail
//    val char = Array("A", "B", "C", "D", "E", "F")
//
//    val result=buffer.tail.map{x=>
//      val body=x.trim.split("\t").tail
//      val gene=x.trim.split("\t")(0).trim
//      val key=head.map{y=>
//        val test = y.trim.split(",").map{z=>
//          val index=sample.indexOf(z.trim)
//          if(body(index)!="0") z.trim else ""
//        }
//        if(!test.contains("")) char(head.indexOf(y)) else ""
//      }.mkString
//      (key,gene)
//    }.filter(r=>r._1!="")
//
//    val data = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
//    val name = head.zipWithIndex.map(x => char(x._2) -> x._1).toMap
//    val values = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).length)
//
//    println(data.toBuffer)
//    println(name.toBuffer)
//    println(values.toBuffer)


//    val buffer = FileUtils.readLines(new File("F:\\CloudPlatform\\files\\examples\\venn.txt")).asScala
//
//    val matrix = buffer.map(_.split("[\t|;|,]"))
//
//    val head = matrix.head //head=sample
//    val char = Array("A", "B", "C", "D", "E", "F")
//
//    val body = (0 to matrix.head.length-1).map { x =>
//      matrix.map { y =>
//        if (x<=y.length) y(x) else ""
//      }.distinct.tail.filterNot(_=="") //init 去掉数组最后一个元素,tail去掉第一个元素
//    }.toBuffer
//    //body=array(每一列数据为一行array,,,)
//    //      println(body.toList)
//
//    val result = body.flatMap { x =>
//      val test=x.map { y =>
//        val key = body.filter(_.contains(y)).map(z => char(body.indexOf(z))).mkString
//        (key, y)
//      }.distinct
//      test
//    }.distinct
//
//    println(result)
//
//    //result=>array((headers,gene),(),(),())  headers 是该元素的sample对应ABCD
//    //      println(result.toList)
//
//    val data = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
//    val name = head.zipWithIndex.map(x => char(x._2) -> x._1).toMap
//    val values = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).length)
//
//    println(data.toBuffer)
//    println(name.toBuffer)
//    println(values.toBuffer)

//    val tableFile1=new File("F:\\CloudPlatform\\users\\6\\IGC8510520\\table1.txt")
//    val g = FileUtils.readLines(tableFile1).asScala.head.trim.split("\t")
//    val e = g
//
//    val list=e.drop(1)++g.drop(1).distinct
//
//
//    val result=FileUtils.readLines(new File("F:\\CloudPlatform\\users\\6\\IGC8510520\\out\\pandv.xls")).asScala
//    var eid=0;
//
//    var soutar:List[List[String]]=List(List(""))
//    var resultFilter = Array("")
//    result.drop(1).foreach{x=>
//      val ei = x.split("\"").filter(_.trim!="")
//      val source=ei(1)
//      val target=ei(2)
//      if(!soutar.contains(List(source,target)) || !soutar.contains(List(target,source))) {
//        soutar=soutar:+List(source,target):+List(target,source)
//        resultFilter=resultFilter:+x
//      }
//    }
//
//    println(resultFilter.drop(1).toList)


//    println(edges)





//    val buffer=FileUtils.readLines(new File("F:\\CloudPlatform\\files\\examples\\VennChart.txt")).asScala
//    val sample=buffer(0).split("\t")
//    val gene=buffer.map{x=>
//      x.split("\t")(0)
//    }.drop(1)
////    println(gene.toList)
////    println(sample.toList)
//    val head=FileUtils.readLines(new File("F:\\CloudPlatform\\files\\examples\\VennChartGroup.txt")).asScala
//    println(head.toList)
//
//    val char = Array("A", "B", "C", "D", "E", "F")
//
//    val result=buffer.map{x=>
//      val body=x.split("\t")
//      val gene=x.split("\t")(0)
//      val key=head.map{y=>
//        val test = y.split(",").map{z=>
//          val index=sample.indexOf(z)
//          println("z="+z)
//          println("index="+index)
//          println("body="+body(index))
//          if(body(index)!="0") z else ""
//        }
//        println(test.toList)
//        println()
//        if(!test.contains("")) char(head.indexOf(y)) else ""
//      }.mkString
//      (key,gene)
//    }.filter(r=>r._1!="")
//
//    val data = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
//    val name = head.zipWithIndex.map(x => char(x._2) -> x._1).toMap
//    val values = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).length)
//
//    println(result)

//    val test=head.map(_.split(",").map(sample.indexOf(_))).filter(!_.contains(-1))
//
//    println(test)
//
//    test.foreach(x=>println(x.toList))

//    head.map(_.split(",").map(sample.indexOf(_)))

//    buffer.map(_.split("[\t|;|,]").length).distinct

//        val body=head.map{x=>
//          println(x)
//          val col=x.split("&").map{y=>
//            val index=sample.indexOf(y) //sample下标
//            val line=buffer.drop(1).map{z=>
//              val data=z.split("\t")(index+1)
//              val name=z.split("\t")(0)
//              if(data !="0" ) name else ""
//            }
//            line.filter(_!="")
//          }
//          println()
//          println((x+:col.flatten).toList)
//          println()
//          x+:col.flatten
//        }
//
//        println(body.toList)

//    val result=gene.map{g=>
//      val key=body.filter(_.contains(g)).map(z=> char(body.indexOf(z))).mkString
//      (key,g)
//    }
//
//    println(result)






//    val body=head.map{x=>
//      println(x)
//      val col=x.split("&").map{y=>
//        val index=sample.indexOf(y) //sample下标
//        val line=buffer.drop(1).map{z=>
//          val data=z.split("\t")(index+1)
//          val name=z.split("\t")(0)
//          if(data !="0" ) name else ""
//        }
//        line.filter(_!="")
//      }
//      println()
//      println((x+:col.flatten).toList)
//      println()
//      x+:col.flatten
//    }
//
//    println(body.toList)




    //    val e=evi.map{line=>
    //      line.trim.split("\t").head
    //    }


//    val(valid,code,responsecode)=Utils.sendMessage("12345678911")
//    println(valid)
//    println(code)
//    println(responsecode)
//    val verify: mutable.HashMap[String, String] = mutable.HashMap()
//    System.out.println(verify.get("13774463227"))


//    val files = new File("F:\\CloudPlatform\\users\\1\\PCA624165229\\out").listFiles().map(_.getAbsolutePath)
//    val name=new File("F:\\CloudPlatform\\users\\1\\PCA624165229\\out").listFiles().map(_.getName)
//    println(files.toList)
//    println(name.toList)

//    val genus=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\data\\net_genus.txt")).asScala
//    val g=genus.map{line=>
//      line.trim.split("\t").head
//    }
//    println(g.length)
//
//    val evi=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\data\\net_env.txt")).asScala
//    val e=evi.map{line=>
//      line.trim.split("\t").head
//    }
//
//    val list=e.drop(1)++g.drop(1)
//    var count=0;
//    val nodes=list.map{x=>
//      count=count+1
//      val id=list.indexOf(x).toString
//      val xy=Json.obj("x"->Math.random()*500,"y"->Math.random()*500)
//      val (group,score)=
//        if(count<=e.drop(1).length) ("evi",0.006769776522008331) //环境node
//        else ("gene",0.0022841757103715943) //基因node
//      val data=Json.obj("id"->id,"name"->x,"score"->score,"group"->group)
//      Json.obj("data"->data,"position"->xy,"group"->"nodes")
//    }
//
//    val result=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\test\\result.xls")).asScala
//    val edges=result.drop(1).map{x=>
//      val e = x.split("\"").filter(_.trim!="")
//      val source=list.indexOf(e(1))
//      val target=list.indexOf(e(2))
//      val weight=e(3).trim.split("\t").last.toDouble
//      val data=Json.obj("source"->source,"target"->target,"weight"->weight)
//      Json.obj("data"->data,"group"->"edges")
//    }
//
//    val row=nodes++edges
//
//
//
//    println(Json.obj("rows"->row).toString())


//    val dutyDir="F:/CloudPlatform/users/1/gokegg"

//    Utils.pdf2Png(dutyDir+"/out/gokegg.Go.enrich.pdf",dutyDir+"/temp/gokegg.Go.enrich.png")
//    Utils.pdf2Png(dutyDir+"/out/gokegg.Ko.enrich.pdf",dutyDir+"/temp/gokegg.Ko.enrich.png")
//    Utils.pdf2Png(dutyDir+"/out/ko_stack.pdf",dutyDir+"/temp/ko_stack.png")
//    Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/temp/ko_dodge.png")
//    Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/temp/go_stack.png")
//    Utils.pdf2Png(dutyDir+"/out/go_dodge.pdf",dutyDir+"/temp/go_dodge.png")

//    new File("F:\\CloudPlatform\\R\\pca\\test\\test_out\\table.zip").createNewFile()
//    CompressUtil.zip("F:\\CloudPlatform\\R\\pca\\test\\test_out\\temp","F:\\CloudPlatform\\R\\pca\\test\\test_out\\table.zip")

//    Utils.pdf2Png("F:\\CloudPlatform\\R\\pca\\test\\test_out\\pca.pdf","F:\\CloudPlatform\\R\\pca\\test\\test_out\\pca.png")

//    val t=" -b A,C,D -c tt"
//    System.out.println(t.substring(4,t.indexOf("-c")))

//    val genus=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\data\\genus.txt")).asScala
//    val g=genus.map{line=>
//      line.trim.split("\t").head
//    }
//
//    val evi=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\data\\env.txt")).asScala
//    val e=evi.map{line=>
//      line.trim.split("\t").head
//    }
//
//    val list=e.drop(1)++g.drop(1)
//    var count=0;
//    val nodes=list.map{x=>
//      count=count+1
//      val evi= if(count<=e.drop(1).length) "Environment" else "GeneId"
//      Json.obj("name"->x,"value"->1,"category"->evi)
//    }
//
//    val data=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\test\\result.xls")).asScala
//    val links=data.map{x=>
//      val e = x.split("\"").filter(_.trim!="")
//      val source=list.indexOf(e(1))
//      val target=list.indexOf(e(2))
//      Json.obj("source"->source,"target"->target)
//    }
//
//    val cat=(Json.obj("name"->"GeneId","base"->"GeneId"),Json.obj("name"->"Environment","base"->"Environment"))
//
//    val rows=Json.obj("type"->"force","categories"->cat,"nodes"->nodes,"links"->links)
//    println(rows)

//    val ge=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\data\\cytoscape-TF.txt")).asScala
//    val source=ge.map{line=>
//      line.trim.split("\t").filter(_.trim!="").head.trim
//    }.drop(1).distinct
//    val c=ge.map{line=>
//      line.trim.split("\t").filter(_.trim!="")(1).trim
//    }.drop(1).distinct
//    val target=ge.map{line=>
//      line.trim.split("\t").filter(_.trim!="").last
//    }.drop(1).distinct
//    val list2=(source++target).distinct
//
//    println(c)



//    val head=FileUtils.readFileToString(new File("F:\\CloudPlatform\\R\\heatmap\\cluster.txt")).trim.split("\n")
//    println("row="+(head.length-1))
//    println("col="+(head(1).split("\t").length-1))
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

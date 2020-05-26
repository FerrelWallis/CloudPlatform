package utils

import java.io.File

import org.apache.commons.io.FileUtils

object Utils {

  val windowsPath="F:/CloudPlatform/"
  val linuxPath="/root/CloudPlatform/"

  val path : String= {
    if (new File(windowsPath).exists()) windowsPath else linuxPath
  }

  val softwares=(("1","GO富集分析","应用超几何检验，找出与整个基因组背景相比，在差异表达基因中显著富集的GO条目","gogsea.png"),("2","KEGG富集分析","应用超几何检验，找出与整个基因组背景相比，在差异表达基因中显著性富集的Pathway","pathwaygsea.png"),("3","主成分分析（PCA）","将多个变量通过线性变换，筛选出数个比较重要的变量","pca.png"),("4","CCA/RDA","计算并展示环境因子、微生物群落和样本关系","cca.png"),("5","Heatmap 热图","将表格数据绘制成一个热图","heatmap.png"),("6","Boxplot 盒型图","将表格数据绘制成一个盒形图","box.png"),("7","权重网络图","一款图形化显示权重网络并进行分析和编辑的软件","cytoscape.png"),("8","有向网络图","一款图形化显示有向网络并进行分析和编辑的软件","cytoscape.png"))

  val funcAnalyst=(("1","GO富集分析","应用超几何检验，找出与整个基因组背景相比，在差异表达基因中显著富集的GO条目","gogsea.png"),("2","KEGG富集分析","应用超几何检验，找出与整个基因组背景相比，在差异表达基因中显著性富集的Pathway","pathwaygsea.png"))

  val ClusterAnalyst=(("3","主成分分析（PCA）","将多个变量通过线性变换，筛选出数个比较重要的变量","pca.png"),("4","CCA/RDA","计算并展示环境因子、微生物群落和样本关系","cca.png"))

  val diffAnalyst=(("5","Heatmap 热图","将表格数据绘制成一个热图","heatmap.png"),("6","Boxplot 盒型图","将表格数据绘制成一个盒形图","box.png"),("7","权重网络图","一款图形化显示权重网络并进行分析和编辑的软件","cytoscape.png"),("8","有向网络图","一款图形化显示有向网络并进行分析和编辑的软件","cytoscape.png"))

  val sequeAnalyst=()

  val nromalDraw=()

  val chartTrans=()


  val suffix: String = {
    if (new File(windowsPath).exists()) ".exe" else " "

  }

  def deleteDirectory(direcotry: String) = {
    try {
      FileUtils.deleteDirectory(new File(direcotry))
    } catch {
      case _: Throwable =>
    }
  }

  def deleteDirectory(direcotry: File) = {
    try {
      FileUtils.deleteDirectory(direcotry)
    } catch {
      case _: Throwable =>
    }
  }

  val scriptHtml =
    """
      |<script>
      |  $(function () {
      |        $("footer:first").remove()
      |        $("#content").css("margin","0")
      |       $(".linkheader>a").each(function () {
      |        var text=$(this).text()
      |        $(this).replaceWith("<span style='color: #222222;'>"+text+"</span>")
      |       })
      |
      |      $("tr").each(function () {
      |         var a=$(this).find("td>a:last")
      |      var text=a.text()
      |      a.replaceWith("<span style='color: #222222;'>"+text+"</span>")
      |     })
      |
      |       $("p.titleinfo>a").each(function () {
      |        var text=$(this).text()
      |        $(this).replaceWith("<span style='color: #606060;'>"+text+"</span>")
      |       })
      |
      |       $(".param:eq(1)").parent().hide()
      |       $(".linkheader").hide()
      |
      |    })
      |</script>
  """.stripMargin



}

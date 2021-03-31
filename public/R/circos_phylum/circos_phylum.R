pacman::p_load(circlize, grid, reshape2, ComplexHeatmap, dplyr, lazyopt, magrittr, tibble, randomcoloR, stringr)

arg <- c("-i", "F:/CloudPlatform/R/circos_species/test/phylum.xls",
         "-g", "F:/CloudPlatform/R/circos_species/test/map.txt",
         "-o", "F:/CloudPlatform/R/circos_species/test",
         "-df", "TRUE",
         "-ds", "TRUE",
         "-dl", "TRUE",
         "-flp", "20 mm@60@0.6@outside",
         "-slp", "0.3",
         "-tlp", "0.4",
         "-th", "0.05:0.05:0.03",
         "-spos", "90",
         "-gs", "2",
         "-ot", "10000:NULL",
         "-is", "12:15"
         )

opt <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                "grouppath", "g", 2, "character", "The path to the group data read", "",
                "outputfilepath", "o", 2, "character", "The package path of the output image", "",
                "drawFirst", "df", 1, "logical", "whether draw first track", "TRUE",
                "drawSecond", "ds", 1, "logical", "whether draw second track", "TRUE",
                "drawLine", "dl", 1, "logical", "whether draw innerline", "TRUE",
                "firstLabelParam", "flp", 1, "character", "OTU Label params in first track. vjust only for outside labels(mm/cm/inches). label style(auto/inside/outside), eg: vjust@maxlength@fontsize@style", "10 mm@60@0.4@auto",
                "SecondLabelParam", "slp", 1, "character", "the font size of the second track labels ", "0.2",
                "ThirdLabelParam", "tlp", 1, "character", "Label params in first track. whether draw otu name @ labels font size , eg: fontsize", "0.4",
                "trackHeights", "th", 1, "character", "first track height:second:third", "0.05:0.05:0.03",
                "startdegree", "spos", 1 , "numeric", "start degree", "90",
                "gapsize", "gs", 1, "character", "set one number(every gap is the same) or set a sequence of number splited by ',' ", "1",
                "otuthreshold", "ot", 1, "character", "min:max", "0:NULL",
                "otucolor", "oc", 1, "character", "Set the color to ':' split", "",
                "groupcolor", "gc", 1, "character", "Set the color to ':' split", "",
                "imageSize", "is", 1, "character", "The height and width of the picture", "10:12",
                "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
                "imageName", "in", 1, "character", "picture name", "phylum_circos",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt(arg)


otu_table_file <- opt %$% read.delim(tablepath, sep = '\t', stringsAsFactors = F, header = T, check.names = F)

####预处理
all_otu <- otu_table_file[,1]

#依据 group_file 的内容，获取“样本/分组”排序
group <- opt %$% read.delim(grouppath, sep = '\t', stringsAsFactors = F, header = T, check.names = F)
#这里样本根据分组排个序，以防有混写的情况导致作图位置对不上
group <- group[order(group[,2]),]
all_group <- unique(group[,2])
group[,2] <- factor(group[,2], levels = all_group)
all_sample <- group[,1]

#基于上述排序结果，预处理 otu_table_file
otu_table <- otu_table_file[,c("OTU ID",group[,1])] #使用样本排过序的table
rownames(otu_table) <- otu_table[,1]
otu_table <- otu_table[all_sample]

##生成绘图文件
#circlize 外圈属性数据
all_ID <- c(all_otu, all_sample)
accum_otu <- rowSums(otu_table)
accum_sample <- colSums(otu_table)
all_ID_xlim <- cbind(rep(0, length(all_ID)),data.frame(c(accum_otu, accum_sample)))

#检查是否有otu为0的
otuthreshold <- fenge(opt$otuthreshold)
otu <- data.frame(accum_otu)
min <- otu %>% filter(accum_otu <= as.numeric(otuthreshold[1])) %>% rownames()
max <- if(otuthreshold[2] == "NULL") c() else filter(otu, accum_otu >= as.numeric(otuthreshold[2])) %>% rownames()
zerolist <- c(min,max)

#重新整合前面数据，删除zerolist
taxonomy <- otu_table_file[,1] %>% data.frame()
rownames(taxonomy) <- taxonomy[,1]
colnames(taxonomy)<-c("OTU_ID")
taxonomy["phylum"] <- taxonomy["OTU_ID"]
taxonomy <- taxonomy[!rownames(taxonomy) %in% zerolist,]
tax_phylum <- otu_table_file[,1]
tax_phylum <- tax_phylum[!tax_phylum %in% zerolist]
taxonomy[,1] <- factor(taxonomy[,1], levels = tax_phylum)
taxonomy$phylum <- factor(taxonomy$phylum, levels = tax_phylum)

all_otu <- tax_phylum
all_ID <- all_ID[!all_ID %in% zerolist]
all_ID_xlim <- all_ID_xlim[!rownames(all_ID_xlim) %in% zerolist,]
accum_otu <- accum_otu[!names(accum_otu) %in% zerolist]
accum_sample <- accum_sample[!names(accum_sample) %in% zerolist]
otu_table <- otu_table[!rownames(otu_table) %in% zerolist,]

#circlize 内圈连线数据
otu_table$otu_ID <- all_otu
plot_data <- melt(otu_table, id = 'otu_ID') #此处使用了reshape2包中的melt()命令
colnames(plot_data)[2] <- 'sample_ID'
plot_data$otu_ID <- factor(plot_data$otu_ID, levels = all_otu)
plot_data$sample_ID <- factor(plot_data$sample_ID, levels = all_sample)
plot_data <- plot_data[order(plot_data$otu_ID, plot_data$sample_ID), ]
# plot_data <- filter(plot_data, value != 0)
plot_data <- plot_data[c(2, 1, 3, 3)]

#颜色设置  客户只设置otu颜色和group颜色，样本颜色由group颜色梯度产生
color_otu <- opt %$% 
  if(otucolor == "") distinctColorPalette(length(all_otu)) else c(fenge(otucolor),distinctColorPalette(length(all_otu))) 
color_otu <- color_otu[0:length(all_otu)]
names(color_otu) <- all_otu
color_phylum <- color_otu

color_group <- opt %$% if(groupcolor == "") distinctColorPalette(length(all_group)) else fenge(groupcolor)
names(color_group) <- all_group
color_sample <- c()
for(i in 1:length(all_group)) {
  temp <- filter(group, Group == all_group[i])
  num <- length(temp[,1])
  color <- color_group[i]
  color_sample <- c(color_sample,colorRampPalette(c(color,"#ffffff"))(num+1)[-(num+1)])
}
names(color_sample) <- all_sample



####circlize 绘图
firsttrack <- function(opt,taxonomy,tax_phylum,color_phylum,group,all_group,color_group) {
  ##绘制 OTU 分类、样本分组区块（第一圈）
  height <- as.numeric(fenge(opt$trackHeights)[1])
  
  circos.trackPlotRegion(
    ylim = c(0, 1), track.height = height, bg.border = NA, 
    panel.fun = function(x, y) {
      sector.index = get.cell.meta.data('sector.index')
      xlim = get.cell.meta.data('xlim')
      ylim = get.cell.meta.data('ylim')
    })
  
  firstLabelParam <- fenge(opt$firstLabelParam,"@")
  cex <- as.numeric(firstLabelParam[3])
  vjust <- firstLabelParam[1]
  maxlen <- as.numeric(firstLabelParam[2])
  style <- firstLabelParam[4]
  
  for (i in 1:length(tax_phylum)) {
    tax_OTU <- {subset(taxonomy, phylum == tax_phylum[i])}[,1]
    text <- if(nchar(tax_phylum[i]) > maxlen) paste0(substr(tax_phylum[i], 1, maxlen),"...") else tax_phylum[i]
    width <- get.cell.meta.data("cell.width", sector.index = tax_OTU, track.index = 1)
    if(style == "auto") {
      if (width < cex*nchar(as.character(text))) {
        highlight.sector(tax_OTU, track.index = 1, col = color_phylum[i], text = text, cex = cex, text.col = 'black', text.vjust = vjust, facing=c("clockwise"), niceFacing = TRUE)
      }else {
        highlight.sector(tax_OTU, track.index = 1, col = color_phylum[i], text = text, cex = cex, text.col = 'black', niceFacing = TRUE)
      }
    } else if (style == "inside") {
      highlight.sector(tax_OTU, track.index = 1, col = color_phylum[i], text = text, cex = cex, text.col = 'black', niceFacing = TRUE)
    } else {
      highlight.sector(tax_OTU, track.index = 1, col = color_phylum[i], text = text, cex = cex, text.col = 'black', text.vjust = vjust, facing=c("clockwise"), niceFacing = TRUE)
    }
  }
  
  for (i in 1:length(all_group)) {
    group_sample <- {subset(group, Group == all_group[i])}[,1]
    text <- if(nchar(all_group[i]) > maxlen) paste0(substr(all_group[i], 1, maxlen),"...") else all_group[i]
    width <- get.cell.meta.data("cell.width", sector.index = group_sample[1], track.index = 1) * length(group_sample)
    if(style == "auto") {
      if (width < cex*nchar(as.character(text))) {
        highlight.sector(group_sample, track.index = 1, col = color_group[i], text = text, cex = cex, text.col = 'black', text.vjust = vjust, facing=c("clockwise"), niceFacing = TRUE)
      }else {
        highlight.sector(group_sample, track.index = 1, col = color_group[i], text = text, cex = cex, text.col = 'black', niceFacing = TRUE)
      }
    } else if (style == "inside") {
      highlight.sector(group_sample, track.index = 1, col = color_group[i], text = text, cex = cex, text.col = 'black', niceFacing = TRUE)
    } else {
      highlight.sector(group_sample, track.index = 1, col = color_group[i], text = text, cex = cex, text.col = 'black', text.vjust = vjust, facing=c("clockwise"), niceFacing = TRUE)
    }
    
    # highlight.sector(group_sample, track.index = 1, col = color_group[i], text = text, cex = cex, text.col = 'black', niceFacing = TRUE)
    
  }
}

secondtrack <- function(cex) {
  #添加百分比注释（第二圈）
  height <- as.numeric(fenge(opt$trackHeights)[2])
  
  circos.trackPlotRegion(
    ylim = c(0, 1), track.height = height, bg.border = NA,
    panel.fun = function(x, y) {
      sector.index = get.cell.meta.data('sector.index')
      xlim = get.cell.meta.data('xlim')
      ylim = get.cell.meta.data('ylim')
    } )
  
  circos.track(
    track.index = 2, bg.border = NA,
    panel.fun = function(x, y) {
      xlim = get.cell.meta.data('xlim')
      ylim = get.cell.meta.data('ylim')
      sector.name = get.cell.meta.data('sector.index')
      xplot = get.cell.meta.data('xplot')
      
      by = ifelse(abs(xplot[2] - xplot[1]) > 30, 0.25, 1)
      for (p in c(0, seq(by, 1, by = by))) circos.text(p*(xlim[2] - xlim[1]) + xlim[1], mean(ylim) + 0.4, paste0(p*100, '%'), cex = cex, adj = c(0.5, 0), niceFacing = FALSE)
      
      circos.lines(xlim, c(mean(ylim), mean(ylim)), lty = 3)
    } )
}

thirdForth <- function(opt, color_otu, color_sample, all_otu) {
  #绘制 OTU、样本主区块（第三圈）
  maxlen <- as.numeric(fenge(opt$firstLabelParam,"@")[2])
  cex <- as.numeric(opt$ThirdLabelParam)
  height <- as.numeric(fenge(opt$trackHeights)[3])
  
  firstLabelParam <- fenge(opt$firstLabelParam,"@")
  maxlen <- as.numeric(firstLabelParam[2])
  
  circos.trackPlotRegion(
    ylim = c(0, 1), track.height = height, bg.col = c(color_otu, color_sample), bg.border = NA, track.margin = c(0, 0.01),
    panel.fun = function(x, y) {
      xlim = get.cell.meta.data('xlim')
      sector.name = get.cell.meta.data('sector.index')
      
      text <- if(nchar(sector.name) > maxlen) paste0(substr(sector.name, 1, maxlen),"...") else sector.name
      
      circos.axis(h = 'top', labels.cex = cex, major.tick.length = 0.4, labels.niceFacing = FALSE)
      if(!sector.name %in% all_otu) circos.text(mean(xlim), 0.2, text, cex = cex, niceFacing = TRUE, adj = c(0.5, 0)) 
    })
  
  #绘制 OTU、样本副区块（第四圈）
  circos.trackPlotRegion(ylim = c(0, 1), track.height = 0.03, track.margin = c(0, 0.01))
}

innerline <- function(plot_data,accum_otu,accum_sample,color_otu,color_sample) {
  #绘制 OTU-样本关联连线（最内圈）
  for (i in seq_len(nrow(plot_data))) {
    circos.link(
      plot_data[i,2], c(accum_otu[plot_data[i,2]], accum_otu[plot_data[i,2]] - plot_data[i,4]),
      plot_data[i,1], c(accum_sample[plot_data[i,1]], accum_sample[plot_data[i,1]] - plot_data[i,3]),
      col = paste0(color_otu[plot_data[i,2]], '70'), border = NA )
    
    circos.rect(accum_otu[plot_data[i,2]], 0, accum_otu[plot_data[i,2]] - plot_data[i,4], 1, sector.index = plot_data[i,2], col = color_sample[plot_data[i,1]], border = NA)
    circos.rect(accum_sample[plot_data[i,1]], 0, accum_sample[plot_data[i,1]] - plot_data[i,3], 1, sector.index = plot_data[i,1], col = color_otu[plot_data[i,2]], border = NA)
    
    accum_otu[plot_data[i,2]] = accum_otu[plot_data[i,2]] - plot_data[i,4]
    accum_sample[plot_data[i,1]] = accum_sample[plot_data[i,1]] - plot_data[i,3]
  }
}

drawlegend <- function(taxonomy, all_otu, color_otu, fontsize) {
  ##添加图例
  otu_legend <- Legend(
    at = all_otu, labels = taxonomy$phylum, labels_gp = gpar(fontsize = fontsize),
    grid_height = unit(0.3, 'cm'), grid_width = unit(0.3, 'cm'), type = 'points', pch = NA, background = color_otu)
  
  pushViewport(viewport(x = 0.9, y = 0.5))
  grid.draw(otu_legend)
  upViewport()
  
}

plotmain <- function(opt, all_otu, all_sample, all_ID, all_ID_xlim, all_group, taxonomy, group, plot_data, tax_phylum, accum_otu, accum_sample, color_phylum, color_group, color_otu, color_sample) {
  
  circle_size = unit(1, 'snpc')
  
  ##整体布局
  gap_size <- if(opt$gapsize == "") c(rep(3, length(all_otu) - 1), 3, rep(3, length(all_sample) - 1), 3) else {
                temp <- fenge(opt$gapsize, ",") %>% as.numeric()
                if(length(temp) == 1) c(rep(temp[1], length(all_otu) - 1), temp[1], rep(temp[1], length(all_sample) - 1), temp[1])
                else temp
              }
  circos.par(cell.padding = c(0, 0, 0, 0), start.degree = as.numeric(opt$startdegree), gap.degree = gap_size)
  circos.initialize(factors = factor(all_ID, levels = all_ID), xlim = all_ID_xlim)
  
  if(opt$drawFirst) firsttrack(opt,taxonomy,tax_phylum,color_phylum,group,all_group,color_group)
  if(opt$drawSecond) secondtrack(as.numeric(opt$SecondLabelParam))
  thirdForth(opt, color_otu, color_sample, all_otu)
  if(opt$drawLine) innerline(plot_data,accum_otu,accum_sample,color_otu,color_sample)
  
  # drawlegend(taxonomy, all_otu, color_otu, opt$legendFontSize)
  
  ##清除 circlize 样式并关闭画板
  circos.clear()
}



imageSize <- fenge(opt$imageSize)
savepicture(plotmain(opt, all_otu, all_sample, all_ID, all_ID_xlim, all_group, taxonomy, group, plot_data, tax_phylum, accum_otu, accum_sample, color_phylum, color_group, color_otu, color_sample), opt$imageformt, paste0(opt$outputfilepath, "/", opt$imageName, ".", opt$imageformt), height = as.numeric(imageSize[1]), width = as.numeric(imageSize[2]))

color <- c(color_otu, color_group) %>% data.frame() %>% rownames_to_column() 
colnames(color) <- c("rowname", "color")
write.table(color, file = paste0(opt$outputfile,"/colors.xls"), sep = "\t", col.names = FALSE, row.names = FALSE)


# 
# ##清除 circlize 样式并关闭画板
# circos.clear()
# dev.off()


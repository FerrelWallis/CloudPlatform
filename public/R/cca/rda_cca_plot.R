suppressMessages(library("lazyopt"))
suppressMessages(library("dplyr"))
args <- c("-pci", "F:/CloudPlatform/R/cca/test/percent.xls", 
          "-sai", "F:/CloudPlatform/R/cca/test/samples.xls", 
          "-spi", "F:/CloudPlatform/R/cca/test/species.xls", 
          "-ei", "F:/CloudPlatform/R/cca/test/envi.xls", 
          "-o", "F:/CloudPlatform/R/cca/test/", 
          "-g", "F:/CloudPlatform/R/cca/test/group.xls"
)
spec <- matrix(c("samplespath", "sai", 2, "character", "Sample data path", "",
                 "speciespath", "spi", 2, "character", "Data of species path", "",
                 "envipath", "ei", 2, "character", "Environmental data path", "",
                 "percentpath", "pci", 2, "character", "Percentage data path", "",
                 "grouppath", "g", 1, "character", "groupFile path", "",
                 "filepath", "o", 2, "character", "package path of image output", "",
                 "xyread", "xyr", 1, "character", "The xy axis data read", "1:2",
                 "origin_point", "op", 1, "character", "set The origin point x y coordinates", "0:0",
                 "showsample_pt", "sspt", 1, "character", "Whether to display sample points and names", "TRUE:TRUE",
                 "samplepoint", "sap", 1, "character", "The pattern of the sample point", "#1E90FF:6",
                 "sampletext", "sat", 1, "character", "The pattern of the sample text", "#1E90FF:7",
                 "groupcolor", "gc", 1, "character", "set group colours", "#336666:#996633:#CCCC33:#336633:#990033:#FFCC99:#333366:#669999:#996600",
                 "showenvi_pl", "sepl", 1, "character", "show the points and lines of the environment", "TRUE:TRUE",
                 "envitext", "ett", 1, "character", "The pattern of the envi text", "#E41A1C:7",
                 "enviline_color", "elc", 1, "character", "color of line envi", "#E41A1C",
                 "showspecies_pt", "sppt", 1, "character", "show the points and text of the species", "TRUE:TRUE",
                 "speciespoint", "spp", 1, "character", "The pattern of the species point", "#FF8C00:6",
                 "speciestext", "spt", 1, "character", "The pattern of the species text", "#FF8C00:7",
                 "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:16",
                 "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:16",
                 "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name", "sans:bold.italic:18",
                 "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name", "sans:bold.italic:18",
                 "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:12: ",
                 "imageSize", "is", 1, "character", "The height and width of the picture", "12:12",
                 "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
                 "legendtext_style", "lts", 1, "character", "Legend text style Font:font type:font size", "sans:bold.italic:15",
                 "legendtitle_style", "lms", 1, "character", "Legend title style Font:font type:font size:name", "sans:bold.italic:19: ",
                 "help", "h", 1, "character", "help document", ""
), byrow = TRUE, ncol = 6)
#opt <- lazyopt::lazyopt(spec, args)
opt <- lazyopt(spec)


#---------------------------------------------------------------------------------------------------------

fenge <- function(str) {
  str <- strsplit(str, ":")[[1]]
  return(str)
}

cbiaoge <- function(biaoge) {
  rownames(biaoge) <- biaoge[, 1]
  biaoge <- biaoge[, -1]
  return(biaoge)
}

origin_point <- as.numeric(fenge(opt$origin_point))
imageSize <- as.numeric(fenge(opt$imageSize))
xyread <- as.numeric(fenge(opt$xyread))
showsample_pt <- as.logical(fenge(opt$showsample_pt))
showenvi_pl <- as.logical(fenge(opt$showenvi_pl))
showspecies_pt <- as.logical(fenge(opt$showspecies_pt))
samplepoint <- fenge(opt$samplepoint)
sampletext <- fenge(opt$sampletext)
envitext <- fenge(opt$envitext)
speciespoint <- fenge(opt$speciespoint)
speciestext <- fenge(opt$speciestext)
main_style <- fenge(opt$main_style)
legendtext_style <- opt$legendtext_style %>% fenge()
legendtitle_style <- opt$legendtitle_style %>% fenge()

xtext_style <- fenge(opt$xtext_style)
ytext_style <- fenge(opt$ytext_style)
xlab_style <- fenge(opt$xlab_style)
ylab_style <- fenge(opt$ylab_style)

aa <- read.delim(opt$percentpath, check.names = FALSE)
RDA1 <- paste(aa[xyread[1], 1], ":", aa[xyread[1], 2])
RDA2 <- paste(aa[xyread[2], 1], ":", aa[xyread[2], 2])
bb <- read.delim(opt$samplespath, check.names = FALSE)
bb <- as.matrix(cbiaoge(bb))
cc <- read.delim(opt$speciespath, check.names = FALSE)
cc <- as.matrix(cbiaoge(cc))
dd <- read.delim(opt$envipath, check.names = FALSE)
dd <- as.matrix(cbiaoge(dd))

#提取并转换“样本”数�?
samples <- data.frame(sample = row.names(bb), RDA1 = bb[, 1], RDA2 = bb[, 2])

#提取并转换“物种”数�?
species <- data.frame(spece = row.names(cc), RDA1 = cc[, 1], RDA2 = cc[, 2])

#提取并转换“环境因子”数�?
envi <- data.frame(en = row.names(dd), RDA1 = dd[, 1], RDA2 = dd[, 2])
ll <- nrow(envi) * 2

#构建环境因子直线坐标
line_x <- 1:ll
line_x[seq(1, ll, 2)] <- origin_point[1]; line_x[seq(2, ll, 2)] <- envi[, 2]

line_y <- 1:ll
line_y[seq(1, ll, 2)] <- origin_point[2]; line_y[seq(2, ll, 2)] <- envi[, 3]

groupnames <- as.character(t(matrix(rep(rownames(envi), 2), ncol = 2)))
line_g <- groupnames
line_data <- data.frame(x = line_x, y = line_y, group = line_g)


resolution <- opt$resolution; if (resolution != 72 &&
  resolution != 96 &&
  resolution != 300 &&
  resolution != 600) { resolution <- 300 }

if (opt$grouppath != "") {
  gd <- read.delim(opt$grouppath, header = T)
  groupline <- rep(NA, nrow(samples))
  key1 <- as.character(samples[, 1])
  key2 <- as.character(gd[, 1])
  ll <- length(key1)
  linedata <- rep(NA, ll)
  for (i in 1:ll) {
    a <- key1[i]
    b <- which(key2 == a)[1]
    if (length(b) == 1) {
      linedata[i] <- as.character(gd[b, 2])
    }
  }
  line_data[which(is.na(linedata))] <- "no group"
  samples[, 4] <- linedata

}else {
  samples[, 4] <- rep(as.character(sampletext[1]), nrow(samples))
}


suppressMessages(library("ggrepel"))
suppressMessages(library("ggplot2"))

result <- ggplot(data = samples, aes(RDA1, RDA2)) +
  theme_bw() +
  theme(panel.grid = element_blank()) +
  labs(x = RDA1, y = RDA2, title = main_style[4]) +
  theme(axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
        axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
        axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
        axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2]),
        title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
        legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
        legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
        plot.title = element_text(hjust = 0.5)
  )
if (showspecies_pt[2]) {
  #显示物种数据的名�?
  result <- result + geom_text_repel(data = species, aes(label = spece, x = RDA1, y = RDA2), color = as.character(speciestext[1]), size = as.numeric(speciestext[2]))
}

#设置原点并划�?
result <- result +
  geom_hline(yintercept = origin_point[2], linetype = 2) +
  geom_vline(xintercept = origin_point[1], linetype = 2)

if (showspecies_pt[1]) {
  #物种数据�?
  result <- result + geom_point(data = species, size = as.numeric(speciespoint[2]), color = as.character(speciespoint[1]), shape = 18)
}
if (showenvi_pl[1]) {
  #显示环境因子数据名字
  result <- result + geom_text(data = envi, aes(label = en), color = as.character(envitext[1]), size = as.numeric(envitext[2]))
}
if (showenvi_pl[2]) {
  #原点指向环境因子的直�?
  line_data$x <- line_data$x*14/15
  line_data$y <- line_data$y*14/15
  result <- result + geom_path(data = line_data, aes(x = x, y = y, group = group), color = opt$enviline_color, arrow = arrow(length = unit(0.5, "cm")))
}


if (opt$grouppath != "") {
  result <- result +
    theme(legend.position = "right") +
    labs(color = "group")
  if (showsample_pt[1]) {
    #显示样本�?
    result <- result + geom_point(aes(color = as.character(samples[, 4])), size = as.numeric(samplepoint[2])) }

  if (showsample_pt[2]) {
    #显示样本名称
    result <- result + geom_text_repel(aes(label = sample, color = as.character(samples[, 4])), show.legend = F, size = as.numeric(sampletext[2]))
  }
  groupcolor <- fenge(opt$groupcolor); groupcolor <- groupcolor[1:length(unique(samples[, 4]))]
  result <- result + scale_color_manual(values = groupcolor)
}else {
  result <- result + theme(legend.position = "none", legend.title = element_blank())
  if (showsample_pt[1]) {
    #显示样本�?
    result <- result + geom_point(color = samples[, 4][1], size = as.numeric(samplepoint[2])) }

  if (showsample_pt[2]) {
    #显示样本名称
    result <- result + geom_text_repel(aes(label = sample), color = samples[, 4][1], size = as.numeric(sampletext[2]))
  }
}
ggsave(paste0(opt$filepath, "/", "rdacca", ".pdf"), width = imageSize[1], height = imageSize[2], dpi = resolution)




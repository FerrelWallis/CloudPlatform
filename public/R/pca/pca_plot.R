rm(list = ls(all = TRUE))
suppressMessages(library("lazyopt"))
suppressMessages(library("car"))
suppressMessages(library("ggplot2"))
suppressMessages(library("ggrepel"))
suppressMessages(library("dplyr"))
suppressMessages(library("ggforce"))
suppressMessages(library("magrittr"))

arg <- c("-i", "F:/CloudPlatform/R/pca/test/pca.x.xls",
         "-g", "F:/CloudPlatform/R/pca/PCA_group.txt",
         "-o", "F:/CloudPlatform/R/pca/test/",
         "-si", "F:/CloudPlatform/R/pca/test/pca.sdev.xls"
)

spec <- matrix(c("tablepath", "i", 2, "character", "this is datatable path", "",
                 "sedvpath", "si", 2, "character", "this is sedv table path", "",
                 "filepath", "o", 1, "character", "this is file packagepath", getwd(),
                 "display_area", "da", 1, "character", "Zoom in on an area,sample x:2,4,only x or y", " ",
                 "pcaxy", "pxy", 1, "character", "The xy axis selected from the columns of the table", "PC1:PC2",
                 "grouppath", "g", 1, "character", "this is group path", "",
                 "lab", "b", 1, "character", "Displays the data names of those groups, split by ','", "",
                 "show_lab", "sl", 1, "logical", "show nogroup lab in no group", "FALSE",
                 "circle", "c", 1, "logical", "It is not recommended to use this parameter; the drawing is usually ugly", "FALSE",
                 "circle_level", "cl", 1, "numeric", "draw elliptical contours at these (normal) probability or confidence levels.", "0.75",
                 "imageSize", "is", 1, "character", "The height and width of the picture", "15:12",
                 "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
                 "imageName", "in", 1, "character", "picture name", "pca",
                 "colorstyle", "cs", 1, "character", "The parameter used to set the color of the column", "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
                 "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
                 "xtext_style", "xts", 1, "character", "X text style  sample:Font:font type:font size", "sans:plain:15",
                 "ytext_style", "yts", 1, "character", "Y text style  sample:Font:font type:font size", "sans:plain:15",
                 "xlab_style", "xls", 1, "character", "X lab style  sample:Font:font type:font size:name", "sans:plain:17",
                 "ylab_style", "yls", 1, "character", "Y lab style  sample:Font:font type:font size:name", "sans:plain:17",
                 "main_style", "ms", 1, "character", "Main style  sample:Font:font type:font size:name", "sans:plain:17: ",
                 "legendtext_style", "lts", 1, "character", "Legend text style  sample: Font:font type:font size", "sans:plain:14",
                 "legendtitle_style", "lms", 1, "character", "Legend title style  sample: Font:font type:font size:name", "sans:bold.italic:15: ",
                 "onecolor", "oc", 1, "character", "no group file color", "#48FF75",
                 "segment.size", "ss", 1, "logical", "wheather to show segment.size", "TRUE",
                 "help", "h", 0, "logical", "help document", "TRUE"
), byrow = TRUE, ncol = 6)
opt <- lazyopt(spec)

fenge <- function(str) {
  str <- strsplit(str, ":")[[1]]
  return(str)
}

fenge2 <- function(str) {
  str <- strsplit(str, ",")[[1]]
  return(str)
}

imageSize <- opt$imageSize; imageSize <- fenge(imageSize)
resolution <- opt$resolution; if (resolution != 72 &&
  resolution != 96 &&
  resolution != 300 &&
  resolution != 600) { resolution <- 300 }
mycol <- strsplit(opt$colorstyle, ":")[[1]]
main_style <- fenge(opt$main_style)

xtext_style <- fenge(opt$xtext_style)

ytext_style <- fenge(opt$ytext_style)

xlab_style <- fenge(opt$xlab_style)

ylab_style <- fenge(opt$ylab_style)

legendtext_style <- fenge(opt$legendtext_style)

legendtitle_style <- fenge(opt$legendtitle_style)

lab <- opt$lab; if (lab == "") { lab <- NULL }

segment.size <- opt$segment.size; if (segment.size == TRUE) {
  segment.size <- 0.5
}else {
  segment.size <- NA
}

findxiab <- function(group) {
  un <- unique(group); l <- length(un)
  xiabiao <- c()
  for (i in 1:l) {
    xiabiao[i] <- max(which(group == un[i]))
  }
  return(xiabiao)
}

changc <- function(xiab, dd) {
  length <- length(xiab)
  z <- 0
  for (i in (1:length)) {
    x <- xiab[i]
    dd <- c(
      dd[(1:(x + z))], dd[(x + z)], dd[(x + z + 1):length(dd)])
    z <- z + 1
  }
  return(dd[1:(length(dd) - 2)])
}

readx <- function(data) {
  rownames(data) <- data[, 1]; data <- data[, -1]
  return(data)
}

sedv <- read.delim(opt$sedvpath, header = FALSE)
data <- read.delim(opt$tablepath)
data <- readx(data)
optxy <- fenge(opt$pcaxy)
ip <- colnames(data)
xid <- which(ip == optxy[1])
yid <- which(ip == optxy[2])
xnumber <- as.character(sedv[xid, 2]); ynumber <- as.character(sedv[yid, 2])
xname <- colnames(data)[xid]; yname <- colnames(data)[yid]
data <- data.frame(names = rownames(data), x = data[, xid], y = data[, yid])


if (opt$grouppath == "") {
  gg <- ggplot(data) +
    theme_bw() +
    geom_point(aes(x = x, y = y), size = 5, color = opt$onecolor) +
    theme(legend.justification = c("right", "top"),
          panel.grid.major = element_blank(),
          panel.grid.minor = element_blank(),
          axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
          axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
          legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
          legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
          title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
          axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
          axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2]),
          plot.title = element_text(hjust = 0.5)
    ) +
    labs(x = paste(xname, xnumber, " "), y = paste(yname, ynumber, " "), title = main_style[4])

  if (opt$show_lab) {
    gg <- gg + geom_text_repel(aes(x = x, y = y, label = data[, 1]), size = 6, force = 20, arrow = arrow(length = unit(0.01, "npc"), type = "open", ends = "last"), point.padding = 0.5, show.legend = F, segment.size = segment.size) }

  if (opt$circle) {
    result <- as.data.frame(dataEllipse(data[, 2], data[, 3], levels = opt$circle_level, draw = FALSE, segments = 51))
    gg <- gg + geom_path(data = result, aes(x = x, y = y), show.legend = FALSE)
  }


}else {
  gdata <- read.delim(opt$grouppath, header = T) %>% set_rownames(.[,1])
  gdata <- gdata[as.vector(data[,1]),]
  groupline <- rep(NA, nrow(data))
  key1 <- as.character(data[, 1])
  key2 <- as.character(gdata[, 1])
  key <- which(data[, 1] == key2)
  keylength <- length(key)
  for (i in 1:keylength) {
    n <- key[i]
    c <- which(gdata[, 1] == as.character(data[n, 1]))[1]
    value <- gdata[c, 2]
    groupline[n] <- as.character(value)
  }
  groupline[which(is.na(groupline))] <- "no group"


  data[, 4] <- groupline; colnames(data)[4] <- "colorshape"
  lab <- opt$lab; if (lab == "") { lab <- NULL }
  labdata <- NA
  if (opt$grouppath != "" & !is.null(lab)) {
    lab <- fenge2(lab)
    kl <- length(lab)
    fuhe <- c()
    for (i in 1:kl) {
      a <- which(data[, 4] == lab[i])
      if (length(a) != 0) {
        a <- a[1:(length(a))]
        fuhe <- c(fuhe, a)
      }
    }

    labdata <- as.character(data[, 1])
    lab <- labdata
    labdata[fuhe] <- NA
    lab[which(!is.na(labdata))] <- NA
    labdata <- lab
  }
  co <- fenge(opt$colorstyle)
  gg <- ggplot(data) +
    theme_bw() +
    geom_point(aes(x = x, y = y, color = data[, 4]
    ), size = 4) +
    theme(legend.justification = c("right", "top"),
          panel.grid.major = element_blank(),
          panel.grid.minor = element_blank(),
          axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
          axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
          legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
          legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
          title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
          axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
          axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2]),
          plot.title = element_text(hjust = 0.5)
    ) +
    labs(x = paste(xname, xnumber, " "), y = paste(yname, ynumber, " "), title = main_style[4]) +
    geom_text_repel(aes(x = x, y = y, label = labdata,
                        color = data[, 4]), size = 6, force = 20
      , arrow = arrow(length = unit(0.01, "npc"), type = "open", ends = "last")
      , point.padding = 0.6, show.legend = F
      , segment.size = segment.size
    ) +
    scale_color_manual(values = co, name = legendtitle_style[4])

  if (opt$circle) {
    data2 <- data[which(data$colorshape != "no group"),]
    uniq <- unique(data2$colorshape)
    uniqll <- length(uniq)
    result <- NULL
    for (i in 1:uniqll) {
      checknums <- which(data2$colorshape == uniq[i])
      linshidata2 <- as.data.frame(data2[checknums, c(2, 3)])
      linshidata2 <- as.data.frame(dataEllipse(linshidata2[, 1], linshidata2[, 2], levels = opt$circle_level, draw = FALSE, segments = 51))
      linshidata2 <- linshidata2 %>% mutate(colorshape = uniq[i])
      result <- rbind(result, linshidata2)
    }
    gg <- gg + geom_path(data = result, aes(x = x, y = y, group = colorshape, color = colorshape), show.legend = FALSE)
  }
}

if (opt$display_area != " ") {
  display_area <- fenge(opt$display_area)
  area <- as.numeric(fenge2(display_area[2]))
  if (display_area[1] == "x") {
    gg <- gg + facet_zoom(xlim = c(area[1], area[2]))
  }
  if (display_area[1] == "y") {
    gg <- gg + facet_zoom(ylim = c(area[1], area[2]))
  }
}
filepath <- filepath <- paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt)
ggsave(filepath, gg, width = as.numeric(imageSize[1]), height = as.numeric(imageSize[2]), dpi = resolution)

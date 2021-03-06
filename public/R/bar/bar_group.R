pacman::p_load(lazyopt, magrittr, reshape, ggplot2, dplyr)
arg <- c("-i","F:/CloudPlatform/files/examples/bar_table.txt",
         "-o","F:/CloudPlatform/R/bar/output",
         "-xls","sans:bold.italic:20:X Title",
         "-yls","sans:bold.italic:20:Y Title",
         "-xta","90"
         )

opt <- matrix(c(
  "group", "g", 1, "character", "file group path", " ",
  "colorstyle", "cs", 1, "character", "Set the color to ':' split", "red:#FFC913:yellow:green:cyan:#297EFF:purple:pink",
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "filepath", "o", 2, "character", "The package path of the output image", "",
  "merge", "m", 1, "logical", "whether to merge datas which otu lower than merge_threshold%", "TRUE",
  "merge_threshold", "mt", 1, "numeric", "the merge_threshold", "1",
  "imageSize", "is", 1, "character", "The height and width of the picture", "20:20",
  "xtext_angle", "xta", 1, "numeric", "X-aixs text angle", "0",
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:18",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:16",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name:angle", "sans:bold.italic:20: :0",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name:angle", "sans:bold.italic:20: :90",
  "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:22: ",
  "legendtext_style", "lts", 1, "character", "Legend text style Font:font type:font size", "sans:bold.italic:15",
  "legendtitle_style", "lms", 1, "character", "Legend title style Font:font type:font size:name", "sans:bold.italic:19: ",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "legend_position", "lp", 1, "character", "legeng position,right,left,bottom,top", "right",
  "legend_nrows", "lrs", 1, "character", "Figure number of routine", "NULL",
  "legend_ncols", "lcs", 1, "character", "Legend for the number of columns", "NULL",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
  "imageName", "in", 1, "character", "picture name", "bar_group",
  "legend_size", "ls", 1, "numeric", "size of legend ", "15",
  "persent", "pe", 1, "logical", "The percentage change", "TRUE",
  "legend_order", "lo", 1, "numeric", "Set legend order, 0 means alphabetical order, 1 means sorted by abundance", "0",
  "bar_width", "bw", 1, "numeric", "bar width 0 - 1", "0.9",
  "x_order", "xo", 1, "character", "x-axis sample order", "",
  "help", "h", 0, "numeric", "Help document", "0.8"
), byrow = TRUE, ncol = 6) %>% lazyopt()

persent <- function(data, p) {
  if (p) {
    persentData <- data
    for (c in seq_len(ncol(data))) {
      for (r in seq_len(nrow(data))) {
        persentData[r, c] <- (data[r, c] / sum(data[, c])) * 100
      }
    }
    return(persentData)
  }
  return(data)
}

groupf <- function(plotData, opt, data) {
  if (opt$group != " ") {
    groupData <- matrix(rep("noGroup", ncol(data) * nrow(data)), ncol = ncol(data))
    rownames(groupData) <- rownames(data)
    colnames(groupData) <- colnames(data)
    group <- read.delim(opt$group, check.names = F, header = TRUE)
    l1 <- colnames(data)
    ll <- nrow(group)
    if (ll > 0) {
      for (i in 1:ll) {
        s <- as.character(group[i, 1])
        nc <- which(l1 == s)
        if (!is.logical(nc)) {
          data[, nc] <- rep(as.character(group[i, 2]), nrow(data))
        }
      }
    }
    data %<>% melt()
    plotData %<>% mutate(group = data[, 3])
    return(plotData)
  }
  return(plotData)
}

abundant <- function(plotData) {
  abuorder <- tapply(plotData$y, plotData$color, sum) %>%
    sort() %>%
    rev() %>%
    names()
  plotData$color <- factor(plotData$color, levels = abuorder)
  return(plotData)
}

data <- opt$tablepath %>%
  read.delim(check.names = F, header = TRUE, row.names = 1) %>%
  as.matrix()
#柱状图每行求和设置百分比参数，低于Other。是否合并低丰度结果 1%
if(opt$merge) {
  tempdata <- apply(data, 1, sum)
  sum <- sum(tempdata)
  
  checkfun <- function(x,sum) {
    return (x/sum * 100)
  }
  
  datapersent <- apply(data.frame(tempdata), 2, function(x) checkfun(x,sum)) %>% data.frame()
  invaliddata <- filter(datapersent, tempdata < opt$merge_threshold) %>% rownames()
  if(!is.null(invaliddata)) {
    othercol <- if(length(invaliddata) > 1) data[rownames(data) %in% invaliddata,] %>% apply(2, function(x) sum(x)) else data[rownames(data) %in% invaliddata,]
    data <- data[!rownames(data) %in% invaliddata,] %>% rbind(Other=othercol)
  }
}

if(opt$x_order != "") {
  xorder <- opt$x_order %>% lazyopt::fenge(",") %>% as.character()
  data <- data[,xorder]
}

plotData <- data %>%
  persent(opt$persent) %>%
  melt() %>%
  set_colnames(c("color", "x", "y")) %>%
  groupf(opt, data)

if(opt$legend_order == 1) plotData %<>% abundant()

if(opt$x_order != "") plotData$x <- factor(plotData$x, levels = opt$x_order %>% lazyopt::fenge(",") %>% as.character()) else plotData$x %<>% as.factor()

legendtitle_style <- opt$legendtitle_style %>% lazyopt::fenge()

main_style <- opt$main_style %>% lazyopt::fenge()

xtext_style <- opt$xtext_style %>% lazyopt::fenge()

ytext_style <- opt$ytext_style %>% lazyopt::fenge()

xlab_style <- opt$xlab_style %>% lazyopt::fenge()

ylab_style <- opt$ylab_style %>% lazyopt::fenge()

legendtext_style <- opt$legendtext_style %>% lazyopt::fenge()

legendtitle_style <- opt$legendtitle_style %>% lazyopt::fenge()

imageSize <- opt$imageSize %>% lazyopt::fenge()

colorstyle <- opt$colorstyle %>% lazyopt::fenge()

resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()

legend_nrows <- opt$legend_nrows; if (legend_nrows == "NULL") { legend_nrows <- NULL }else { legend_nrows <- as.numeric(legend_nrows) }

legend_ncols <- opt$legend_ncols; if (legend_ncols == "NULL") { legend_ncols <- NULL }else { legend_ncols <- as.numeric(legend_ncols) }


pp <- ggplot(plotData) +
  geom_col(aes(x = x, y = y, fill = color), color = "black", width = as.numeric(opt$bar_width)) +
  theme_bw() +
  scale_y_continuous(expand = c(0, 0)) +
  theme(
    axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
    legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
    legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
    legend.position = opt$legend_position,
    title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
    axis.title.x =  element_text(size = as.numeric(xlab_style[3]),family = xlab_style[1],face = xlab_style[2],angle = as.numeric(xlab_style[5]),hjust = 0.5,vjust = 0.5),
    axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], angle = as.numeric(ylab_style[5]),hjust = 0.5,vjust = 0.5),
    strip.text = element_text(size = 20, family = "sans", face = "bold.italic"),
    legend.key.size = unit(opt$legend_size, "mm"),
    panel.grid.major = element_blank(),
    panel.grid.minor = element_blank(),
    plot.title = element_text(hjust = 0.5)
  ) +
  guides(fill = guide_legend(reverse = F, ncol = legend_ncols, byrow = TRUE, nrow = legend_nrows)) +
  labs(title = main_style[4], y = ylab_style[4], x = xlab_style[4], fill = legendtitle_style[4])
pp <- pp + scale_fill_manual(values = colorRampPalette(colorstyle)(pp$data$color %>% unique() %>% length()))
if (opt$group != " ") {
  pp <- pp + facet_grid(. ~ group, scales = "free", space = "free")
}
if (opt$xtext_angle == 0 || opt$xtext_angle == 360) {
  pp <- pp + theme(axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3]), angle = opt$xtext_angle))
} else {
  pp <- pp + theme(axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3]), angle = opt$xtext_angle, hjust = 1))
} 
  
paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt) %>%
  ggsave(pp, width = as.numeric(imageSize[1]), height = as.numeric(imageSize[2]), dpi = resolution, units = "in")



pacman::p_load(lazyopt, car, ggplot2, ggrepel, dplyr, ggforce, magrittr, tibble)

arg <- c("-i", "F:/CloudPlatform/R/scatterpoint/sca_table.txt",
         "-o", "F:/CloudPlatform/R/scatterpoint",
         "-xyd", "2:3",
         "-xl", "log2",
         "-yl", "log2",
         "-vl", "6,9",
         "-hl", "8,12",
         "-dl", "3,1;3,2"
)

opt <- matrix(c("inputpath", "i", 2, "character", "this is datatable path", "",
                "xydata", "xyd", 2, "character", "the column number of XY,eg 2:3", "2:3",
                "filepath", "o", 2, "character", "this is file packagepath", getwd(),
                "xlog", "xl", 1, "character", "preprogress x data, none/log2/log10", "log2",
                "ylog", "yl", 1, "character", "preprogress y data, none/log2/log10", "log2",
                "method", "m", 1, "character", "Operation method:'pearson','kendall','spearman'", "pearson",
                "hline", "hl", 1, "character", "draw multiple the horizontally auxilary lines, input intercepts seperated by ','", "",
                "vline", "vl", 1, "character", "draw multiple the vertically auxilary lines, input intercepts seperated by ','", "",
                "dline", "dl", 1, "character", "draw multiple the diagonals, input 'intercepts,sploe' seperated by ';'", "",
                "drawcor", "dc", 1, "logical", "whether to draw correlation", "TRUE",
                "drawaxis", "dax", 1, "logical", "whether to draw the origin axis", "TRUE",
                "cor_just", "coj", 1, "character", "the hjust and vjust of correlation, hjust:vjust", "1.5:10",
                "cor_color", "coc", 1, "character", "the color of the correlation annotation", "black",
                "cor_size", "cos", 1, "numeric", "the font size of the correlation annotation", "4.5",
                "display_area", "da", 1, "character", "Zoom in on an area,sample x:2,4,only x or y", " ",
                "dot_color", "doc", 1, "character", "no group file color", "red",
                "imageSize", "is", 1, "character", "The width and height of the picture", "10:10",
                "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
                "imageName", "in", 1, "character", "picture name", "scatter",
                "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
                "xtext_style", "xts", 1, "character", "X text style  sample:Font:font type:font size", "sans:plain:15",
                "ytext_style", "yts", 1, "character", "Y text style  sample:Font:font type:font size", "sans:plain:15",
                "xlab_style", "xls", 1, "character", "X lab style  sample:Font:font type:font size:name:angle", "sans:plain:17::0",
                "ylab_style", "yls", 1, "character", "Y lab style  sample:Font:font type:font size:name:angle", "sans:plain:17::90",
                "main_style", "ms", 1, "character", "Main style  sample:Font:font type:font size:name", "sans:plain:17: ",
                "help", "h", 0, "logical", "help document", "TRUE"
), byrow = TRUE, ncol = 6) %>% lazyopt()

imageSize <- lazyopt::fenge(opt$imageSize) %>% as.numeric()

main_style <- lazyopt::fenge(opt$main_style)

xtext_style <- lazyopt::fenge(opt$xtext_style)

ytext_style <- lazyopt::fenge(opt$ytext_style)

xlab_style <- lazyopt::fenge(opt$xlab_style)

ylab_style <- lazyopt::fenge(opt$ylab_style)

cor_just <- fenge(opt$cor_just) %>% as.numeric()

xydata <- fenge(opt$xydata) %>% as.numeric()

data <- read.table(opt$inputpath, sep="\t", head=T, check.names=F) %>% set_rownames(.[,1]) %>% .[,xydata]
labx <- if(xlab_style[4] == "") colnames(data)[1] else xlab_style[4]
laby <- if(ylab_style[4] == "") colnames(data)[2] else ylab_style[4]

data %<>% set_colnames(c("x","y"))
if(opt$xlog == "log2") data$x <- log2(data$x + 1) 
if(opt$xlog == "log10") data$x <- log10(data$x + 1)
if(opt$ylog == "log2") data$y <- log2(data$y + 1) 
if(opt$ylog == "log10") data$y <- log10(data$y + 1)
#相关性系数
correlation <- cor(data$x, data$y, method = opt$method)

gg <- ggplot(data) +
  theme_bw() +
  geom_point(aes(x = x, y = y), size = 5, color = opt$dot_color) +
  theme(panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(),
        axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
        axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
        title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
        axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2], angle = as.numeric(xlab_style[5]), hjust = 0.5, vjust = 0.5),
        axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], angle = as.numeric(ylab_style[5]), hjust = 0.5, vjust = 0.5),
        plot.title = element_text(hjust = 0.5)
  ) +
  labs(x = labx, y = laby, title = main_style[4]) + 
  annotate("text", x = Inf, y = -Inf, label=paste0("correlation = ", round(correlation,6)), hjust = cor_just[1], vjust = -cor_just[2], size = opt$cor_size, colour = opt$cor_color)


if (opt$display_area != " ") {
  display_area <- lazyopt::fenge(opt$display_area)
  area <- as.numeric(lazyopt::fenge(display_area[2], ","))
  if (display_area[1] == "x") {
    gg <- gg + facet_zoom(xlim = c(area[1], area[2]))
  }
  if (display_area[1] == "y") {
    gg <- gg + facet_zoom(ylim = c(area[1], area[2]))
  }
}

if(opt$drawaxis) {
  gg <- gg + geom_hline(yintercept=0,lty=1,lwd=0.8) + geom_vline(xintercept=0,lty=1,lwd=0.8)
}

if(opt$hline != "") {
  intercept <- fenge(opt$hline, ",") %>% as.numeric()
  gg <- gg + geom_hline(yintercept=intercept,lty=2,lwd=0.5)
}

if(opt$vline != "") {
  intercept <- fenge(opt$vline, ",") %>% as.numeric()
  gg <- gg + geom_vline(xintercept=intercept,lty=2,lwd=0.5)
}

if(opt$dline != "") {
  inter_slope <- fenge(opt$dline, ";")
  is <- c()
  for(i_s in inter_slope) {
    is <- fenge(i_s, ",") %>% as.numeric()
    gg <- gg + geom_abline(intercept=is[1], slope = is[2],lty=2,lwd=0.5)
  }
}

paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt) %>%
  ggsave(gg, width = imageSize[1], height = imageSize[2], dpi = opt$resolution)


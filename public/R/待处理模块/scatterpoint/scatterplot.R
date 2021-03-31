pacman::p_load(lazyopt, car, ggplot2, ggrepel, dplyr, ggforce, magrittr, tibble)

arg <- c("-i", "F:/CloudPlatform/R/nmds/nmds_sites.xls",
         "-g", "F:/CloudPlatform/R/nmds/nmds_group.txt",
         "-o", "F:/CloudPlatform/R/nmds/",
         "-pxy", "MDS1:MDS2"
)

opt <- matrix(c("tablepath", "i", 2, "character", "this is datatable path", "",
                "xydata", "xyd", 2, "character", "the column number of XY,eg 2:3", "2:3",
                "filepath", "o", 2, "character", "this is file packagepath", getwd(),
                "xlog", "xl", 1, "character", "preprogress x data, none/log2/log10", "log2",
                "ylog", "yl", 1, "character", "preprogress y data, none/log2/log10", "log2",
                "auxiliaryline", "al", 1, "character", "add the auxiliary line,horizontal,vertical,diagonal, eg: h:2@v:3@diag:2,3", "",
                "drawcorrelation", "dc", 1, "logical", "whether draw correlation", "TRUE",
                "display_area", "da", 1, "character", "Zoom in on an area,sample x:2,4,only x or y", " ",
                "onecolor", "oc", 1, "character", "no group file color", "#48FF75",
                "imageSize", "is", 1, "character", "The height and width of the picture", "15:12",
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

imageSize <- lazyopt::fenge(opt$imageSize)
width <- imageSize[1] %>% as.numeric()
height <- imageSize[2] %>% as.numeric()
main_style <- lazyopt::fenge(opt$main_style)

xtext_style <- lazyopt::fenge(opt$xtext_style)

ytext_style <- lazyopt::fenge(opt$ytext_style)

xlab_style <- lazyopt::fenge(opt$xlab_style)

ylab_style <- lazyopt::fenge(opt$ylab_style)

xydata <- fenge(opt$xydata) %>% as.numeric()
data <- read.table(opt$inputpath, sep="\t", head=T, check.names=F) %>% set_rownames(data[,1]) %>% data.frame() %>%  .[,xydata] %>% .[order(.[,1]),] %>% set_colnames(c("x","y"))
labx <- if(xlab_style[3] == "") colnames(data)[xydata[1]] else xlab_style[3]
laby <- if(ylab_style[3] == "") colnames(data)[xydata[2]] else ylab_style[3]

gg <- ggplot(data) +
  theme_bw() +
  geom_point(aes(x = x, y = y), size = 5, color = opt$onecolor) +
  theme(legend.justification = c("right", "top"),
        panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(),
        axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
        axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
        title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
        axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
        axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2]),
        plot.title = element_text(hjust = 0.5)
  ) +
  labs(x = labx, y = laby, title = main_style[4])

# #¸¨ÖúÏß
# gg <- gg + geom_vline(xintercept=c(-(opt$FCcutoff_Line),opt$FCcutoff_Line),lty=3, col=opt$line_col,lwd=0.5) +
#   geom_hline(yintercept=-log10(opt$pCutoff_Line),lty=3,col=opt$line_col,lwd=0.5)

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
paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt) %>%
  ggsave(gg, width = width, height = height, dpi = resolution)


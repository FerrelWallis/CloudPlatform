pacman::p_load(lazyopt, ggplot2, dplyr, magrittr, tibble)
spec <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                 "filepath", "o", 2, "character", "The package path of the output image", "",
                 "imagename","in",1,"character","the image name", "box",
                 "imageSize", "ls", 1, "character", "The height and width of the picture", "20:14",
                 "xtext_style", "xts", 1, "character", "X text style  sample:Font:font type:font size", "sans:bold.italic:13",
                 "ytext_style", "yts", 1, "character", "Y text style  sample:Font:font type:font size", "sans:bold.italic:14",
                 "xlab_style", "xls", 1, "character", "X lab style  sample:Font:font type:font size:name", "sans:bold.italic:12: ",
                 "ylab_style", "yls", 1, "character", "Y lab style  sample:Font:font type:font size:name", "sans:bold.italic:13: ",
                 "main_style", "ms", 1, "character", "Main style  sample:Font:font type:font size:name", "sans:bold.italic:12: ",
                 "legendtext_style", "lts", 1, "character", "Legend text style  sample: Font:font type:font size", "sans:bold.italic:15",
                 "legendtitle_style", "lls", 1, "character", "Legend title style  sample: Font:font type:font size:name", "sans:bold.italic:15: ",
                 "colorstyle", "cs", 1, "character", "The parameter used to set the color of the column. The parameter arrangement is dark: the light color is a color group", "#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
                 "resolution", "dpi", 1, "numeric", "Here dpi is used to adjust the resolution, it can be 72,96,300 or 600, not all of these Numbers will become 300", "300",
                 "main_position", "mp", 1, "numeric", "Set the horizontal position of the main heading between 0 and 1, from left to right", "0.5",
                 "legend_position", "lp", 1, "character", "Set the position of the legend, parameter mode 'position: position ', four positions, right,left,bottom,top", "right:top",
                 "legend_exist", "le", 1, "logical", "Sets whether the legend exists", "TRUE",
                 "alpha", "alp", 1, "numeric", "Sets the overall transparency of the color", "0.8",
                 "box_width", "bw", 1, "numeric", "Width of box", "0.7",
                 "legend_size_arrange", "lsa", 1, "character", "Used to set the arrangement of the legend and the overall width and height", "1:1.5:1.5",
                 "shoupoint", "sp", 1, "logical", "Whether to show the inside of the box", "FALSE",
                 "ylim", "ymm", 1, "character", "axis ymin and ymax split by ':'", "",
                 "addition", "add", 1, "numeric", "addition transparency, when '0' no display ", "1",
                 "flip", "f", 1, "logical", "whether to flip the graph", "TRUE",
                 "diversity","d",1,"logical","whether to draw alpha diveristy graph", "FALSE",
                 "div_colname","dc",1,"character","the colname of data to draw alpha diveristy graph", "S.obs",
                 "help", "h", 0, "logical", "Help document", "TRUE"
), byrow = TRUE, ncol = 6)

args<-c("-i","F:/CloudPlatform/users/6/ADB317155813/out/alpha_diversity.xls",
        "-o","F:/CloudPlatform/users/6/ADB317155813/out",
        "-sp","TRUE",
        "-ls", "12:10",
        "-in", "S.obs",
        "-d","TRUE",
        "-dc","S.obs"
)
# opt <- lazyopt(spec,args)
opt <- lazyopt(spec)

fenge <- function(str) {
  str <- strsplit(str, ":")[[1]]
  return(str)
}

tablepath <- opt$tablepath

filepath <- opt$filepath

main_style <- opt$main_style; main_style <- fenge(main_style)

xtext_style <- opt$xtext_style; xtext_style <- fenge(xtext_style)

ytext_style <- opt$ytext_style; ytext_style <- fenge(ytext_style)

xlab_style <- opt$xlab_style; xlab_style <- fenge(xlab_style)

ylab_style <- opt$ylab_style; ylab_style <- fenge(ylab_style)

legendtext_style <- opt$legendtext_style; legendtext_style <- fenge(legendtext_style)

legendtitle_style <- opt$legendtitle_style; legendtitle_style <- fenge(legendtitle_style)

imageSize <- opt$imageSize; imageSize <- fenge(imageSize)

colorstyle <- strsplit(opt$colorstyle, ":")[[1]]

resolution <- opt$resolution; if (resolution != 72 &&
  resolution != 96 &&
  resolution != 300 &&
  resolution != 600) { resolution <- 300 }

legend_size_arrange <- opt$legend_size_arrange; legend_size_arrange <- fenge(legend_size_arrange)

legend_position <- opt$legend_position; legend_position <- fenge(legend_position)

if(opt$diversity) {
  usedata <- read.delim(tablepath, check.names = F) %>% select(Group, opt$div_colname) %>% set_colnames(c("name","data"))
} else {
  data <- read.delim(tablepath, check.names = F)
  data <- as.matrix(data)
  names <- colnames(data); cl <- ncol(data); rw <- nrow(data)
  dim(data) <- c(rw * cl, 1)
  data <- as.numeric(data)
  name <- as.character(t(matrix(rep(names, rw), nrow = cl)))
  usedata <- data.frame(name, data)
}

result <- ggplot() +
  theme_classic() +
  geom_boxplot(data = usedata, aes(x = name, y = data, color = name), outlier.colour = "blue", outlier.size = 1, outlier.alpha = opt$addition, show.legend = opt$legend_exist, 
               width = opt$box_width, alpha = opt$alpha) +
  scale_color_manual(values = colorstyle, name = legendtitle_style[4]) +
  theme(
    legend.key.width = unit(as.numeric(legend_size_arrange[2]), "cm"),
    legend.key.height = unit(as.numeric(legend_size_arrange[3]), "cm"),
    legend.position = legend_position[1],
    legend.justification = legend_position[2],
    #标题调节居左中右
    plot.title = element_text(hjust = opt$main_position),
  ) +
  guides(color = guide_legend(
    reverse = T,
    ncol = as.numeric(legend_size_arrange[1]),
    byrow = TRUE
  )) +
  labs(y = xlab_style[4], x = ylab_style[4], title = main_style[4], color = " ") +
  theme(panel.border = element_blank(),
        panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(),
        legend.background = element_blank(),
  ) +
  theme(
    axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
    axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
    legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
    legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
    title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
    axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
    axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2])) 

if (opt$ylim != "") {
  ylim <- fenge(opt$ylim) %>% as.numeric()
  result <- result + ylim(ylim[1], ylim[2])
}

if(opt$flip) result <- result + coord_flip()
if (opt$shoupoint == TRUE) { result <- result + geom_jitter(data = usedata, aes(x = name, y = data, color = name), alpha = 0.5, width = opt$box_width / 2.2) }
filepath <- paste0(opt$filepath, "/", opt$imagename, ".", "pdf")
ggsave(filepath, result, width = as.numeric(imageSize[1]), height = as.numeric(imageSize[2]), dpi = resolution)



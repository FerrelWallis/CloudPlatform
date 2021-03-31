rm(list = ls(all = TRUE))
suppressMessages(library("ggplot2"))
suppressMessages(library("dplyr"))
suppressMessages(library("lazyopt"))
suppressMessages(library("ggrepel"))

fenge <- function(str, sp = ":") {
  return(strsplit(str, sp)[[1]])
}

opt <- matrix(c(
  "colorstyle", "cs", 1, "character", "Set the color to ':' split", "#1E90FF:#E41A1C",
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "filepath", "o", 2, "character", "The package path of the output image", "",
  "point_size", "ps", 1, "numeric", "set the point size", "2",
  "yrange", "yr", 1, "character", "y range split by ':'", "all",
  "add_hline", "ah", 1, "character", "Add a horizontal line，sample number:colour:linesize", "1.5:gold2:0.8",
  "label_style", "lbs", 1, "character", "label text style,sample Font:font type:font size:colour", "sans:plain:4:#CA9197",
  "legend_image_size", "lis", 1, "numeric", "image of lengend size ", "3",
  "legendtext_style", "lts", 1, "character", "Legend text style  sample: Font:font type:font size", "sans:plain:14",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:12",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name:colour", "sans:bold.italic:16:virtual:black",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name:colour", "sans:bold.italic:16:LOD:black",
  "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:16: :black",
  "imageSize", "ls", 1, "character", "The height and width of the picture", "12:11",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
  "imageName", "in", 1, "character", "picture name", "manhadun_virtual",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

imageSize <- as.numeric(fenge(opt$imageSize))
resolution <- opt$resolution; if (resolution != 72 &&
  resolution != 96 &&
  resolution != 300 &&
  resolution != 600) { resolution <- 300 }
main_style <- fenge(opt$main_style)
xlab_style <- fenge(opt$xlab_style)
ylab_style <- fenge(opt$ylab_style)
ytext_style <- fenge(opt$ytext_style)
add_hline <- fenge(opt$add_hline)
legendtext_style <- fenge(opt$legendtext_style)
label_style <- fenge(opt$label_style)
yrange <- opt$yrange; if (yrange == "all") { yrange <- NULL }else { yrange <- as.numeric(fenge(yrange)) }

data <- read.delim(opt$tablepath)
colnames(data) <- c("chr", "site", "lod", "name")
data <- data[which(!is.na(data[, 3])),]

yline <- as.numeric(add_hline[1])
data <- data %>% mutate(col = case_when(lod > yline ~ "up", lod <= yline ~ "down"))
data[, 4][which(data[, 3] <= yline)] <- ""

p <- ggplot(data) +
  geom_point(aes(x = site, y = lod, col = col), show.legend = TRUE, size = opt$point_size) +
  theme_bw() +
  scale_x_continuous(expand = c(0.01, 0.01), labels = NULL) +
  scale_y_continuous(expand = c(0.01, 0.01), limits = yrange) +
  geom_hline(yintercept = yline, linetype = 2, col = add_hline[2], size = as.numeric(add_hline[3])) +
  labs(x = xlab_style[4], col = "", title = main_style[4], y = ylab_style[4]) +
  theme(panel.border = element_blank(),
        panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(),
        legend.background = element_blank(),
  ) +
  geom_text_repel(aes(x = site, y = lod, label = name)
    , size = as.numeric(label_style[3]), force = 10, col = label_style[4], family = label_style[1], face = label_style[2]
  ) +
  scale_color_manual(values = fenge(opt$colorstyle)) +
  theme(
    axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
    axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2], colour = xlab_style[5]),
    axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], colour = ylab_style[5]),
    legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
    title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2], main_style[5]),
    plot.title = element_text(hjust = 0.5)
  ) +
  guides(colour = guide_legend(override.aes = list(size = opt$legend_image_size), byrow = TRUE, reverse = TRUE))
ggsave(filename = paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt), p, width = imageSize[1], height = imageSize[2], dpi = resolution)

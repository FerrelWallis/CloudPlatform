pacman::p_load(ggplot2, treemapify, lazyopt, magrittr, dplyr)

maxmin <- function(c) {
  max <- max(c); min <- min(c)
  k <- max - min
  return((c - min) / k)
}

fenge <- function(str) {
  return(strsplit(str, ":")[[1]])
}

arg <- c("-i", "E:/projects/DrawPicture-r/com/yun-work/treemap/da.txt",
         "-o", "E:/projects/DrawPicture-r/com/yun-work/treemap")

opt <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                "filepath", "o", 2, "character", "The package path of the output image", "",
                "colorstyle", "cs", 1, "character", "The parameter used to set the color of the column. The parameter arrangement is dark: the light color is a color group", "#67001F:#890A24:#AB1529:#BF3237:#CF5246:#DE725B:#EB9273:#F5AE8E:#F9C7AD:#FCDDCB:#F9EAE1:#F7F7F7:#E5EEF3:#D4E6F0:#BAD9E9:#9DCAE1:#7CB7D6:#58A0CA:#3C8ABE:#2D76B4:#1E61A5:#114883:#053061",
                "smallgrid_border_col", "sbc", 1, "character", "Small grid border color", "white",
                "largegrid_border_col", "lbc", 1, "character", "Large grid border color", "gray",
                "Large_label_color", "llc", 1, "character", "Large label color", "black",
                "small_label_color", "slc", 1, "character", "small label color", "#842B00",
                "xlab_style", "xls", 1, "character", "X lab style  sample:Font:font type:font size:name", "sans:plain:17: :black",
                "ylab_style", "yls", 1, "character", "Y lab style  sample:Font:font type:font size:name", "sans:plain:17: :black",
                "main_style", "ms", 1, "character", "Main style  sample:Font:font type:font size:name", "sans:plain:17: :black",
                "imageSize", "ls", 1, "character", "The height and width of the picture", "18:19",
                "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
                "imageName", "in", 1, "character", "picture name", "treemap",
                "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
                "help", "h", 0, "logical", "Help document", "TRUE"
), byrow = TRUE, ncol = 6) %>% lazyopt(arg = NULL)


imageSize <- as.numeric(fenge(opt$imageSize))
xlab_style <- fenge(opt$xlab_style)
ylab_style <- fenge(opt$ylab_style)
main_style <- fenge(opt$main_style)
data <- read.delim(opt$tablepath)
data[, 3] <- maxmin(data[, 3])
colnames(data) <- c("id", "parent", "value")
p <- ggplot(data, aes(area = value, fill = parent, label = id, subgroup = parent)) +
  geom_treemap(aes(alpha = value), color = opt$smallgrid_border_col) +
  guides(alpha = FALSE, fill = FALSE) +
  geom_treemap_subgroup_border(color = opt$largegrid_border_col) +
  geom_treemap_subgroup_text(place = "centre", grow = T, alpha = 0.71, colour = opt$Large_label_color, fontface = "italic", min.size = 0
    , inherit.aes = TRUE) +
  geom_treemap_text(fontface = "italic", colour = opt$small_label_color, place = "bottomright", grow = TRUE, alpha = 0.7) +
  theme(axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2], colour = xlab_style[5]),
        axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], colour = ylab_style[5]),
        title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2], main_style[5]),
        plot.title = element_text(hjust = 0.5)
  ) +
  labs(x = xlab_style[4], y = ylab_style[4], title = main_style[4]) +
  scale_fill_manual(values = colorRampPalette(fenge(opt$colorstyle))(length(unique(data[, 2]))))


resolution <- opt$resolution; if (resolution != 72 &&
  resolution != 96 &&
  resolution != 300 &&
  resolution != 600) { resolution <- 300 }
filepath <- paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt)
ggsave(filename = filepath, p, width = imageSize[1], height = imageSize[2], dpi = resolution)
dd <- data.frame(data[, 2] %>% levels, colorRampPalette(fenge(opt$colorstyle))(length(unique(data[, 2]))))
write.table(dd, file = paste0(opt$filepath, "/", "group_color.xls"), sep = "\t", row.names = F, col.names = F)





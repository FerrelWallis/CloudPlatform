pacman::p_load(magrittr, reshape, lazyopt, dplyr, ggplot2)
opt <- matrix(c(
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "filepath", "o", 2, "character", "The package path of the output image", "",
  "groupfile", "g", 1, "character", "groupfile path,wait ", " ",
  "use_nrow", "un", 1, "numeric", "use head nrow", "-1",
  "box_width", "bw", 1, "numeric", "set box_width", "0.1",
  "colorstyle", "cs", 1, "character", "Set the color to ':' split", "#B2182B:#E69F00:#56B4E9:#009E73:#F0E442:#0072B2:#D55E00:#CC79A7:#CC6666:#9999CC:#66CC99:#999999:#ADD1E5",
  "yrange", "yr", 1, "character", "y range split by ':'", "all",
  "flip", "fp", 1, "logical", "whether to flip", "FALSE",
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:18",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:16",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name", "sans:bold.italic:20: ",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name", "sans:bold.italic:20: ",
  "strip_text_style", "sts", 1, "character", "Y lab style Font:font type:font size", "sans:bold.italic:16",
  "legendtext_style", "lts", 1, "character", "Legend text style  sample: Font:font type:font size", "sans:plain:14",
  "legendtitle_style", "lms", 1, "character", "Legend title style  sample: Font:font type:font size:name", "sans:bold.italic:15: ",
  "imageSize", "is", 1, "character", "The height and width of the picture", "20:20",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
  "imageName", "in", 1, "character", "picture name", "violin",
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()
xtext_style <- lazyopt::fenge(opt$xtext_style)
ytext_style <- lazyopt::fenge(opt$ytext_style)
xlab_style <- lazyopt::fenge(opt$xlab_style)
ylab_style <- lazyopt::fenge(opt$ylab_style)
strip_text_style <- lazyopt::fenge(opt$strip_text_style)
imageSize <- lazyopt::fenge(opt$imageSize) %>% as.numeric()
legendtext_style <- lazyopt::fenge(opt$legendtext_style)
legendtitle_style <- lazyopt::fenge(opt$legendtitle_style)
resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()
yrange <- opt$yrange; if (yrange == "all") { yrange <- NULL }else { yrange <- lazyopt::fenge(yrange) %>% as.numeric() }
data <- read.delim(opt$tablepath, check.names = FALSE, row.names = 1)
if (opt$use_nrow != -1) {
  data <- head(data, n = opt$use_nrow)
}
data %<>% melt()
colnames(data)[1:2] <- c("variable", "value")
if (opt$groupfile != " ") {
  group <- read.delim(opt$groupfile, check.names = FALSE, row.names = 1)

  groupdata <- read.delim(opt$tablepath, check.names = FALSE, row.names = 1)

  if (opt$use_nrow != -1) { groupdata <- head(groupdata, n = opt$use_nrow) }

  group2 <- colnames(groupdata) %>% match(rownames(group))

  colnames(groupdata) <- as.character(group[, 1])[group2]

  data[, 3] <- (groupdata %>% melt())[, 1]
  colnames(data)[3] <- "group"
}
p <- ggplot(data, aes(x = variable, y = value)) +
  geom_violin(aes(color = variable, fill = variable), trim = FALSE) +
  geom_boxplot(width = opt$box_width, inherit.aes = TRUE, outlier.fill = NULL, outlier.alpha = 0) +
  theme_bw() +
  theme(panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(),
        axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
        axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3]), angle = 60, vjust = 0.5),
        axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
        axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2]),
        strip.text = element_text(family = strip_text_style[1], face = strip_text_style[2], size = as.numeric(strip_text_style[3])),
        legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
        legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2])
  ) +
  scale_color_manual(values = lazyopt::fenge(opt$colorstyle)) +
  scale_fill_manual(values = lazyopt::fenge(opt$colorstyle)) +
  labs(x = xlab_style[4], y = ylab_style[4], color = " ", fill = " ") +
  scale_y_continuous(limits = yrange)
if (opt$groupfile != " ") {
  p <- p + facet_grid(. ~ group, scales = "free")
}
if (opt$flip) { p <- p + coord_flip() }
paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt) %>%
  ggsave(p, width = imageSize[1], height = imageSize[2], dpi = resolution)

pacman::p_load(ggplot2, ggpubr, dplyr, magrittr, grDevices, RColorBrewer, lazyopt)

getcolor <- function(color) {
  result <- color[1]
  for (i in 2:length(color)) {
    result <- paste(result, color[i], sep = ":")
  }
  return(result)
}

ggstyle <- function(gg, opt, data) {
  xlab_style <- lazyopt::fenge(opt$xlab_style)
  ylab_style <- lazyopt::fenge(opt$ylab_style)
  ytext_style <- lazyopt::fenge(opt$ytext_style)
  col <- colorRampPalette(lazyopt::fenge(opt$colorstyle))(data[, 1] %>% unique() %>% length())
  gg <- gg +
    scale_fill_manual(values = col) +
    theme(
      panel.border = element_blank(),
      panel.grid.major = element_blank(),
      panel.grid.minor = element_blank(),
      legend.background = element_blank()
    ) +
    theme(
      axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
      axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2], colour = xlab_style[4]),
      axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], colour = ylab_style[4]),
    )
  return(gg)
}

arg <- c("-i", "F:/CloudPlatform/R/break_bar/breakbar2.txt",
         "-o", "F:/CloudPlatform/R/break_bar",
         "-bsp","2000:3000"
)

opt <- matrix(c(
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "filepath", "o", 2, "character", "The package path of the output image", "",
  "Broken_shaft_position", "bsp", 2, "character", "Set the range of the broken shaft, min:max", "",
  "top_part_size", "tps", 1, "numeric", "set top part size ,total size is 1", "0.3",
  "colorstyle", "cs", 1, "character", "Set the color to ':' split", getcolor(brewer.pal(8, "Set2")),
  "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:16: :black",
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:11",
  "x_text_positon", "xtp", 1, "character", "split by ':', sample angle:hjust:vjust", "90:1:0.5",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:11",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name:color", "sans:bold.italic:14:black: ",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name:color", "sans:bold.italic:14:black: ",
  "legendtext_style", "lts", 1, "character", "Legend text style  sample: Font:font type:font size", "sans:plain:14",
  "legendtitle_style", "lms", 1, "character", "Legend title style  sample: Font:font type:font size:name", "sans:bold.italic:15: ",
  "imageSize", "is", 1, "character", "The height and width of the picture", "NULL:NULL",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
  "imageName", "in", 1, "character", "picture name", "duan_bar",
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

opt$tablepath


top_part_size <- opt$top_part_size
ylab <- ylab_style <- lazyopt::fenge(opt$ylab_style)[5]
xlab <- xlab_style <- lazyopt::fenge(opt$xlab_style)[5]
main_style <- lazyopt::fenge(opt$main_style)
resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()
data <- read.delim(opt$tablepath, check.names = FALSE, header = TRUE) %>% select(c(1, 2))
colnames(data) <- c("names", "y")
n <- fenge(opt$Broken_shaft_position) %>% as.numeric()
x_text_positon <- lazyopt::fenge(opt$x_text_positon) %>% as.numeric()
xtext_style <- lazyopt::fenge(opt$xtext_style)
top <- ggplot(data) +
  geom_col(aes(x = names, y = y, fill = names), show.legend = FALSE) +
  theme_bw() +
  coord_cartesian(ylim = c(n[2], max(data$y))) +
  scale_y_continuous(expand = c(0, 0)) +
  labs(x = NULL) +
  theme(axis.text.x = element_blank(), axis.ticks.x = element_blank())
top <- ggstyle(top, opt, data) +
  theme(
    axis.line.y = element_line(),
    title = element_text(size = as.numeric(main_style[3]), 
    family = main_style[1], 
    face = main_style[2], 
    main_style[5]),
    plot.title = element_text(hjust = 0.5)
  ) +
  labs(title = main_style[4])
bottom <- ggplot(data) +
  geom_col(aes(x = names, y = y, fill = names), show.legend = FALSE) +
  theme_bw() +
  coord_cartesian(ylim = c(min(data$y) - abs(min(data$y)), n[1])) +
  scale_y_continuous(expand = c(0, 0)) +
  theme(
  axis.line = element_line(),
  axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3]), angle = x_text_positon[1], hjust = x_text_positon[2], vjust = x_text_positon[3])
  )
bottom <- ggstyle(bottom, opt, data) + xlab(xlab)
if (top_part_size >= 0.5) { top <- top + ylab(ylab); bottom <- bottom + ylab("") }else {
  bottom <- bottom + ylab(ylab); top <- top + ylab("") }

imageSize <- lazyopt::fenge(opt$imageSize)
width <- NA; height <- NA
if (imageSize[1] != "NULL") { width <- imageSize[1] %>% as.numeric() }
if (imageSize[2] != "NULL") { height <- imageSize[2] %>% as.numeric() }
ggarrange(top, bottom, heights = c(top_part_size, 1 - top_part_size), ncol = 1, nrow = 2, common.legend = FALSE, align = "v") %>%
  ggsave(paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt), ., width = width, height = height, dpi = resolution)


data %>% set_colnames(c("1","2"))





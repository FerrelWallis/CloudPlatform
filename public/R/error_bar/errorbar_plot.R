pacman::p_load(ggplot2, dplyr, magrittr, reshape, sqldf, lazyopt)
opt <- matrix(c(
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "filepath", "o", 2, "character", "The package path of the output image", "",
  "bar_color", "bc", 1, "character", "set bar color", "blue",
  "line_color", "lc", 1, "character", "set bar color", "black",
  "point_color", "pc", 1, "character", "set point_color color", "yellow",
  "row_scale", "rs", 1, "character", "scale:center", "TRUE:FALSE",
  "line_width", "lw", 1, "numeric", "the width of the error line", "0.5",
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:13",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name:colour", "sans:bold.italic:16: :black",
  "x_text_positon", "xtp", 1, "character", "split by ':', sample angle:hjust", "60:1",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:12",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name:colour", "sans:bold.italic:16:value:black",
  "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:16: :black",
  "imageSize", "ls", 1, "character", "The height and width of the picture,NULL adaptive", "NULL:NULL",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
  "imageName", "in", 1, "character", "picture name", "error_bar",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

pp <- function(pp, opt) {
  xlab_style <- lazyopt::fenge(opt$xlab_style)
  ylab_style <- lazyopt::fenge(opt$ylab_style)
  main_style <- lazyopt::fenge(opt$main_style)
  xtext_style <- lazyopt::fenge(opt$xtext_style)
  ytext_style <- lazyopt::fenge(opt$ytext_style)
  x_text_positon <- lazyopt::fenge(opt$x_text_positon) %>% as.numeric()
  return(pp +
           geom_errorbar(aes(x = pos, ymin = y - df, ymax = y + df), color = opt$bar_color, width = opt$line_width) +
           geom_line(aes(x = pos, y = y), linetype = 1, color = opt$line_color) +
           geom_point(aes(x = pos, y = y), color = opt$point_color, size = 2) +
           scale_x_continuous(labels = pp$data$x, breaks = pp$data$pos) +
           theme_bw() +
           theme(
             panel.border = element_blank(), 
             axis.line = element_line(),
             panel.grid.major = element_blank(),
             panel.grid.minor = element_blank(),
             legend.background = element_blank(),
             axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
             axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3]), angle = x_text_positon[1], hjust = x_text_positon[2]),
             axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2], colour = xlab_style[5]),
             axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], colour = ylab_style[5]),
             title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2], main_style[5]),
             plot.title = element_text(hjust = 0.5)
           ) +
           labs(title = main_style[4], y = ylab_style[4], x = xlab_style[4])
  )
}

row_scale <- lazyopt::fenge(opt$row_scale) %>% as.logical()
resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()
imageSize <- lazyopt::fenge(opt$imageSize)
width <- NA; height <- NA
if (imageSize[1] != "NULL") { width <- imageSize[1] %>% as.numeric() }
if (imageSize[2] != "NULL") { height <- imageSize[2] %>% as.numeric() }
data1 <- read.delim(opt$tablepath, check.names = FALSE, row.names = 1) %>%
  t() %>%
  scale(scale = row_scale[1], center = row_scale[2]) %>%
  t() %>%
  melt() %>%
  select(2, 3) %>%
  set_colnames(c("x", "y"))
sqldf("select x,avg(y),stdev(y) from data1 group by x") %>%
  set_colnames(c("x", "y", "df")) %>%
  mutate(pos = (seq_len(nrow(.)))) %>%
  ggplot() %>%
  pp(opt) %>%
  ggsave(paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt), ., width = width, height = height, dpi = resolution)

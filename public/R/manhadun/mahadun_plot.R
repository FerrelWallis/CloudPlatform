pacman::p_load(ggplot2, dplyr, lazyopt, magrittr, grDevices, RColorBrewer)

xchange <- function(data1, area = 1, region = 2) {
  data <- data1
  kk <- unique(data[, area])
  ll <- length(kk)
  for (i in 2:ll) {
    zz1 <- which(data[, area] == kk[i - 1])
    max <- max(data[zz1, region])
    zz2 <- which(data[, area] == kk[i])
    data[zz2, region] <- data[zz2, region] + max
  }
  return(data)
}

getxlabposition <- function(data, area = 1, region = 2) {
  kk <- unique(data[, area])
  ll <- length(kk)
  result <- 1:ll
  for (i in 1:ll) {
    z <- data[which(data[, area] == kk[i]), region]
    result[i] <- ((max(z) - min(z)) / 2) + min(z)
  }
  return(result)
}

getxlineposition <- function(data, area = 1, region = 2) {
  kk <- unique(data[, area])
  ll <- length(kk) - 1
  result <- 1:ll
  for (i in 1:ll) {
    z <- data[which(data[, area] == kk[i]), region]
    result[i] <- max(z)
  }
  return(result)
}

getn <- function(num) {
  result <- 1
  while ((num <- (num / 10)) > 100) {
    result <- result * 10
  }
  return(result)
}

getcolor <- function(color) {
  result <- color[1]
  for (i in 2:length(color)) {
    result <- paste(result, color[i], sep = ":")
  }
  return(result)
}

opt <- matrix(c(
  "colorstyle", "cs", 1, "character", "Set the color to ':' split", "#E41A1C:#9B445D:#526E9F:#3C8A9B:#469F6C:#54A453:#747B78:#94539E:#BD6066:#E97422:#FF990A:#FFCF20:#FAF632:#D4AE2D:#AF6729:#BF6357:#E17597:#E884B9:#999999",
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "filepath", "o", 2, "character", "The package path of the output image", "",
  "add_hline", "ah", 1, "character", "Add a horizontal line，sample number:colour:linesize:display", "20:green:1:FALSE",
  "high_point_color", "hpc", 1, "character", "set colour of high point", "#7F4B89",
  "pointsize", "ps", 1, "character", "set point size top:bottom", "0.5:0.5",
  "draw_type", "dt", 1, "character", "point or line", "point",
  "page", "p", 1, "logical", "whether to page image", "FALSE",
  "yrange", "yr", 1, "character", "y range split by ':'", "all",
  "legendtext_style", "lts", 1, "character", "Legend text style  sample: Font:font type:font size", "sans:plain:14",
  "legend_imagesize", "lis", 1, "numeric", "set legend point image size", "3",
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:13",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:12",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name:colour", "sans:bold.italic:16: :black",
  "x_text_positon", "xtp", 1, "character", "split by ':', sample angle:hjust", "60:1",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name:colour", "sans:bold.italic:16: :black",
  "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:16: :black",
  "imageSize", "ls", 1, "character", "The height and width of the picture", "13:7",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
  "imageName", "in", 1, "character", "picture name", "manhadun",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

data <- read.delim(opt$tablepath, check.names = FALSE)

data[, 2] <- data[, 2] / (getn(max(data[, 2])))

data <- xchange(data)
xlabposition <- getxlabposition(data)
xline <- getxlineposition(data)
colnames(data)[1:3] <- c("Chr", "region", "LOD")

xlab_style <- lazyopt::fenge(opt$xlab_style)
ylab_style <- lazyopt::fenge(opt$ylab_style)
main_style <- lazyopt::fenge(opt$main_style)
xtext_style <- lazyopt::fenge(opt$xtext_style)
ytext_style <- lazyopt::fenge(opt$ytext_style)
legendtext_style <- lazyopt::fenge(opt$legendtext_style)
add_hline <- lazyopt::fenge(opt$add_hline)

yrange <- opt$yrange; if (yrange == "all") { yrange <- NULL }else { yrange <- as.numeric(lazyopt::fenge(yrange)) }
imageSize <- as.numeric(lazyopt::fenge(opt$imageSize))
resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()
x_text_positon <- lazyopt::fenge(opt$x_text_positon) %>% as.numeric()
pointsize <- lazyopt::fenge(opt$pointsize) %>% as.numeric()


data <- data %>%
  mutate(size2 = LOD) %>%
  mutate(col = Chr)

if (add_hline[4] %>% as.logical()) {
  data %<>% mutate(size = case_when(size2 > as.numeric(add_hline[1]) ~ 1.5, size2 <= as.numeric(add_hline[1]) ~ 1)) %>%
    select(c(1, 2, 3, 5, 6))
  data$col[which(data$LOD > as.numeric(add_hline[1]))] <- rep("high", length(which(data$LOD > as.numeric(add_hline[1]))))
  d <- as.character(data$col)
  d[which(is.na(d))] <- " "
  data$col <- d }else {
  colnames(data)[4] <- "size"
  data$size <- rep(1, nrow(data))
  data %<>% select(c(1, 2, 3, 5, 4))
}
point_col <- lazyopt::fenge(opt$colorstyle)[seq_along(unique(data[, 4]))]
if (add_hline[4] %>% as.logical()) { point_col %<>% c(opt$high_point_color, .) }
p <- ggplot(data, aes(x = region, y = LOD, col = col, size = size, group = Chr))
if (opt$draw_type == "point") { p <- p + geom_point(type = 2) }else { p <- p + geom_line() }
if (!opt$page) { p <- p +
  theme_bw() +
  geom_vline(xintercept = xline, linetype = 'dashed', color = 'grey') }
p <- p +
  scale_y_continuous(expand = c(0, 0), limits = yrange) +
  scale_x_continuous(expand = c(0, 0), breaks = xlabposition, labels = unique(data[, 1])) +
  theme(panel.border = element_blank(),
        panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(),
        legend.background = element_blank(),
  ) +
  theme(
    legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
    axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
    axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3]), angle = x_text_positon[1], hjust = x_text_positon[2]),
    axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2], colour = xlab_style[5]),
    axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], colour = ylab_style[5]),
    title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2], main_style[5]),
    plot.title = element_text(hjust = 0.5)
  ) +
  scale_color_manual(values = point_col, na.value = "green") +
  labs(x = xlab_style[4], y = ylab_style[4], title = main_style[4], col = "") +
  guides(colour = guide_legend(override.aes = list(size = opt$legend_imagesize), labels = letters[1:9]), size = FALSE) +
  scale_size_continuous(range = c(pointsize[1], pointsize[2]))

if (add_hline[4] %>% as.logical()) {
  p <- p + geom_hline(yintercept = as.numeric(add_hline[1]), linetype = 2, col = add_hline[2], size = as.numeric(add_hline[3]))
}
if (opt$page) { p <- p + facet_grid(. ~ Chr, scales = "free") }

paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt) %>%
  ggsave(filename = ., p, width = imageSize[1], height = imageSize[2], dpi = resolution)


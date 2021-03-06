pacman::p_load(lazyopt, ggplot2, dplyr, magrittr)

arg <- c("-i", "F:/CloudPlatform/R/pie/test/table.txt",
         "-o", "F:/CloudPlatform/R/pie/"
)


opt <- matrix(c(
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "imagepackagepath", "o", 2, "character", "outputimage path", "",
  "pie_type", "pt", 1, "character", "pietype only two chose ring,full", "ring",
  "read_line", "rl", 1, "character", "c or r  split by : ,", "c:1,2",
  "colour", "c", 1, "character", "set color split by ','", "#B2182B,#E69F00,#56B4E9,#009E73,#F0E442,#0072B2,#D55E00,#CC79A7,#CC6666,#9999CC,#66CC99,#999999,#ADD1E5",
  "imageformat", "if", 1, "character", "Image formats include PDF,tiff,PNG", "pdf",
  "imageName", "in", 1, "character", "Pictures of name", "pie",
  "imageSize", "is", 1, "character", "The height and width of the picture", "NULL:NULL",
  "title_style", "ts", 1, "character", "set title style name:size:color", "NULL:NULL:NULL",
  "resolution", "dpi", 1, "numeric", "dpi is used to adjust the resolution,only 72,96,300 or 600", "300",
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

read_line <- opt %$% lazyopt::fenge(read_line)
data <- opt %$% read.delim(tablepath)
ll <- lazyopt::fenge(read_line[2], ",") %>% as.numeric()
if (read_line[1] == "c") {
  data <- data.frame(name = data[, ll[1]], value = data[, ll[2]]) %>% arrange(value)
}
if (read_line[1] == "r") {
  data <- data.frame(name = data[ll[1],], value = data[ll[2],]) %>% arrange(value)
}
xwidth <- c(0, data[, 2]); x <- 0
for (i in 2:length(xwidth)) { x[i] <- x[i - 1] + xwidth[i] }
xmin <- x[1:(length(x) - 1)]; xmax <- x[2:length(x)]
ymin <- rep(1, length(xmin)); ymax <- rep(2, length(xmin))
drawdata <- data.frame(xmin, xmax, ymin, ymax, name = data[, 1], percent = ((data[, 2] / sum(data[, 2])) * 100) %>%
  round(2) %>%
  paste0("%") %>%
  paste0(data[, 1], "\n", "percent ", .), value = data[, 2])

col <- opt %$% lazyopt::fenge(colour, ",") %>% .[1:(drawdata$name %>% as.character() %>% length())]

write.table(data.frame(drawdata$name,col), paste0(opt$imagepackagepath, "/col.xls"), sep="\t", quote = F, row.names = F, col.names = F)

title_style <- opt %$% lazyopt::fenge(title_style)
titlecolor <- NULL; titlesize <- NULL; title <- NULL
if (title_style[1] != "NULL") { title <- title_style[1] }
if (title_style[2] != "NULL") { titlesize <- title_style[2] }
if (title_style[3] != "NULL") { titlecolor <- title_style[3] }
if (opt$pie_type == "full") {
  p <- ggplot(drawdata) +
    geom_rect(aes(ymin = xmin, ymax = xmax, xmin = ymin, xmax = ymax, fill = name), color = "white") +
    theme_void() +
    coord_polar("y") +
    scale_fill_manual(values = col, breaks = drawdata$name %>% as.character()) +
    labs(title = title) +
    theme(plot.title = element_text(hjust = 0.5, colour = titlecolor, size = titlesize), legend.position = "bottom")
}
if (opt$pie_type == "ring") {
  xp <- c(0.5, 1.5, 2) %>% rep(., 30)
  p <- ggplot(drawdata) +
    geom_rect(aes(ymin = xmin, ymax = xmax, xmin = ymin, xmax = ymax, fill = name, color = name), color = "white") +
    theme_void() +
    coord_polar("y") +
    geom_label(x = xp[seq_len(nrow(drawdata))], aes(y = (xmax + xmin) / 2, label = percent, color = name), show.legend = FALSE) +
    xlim(c(-1, 2)) +
    theme(legend.position = "none") +
    scale_fill_manual(values = col, breaks = drawdata$name %>% as.character()) +
    scale_color_manual(values = col, breaks = drawdata$name %>% as.character()) +
    labs(title = title) +
    theme(plot.title = element_text(hjust = 0.5, colour = titlecolor, size = titlesize))
}
imageSize <- lazyopt::fenge(opt$imageSize)
resolution <- match.arg(opt %$% as.character(resolution), c("72", "96", "300", "600")) %>% as.numeric()
width <- NA; height <- NA
if (imageSize[1] != "NULL") { width <- imageSize[1] %>% as.numeric() }
if (imageSize[2] != "NULL") { height <- imageSize[2] %>% as.numeric() }
paste0(opt$imagepackagepath, "/", opt$imageName, ".", opt$imageformat) %>%
  ggsave(p, width = width, height = height, dpi = resolution)

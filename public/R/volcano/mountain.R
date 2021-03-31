pacman::p_load(lazyopt, EnhancedVolcano, dplyr, magrittr)
opt <- matrix(c(
  "tablepath", "i", 2, "character", "The path to the table data read", "",
  "filepath", "o", 2, "character", "The package path of the output image", "",
  "pCutoff_Line", "pcl", 1, "numeric", "A horizontal line will be drawn at -log10(pCutoff)", "10e-6",
  "FCcutoff_Line", "fcl", 1, "numeric", "Vertical lines will be drawn at the negative and positive values of log2FCcutoff", "1",
  "show_Line", "sp", 1, "logical", "whether to display pcline", "TRUE",
  "line_col", "lc", 1, "character", "set Line colour", "black",
  "labs_show", "ls", 1, "character", "display somelabs ,str split by ':'", " ",
  "xrang", "xr", 1, "character", "set range of x,split by :", "NULL",
  "yrang", "yr", 1, "character", "set range of y,split by :", "NULL",
  "colorstyle", "cs", 1, "character", "Set the color to ':' split", "#B7B7B7:#4DAF4A:#1E90FF:#E41A1C",
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:18",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:16",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name", "sans:bold.italic:20:log2(FC)",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name", "sans:bold.italic:20:-log10(pvalue)",
  "legend_position", "lp", 1, "character", "position of legend", "bottom",
  "legend_size_type", "lt", 1, "character", "imgaesize:fontsize, sample 6:16", "6:16",
  "imageSize", "is", 1, "character", "The height and width of the picture", "20:20",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
  "imageName", "in", 1, "character", "picture name", "mountain",
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

xtext_style <- lazyopt::fenge(opt$xtext_style)
ytext_style <- lazyopt::fenge(opt$ytext_style)
xlab_style <- lazyopt::fenge(opt$xlab_style)
ylab_style <- lazyopt::fenge(opt$ylab_style)
if (opt$show_Line) { line <- "longdash" }else { line <- "blank" }
legend_size_type <- lazyopt::fenge(opt$legend_size_type) %>% as.numeric()
imageSize <- lazyopt::fenge(opt$imageSize) %>% as.numeric()
resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()
xrang <- opt$xrang; yrang <- opt$yrang
if (xrang == "NULL") { xrang <- NULL }else { xrang <- lazyopt::fenge(opt$xrang) %>% as.numeric() }
if (yrang == "NULL") { yrang <- NULL }else { yrang <- lazyopt::fenge(opt$yrang) %>% as.numeric() }

p <- opt$tablepath %>%
  read.delim(check.names = FALSE, row.names = 1, header = TRUE) %>%
  EnhancedVolcano(., lab = rownames(.), x = "log2(fc)", y = "FDR", col = lazyopt::fenge(opt$colorstyle),
                  colAlpha = 0.9,
                  gridlines.major = FALSE,
                  gridlines.minor = FALSE,
                  cutoffLineType = line,
                  cutoffLineCol = opt$line_col,
                  pCutoff = opt$pCutoff_Line,
                  FCcutoff = opt$FCcutoff_Line,
                  legendPosition = opt$legend_position,
                  legendLabSize = legend_size_type[2],
                  legendIconSize = legend_size_type[1],
                  selectLab = lazyopt::fenge(opt$labs_show),
                  boxedLabels = TRUE, shape = 1
  )
p <- p +
  theme(axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
        axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
        axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
        axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2])
  ) +
  labs(x = xlab_style[4], y = ylab_style[4]) +
  scale_y_continuous(limits = yrang) +
  scale_x_continuous(limits = xrang)

paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt) %>%
  ggsave(p, width = imageSize[1], height = imageSize[2], dpi = resolution)

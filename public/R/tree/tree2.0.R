pacman::p_load(dplyr, lazyopt, ggplot2, ggtree, magrittr)

arg <- c("-i", "E:/projects/R_draw_sc/yun/tree/usedata/pop.tre.tr",
         "-o", "E:/projects/R_draw_sc/yun/tree/image",
         "-g", "E:/projects/R_draw_sc/yun/tree/usedata/group.txt"
)

opt <- c("treepath", "i", 2, "character", "this is tree path", "",
         "filepackagepath", "o", 2, "character", "this is file path", "",
         "grouppath", "g", 1, "character", "this is group path", "",
         "replace_file", "rf", 1, "character", "this is replace filepath", " ",
         "imageformat", "if", 1, "character", "Image formats include PDF,tiff,PNG", "pdf",
         "imageName", "in", 1, "character", "Pictures of name", "tree",
         "imageSize", "is", 1, "character", "The height and width of the picture", "20:23",
         "colorstyle", "cs", 1, "character", "The parameter used to set the color of the column", "black:#3A89CC:#769C30:#CD0000:#D99536:#7B0078:#BFBC3B:#6E8B3D:#00688B:#C10077:#CAAA76:#474700:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
         "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
         "branchWidth", "bw", 1, "numeric", "Set the branch width", "2",
         "fontSize", "fs", 1, "numeric", "Set font size", "12",
         "fontspace", "ss", 1, "numeric", "The space taken up by the tag in terms of the height of the tree", "1",
         "legend_size_arrange", "lsa", 1, "character", "The length and width of the legend", "2:1",
         "legend_font_size", "lfs", 1, "numeric", "font size of legeng text", "26",
         "legend_ncol", "ln", 1, "numeric", "The number of columns in the legend", "1",
         "show_legend", "sl", 1, "logical", "Whether to show legend", "TRUE",
         "show_support_branchlength", "ssb", 1, "logical", "whether to show support", "FALSE",
         "help", "h", 0, "logical", "help document", "TRUE"
) %>%
  matrix(byrow = TRUE, ncol = 6) %>%
  lazyopt()

imageSize <- opt %$%
  fenge(imageSize) %>%
  as.numeric()

legend_size_arrange <- opt %$%
  fenge(legend_size_arrange)

resolution <- opt %$%
  as.character(resolution) %>%
  match.arg(c("72", "96", "300", "600")) %>%
  as.numeric()

colorstyle <- opt %$%
  fenge(colorstyle)

tree <- opt %$%
  read.delim(treepath, check.names = FALSE) %>%
  names() %>%
  gsub(pattern = " ", replacement = "_", x = .) %>%
  read.tree(text = .)


if (opt$grouppath != "") {
  gg <- opt %$%
    read.delim(grouppath)

  pp <- split((gg[, 1]) %>%
                as.character(), gg[, 2]) %>%
    groupOTU(tree, .)
  pp <- ggtree(pp,
               aes(color = group)
    , size = opt$branchWidth
    , branch.length = "none")

  zk <- levels(pp$data$group)
  zk <- zk[which(zk != "0")]

  pp <- pp +
    geom_tiplab(align = TRUE, size = opt$fontSize, show.legend = F) +
    scale_color_manual(values = colorstyle, breaks = zk) +
    theme(legend.position = "left",
          legend.justification = "top",
          legend.title = element_blank(),
          legend.key.width = unit(as.numeric(legend_size_arrange[1]), "cm"),
          legend.key.height = unit(as.numeric(legend_size_arrange[2]), "cm"),
          legend.text = element_text(size = opt$legend_font_size)
    )
  if (opt$show_legend) {
    pp <- pp + guides(color = guide_legend(ncol = opt$legend_ncol))
  }else {
    pp <- pp + guides(color = F)
  }
  pp <- pp +
    xlim(NA, max(pp$data$x) +
      opt$fontspace)
  if (as.logical(opt$show_support_branchlength)) {
    if (is.null(tree$node.label)) {
      pp <- pp + geom_nodelab(aes(subset = !isTip, label = branch.length),
                              hjust = 1.3, vjust = -0.5, size = 7, show.legend = FALSE)
    }else {
      pp <- pp + geom_nodelab(aes(subset = !isTip, label = label),
                              hjust = 1.3, vjust = -0.5, size = 7, show.legend = FALSE)
    }
  }
}
if (opt$grouppath == "") {
  pp <- ggtree(tree
    , size = opt$branchWidth
    , branch.length = "none"
  ) +
    geom_tiplab(align = TRUE, size = opt$fontSize) +
    scale_color_manual(values = colorstyle) +
    guides(color = F)
  pp <- pp + xlim(NA, max(pp$data$x) + opt$fontspace)
  if (as.logical(opt$show_support_branchlength)) {
    if (is.null(tree$node.label)) {
      pp <- pp + geom_nodelab(aes(subset = !isTip, label = branch.length), hjust = 1.3, vjust = -0.5, size = 7, show.legend = FALSE)
    }else {
      pp <- pp + geom_nodelab(aes(subset = !isTip, label = label), hjust = 1.3, vjust = -0.5, size = 7, show.legend = FALSE)
    }
  }
}
if (opt$replace_file != " ") {
  ins <- read.delim(opt$replace_file)
  dd <- pp$data
  for (i in seq_len(nrow(ins))) {
    dd$label[which(dd$label == ins[i, 1])] <- as.character(ins[i, 2])
  }
  pp$data <- dd
}

paste0(opt$filepackagepath, "/", opt$imageName, ".", opt$imageformat) %>%
  ggsave(pp, width = imageSize[1], height = imageSize[2], dpi = resolution)

pp
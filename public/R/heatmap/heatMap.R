# Title     : TODO
# Objective : TODO
# Created by: Administrator
# Created on: 2020/10/26 0026
pacman::p_load(lazyopt, pheatmap, dplyr, magrittr, ggtree, ape, ggplotify)

#setwd("E:/projects/R_draw_sc/yun/heatmap/data")
#arg <- c("-i", "heatmap.xls",
#         "-o", "E:/projects/R_draw_sc/yun/heatmap/image",
#         "-sn", "TRUE:TRUE:TRUE",
#         "-ari", "heat_rowgroup.xls",
#         "-aci", "heat_colgroup.xls",
#         "-lg", "lg10",
#         "-crw", "TRUE",
#         "-accs", "time@blue:green,group@white:black",
#         "-acrs", "group2@pink:blue"
#)

opt <- matrix(c("tablepath", "i", 2, "character", "Data matrix table path", "",
                "imagepackagepath", "o", 1, "character", "Output the folder package in which the image resides", getwd(),
                "color", "c", 1, "character", "color style split ':',no limit to the number of colors", "#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0",
                "color_counts", "cc", 1, "numeric", "The number of colors generated based on color_tyle", "300",
                "input_nrow", "inr", 1, "character", "the number of rows entered", "all",
                "input_ncol", "inc", 1, "character", "the number of cols entered", "all",
                "log", "lg", 1, "character", "Data log processing,only 'none','lg10','lg2'", "none",
                "scale", "sc", 1, "character", "Data normalization,only row,col,none", "none",
                "annotation_row", "ari", 1, "character", "row grouping file path", " ",
                "annotation_col", "aci", 1, "character", "col grouping file path", " ",
                "treerowfile", "trf", 1, "character", "row tree file path", " ",
                "treecolfile", "tcf", 1, "character", "col tree file path", " ",
                "labfilepath", "lfi", 1, "character", "Custom grid label file", " ",
                "checked_lab_names", "cln", 1, "logical", "wheather to checked file col and row names", "TRUE",
                "cluster_row", "crw", 1, "character", "wheather to cluster row", "FALSE",
                "cluster_col", "ccl", 1, "character", "wheather to cluster col", "FALSE",
                "cluster_row_method", "crm", 1, "character", "ward,single,complete,average,mcquitty,median,centroid", "complete",
                "cluster_col_method", "ccm", 1, "character", "ward,single,complete,average,mcquitty,median,centroid", "complete",
                "cellbord_color", "cbc", 1, "character", "set cellbord color,'none'mean No border ", "white",
                "lab_number_color", "lnc", 1, "character", "color of lab", "black",
                "treeheigh", "th", 1, "character", "set tree height col and row ,split by ':'", "50:50",
                "showxyc_names", "sn", 1, "character", "Whether to display the XY axis and data text", "TRUE:TRUE:FALSE",
                "xyfont_size", "fs", 1, "character", "xyfront size", "10:10",
                "show_map_type", "smt", 1, "character", "left,right,top,bottom,topTriangle,bottomTriangle,full", "full",
                "row_parts", "rp", 1, "numeric", "Row clustering is divided into several parts", "1",
                "col_parts", "cp", 1, "numeric", "The cluster is divided into several parts", "1",
                "fontsize_number", "fn", 1, "numeric", "number font size", "15",
                "main", "m", 1, "character", "title of image", " ",
                "na_colour", "nc", 1, "character", "color of NA", "#DDDDDD",
                "labels_row", "lr", 1, "logical", "Whether to display the row name", "TRUE",
                "labels_col", "lc", 1, "logical", "Whether to display the col name", "TRUE",
                "show_annotataion_names", "san", 1, "character", "Whether to display col row names", "TRUE:TRUE",
                "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
                "x_font_angle", "xfa", 1, "numeric", "The rotation Angle of the X-axis font,270,0,45,90,315", "90",
                "annotation_cow_colors", "accs", 1, "character", "set annotation color split by [, @ :]", " ",
                "annotation_row_colors", "acrs", 1, "character", "set annotation color split by [, @ :]", " ",
                "imageName", "in", 1, "character", "picture name", "heatmap",
                "help", "h", 0, "logical", "help document", "TRUE"
), byrow = TRUE, ncol = 6) %>% lazyopt(arg = NULL)

topTriangle <- function(data) {
  data %<>% as.data.frame()
  data[, ncol(data) + 1] <- NA
  data[nrow(data) + 1,] <- NA
  for (k in 1:min(ncol(data), nrow(data))) {
    data[(k + 1):nrow(data), k] <- NA
  }
  data <- data[1:(nrow(data) - 1), 1:(ncol(data) - 1)]
  return(data %>% as.matrix())
}

bottomTriangle <- function(data) {
  for (k in 2:min(ncol(data), nrow(data))) {
    data[1:(k - 1), k] <- NA
  }
  return(data)
}

left <- function(data) {
  mid <- round(((ncol(data) / 2) + 0.1))
  data[, (mid + 1):ncol(data)] <- NA
  return(data)
}

right <- function(data) {
  mid <- round(((ncol(data) / 2) + 0.1))
  data[, 1:(mid - 1)] <- NA
  return(data)
}

top <- function(data) {
  mid <- round(((nrow(data) / 2) + 0.1))
  data[(mid + 1):nrow(data),] <- NA
  return(data)
}

bottom <- function(data) {
  mid <- round(((nrow(data) / 2) + 0.1))
  data[1:(mid - 1),] <- NA
  return(data)
}

changetype <- function(data, str) {
  if (str == "left") { data <- left(data) }
  if (str == "right") { data <- right(data) }
  if (str == "top") { data <- top(data) }
  if (str == "bottom") { data <- bottom(data) }
  if (str == "topTriangle") { data <- topTriangle(data) }
  if (str == "bottomTriangle") { data <- bottomTriangle(data) }
  return(data)
}

data <- read.delim(opt$tablepath, row.names = 1, check.names = FALSE)


if (opt$input_nrow != "all") {
  inputrow <- lazyopt::fenge(opt$input_nrow, ",")
  nr <- NULL
  for (i in seq_along(inputrow)) {
    if (length(strsplit(inputrow[i], "-")[[1]]) == 1) {
      nr[length(nr) + 1] <- lazyopt::fenge(inputrow[i], "-") %>% as.numeric()
    }else {
      z <- lazyopt::fenge(inputrow[i], "-") %>% as.numeric()
      nr[length(nr) + 1:(length(nr) + 1 + z[2] - z[1])] <- z[1]:z[2]
    }
  }
  data <- data[nr %>% unique(),]
}
if (opt$input_ncol != "all") {
  input_ncol <- lazyopt::fenge(opt$input_ncol, ",")
  nr <- NULL
  for (i in seq_along(input_ncol)) {
    if (length(strsplit(input_ncol[i], "-")[[1]]) == 1) {
      nr[length(nr) + 1] <- lazyopt::fenge(input_ncol[i], "-") %>% as.numeric()
    }else {
      z <- lazyopt::fenge(input_ncol[i], "-") %>% as.numeric()
      nr[length(nr) + 1:(length(nr) + 1 + z[2] - z[1])] <- z[1]:z[2]
    }
  }
  data <- data[, nr %>% unique()] }
if (opt$log == "lg2") { data <- log2(data + 1) }
if (opt$log == "lg10") { data <- log10(data + 1) }
if (opt$scale == "row") { data <- t(scale(t(data))); data[is.nan(data)] <- NA }
if (opt$scale == "col") { data <- scale(data); data[is.nan(data)] <- NA }


if (opt$treecolfile == " ") {
  cluster_cols <- opt$cluster_col %>% as.logical()
  if (cluster_cols) {
    data %>%
      t() %>%
      dist() %>%
      hclust(method = opt$cluster_col_method) %>%
      as.phylo.hclust() %T>%
      write.tree(file = paste0(opt$imagepackagepath, "/col.tre"), .)
    cluster_cols <- paste0(opt$imagepackagepath, "/col.tre") %>%
      read.tree() %>%
      as.hclust()
    data <- data[, ggtree(cluster_cols)$data %>% arrange(y) %$% unique(label) %>%
                     na.omit() %>%
                     as.character() %>%
                     match(data %>% colnames())]
  }
}else {
  cluster_cols <- opt$treecolfile %>% read.tree() %>% as.hclust()
  data <- data[, cluster_cols %>% ggtree() %$%
                   arrange(data, y) %$%
                   unique(label) %>%
                   na.omit() %>%
                   as.character() %>%
                   match(data %>% colnames())]
}


if (opt$treerowfile == " ") {
  cluster_rows <- opt$cluster_row %>% as.logical()
  if (cluster_rows) {
    data %>%
      dist() %>%
      hclust(method = opt$cluster_row_method) %>%
      as.phylo.hclust() %>%
      write.tree(file = paste0(opt$imagepackagepath, "/row.tre"), .)

    cluster_rows <- paste0(opt$imagepackagepath, "/row.tre") %>%
      read.tree() %>%
      as.hclust()
    data <- data[cluster_rows %>% ggtree() %$%
                   arrange(data, y) %$%
                   unique(label) %>%
                   na.omit() %>%
                   as.character() %>%
                   match(data %>% rownames()),]
  }
}else {
  cluster_rows <- opt$treerowfile %>% read.tree() %>% as.hclust()
  data <- data[cluster_rows %$% match(labels, data %>% rownames()),]
}
annotation1 <- NA
if (opt$annotation_row != " ") {
  annotation1 <- read.delim(opt$annotation_row, row.names = 1, check.names = FALSE)
}
annotation2 <- NA
if (opt$annotation_col != " ") {
  annotation2 <- read.delim(opt$annotation_col, row.names = 1, check.names = FALSE)
}


showxyc_names <- lazyopt::fenge(opt$showxyc_names)
xyfont_size <- lazyopt::fenge(opt$xyfont_size)
main <- opt$main
if (main != " ") { main <- opt$main }else { main <- NA }
if (as.logical(opt$labels_row)) { labels_row <- NULL }else { labels_row <- rep("", nrow(data)) }
if (as.logical(opt$labels_col)) { labels_col <- NULL }else { labels_col <- rep("", ncol(data)) }
show_annotataion_names <- lazyopt::fenge(opt$show_annotataion_names)
treeheigh <- lazyopt::fenge(opt$treeheigh)
display <- as.logical(showxyc_names[3])
if (opt$labfilepath != " ") {
  display <- opt$labfilepath %>% read.delim(row.names = 1, check.names = FALSE)
  if (opt$checked_lab_names %>% as.logical()) {
    datac <- rownames(data)
    displayc <- rownames(display)
    display <- display[match(datac, displayc),]
    datac <- colnames(data)
    displayc <- colnames(display)
    display <- display[, match(datac, displayc)]
  }
}
type <- opt$show_map_type
if (class(display) != "logical") {
  display <- changetype(display, opt$show_map_type)
  display[is.na(display)] <- ""
}
if (opt$show_map_type != "full") {
  data <- changetype(data, opt$show_map_type)
}
if (opt$cellbord_color == "none") {
  opt$cellbord_color <- NA
}

annco <- function(ano, ch) {
  if (is.na(ano) || is.na(ch)) { return(NA) }else {
    names <- c()
    result <- list()
    a1 <- fenge(ch, ",")
    z <- seq_along(a1)
    for (i in z) {
      a2 <- fenge(a1[i], "@")
      a21 <- a2[1]
      ll <- which(colnames(ano) == a21)
      if (class(ano[, ll]) == "integer" || class(ano[, ll]) == "numeric") {
        result[[i]] <- colorRampPalette(fenge(a2[2]))(
          length(unique(ano[, ll]))
        )
        names(result[[i]]) <- unique(ano[, ll])
        names[i] <- a21
      }else {
        result[[i]] <- colorRampPalette(fenge(a2[2]))(
          length(unique(ano[, ll]))
        )
        names(result[[i]]) <- unique(ano[, ll])
        names[i] <- a21
      }
    }
    names(result) <- names
    return(result)
  }
}

hebinglist <- function(d1, d2) {
  if (is.na(d1)) { return(d2) }
  if (is.na(d2)) { return(d1) }
  if (!is.na(d2) && !is.na(d1)) {
    result <- list()
    names <- c(names(d1), names(d2))

    for (i in seq_along(d1)) {
      result[[i]] <- d1[[i]]
    }
    ll <- length(result)
    for (i in seq_along(d2)) {
      result[[ll + i]] <- d2[[i]]
    }
    names(result) <- names
    return(result)
  }
}

addcoloropt <- function(annotation, colstr) {
  if (is.na(annotation)) { return(" ") }else {

    cn <- colnames(annotation);
    if (colstr == " ") {
      result <- ""
      for (i in seq_along(cn)) {
        result <- paste0(result, cn[i], "@", "red:", "blue", ",")
      }
      result <- substr(result, 1, nchar(result) - 1)
      return(result)
    }else {
      colstr <- fenge(colstr, ",")
      name <- c()
      for (i in seq_along(colstr)) {
        name[i] <- fenge(colstr[i], "@")[1]
      }
      names(colstr) <- name

      if (length(cn) != length(colstr)) {
        for (i in which(cn != name)) {
          colstr[length(colstr) + 1] <- paste0(cn[i], "@", "red:", "blue")
        }
      }
      return(paste(colstr, collapse = ","))
    }


  }
}

annotation_colors <- list()


if (!is.na(annotation1) || !is.na(annotation2)) {
  o1 <- addcoloropt(annotation1, opt$annotation_row_colors)
  o2 <- addcoloropt(annotation2, opt$annotation_cow_colors)

  lis1 <- annco(annotation1, o1)
  lis2 <- annco(annotation2, o2)
  lis3 <- hebinglist(lis1, lis2)
  annotation_colors <- lis3
}else {
  annotation_colors <- NA
}


#annotation_colors <- list(
#  group2 = c(a = "pink", b = "yellow", c = "purple"),
#  time = c("red", "yellow"),
#  group = c(a = "red", b = "blue", c = "black")
#)

pp <- pheatmap(data
  , cluster_rows = cluster_rows
  , treeheight_row = as.numeric(treeheigh[1])
  , cluster_cols = cluster_cols
  , treeheight_col = as.numeric(treeheigh[2])
  , border_color = opt$cellbord_color
  , display_numbers = display
  , show_colnames = as.logical(showxyc_names[1])
  , show_rownames = as.logical(showxyc_names[2])
  , color = rev(colorRampPalette(lazyopt::fenge(opt$color))(opt$color_counts))
  , fontsize_row = as.numeric(xyfont_size[1])
  , fontsize_col = as.numeric(xyfont_size[2])
  , cutree_rows = opt$row_parts
  , cutree_cols = opt$col_parts
  , fontsize_number = opt$fontsize_number
  , annotation_col = annotation2
  , annotation_row = annotation1
  , number_color = opt$lab_number_color
  , main = main
  , na_col = opt$na_colour
  , labels_row = labels_row
  , labels_col = labels_col
  , annotation_names_col = as.logical(show_annotataion_names[2])
  , annotation_names_row = as.logical(show_annotataion_names[1])
  , angle_col = opt$x_font_angle
  , annotation_colors = annotation_colors
  , filename = paste0(opt$imagepackagepath, "/", opt$imageName, ".", opt$imageformt)
)


if (!is.na(annotation_colors)) {
  wo1 <- c(); wo2 <- c(); value <- c()
  for (i in seq_along(annotation_colors)) {
    wo1 <- c(wo1, names(annotation_colors[[i]]))
    wo2 <- c(wo2, rep(names(annotation_colors)[i], length(annotation_colors[[i]])))
    value <- c(value, annotation_colors[[i]])
  }
  data.frame(groupname = wo2, onegroup_name = wo1, value = value) %>% write.table(
    paste0(opt$imagepackagepath, "/", "color_zhushi.xls"), sep = "\t", col.names = NA)
}
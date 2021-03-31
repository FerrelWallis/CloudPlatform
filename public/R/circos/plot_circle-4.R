pacman::p_load(circlize, lazyopt, dplyr, magrittr, RColorBrewer, ComplexHeatmap)

add_highlight <- function(index, track, col) {
  #开始位置start.degree,结束位置end.degree 顺时针旋�?,区域颜色col  扇形线边框样式lty
  st <- get.cell.meta.data("cell.start.degree", sector.index = index)
  end <- get.cell.meta.data("cell.end.degree", sector.index = index)
  rou2 <- get.cell.meta.data("cell.bottom.radius", track.index = track)
  rou1 <- get.cell.meta.data("cell.top.radius", track.index = track)
  draw.sector(st, end, rou1 = rou1, rou2 = rou2, col = col, lty = 0)
}

add_hlights <- function(areadata) {
  #  chr2@1-2@#984EA3;chr7@2@#984EA3
  lists <- lazyopt::fenge(areadata, ";")
  for (i in seq_along(lists)) {
    l1 <- lazyopt::fenge(lists[i], "@")
    tracks <- l1[2] %>% lazyopt::fenge("-") %>% as.numeric()
    if (tracks[2] %>% is.na()) { tracks[2] <- tracks[1] }
    for (k in (tracks[1]:tracks[2])) {
      add_highlight(l1[1], k, col = paste0(l1[3], "80"))
    }
  }
}

otherlegend <- function(yu, length = 4, title = "") {
  dd <- colorRampPalette(yu[[2]])(yu[[1]])
  return(Legend(at = c(-2, 0, 2), labels_gp = gpar(fontsize = 0), col_fun = colorRamp2(seq(-2, 2, length.out = length(dd)), dd), direction = 'horizontal',
                border = NA, legend_width = unit(length, 'cm'), title = title, title_position = 'topleft'))
}

drawlineormap <- function(x, y, dt, type, col) {

  ntocol <- function(y, col) {
    yu <- unique(y)
    lly <- length(yu)
    cols <- colorRampPalette(col, alpha = 0.5)(lly)
    result <- y
    for (i in 1:lly) {
      a1 <- sort(yu)
      result[which(y == a1[i])] <- cols[i]
    }
    return(result)
  }

  ymax <- CELL_META$cell.ylim[2] * 0.9
  if (type == "h") {
    cc <- ntocol(y, col = col)
    circos.rect(x - dt, 0, x + dt, ymax, col = cc, lwd = 0.4, border = NA) }
  if (type == "l") {
    cc <- ntocol(y, col = col)
    circos.rect(x - dt, 0, x + dt, y, col = cc, lwd = 0.4, border = NA)
  }
}

needaxis <- function(a, n = 5) {
  # fi <- a / 1000000000
  # result <- c()
  # if (fi >= 10) {
  #   k1 <- ((a / 1000000000) / n) %>% round()
  #   while ((k1 %% 5) != 0) {
  #     k1 <- k1 + 1
  #   }
  #   k2 <- k1 * 1000000000
  #   result[1] <- k1; result[2] <- k2; result[3] <- 1000000000
  # }else {
  #   k1 <- ((a / 1000000) / n) %>% round()
  #   while ((k1 %% 5) != 0) {
  #     k1 <- k1 + 1
  #   }
  #   k2 <- k1 * 1000000
  #   result[1] <- k1; result[2] <- k2; result[3] <- 1000000
  # }
  result <- c()
  if (a >= 10^9) {
    k1 <- ((a / 10^9) / n) %>% round()
    while ((k1 %% 5) != 0) {
      k1 <- k1 + 1
    }
    k2 <- k1 * 10^9
    result[1] <- k1; result[2] <- k2; result[3] <- 10^9
  }else if(a >= 10^6) {
    k1 <- ((a / 10^6) / n) %>% round()
    while ((k1 %% 5) != 0) {
      k1 <- k1 + 1
    }
    k2 <- k1 * 10^6
    result[1] <- k1; result[2] <- k2; result[3] <- 10^6
  }else if(a >= 10^3) {
    k1 <- ((a / 10^3) / n) %>% round()
    while ((k1 %% 5) != 0) {
      k1 <- k1 + 1
    }
    k2 <- k1 * 10^3
    result[1] <- k1; result[2] <- k2; result[3] <- 10^3
  }else {
    k1 <- ((a / 1) / n) %>% round()
    while ((k1 %% 5) != 0) {
      k1 <- k1 + 1
    }
    k2 <- k1 * 1
    result[1] <- k1; result[2] <- k2; result[3] <- 1
  }
  return(result)
}

firsttrack <- function(data, trackheight = 0.1, bgcol = NULL, label = NULL,
                       bgborder = "black", textcex = par("cex"),
                       textcol = par("col"), labcex = par("cex"),
                       labcol = par("col"), bglty = par("lty"), textfont = 2,
                       textposition = "center", showlabel = TRUE,
                       kedulwd = 1, kedusize = 0.7, keducol = "black") {
  ff <- rep(as.character(data[, 1]), 2); fx <- c(as.numeric(data[, 2]), rep(0, nrow(data)))

  result <- needaxis(max(data$length), 4)

  if (!is.null(bgcol)) {
    col <- bgcol
  }else {
    col <- as.character(data[, 3])
  }
  circos.initialize(factors = ff, x = fx)
  circos.track(ylim = c(0, 1), bg.col = col, track.height = trackheight, bg.lty = bglty, bg.border = bgborder,
               panel.fun = function(x, y) {
                 if (textposition == "outside") {
                   circos.text(CELL_META$xcenter, CELL_META$ylim[2] + mm_y(10), CELL_META$sector.index, niceFacing = TRUE,
                               cex = textcex, col = textcol, font = textfont)
                 }
                 if (textposition == "center") {
                   circos.text(CELL_META$xcenter, CELL_META$ycenter, CELL_META$sector.index, niceFacing = TRUE,
                               cex = textcex, col = textcol, font = textfont)
                 }
                 if (showlabel) {
                   if (CELL_META$sector.index == get.all.sector.index()[1]) {
                     circos.text(CELL_META$xlim[1] - mm_x(7), CELL_META$ycenter
                       , labels = label, niceFacing = TRUE, cex = labcex, col = labcol) }
                 }
                 g <- CELL_META$xlim[2]
                 circos.xaxis(major.at = seq(0, g, result[2]), minor.ticks = 4, labels.cex = kedusize,
                              labels = seq(0, g / result[3], result[1]), col = keducol, lwd = kedulwd)

               }
  )
}

othertrack <- function(data, trackheight = 0.1, bgcol = "white", bglty = par("lty"), bgborder = "black",
                       labcex = par("cex"), labcol = par("col"), label = NULL,
                       type = "h", col = c("#E41A1C", "#1E90FF", "#FF8C00", "#4DAF4A", "#984EA3"), showlabel = TRUE)
{
  ff <- data[, 1]; fx <- (data[, 2] + data[, 3]) / 2; fy <- data[, 4]; dt <- data[1, 2] + data[1, 3]

  circos.track(factors = ff, x = fx, y = fy,
               bg.col = bgcol, track.height = trackheight, bg.lty = bglty, bg.border = bgborder,
               panel.fun = function(x, y) {
                 drawlineormap(x, y, dt, type, col = col)
                 if (showlabel) {
                   if (CELL_META$sector.index == get.all.sector.index()[1]) {
                     circos.text(CELL_META$xlim[1] - mm_x(7), CELL_META$ycenter
                       , labels = label, niceFacing = TRUE,
                                 cex = labcex, col = labcol) }
                 }
               }
  )

  return(list(length(unique(fy)), col))
}

linkline <- function(colline, lwd = par("lwd"), lty = par("lty"), h = 0.5) {
  for (i in seq_len(nrow(colline))) {
    circos.link(as.character(colline[i, 1]), c(as.numeric(colline[i, 2]), as.numeric(colline[i, 3])),
                as.character(colline[i, 4]), c(as.numeric(colline[i, 5]), as.numeric(colline[i, 6])),
                h = h, col = as.character(colline[i, 7]),
                lwd = lwd, lty = lty
    )
  }
}

plotc <- function(opt) {
  kedu_style <- opt$kedu_style %>% lazyopt::fenge()
  first_track <- lazyopt::fenge(opt$first_track, "@")
  circle_base_set <- opt$circle_base_set %>% lazyopt::fenge(":")
  gap <- as.numeric(lazyopt::fenge(circle_base_set[1], ","))

  if (opt$show_track_label && (length(gap) == 1)) {
    gap <- first_track[1] %>%
      read.delim() %>%
      nrow() %>%
      rep(gap, .) %>%
      as.numeric()
    if (gap[length(gap)] < 60) { gap[length(gap)] <- 60 }
  }

  circos.par("points.overflow.warning" = FALSE,
             "gap.degree" = gap,
             "clock.wise" = TRUE,
             "track.margin" = c(0, 0),
             cell.padding = c(0.02, 0, 0.02, 0)
    , start.degree = (circle_base_set[2]) %>% as.numeric())
  dataf <- read.delim(first_track[1])
  if (is.na(first_track[4]) || (first_track[4] == "NULL")) { firstbgcol <- NULL }else { firstbgcol <- colorRampPalette(lazyopt::fenge(first_track[4], ","))(dataf %$% unique(Chr) %>% length()) }
  if (is.na(first_track[2]) || (first_track[2] == "NULL")) { textcol <- "black" }else { textcol <- first_track[2] }
  if (is.na(first_track[5]) || (first_track[5] == "NULL")) { textposition <- "center" }else { textposition <- first_track[5] }
  if (is.na(first_track[3]) || (first_track[3] == "NULL")) { firsttrackheight <- 0.1 }else { firsttrackheight <- as.numeric(first_track[3]) }
  if (is.na(first_track[6]) || (first_track[6] == "NULL")) { textcex <- 1 }else { textcex <- as.numeric(first_track[6]) }
  if (ncol(dataf) == 2) {
    dataf$color <- colorRampPalette(brewer.pal(8, "Set3"))(nrow(dataf))
  }
  firsttrack(dataf, strsplit(basename(first_track[1]), ".", TRUE)[[1]][1], trackheight = firsttrackheight,
             bgcol = firstbgcol, textcol = textcol, textposition = textposition, showlabel = opt$show_track_label, textcex = textcex,
             keducol = kedu_style[3] %>% as.character(), kedulwd = kedu_style[1] %>% as.numeric(), bglty = opt %$% as.numeric(bg_lty),
             kedusize = kedu_style[2] %>% as.numeric()
  )

  if (opt$other_track != " ") {
    legendlabels <- NULL
    legendsdata <- list()
    other_track <- lazyopt::fenge(opt$other_track, ";")
    for (i in seq_along(other_track)) {
      usedata <- lazyopt::fenge(other_track[i], "@")
      if ((is.na(usedata[3]) || (usedata[3] == "NULL")))
      {
        dd <- read.delim(usedata[1])
        if (ncol(dd) == 4) {
          col <- colorRampPalette(brewer.pal(2, "Set3"))(nrow(dataf))

        }else {
          col <- as.character(dd[1, 6]) %>% lazyopt::fenge("-")
        }
      }else {
      { col <- lazyopt::fenge(usedata[3], ",") }
      }
      if ((is.na(usedata[2]) || (usedata[2] == "NULL"))) { othertrackheight <- 0.1 }else { othertrackheight <- as.numeric(usedata[2]) }
      if ((is.na(usedata[4]) || (usedata[4] == "NULL"))) { othetype <- "l" }else { othetype <- usedata[4] }
      legendlabels[i] <- strsplit(basename(usedata[1]), ".", TRUE)[[1]][1]
      legendsdata[[i]] <- othertrack(read.delim(usedata[1]), label = strsplit(basename(usedata[1]), ".", TRUE)[[1]][1]
        , trackheight = othertrackheight, col = col, type = othetype, bgborder = "black", showlabel = opt$show_track_label, bglty = opt %$% as.numeric(bg_lty))

    }
  }
  get.all.sector.index() %>%
    as.data.frame() %>%
    set_colnames("sector_index") %>%
    write.table(., file = paste0(opt$filepath, "/", "circle_data.xls"), sep = "\t", row.names = FALSE)
  if (opt$hilight_area != "NULL") {
    add_hlights(opt$hilight_area)
  }
  if (opt$colline_data != " ") {
    linkline(read.delim(opt$colline_data), h = opt$colline_height)
  }
  if (opt$showlegend && opt$other_track != " ") {
    legendresult <- list()
    for (i in seq_along(legendsdata)) {

      legendresult[[i]] <- otherlegend(legendsdata[[i]], length = opt$legend_type_length, title = legendlabels[i])

      if (i == 1) { ld <- packLegend(legendresult[[i]]) }else {
        ld <- packLegend(ld, legendresult[[i]])
      }
    }
    grid.draw.Legends(ld)
  }
  circos.clear()
}

plotci_1 <- function() {
  arg <- c("-ft", "F:/CloudPlatform/R/circos/test/run1.txt", 
           "-o", "F:/CloudPlatform/R/circos/test", 
           "-cd", "F:/CloudPlatform/R/circos/test/run1_colinear.txt",
           "-in","cc4"
  )
  opt <- matrix(c(
    "first_track", "ft", 2, "character", "first track,split by'@' datapath@labelcol@trackheight@bgcol@labelposition@textcex", "",
    "other_track", "ot", 1, "character", "other tracks split by '@'and';' datapath@trackheight@col@type;.....", " ",
    "parameter_specification", "ps", 1, "character", "trackcol is split by ',' ,it can be one color or colors,type only 'l'or 'h'", "",
    "colline_data", "cd", 1, "character", "colline data path", " ",
    "showlegend", "sl", 1, "logical", "whether to show legend of othertracks", "FALSE",
    "kedu", "k", 1, "logical", "whether to show axis,if TRUE,gap automatically goes to 0", "FALSE",
    "kedu_style", "ks", 1, "character", "set kedu style,split by ':' sample lwd:fontsize:col", "1:0.7:black",
    "show_track_label", "stl", 1, "logical", "whether to show each track label", "FALSE",
    "legend_type_length", "ltl", 1, "numeric", "legend type length", "4",
    "colline_height", "ch", 1, "numeric", "collineline length", "0.5",
    "circle_base_set", "cbs", 1, "character", "sample gap:degree  gap can be split by ','", "2:90",
    "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
    "imageName", "in", 1, "character", "picture name", "circle",
    "filepath", "o", 2, "character", "this is file packagepath", getwd(),
    "bg_lty", "blt", 1, "character", "set circle line style ", "1",
    "hilight_area", "ha", 1, "character", "Set the highlight area,split by [; @  -]", "NULL",
    "help", "h", 0, "logical", "help document", "TRUE"
  ), byrow = TRUE, ncol = 6) %>% lazyopt()
  savepicture(plotc(opt), opt$imageformt, paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt))
}
plotci_1()

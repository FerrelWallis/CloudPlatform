pacman::p_load(ggplot2, dplyr, lazyopt, magrittr)


arg <- c("-i", "E:/R_script_lingen/.idea/com/goko/data/da1.txt",

         "-o", "E:/R_script_lingen/.idea/com/goko/image"
)
opt <- c("barwidth", "br", 1, "numeric", "Width of column", "0.9",
         "ymax", "yr", 1, "character", "The Y-axis", "ymax",
         "group", "g", 1, "logical", "group by up down", "FALSE",
         "nums", "n", 1, "numeric", "Sets the number of rows to be read from top", "-1",
         "stringMax", "sm", 1, "numeric", "Sets the maximum length of the Y-axis string", "50",
         "colorstyle", "cs", 1, "character", "Set the color to ':' split", "#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF",
         "tablepath", "i", 2, "character", "The path to the table data read", "",
         "filepath", "o", 2, "character", "The package path of the output image", "",
         "imageSize", "is", 1, "character", "The height and width of the picture", "20:14",
         "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:13",
         "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:14",
         "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name", "sans:bold.italic:12: ",
         "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name", "sans:bold.italic:12: ",
         "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:12:Gene number",
         "legendtext_style", "lts", 1, "character", "Legend text style Font:font type:font size", "sans:bold.italic:15",
         "legendtitle_style", "lms", 1, "character", "Legend title style Font:font type:font size:name", "sans:bold.italic:15:down    up",
         "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
         "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
         "imageName", "in", 1, "character", "picture name", "Ko",
         "help", "h", 0, "logical", "Help document", "TRUE"
) %>%
  matrix(byrow = TRUE, ncol = 6) %>%
  lazyopt(arg = NULL)


legendtitle_style <- opt %$% fenge(legendtitle_style)


filepath <- opt$filepath

number <- opt$nums

main_style <- opt %$% fenge(main_style)
xtext_style <- opt %$% fenge(xtext_style)
ytext_style <- opt %$% fenge(ytext_style)
xlab_style <- opt %$% fenge(xlab_style)
ylab_style <- opt %$% fenge(ylab_style)

legendtext_style <- opt %$% fenge(legendtext_style)
legendtitle_style <- opt %$% fenge(legendtitle_style)
imageSize <- opt %$% fenge(imageSize) %>% as.numeric()
colorstyle <- opt %$% fenge(colorstyle)

strmax <- opt$stringMax


resolution <- opt %$% as.character(resolution) %>%
  match.arg(c("72", "96", "300", "600")) %>%
  as.numeric()


data <- read.delim(opt$tablepath, check.names = FALSE, header = TRUE); nc <- ncol(data)


if (nc == 7) {
  if (number > nrow(data) || number < 0) { number <- nrow(data) }
  needindex <- 1:number
  data <- data[needindex,] %>%
    select(type = 2, name = 3, 5, 6, pvalue = 7, 4) %>%
    arrange(type) %>%
    mutate(star = case_when(pvalue <= 0.001 ~ "      ***", pvalue > 0.001 & pvalue <= 0.01 ~ "      **", pvalue > 0.01 & pvalue <= 0.05 ~ "      *")) %>%
    select(1, 2, 3, 4, 6, 7)


  y <- c(as.numeric(data[1:number, 3]), as.numeric(data[1:number, 4]))
  updown <- c(rep("up", number), rep("down", number))
  data <- rbind(data, data) %>%
    mutate(y = y) %>%
    select(1, 2, 5, 6, 7) %>%
    mutate(updown = updown)

  x <- as.character(data$name)
  for (i in 1:length(x)) {
    if (nchar(x[i]) > strmax) {
      x[i] <- paste(substr(x[i], 1, (strmax - 3)), "...", sep = "")
    }
  }
  data[, 2] <- x
  data[, 2] <- factor(data[, 2], levels = unique(data[, 2][1:number]))
  type <- as.character(data[, 1])
  for (i in (number + 1):(2 * number)) {
    type[i] <- paste(type[i], "down", sep = " ")
  }
  data[, 1] <- type
  un <- unique(data[, 1]); legend_lab <- c()
  legend_lab[seq(1, length(un), 2)] <- un[1:(length(un) / 2)]
  legend_lab[seq(2, length(un), 2)] <- ""
  if (opt$ymax == "ymax") {
    ymax <- max(data$y) * 1.3
  }else {
    ymax <- as.numeric(opt$ymax)
  }


  pp <- ggplot(data) +
    theme_bw() +
    geom_col(aes(x = name, y = y, fill = type), position = "dodge2", width = opt$barwidth) +
    scale_fill_manual(values = colorstyle, labels = legend_lab, name = legendtitle_style[4]) +
    guides(fill = guide_legend(reverse = T, ncol = 2, byrow = TRUE,)) +
    labs(title = main_style[4], y = ylab_style[4], x = xlab_style[4]) +
    theme(
      axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
      axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
      legend.justification = c("right", "top"),
      legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
      legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
      title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
      axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
      axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2])
    ) +
    scale_y_continuous(guide = guide_axis(position = "top"), limits = c(0, ymax)) +
    theme(axis.line.x = element_line(arrow = arrow(length = unit(0, 'cm'), type = "open")),
          plot.title = element_text(hjust = 0.5)
    ) +
    coord_flip()
  if (opt$group) {
    pp <- pp + facet_grid(. ~ updown, scales = "free",)
    pp <- pp + theme(
      strip.background = element_rect(
        color = "black", fill = "#FFB5BA", size = 0),
      strip.text = element_text(color = "black", size = 12, family = "sans", face = "bold.italic"),
      #panel.border = element_blank(),
      panel.grid.major = element_blank(),
      panel.grid.minor = element_blank(),
      legend.background = element_blank()
    )
  }else {
    pp <- pp + theme(panel.border = element_blank(),
                     panel.grid.major = element_blank(),
                     panel.grid.minor = element_blank(),
                     legend.background = element_blank()
    )
  }
  opt %$% paste0(filepath, "/", imageName, ".", imageformt) %>%
    ggsave(pp, width = imageSize[1], height = imageSize[2], dpi = resolution)

  ll <- length(unique(pp$data$type))

  data.frame(name = unique(pp$data$type)[1:(length(unique(pp$data$type)) / 2)], color = colorstyle[seq(1, ll, 2)]) %>% write.table(
    file = paste0(filepath, "/", "color.xls"), sep = "\t", row.names = F
  )
}


if (nc == 5) {
  if (number > nrow(data) || number < 0) { number <- nrow(data) }
  cc <- 1:number
  data <- data[cc,]
  colorstyle <- colorstyle[seq(1, length(colorstyle), 2)]

  x <- as.character(data$Description)
  for (i in 1:length(x)) {
    if (nchar(x[i]) > strmax) {
      x[i] <- paste(substr(x[i], 1, (strmax - 3)), "...", sep = "")
    }
  }
  data$Description <- x

  data <- data %>% arrange(Category) %>% select(3, 2, 4, 5)

  data[, 1] <- factor(data[, 1], levels = unique(data[, 1]))
  if (opt$ymax == "ymax") {
    ymax <- max(data$DEG) * 1.3
  }else {
    ymax <- as.numeric(opt$ymax)
  }


  pp <- ggplot(data) +
    geom_col(aes(x = Description, y = DEG, fill = Category)) +
    theme_bw() +
    scale_fill_manual(values = colorstyle, name = "") +
    guides(fill = guide_legend(reverse = T, ncol = 1, byrow = TRUE,)) +
    labs(title = main_style[4], y = ylab_style[4], x = xlab_style[4]) +
    theme(

      axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),

      axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
      legend.justification = c("right", "top"),

      legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),

      legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),

      title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),

      axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),

      axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2])
    ) +
    scale_y_continuous(guide = guide_axis(position = "top"), limits = c(0, ymax)) +
    theme(axis.line.x = element_line(arrow = arrow(length = unit(0, 'cm'), type = "open")),
          plot.title = element_text(hjust = 0.5)
    ) +
    coord_flip()

  pp <- pp + theme(panel.border = element_blank(),
                   panel.grid.major = element_blank(),
                   panel.grid.minor = element_blank(),
                   legend.background = element_blank())

  opt %$% paste0(filepath, "/", imageName, ".", imageformt) %>%
    ggsave(pp, width = imageSize[1], height = imageSize[2], dpi = resolution)
  data.frame(name = unique(pp$data$Category), color = colorstyle[1:length(unique(pp$data$Category))]) %>% write.table(
    file = paste0(filepath, "/", "color.xls"), sep = "\t", row.names = F
  )
}









pacman::p_load(lazyopt, dplyr, magrittr, stringr, ggplot2)

arg <-  c("-i", "F:/CloudPlatform/R/bubble/bubble.Go.enrich.txt",
          "-o", "F:/CloudPlatform/R/bubble",
          "-dt", "go"
)

godata <- function(data) {
  return (unlist(strsplit(data, "/", fixed = T))[1] %>% as.numeric())
}

description <- function(des, len) {
  if(str_length(des) > len) {
    return (str_c(str_sub(des, 0, len), "..."))
  } else return (des %>% as.character())
}

pp <- function(opt) {
  data <- read.delim(opt$inputfile, sep="\t", head=T, check.names=F) %>% .[1:opt$top,]
  if(opt$datatype == "go") {
    data <- data %>% select(c(3,8,4))
    data[,3] <- lapply(data[,3] %>% as.character(), function(x) godata(x)) %>% unlist()
  } else if(opt$datatype == "kegg") {
    data <- data %>% select(c(3,1,5))
  }
  write.table(data, paste0(opt$filepath, "/standard_table.txt"), sep="\t", row.names = F, quote = F)
  
  data[,1] <- lapply(data[,1], function(x) description(x, opt$des_length)) %>% unlist()
  ggp <- opt %$% data %>%
    set_colnames(c("y", "group", "x")) %>%
    ggplot()
  col <- opt %$% lazyopt::fenge(pointcolor)
  (ggp +
    geom_point(aes(color = group, y = y, x = x,size = x)) +
    theme_gray() + 
    scale_color_manual(values = col[1:((ggp$data$group) %>% unique() %>% length())])) +
    labs(color="Group", size="Size")%>% return()
}

lazyopt_ggplot2 <- function (opt, ggp, Development_mode = FALSE) 
{
  xlab_style <- lazyopt::fenge(opt$xlab_style)
  ylab_style <- lazyopt::fenge(opt$ylab_style)
  main_style <- lazyopt::fenge(opt$main_style)
  xtext_style <- lazyopt::fenge(opt$xtext_style)
  ytext_style <- lazyopt::fenge(opt$ytext_style)
  x_text_positon <- lazyopt::fenge(opt$x_text_positon) %>% 
    as.numeric()
  imageSize <- lazyopt::fenge(opt$imageSize)
  resolution <- match.arg(opt$resolution %>% as.character(), 
                          c("72", "96", "300", "600")) %>% as.numeric()
  width <- NA
  height <- NA
  if (imageSize[1] != "NULL") {
    width <- imageSize[1] %>% as.numeric()
  }
  if (imageSize[2] != "NULL") {
    height <- imageSize[2] %>% as.numeric()
  }
  result <- (ggp + 
               theme(legend.background = element_blank(), 
                     axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])), 
                     axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3]), angle = x_text_positon[1], hjust = x_text_positon[2]), 
                     axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2], colour = xlab_style[5]), 
                     axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2], colour = ylab_style[5]), 
                     title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2], color = main_style[5]),
                     plot.title = element_text(hjust = 0.5)) + 
               labs(title = main_style[4], y = ylab_style[4], x = xlab_style[4]))
  if (Development_mode) {
    return(result)
  }
  result %>% ggsave(paste0(opt$filepath, "/", opt$imageName, 
                           ".", opt$imageformt), ., width = width, height = height, 
                    dpi = resolution)
}


matrix(c(
  "inputfile", "i", 2, "character", "inputfile path", " ",
  "pointsize", "ps", 1, "numeric", "set point size", "2",
  "pointcolor", "pc", 1, "character", "set point color", "#E41A1C:#1E90FF:#4DAF4A:#984EA3:#ADD1E5:#999999:#66CC99:#9999CC:#CC6666:#FF8C00",
  "datatype", "dt", 1, "character", "standard file or GO file or KEGG file. input 'standard' or 'go' or 'kegg'","standard",
  "des_length", "dl", 1, "numeric", "the length of description","50",
  "top", "top", 1, "numeric", "the number of data for painting","30",
  "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:16: :black", 
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:11", 
  "x_text_positon", "xtp", 1, "character", "split by ':', sample angle:hjust", "60:1", 
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:11", 
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name:color", "sans:bold.italic:14: :black", 
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name:color", "sans:bold.italic:14: :black", 
  "imageSize", "is", 1, "character", "The height and width of the picture", "NULL:NULL", 
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300", 
  "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf", 
  "imageName", "in", 1, "character", "picture name", "boxplot_p", 
  "filepath", "o", 2, "character", "The package path of the output image", getwd(), 
  "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% 
  lazyopt() %>%
  lazyopt_ggplot2(., pp(.), FALSE)









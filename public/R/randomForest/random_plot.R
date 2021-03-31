#柱状图 top 
pacman::p_load(lazyopt, magrittr, reshape, ggplot2, dplyr, stringr)
args <- c("-i","F:/CloudPlatform/R/randomForest/VarImp.xls",
         "-o","F:/CloudPlatform/R/randomForest/t"
)

opt <- matrix(c(
  "inputpath", "i", 2, "character", "The path to the table data read", "",
  "outpath", "o", 2, "character", "The package path of the output image", "",
  "topdata", "td", 1, "numeric", "The amount of data readed from top","15",
  "colorstyle", "cs", 1, "character", "Set the color to ':' split", "#E41A1C:#9B445D:#526E9F:#3C8A9B:#469F6C:#54A453:#747B78:#94539E:#BD6066:#E97422:#FF990A:#FFCF20:#FAF632:#D4AE2D:#AF6729:#BF6357",
  "des_length", "dl", 1, "numeric", "the length of description","20",
  "xtext_positon", "xtp", 1, "character", "split by ':', sample angle:hjust:vjust", "90:1:0.5",
  "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:12",
  "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:12",
  "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name", "sans:bold.italic:18:Metabolite",
  "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name", "sans:bold.italic:18:VarImp",
  "main_style", "ms", 1, "character", "Main style Font:font type:font size:name", "sans:bold.italic:22: ",
  "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
  "imageSize", "is", 1, "character", "The height and width of the picture", "15:15",
  "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
  "imageName", "in", 1, "character", "picture name", "randomForest",
  "help", "h", 0, "logical", "help document", "TRUE"
), byrow = TRUE, ncol = 6) %>% lazyopt()

description <- function(des, len) {
  if(str_length(des) > len) {
    return (str_c(str_sub(des, 0, len), "..."))
  } else return (des %>% as.character())
}

data <- opt$inputpath %>% read.delim(check.names = F, header = TRUE) 

if(opt$topdata < nrow(data)) data %<>% .[1:opt$topdata,]

data[,1] <- lapply(data[,1], function(x) description(x, opt$des_length)) %>% unlist()

data %<>% mutate(Metabolite=factor(Metabolite, levels=rev(Metabolite)))

xtext_positon <- opt$xtext_positon %>% lazyopt::fenge() %>% as.numeric()

main_style <- opt$main_style %>% lazyopt::fenge()

xtext_style <- opt$xtext_style %>% lazyopt::fenge()

ytext_style <- opt$ytext_style %>% lazyopt::fenge()

xlab_style <- opt$xlab_style %>% lazyopt::fenge()

ylab_style <- opt$ylab_style %>% lazyopt::fenge()

imageSize <- opt$imageSize %>% lazyopt::fenge()

resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()

col <- lazyopt::fenge(opt$colorstyle)

#柱子从大到小 柱子横向

pp <- ggplot(data) +
  geom_col(aes(x = Metabolite, y = VarImp, fill = Metabolite), show.legend = F) +
  theme_bw() +
  scale_fill_manual(values = col) +
  theme(
    axis.line = element_line(),
    axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
    axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
    axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
    axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2]),
    title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
    panel.grid.major = element_blank(),
    panel.grid.minor = element_blank(),
    plot.title = element_text(hjust = 0.5)
  ) +
  labs(x = xlab_style[4], y = ylab_style[4], title = main_style[4]) + 
  coord_flip(ylim = c(min(data$VarImp) - abs(min(data$VarImp)), max(data$VarImp)*1.1)) +
  scale_y_continuous(expand = c(0, 0)) 


paste0(opt$outpath, "/", opt$imageName, ".", opt$imageformt) %>%
  ggsave(pp, width = as.numeric(imageSize[1]), height = as.numeric(imageSize[2]), dpi = resolution, units = "in")


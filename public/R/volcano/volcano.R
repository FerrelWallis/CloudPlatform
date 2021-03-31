pacman::p_load(lazyopt, ggrepel, dplyr, magrittr, ggplot2)

args <- c("-i","/Users/hudiecarina/Desktop/volcano.txt",
          "-o","/Users/hudiecarina/Desktop/volcano")

opt<-matrix(c("tablepath", "i", 2, "character", "The path to the table data read","",
             "filepath", "o", 2, "character", "The package path of the output image", "",
             "pCutoff_Line", "pcl", 1, "numeric", "A horizontal line will be drawn at -log10(pCutoff)", "10e-6",
             "FCcutoff_Line", "fcl", 1, "numeric", "Vertical lines will be drawn at the negative and positive values of log2FCcutoff", "1",
             "show_Line", "sp", 1, "logical", "whether to display pcline", "TRUE",
             "line_col", "lc", 1, "character", "set Line colour", "black",
             "xrang", "xr", 1, "character", "set range of x,split by :", "NULL",
             "yrang", "yr", 1, "character", "set range of y,split by :", "NULL",
             "colorstyle", "cs", 1, "character", "color style", "blue:red:grey",
             "xtext_style", "xts", 1, "character", "X text style Font:font type:font size", "sans:bold.italic:16",
             "ytext_style", "yts", 1, "character", "Y text style Font:font type:font size", "sans:bold.italic:16",
             "xlab_style", "xls", 1, "character", "X lab style Font:font type:font size:name", "sans:bold.italic:18:log2(FC)",
             "ylab_style", "yls", 1, "character", "Y lab style Font:font type:font size:name", "sans:bold.italic:18:-log10(pvalue)",
             "title_style","ts",1, "character","title style Font:font type:font size:name", "sans:bold.italic:20: ",
             "legend_title_style", "lts", 1, "character", "legend title style Font:font type:font size:name", "sans:bold.italic:16: ",
             "legend_text_style","ltes",1,"character", "legend title style", "sans:bold.italic:12",
             "imageSize", "is", 1, "character", "The height and width of the picture", "15:15",
             "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
             "imageformt", "if", 1, "character", "pdf,tiff,png", "pdf",
             "imageName", "in", 1, "character", "picture name", "mountain",
             "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6)%>%lazyopt()

xtext_style <- lazyopt::fenge(opt$xtext_style)
ytext_style <- lazyopt::fenge(opt$ytext_style)
xlab_style <- lazyopt::fenge(opt$xlab_style)
ylab_style <- lazyopt::fenge(opt$ylab_style)
ylab_style <- lazyopt::fenge(opt$ylab_style)
ylab_style <- lazyopt::fenge(opt$ylab_style)
legend_title_style<- lazyopt::fenge(opt$legend_title_style)
title_style<- lazyopt::fenge(opt$title_style)
legend_text_style<- lazyopt::fenge(opt$legend_text_style)


# if (opt$show_Line) { line <- "longdash" }else { line <- "blank" }

imageSize <- lazyopt::fenge(opt$imageSize) %>% as.numeric()
resolution <- match.arg(opt$resolution %>% as.character(), c("72", "96", "300", "600")) %>% as.numeric()
xrang <- opt$xrang; yrang <- opt$yrang
if (xrang == "NULL") { xrang <- NULL }else { xrang <- lazyopt::fenge(opt$xrang) %>% as.numeric() }
if (yrang == "NULL") { yrang <- NULL }else { yrang <- lazyopt::fenge(opt$yrang) %>% as.numeric() }

Dat<-read.table(opt$tablepath,header=TRUE,check.names = FALSE)

# p=ggplot(data=Dat,aes(x=Dat$`log2(fc)`,y=-log10(Dat$pvalue)))+ geom_point() + theme_minimal()
# p2 <- p + geom_vline(xintercept=c(-(opt$FCcutoff_Line), opt$FCcutoff_Line), col="red") +
#           geom_hline(yintercept=-log10(opt$pCutoff_Line), col="red")

Dat$diffexpressed<-"NO"
Dat$diffexpressed[Dat$`log2(fc)` > opt$FCcutoff_Line&Dat$pvalue<opt$pCutoff_Line] <- "UP"
Dat$diffexpressed[Dat$`log2(fc)` < opt$FCcutoff_Line&Dat$pvalue<opt$pCutoff_Line] <- "DOWN"

p = ggplot(data=Dat,aes(x=Dat$`log2(fc)`,y=-log10(Dat$pvalue),col=diffexpressed))+ geom_point() + theme_bw()

if (opt$show_Line) {
  p = p +
    geom_vline(xintercept=c(-(opt$FCcutoff_Line),opt$FCcutoff_Line),lty=3, col=opt$line_col,lwd=0.5) +
    geom_hline(yintercept=-log10(opt$pCutoff_Line),lty=3,col=opt$line_col,lwd=0.5)
}

# p3 <- p2 +scale_color_manual(values=c(opt$colorstyle1, opt$colorstyle2, opt$colorstyle3))
mycolors <- fenge(opt$colorstyle)
names(mycolors) <- c("DOWN", "UP", "NO")

p <- p + scale_colour_manual(values=mycolors, name=legend_title_style[4])+
       theme(axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
             legend.justification = c("right", "top"),
             legend.text = element_text(family = legend_text_style[1], size = as.numeric(legend_text_style[3]), face = legend_text_style[2]),
             legend.title = element_text(size = as.numeric(legend_title_style[3]), family = legend_title_style[1], face = legend_title_style[2]),
             title = element_text(size = as.numeric(title_style[3]), family = title_style[1], face = title_style[2]),
             plot.title = element_text(hjust = 0.5),
             axis.text.x = element_text(family = xtext_style[1], face = xtext_style[2], size = as.numeric(xtext_style[3])),
             axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
             axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2]))+
             labs(x = xlab_style[4], y = ylab_style[4],title = title_style[4])+
             scale_y_continuous(limits = yrang)+
             scale_x_continuous(limits = xrang)
         
       
paste0(opt$filepath, "/", opt$imageName, ".", opt$imageformt) %>%
ggsave(p, width = imageSize[1], height = imageSize[2], dpi = resolution)



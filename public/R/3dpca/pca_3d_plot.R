pacman::p_load(scatterplot3d, optparse, tidyverse, magrittr, dplyr, lazyopt)

arg <- c("-pc", "F:/CloudPlatform/R/3dpca/pca.x.xls",
         "-sd", "F:/CloudPlatform/R/3dpca/pca.sdev.xls",
         "-g", "F:/CloudPlatform/R/3dpca/PCA_group.txt",
         "-o", "F:/CloudPlatform/R/3dpca"
)

#是否在p值的正态近似中应用连续性校正
opt <- matrix(c("pcDataFile", "pc", 2, "character", "table file", "",
                "sdev", "sd", 2, "character", "sdev file", "",
                "group", "g", 1, "character", "sample group file", "",
                "outpath", "o", 2, "character", "output path", "",
                "colorstyle", "cs", 1, "character", "The parameter used to set the color of the column", "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
                "axis_color", "ac", 1, "character", "the color of axis", "black",
                "grid_color", "gc", 1, "character", "the color of grid", "lightgrey",
                "xyz", "xyz", 1, "character", "x:y:z", "PC1:PC2:PC3",
                "pointSize", "ps", 1, "numeric", "Point Size", "2",
                "Angle", "an", 1, "numeric", "Angle between x-axis and y-axis", "30",
                "axis_Size", "as", 1, "numeric", "Axis size", "1.1",
                "lab_Size", "ls", 1, "numeric", "lab Size", "1.5",
                "legend_Size", "les", 1, "numeric", "legend Size", "1.2",
                "legend_col", "lec", 1, "numeric", "columns of legend", "1",
                "legend_pos", "lep", 1, "character", "position of legend", "topleft",
                "label_text", "llt", 1, "logical", "whether draw labels", "FALSE",
                "label_pos", "llp", 1, "numeric", "position of labels, 1 below, 2 left, 3 above, 4 right", "3",
                "label_Size", "lls", 1, "numeric", "set the size of labels", "0.7",
                "main_Title", "mt", 1, "character", "Main Title", "",
                "scaley", "sy", 1, "numeric", "scale of y axis related to x- and z axis.", "1",
                "xlim", "xl", 1, "character", "x-axis limitation", "",
                "ylim", "yl", 1, "character", "y-axis limitation", "",
                "zlim", "zl", 1, "character", "z-axis limitation", "",
                "imageName", "in", 1, "character", "image name", "pca_3d",
                "imageFormat", "if", 1, "character", "image format, pdf,png,tiff", "pdf",
                "imageSize", "is", 1, "character", "The height and width of the picture", "12:12",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

pc <- read.table(opt$pc, sep="\t", head=T, check.names=F) %>% set_colnames(c("SampleID", colnames(.)[-1]))
sdev <- read.table(opt$sdev, sep="\t", head=F, check.names=F)

optxyz <- fenge(opt$xyz)
ip <- colnames(pc)[-1]
xid <- which(ip == optxyz[1])
yid <- which(ip == optxyz[2])
zid <- which(ip == optxyz[3])

if(opt$group != "") {
  group <- read.table(opt$group, sep="\t", head=F, check.names=F) %>% set_colnames(c("SampleID", "ClassNote"))
  sampleInfo <- group %>% select(c("SampleID", "ClassNote"))
  classNote <- unique(group$ClassNote)
  sampleColDf <- data.frame(ClassNote = classNote, col = fenge(opt$colorstyle)[1:length(classNote)]) 
  sampleColDf$col <- factor(sampleColDf$col, levels = sampleColDf$col)
  
  pc123 <- pc %>%
    select(c("SampleID", optxyz[1], optxyz[2], optxyz[3])) %>%
    set_colnames(c("SampleID", "xPc", "yPc", "zPc")) %>%
    left_join(sampleInfo, by = c("SampleID")) %>%
    left_join(sampleColDf, by = c("ClassNote"))
} else {
  onecolor <- fenge(opt$colorstyle)[1] %>% rep(., len = length(pc$SampleID)) %>% data.frame() %>% set_colnames(c("col"))
  pc123 <- pc %>%
    select(c("SampleID", optxyz[1], optxyz[2], optxyz[3])) %>%
    set_colnames(c("SampleID", "xPc", "yPc", "zPc")) %>% 
    cbind(onecolor)
}

impoPc1 <- sdev[xid, 2]
impoPc2 <- sdev[yid, 2]
impoPc3 <- sdev[zid, 2]

xlim <- if(opt$xlim == "") c() else fenge(opt$xlim) %>% as.numeric()
ylim <- if(opt$ylim == "") c() else fenge(opt$ylim) %>% as.numeric()
zlim <- if(opt$zlim == "") c() else fenge(opt$zlim) %>% as.numeric()

#超出xlim,ylim,zlim的标签去除
if(opt$xlim != "") pc123 %<>% subset(xPc >= xlim[1] & xPc <= xlim[2])
if(opt$ylim != "") pc123 %<>% subset(yPc >= ylim[1] & yPc <= ylim[2])
if(opt$zlim != "") pc123 %<>% subset(zPc >= zlim[1] & zPc <= zlim[2])

imageSize <- fenge(opt$imageSize)

pdf(paste0(opt$outpath, "/", opt$imageName, ".", opt$imageFormat), width = as.numeric(imageSize[2]), height = as.numeric(imageSize[1]))

gg <- scatterplot3d(pc123$xPc, pc123$yPc, pc123$zPc, color = pc123$col, pch = 20, col.axis = opt$axis_color, col.grid = opt$grid_color, mar = c(4, 4, 4, 3) + 0.1,
                    xlab = paste0(optxyz[1], "  ", impoPc1), ylab = paste0(optxyz[2], "  ", impoPc2), zlab = paste0(optxyz[3], "  ", impoPc3), scale.y = opt$scaley,
                    main = opt$main_Title, 
                    cex.symbols = opt$pointSize, 
                    angle = opt$Angle, 
                    cex.lab = opt$lab_Size, 
                    cex.axis = opt$axis_Size, 
                    xlim = xlim, 
                    ylim = ylim, 
                    zlim = zlim,
                    bg = "black")


if(opt$label_text) text(gg$xyz.convert(pc123$xPc, pc123$yPc, pc123$zPc), labels=pc123$SampleID, pos=opt$label_pos, cex = opt$label_Size)

if(opt$group != "") {
  legend(opt$legend_pos,inset=-0.01,pch=20,yjust=0,legend=levels(pc123$ClassNote), col = levels(sampleColDf$col), cex = opt$legend_Size, bty = 'n', xjust = 1, ncol = opt$legend_col)
} else {
  legend(opt$legend_pos,inset=-0.01,pch=20,yjust=0,legend=c("nogroup"), col = levels(pc123$col), cex = opt$legend_Size, bty = 'n', xjust = 1, ncol = 1)
}

dev.off()

pacman::p_load(tibble, base, ggtern, dplyr, ggplot2, lazyopt, magrittr)

arg <- c("-i", "F:/CloudPlatform/R/ternary/ternay_otu.txt",
         "-g", "F:/CloudPlatform/R/ternary/ternay_group.txt",
         "-t", "F:/CloudPlatform/R/ternary/ternay_enrich.txt",
         "-o", "F:/CloudPlatform/R/ternary",
         "-psz", "1")


opt <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                "grouppath", "g", 2, "character", "The path to the group data read", "",
                "tagpath", "t", 2, "character", "The path to the enrich or phylum data read", "",
                "outputfilepath", "o", 2, "character", "The package path of the output image", "",
                "point_colorstyle", "pcs", 1, "character", "Set the color to ':' split", "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#4daf4a:#984ea3:#a65628:#999999",
                "pointsize", "psz", 1, "numeric", "set point size method,0 mean none, 1 mean log2, 2 mean log10", "0",
                "mainlab", "ml", 1, "character", "set mainlab,split by [:] name:position", " :0.42",
                "xlab", "xl", 1, "character", "set xlab", " ",
                "ylab", "yl", 1, "character", "set ylab", " ",
                "zlab", "zl", 1, "character", "set zlab ", " ",
                "legendlab", "ll", 1, "character", "set legend title", " ",
                "imageSize", "is", 1, "character", "The height and width of the picture", "NULL:NULL",
                "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
                "imageformt", "if", 1, "character", "pdf,tiff,png", "png",
                "imageName", "in", 1, "character", "picture name", "sanyuan",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

resolution <- match.arg(opt %$%
                          as.character(resolution),
                        c("72", "96", "300", "600")) %>%
  as.numeric()

turnsize <- function(tempdata) {
  ave <- (tempdata[,1]+tempdata[,2]+tempdata[,3])/3
  if(opt$pointsize == 0) return(ave)
  else if(opt$pointsize == 1) return(log2(ave))
  else return(log10(ave))
}


table <- opt %$%
  read.delim(tablepath, header=T, check.names = FALSE)

group <- opt %$%
  read.delim(grouppath, header=T, check.names = FALSE)

enrich <- opt %$%
  read.delim(tagpath, header=T, check.names = FALSE)

colname <- colnames(table)
tgroup <- levels(group$group)
# a b c


aa <- c()
temp <- filter(group, group == tgroup[1])$samples  #NZ NQ 230Z
for(j in 1:length(temp)){
  index <- which(colname %in% as.character(temp[j]))
  aa<-c(aa,table[,index])
}
aa %<>% as.data.frame() %>% rownames_to_column() 
aa$rowname <- as.integer(aa$rowname)


bb <- c()
temp <- filter(group, group == tgroup[2])$samples  #NZ NQ 230Z
for(j in 1:length(temp)){
  index <- which(colname %in% as.character(temp[j]))
  bb<-c(bb,table[,index])
}
bb %<>% as.data.frame() %>% rownames_to_column()
bb$rowname <- as.integer(bb$rowname)

cc <- c()
temp <- filter(group, group == tgroup[3])$samples  #NZ NQ 230Z
for(j in 1:length(temp)){
  index <- which(colname %in% as.character(temp[j]))
  cc<-c(cc,table[,index])
}
cc %<>% as.data.frame() %>% rownames_to_column()
cc$rowname <- as.integer(cc$rowname)

gg <- table$OTUID

data <- merge(aa,bb,by="rowname",all=TRUE) 
data %<>% merge(cc,by="rowname",all=TRUE) %>% as.data.frame()

data["group"] <- enrich[,2]
data <- data[,-1]
colnames(data) <- c(tgroup,colnames(enrich)[2])

tempdata<-data
tempdata[is.na(tempdata)] <- 0
data["size"] <- turnsize(tempdata)

names <- data %>%
  colnames()

if (opt$xlab != " ") {
  names[1] <- opt$xlab
}

if (opt$ylab != " ") {
  names[2] <- opt$ylab
}

if (opt$zlab != " ") {
  names[3] <- opt$zlab
}

if (opt$legendlab != " ") {
  names[4] <- opt$legendlab
}

main <- opt %$%
  fenge(mainlab)


if (opt$legendlab == " ") {
  opt$legendlab <- names[4]
}

point_colorstyle <- opt %$%
  fenge(point_colorstyle)

point_colorstyle <- point_colorstyle[data[, 4] %>%
                                       unique() %>%
                                       seq_along]

imageSize <- opt %$%
  fenge(imageSize)
width <- NA
height <- NA
if (imageSize[1] != "NULL") {
  width <- imageSize[1] %>%
    as.numeric()
}
if (imageSize[2] != "NULL") {
  height <- imageSize[2] %>%
    as.numeric()
}

(ggtern(data,
        aes(x = data[, 1], y = data[, 2], z = data[, 3], color = data[, 4], group = data[, 4])) +
  theme_rgbw() +
  geom_point(aes(size = data[,5])) +
  guides(color = guide_legend(order = 1), size = guide_legend(order = 0)) +
  labs(x = names[1], y = names[2], z = names[3], title = main[1], color = names[4], size = "Size") +
  scale_color_manual(values = point_colorstyle) +
  theme(plot.title = element_text(hjust = main[2] %>%
    as.numeric()
  )
  )
) %>%
  ggsave(filename = opt %$%
    paste0(outputfilepath, "/", imageName, ".", imageformt),
         width = width, height = height, dpi = resolution)










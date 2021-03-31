rm(list=ls(all=TRUE))
options(warn = -1)
args <- commandArgs(TRUE)
#args <- c("-i","C:/Users/Administrator/Desktop/ss/pca.x.xls","-si","C:/Users/Administrator/Desktop/ss/pca.sdev.xls"
#          ,"-o","C:/Users/Administrator/Desktop/ss","-g","E:/projects/DrawPicture-r/com/work/PCA-/data/group.txt"
#          ,"-b","B,I","-c","TRUE"
#)
checknecessary <- function(args, spec) {
  dd <- spec[, 3]; dd <- which(dd == "2")
  data <- as.character(t(spec[dd, 1:2])); x <- data
  lab <- args[seq(1, length(args), 2)]; ll <- length(lab)
  for (i in 1:ll) {
    data[which(data == lab[i])] <- 1
  }
  data[which(data != "1")] <- 0
  jishu <- as.numeric(data[seq(1, length(data), 2)]); oushu <- as.numeric(data[seq(2, length(data), 2)])
  panduan <- jishu + oushu
  panduan <- which(panduan == 0)
  if (length(panduan) == 0) { return(TRUE) }else {
    cat("    Tip:Missing the necessary parameters/n")
    ll <- length(dd)
    for (i in 1:ll) {
      cat(paste("[", "[", spec[dd[i], 1], "|", spec[dd[i], 2], "]", "<", spec[dd[i], 4], ">", "]", sep = ""))
    }
    return(FALSE) }
}
changgeshowstr <- function(list) {
  lmax <- max(nchar(list)) + 4
  ll <- length(list)
  for (i in 1:ll) {

    cc <- lmax - nchar(list[i])

    list[i] <- paste(list[i], paste(rep(" ", cc), collapse = ""), sep = "")
  }
  return(list)
}
star <- function(list) {
  list[which(list == "2")] <- "*"
  list[which(list != "*")] <- " "
  return(list)
}
makeopt <- function(spec) {
  s <- spec; ll <- nrow(s)
  opt <- list()
  for (i in 1:ll) {

    if (s[i, 4] == "numeric") {
      opt[[i]] <- as.numeric(s[i, 6])
    }
    if (s[i, 4] == "logical") {
      opt[[i]] <- as.logical((s[i, 6]))
    }
    if (s[i, 4] == "character") {
      opt[[i]] <- s[i, 6]
    }
  }
  names(opt) <- s[, 1]
  return(opt)
}
getScriptName <- function() {
  falseargs <- commandArgs()
  falseargslist <- strsplit(falseargs[4], "=")
  return(as.character(falseargslist[[1]][2]))
}
caterror <- function(spec) {
  nleng <- nrow(spec); dd_list <- c()
  for (i in 1:nleng) {
    dd_list[i] <- paste("[", "[", spec[i, 1], "|", spec[i, 2], "]", "<", spec[i, 4], ">", "]", sep = "")
  }
  alldd <- paste(dd_list, collapse = " ")
  cat(paste("Useage:", getScriptName(), alldd, sep = " "))
}
showhelp <- function(spec) {
  caterror(spec); cat("\n")
  length <- nrow(spec)
  list1 <- paste(spec[, 1], "|", spec[, 2], sep = ""); list2 <- spec[, 3]; list3 <- spec[, 4]; list4 <- spec[, 5]; list5 <- spec[, 6]
  list1 <- changgeshowstr(list1)
  list2 <- star(list2); list2 <- changgeshowstr(list2)
  list3 <- changgeshowstr(list3)
  list4 <- changgeshowstr(list4)
  list5 <- changgeshowstr(list5)
  for (i in 1:length) {
    cat(paste("    ", list1[i], list2[i], list3[i], list4[i], "default  ", list5[i], "\n"))
  }

}
checkp <- function(args, spec) {
  length <- length(args); ip <- seq(1, length, 2); dd <- args[ip]
  cc <- c(spec[, 1], spec[, 2])
  if (all(dd %in% cc)) { return(T) }else {
    return(F) }
}
ascanshu <- function(opt, spec) {
  as <- spec[, 4]; ip <- which(as == "numeric")
  if(length(ip) == 0 ){return (opt)}
  lab <- spec[ip, 1]; length <- length(lab)

  for (i in 1:length) {
    a <- substr(lab[i], 3, nchar(lab[i]))
    opt[[which(names(opt) == a)]] <- as.numeric(opt[[which(names(opt) == a)]])
  }
  return(opt)
}
completeopt <- function(args, opt, spec) {

  lab <- args[seq(1, length(args), 2)]; num <- args[seq(2, length(args), 2)]
  list <- list(); ll <- nrow(spec)
  for (i in 1:ll) {
    list[[i]] <- spec[i, 1]
  }
  names(list) <- spec[, 2]
  ll <- length(lab)
  for (i in 1:ll) {
    if (substr(lab[i], 1, 2) != "--") {
      lab[i] <- list[[which(names(list) == lab[i])]]
    }
    lab[i] <- substr(lab[i], 3, nchar(lab[i]))
  }
  for (i in 1:ll) {
    opt[[which(names(opt) == lab[i])]] <- num[i]
  }
  opt <- ascanshu(opt, spec)
  return(opt)
}
cheackun <- function(spec) {
  if (length(spec[, 1]) != length(unique(spec[, 1])) || length(spec[, 2]) != length(unique(spec[, 2])))
    { return(F) }else { return(T) }
}
spec <- matrix(c("tablepath","i",2,"character","this is datatable path","",
                 "sedvpath","si",2,"character","this is sedv table path","",
                 "filepath","o",1,"character","this is file packagepath",getwd(),
                 "grouppath","g",1,"character","this is group path","",
                 "lab","b",1,"character","Displays the data names of those groups, split by ','","",
                 "circle","c",1,"logical","It is not recommended to use this parameter; the drawing is usually ugly","FALSE",
                 "imageSize", "is", 1, "character", "The height and width of the picture", "15:12",
                 "colorstyle","cs",1,"character","The parameter used to set the color of the column","#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
                 "resolution","dpi",1,"numeric","Set the resolution to allow 72,96,300 or 600","300",
                 "xtext_style", "xts", 1, "character", "X text style  sample:Font:font type:font size", "sans:plain:15",
                 "ytext_style", "yts", 1, "character", "Y text style  sample:Font:font type:font size", "sans:plain:15",
                 "xlab_style", "xls", 1, "character", "X lab style  sample:Font:font type:font size:name", "sans:plain:17",
                 "ylab_style", "yls", 1, "character", "Y lab style  sample:Font:font type:font size:name", "sans:plain:17",
                 "main_style", "ms", 1, "character", "Main style  sample:Font:font type:font size:name", "sans:plain:17: ",
                 "legendtext_style", "lts", 1, "character", "Legend text style  sample: Font:font type:font size", "sans:plain:14",
                 "legendtitle_style", "lms", 1, "character", "Legend title style  sample: Font:font type:font size:name", "sans:bold.italic:15: ",
                 "onecolor","oc",1,"character","no group file color","#48FF75",
                 "help","h",0,"logical","help document","TRUE"
), byrow = TRUE, ncol = 6)
if (!cheackun(spec)) {
  cat("Developer your parameter write repeat, quick fix bug")
  quit()
}
opt <- makeopt(spec)
speco <- spec
spec[, 1] <- paste("--", spec[, 1], sep = ""); spec[, 2] <- paste("-", spec[, 2], sep = "")
if (length(args) == 0 ||
  args == "--help" ||
  args == "-h") {
  showhelp(spec)
  quit()
}
if (length(args) %% 2 == 1 || !checkp(args, spec)) {
  cat("Tip:Parameter name error or missing parameter content/n")
  caterror(spec)
  quit()
}
if (!checknecessary(args, spec)) {
  cat("/n")
  quit()
}
opt <- completeopt(args, opt, spec)
rm(list=ls()[which(ls()!='opt')])

#--------------------------------------------------------------
fenge <- function(str) {
  str <- strsplit(str, ":")[[1]]
  return(str)
}
fenge2 <- function(str) {
  str <- strsplit(str, ",")[[1]]
  return(str)
}
imageSize <- opt$imageSize; imageSize <- fenge(imageSize)
resolution <- opt$resolution;if(resolution != 72 && resolution != 96 && resolution != 300 && resolution != 600){resolution <- 300}
mycol <- strsplit(opt$colorstyle, ":")[[1]]
main_style <- opt$main_style; main_style <- fenge(main_style)

xtext_style <- opt$xtext_style; xtext_style <- fenge(xtext_style)

ytext_style <- opt$ytext_style; ytext_style <- fenge(ytext_style)

xlab_style <- opt$xlab_style; xlab_style <- fenge(xlab_style)

ylab_style <- opt$ylab_style; ylab_style <- fenge(ylab_style)

legendtext_style <- opt$legendtext_style; legendtext_style <- fenge(legendtext_style)

legendtitle_style <- opt$legendtitle_style; legendtitle_style <- fenge(legendtitle_style)

lab <- opt$lab;if(lab == ""){lab <- NULL}

findxiab <- function (group){
  un <- unique(group);l <- length(un)
  xiabiao <- c()
  for(i in 1:l){
    xiabiao[i] <- max(which(group == un[i]))
  }
  return (xiabiao)
}
changc <- function (xiab,dd){
  length <- length(xiab)
  z <- 0
  for(i in (1:length)){
    x <- xiab[i]
    dd <- c(
      dd[(1:(x+z))],dd[(x+z)],dd[(x+z+1):length(dd)])
    z <- z+1
  }
  return(dd[1:(length(dd)-2)])
}


suppressMessages(library("ggplot2"))
suppressMessages(library("ggrepel"))


sedv <- read.delim(opt$sedvpath,header = FALSE)
xnumber <- sedv[1,2];ynumber <- sedv[2,2]
data <- read.delim(opt$tablepath)
data <- data.frame(names=rownames(data),x=data[,1],y=data[,2])


xmin <- min(data$x)*2;xmax <- max(data$x)*2
ymin <- min(data$y)*2;ymax <- max(data$y)*2

if(opt$grouppath == ""){
gg <-ggplot(data)+
  theme_bw()+
  geom_point(aes(x=x,y=y),size=5,color=opt$onecolor)+
  theme(legend.justification = c("right","top"),
        #panel.border = element_blank(),
        panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(),
        #y轴文本样式
        axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
        #x轴文本样式
        axis.text.x = element_text(family = ylab_style[1], face = ylab_style[2], size = as.numeric(ylab_style[3])),
        #图例文本样式
        legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
        #图例标题样式
        legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
        #主标题样式
        title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
        #x轴标题样式
        axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
        #y轴标题样式
        axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2])
  )+
  labs(x = paste("PC1:",xnumber," "),y = paste("PC2:",ynumber," ") ,title = main_style[4])+
  xlim(c(xmin,xmax))+
  ylim(c(ymin,ymax))+
  geom_text_repel(aes(x=x,y=y,label=data[,1]),size=6,force = 20,arrow = arrow(length = unit(0.01, "npc"), type = "open", ends = "last"),
                  point.padding = 0.5)
filepath <- paste(opt$filepath, "/","pca_nogroup.pdf", sep = "")
ggsave(filepath,gg,width = as.numeric(imageSize[1]),height =as.numeric(imageSize[2]),dpi=resolution )
}else{
  gdata <- read.delim(opt$grouppath,header = T)
  ll <- nrow(data)
  colorshape <- c();names <- as.character(data[,1]);key <- as.character(gdata[,1]);value <- as.character(gdata[,2])
  for(i in 1:ll){
      colorshape[i] <- value[which(key==names[i])]
  }
  data[,4] <- colorshape;colnames(data)[4] <- "colorshape"
  lab <- opt$lab;if(lab == ""){lab <- NULL}
  labdata <- NA
  if(opt$grouppath != "" & !is.null(lab)){
    lab <- fenge2(lab)
    kl <- length(lab)
    fuhe <- c()
    for(i in 1:kl){
      a <- which(data[,4] == lab[i])
      if(length(a) != 0){
        a <- a[1:(length(a)-1)]
        fuhe <- c(fuhe,a)
      }
    }
    labdata <- as.character(data[,1])
    lab <- labdata
    labdata[fuhe] <- NA
    lab[which(!is.na(labdata))] <- NA
    labdata <- lab
  }


  co <- fenge(opt$colorstyle)
  gg <-  ggplot(data)+
    theme_bw()+
    geom_point(aes(x=x,y=y,color= data[,4]
                   #,shape=data[,4]
    ),size=4)+
    theme(legend.justification = c("right","top"),
          #panel.border = element_blank(),
          panel.grid.major = element_blank(),
          panel.grid.minor = element_blank(),
          #y轴文本样式
          axis.text.y = element_text(family = ytext_style[1], face = ytext_style[2], size = as.numeric(ytext_style[3])),
          #x轴文本样式
          axis.text.x = element_text(family = ylab_style[1], face = ylab_style[2], size = as.numeric(ylab_style[3])),
          #图例文本样式
          legend.text = element_text(family = legendtext_style[1], size = as.numeric(legendtext_style[3]), face = legendtext_style[2]),
          #图例标题样式
          legend.title = element_text(size = as.numeric(legendtitle_style[3]), family = legendtitle_style[1], face = legendtitle_style[2]),
          #主标题样式
          title = element_text(size = as.numeric(main_style[3]), family = main_style[1], face = main_style[2]),
          #x轴标题样式
          axis.title.x = element_text(size = as.numeric(xlab_style[3]), family = xlab_style[1], face = xlab_style[2]),
          #y轴标题样式
          axis.title.y = element_text(size = as.numeric(ylab_style[3]), family = ylab_style[1], face = ylab_style[2])
    )+
    labs(x = paste("PC1:",xnumber," "),y = paste("PC2:",ynumber," ") ,title = main_style[4])+
    #geom_text(aes(x=x,y=y,label=labdata,
    #               color = data[,4]),size=6,inherit.aes = F,na.rm = T,parse = T,
    #             check_overlap = F,show.legend = as.vector(F)
    # )+
    geom_text_repel(aes(x=x,y=y,label=labdata,
                        color= data[,4]),size=6,force = 20,arrow = arrow(length = unit(0.01, "npc"), type = "open", ends = "last"),
                    point.padding = 0.5)+
    scale_color_manual(values = co,name=legendtitle_style[4])+
    xlim(c(xmin,xmax))+
    ylim(c(ymin,ymax))
  if(opt$circle){
    gg <- gg +stat_ellipse(data = data,aes(x=data[,2],y=data[,3],color=data[,4],group=data[,4])
      #,type="norm"
    )
    #gg <- gg + stat_ellipse(data =data,aes(x=data[,2],y=data[,3],fill=data$colorshape,color=data$colorshape),
    #                       type = "norm", geom = "polygon",alpha= 0.2,color=NA)
  }
  filepath <- paste(opt$filepath, "/", "pca-group", ".pdf", sep = "")
  ggsave(filepath,gg,width = as.numeric(imageSize[1]),height =as.numeric(imageSize[2]),dpi=resolution)
}




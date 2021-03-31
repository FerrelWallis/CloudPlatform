rm(list=ls(all=TRUE))

options(warn = -1)
args <- commandArgs(TRUE)
#args <- c("-i","C:/Users/Administrator/Desktop/yun/pca_twopart/data/table.txt",
#          "-o","C:/Users/Administrator/Desktop/yun/pca_twopart/xls","--scale","TRUE"
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
  if(length(ip) == 0){return(opt)}
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
spec <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                 "filepath", "o", 1, "character", "The package path of the output files", getwd(),
                 "scale","sca",1,"logical","scale data","TRUE"
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

#-------------------------------------------------------------------------------

setwd(opt$filepath)

da <- read.delim(opt$tablepath,check.names=FALSE,header = TRUE)

rownames(da) <- da[,1]
da <- da[,-1]
pca <- prcomp(t(da),scale = as.logical(opt$scale))
pvar <- pca$sdev ^2
pvar <- round((pvar /sum(pvar))*100,2)
pca12 <- pca$x
pca12 <- as.data.frame(pca12)
pvar <- paste(pvar,"%",sep="")
pvar <- as.data.frame(pvar)
rownames(pvar) <- paste("PCA",(1:nrow(pvar)),sep="")


names(pvar)<- NULL
  #画图数据
  write.table(x=pca12,file="pca.x.xls",sep="\t",col.names=NA)
  #标准�?
  write.table(x=pvar,file="pca.sdev.xls",sep="\t",col.names = NA)
  #特征向量矩阵
  write.table(x=pca$rotation ,file="pca.rotation.xls",sep="\t",col.names = NA)



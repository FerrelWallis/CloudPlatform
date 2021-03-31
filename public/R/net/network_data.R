#要求table1和table2的列数相�?
#method “pearson�?, “kendall�?, “spearman�?
rm(list = ls(all = TRUE))
options(warn = -1)
args <- commandArgs(TRUE)
#args <- c("-i1","E:/projects/DrawPicture-r/com/yun-work/network/data/env.txt",
#          "-i2","E:/projects/DrawPicture-r/com/yun-work/network/data/genus.nor.txt",
#          "-o","C:/Users/Administrator/Desktop/yun/network/network.xls"
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
  if (length(ip) == 0) { return(opt) }
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
spec <- matrix(c("tablepath1", "i1", 2, "character", "The file path of the table", "",
                 "tablepath2", "i2", 2, "character", "The file path of the table", "",
                 "method1", "m1", 1, "character", "Operation method:'pearson','kendall','spearman'", "pearson",
                 "method2", "m2", 1, "character", "Operation method:'pearson','kendall','spearman'", "pearson",
                 "filepath", "o", 2, "character", "Output file path", "",
                 "help", "-h", 1, "logical", "show help document", "TRUE"
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
rm(list = ls()[which(ls() != 'opt')])

corzhuanhuan <- function(data) {
  number <- as.numeric(matrix(data, nrow = 1))
  firstline <- rep(rownames(data), ncol(data))
  secondline <- as.character(t(matrix(rep(colnames(data), nrow(data)), ncol = nrow(data))))
  return(data.frame(firstline, secondline, number))
}
table1 <- read.delim(opt$tablepath1, check.names = FALSE, header = TRUE)
table2 <- read.delim(opt$tablepath2, , check.names = FALSE, header = TRUE)
rownames(table1) <- table1[, 1]; table1 <- table1[, -1]; table1 <- t(as.matrix(table1))
rownames(table2) <- table2[, 1]; table2 <- table2[, -1]; table2 <- t(as.matrix(table2))
pp <- cor(table1, table2, method = opt$method1)
outputdata <- corzhuanhuan(pp)
pvalueline <- c()
a1 <- ncol(table2); a2 <- ncol(table1)
for (i in 1:a1) {
  b1 <- as.numeric(table2[, i])
  for (x in 1:a2) {
    b2 <- as.numeric(table1[, x])
    result <- cor.test(b1, b2, method = opt$method2)
    pvalueline <- c(pvalueline, result$p.value)
  }
}
outputdata[, 4] <- pvalueline; colnames(outputdata) <- c("ID1", "ID2", opt$method1, "pvalue")
write.table(outputdata,file = opt$filepath,sep = "\t",col.names = NA, quote = F)



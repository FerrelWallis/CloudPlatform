options(warn = -1)
args <- commandArgs(TRUE)
# args <- c("-i1","F:/CloudPlatform/R/igc/test/table1.txt",
#            "-o","F:/CloudPlatform/R/igc/test/",
#            "-i2","F:/CloudPlatform/R/igc/test/table2.txt",
#            "-m", "pearson"
# )

checknecessary <- function(args, spec) {
  dd <- spec[, 3]; dd <- which(dd == "2")
  data <- as.character(t(spec[dd, 1:2]))
  lab <- args[seq(1, length(args), 2)]; ll <- length(lab)
  for (i in 1:ll) {
    data[which(data == lab[i])] <- 1
  }
  data[which(data != "1")] <- 0
  jishu <- as.numeric(data[seq(1, length(data), 2)]); oushu <- as.numeric(data[seq(2, length(data), 2)])
  panduan <- jishu + oushu
  panduan <- which(panduan == 0)
  if (length(panduan) == 0) { return(TRUE) }else {
    cat("    Tip:Missing the necessary parameters\n")
    ll <- length(dd)
    for (i in 1:ll) {
      cat(paste0("[", "[", spec[dd[i], 1], "|", spec[dd[i], 2], "]", "<", spec[dd[i], 4], ">", "]"))
    }
    return(FALSE) }
}

changgeshowstr <- function(list) {
  lmax <- max(nchar(list)) + 4
  ll <- length(list)
  for (i in 1:ll) {

    cc <- lmax - nchar(list[i])

    list[i] <- paste0(list[i], paste0(rep(" ", cc), collapse = ""))
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
  nleng <- nrow(spec); dd_list <- NULL
  for (i in 1:nleng) {
    dd_list[i] <- paste0("[", "[", spec[i, 1], "|", spec[i, 2], "]", "<", spec[i, 4], ">", "]")
  }
  alldd <- paste(dd_list, collapse = " ")
  cat(paste("Useage:", getScriptName(), alldd, sep = " "))
}

showhelp <- function(spec) {
  caterror(spec); cat("\n")
  length <- nrow(spec)
  list1 <- paste0(spec[, 1], "|", spec[, 2]); list2 <- spec[, 3]; list3 <- spec[, 4]; list4 <- spec[, 5]; list5 <- spec[, 6]
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

spec <- matrix(c("tablepath1", "i1", 2, "character", "The path to the table data read.necessary", " ",
                 "tablepath2", "i2", 1, "character", "The path to the table data read", " ",
                 "filepackagepath", "o", 2, "character", "The package path of the output image", "",
                 "method","m",1,"character","pearson,spearman","pearson",
                 "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6)
if (!cheackun(spec)) {
  cat("Developer your parameter write repeat, quick fix bug")
  quit()
}
opt <- makeopt(spec)
speco <- spec
spec[, 1] <- paste0("--", spec[, 1]); spec[, 2] <- paste0("-", spec[, 2])
if (length(args) == 0 ||
  args == "--help" ||
  args == "-h") {
  showhelp(spec)
  quit()
}
if (length(args) %% 2 == 1 || !checkp(args, spec)) {
  cat("Tip:Parameter name error or missing parameter content\n")
  caterror(spec)
  quit()
}
if (!checknecessary(args, spec)) {
  cat("\n")
  quit()
}
opt <- completeopt(args, opt, spec)

suppressMessages(library("pheatmap"))
suppressMessages(library("reshape"))
method <- opt$method

if (opt$tablepath2 != " ") {
  table1 <- as.matrix(read.delim(opt$tablepath1, check.names = FALSE, header = TRUE, row.names = 1))
  table2 <- as.matrix(read.delim(opt$tablepath2, check.names = FALSE, header = TRUE, row.names = 1))


  pp <- cor(table1, table2,method =method )

  a1 <- ncol(table1); a2 <- ncol(table2)

  pdata <- matrix(ncol = a2, nrow = a1)

  colnames(pdata) <- colnames(table2)
  rownames(pdata) <- colnames(table1)
  for (i in 1:a1) {
    use1 <- table1[, i]
    for (i2 in 1:a2) {
      use2 <- table2[, i2]
      pv <- cor.test(use1, use2,method=method)$p.value
      pdata[i, i2] <- pv
    }
  }


  candp <- melt(pp)
  candp[, 4] <- melt(pdata)[, 3]
  names(candp) <- c("x", "y", "c", "p")
  pp <- as.data.frame(pp)
  pp[is.na(pp)] <- 0
  pdata <- as.data.frame(pdata)
  pdata[is.na(pdata)] <- 1
  candp <- as.data.frame(candp)
  

  write.table(file = paste0(opt$filepackagepath, "/cor.xls"), pp, col.names = NA, sep = "\t", quote = F)
  write.table(file = paste0(opt$filepackagepath, "/pvalue.xls"), pdata, col.names = NA, sep = "\t", quote = F)
  write.table(file = paste0(opt$filepackagepath, "/pandv.xls"), candp, col.names = NA, sep = "\t", quote = F)

  display <- pdata
  l1 <- nrow(display); l2 <- ncol(display)
  for (i in 1:l1) {
    for (k in 1:l2) {
      a <- as.numeric(display[i, k])
      if (a <= 0.001) {
        a <- "***"
      }
      if (0.001 < a && a <= 0.01) {
        a <- "**"
      }
      if (0.01 < a && a < 0.05) {
        a <- "*"
      }
      if (a >= 0.05) {
        a <- ""
      }
      display[i, k] <- a
    }
  }
  write.table(file = paste0(opt$filepackagepath, "/p_star.xls"), display, col.names = NA, sep = "\t", quote = F)
}else {
  table1 <- as.matrix(read.delim(opt$tablepath1, check.names = FALSE, header = TRUE, row.names = 1))
  pp <- cor(table1,method=method)
  table2 <- table1
  a1 <- ncol(table1); a2 <- ncol(table2)

  pdata <- matrix(ncol = a2, nrow = a1)

  colnames(pdata) <- colnames(table2)
  rownames(pdata) <- colnames(table1)
  for (i in 1:a1) {
    use1 <- table1[, i]
    for (i2 in 1:a2) {
      use2 <- table2[, i2]
      pv <- cor.test(use1, use2,method=method)$p.value
      pdata[i, i2] <- pv
    }
  }

  candp <- melt(pp)
  candp[, 4] <- melt(pdata)[, 3]
  names(candp) <- c("x", "y", "c", "p")
  pp <- as.data.frame(pp)
  pp[is.na(pp)] <- 0
  pdata <- as.data.frame(pdata)
  pdata[is.na(pdata)] <- 1
  candp <- as.data.frame(candp)

  write.table(file = paste0(opt$filepackagepath, "/cor.xls"), pp, col.names = NA, sep = "\t", quote = F)
  write.table(file = paste0(opt$filepackagepath, "/pvalue.xls"), pdata, col.names = NA, sep = "\t", quote = F)
  write.table(file = paste0(opt$filepackagepath, "/pandv.xls"), candp, col.names = NA, sep = "\t", quote = F)

  display <- pdata
  l1 <- nrow(display); l2 <- ncol(display)
  for (i in 1:l1) {
    for (k in 1:l2) {
      a <- as.numeric(display[i, k])
      if (a <= 0.001) {
        a <- "***"
      }
      if (0.001 < a && a <= 0.01) {
        a <- "**"
      }
      if (0.01 < a && a < 0.05) {
        a <- "*"
      }
      if (a >= 0.05) {
        a <- ""
      }
      display[i, k] <- a
    }
  }
  write.table(file = paste0(opt$filepackagepath, "/p_star.xls"), display, col.names = NA, sep = "\t", quote = F)
}


options(warn = -1)
# args <- commandArgs(TRUE)
args <- c("-pi","F:/CloudPlatform/R/cca/test/otu.txt"
 ,"-ei","F:/CloudPlatform/R/cca/test/envi.txt"
 ,"-o","F:/CloudPlatform/R/cca/test"
 ,"-m","RDA"
)
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
spec <- matrix(c("phypath", "pi", 2, "character", "The path to the table data read", "",
                 "envpath", "ei", 2, "character", "The package path of the output image", "",
                 "filepath", "o", 2, "character", "package path of image output", "",
                 "method", "m", 1, "character", "method contains 'CCA','RDA'", "RDA",
                 "help", "h", 1, "character", "help document", ""
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


#---------------------------------------------------------------------------------------------------------


suppressMessages(library("vegan"))
#先读取数据数�?
pdata <- read.delim(opt$phypath, header = TRUE, check.name = F)
rownames(pdata) <- pdata[, 1]; pdata <- pdata[, -1]
pdata <- data.frame(t(pdata))
#pdata
#          Proteobacteria Bacteroidetes Actinobacteria Firmicutes Chloroflexi Gemmatimonadetes Acidobacteria Patescibacteria Cyanobacteria Verrucomicrobia
#1             10990          4661           1965        848        1584             1378          1349             271             5             131
#2             12410          4189           5959        314        2572             1230          1267             690           134             197
#115W           8483          1683           4446       7092        2070             1265          1751             296            42              79
#148Z          14226          5365           1449        954        1193             1333           224             292           560             277
#724Q          10784           818           3478      10597        1585              745           863             212            53              84
#3             15897          5963           1110        119         862             1144           740             339            67              69
#4             13278          5085           5195        482        2160             1378           792             486           312              45
#230Q          21894          1606           1121        900         693              516           556             133            11              43
#230Z          15324          3658           2683        641        3304             1535          1173             660            92              91
#NQ            12071          4200           2886        179        1982             1182          1413             367            82              93
#NZ            24983          1172            508        785          35              113            47              82            41               0


#读取环境因子数据
edata <- read.delim(opt$envpath, header = T, check.name = F)
rename <- edata[, 1]; edata <- edata[, -1]; rownames(edata) <- rename
#edata
#          pH       salt     water         TN         P      TN.P    TOC.P       TOC   TOC.TN
#NZ   0.9609462 0.14457421 0.9138139 0.11660774 1.1065309 1.4337698 2.866157 0.9842572 1.463296
#NQ   0.9474337 0.79274179 1.0073210 0.06220581 0.6963564 1.5997739 4.057058 1.6652995 2.469763
#230Z 0.9585639 0.41413736 1.2420442 0.02938378 1.0831441 0.8633229 2.506600 0.6585837 1.714330
#230Q 0.9675480 0.31701810 1.3909351 0.03502928 0.9561684 1.0588055 3.106232 1.0515384 2.090399
#148Z 0.9552065 0.62324929 1.3132343 0.10720997 1.2347703 1.2629255 2.510880 0.7942789 1.293804
#724Q 0.9527924 0.58771097 1.5386994 0.07773118 0.7589119 1.6268534 3.273453 0.9952841 1.666237
#115W 0.9439889 0.71600334 1.3856063 0.02938378 0.4608978 1.5802405 4.239702 1.5291736 2.671968
#1    1.0132587 0.05307844 0.6627578 0.08778142 0.8937618 1.5287882 3.993251 1.8340390 2.478826
#2    0.9459607 0.05307844 0.5051500 0.05153839 0.7419391 1.4604468 3.934080 1.6002103 2.490197
#3    0.9537597 0.33243846 1.1430148 0.04060234 0.9628427 1.1132747 3.167447 1.1146110 2.092194
#4    0.9951963 0.06258198 0.9637878 0.01786772 0.8750613 0.8727388 3.331553 1.1743506 2.522314

dec <- decorana(veg = pdata)


#dec
#                  DCA1    DCA2    DCA3     DCA4
#Eigenvalues     0.1745 0.08099 0.07011 0.056163
#Decorana values 0.2128 0.04838 0.01150 0.005464
#Axis lengths    1.0887 0.75183 0.70685 0.617369

#根据看分析结果中Axis Lengths的第一轴的大小
#如果大于4.0,就应选CCA（基于单峰模型，典范对应分析�?
#如果�?3.0-4.0之间，选RDA和CCA均可
#如果小于3.0, RDA的结果会更合理（基于线性模型，冗余分析�?

#sp0 <- rda(pdata ~ 1,edata)
#sp0
#      PC1       PC2       PC3       PC4       PC5       PC6       PC7       PC8       PC9
#230640019  14584603   5897882   1976853    377254    135335     13952      2618      1271


if (opt$method == "RDA") {
  sp1 <- rda(pdata ~ ., edata)
}
if (opt$method == "CCA") {
  sp1 <- cca(pdata ~ ., edata)
}
if (opt$method != "CCA" & opt$method != "RDA") { cat(paste("error method:", opt$method, sep = "")); quit() }
#sp1
#    RDA1     RDA2     RDA3     RDA4     RDA5     RDA6     RDA7     RDA8     RDA9
#27340305  9980333  2099739   575975   233508    40932     4410     3106      173
#
#Eigenvalues for unconstrained axes:
#PC1
#3731325
#cof <- coef(sp1)
#cof
#              RDA1        RDA2       RDA3        RDA4        RDA5        RDA6       RDA7       RDA8       RDA9
#pH        -0.4617515   18.137848  18.810077   51.631801 -100.353047 -32.7012179 -47.962839  31.625682 -30.349905
#salt      -0.6153840    2.435890   2.507287    4.229011   -9.081412  -1.6533708  -1.827274   1.709142  -1.317328
#water      0.8367332   -2.486120  -2.662608   -6.075875    9.197125   2.9299621   3.496295  -3.208216   1.582136
#TN        45.6872681   32.312657 -23.507576  -24.704413  -10.463506  41.0495115 108.308567 -42.174578  63.065806
#P        -20.7033006  -16.692857  13.001035   10.075582   -2.733192  -9.0806401 -45.693895  42.093263 -27.041427
#`TN-P`   -33.4082083 -150.730480  -9.445192 -141.954150  284.798794  -0.3214922   7.755425 -15.561988  18.154872
#`TOC-P`   14.8607250  132.039472  20.865749  147.091858 -279.779311 -11.4644091 -48.768747  52.552910 -40.687425
#TOC       12.5883477    3.694516 -11.863142  -17.042311   23.536926   9.8497329  35.123406 -37.789247  18.088562
#`TOC-TN` -26.3102011 -139.860963 -11.254846 -138.230762  270.041569   5.9781546  20.193044 -20.384050  25.462474


new <- sp1$CCA
RDA <- sp1$CCA$eig
dd <- round(RDA / sum(RDA), 4) * 100; rownames <- names(dd)
dd <- as.character(paste(dd, "%", sep = ""))
rda_cca_percent <- data.frame(percent = dd)
rownames(rda_cca_percent) <- rownames
write.table(rda_cca_percent, paste(opt$filepath, "/", "percent.xls", sep = ""), sep = "\t", col.names = NA)
write.table(as.data.frame(new$u), paste(opt$filepath, "/", "samples.xls", sep = ""), sep = "\t", col.names = NA)
write.table(as.data.frame(new$v), paste(opt$filepath, "/", "species.xls", sep = ""), sep = "\t", col.names = NA)
write.table(as.data.frame(new$biplot), paste(opt$filepath, "/", "envi.xls", sep = ""), sep = "\t", col.names = NA)


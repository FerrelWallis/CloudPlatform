pacman::p_load(reshape2, dplyr, lazyopt, magrittr, tibble, FSA)

arg <- c("-i", "F:/CloudPlatform/R/anova/aov_table.txt",
         "-g", "F:/CloudPlatform/R/anova/aov_group.txt",
         "-o", "F:/CloudPlatform/R/kruskalwallis"
)

#是否在p值的正态近似中应用连续性校正
opt <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                "grouppath", "g", 2, "character", "The path to the group data read", "",
                "outputfilepath", "o", 2, "character", "The package path of the output image", "",
                "corrected", "c", 1, "character", "the adjust method for p value:holm, hochberg, hommel, bonferroni, BH, BY, fdr, none", "fdr",
                "pthrN", "ptn", 1 , "numeric", "Corrected p-value significance threshold: Must be between 0 and 1", "",
                "qthrN", "qtn", 1 , "numeric", "Corrected p-value significance threshold: Must be between 0 and 1", "",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

std <- function(x) sd(x) / sqrt(length(x))

data = read.table(opt$tablepath, header=T, quote="", sep="\t", check.names=F)
group <- read.table(opt$grouppath, header = F, quote="", sep = "\t",check.names=FALSE)
uniq.group <- unique(group[,2]) %>% as.character()

combine <- function(groupname) {
  curgroup <- filter(group, V2 == groupname)[,1] %>% as.character()
  mean <- apply(data[, curgroup], 1, mean)
  var <- apply(data[, curgroup], 1, var)
  stderr <- apply(data[, curgroup], 1, std)
  colname <- c(paste0("mean(", groupname, ")"), paste0("variance(", groupname, ")"), paste0("stderr(", groupname, ")"))
  return (data.frame(mean, var, stderr) %>% set_rownames(data[,1]) %>% set_colnames(colname))
}

okk <- apply(data.frame(uniq.group), 1, function(x) {combine(x)}) %>% data.frame() %>% set_rownames(data[,1])

myfacFcVn <- function(groupname) {
  sample <- filter(group, V2 == groupname)[,1] %>% as.character()
  rep(groupname, length(sample))
}
facFcVn <- apply(data.frame(uniq.group), 1, function(x) myfacFcVn(x)) %>% unlist() %>% as.factor()

colorder <- apply(data.frame(uniq.group), 1, function(x) filter(group, V2 == x)[,1]) %>% unlist() %>% as.character()
okk$p <- apply(data[,colorder], 1, function(x) kruskal.test(x ~ facFcVn)[["p.value"]])
okk$p_cor <- p.adjust(okk$p, method = opt$corrected)

mydunn <- function(x) {
  temp <- dunnTest(x ~ facFcVn)$res %>% data.frame()
  tt <- temp[,4] %>% set_names(temp[,1])
  return(tt)
}

dunn <- apply(data[,colorder], 1, function(x) mydunn(x)) %>% t()
dunnname <- apply(data.frame(colnames(dunn)), 1, function(x) paste0("p(", x, ")"))

okk <- cbind(okk, dunn)
okk <- okk[order(okk[, "p"]),]

colname <- apply(data.frame(uniq.group), 1, function(x) { c(paste0("mean(", x, ")"), paste0("variance(", x, ")"), paste0("stderr(", x, ")")) }) %>% append(c("p", opt$corrected, dunnname)) %>% as.vector()

okk1 <- okk %>% set_colnames(colname)
write.table(okk1, file =  paste0(opt$outputfilepath, "/kw_test_results.xls"), sep = "\t", quote = FALSE, col.names = NA)

if(!is.na(opt$pthrN)) okk <- filter(okk, p <= opt$pthrN)
if(!is.na(opt$qthrN)) okk <- filter(okk, p_cor <= opt$qthrN)
okk2 <- okk %>% set_colnames(colname)
write.table(okk2, file =  paste0(opt$outputfilepath, "/kw_test_significant_results.xls"), sep = "\t", quote = FALSE, col.names = NA)

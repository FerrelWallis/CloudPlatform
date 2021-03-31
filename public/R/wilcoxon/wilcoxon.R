pacman::p_load(reshape2, dplyr, lazyopt, magrittr, tibble)

arg <- c("-i", "F:/CloudPlatform/R/wilcoxon/ttest_table.txt",
         "-g", "F:/CloudPlatform/R/wilcoxon/ttest_group.txt",
         "-o", "F:/CloudPlatform/R/wilcoxon"
)

#是否在p值的正态近似中应用连续性校正
opt <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                "grouppath", "g", 2, "character", "The path to the group data read", "",
                "outputfilepath", "o", 2, "character", "The package path of the output image", "",
                "paired", "p", 1, "logical", "Whether you want a paired t-test?", "FALSE",
                "tf", "tf", 1, "logical", "Whether to apply continuity correction in the normal approximation for the p-value?", "TRUE",
                "corrected", "c", 1, "character", "the adjust method for p value:holm, hochberg, hommel, bonferroni, BH, BY, fdr, none", "fdr",
                "pthrN", "ptn", 1 , "numeric", "Corrected p-value significance threshold: Must be between 0 and 1", "",
                "qthrN", "qtn", 1 , "numeric", "Corrected p-value significance threshold: Must be between 0 and 1", "",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

std <- function(x) sd(x) / sqrt(length(x))

data = read.table(opt$tablepath, header=T, quote="", sep="\t", check.names=F)
group <- read.table(opt$grouppath, header = T, quote="", sep = "\t", check.names=F) %>% set_colnames(c("V1","V2"))

uniq.group <- unique(group$V2) %>% as.character()
group1 <- filter(group, V2 == uniq.group[1])[,1] %>% as.character()
group2 <- filter(group, V2 == uniq.group[2])[,1] %>% as.character()

mean1 <- apply(data[, group1], 1, mean)
mean2 <- apply(data[, group2], 1, mean)
var1 <- apply(data[, group1], 1, var)
var2 <- apply(data[, group2], 1, var)
std1 <- apply(data[, group1], 1, std)
std2 <- apply(data[, group2], 1, std)

mylog2 <- function(x) {
  m1 <- if(x[1] == 0) 10e-6 else x[1]
  m2 <- if(x[2] == 0) 10e-6 else x[2]
  log2(m1 / m2)
}

logFC = apply(data.frame(mean1,mean2), 1, mylog2)  #mean1或mean2 如果是0 改成10e-6

okk <- data.frame(mean1 = mean1, mean2 = mean2, var1 = var1, var2 = var2, std1 = std1, std2 = std2, logFC = logFC) %>% set_rownames(data[,1])

mywilcox <- function(row, group1, group2) {
  x = as.numeric(row[group1])
  y = as.numeric(row[group2])
  tets = wilcox.test(x , y, alternative = "two.sided", paired = opt$paired, correct=opt$tf)
  tets$p.value
}

okk$p <- apply(data, 1, function(x) mywilcox(x, group1, group2))

okk$p_cor = p.adjust(okk$p, method = opt$corrected)
okk = okk[order(okk[, "p"]),]

colname <- c(paste0("mean(", uniq.group[1], ")"), paste0("mean(", uniq.group[2], ")"), paste0("variance(", uniq.group[1], ")"), paste0("variance(", uniq.group[2], ")"), paste0("stderr(", uniq.group[1], ")"), paste0("stderr(", uniq.group[2], ")"), paste0("log2FC(", uniq.group[2], "/", uniq.group[1], ")"), "p", opt$corrected)

okk1 <- okk %>% set_colnames(colname)

#col.names = NA列名开头空出一格
write.table(okk1, file =  paste0(opt$outputfilepath, "/wilcox_test_results.xls"), sep = "\t", quote = FALSE, col.names = NA)

if(!is.na(opt$pthrN)) okk <- filter(okk, p <= opt$pthrN)
if(!is.na(opt$qthrN)) okk <- filter(okk, p_cor <= opt$qthrN)
okk2 <- okk %>% set_colnames(colname)
write.table(okk2, file =  paste0(opt$outputfilepath, "/wilcox_test_significant_results.xls"), sep = "\t", quote = FALSE, col.names = NA)

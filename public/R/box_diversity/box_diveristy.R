pacman::p_load(lazyopt, vegan, ggplot2, dplyr, magrittr, tibble)

arg <- c("-i", "F:/CloudPlatform/users/6/ADB317154952/table.txt",
         "-g", "F:/CloudPlatform/users/6/ADB317154952/group.txt",
         "-o", "F:/CloudPlatform/users/6/ADB317154952"
)

opt <- matrix(c("inpath", "i", 2, "character", "input otu table", "",
                "grouppath", "g", 2, "character", "input group table", "",
                "outpath", "o", 2, "character", "output dir", "",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

otu_table <- read.delim(opt$inpath, header=T, sep="\t", row.names=1, stringsAsFactors = FALSE, check.names=F)

Transp_otu <- t(otu_table)

Alpha_diversity_index <- function(x, tree = NULL, base = exp(1)) {
  est <- estimateR(x)
  Obs <-  est[1, ]
  Shannon <- diversity(x, index = 'shannon', base = base)
  Simpson <- diversity(x, index = 'simpson')
  Pielou <- Shannon / log(Obs, base)
  goods_coverage <- 1 - rowSums(x == 1) / rowSums(x)
  
  result <- rbind(est, Shannon, Simpson,
                  Pielou, goods_coverage)
  if (!is.null(tree)) {
    Pd <- pd(x, tree, include.root = FALSE)[1]
    Pd <- t(Pd)
    result <- rbind(result, Pd)
  }
  result <- as.data.frame(t(result))
  return(result)
}

alpha_diversity <- Alpha_diversity_index(Transp_otu)
metadata <- read.delim(opt$grouppath, header=T, sep="\t", check.names = F)

alpha_diversity$Sample <- rownames(alpha_diversity)
plot_data <- merge(metadata, alpha_diversity, by = "Sample")

write.table(plot_data, paste0(opt$outpath, "/alpha_diversity.xls"), sep="\t", row.names = FALSE, quote = F)



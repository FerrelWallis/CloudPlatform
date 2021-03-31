pacman::p_load(lazyopt, dplyr, magrittr, vegan, ape)

# arg <- c("-i","F:/CloudPlatform/R/pcoa/pcoa_table.txt",
#          "-o","F:/CloudPlatform/R/pcoa")

opt <- matrix(c("tablepath", "i", 2, "character", "The path to the table data read", "",
                "filepath", "o", 2, "character", "The package path of the output files", getwd()
), byrow = TRUE, ncol = 6) %>% lazyopt()

opt %$% setwd(filepath)
pcoa <- opt %$% read.delim(tablepath, row.names = 1, check.names = FALSE, header = TRUE) %>% pcoa()
pvar <- NULL
for (i in seq_along(pcoa$values$Relative_eig)) {
  pvar[i] <- floor(pcoa$values$Relative_eig[i] * 100)
}
nums <- which(pvar > 0)
pvar <- pvar[nums] %>%
  paste0("%") %>%
  as.data.frame() %>%
  set_rownames(paste0("PCA", (seq_len(nrow(.))))) %>%
  set_colnames(NULL) %>%
  write.table(x = ., file = "PCOA.sdev.xls", sep = "\t", col.names = NA)
pcoa$vectors[, nums] %>%
  set_colnames(paste0("PCOA", 1:(ncol(.)))) %>%
  write.table(x = ., file = "PCOA.x.xls", sep = "\t", col.names = NA)





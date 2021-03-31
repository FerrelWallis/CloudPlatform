# Title     : TODO
# Objective : TODO
# Created by: xuyj
# Created on: 2020/10/14 0014
pacman::p_load(lazyopt, dplyr, magrittr)
arg <- c("-i", "E:/projects/R_draw_sc/test/table-t/data/a.xls",
         "-o", "E:/projects/R_draw_sc/test/table-t/data/a-t.xls")

opt <- matrix(
  c("inputfile", "i", 2, "character", "inputfile path", "",
    "outputfile", "o", 2, "character", "outputfile path", ""),
  ncol = 6, byrow = TRUE) %>% lazyopt(arg = NULL)
opt %$%
  read.delim(inputfile, check.names = FALSE, header = TRUE) %>%
  t() %>%
  write.table(., file = opt$outputfile, sep = "\t", col.names = NA)


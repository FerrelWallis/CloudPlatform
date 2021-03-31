pacman::p_load(optparse, tidyverse, magrittr, igraph, readxl, lazyopt)

arg <- c("-t", "F:/CloudPlatform/R/net/test/result.xls",
         "-o", "F:/CloudPlatform/R/net/test/"
)

opt <- matrix(c("tablepath", "t", 2, "character", "The path of table file", "",
                "p_thres", "pt", 1, "numeric", "Threshold of p value", "0.1",
                "c_thres", "ct", 1, "numeric", "Threshold of c value", "1",
                "outpath", "o", 2, "character", "The package path of the output image", "",
                "help", "h", 0, "numeric", "Help document", "1"
), byrow = TRUE, ncol = 6) %>% lazyopt()

table <- read.table(opt$tablepath, header=T, quote="", sep="\t", check.names=F) %>%.[,-1] %>% set_colnames(c("x","y","c","p"))

edgeData <- table %>% filter(p <= opt$p_thres, c <= opt$c_thres, !is.na(c), !is.na(p)) %>% set_colnames(c("Node1","Node2","r","P"))
Node1 <- unique(edgeData$Node1) %>% as.character()
Node2 <- unique(edgeData$Node2) %>% as.character()

graph <- graph_from_data_frame(edgeData, directed = F)
graph
vector <- igraph::evcent(graph = graph, directed = F) %>%
  .$vector
vector

nodeData <- unique(c(Node1,Node2)) %>% as.character() %>% data.frame() %>% set_colnames(c("Node")) %>% rowwise()

result <- nodeData %>% as_tibble() 
node <- nodeData %>% as_tibble() %>% .$Node
result$degree <- igraph::degree(graph = graph, v = node)
result$closeness <- igraph::closeness(graph = graph, vids = node)
result$betweenness <- igraph::betweenness(graph = graph, v =node, directed = F)
nodeData <- result %>% ungroup() %>% mutate(evcent = vector) %>% select(c("Node", "degree", "closeness", "betweenness", "evcent")) %>% set_colnames(c("Node", "degree", "Closeness Centrality", "Betweenness Centrality", "Eigenvector Centrality"))

write.table(nodeData, file =  paste0(opt$outpath, "/Network_Nodes.xls"), sep = "\t", quote = FALSE, col.names = NA)

outEdgeData <- edgeData %>%
  mutate(`|r|` = abs(r)) %>%
  mutate(`r>0` = (r > 0)) %>%
  mutate(`-ln (p)` = (-log(P)))

write.table(outEdgeData, file =  paste0(opt$outpath, "/Network_Edges.xls"), sep = "\t", quote = FALSE, col.names = NA)

#!usr/bin/perl -w
=head1 Version
Author: Zeng Liang,273590664@qq.com
Date: 2019-01-24
=head1 Usage
-type        <int>   normalize type , 1: row normalize ; 2: column normalize; 3 : do nothing; default 3
-format      <int>   work format, 
                     1: correct from -1 to 1 ; 
                     2: zscore correct ; 
                     3: correct from 0 to 1 ; 
                     4: do nothing; default 4
                     5: val/max;
                     6: val/all 
		     7: correlation mat;
                     8: row 2 column;
                     9: mat cluster;
                     10: filter mat value
                     11: average mat from group file
                     12: pca analysis   
                     13: ttest for group
                     14: wilcox for group 
-prefix      <str>   output:prefix ; work for format = 9, 12, 14; default: out
-group       <str>   input group file; work for format = 9 , 11, 12,14
-sample1     <str>   a1,a2,a3; work for format = 13
-sample2     <str>   b1,b2,b3; work for format = 13
-filter      <int>   value filter cutoff; default: 1
-triangle    <int>   report triangle matrix, 1: upper triangle matrix ; 2: lower triangle matrix ; 3:do nothing; defalut 3; must col num = row num
-diagonal    <int>   report diagonal,only work for triangle 1,2 ; 1: yes; 2: no; default : 2
-log         <int>   log for data, 1: log10(data+1);  2: log2(data+1), 3: log10(data); 4: log2(data); 5 :do nothing; defalut: 5; also work for format = 9
-split       <int>   split file method , 1: \t ; 2: \s+; default: 1
-help                output help information to screen
=head1 Exmple
perl deal_table.pl mat.txt -log 1
perl deal_table.pl mat.txt -format 7
perl deal_table.pl mat1.txt mat2.txt -format 7
perl deal_table.pl mat.txt -format 8
perl deal_table.pl mat.txt -format 9 -log 1
perl deal_table.pl mat.txt -format 9 -log 2 -group group.txt
perl deal_table.pl mat.txt -format 10 -filter 2
perl deal_table.pl mat.txt -group group.txt -format 11
perl deal_table.pl mat.txt -group group.txt -format 12
perl deal_table.pl mat.txt -sample1 a1,a2,a3 -sample2 b1,b2,b3 -format 13
perl deal_table.pl mat.txt -group group.txt -format 14
matrix.txt 
ID	s1	s2	s3
gene1	1 	2	3
gene2	4	5	6
...
group.txt
sample	Group
A1	A
A2	A
...
output: -triangle 1 -diagonal 1
ID	gene1	gene2	gene3	gene4
gene1	0	0.8	0.7	0.6
gene2	0	0	0.5	0.4
gene3	0	0	0	0.3
...
output: -triangle 2 -diagonal 2
ID	gene1	gene2	gene3	gene4
gene1	1	0	0	0
gene2	0.8	1	0	0
gene3	0.7	0.5	1	0
=cut
use strict;use Getopt::Long;use FindBin qw($Bin $Script);       use POSIX; 
#use Statistics::TTest;
my ($type, $format, $triangle, $diagonal, $help, $split, $log, $prefix, $group, $filter, $sample1, $sample2);
GetOptions(
	"type:i"=>\$type,
	"format:i"=>\$format,
	"prefix:s"=>\$prefix,
	"group:s"=>\$group,
	"sample1:s"=>\$sample1,
	"sample2:s"=>\$sample2,
        "filter:i"=>\$filter,
	"triangle:i"=>\$triangle,
	"diagonal:i"=>\$diagonal,	
	"split:i"=>\$split,
	"log:i"=>\$log,
	"help"=>\$help
);
die `pod2text $0` if (@ARGV < 1 || $help);
$type    ||=3;  $format   ||=4;       $triangle  ||=3;    $diagonal    ||=2; 	$split	||=1;	$log	||=5; $prefix ||="out"; $filter ||=1;
my %mat;    my %raw;    my $row=0;  my $col=0;  
my %max_col;    my %max_row;    my %min_col;    my %min_row;my %var_col;    my %var_row;    my %mea_col;    my %mea_row;    my %all_col;	my %all_row;
if($format <= 6){
	open A,$ARGV[0];	my $head=<A>;	print "$head";	chomp $head;	my @head=split /\t/,$head;
	if($split == 2){
		my @head=split /\s+/,$head;
	}
	foreach my $k(1..$#head){
		$max_col{$k}=-10000000;	$min_col{$k}=10000000;	
	}
	while(<A>){
		chomp;my @a=split /\t/;
		if($split == 2){		@a=split /\s+/;	}
		$row++;		$col=$#a;	$raw{$row}=[@a];
		$max_row{$row}=-100000;	$min_row{$row}=100000;	
		foreach my $k(1..$#a){
			$mea_row{$row}+=($a[$k]/$col);	$all_row{$row}+=$a[$k];
			$mea_col{$k}+=$a[$k];		$all_col{$k}+=$a[$k];
			if($max_row{$row} < $a[$k]){		$max_row{$row}=$a[$k];	}
			if($min_row{$row} > $a[$k]){            $min_row{$row}=$a[$k];  }
			if($max_col{$k} < $a[$k]){            $max_col{$k}=$a[$k];      }
			if($min_col{$k} > $a[$k]){            $min_col{$k}=$a[$k];      }
		}
	}
	if($type == 3 && $format == 4 && $triangle == 3){
		foreach my $k1(1..$row){
			print "$raw{$k1}[0]";
			foreach my $k2(1..$col){
				my $corr=0;
				if($log == 1){		$corr=log(($raw{$k1}[$k2]+1))/log(10);	 }
				if($log == 2){          $corr=log(($raw{$k1}[$k2]+1))/log(2);    }
				if($log == 3){ 
					if($raw{$k1}[$k2] > 0){
						$corr=log($raw{$k1}[$k2])/log(10);        
					}
				}
				if($log == 4){  
					if($raw{$k1}[$k2] > 0){
						$corr=log($raw{$k1}[$k2])/log(2);         
					}
				}
				print "\t$corr";
			}
			print "\n";
		}
	}

	foreach my $k(1..$col){	$mea_col{$k}=$mea_col{$k}/$row;	}

	foreach my $k1(1..$row){
		foreach my $k2(1..$col){
			if($col > 1){			$var_row{$k1}+=(($raw{$k1}[$k2]-$mea_row{$k1})**2)/($col-1);		}
			if($row > 1){			$var_col{$k2}+=(($raw{$k1}[$k2]-$mea_col{$k2})**2)/($row-1);		}
		}
	}
	if($col > 1){
		foreach my $k(1..$row){	$var_row{$k}=sqrt($var_row{$k});	}
	}
	if($row > 1){
		foreach my $k(1..$col){	$var_col{$k}=sqrt($var_col{$k});	}
	}

	if($type == 2 && $triangle == 3){
		foreach my $k1(1..$row){
			print "$raw{$k1}[0]";
			foreach my $k2(1..$col){
				my $corr=$raw{$k1}[$k2];	my $len=$max_col{$k2}-$min_col{$k2};
				if($format == 5){				$corr=$raw{$k1}[$k2]/$max_col{$k2};			}
				if($format == 6){                               $corr=$raw{$k1}[$k2]/$all_col{$k2};                     }
				if($format == 3){
					if($len > 0){				$corr=($raw{$k1}[$k2]-$min_col{$k2})/$len;		}
				}
				if($format == 2){
					$corr=0;
					if($var_col{$k2} > 0){			$corr=($raw{$k1}[$k2]-$mea_col{$k2})/$var_col{$k2};	}
				}
				if($format == 1){
					if($len > 0){				$corr=2*($raw{$k1}[$k2]-$min_col{$k2})/$len-1;		}
				}
				print "\t$corr";
			}
			print "\n";
		}
	}
	if($type == 1 && $triangle == 3){
		foreach my $k1(1..$row){
			print "$raw{$k1}[0]";
			foreach my $k2(1..$col){
				my $len=$max_row{$k1}-$min_row{$k1};            my $corr=$raw{$k1}[$k2];
				if($format == 5){				$corr=$raw{$k1}[$k2]/$max_row{$k1};			}
				if($format == 6){                               $corr=$raw{$k1}[$k2]/$all_row{$k1};                     }
				if($format == 3){
					if($len > 0){				$corr=($raw{$k1}[$k2]-$min_row{$k1})/$len;		}
				}
				if($format == 2){
					$corr=0;
					if($var_row{$k1} > 0){			$corr=($raw{$k1}[$k2]-$mea_row{$k1})/$var_row{$k1};	}
				}
				if($format == 1){
					if($len > 0){				$corr=2*($raw{$k1}[$k2]-$min_row{$k1})/$len-1;		}
				}
				print "\t$corr";
			}
			print "\n";
		}
	}
	if($row == $col && $type == 3 && $format == 4 && $triangle == 1 && $diagonal == 1){
		foreach my $k1(1..$row){
			print "$raw{$k1}[0]";
			foreach my $k2(1..$col){
				if($k1 < $k2){				print "\t$raw{$k1}[$k2]";	}
				if($k1 >= $k2){				print "\t0";			}
			}
			print "\n";
		}
	}
	if($row == $col && $type == 3 && $format == 4 && $triangle == 1 && $diagonal == 2){
		foreach my $k1(1..$row){
			print "$raw{$k1}[0]";
			foreach my $k2(1..$col){
				if($k1 <= $k2){			print "\t$raw{$k1}[$k2]";		}
				if($k1 > $k2){			print "\t0";				}
			}
			print "\n";
		}
	}
	if($row == $col && $type == 3 && $format == 4 && $triangle == 2 && $diagonal == 1){
		foreach my $k1(1..$row){
			print "$raw{$k1}[0]";
			foreach my $k2(1..$col){
				if($k1 > $k2){
					print "\t$raw{$k1}[$k2]";
				}
				if($k1 <= $k2){
					print "\t0";
				}
			}
			print "\n";
		}
	}
	if($row == $col && $type == 3 && $format == 4 && $triangle == 2 && $diagonal == 2){
		foreach my $k1(1..$row){
			print "$raw{$k1}[0]";
			foreach my $k2(1..$col){
				if($k1 >= $k2){
					print "\t$raw{$k1}[$k2]";
				}
				if($k1 < $k2){
					print "\t0";
				}
			}
			print "\n";
		}
	}
}
if($format  == 7){
	if(@ARGV == 2){
		open OUT,">$ARGV[0].r";
		print OUT "da1<-read.table(\"$ARGV[1]\",sep=\"\\t\",header=T)
da2<-read.table(\"$ARGV[0]\",sep=\"\\t\",header=T)
size1=nrow(da1)
size2=nrow(da2)
pvalue<-vector()
value<-vector()
pp1<-vector()
pp2<-vector()
num<-1
for(i in 1:size1){
	for(j in 1:size2){
		a1<-as.numeric(da1[i,2:ncol(da1)])
		a2<-as.numeric(da2[j,2:ncol(da2)])
		corr<-cor.test(a1,a2)
		pp1[num]<-as.character(da1[i,1])
		pp2[num]<-as.character(da2[j,1])
		esti<-as.matrix(corr\$estimate)[1]
		value[num]<-esti
		pvalue[num]<-corr\$p.value
		num<-num+1
	}
}
write.table(data.frame(id1=pp1,id2=pp2,correlation=value,pvalue=pvalue),\"correlation.txt\",quote=FALSE,sep=\"\\t\",row.names = F)\n";
	close OUT;
	`R --no-save < $ARGV[0].r`;
	}
	if(@ARGV == 1){
		open OUT,">$ARGV[0].r";
		print OUT "library(Hmisc)
data<-read.delim(\"$ARGV[0]\", header=T, check.names=F, sep=\"\\t\")
rownames(data) = data[,1]
data = data[,2:length(data[1,])]
data = as.matrix(data)
colnames(data)<-colnames(data)
value <- rcorr(data,type=\"pearson\")
corr<-value\$r
pvalue<-value\$P
write.table(corr,\"correlation.xls\",quote=FALSE,sep=\"\\t\")
write.table(pvalue,\"pvalue.xls\",quote=FALSE,sep=\"\\t\")\n";
		close OUT;
		`R --no-save < $ARGV[0].r`;
	}
}
if($format == 8){
	open A,$ARGV[0];	my $num=0;  my %h;
	while(<A>){
		chomp;my @a=split /\t/;
		if($split == 2){        @a=split /\s+/; }
		$h{$.}=[@a];	    $num=$#a;
	}
	close A;
	foreach my $k1(0..$num){
		my @sam;
		foreach my $k2(sort {$a<=>$b} keys %h){
			push @sam,$h{$k2}[$k1];
		}
		my $sam=join "\t",@sam;	    print "$sam\n";
	}
}
if($format == 9){
	my $sample_dist = "euclidean";	my $sample_clust = "complete";	my $sample_cor = "pearson";
	open RCMD, ">$prefix.cmd.r";
	print RCMD "library('ape')
pdf(\"$prefix.hcluster.pdf\", width=8,height=6)
par(mar=c(3,2,2,12))
data = read.table(\"$ARGV[0]\", header=T, com='', sep=\"\\t\", row.names=1, check.names=F)
data = as.matrix(data)\n";
if($log == 1){
	print RCMD "data = log10(data+1)\n";
}
if($log == 2){
        print RCMD "data = log2(data+1)\n";
}
if($log == 3){
        print RCMD "data = log10(data)\n";
}
if($log == 4){
        print RCMD "data = log2(data)\n";
}
print RCMD "sample_cor = cor(data, method=\"$sample_cor\", use='pairwise.complete.obs')
sample_dist = as.dist(1-sample_cor)
write.table(sample_cor,file=\"$prefix.correlation.txt\",sep=\"\t\",quote=F)
hc_samples = hclust(sample_dist, method=\"$sample_clust\")
tree <-as.dendrogram(hc_samples)
plot(tree,type=\"rectangle\",horiz=TRUE,cex=0.1)
tr <-as.phylo.hclust(hc_samples)
write.tree(tr,\"$prefix.hcluster.tre\")
dev.off()\n";
	close RCMD;
	`R --no-save < $prefix.cmd.r`;
	if(defined $group){
		`/lustre/sdb/xueyj/conda/lib/bin/Rscript $Bin/xueyj/tree/tree2.0.R -i $prefix.hcluster.tre -o ./ -g $group -if $prefix.hcluster.group.pdf -bw 1`;
	}
}
if($format == 10){
	open A,$ARGV[0]; my $head=<A>;	print "$head";
	while(<A>){
	    chomp;my @a=split /\t/;my $num=0;
	    foreach my $k(1..$#a){
        	if($a[$k] > $filter){
	            $num++;
        	}
	    }
	    next if($num == 0);	    print "$_\n";
	}
}
if($format == 11){
	open A,$ARGV[0];        open B,$group;	my %group;<B>; my %map;
	while(<B>){
        	chomp;my @a=split; $group{$a[1]}=1;$map{$a[0]}=$a[1];
	}
	close B;
	my $h=<A>;      chomp $h;       my @h=split /\t/,$h;    my @group=sort keys %group;     my $sub_group=join "\t",@group;
	print "GeneID\t$sub_group\n";
	while(<A>){
        	chomp;my @a=split; my %aver;
	        foreach my $k(1..$#a){
        	        $aver{$map{$h[$k]}}[0]+=$a[$k];
                	$aver{$map{$h[$k]}}[1]++;
	        }
        	print "$a[0]";
	        foreach my $k(@group){
        	        printf "\t%.3f",$aver{$k}[0]/$aver{$k}[1],
	        }
        	print "\n";
	}
	close A; close B;
}
if($format == 12){
	if(defined $group){
		`perl $Bin/plot-pca.pl -i $ARGV[0] -o $prefix -m $group -g Group`;
	}
	else{
		`perl $Bin/plot-pca.pl -i $ARGV[0] -o $prefix`;
	}
}
if($format == 13){
	open A,$ARGV[0]; my $head=<A>;	chomp $head;my @head=split /\t/,$head; my @sample1=split /,/,$sample1; my @sample2=split /,/,$sample2;	my %group1;	my %group2;
	print "$head[0]"; my $ttest = new Statistics::TTest;
	foreach my $k1(1..$#head){
		foreach my $k2(@sample1){
			if($head[$k1] eq $k2){
				$group1{$k1}=1;	print "\t$head[$k1]";
			}
		}
	}
	foreach my $k1(1..$#head){
		foreach my $k2(@sample2){
			if($head[$k1] eq $k2){
				$group2{$k1}=1;	print "\t$head[$k1]";
			}
		}
	}
	print "\tttest-value\tpvalue\n";
	while(<A>){
	        chomp;my @a=split; my @group1; my @group2;
		foreach my $k(1..$#a){
			if(exists $group1{$k}){	push @group1,$a[$k];	}
			if(exists $group2{$k}){ push @group2,$a[$k];    }
		}
		$ttest->load_data(\@group1,\@group2);
		my $s1=$ttest->{s1};            my $s2=$ttest->{s2};
		my $mean1=$s1->mean;            my $mean2=$s2->mean;
		my $var1=$s1->variance;         my $var2=$s2->variance;
		my $tvalue=$ttest->t_statistic;
		my $pvalue=$ttest->{t_prob};
		my $group1=join "\t",@group1;	my $group2=join "\t",@group2;
		print "$a[0]\t$group1\t$group2\t$tvalue\t$pvalue\n";

	}
}
if($format == 14){
	open OUT,">$prefix.cmd.r";
	print OUT "da = read.table(\"$ARGV[0]\",sep=\"\\t\",header=TRUE,check.names=F)
data<-da[,-1]
row.names(data)<-da[,1]
tr_data<-as.matrix(data)
group = read.table(\"$group\",sep=\"\\t\",header=TRUE,check.names=F)
sub<-table(group[,2])
pos1<-which(colnames(tr_data)==as.character(group[which(group[,2]==names(sub)[1]),1]))
pos2<-which(colnames(tr_data)==as.character(group[which(group[,2]==names(sub)[2]),1]))
num<-nrow(tr_data)
pvalue<-vector()
for(i in 1:num){
	d1=tr_data[i,pos1]
	d2=tr_data[i,pos2]
	wt <-wilcox.test(d1,d2,exact=F)
	pvalue[i]<-wt\$p.value
}
padjust<-p.adjust(pvalue,method=\"BH\")
result<-cbind(rownames(tr_data),tr_data[,pos1],tr_data[,pos2],pvalue=pvalue,FDR=padjust)
write.table(result,\"$prefix.wilcox.xls\", sep = \"\\t\", quote = F ,row.names = F , col.names = T)\n";
	close OUT;
	`R --no-save < $prefix.cmd.r`;
	`rm $prefix.cmd.r`;
}

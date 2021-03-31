#!/usr/bin/perl
use strict;
use warnings;
die "usage:perl $0 <file> <type:kegg or go>  <out.prefix> <length>\n" unless @ARGV == 4;
open A,$ARGV[0];open OUT,">$ARGV[2]ment.xls.1";
if($ARGV[1] eq "kegg"){
	<A>;
	while(<A>){
		my @a=split /\t+/;		my $len1=length($a[2]);
		if ($len1 >$ARGV[3]){
			$a[2] = substr( $a[2], 0, $ARGV[3] );		$a[2] .= "..";
			print OUT "$a[2]\t$a[4]\t$a[5]\t$a[6]\n";
		}
		else{
			print OUT "$a[2]\t$a[4]\t$a[5]\t$a[6]\n";
		}

	}
	close A;
}
if($ARGV[1] eq "go"){
	<A>;
	while(<A>){
		chomp;my @a=split /\t+/;my @b1=split /\//,$a[3];my @b2=split /\//,$a[4];my $len=length($a[2]);
		if ($len >$ARGV[3]){
			$a[2] = substr( $a[2], 0, $ARGV[3] );
			$a[2] .= "..";
			print OUT "$a[2]\t$b1[0]\t$b2[0]\t$a[5]\n";
		}
		else{
			print OUT "$a[2]\t$b1[0]\t$b2[0]\t$a[5]\n";
		}
	}
	close A;
}
open KEGGOUT,">$ARGV[0].cmd.r";
print  	KEGGOUT "library(ggplot2)
data <-read.delim(file=\"$ARGV[2]ment.xls.1\",header=FALSE,sep=\"\\t\")
numm<-nrow(data)
if(numm > 31){
	data2 <-data[1:30,]
}else{
	data2 <-data
}
Term<-as.matrix(data2[,1])
DEGs_number<-as.matrix(data2[,2])
c<-as.matrix(data2[,3])
Rich_factor<-DEGs_number/c;Rich_factor
Pvalue<-as.matrix(data2[,4])
pp<-ggplot(data2,aes(x=Rich_factor,y=reorder(Term,Pvalue),colour=Pvalue,size=DEGs_number))+geom_point()+scale_size_continuous(range=c(2,6))+scale_colour_gradientn(colours=c(\"red\",\"yellow\",\"green\",\"lightblue\",\"blue\"))+ylab(\"Term\")
ggsave(plot=pp,height=8,width=8,filename=\"$ARGV[2].pdf\", useDingbats=FALSE)";
`/lustre/sdb/taoye/miniconda3/bin/R --no-save < $ARGV[0].cmd.r`;	`rm $ARGV[0].cmd.r`;
close KEGGOUT;
